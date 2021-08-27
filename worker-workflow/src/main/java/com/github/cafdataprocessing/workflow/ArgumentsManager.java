/*
 * Copyright 2017-2021 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.workflow;

import com.github.cafdataprocessing.workflow.model.ArgumentDefinition;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Field;
import com.microfocus.darwin.settings.client.*;
import com.squareup.okhttp.*;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import com.squareup.okhttp.logging.HttpLoggingInterceptor.Level;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ArgumentsManager {

    private final static Logger LOG = LoggerFactory.getLogger(ArgumentsManager.class);
    private static final int SETTINGS_SERVICE_CACHE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MiB
    private static final String SETTINGS_SERVICE_CACHE_TEMP_DIRECTORY_PREFIX = "settings-service-http-cache";
    private static final Interceptor FORCE_CACHE_REFRESH_CACHE_CONTROL_INTERCEPTOR = new ForceCacheRefreshCacheControlInterceptor();

    private final Gson gson = new Gson();
    private final SettingsApi settingsApi;
    private final OkHttpClient okHttpClient;

    public ArgumentsManager(final String settingsServiceUrl)
    {
        this(new SettingsApi(), settingsServiceUrl);
    }

    public ArgumentsManager(final SettingsApi settingsApi, final String settingsServiceUrl){
        Objects.requireNonNull(settingsApi);
        Objects.requireNonNull(settingsServiceUrl);
        this.settingsApi = settingsApi;
        okHttpClient = new OkHttpClient();
        final File settingsServiceCacheDirectory;
        try {
            settingsServiceCacheDirectory = Files.createTempDirectory(SETTINGS_SERVICE_CACHE_TEMP_DIRECTORY_PREFIX).toFile();
        } catch (final IOException ex) {
            throw new RuntimeException("Unable to create a temporary directory for the Settings Service cache", ex);
        }
        final Cache cache = new Cache(settingsServiceCacheDirectory, SETTINGS_SERVICE_CACHE_SIZE_BYTES);
        okHttpClient.setCache(cache);
        okHttpClient.networkInterceptors().add(getCacheControlInterceptor());
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(Level.BODY);
        okHttpClient.interceptors().add(logging);
        
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(settingsServiceUrl);
        apiClient.setHttpClient(okHttpClient);
        settingsApi.setApiClient(apiClient);
    }

    private Interceptor getCacheControlInterceptor() {
        return chain -> {
            final Response response = chain.proceed(chain.request());
            final CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(5, TimeUnit.MINUTES)
                    .build();

            return response.newBuilder()
                    .header("Cache-Control", cacheControl.toString())
                    .build();
        };
    }

    private final static class ForceCacheRefreshCacheControlInterceptor implements Interceptor
    {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException
        {
            final CacheControl cacheControl = new CacheControl.Builder()
                .noCache()
                .build();

            final Request originalRequest = chain.request();
            final Request forceCacheRefreshRequest = originalRequest.newBuilder().cacheControl(cacheControl).build();
            return chain.proceed(forceCacheRefreshRequest);
        }
    }

    public void addArgumentsToDocument(
        final List<ArgumentDefinition> argumentDefinitions,
        final Document document,
        final Optional<Long> settingsServiceLastUpdateTimeMillisOpt)
            throws DocumentWorkerTransientException {
          
        // If processing a poison document (a document that a downstream worker has redirected
        // back to the workflow worker), the ArgumentsManager should not try to re-resolve the 
        // arguments again, but instead: 
        // 
        // 1. Trust that the CAF_WORKFLOW_SETTINGS on the document field are valid (after 
        //    performing some checks inside the isPoisonDocument method).
        // 2. Return without performing any resolving of arguments.
        if (PoisonMessageDetector.isPoisonDocument(document)) {
            return;
        }

        final Map<String, String> arguments = new HashMap<>();

        for(final ArgumentDefinition argumentDefinition : argumentDefinitions) {
            String value = null;
            if(argumentDefinition.getSources() != null){
                for(final ArgumentDefinition.Source source: argumentDefinition.getSources()) {
                    switch (source.getType()){
                        case CUSTOM_DATA: {
                            value = document.getCustomData(source.getName());
                            break;
                        }
                        case FIELD: {
                            final Field field = document.getField(source.getName());
                            if(field.hasValues()){
                                value = field.getStringValues().get(0);
                            }
                            break;
                        }
                        case SETTINGS_SERVICE: {
                            value = getFromSettingService(
                                source.getName(), source.getOptions(), document, settingsServiceLastUpdateTimeMillisOpt);
                            break;
                        }
                        default: {
                            throw new UnsupportedOperationException(String.format("Invalid source type [%s].",
                                    source.getType()));
                        }
                    }
                    if(!Strings.isNullOrEmpty(value)){
                        break;
                    }
                }
            }

            if(Strings.isNullOrEmpty(value) && !Strings.isNullOrEmpty(argumentDefinition.getDefaultValue())) {
                value = argumentDefinition.getDefaultValue();
            }

            if(!Strings.isNullOrEmpty(value)){
                arguments.put(argumentDefinition.getName(), value);
            }
        }

        document.getField("CAF_WORKFLOW_SETTINGS").set(gson.toJson(arguments));
    }

    private String getFromSettingService(
        final String name,
        final String options,
        final Document document,
        final Optional<Long> settingsServiceLastUpdateTimeMillisOpt)
            throws DocumentWorkerTransientException {

        final Pattern pattern = Pattern.compile("(?<prefix>[a-zA-Z-_.]*)%(?<type>f|cd):(?<name>[a-zA-Z-_.]*)%(?<suffix>[a-zA-Z-_.]*)");
        final List<String> scopes = new ArrayList<>();
        final List<String> priorities = new ArrayList<>();
        final String[] scopesToProcess = options.split(",");
        int priority = 1;
        for(final String scope:scopesToProcess) {
            final Matcher matcher = pattern.matcher(scope);
            if (matcher.matches()){
                final String prefix = matcher.group("prefix");
                final String type = matcher.group("type");
                final String fieldName = matcher.group("name");
                final String suffix = matcher.group("suffix");
                if (type.equals("f")){
                    final Field field = document.getField(fieldName);
                    if(field.hasValues()){
                        boolean fldScopeValueAdded = false;
                        final List<String> fldValues = field.getStringValues();
                        for (final String fldValue : fldValues) {
                            if (!Strings.isNullOrEmpty(fldValue)) {
                                fldScopeValueAdded = true;
                                scopes.add(String.format("%s%s%s", prefix, fldValue, suffix));
                                priorities.add(String.valueOf(priority));
                            }
                        }
                        if (fldScopeValueAdded) {
                            priority++;
                        }
                    }
                }
                else if (type.equals("cd")){
                    final String value = document.getCustomData(fieldName);
                    if (!Strings.isNullOrEmpty(value)) {
                        scopes.add(String.format("%s%s%s", prefix, value, suffix));
                        priorities.add(String.valueOf(priority));
                        priority++;
                    }
                }
            }
            else {
                scopes.add(scope);
                priorities.add(String.valueOf(priority));
                priority++;
            }
        }

        final ResolvedSetting resolvedSetting;
        try {
            if (settingsServiceLastUpdateTimeMillisOpt.isPresent()) {
                try {
                    okHttpClient.interceptors().add(FORCE_CACHE_REFRESH_CACHE_CONTROL_INTERCEPTOR);
                    resolvedSetting = settingsApi.getResolvedSetting(name, String.join(",", scopes), String.join(",", priorities));
                } finally {
                    okHttpClient.interceptors().removeIf(ForceCacheRefreshCacheControlInterceptor.class::isInstance);
                }
            } else {
                resolvedSetting = settingsApi.getResolvedSetting(name, String.join(",", scopes), String.join(",", priorities));
            }
            
        } catch (final ApiException e) {
            if(e.getCode()==404){
                LOG.warn(String.format("Setting [%s] was not found in the settings service.", name));
                return null;
            }
            throw new DocumentWorkerTransientException(e.getMessage());
        }
        if(resolvedSetting==null){
            return null;
        }
        return resolvedSetting.getValue();
    }

    @SuppressWarnings("unused")
    public void checkHealth() {
        try {
            okHttpClient.interceptors().add(FORCE_CACHE_REFRESH_CACHE_CONTROL_INTERCEPTOR);
            settingsApi.getSetting("healthcheck");
        }
        catch (final ApiException ex){
            if(ex.getCode()!=404){
                throw new RuntimeException(ex.getMessage(), ex);
            }
        } finally {
            okHttpClient.interceptors().removeIf(ForceCacheRefreshCacheControlInterceptor.class::isInstance);
        }
    }
}
