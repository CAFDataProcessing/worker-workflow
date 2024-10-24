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

import com.github.cafapi.common.api.ConfigurationException;
import com.github.cafdataprocessing.worker.document.model.Document;
import com.github.cafdataprocessing.worker.document.testing.CustomDataBuilder;
import com.github.cafdataprocessing.worker.document.testing.DocumentBuilder;
import com.github.cafdataprocessing.workflow.restclients.settings_service.api.SettingsApi;
import com.github.cafdataprocessing.workflow.testing.ActionExpectationsBuilder;
import com.github.cafdataprocessing.workflow.testing.WorkflowTestExecutor;

import java.util.HashMap;
import java.util.Map;

import com.github.workerframework.worker.api.WorkerException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

public class WorkflowWorkerTest
{
    private WorkflowTestExecutor workflowTestExecutor;
    private WorkflowWorker workflowWorker;
    private String action_1_queueName;
    private String action_2_queueName;
    private String action_3_queueName;

    @BeforeEach
    public void before() {
        workflowTestExecutor = new WorkflowTestExecutor();

        final WorkflowWorkerConfiguration workflowWorkerConfiguration = new WorkflowWorkerConfiguration();
        workflowWorkerConfiguration.setWorkflowsDirectory(WorkflowDirectoryProvider.getWorkflowDirectory("workflow-worker-test"));
        workflowWorkerConfiguration.setSettingsServiceUrl("mocked service");

        final ArgumentsManager argumentsManager = new ArgumentsManager(mock(SettingsApi.class), mock(SettingsApi.class),
                workflowWorkerConfiguration.getSettingsServiceUrl());
        try {
            final Document document = DocumentBuilder.configure().build();
            workflowWorker = new WorkflowWorker(
                    workflowWorkerConfiguration,
                    new WorkflowManager(document.getApplication(), workflowWorkerConfiguration.getWorkflowsDirectory(),
                        workflowWorkerConfiguration.getContextScriptFilePath()),
                    new ScriptManager(),
                    argumentsManager,
                    new FailureFieldsManager());
        } catch (ConfigurationException | WorkerException e) {
            throw new RuntimeException(e);
        }
        if(System.getenv("QUEUE_NAME_SOURCE")!=null && System.getenv("QUEUE_NAME_SOURCE").equalsIgnoreCase("ENV_VAR")){
            action_1_queueName="dataprocessing-action-1-in";
            action_2_queueName="dataprocessing-action-2-in";
            action_3_queueName="dataprocessing-action-3-in";
        }
        else{
            action_1_queueName="action_1-in";
            action_2_queueName="action_2-in";
            action_3_queueName="action_3-in";
        }
    }

    @Test
    public void validateAllActionsTest() throws Exception {

        final Map<String, String[]> fields = new HashMap<>();

        fields.put("example", new String[]{"value from field"});
        fields.put("field-should-exist", new String[]{"action 2 requires this field to be present"});
        fields.put("fieldHasValue", new String[]{"This value"});

        final ActionExpectationsBuilder actionExpectationsBuilder = new ActionExpectationsBuilder();
        actionExpectationsBuilder
                .withAction("action_1")
                    .successQueue(action_1_queueName)
                    .failureQueue(action_1_queueName)
                    .withCustomData()
                    .addCustomData("example", "value from field")
                    .addCustomData("valueFromLiteral", "literalExample")
                .actionExpectationsBuilder()
                .withAction("action_2")
                    .successQueue(action_2_queueName)
                    .failureQueue(action_2_queueName)
                    .withCustomData()
                    .addCustomData("literalWithDoubleQuotes_1", "literalExample_1")
                    .addCustomData("literalWithDoubleQuotes_2", "literalExample_2")
                    .addCustomData("literalWithDoubleQuotes_3", "literalExample_3")
                .actionExpectationsBuilder()
                .withAction("action_3")
                .successQueue(action_3_queueName)
                .failureQueue(action_3_queueName)
                .withCustomData();

        workflowTestExecutor.assertWorkflowActionsExecuted("sample-workflow",
                workflowWorker,
                fields,
                null,
                actionExpectationsBuilder.build());
    }

