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

public class SettingsManagerTest {

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
}
