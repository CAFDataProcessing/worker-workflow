package com.github.cafdataprocessing.workflow;

import com.github.cafdataprocessing.workflow.model.SettingDefinition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import com.microfocus.darwin.settings.client.ResolvedSetting;
import com.microfocus.darwin.settings.client.SettingsApi;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SettingsManagerTests {

    @Test
    public void settingFromFieldTest() throws Exception {

        final List<SettingDefinition> settingDefinitions = getSettingsDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .documentBuilder()
                .withFields()
                    .addFieldValue("exampleField", "value of example field")
                .documentBuilder()
                .build();

        final SettingsManager settingsManager = new SettingsManager(settingsApi, "");
        settingsManager.applySettingsCustomData(settingDefinitions, document);

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> settingsPayload = gson.fromJson(
                document.getTask().getResponse().getCustomData().get("CAF_WORKFLOW_SETTINGS"), type);

        assertEquals("value of example field", settingsPayload.get("example"));
    }

    @Test
    public void settingFromTaskSettingTest() throws Exception {

        final List<SettingDefinition> settingDefinitions = getSettingsDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                    .add("TASK_SETTING_EXAMPLE", "value of task setting example field")
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        final SettingsManager settingsManager = new SettingsManager(settingsApi, "");
        settingsManager.applySettingsCustomData(settingDefinitions, document);

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> settingsPayload = gson.fromJson(
                document.getTask().getResponse().getCustomData().get("CAF_WORKFLOW_SETTINGS"), type);

        assertEquals("value of task setting example field", settingsPayload.get("example"));
    }

    @Test
    public void settingFromCustomDataTest() throws Exception {

        final List<SettingDefinition> settingDefinitions = getSettingsDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("exampleCustomData", "value of from custom data")
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        final SettingsManager settingsManager = new SettingsManager(settingsApi, "");
        settingsManager.applySettingsCustomData(settingDefinitions, document);

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> settingsPayload = gson.fromJson(
                document.getTask().getResponse().getCustomData().get("CAF_WORKFLOW_SETTINGS"), type);

        assertEquals("value of from custom data", settingsPayload.get("example"));
    }

    @Test
    public void settingFromSettingsServiceUsingCustomDataTest() throws Exception {

        final List<SettingDefinition> settingDefinitions = getSettingsDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("valueFromSettingsService");
        when(settingsApi.getResolvedSetting("exampleSetting", "repository-rId,tenantId-tId-some-suffix"))
                .thenReturn(resolvedSetting);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("repositoryId", "rId")
                .add("tenantId", "tId")
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        final SettingsManager settingsManager = new SettingsManager(settingsApi, "");
        settingsManager.applySettingsCustomData(settingDefinitions, document);

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> settingsPayload = gson.fromJson(
                document.getTask().getResponse().getCustomData().get("CAF_WORKFLOW_SETTINGS"), type);

        assertEquals("valueFromSettingsService", settingsPayload.get("example"));
    }

    @Test
    public void settingFromSettingsServiceUsingFieldTest() throws Exception {

        final List<SettingDefinition> settingDefinitions = getSettingsDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("valueFromSettingsService");
        when(settingsApi.getResolvedSetting("exampleSetting", "repository-rId,tenantId-tId-some-suffix"))
                .thenReturn(resolvedSetting);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("tenantId", "tId")
                .documentBuilder()
                .withFields()
                .addFieldValue("repositoryId", "rId")
                .documentBuilder()
                .build();

        final SettingsManager settingsManager = new SettingsManager(settingsApi, "");
        settingsManager.applySettingsCustomData(settingDefinitions, document);

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> settingsPayload = gson.fromJson(
                document.getTask().getResponse().getCustomData().get("CAF_WORKFLOW_SETTINGS"), type);

        assertEquals("valueFromSettingsService", settingsPayload.get("example"));
    }

    private List<SettingDefinition> getSettingsDefinitions() {
        final List<SettingDefinition> settingDefinitions = new ArrayList<>();
        SettingDefinition settingDefinition = new SettingDefinition();
        settingDefinition.setName("example");
        settingDefinition.setSources(new ArrayList<>());

        {
            final SettingDefinition.Source fieldSource = new SettingDefinition.Source();
            fieldSource.setName("exampleField");
            fieldSource.setType(SettingDefinition.SourceType.FIELD);
            settingDefinition.getSources().add(fieldSource);
        }

        {
            //Apollo product example where custom data gets prefixed with TASK_SETTING_
            final SettingDefinition.Source taskCustomDataSource = new SettingDefinition.Source();
            taskCustomDataSource.setName("TASK_SETTING_EXAMPLE");
            taskCustomDataSource.setType(SettingDefinition.SourceType.CUSTOM_DATA);
            settingDefinition.getSources().add(taskCustomDataSource);
        }

        {
            final SettingDefinition.Source customDataSource = new SettingDefinition.Source();
            customDataSource.setName("exampleCustomData");
            customDataSource.setType(SettingDefinition.SourceType.CUSTOM_DATA);
            settingDefinition.getSources().add(customDataSource);
        }

        {
            final SettingDefinition.Source settingsServiceSource = new SettingDefinition.Source();
            settingsServiceSource.setName("exampleSetting");
            settingsServiceSource.setType(SettingDefinition.SourceType.SETTINGS_SERVICE);
            settingsServiceSource
                    .setOptions("repository-%f:repositoryId%,repository-%cd:repositoryId%," +
                            "tenantId-%cd:tenantId%-some-suffix");
            settingDefinition.getSources().add(settingsServiceSource);
        }

        settingDefinitions.add(settingDefinition);
        return settingDefinitions;
    }

//    @Test
//    public void myTest() throws Exception {
//
//        final WorkflowWorkerConfiguration workflowWorkerConfiguration = new WorkflowWorkerConfiguration();
//        workflowWorkerConfiguration.setWorkflowsDirectory(Resources.getResource("workflow-worker-test").getPath());
//
//        final SettingsManager settingsManager = mock(SettingsManager.class);
//
//        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
//                .withReference("test-document")
//                .withCustomData()
//                    .add("workflowName", "sample-workflow")
//                .documentBuilder()
//                .withFields()
//                .documentBuilder()
//                .build();
//
//        final WorkflowWorker workflowWorker = new WorkflowWorker(document.getApplication(), workflowWorkerConfiguration,
//                settingsManager);
//
//        workflowWorker.processDocument(document);
//
//        assertTrue(failuresToString(document), document.getFailures().isEmpty());
//        assertEquals("", document.getCustomData("CAF_WORKFLOW_SETTINGS"));
//    }
//
//    private String failuresToString(final Document document){
//        final StringBuilder stringBuilder = new StringBuilder();
//        for(final Failure failure: document.getFailures()){
//            stringBuilder.append(failure.getFailureMessage());
//            stringBuilder.append("\n");
//        }
//        return stringBuilder.toString();
//    }
}
