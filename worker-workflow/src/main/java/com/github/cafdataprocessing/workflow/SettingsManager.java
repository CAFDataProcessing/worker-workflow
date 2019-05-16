package com.github.cafdataprocessing.workflow;

import com.github.cafdataprocessing.workflow.model.RepoConfigSource;
import com.github.cafdataprocessing.workflow.model.WorkflowSettings;
import com.google.gson.Gson;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.FieldValue;
import com.microfocus.darwin.settings.client.*;
import com.squareup.okhttp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class SettingsManager {

    private final static Logger LOG = LoggerFactory.getLogger(SettingsManager.class);
    private final Gson gson = new Gson();
    private final SettingsApi settingsApi;

    public SettingsManager()
    {
        this(new SettingsApi());
    }

    public SettingsManager(final SettingsApi settingsApi){
        this.settingsApi = settingsApi;
        final ApiClient apiClient = new ApiClient();
        final OkHttpClient client = new OkHttpClient();
        client.interceptors().add(getCacheControlInterceptor());
        final String settingsServiceUrl = System.getenv("CAF_SETTINGS_SERVICE_URL");
        Objects.requireNonNull(settingsServiceUrl);
        apiClient.setBasePath(settingsServiceUrl);
        settingsApi.setApiClient(apiClient);
    }

    private Interceptor getCacheControlInterceptor() {
        return chain -> {
            Response response = chain.proceed(chain.request());
            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(5, TimeUnit.MINUTES)
                    .build();

            return response.newBuilder()
                    .header("Cache-Control", cacheControl.toString())
                    .build();
        };
    }

    public void applySettingsCustomData(final WorkflowSettings workflowSettings, final Document document){
        final Map<String, String> resolvedSettings = new HashMap<>();

        addTaskSettings(resolvedSettings, document, workflowSettings);
        addTenantSettings(resolvedSettings, document, workflowSettings);
        addRepositorySettings(resolvedSettings, document, workflowSettings);

        document.getField("CAF_WORKFLOW_SETTINGS").set(gson.toJson(resolvedSettings));
        document.getTask().getResponse().getCustomData().put("CAF_WORKFLOW_SETTINGS",
                gson.toJson(resolvedSettings));

    }

    @SuppressWarnings("unused")
    public void checkHealth() {
        try {
            final Setting setting = settingsApi.getSetting("healthcheck");
        }
        catch (ApiException ex){
            if(ex.getCode()!=404){
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
    }

    private void addTaskSettings(final Map<String, String> resolvedSettings, final Document document,
                                 final WorkflowSettings workflowSettings) {

        for(final String taskSetting: workflowSettings.getTaskSettings()) {
            final String taskSettingValue = document.getCustomData("TASK_SETTING_" +
                    taskSetting.toUpperCase(Locale.US));
            resolvedSettings.put(taskSetting, taskSettingValue);
        }
    }

    private void addTenantSettings(final Map<String, String> resolvedSettings, final Document document,
                                   final WorkflowSettings workflowSettings) {

        final String tenantId = document.getCustomData("tenantId");
        Objects.requireNonNull(tenantId);
        final String tenantScope = String.format("tenant-%s", tenantId);

        for(final String tenantSetting: workflowSettings.getTenantSettings()){
            final ResolvedSetting tenantSettingValue;
            try {
                tenantSettingValue = settingsApi.getResolvedSetting(tenantSetting, tenantScope);
            } catch (ApiException ex) {
                //TODO
                throw new RuntimeException(ex);
            }
            resolvedSettings.put(tenantSetting, tenantSettingValue.getValue());
        }
    }

    private void addRepositorySettings(final Map<String, String> resolvedSettings, final Document document,
                                       final WorkflowSettings workflowSettings) {

        final String tenantId = document.getCustomData("tenantId");
        Objects.requireNonNull(tenantId);
        final String tenantScope = String.format("tenant-%s", tenantId);


        for(final Map.Entry<String, RepoConfigSource> entry: workflowSettings.getRepositorySettings().entrySet()){
            final RepoConfigSource repoConfigSource = entry.getValue();
            final String repositoryId;
            switch (repoConfigSource.getSource()){
                case FIELD: {
                    final FieldValue fieldValue = document.getField(repoConfigSource.getKey()).getValues().stream()
                            .findFirst().orElseThrow(()
                                    -> new RuntimeException("Unable to obtain repository id from document field for config "
                                    + repoConfigSource.getKey()));
                    repositoryId = fieldValue.getStringValue();
                    break;
                }
                case CUSTOMDATA: {
                    repositoryId = document.getCustomData(repoConfigSource.getKey());
                    if (repositoryId == null) {
                        throw new RuntimeException("Unable to obtain repository id from customdata for config "
                                + repoConfigSource.getKey());
                    }
                    break;
                }
                default: {
                    throw new RuntimeException(String.format("Unsupported source [%s].", repoConfigSource.getSource()));
                }
            }

            final String repositoryScope = String.format("repository-%s", repositoryId);
            final ResolvedSetting resolvedSetting;
            try {
                resolvedSetting = settingsApi.getResolvedSetting(entry.getKey(),
                        String.join(",", new String[]{repositoryScope, tenantScope}));
            } catch (ApiException ex) {
                //TODO
                throw new RuntimeException("Problem getting setting.");
            }
            resolvedSettings.put(entry.getKey(), resolvedSetting.getValue());
        }
    }

}