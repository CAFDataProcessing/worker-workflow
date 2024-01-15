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
package com.github.cafdataprocessing.workflow.testing;

import com.google.common.base.Strings;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.*;
import com.hpe.caf.worker.document.scripting.events.TaskEventObject;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.FieldsBuilder;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.apache.commons.io.FileUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.io.IOAccess;

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

        final DocumentBuilder documentBuilder = DocumentBuilder.configure();
        final FieldsBuilder fieldsBuilder = documentBuilder.withFields();

        for(final Map.Entry<String, String[]> entry : fields.entrySet()){
            for(final String value: entry.getValue()){
                fieldsBuilder.addFieldValue(entry.getKey(), value);
            }
        }

        if (completedActions != null) {
            for (final String completedAction : completedActions) {
                fieldsBuilder.addFieldValue("CAF_WORKFLOW_ACTIONS_COMPLETED", completedAction);
            }
        }

        try {
            assertWorkflowActionsExecuted(workflowName, documentWorker, documentBuilder.build(), customData, actionExpectations);
        } catch (final WorkerException e) {
            throw new RuntimeException(e);
        }
    }

    public void assertWorkflowActionsExecuted(final String workflowName,
                                              final DocumentWorker documentWorker,
                                              final Document originalDocument,
                                              final Map<String, String> customData,
                                              final List<ActionExpectation> actionExpectations) {

        Document documentForExecution = cloneDocument(originalDocument, workflowName, customData);

        try {
            //Process the document with the workflow worker to generate the arguments, attach scripts and set the first
            //target action.
            documentWorker.processDocument(documentForExecution);
            assertEquals(failuresToString(documentForExecution), 0, documentForExecution.getFailures().size());
            executeOnAfterProcessTaskScript(documentForExecution);
        } catch (DocumentWorkerTransientException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(actionExpectations == null || actionExpectations.size() == 0){
            assertEquals("No actions to execute was expected.", 0,
                    documentForExecution.getField("CAF_WORKFLOW_ACTION").getValues().size());
        }

        validateAction(actionExpectations.get(0), documentForExecution);

        if(actionExpectations.size() > 1) {
            for(int index = 1; index < actionExpectations.size(); index ++){

                try {
                    final String action = documentForExecution
                            .getField("CAF_WORKFLOW_ACTION").getStringValues().get(0);
                    final List<String> completedActions = documentForExecution
                            .getField("CAF_WORKFLOW_ACTIONS_COMPLETED").getStringValues();

                    documentForExecution = cloneDocument(originalDocument, workflowName, customData);
                    documentForExecution.getField("CAF_WORKFLOW_ACTION").add(action);
                    for(final String completedAction: completedActions){
                        documentForExecution.getField("CAF_WORKFLOW_ACTIONS_COMPLETED").add(completedAction);
                    }

                    documentWorker.processDocument(documentForExecution);
                    assertEquals(failuresToString(documentForExecution), 0, documentForExecution.getFailures().size());
                    executeOnAfterProcessTaskScript(documentForExecution);
                } catch (DocumentWorkerTransientException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                validateAction(actionExpectations.get(index), documentForExecution);
            }
        }

        //Verify there are no unexpected actions, in other words actions that weren't supplied in the expectations
        try {
            documentWorker.processDocument(documentForExecution);
            executeOnAfterProcessTaskScript(documentForExecution);
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

    private static void executeOnAfterProcessTaskScript(final Document document) {

        final Scripts scripts = document.getTask().getScripts();
        final Script inlineScript = scripts.get(0);

        assertEquals("temp-workflow.js", inlineScript.getName());

        final ScriptEngine scriptEngine = GraalJSScriptEngine.create(
                null,
                Context.newBuilder("js")
                    .allowExperimentalOptions(true) // Needed for loading from classpath
                    .allowHostAccess(HostAccess.ALL) // Allow JS access to public Java methods/members
                    .allowHostClassLookup(s -> true) // Allow JS access to public Java classes
                    .allowIO(IOAccess.ALL)
                    .option("js.load-from-classpath", "true"));
        final Invocable invocable = (Invocable) scriptEngine;

        //Write the js to disk so you can set a breakpoint
        //https://intellij-support.jetbrains.com/hc/en-us/community/posts/206834455-Break-Point-ignored-while-debugging-Nashorn-Javascript
        final Path p = Paths.get("target", "workflow.js");

        try {
            FileUtils.write(p.toFile(), inlineScript.getScript(), StandardCharsets.UTF_8);
            scriptEngine.eval("load('" + p.toString().replace("\\", "\\\\") + "');");

//        scriptEngine.eval(inlineScript.getScript());

            final TaskEventObject taskEventObject = new TaskEventObject(document.getTask());
            invocable.invokeFunction("onAfterProcessTask", taskEventObject);
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

    private Document cloneDocument(final Document document, final String workflowName, final Map<String, String> customData) {

        final DocumentBuilder documentBuilder = DocumentBuilder.configure();

        if (!Strings.isNullOrEmpty(workflowName)) {
            documentBuilder.withCustomData().add("workflowName", workflowName);
        }

        if(customData!=null){
            for(final Map.Entry<String, String> entry: customData.entrySet()){
                documentBuilder.withCustomData().add(entry.getKey(), entry.getValue());
            }
        }

        cloneDocument(document, documentBuilder);

        try {
            return documentBuilder.build();
        } catch (WorkerException e) {
            throw new RuntimeException(e);
        }
    }

    private void cloneDocument(final Document document, final DocumentBuilder documentBuilder) {
        cloneFields(document, documentBuilder.withFields());

        for(final Document subdocument: document.getSubdocuments()){
            final DocumentBuilder subdocumentBuilder = DocumentBuilder.configure();
            documentBuilder.withSubDocuments(subdocumentBuilder);
            cloneDocument(subdocument, subdocumentBuilder);
        }
    }

    private void cloneFields(final Document document, final FieldsBuilder fieldsBuilder){
        for(final Field field: document.getFields()){
            for(final String fieldValue: field.getStringValues()) {
                fieldsBuilder.addFieldValue(field.getName(), fieldValue);
            }
        }

    }

}
