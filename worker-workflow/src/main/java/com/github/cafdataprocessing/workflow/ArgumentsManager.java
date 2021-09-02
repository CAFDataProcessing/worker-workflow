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
import net.jodah.expiringmap.ExpiringMap;

public class ArgumentsManager {

    private final static Logger LOG = LoggerFactory.getLogger(ArgumentsManager.class);
    private static final int SETTINGS_SERVICE_CACHE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MiB
    private static final String SETTINGS_SERVICE_CACHE_TEMP_DIRECTORY_PREFIX = "settings-service-http-cache";
    private static final int SETTINGS_SERVICE_CACHE_EXPIRATION_TIME_MINUTES = 5;
    private static final Interceptor FORCE_CACHE_REFRESH_CACHE_CONTROL_INTERCEPTOR = new ForceCacheRefreshCacheControlInterceptor();
    private static final Map<SettingsServiceLastAccessTimeMapKey, Long> SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP
        = ExpiringMap
            .builder()
            .expiration(SETTINGS_SERVICE_CACHE_EXPIRATION_TIME_MINUTES, TimeUnit.MINUTES)
            .build();

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
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(settingsServiceUrl);
        apiClient.setHttpClient(okHttpClient);
        settingsApi.setApiClient(apiClient);
    }

    private Interceptor getCacheControlInterceptor() {
        return chain -> {
            final Request request = chain.request();
            if (isCacheableRequest(request)) {
                final Response response = chain.proceed(request);
                final CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(SETTINGS_SERVICE_CACHE_EXPIRATION_TIME_MINUTES, TimeUnit.MINUTES)
                    .build();

                return response.newBuilder()
                    .header("Cache-Control", cacheControl.toString())
                    .build();
            } else {
                return chain.proceed(request); 
            }
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

    private final static class RecordLastAccessTimeInterceptor implements Interceptor
    {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException
        {
            final Request request = chain.request();
            final Response response = chain.proceed(request);
            if (isCacheableRequest(request) && response.isSuccessful()) {
                final SettingsServiceLastAccessTimeMapKey key = SettingsServiceLastAccessTimeMapKey.from(request.uri());
                final long now = Instant.now().toEpochMilli();
                SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP.put(key, now);
                LOG.debug(String.format(
                    "Recorded last access time for: %s as: %s. Size of SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP is now: %s",
                    key, now, SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP.size()));
            }
            return response;
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
            if (shouldForceCacheRefresh(name, scopes, priorities, settingsServiceLastUpdateTimeMillisOpt)) {
                resolvedSetting = forceCacheRefresh(
                    () -> settingsApi.getResolvedSetting(name, String.join(",", scopes), String.join(",", priorities)));
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
            settingsApi.getSetting("healthcheck");
        }
        catch (final ApiException ex){
            if(ex.getCode()!=404){
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
    }

    private static boolean isCacheableRequest(final Request request)
    {
        return !request.urlString().contains("healthcheck");
    }

    private static boolean shouldForceCacheRefresh(
        final String settingName,
        final List<String> scopes,
        final List<String> priorities,
        final Optional<Long> settingsServiceLastUpdateTimeMillisOpt)
    {
        if (!settingsServiceLastUpdateTimeMillisOpt.isPresent()) {
            return false;
        }
        final SettingsServiceLastAccessTimeMapKey key = SettingsServiceLastAccessTimeMapKey.from(settingName, scopes, priorities);
        final Long settingsServiceLastAccessTimeMillis = SETTINGS_SERVICE_LAST_ACCESS_TIME_MAP.get(key);
        if (settingsServiceLastAccessTimeMillis == null) {
            return true;
        }
        if (settingsServiceLastUpdateTimeMillisOpt.get() > settingsServiceLastAccessTimeMillis) {
            LOG.debug(String.format("Forcing cache refresh for: %s because the last update time: %s is greater than "
                + "the last access time: %s", key, settingsServiceLastUpdateTimeMillisOpt.get(), settingsServiceLastAccessTimeMillis));
            return true;
        }  else {
            LOG.debug(String.format("NOT forcing cache refresh for: %s because the last update time: %s is NOT greater "
                + "than the last access time: %s", key, settingsServiceLastUpdateTimeMillisOpt.get(),
                settingsServiceLastAccessTimeMillis));
            return false;
        }
    }

    private ResolvedSetting forceCacheRefresh(final SettingsServiceApiCall settingsServiceApiCall) throws ApiException
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

    private static final class SettingsServiceLastAccessTimeMapKey
    {
        private final String key;

        private SettingsServiceLastAccessTimeMapKey(final String key)
        {
            this.key = key;
        }

        public static SettingsServiceLastAccessTimeMapKey from(final URI uri)
        {
            final String key = String.format("%s?%s", uri.getPath(), uri.getQuery());
            return new SettingsServiceLastAccessTimeMapKey(key);
        }

        public static SettingsServiceLastAccessTimeMapKey from(
            final String settingName,
            final List<String> scopes,
            final List<String> priorities)
        {
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
            final String key = urlQueryParams != null
                ? String.format("/settings/%s/resolved?%s", settingName, urlQueryParams)
                : String.format("/settings/%s/resolved", settingName);
            return new SettingsServiceLastAccessTimeMapKey(key);
        }

        @Override
        public int hashCode()
        {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.key);
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SettingsServiceLastAccessTimeMapKey other = (SettingsServiceLastAccessTimeMapKey) obj;
            return Objects.equals(this.key, other.key);
        }

        @Override
        public String toString()
        {
            return key;
        }
    }
}
