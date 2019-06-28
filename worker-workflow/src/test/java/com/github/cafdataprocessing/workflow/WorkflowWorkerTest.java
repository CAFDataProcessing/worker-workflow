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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cafdataprocessing.workflow.models.DocumentTest;
import com.github.cafdataprocessing.workflow.models.NewFailure;
import com.github.cafdataprocessing.workflow.models.TaskTest;
import com.github.cafdataprocessing.workflow.models.WorkerTaskDataTest;
import com.github.cafdataprocessing.workflow.testing.ActionExpectationsBuilder;
import com.github.cafdataprocessing.workflow.testing.WorkflowTestExecutor;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.TaskSourceInfo;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Failures;
import com.hpe.caf.worker.document.model.Fields;
import com.hpe.caf.worker.document.model.Task;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.microfocus.darwin.settings.client.SettingsApi;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import static java.util.stream.Collectors.toList;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.lang3.StringUtils;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.core.IsNull.nullValue;

import static org.mockito.Mockito.mock;

public class WorkflowWorkerTest
{
    private WorkflowTestExecutor workflowTestExecutor;
    private WorkflowWorker workflowWorker;
    private String action_1_queueName;
    private String action_2_queueName;
    private String action_3_queueName;

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
    public void failuresPositiveTest() throws ScriptException, NoSuchMethodException, FileNotFoundException, WorkerException, IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        final Document builderDoc = DocumentBuilder.configure().withFields()
            .addFieldValues("CAF_WORKFLOW_ACTION", "super_action")
            .addFieldValue("CAF_WORKFLOW_NAME", "example_workflow")
            .addFieldValue("FAILURES", "")
            .addFieldValue("example", "value from field")
            .addFieldValue("fieldHasValue", "This value")
            .documentBuilder()
            .withSubDocuments(DocumentBuilder.configure().withFields()
                .addFieldValue("field-should-exist", "action 2 requires this field to be present")
                .documentBuilder())
            .build();
        builderDoc.addFailure("error_id_1", "message 1");

        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataTest("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskTest(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentTest("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        final ObjectMapper mapper = new ObjectMapper();
        final List<NewFailure> failuresRetrieved = (List<NewFailure>) invocable.invokeFunction("haveFailuresChanged", document);
        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream().findFirst().get().getFailureId(), is(equalTo("error_id_1")));
        assertThat(document.getFailures().stream().findFirst().get().getFailureStack(), is(nullValue()));
        final String serializeFailure = mapper.writeValueAsString(failuresRetrieved.get(0));
        final NewFailure failureMessage = mapper.readValue(serializeFailure, NewFailure.class);
        assertThat(failureMessage.getFailureId(), is(equalTo("error_id_1")));
        assertThat(failureMessage.getStack(), is(nullValue()));
        assertThat(failureMessage.getDescription().getSource(), is(equalTo("super_action")));
        assertThat(failureMessage.getDescription().getOriginalDescription(), is(equalTo("message 1")));
        assertThat(failureMessage.getDescription().getVersion(), is(equalTo(5)));
        assertThat(failureMessage.getDescription().getWorkflowName(), is(equalTo("example_workflow")));
        assertThat(failureMessage.getDescription().getStack(), is(nullValue()));
    }
    
    @Test
    public void onAfterProcessDocumentTest() throws ScriptException, NoSuchMethodException, FileNotFoundException, WorkerException,
                                                    IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        final Document builderDoc = DocumentBuilder.configure().withFields()
            .addFieldValues("CAF_WORKFLOW_ACTION", "super_action")
            .addFieldValue("CAF_WORKFLOW_NAME", "example_workflow")
            .addFieldValue("FAILURES", "")
            .addFieldValue("example", "value from field")
            .addFieldValue("fieldHasValue", "This value")
            .documentBuilder()
            .withSubDocuments(DocumentBuilder.configure().withFields()
                .addFieldValue("field-should-exist", "action 2 requires this field to be present")
                .documentBuilder())
            .build();
        builderDoc.addFailure("error_id_1", "message 1");

        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataTest("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskTest(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentTest("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        invocable.invokeFunction("onAfterProcessDocument", document);
        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream().findFirst().get().getFailureId(), is(equalTo("error_id_1")));
        assertThat(document.getFailures().stream().findFirst().get().getFailureStack(), is(nullValue()));
        
    }
    
    @Test
    public void multipleFailuresPositiveTest() throws ScriptException, NoSuchMethodException, FileNotFoundException, WorkerException,
                                                      IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        final Document builderDoc = DocumentBuilder.configure().withFields()
            .addFieldValues("CAF_WORKFLOW_ACTION", "super_action")
            .addFieldValue("CAF_WORKFLOW_NAME", "example_workflow")
            .addFieldValue("FAILURES", "")
            .addFieldValue("example", "value from field")
            .addFieldValue("fieldHasValue", "This value")
            .documentBuilder()
            .withSubDocuments(DocumentBuilder.configure().withFields()
                .addFieldValue("field-should-exist", "action 2 requires this field to be present")
                .documentBuilder())
            .build();
        builderDoc.addFailure("error_id_1", "message 1");
        builderDoc.addFailure("error_id_2", "message 2");

        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataTest("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskTest(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentTest("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        final ObjectMapper mapper = new ObjectMapper();
        final List<NewFailure> failuresRetrieved = (List<NewFailure>) invocable.invokeFunction("haveFailuresChanged", document);
        assertThat(document.getFailures().size(), is(equalTo((2))));
        assertThat(document.getFailures().stream().map(f -> f.getFailureId()).collect(toList()), contains("error_id_1", "error_id_2"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureStack()).filter(s -> !StringUtils.isEmpty(s)).collect(toList()),
                   is(emptyCollectionOf(String.class)));

        for (int i = 0; i < failuresRetrieved.size(); i++) {
            final String serializeFailure = mapper.writeValueAsString(failuresRetrieved.get(i));
            final NewFailure failureMessage = mapper.readValue(serializeFailure, NewFailure.class);
            assertThat(failureMessage.getFailureId(), isOneOf("error_id_1", "error_id_2"));
            assertThat(failureMessage.getStack(), is(nullValue()));
            assertThat(failureMessage.getDescription().getSource(), is(equalTo("super_action")));
            assertThat(failureMessage.getDescription().getOriginalDescription(), isOneOf("message 1", "message 2"));
            assertThat(failureMessage.getDescription().getVersion(), is(equalTo(5)));
            assertThat(failureMessage.getDescription().getWorkflowName(), is(equalTo("example_workflow")));
            assertThat(failureMessage.getDescription().getStack(), is(nullValue()));
        }
    }
    
    @Test(expected = NoSuchElementException.class)
    public void failuresNegativeNoFailuresFieldTest() throws ScriptException, NoSuchMethodException, FileNotFoundException,
                                                             WorkerException, IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        final Document builderDoc = DocumentBuilder.configure().withFields()
            .addFieldValues("CAF_WORKFLOW_ACTION", "super_action")
            .addFieldValue("CAF_WORKFLOW_NAME", "example_workflow")
            .addFieldValue("example", "value from field")
            .addFieldValue("fieldHasValue", "This value")
            .documentBuilder()
            .withSubDocuments(DocumentBuilder.configure().withFields()
                .addFieldValue("field-should-exist", "action 2 requires this field to be present")
                .documentBuilder())
            .build();
        builderDoc.addFailure("error_id_1", "message 1");
        builderDoc.addFailure("error_id_2", "message 2");

        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataTest("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskTest(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentTest("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        invocable.invokeFunction("haveFailuresChanged", document);
    }
    
    @Test(expected = NoSuchElementException.class)
    public void failuresNegativeNoWorkflowNameFieldTest() throws ScriptException, NoSuchMethodException, FileNotFoundException,
                                                             WorkerException, IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        final Document builderDoc = DocumentBuilder.configure().withFields()
            .addFieldValues("CAF_WORKFLOW_ACTION", "super_action")
            .addFieldValue("FAILURES", "")
            .addFieldValue("example", "value from field")
            .addFieldValue("fieldHasValue", "This value")
            .documentBuilder()
            .withSubDocuments(DocumentBuilder.configure().withFields()
                .addFieldValue("field-should-exist", "action 2 requires this field to be present")
                .documentBuilder())
            .build();
        builderDoc.addFailure("error_id_1", "message 1");
        builderDoc.addFailure("error_id_2", "message 2");

        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataTest("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskTest(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentTest("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        invocable.invokeFunction("haveFailuresChanged", document);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void failuresNegativeNoWorkflowActionFieldTest() throws ScriptException, NoSuchMethodException, FileNotFoundException,
                                                             WorkerException, IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        final Document builderDoc = DocumentBuilder.configure().withFields()
            .addFieldValue("FAILURES", "")
            .addFieldValue("CAF_WORKFLOW_NAME", "example_workflow")
            .addFieldValue("example", "value from field")
            .addFieldValue("fieldHasValue", "This value")
            .documentBuilder()
            .withSubDocuments(DocumentBuilder.configure().withFields()
                .addFieldValue("field-should-exist", "action 2 requires this field to be present")
                .documentBuilder())
            .build();
        builderDoc.addFailure("error_id_1", "message 1");
        builderDoc.addFailure("error_id_2", "message 2");

        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataTest("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskTest(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentTest("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        invocable.invokeFunction("haveFailuresChanged", document);
    }

}
