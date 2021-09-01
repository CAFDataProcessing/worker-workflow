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
import com.google.common.base.Joiner;
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
import java.net.URI;
import java.nio.file.Files;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class ArgumentsManager {

    private final static Logger LOG = LoggerFactory.getLogger(ArgumentsManager.class);
    private static final int SETTINGS_SERVICE_CACHE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MiB
    private static final String SETTINGS_SERVICE_CACHE_TEMP_DIRECTORY_PREFIX = "settings-service-http-cache";
    private static final Interceptor FORCE_CACHE_REFRESH_CACHE_CONTROL_INTERCEPTOR = new ForceCacheRefreshCacheControlInterceptor();
    private static final Map<String, Long> SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP = new HashMap<>();

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
        okHttpClient.networkInterceptors().add(new RecordLastAccessTimeInterceptor());
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
            final Request request = chain.request();
            if (shouldCacheSettingsServiceResponse(request)) {
                final Response response = chain.proceed(request);
                final CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(5, TimeUnit.MINUTES)
                    .build();

                return response.newBuilder()
                    .header("Cache-Control", cacheControl.toString())
                    .build();
            } else {
                return chain.proceed(request); 
            }
        };
    }

    // Important that this is added as an application interceptor (as opposed to a network interceptor), otherwise it has no affect.
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

    // Important that this is added as a network interceptor (as opposed to an application interceptor), to ensure it won't be invoked
    // for cached responses.
    private final static class RecordLastAccessTimeInterceptor implements Interceptor
    {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException
        {
            final Request request = chain.request();
            if (shouldCacheSettingsServiceResponse(request)) {
                LOG.warn("RORY updating map. Before: " + Arrays.toString(SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP.entrySet().toArray()));
                //   request.uri()
                LOG.warn("RORY URL PATH " + request.url().getPath());
                LOG.warn("RORY uri PATH " + request.uri().getPath());
                LOG.warn("RORY uri raw path " + request.uri().getRawPath());
                LOG.warn("RORY httpurl encoded path " + request.httpUrl().encodedPath());

                /**
                 * RORY URL PATH /settings/ee.grammarmap/resolved RORY uri PATH /settings/ee.grammarmap/resolved RORY uri raw path
                 * /settings/ee.grammarmap/resolved RORY httpurl encoded path /settings/ee.grammarmap/resolved
                 *
                 */
                LOG.warn("RORY URL query " + request.url().getQuery());
                LOG.warn("RORY uri query " + request.uri().getQuery());
                LOG.warn("RORY uri raw query " + request.uri().getRawQuery());
                LOG.warn("RORY httpurl encoded query " + request.httpUrl().encodedQuery());

                LOG.warn("RORY URL toString " + request.url().toString());
                LOG.warn("RORY urlString " + request.urlString());

                final URI uri = request.uri();
                final String key = String.format("%s?%s", uri.getRawPath(), uri.getRawQuery());
                SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP.put(key, Instant.now().toEpochMilli());
                LOG.warn("RORY updated map: After: " + Arrays.toString(SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP.entrySet().toArray()));
                return chain.proceed(request);
            } else {
                return chain.proceed(request);
            }
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
            if (shouldForceSettingsServiceCacheRefresh(name, scopes, priorities, settingsServiceLastUpdateTimeMillisOpt)) {
                 LOG.warn(String.format("RORY number of interceptors before forcing cache refresh" + okHttpClient.interceptors().size()));
                 LOG.warn(String.format("RORY forcing settings service cache refresh."));
                    resolvedSetting = forceSettingsServiceCacheRefresh(
                        () -> settingsApi.getResolvedSetting(name, String.join(",", scopes), String.join(",", priorities)));
                LOG.warn(String.format("RORY number of interceptors after forcing cache refresh" + okHttpClient.interceptors().size()));
            } else {
                LOG.warn(String.format("RORY NOT forcing settings service cache refresh."));
                LOG.warn(String.format("RORY number of interceptors before NOT forcing cache refresh" + okHttpClient.interceptors().size()));
                resolvedSetting = settingsApi.getResolvedSetting(name, String.join(",", scopes), String.join(",", priorities));
                LOG.warn(String.format("RORY number of interceptors after NOT forcing cache refresh" + okHttpClient.interceptors().size()));
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
            settingsApi.getSetting("healthcheck");
        }
        catch (final ApiException ex){
            if(ex.getCode()!=404){
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
    }

    private static boolean shouldCacheSettingsServiceResponse(final Request request)
    {
        return !request.urlString().contains("healthcheck");
    }

    private static boolean shouldForceSettingsServiceCacheRefresh(
        final String settingName,
        final List<String> scopes,
        final List<String> priorities,
        final Optional<Long> settingsServiceLastUpdateTimeMillisOpt)
    {
        if (!settingsServiceLastUpdateTimeMillisOpt.isPresent()) {
            return false;
        }
        final Long settingsServiceLastUpdateTimeMillis = settingsServiceLastUpdateTimeMillisOpt.get();
        final String scopesUrlQueryParam = (scopes != null && !scopes.isEmpty()) 
            ? "scopes=" + String.join(",", scopes)
            : null;
        final String prioritiesUrlQueryParam = (priorities != null && !priorities.isEmpty())
            ? "priorities=" + String.join(",", priorities)
            : null;
        final String urlQueryParams = Joiner.on("&").skipNulls().join(
            scopesUrlQueryParam,
            prioritiesUrlQueryParam
        );
            
            
        LOG.warn("RORY in new method urlQueryParams " + urlQueryParams);
        
        final String key = urlQueryParams != null ? 
            String.format("/settings/%s/resolved?%s", settingName, urlQueryParams) :
            String.format("/settings/%s/resolved", settingName);
        LOG.warn("RORY in new method key " + key);
        
        final Long lastAccessTime = SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP.get(key);
        if (lastAccessTime == null) {
            LOG.warn("RORY in new map does not contain key " + key);
            return false;
        }
        LOG.warn("RORY in new map does contains key " + key + " + with value " + lastAccessTime);
        if (settingsServiceLastUpdateTimeMillisOpt.get() > lastAccessTime) {
            LOG.warn("RORY returning true " + settingsServiceLastUpdateTimeMillis + " > " + lastAccessTime);
            return true;
        }  else {
            LOG.warn("RORY returning false " + settingsServiceLastUpdateTimeMillis + " > " + lastAccessTime);
            return false;
        }
        

//            .map(s -> "?" + s)
//            .orElse("");
    }

    private ResolvedSetting forceSettingsServiceCacheRefresh(final SettingsServiceApiCall settingsServiceApiCall) throws ApiException
    {
        try {
            okHttpClient.interceptors().add(FORCE_CACHE_REFRESH_CACHE_CONTROL_INTERCEPTOR);
            return settingsServiceApiCall.call();
        } finally {
            okHttpClient.interceptors().removeIf(ForceCacheRefreshCacheControlInterceptor.class::isInstance);
        }
    }

    @FunctionalInterface
    private interface SettingsServiceApiCall
    {
        public ResolvedSetting call() throws ApiException;
    }
}
