/*
 * Copyright 2017-2024 Open Text.
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
import com.github.cafdataprocessing.workflow.restclients.settings_service.api.SettingsApi;
import com.github.cafdataprocessing.workflow.restclients.settings_service.model.ResolvedSetting;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArgumentsManagerTest {
    private static final Logger LOG = LoggerFactory.getLogger(ArgumentsManagerTest.class);

    @Test
    public void argumentFromFieldTest() throws Exception {

        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .documentBuilder()
                .withFields()
                    .addFieldValue("exampleField", "value of example field")
                .documentBuilder()
                .build();

        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);

        assertEquals("value of example field", arguments.get("example"));
        assertEquals("A default value", arguments.get("shouldDefault"));
    }

    @Test
    public void argumentFromTaskSettingTest() throws Exception {

        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .add("TASK_SETTING_EXAMPLE", "value of task setting example field")
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);

        assertEquals("value of task setting example field", arguments.get("example"));
    }

    @Test
    public void argumentFromCustomDataTest() throws Exception {

        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .add("exampleCustomData", "value of from custom data")
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);

        assertEquals("value of from custom data", arguments.get("example"));
    }

    @Test
    public void argumentFromSettingsServiceUsingCustomDataTest() throws Exception {

        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("valueFromSettingsService");
        when(settingsApi.getResolvedSetting("exampleSetting", "repository-rId,tenantId-tId-some-suffix", "1,2"))
                .thenReturn(resolvedSetting);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .add("repositoryId", "rId")
                .add("tenantId", "tId")
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);

        assertEquals("valueFromSettingsService", arguments.get("example"));
    }

    @Test
    public void argumentFromSettingsServiceUsingFieldTest() throws Exception {

        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("valueFromSettingsService");
        when(settingsApi.getResolvedSetting("exampleSetting", "repository-rId,tenantId-tId-some-suffix", "1,2"))
                .thenReturn(resolvedSetting);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .add("tenantId", "tId")
                .documentBuilder()
                .withFields()
                .addFieldValue("repositoryId", "rId")
                .documentBuilder()
                .build();

        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);

        assertEquals("valueFromSettingsService", arguments.get("example"));
    }

    @Test
    public void argumentFromSettingsServiceUsingMVFieldTest() throws Exception {
        LOG.info("Running argumentFromSettingsServiceUsingMVFieldTest...");
        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("valueFromSettingsService");
        when(settingsApi.getResolvedSetting("exampleSetting", "repository-rId1,repository-rId2,tenantId-tId-some-suffix", "1,1,2"))
                .thenReturn(resolvedSetting);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .add("tenantId", "tId")
                .documentBuilder()
                .withFields()
                .addFieldValue("repositoryId", "rId1")
                .addFieldValue("repositoryId", "rId2")
                .documentBuilder()
                .build();
        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);
        LOG.info("argumentFromSettingsServiceUsingMVFieldTest arguments: {}", arguments);
        assertEquals("valueFromSettingsService", arguments.get("example"));
    }

    @Test
    public void argumentFromSettingsServiceUsingOnlyCVFieldTest() throws Exception {
        LOG.info("Running argumentFromSettingsServiceUsingOnlyCVFieldTest...");
        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("testValueFromSettingsService");
        when(settingsApi.getResolvedSetting("exampleOnlyCustomData", "shouldIdx-true,tenantId-tId-some-suffix", "1,2"))
                .thenReturn(resolvedSetting);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .add("tenantId", "tId")
                .add("shouldIdxCD", "true")
                .documentBuilder()
                .withFields()
                .addFieldValue("wkbkId", "wId1")
                .addFieldValue("wkbkId", "wId2")
                .documentBuilder()
                .build();
        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);
        LOG.info("argumentFromSettingsServiceUsingOnlyCVFieldTest arguments: {}", arguments);
        assertEquals("testValueFromSettingsService", arguments.get("example"));
    }

    @Test
    public void argumentFromSettingsServiceUsingCVThenMVFieldTest() throws Exception {
        LOG.info("Running argumentFromSettingsServiceUsingCVThenMVFieldTest...");
        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("testCDMVValueFromSettingsService");
        when(settingsApi.getResolvedSetting("exampleCustomDataAndFld",
            "shouldIdx-true,tenantId-tId-some-suffix,workbook-wId1-somewkbk-suffix,workbook-wId2-somewkbk-suffix,case-cId", "1,2,3,3,4"))
        .thenReturn(resolvedSetting);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .add("tenantId", "tId")
                .add("shouldIdxCD", "true")
                .documentBuilder()
                .withFields()
                .addFieldValue("wkbkId", "wId1")
                .addFieldValue("wkbkId", "wId2")
                .addFieldValue("caseId", "cId")
                .documentBuilder()
                .build();
        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);
        LOG.info("argumentFromSettingsServiceUsingCVThenMVFieldTest arguments: {}", arguments);
        assertEquals("testCDMVValueFromSettingsService", arguments.get("example"));
    }

    @Test
    public void argumentFromSettingsServiceUsingPreSetValueCVMVFieldTest() throws Exception {
        LOG.info("Running argumentFromSettingsServiceUsingPreSetValueCVMVFieldTest...");
        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("testPreValCDMVValueFromSettingsService");
        when(settingsApi.getResolvedSetting("exampleValCustomDataAndFld",
            "not-a-repo,shouldIdx-true,tenantId-tId-some-suffix,workbook-wId1-somewkbk-suffix,workbook-wId2-somewkbk-suffix,case-cId",
            "1,2,3,4,4,5"))
        .thenReturn(resolvedSetting);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .add("tenantId", "tId")
                .add("shouldIdxCD", "true")
                .documentBuilder()
                .withFields()
                .addFieldValue("wkbkId", "wId1")
                .addFieldValue("wkbkId", "wId2")
                .addFieldValue("caseId", "cId")
                .documentBuilder()
                .build();
        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);
        LOG.info("argumentFromSettingsServiceUsingPreSetValueCVMVFieldTest arguments: {}", arguments);
        assertEquals("testPreValCDMVValueFromSettingsService", arguments.get("example"));
    }

    @Test
    public void argumentFromSettingsServiceUsingPreSetOnlyTest() throws Exception {
        LOG.info("Running argumentFromSettingsServiceUsingPreSetOnlyTest...");
        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("testPreValFromSettingsService");
        when(settingsApi.getResolvedSetting("examplePreSetVal",
            "not-a-repo1,not-a-repo2",
            "1,2"))
        .thenReturn(resolvedSetting);

        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .add("tenantId", "tId")
                .add("shouldIdxCD", "true")
                .documentBuilder()
                .withFields()
                .addFieldValue("wkbkId", "wId1")
                .addFieldValue("wkbkId", "wId2")
                .addFieldValue("caseId", "cId")
                .documentBuilder()
                .build();
        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> arguments = gson.fromJson(
                document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst().get(), type);
        LOG.info("argumentFromSettingsServiceUsingPreSetOnlyTest arguments: {}", arguments);
        assertEquals("testPreValFromSettingsService", arguments.get("example"));
    }

    @Test
    public void poisonDocumentHandlingTest() throws Exception {
        
        // If processing a poison document (a document that a downstream worker has redirected
        // back to the workflow worker), the ArgumentsManager should not try to re-resolve the 
        // arguments again, but instead: 
        // 
        // 1. Trust that the CAF_WORKFLOW_SETTINGS on the document field are valid.
        // 2. Return without performing any resolving of arguments

        final List<ArgumentDefinition> argumentDefinitions = getArgumentDefinitions();

        final SettingsApi settingsApi = mock(SettingsApi.class);
        
        final ResolvedSetting resolvedSetting = new ResolvedSetting();
        resolvedSetting.setValue("valueFromSettingsService");
        when(settingsApi.getResolvedSetting("exampleSetting", "repository-rId,tenantId-tId-some-suffix", "1,2"))
                .thenReturn(resolvedSetting);

        final Gson gson = new Gson();
        
        final Map<String, String> alreadyResolvedArguments = 
            Collections.singletonMap("example", "valueFromCafWorkflowSettings");
        
        final String alreadyResolvedArgumentsJson = gson.toJson(alreadyResolvedArguments);

        // This document represents a poison document because it has:
        //
        // 1. A non-empty CAF_WORKFLOW_SETTINGS field on the document.
        //
        // 2. No 'workflowName' custom data value.
        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
                .withFields()
                .addFieldValue("repositoryId", "rId")
                .addFieldValue("CAF_WORKFLOW_SETTINGS", alreadyResolvedArgumentsJson)
                .documentBuilder()
                .build();

        final ArgumentsManager argumentsManager = new ArgumentsManager(settingsApi, settingsApi, "");
        argumentsManager.addArgumentsToDocument(argumentDefinitions, document, Optional.empty());
  
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> cafWorkflowSettings = gson.fromJson(
            document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().get(0), type);

        assertEquals("valueFromCafWorkflowSettings", cafWorkflowSettings.get("example"));
    }

    private List<ArgumentDefinition> getArgumentDefinitions() {
        final List<ArgumentDefinition> argumentDefinitions = new ArrayList<>();
        ArgumentDefinition argumentDefinition = new ArgumentDefinition();
        argumentDefinition.setName("example");
        argumentDefinition.setSources(new ArrayList<>());

        {
            final ArgumentDefinition.Source fieldSource = new ArgumentDefinition.Source();
            fieldSource.setName("exampleField");
            fieldSource.setType(ArgumentDefinition.SourceType.FIELD);
            argumentDefinition.getSources().add(fieldSource);
        }

        {
            //Apollo product example where custom data gets prefixed with TASK_SETTING_
            final ArgumentDefinition.Source taskCustomDataSource = new ArgumentDefinition.Source();
            taskCustomDataSource.setName("TASK_SETTING_EXAMPLE");
            taskCustomDataSource.setType(ArgumentDefinition.SourceType.CUSTOM_DATA);
            argumentDefinition.getSources().add(taskCustomDataSource);
        }

        {
            final ArgumentDefinition.Source customDataSource = new ArgumentDefinition.Source();
            customDataSource.setName("exampleCustomData");
            customDataSource.setType(ArgumentDefinition.SourceType.CUSTOM_DATA);
            argumentDefinition.getSources().add(customDataSource);
        }

        {
            final ArgumentDefinition.Source settingsServiceSource = new ArgumentDefinition.Source();
            settingsServiceSource.setName("exampleSetting");
            settingsServiceSource.setType(ArgumentDefinition.SourceType.SETTINGS_SERVICE);
            settingsServiceSource
                    .setOptions("repository-%f:repositoryId%,repository-%cd:repositoryId%," +
                            "tenantId-%cd:tenantId%-some-suffix");
            argumentDefinition.getSources().add(settingsServiceSource);
        }

        {
            final ArgumentDefinition.Source settingsServiceSource = new ArgumentDefinition.Source();
            settingsServiceSource.setName("exampleOnlyCustomData");
            settingsServiceSource.setType(ArgumentDefinition.SourceType.SETTINGS_SERVICE);
            settingsServiceSource
                    .setOptions("shouldIdx-%cd:shouldIdxCD%," +
                            "tenantId-%cd:tenantId%-some-suffix");
            argumentDefinition.getSources().add(settingsServiceSource);
        }

        {
            final ArgumentDefinition.Source settingsServiceSource = new ArgumentDefinition.Source();
            settingsServiceSource.setName("exampleCustomDataAndFld");
            settingsServiceSource.setType(ArgumentDefinition.SourceType.SETTINGS_SERVICE);
            settingsServiceSource
                    .setOptions("shouldIdx-%cd:shouldIdxCD%," +
                            "tenantId-%cd:tenantId%-some-suffix," +
                            "workbook-%f:wkbkId%-somewkbk-suffix," +
                            "case-%f:caseId%");
            argumentDefinition.getSources().add(settingsServiceSource);
        }

        {
            final ArgumentDefinition.Source settingsServiceSource = new ArgumentDefinition.Source();
            settingsServiceSource.setName("exampleValCustomDataAndFld");
            settingsServiceSource.setType(ArgumentDefinition.SourceType.SETTINGS_SERVICE);
            settingsServiceSource
                    .setOptions("not-a-repo," +
                            "shouldIdx-%cd:shouldIdxCD%," +
                            "tenantId-%cd:tenantId%-some-suffix," +
                            "workbook-%f:wkbkId%-somewkbk-suffix," +
                            "case-%f:caseId%");
            argumentDefinition.getSources().add(settingsServiceSource);
        }

        {
            final ArgumentDefinition.Source settingsServiceSource = new ArgumentDefinition.Source();
            settingsServiceSource.setName("examplePreSetVal");
            settingsServiceSource.setType(ArgumentDefinition.SourceType.SETTINGS_SERVICE);
            settingsServiceSource
                    .setOptions("not-a-repo1,not-a-repo2");
            argumentDefinition.getSources().add(settingsServiceSource);
        }

        argumentDefinitions.add(argumentDefinition);

        ArgumentDefinition argumentDefinitionWithDefault = new ArgumentDefinition();
        argumentDefinitionWithDefault.setName("shouldDefault");
        argumentDefinitionWithDefault.setDefaultValue("A default value");

        argumentDefinitions.add(argumentDefinitionWithDefault);

        return argumentDefinitions;
    }
}