    @Test
    public void action2ConditionNotPassTest() throws Exception {

        final Map<String, String[]> fields = new HashMap<>();

        fields.put("example", new String[]{"value from field"});
        
        final ActionExpectationsBuilder actionExpectationsBuilder = new ActionExpectationsBuilder();
        actionExpectationsBuilder
                .withAction("action_1")
                .successQueue(action_1_queueName)
                .failureQueue(action_1_queueName)
                .withCustomData()
                .addCustomData("example", "value from field")
                .addCustomData("valueFromLiteral", "literalExample");

        workflowTestExecutor.assertWorkflowActionsExecuted("sample-workflow",
                workflowWorker,
                fields,
                null,
                actionExpectationsBuilder.build());
    }

    @Test
    public void subDocumentPassConditionTest() throws Exception {

        final Document document = DocumentBuilder.configure().withFields()
                .addFieldValue("example", "value from field")
                .addFieldValue("fieldHasValue", "This value")
                .documentBuilder()
                .withSubDocuments(DocumentBuilder.configure().withFields()
                        .addFieldValue("field-should-exist", "action 2 requires this field to be present")
                        .documentBuilder())
                .build();

        final ActionExpectationsBuilder actionExpectationsBuilder = new ActionExpectationsBuilder();
        actionExpectationsBuilder
                .withAction("action_1")
                .successQueue(action_1_queueName)
                .failureQueue(action_1_queueName)
                .withCustomData()
                .addCustomData("example", "value from field")
                .addCustomData("valueFromLiteral", "literalExample")
                .actionExpectationsBuilder()
                .withAction("action_2")
                .successQueue(action_2_queueName)
                .failureQueue(action_2_queueName)
                .withCustomData()
                .actionExpectationsBuilder()
                .withAction("action_3")
                .successQueue(action_3_queueName)
                .failureQueue(action_3_queueName)
                .withCustomData();

        workflowTestExecutor.assertWorkflowActionsExecuted("sample-workflow",
                workflowWorker,
                document,
                null,
                actionExpectationsBuilder.build());

    }

    @Test
    public void mdcLoggingDataIsAddedToDocumentTaskResponseCustomDataTest() throws Exception {
        
        // On receiving a document with a tenantId provided via custom data, but no correlationId, verify that the workflow worker:
        //
        // 1. Adds the tenantId to the document task response custom data.
        // 2. Generates a random correlationId, and also adds this to the document task response custom data.
        //
        // Adding these two values to the document task response custom data ensures that they will be available to subsequent workers
        // in the workflow.

        // Arrange
        final Document document = DocumentBuilder.configure()
            .withCustomData()
            .add("tenantId", "test-tenant") 
            .documentBuilder()
            .build();
        
        assertNull(document.getTask().getResponse().getCustomData().get("tenantId"));
        assertNull(document.getTask().getResponse().getCustomData().get("correlationId"));
        
        // Act
        workflowWorker.processDocument(document);
        
        // Assert
        assertEquals("test-tenant", document.getTask().getResponse().getCustomData().get("tenantId"));
        assertNotNull(document.getTask().getResponse().getCustomData().get("correlationId"));
    }

    @Test
    public void workflowOnlyRerouteTest() throws Exception {

        final Document document = DocumentBuilder.configure().withFields()
                .documentBuilder()
                .build();

        final ActionExpectationsBuilder actionExpectationsBuilder = new ActionExpectationsBuilder();
        actionExpectationsBuilder
                .withAction("action_1")
                .successQueue(action_1_queueName + "»/sample-message-prioritization-workflow")
                .failureQueue(action_1_queueName)
                .withCustomData();

        workflowTestExecutor.assertWorkflowActionsExecuted("sample-message-prioritization-workflow",
                workflowWorker,
                document,
                null,
                actionExpectationsBuilder.build());
    }

    @Test
    public void tenantIdRerouteTest() throws Exception {

        Map<String, String> customData = new HashMap<>();
        customData.put("tenantId",  "1");
        
        final DocumentBuilder documentBuilder = DocumentBuilder.configure().withFields().documentBuilder();
        final CustomDataBuilder customDataBuilder = new CustomDataBuilder(customData, documentBuilder);
        final Document document = documentBuilder.build();

        final ActionExpectationsBuilder actionExpectationsBuilder = new ActionExpectationsBuilder();
        actionExpectationsBuilder
                .withAction("action_1")
                .successQueue(action_1_queueName + "»/1/sample-message-prioritization-workflow")
                .failureQueue(action_1_queueName)
                .withCustomData();

        workflowTestExecutor.assertWorkflowActionsExecuted("sample-message-prioritization-workflow",
                workflowWorker,
                document,
                customData,
                actionExpectationsBuilder.build());
    }
}
