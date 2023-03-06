/*
 * Copyright 2017-2023 Micro Focus or one of its affiliates.
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

import com.github.cafdataprocessing.workflow.model.Action;
import com.github.cafdataprocessing.workflow.model.ArgumentDefinition;
import com.github.cafdataprocessing.workflow.model.Workflow;
import com.github.cafdataprocessing.workflow.testing.WorkflowTestExecutor;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import com.google.common.io.Resources;
import com.hpe.caf.worker.document.model.Failure;
import com.hpe.caf.worker.document.scripting.events.CancelableDocumentEventObject;
import com.microfocus.darwin.settings.client.SettingsApi;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.apache.commons.io.IOUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class WorkflowManagerTest {

    @Test
    public void getWorkflowTest() throws Exception {
        final TestServices testServices = TestServices.createDefault();
        final Document document = DocumentBuilder.configure().withServices(testServices)
                .withCustomData()
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        final WorkflowManager workflowManager = new WorkflowManager(document.getApplication(),
                WorkflowDirectoryProvider.getWorkflowDirectory("workflow-manager-test"), null);

        final Workflow workflow = workflowManager.get("test-workflow");

        final String expectedScript = Resources.toString(Resources.getResource("workflow-manager-test/expected-script.js"),
                StandardCharsets.UTF_8);

        //TODO Comparison is failing is it line endings or something?
//        assertEquals(expectedScript, workflow.getWorkflowScript());

        final String storedScriptReference = workflow.getStorageReferenceForWorkflowScript();
        final String storedScript = IOUtils.toString(testServices.getDataStore().retrieve(storedScriptReference),
                StandardCharsets.UTF_8);

        assertEquals(workflow.getWorkflowScript(), storedScript);

        final List<ArgumentDefinition> argumentDefinitions = workflow.getArguments();
        assertNotNull(argumentDefinitions);
        assertEquals(5, argumentDefinitions.size());

        final List<Action> actions = workflow.getActions();
        assertEquals(3, actions.size());
        assertEquals("family_hashing", actions.get(0).getName());
        assertEquals("lang_detect", actions.get(1).getName());
        assertEquals("bulk_index", actions.get(2).getName());
    }

    @Test
    public void duplicateActionNameTest() throws WorkerException {

        final Document document = DocumentBuilder.configure().build();

        try {
            final WorkflowManager workflowManager = new WorkflowManager(document.getApplication(),
                    WorkflowDirectoryProvider.getWorkflowDirectory("workflow-manager-duplicate-action-test"),
                    null);
        } catch (ConfigurationException ex) {
            assertEquals("Duplicated action name [action_1].", ex.getMessage());
        }

    }

    @Test
    public void noActionNameTest() throws WorkerException {

        final Document document = DocumentBuilder.configure().build();

        try {
            final WorkflowManager workflowManager = new WorkflowManager(document.getApplication(),
                    WorkflowDirectoryProvider.getWorkflowDirectory("workflow-manager-no-action-name-test"),
                    null);
        } catch (ConfigurationException ex) {
            assertEquals("Action name is not defined for action [0].", ex.getMessage());
        }

    }
    
    @Test
    public void terminateOnFailureValueTest() throws WorkerException, ConfigurationException {

        final Document document = getDocumentWithSubDocument();
        document.getField("CAF_WORKFLOW_ACTION").add("lang_detect");
        document.getField("CAF_WORKFLOW_SETTINGS").add("{}");
        
        final WorkflowManager workflowManager = new WorkflowManager(document.getApplication(),
                                     WorkflowDirectoryProvider.getWorkflowDirectory("wokflow-worker-action-fields-test"),
                                     null);

        final Workflow workflow = workflowManager.get("test-workflow");
        
        final List<Action> actions = workflow.getActions();
        assertEquals(2, actions.size());
        assertEquals("family_hashing", actions.get(0).getName());
        Assert.assertFalse(actions.get(0).isTerminateOnFailure());
        assertEquals("bulk_index", actions.get(1).getName());
        Assert.assertTrue(actions.get(1).isTerminateOnFailure());

    }
    
    @Test
    public void onBeforeProcessDocumentTest() throws Exception
    {
        final Document document = getDocumentWithSubDocument();
        document.getField("CAF_WORKFLOW_ACTION").add("lang_detect");
        document.getField("CAF_WORKFLOW_SETTINGS").add("{}");
        
        final WorkflowManager workflowManager = new WorkflowManager(document.getApplication(),
                                     WorkflowDirectoryProvider.getWorkflowDirectory("workflow-manager-test"),
                                     null);

        final Workflow workflow = workflowManager.get("test-workflow");
        final String workflowScript = workflow.getWorkflowScript();
        executeOnBeforeProcessDocumentScript(workflowScript, document);
    }

    private static void executeOnBeforeProcessDocumentScript(String workflowScript, Document document)
    {
        final ScriptEngine scriptEngine = GraalJSScriptEngine.create(
                null,
                Context.newBuilder("js")
                    .allowExperimentalOptions(true) // Needed for loading from classpath
                    .allowHostAccess(HostAccess.ALL) // Allow JS access to public Java methods/members
                    .allowHostClassLookup(s -> true) // Allow JS access to public Java classes
                    .option("js.load-from-classpath", "true"));
        final Invocable invocable = (Invocable) scriptEngine;
        try {
            scriptEngine.eval(workflowScript);
            invokeOnBeforeFunction(invocable, document);
        } catch (NoSuchMethodException | ScriptException ex) {
            throw new RuntimeException(ex);
        }

    }

    private static void invokeOnBeforeFunction(Invocable invocable, Document document)
        throws NoSuchMethodException, ScriptException
    {
        final CancelableDocumentEventObject cancelTaskEventObject = new CancelableDocumentEventObject(document);
        boolean valueCheck = document.getField("CONTENT_PRIMARY").hasValues();
        invocable.invokeFunction("onBeforeProcessDocument", cancelTaskEventObject);
        assertEquals(!valueCheck, cancelTaskEventObject.cancel);
        if (document.hasSubdocuments()) {
            for (Document subDoc : document.getSubdocuments()) {
                invokeOnBeforeFunction(invocable, subDoc);
            }
        }
    }

    private Document getDocumentWithSubDocument() throws WorkerException
    {
        /**
         * *********
         * doc1, doc7, doc6 pass main document has sub-documents as below
         *
         *      ------- doc7
         *      |                          ------ doc3 ---- docA(content_primary)
         *      |                          |
         *  doc -------- doc1----- doc5-----
         *      |                          |
         *      |                          ------ doc2
         *      ------- doc6
         *
         */
        final DocumentBuilder subDocumentA = DocumentBuilder.configure()
            .withFields()
            .addFieldValue("someField", "Some data to test A")
            .addFieldValue("CONTENT_PRIMARY", "Some data to test 8")
            .documentBuilder();
        final DocumentBuilder subDocument2 = DocumentBuilder.configure()
            .withFields()
            .addFieldValue("someField", "Some data to test-2")
            .documentBuilder();
        final DocumentBuilder subDocument3 = DocumentBuilder.configure()
            .withFields()
            .addFieldValue("someField", "Some data to test-3")
            .documentBuilder().withSubDocuments(subDocumentA);
        final DocumentBuilder subDocument6 = DocumentBuilder.configure()
            .withFields()
            .addFieldValue("someField", "Some data to test-6")
            .documentBuilder()
            .withSubDocuments(subDocument2, subDocument3);

        final DocumentBuilder subDocument7 = DocumentBuilder.configure()
            .withFields()
            .addFieldValue("someField", "Some data to test-7")
            .documentBuilder();
        final DocumentBuilder subDocument8 = DocumentBuilder.configure()
            .withFields()
            .addFieldValue("someField", "Some data to test -8")
            .documentBuilder();

        final DocumentBuilder subDocument4 = DocumentBuilder.configure()
            .withFields()
            .addFieldValue("someField", "Some data to test -4")
            .documentBuilder();
        final DocumentBuilder subDocument5 = DocumentBuilder.configure()
            .withFields()
            .addFieldValue("someField", "Some data to test -5")
            .documentBuilder()
            .withSubDocuments(subDocument4, subDocument8);

        final DocumentBuilder subDocument1 = DocumentBuilder.configure()
            .withFields()
            .addFieldValue("someField", "Some data to test -1")
            .documentBuilder().withSubDocuments(subDocument5);

        //create the document without CONTENT_PRIMARY in root but in subDocument
        final Document document = DocumentBuilder.configure()
            .withSubDocuments(subDocument1, subDocument7, subDocument6).build();

        return document;
    }
}
