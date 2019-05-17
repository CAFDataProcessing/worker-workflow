/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ArgumentsManager {

    private final static Logger LOG = LoggerFactory.getLogger(ArgumentsManager.class);

    private final Gson gson = new Gson();
    private final SettingsApi settingsApi;

    public ArgumentsManager(final String settingsServiceUrl)
    {
        this(new SettingsApi(), settingsServiceUrl);
    }

    public ArgumentsManager(final SettingsApi settingsApi, final String settingsServiceUrl){
        this.settingsApi = settingsApi;
        final ApiClient apiClient = new ApiClient();
        final OkHttpClient client = new OkHttpClient();
        client.interceptors().add(getCacheControlInterceptor());
        Objects.requireNonNull(settingsServiceUrl);
        apiClient.setBasePath(settingsServiceUrl);
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

    public void addArgumentsToDocument(final List<ArgumentDefinition> argumentDefinitions, final Document document)
            throws DocumentWorkerTransientException {

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
                            value = getFromSettingService(source.getName(), source.getOptions(), document);
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
        document.getTask().getResponse().getCustomData().put("CAF_WORKFLOW_SETTINGS",
                gson.toJson(arguments));

    }

    private String getFromSettingService(final String name, final String options, final Document document)
            throws DocumentWorkerTransientException {

        final Pattern pattern = Pattern.compile("(?<prefix>[a-zA-Z-_.]*)%(?<type>f|cd):(?<name>[a-zA-Z-_.]*)%(?<suffix>[a-zA-Z-_.]*)");
        final List<String> scopes = new ArrayList<>();
        final String[] scopesToProcess = options.split(",");

        for(final String scope:scopesToProcess) {
            final Matcher matcher = pattern.matcher(scope);
            if (matcher.matches()){
                String value = null;
                if (matcher.group("type").equals("f")){
                    final Field field = document.getField(matcher.group("name"));
                    if(field.hasValues()){
                        value = field.getStringValues().get(0);
                    }
                }
                else if (matcher.group("type").equals("cd")){
                    value = document.getCustomData(matcher.group("name"));
                }
                if (!Strings.isNullOrEmpty(value)){
                    scopes.add(String.format("%s%s%s",
                            matcher.group("prefix"),
                            value,
                            matcher.group("suffix")));
                }
            }
            else {
                scopes.add(scope);
            }
        }

        final ResolvedSetting resolvedSetting;
        try {
            resolvedSetting = settingsApi.getResolvedSetting(name, String.join(",", scopes));
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
            final Setting setting = settingsApi.getSetting("healthcheck");
        }
        catch (final ApiException ex){
            if(ex.getCode()!=404){
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
    }

}
