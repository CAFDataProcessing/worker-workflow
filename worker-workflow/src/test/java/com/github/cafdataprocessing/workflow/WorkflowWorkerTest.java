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

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hpe.caf.worker.document.model.*;
import com.hpe.caf.worker.document.testing.DocumentBuilder;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.hpe.caf.worker.document.testing.TestServices;
import com.microfocus.darwin.settings.client.SettingsApi;
import org.apache.commons.io.FileUtils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class WorkflowWorkerTest
{
    private WorkflowWorkerConfiguration workflowWorkerConfiguration;
    private DocumentBuilder documentBuilder;
    private SettingsManager settingsManager ;

    @Before
    public void before() {
        workflowWorkerConfiguration = new WorkflowWorkerConfiguration();
        workflowWorkerConfiguration.setWorkflowsDirectory(
                Resources.getResource("workflow-worker-test").getPath());
        workflowWorkerConfiguration.setSettingsServiceUrl("");

        final TestServices testServices = TestServices.createDefault();
        documentBuilder = DocumentBuilder.configure().withServices(testServices);

        final SettingsApi settingsApi = mock(SettingsApi.class);

        settingsManager = new SettingsManager(settingsApi,
                workflowWorkerConfiguration.getSettingsServiceUrl());
    }


    @Test
    public void executeScriptTest() throws Exception {

        final Document document = documentBuilder
                .withCustomData()
                .add("workflowName", "sample-workflow")
                .documentBuilder()
                .withFields()
                .addFieldValue("example", "value from field")
                .documentBuilder()
                .build();

        final WorkflowWorker workflowWorker = new WorkflowWorker(
                workflowWorkerConfiguration,
                new WorkflowManager(document.getApplication(), workflowWorkerConfiguration.getWorkflowsDirectory()),
                new ScriptManager(),
                settingsManager);

        workflowWorker.processDocument(document);

        final Field settingsField = document.getField("CAF_WORKFLOW_SETTINGS");
        assertEquals(1, settingsField.getValues().size());

        final Gson gson = new Gson();
        final Type type = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> settings = gson.fromJson(
                settingsField.getStringValues().get(0), type);

        assertEquals(1, settings.size());
        assertEquals("value from field", settings.get("example"));

        final Field workflowNameField = document.getField("CAF_WORKFLOW_NAME");
        assertEquals(1, workflowNameField.getValues().size());
        assertEquals("sample-workflow", workflowNameField.getStringValues().get(0));

        final Scripts scripts = document.getTask().getScripts();
        assertEquals(2, scripts.size());
        final Script inlineScript = scripts.get(0);
        assertEquals("temp-workflow.js", inlineScript.getName());

        final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        final Invocable invocable = (Invocable) scriptEngine;

        //Write the js to disk so you can set a breakpoint
        //https://intellij-support.jetbrains.com/hc/en-us/community/posts/206834455-Break-Point-ignored-while-debugging-Nashorn-Javascript
        FileUtils.write(new File(".\\target\\workflow.js"), inlineScript.getScript(), StandardCharsets.UTF_8);
        scriptEngine.eval("load('.\\\\target\\\\workflow.js');");

//        scriptEngine.eval(inlineScript.getScript());

        invocable.invokeFunction("processDocument", document);

        assertEquals(failuresToString(document), 0, document.getFailures().size());

        final Response response = document.getTask().getResponse();
        assertEquals("action_1_queueName", response.getSuccessQueue().getName());
        assertEquals("action_1_queueName", response.getFailureQueue().getName());

        assertEquals("value from field", response.getCustomData().get("example"));
        assertEquals("literalExample", response.getCustomData().get("valueFromLiteral"));
    }

    private String failuresToString(final Document document){
        final StringBuilder stringBuilder = new StringBuilder();
        for(final Failure failure: document.getFailures()){
            stringBuilder.append(failure.getFailureMessage());
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
