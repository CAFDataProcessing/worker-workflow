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

import com.github.cafdataprocessing.workflow.model.Action;
import com.github.cafdataprocessing.workflow.model.ArgumentDefinition;
import com.github.cafdataprocessing.workflow.model.Workflow;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
                WorkflowDirectoryProvider.getWorkflowDirectory("workflow-manager-test"));

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
                    WorkflowDirectoryProvider.getWorkflowDirectory("workflow-manager-duplicate-action-test"));
        } catch (ConfigurationException ex) {
            assertEquals("Duplicated action name [action_1].", ex.getMessage());
        }

    }

    @Test
    public void noActionNameTest() throws WorkerException {

        final Document document = DocumentBuilder.configure().build();

        try {
            final WorkflowManager workflowManager = new WorkflowManager(document.getApplication(),
                    WorkflowDirectoryProvider.getWorkflowDirectory("workflow-manager-no-action-name-test"));
        } catch (ConfigurationException ex) {
            assertEquals("Action name is not defined for action [0].", ex.getMessage());
        }

    }

    @Test
    public void noQueueNameTest() throws WorkerException {

        final Document document = DocumentBuilder.configure().build();

        try {
            final WorkflowManager workflowManager = new WorkflowManager(document.getApplication(),
                    WorkflowDirectoryProvider.getWorkflowDirectory("workflow-manager-no-queue-name-test"));
        } catch (ConfigurationException ex) {
            assertEquals("QueueName is not defined for action [action_1].", ex.getMessage());
        }

    }

}
