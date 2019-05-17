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
package com.github.cafdataprocessing.workflow.utils;

import com.github.cafdataprocessing.workflow.*;
import com.google.common.base.Strings;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.model.*;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.FieldsBuilder;
import com.microfocus.darwin.settings.client.SettingsApi;
import org.apache.commons.io.FileUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class WorkflowTestExecutor {

    private final WorkflowWorkerConfiguration workflowWorkerConfiguration;
    private final SettingsManager settingsManager;

    public WorkflowTestExecutor(final String workflowsDirectory) {
        this(mock(SettingsApi.class), workflowsDirectory);
    }

    public WorkflowTestExecutor(final SettingsApi settingsApi, final String workflowsDirectory) {
        workflowWorkerConfiguration = new WorkflowWorkerConfiguration();

        workflowWorkerConfiguration.setWorkflowsDirectory(workflowsDirectory);
        workflowWorkerConfiguration.setSettingsServiceUrl("mocked service");

        settingsManager = new SettingsManager(settingsApi, workflowWorkerConfiguration.getSettingsServiceUrl());
    }

    public void assertWorkflowActionsExecuted(final String workflowName,
                                              final DocumentBuilder documentBuilder,
                                              final List<ActionExpectation> actionExpectations) {
        assertWorkflowActionsExecuted(workflowName, documentBuilder, null, actionExpectations);
    }

    public void assertWorkflowActionsExecuted(final String workflowName,
                                              final DocumentBuilder documentBuilder,
                                              final String[] completedActions,
                                              final List<ActionExpectation> actionExpectations) {

        final FieldsBuilder fieldsBuilder = documentBuilder.withFields();

        if (!Strings.isNullOrEmpty(workflowName)) {
            documentBuilder.withCustomData()
                    .add("workflowName", workflowName);
        }

        if (completedActions != null) {
            for (final String completedAction : completedActions) {
                fieldsBuilder.addFieldValue("CAF_WORKFLOW_ACTIONS_COMPLETED", completedAction);
            }
        }

        final Document document;
        try {
            document = documentBuilder.build();
        } catch (WorkerException e) {
            throw new RuntimeException(e);
        }

        final WorkflowWorker workflowWorker;
        try {
            workflowWorker = new WorkflowWorker(
                    workflowWorkerConfiguration,
                    new WorkflowManager(document.getApplication(), workflowWorkerConfiguration.getWorkflowsDirectory()),
                    new ScriptManager(),
                    settingsManager);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

        try {
            workflowWorker.processDocument(document);
            assertEquals(failuresToString(document), 0, document.getFailures().size());
            executeScript(document);
        } catch (DocumentWorkerTransientException e) {
            throw new RuntimeException(e);
        }

        if(actionExpectations == null || actionExpectations.size() == 0){
            assertEquals("No actions to execute was expected.", 0,
                    document.getField("CAF_WORKFLOW_ACTION").getValues().size());
        }
        else {
            validateAction(actionExpectations.get(0), document);

            if(actionExpectations.size() > 1) {
                for(int index = 1; index < actionExpectations.size(); index ++){

                    try {
                        workflowWorker.processDocument(document);
                        assertEquals(failuresToString(document), 0, document.getFailures().size());
                        executeScript(document);
                    } catch (DocumentWorkerTransientException e) {
                        throw new RuntimeException(e);
                    }

                    validateAction(actionExpectations.get(index), document);
                }
            }
        }

        try {
            workflowWorker.processDocument(document);
            executeScript(document);
        } catch (DocumentWorkerTransientException e) {
            throw new RuntimeException(e);
        }

        if (document.getField("CAF_WORKFLOW_ACTION").getValues().size() > 0){
            fail(String.format("Action [%s] was not defined in the action expectations.",
                    document.getField("CAF_WORKFLOW_ACTION").getStringValues().get(0)));
        }

    }

    private void validateAction(final ActionExpectation actionExpectation, final Document document) {
        final Field actionToExecuteField = document.getField("CAF_WORKFLOW_ACTION");
        assertEquals(actionExpectation.getAction() + " not marked for execution.", 1, actionToExecuteField.getValues().size());
        assertEquals("Expected action not found.", actionExpectation.getAction(),
                actionToExecuteField.getStringValues().get(0));

        final Response response = document.getTask().getResponse();

        if(actionExpectation.getCustomData() != null) {

            for(final Map.Entry<String, String> entry : actionExpectation.getCustomData().entrySet()){
                assertEquals(String.format("Setting [%s] not as expected.", entry.getKey()),
                        entry.getValue(), response.getCustomData().get(entry.getKey()));
            }
        }

        assertEquals(actionExpectation.getAction() + " success queue not as expected.", actionExpectation.getSuccessQueue(),
                response.getSuccessQueue().getName());

        assertEquals(actionExpectation.getAction() + " failure queue not as expected.", actionExpectation.getFailureQueue(),
                response.getFailureQueue().getName());

    }

    private static void executeScript(final Document document) {

        final Scripts scripts = document.getTask().getScripts();
        final Script inlineScript = scripts.get(0);

        assertEquals("temp-workflow.js", inlineScript.getName());

        final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
        final Invocable invocable = (Invocable) scriptEngine;

        //Write the js to disk so you can set a breakpoint
        //https://intellij-support.jetbrains.com/hc/en-us/community/posts/206834455-Break-Point-ignored-while-debugging-Nashorn-Javascript
        final Path p = Paths.get("target", "workflow.js");

        try {
            FileUtils.write(p.toFile(), inlineScript.getScript(), StandardCharsets.UTF_8);
            scriptEngine.eval("load('" + p.toString().replace("\\", "\\\\") + "');");

//        scriptEngine.eval(inlineScript.getScript());

            invocable.invokeFunction("processDocument", document);
        }
        catch (final Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public String failuresToString(final Document document){
        final StringBuilder stringBuilder = new StringBuilder();
        for(final Failure failure: document.getFailures()){
            stringBuilder.append(failure.getFailureMessage());
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
