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
package com.github.cafdataprocessing.workflow.testing;

import com.google.common.base.Strings;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.impl.ResponseCustomDataImpl;
import com.hpe.caf.worker.document.model.*;
import com.hpe.caf.worker.document.testing.CustomDataBuilder;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.FieldsBuilder;
import org.apache.commons.io.FileUtils;

import javax.print.Doc;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WorkflowTestExecutor {

    public void assertWorkflowActionsExecuted(final String workflowName,
                                              final DocumentWorker documentWorker,
                                              final Map<String, String[]> fields,
                                              final Map<String, String> customData,
                                              final List<ActionExpectation> actionExpectations) {
        assertWorkflowActionsExecuted(workflowName, documentWorker, fields, customData, null, actionExpectations);
    }

    public void assertWorkflowActionsExecuted(final String workflowName,
                                              final DocumentWorker documentWorker,
                                              final Map<String, String[]> fields,
                                              final Map<String, String> customData,
                                              final String[] completedActions,
                                              final List<ActionExpectation> actionExpectations) {

        Document documentForExecution = getDocument(workflowName, completedActions, fields, customData);

        try {
            documentWorker.processDocument(documentForExecution);
            assertEquals(failuresToString(documentForExecution), 0, documentForExecution.getFailures().size());
            executeScript(documentForExecution);
        } catch (DocumentWorkerTransientException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(actionExpectations == null || actionExpectations.size() == 0){
            assertEquals("No actions to execute was expected.", 0,
                    documentForExecution.getField("CAF_WORKFLOW_ACTION").getValues().size());
        }
        else {
            validateAction(actionExpectations.get(0), documentForExecution);

            if(actionExpectations.size() > 1) {
                for(int index = 1; index < actionExpectations.size(); index ++){

                    try {
                        documentForExecution = getDocument(workflowName, customData, documentForExecution);
                        documentWorker.processDocument(documentForExecution);
                        assertEquals(failuresToString(documentForExecution), 0, documentForExecution.getFailures().size());
                        executeScript(documentForExecution);
                    } catch (DocumentWorkerTransientException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    validateAction(actionExpectations.get(index), documentForExecution);
                }
            }
        }

        try {
            documentWorker.processDocument(documentForExecution);
            executeScript(documentForExecution);
        } catch (DocumentWorkerTransientException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (documentForExecution.getField("CAF_WORKFLOW_ACTION").getValues().size() > 0){
            fail(String.format("Action [%s] was not defined in the action expectations.",
                    documentForExecution.getField("CAF_WORKFLOW_ACTION").getStringValues().get(0)));
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
                assertEquals(String.format("Action [%s] argument [%s] not as expected.",
                        actionExpectation.getAction(),
                        entry.getKey()),
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

    private String failuresToString(final Document document){
        final StringBuilder stringBuilder = new StringBuilder();
        for(final Failure failure: document.getFailures()){
            stringBuilder.append(failure.getFailureMessage());
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    private Document getDocument(final String workflowName,
                                 final Map<String, String> customData,
                                 final Document previousDocument){

        final Map<String, String[]> fields = new HashMap<>();

        for(final Field field: previousDocument.getFields()){
            fields.put(field.getName(), field.getStringValues().toArray(new String[0]));
        }

        return getDocument(workflowName, null, fields, customData);
    }

    private Document getDocument(final String workflowName,
                                 final String[] completedActions,
                                 final Map<String, String[]> fields,
                                 final Map<String, String> customData) {

        final DocumentBuilder documentBuilder = DocumentBuilder.configure();

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

        if(fields!=null){
            for(final Map.Entry<String, String[]> entry: fields.entrySet()){
                for(final String value:entry.getValue()){
                    fieldsBuilder.addFieldValue(entry.getKey(), value);
                }
            }
        }

        final CustomDataBuilder customDataBuilder = documentBuilder.withCustomData();
        if(customData!=null){
            for(final Map.Entry<String, String> entry: customData.entrySet()){
                customDataBuilder.add(entry.getKey(), entry.getValue());
            }

        }

        try {
            return documentBuilder.build();
        } catch (WorkerException e) {
            throw new RuntimeException(e);
        }
    }

}
