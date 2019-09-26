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

import com.github.cafdataprocessing.workflow.exceptions.UnexpectedCafWorkflowSettingException;
import com.github.cafdataprocessing.workflow.model.ArgumentDefinition;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Field;
import com.microfocus.darwin.settings.client.*;
import com.squareup.okhttp.*;
import java.lang.reflect.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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
            throws DocumentWorkerTransientException, UnexpectedCafWorkflowSettingException {
          
        // If processing a poison document (a document that a downstream worker has redirected
        // back to the workflow worker), the ArgumentsManager should not try to re-resolve the 
        // arguments again, but instead: 
        // 
        // 1. Trust that the CAF_WORKFLOW_SETTINGS on the document field are valid (after 
        //    performing some checks).
        // 2. Copy the CAF_WORKFLOW_SETTINGS from the  document field into the custom data of the 
        //    document task response.
        // 3. Return without performing any resolving of arguments.
        if (isPoisonDocument(document)) {
            final String cafWorkflowSettingsJson = document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().get(0);
            validateExistingCafWorkflowSettings(cafWorkflowSettingsJson, argumentDefinitions);
            document.getTask().getResponse().getCustomData().put("CAF_WORKFLOW_SETTINGS", cafWorkflowSettingsJson);
            
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
    
    private boolean isPoisonDocument(final Document document) {
        // A poison document is a document that a downstream worker has redirected back
        // to the workflow worker. A document is considered poison if:
        //
        // 1. It has a a non-empty CAF_WORKFLOW_SETTINGS field.
        //
        // 2. It does NOT have a 'tenantId' inside it's custom data.
        //
        // Point (2) is important, it's not enough to just check for a non-empty 
        // CAF_WORKFLOW_SETTINGS field, since that makes it possible for a rogue agent to
        // stage documents that already contain CAF_WORKFLOW_SETTINGS field, which could
        // possibly be used to write to, or delete from, another tenant's index.
        //
        // The tenantId that is passed in custom data cannot be controlled by a rogue agent 
        // and is therefore trustworthy, so if that is checked as well, it gives some 
        // confidence that this is really a poison document, and not a document staged by
        // a rogue agent, and as such we can safely use the CAF_WORKFLOW_SETTINGS present
        // in the document and trust that they are valid.
        return document.getField("CAF_WORKFLOW_SETTINGS").hasValues() && 
            (document.getTask().getCustomData("tenantId") == null);
    }

    private void validateExistingCafWorkflowSettings(final String cafWorkflowSettingsJson,
                                                     final List<ArgumentDefinition> argumentDefinitions)
        throws UnexpectedCafWorkflowSettingException
    {
        // Extract the name of each setting from the existing CAF_WORKFLOW_SETTINGS.
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> cafWorkflowSettings = gson.fromJson(cafWorkflowSettingsJson, type);
        final Set<String> cafWorkflowSettingsNames = cafWorkflowSettings.keySet();
        
        // Extract the name of each argument from the workflow arguments.
        final List<String> argumentDefintionNames = argumentDefinitions
            .stream()
            .map(argumentDefintion -> argumentDefintion.getName())
            .collect(Collectors.toList());
        
        // Ensure that each setting that exists in the existing CAF_WORKFLOW_SETTINGS
        // also exists in the workflow arguments; this guards against processing a 
        // document that contains unexpected settings.
        for (final String cafWorkflowSettingName : cafWorkflowSettingsNames) {
            if (!argumentDefintionNames.contains(cafWorkflowSettingName)) {
                throw new UnexpectedCafWorkflowSettingException(cafWorkflowSettingName, argumentDefintionNames);
            }
        }
    }   
}
