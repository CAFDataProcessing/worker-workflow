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

import com.github.cafdataprocessing.workflow.testing.ActionExpectationsBuilder;
import com.github.cafdataprocessing.workflow.testing.WorkflowTestExecutor;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.microfocus.darwin.settings.client.SettingsApi;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class WorkflowWorkerTest
{
    private WorkflowTestExecutor workflowTestExecutor;
    private WorkflowWorker workflowWorker;

    @Before
    public void before() {
        workflowTestExecutor = new WorkflowTestExecutor();

        final WorkflowWorkerConfiguration workflowWorkerConfiguration = new WorkflowWorkerConfiguration();
        workflowWorkerConfiguration.setWorkflowsDirectory(WorkflowDirectoryProvider.getWorkflowDirectory("workflow-worker-test"));
        workflowWorkerConfiguration.setSettingsServiceUrl("mocked service");

        final ArgumentsManager argumentsManager = new ArgumentsManager(mock(SettingsApi.class),
                workflowWorkerConfiguration.getSettingsServiceUrl());

        try {
            final Document document = DocumentBuilder.configure().build();
            workflowWorker = new WorkflowWorker(
                    workflowWorkerConfiguration,
                    new WorkflowManager(document.getApplication(), workflowWorkerConfiguration.getWorkflowsDirectory()),
                    new ScriptManager(),
                    argumentsManager);
        } catch (ConfigurationException | WorkerException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void validateAllActionsTest() throws Exception {

        final DocumentBuilder documentBuilder = DocumentBuilder.configure();
        documentBuilder.withFields()
                .addFieldValue("example", "value from field")
                .addFieldValue("field-should-exist", "action 2 requires this field to be present");

        final ActionExpectationsBuilder actionExpectationsBuilder = new ActionExpectationsBuilder();
        actionExpectationsBuilder
                .withAction("action_1")
                    .successQueue("action_1_queueName")
                    .failureQueue("action_1_queueName")
                    .withCustomData()
                    .addCustomData("example", "value from field")
                    .addCustomData("valueFromLiteral", "literalExample")
                .actionExpectationsBuilder()
                .withAction("action_2")
                    .successQueue("action_2_queueName")
                    .failureQueue("action_2_queueName");

        workflowTestExecutor.assertWorkflowActionsExecuted("sample-workflow",
                workflowWorker,
                documentBuilder,
                actionExpectationsBuilder.build());
    }


    @Test
    public void action2ConditionNotPassTest() throws Exception {

        final DocumentBuilder documentBuilder = DocumentBuilder.configure();
        documentBuilder.withFields()
                .addFieldValue("example", "value from field");

        final ActionExpectationsBuilder actionExpectationsBuilder = new ActionExpectationsBuilder();
        actionExpectationsBuilder
                .withAction("action_1")
                .successQueue("action_1_queueName")
                .failureQueue("action_1_queueName")
                .withCustomData()
                .addCustomData("example", "value from field")
                .addCustomData("valueFromLiteral", "literalExample");

        workflowTestExecutor.assertWorkflowActionsExecuted("sample-workflow",
                workflowWorker,
                documentBuilder,
                actionExpectationsBuilder.build());
    }

}
