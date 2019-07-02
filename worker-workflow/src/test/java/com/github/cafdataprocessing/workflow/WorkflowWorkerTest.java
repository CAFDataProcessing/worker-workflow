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
import com.github.cafdataprocessing.workflow.models.ApplicationMock;
import com.github.cafdataprocessing.workflow.models.DocumentMock;
import com.github.cafdataprocessing.workflow.models.InputMessageProcessorMock;
import com.github.cafdataprocessing.workflow.models.NewFailure;
import com.github.cafdataprocessing.workflow.models.SubdocumentMock;
import com.github.cafdataprocessing.workflow.models.SubdocumentsMock;
import com.github.cafdataprocessing.workflow.models.TaskMock;
import com.github.cafdataprocessing.workflow.models.WorkerTaskDataMock;
import com.github.cafdataprocessing.workflow.testing.ActionExpectationsBuilder;
import com.github.cafdataprocessing.workflow.testing.WorkflowTestExecutor;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.TaskSourceInfo;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Failure;
import com.hpe.caf.worker.document.model.Failures;
import com.hpe.caf.worker.document.model.Fields;
import com.hpe.caf.worker.document.model.InputMessageProcessor;
import com.hpe.caf.worker.document.model.Subdocument;
import com.hpe.caf.worker.document.model.Subdocuments;
import com.hpe.caf.worker.document.model.Task;
import com.hpe.caf.worker.document.scripting.events.DocumentEventObject;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.microfocus.darwin.settings.client.SettingsApi;
import static com.spotify.hamcrest.jackson.IsJsonMissing.jsonMissing;
import static com.spotify.hamcrest.jackson.IsJsonObject.jsonObject;
import static com.spotify.hamcrest.jackson.IsJsonStringMatching.isJsonStringMatching;
import static com.spotify.hamcrest.jackson.IsJsonText.jsonText;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;
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
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
    public void processFailuresTest() throws ScriptException, NoSuchMethodException, FileNotFoundException, WorkerException, IOException
    {
        // test the processFailures() function with a single failure
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
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskMock(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentMock("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        final ObjectMapper mapper = new ObjectMapper();
        final List<String> failuresRetrieved = (List<String>) invocable.invokeFunction("processFailures", document);
        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream().findFirst().get().getFailureId(), is(equalTo("error_id_1")));
        assertThat(document.getFailures().stream().findFirst().get().getFailureStack(), is(nullValue()));

        final NewFailure failureMessage = mapper.readValue(failuresRetrieved.get(0), NewFailure.class);
        assertThat(failureMessage.getFailureId(), is(equalTo("error_id_1")));;
        assertThat(failureMessage.getSource(), is(equalTo("super_action")));
        assertThat(failureMessage.getDescription(), is(equalTo("message 1")));
        assertThat(failureMessage.getVersion(), is(equalTo("5")));
        assertThat(failureMessage.getWorkflowName(), is(equalTo("example_workflow")));
        assertThat(failureMessage.getStack(), is(nullValue()));
    }
    
    @Test
    public void onAfterProcessDocumentSingleDocTest() throws ScriptException, NoSuchMethodException, FileNotFoundException,
                                                             WorkerException, IOException
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
            .build();
        builderDoc.addFailure("error_id_1", "message 1");

        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                          "to", tsi);
        final InputMessageProcessor inputMessageProcessorTest = new InputMessageProcessorMock(true);
        final Application application = new ApplicationMock(inputMessageProcessorTest);
        final Task task = new TaskMock(new HashMap<>(), null, null, wtd, null, application);
        final Document document = new DocumentMock("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc,
                                                   builderDoc);
        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);
        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream().findFirst().get().getFailureId(), is(equalTo("error_id_1")));
        assertThat(document.getFailures().stream().findFirst().get().getFailureStack(), is(nullValue()));
    }
    
    @Test
    public void onAfterProcessDocumentSingleDocWithOriginalFailuresTest() throws ScriptException, NoSuchMethodException,
                                                                                 FileNotFoundException, WorkerException, IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        // doc with one original failure an no subdocuments
        final Document builderDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-no-subdoc-with-stack.json").toString()).build();
        builderDoc.addFailure("error_id_1", "message 1");

        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                          "to", tsi);
        final InputMessageProcessor inputMessageProcessorTest = new InputMessageProcessorMock(true);
        final Application application = new ApplicationMock(inputMessageProcessorTest);
        final Task task = new TaskMock(new HashMap<>(), null, null, wtd, null, application);
        final Document document = new DocumentMock("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc,
                                                   builderDoc);
        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);
        assertThat(document.getFailures().size(), is(equalTo((2))));
        assertThat(document.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   containsInAnyOrder("error_id_1", "original_fail_id_1"));
    }
    
    @Test
    public void onAfterProcessDocumentSubDocTest() throws ScriptException, NoSuchMethodException, FileNotFoundException,
                                                             WorkerException, IOException
    {
        // test for behavior of a failure added to the root doc
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        // doc with one original failure subdocuments
        final Document builderDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-with-subdoc-with-stack.json").toString()).build();
        builderDoc.addFailure("error_id_1", "message 1");

        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();
        final Subdocuments subdocuments = builderDoc.getSubdocuments();
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                          "to", tsi);
        final InputMessageProcessor inputMessageProcessorTest = new InputMessageProcessorMock(true);
        final Application application = new ApplicationMock(inputMessageProcessorTest);
        final Task task = new TaskMock(new HashMap<>(), null, null, wtd, null, application);
        final Document document = new DocumentMock("ref_1", fields, task, new HashMap<>(), failures, subdocuments, null, builderDoc,
                                                   builderDoc);
        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);
        assertThat(document.getFailures().size(), is(equalTo((2))));
        assertThat(document.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   containsInAnyOrder("error_id_1", "original_fail_id_1"));
        final Subdocument firstSubdoc = document.getSubdocuments().stream().filter(s->s.getReference().equals("ref_1_subdoc"))
            .findFirst().get();
        final Subdocument secondSubdoc = document.getSubdocuments().stream().filter(s->s.getReference().equals("ref_2_subdoc"))
            .findFirst().get();
        assertThat(firstSubdoc.getFailures().size(), is(equalTo(1)));
        assertThat(firstSubdoc.getFailures().stream().map(f->f.getFailureId()).findFirst().get(), 
                   is(equalTo("original_fail_subdoc_id_1")));
        assertThat(secondSubdoc.getFailures().size(), is(equalTo(1)));
        assertThat(secondSubdoc.getFailures().stream().map(f->f.getFailureId()).findFirst().get(), 
                   is(equalTo("original_fail_subdoc_id_2")));
    }
    
    @Test
    public void onAfterProcessDocumentSubDocNotProcessedSeparatelyTest() throws ScriptException, NoSuchMethodException,
                                                                                FileNotFoundException, WorkerException, IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        // doc with one original failure subdocuments
        final Document builderDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-no-subdoc-with-stack.json").toString()).build();
        builderDoc.addFailure("error_id_1", "message 1");

        // objs created only to get the failures
        final Document builderForFailuresOne = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-subdocument-no-subdoc-with-stack-1.json").toString()).build();
        builderForFailuresOne.addFailure("error_id_2", "message 2");
        final Document builderForFailuresTwo = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-subdocument-no-subdoc-with-stack-2.json").toString()).build();
        builderForFailuresTwo.addFailure("error_id_3", "message 3");
        
        final Fields fieldsFirstSubDoc = builderForFailuresOne.getFields();
        final Fields fieldsSecondSubDoc = builderForFailuresTwo.getFields();
        final Failures failuresFirstSubDoc = builderForFailuresOne.getFailures();
        final Failures failuresSecondSubDoc = builderForFailuresTwo.getFailures();
        
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                          "to", tsi);
        final InputMessageProcessor inputMessageProcessorTest = new InputMessageProcessorMock(false);
        final Application application = new ApplicationMock(inputMessageProcessorTest);
        final TaskMock task = new TaskMock(new HashMap<>(), null, null, wtd, null, application);
        
        final Subdocument subdocOne = new SubdocumentMock("subd_ref_1", fieldsFirstSubDoc, task, new HashMap<>(),
                                                          failuresFirstSubDoc, null, application, builderDoc, builderDoc);
        final Subdocument subdocTwo = new SubdocumentMock("subd_ref_2", fieldsSecondSubDoc, task, new HashMap<>(),
                                                          failuresSecondSubDoc, null, application, builderDoc, builderDoc);

        final Subdocuments subdocuments = new SubdocumentsMock(Arrays.asList(subdocOne, subdocTwo));
        
        final Fields fields = builderDoc.getFields();
        final Failures failures = builderDoc.getFailures();

        final Document document = new DocumentMock("ref_1", fields, task, new HashMap<>(), failures, subdocuments, null, builderDoc,
                                                   builderDoc);
        task.setDocument(document);
        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);
        assertThat(document.getFailures().size(), is(equalTo((2))));
        assertThat(document.getFailures().stream().map(f->f.getFailureId()).collect(toList()), containsInAnyOrder("error_id_1", 
                                                                                                                  "original_fail_id_1"));
        assertThat(document.getFailures().stream().map(f->f.getFailureStack()).collect(toList()), hasItems("super stack"));
        assertThat(document.getSubdocuments().stream().filter(sub
            -> sub.getReference().equals("subd_ref_1")).flatMap(s->s.getFailures().stream()).map(f->f.getFailureId()).collect(toList()),
                   containsInAnyOrder("error_id_2", "original_fail_id_2_sub"));
        
        final Failure firstFailure = document.getSubdocuments()
            .stream()
            .filter(sub -> sub.getReference().equals("subd_ref_1"))
            .flatMap(s->s.getFailures().stream())
            .filter(i->i.getFailureId().equals("error_id_2"))
            .findFirst()
            .get();
        
        assertThat(firstFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("id", is(jsonText("error_id_2")))));
        assertThat(firstFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("stack", is(jsonMissing()))));
        assertThat(firstFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("source", is(jsonText("super_action")))));
        assertThat(firstFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("version", is(jsonText("5")))));
        assertThat(firstFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("workflowName", is(jsonText("example_workflow")))));

        
        assertThat(document.getSubdocuments().stream().filter(sub
            -> sub.getReference().equals("subd_ref_2")).flatMap(s->s.getFailures().stream()).map(f->f.getFailureId()).collect(toList()),
                   containsInAnyOrder("error_id_3", "original_fail_id_3_sub"));
        
        final Failure secondFailure = document.getSubdocuments()
            .stream()
            .filter(sub -> sub.getReference().equals("subd_ref_2"))
            .flatMap(s->s.getFailures().stream())
            .filter(i->i.getFailureId().equals("error_id_3"))
            .findFirst()
            .get();
        
        assertThat(secondFailure.getFailureId(),
                   is(equalTo("error_id_3")));
        assertThat(secondFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("id", is(jsonText("error_id_3")))));
        assertThat(secondFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("stack", is(jsonMissing()))));
        assertThat(secondFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("source", is(jsonText("super_action")))));
        assertThat(secondFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("version", is(jsonText("5")))));
        assertThat(secondFailure.getFailureMessage(),
                   isJsonStringMatching(jsonObject().where("workflowName", is(jsonText("example_workflow")))));
        
        final int sum = document.getFailures().size() + document.getSubdocuments().stream().mapToInt(s -> s.getFailures().size()).sum();
        assertThat(sum, is(equalTo(6)));
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
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskMock(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentMock("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        final ObjectMapper mapper = new ObjectMapper();
        final List<String> failuresRetrieved = (List<String>) invocable.invokeFunction("processFailures", document);
        assertThat(document.getFailures().size(), is(equalTo((2))));
        assertThat(document.getFailures().stream().map(f -> f.getFailureId()).collect(toList()), contains("error_id_1", "error_id_2"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureStack()).filter(s -> !StringUtils.isEmpty(s)).collect(toList()),
                   is(emptyCollectionOf(String.class)));

        for (int i = 0; i < failuresRetrieved.size(); i++) {
            final NewFailure failureMessage = mapper.readValue(failuresRetrieved.get(i), NewFailure.class);
            assertThat(failureMessage.getFailureId(), isOneOf("error_id_1", "error_id_2"));
            assertThat(failureMessage.getSource(), is(equalTo("super_action")));
            assertThat(failureMessage.getDescription(), isOneOf("message 1", "message 2"));
            assertThat(failureMessage.getVersion(), is(equalTo("5")));
            assertThat(failureMessage.getWorkflowName(), is(equalTo("example_workflow")));
            assertThat(failureMessage.getStack(), is(nullValue()));
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
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskMock(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentMock("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        invocable.invokeFunction("processFailures", document);
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
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskMock(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentMock("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        invocable.invokeFunction("processFailures", document);
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
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                       "to", tsi);
        final Task task = new TaskMock(new HashMap<>(), null, null, wtd, null, null);
        final Document document = new DocumentMock("ref_1", fields, task, new HashMap<>(), failures, null, null, builderDoc, builderDoc);

        invocable.invokeFunction("processFailures", document);
    }
    
    @Test
    public void isFailureInOriginalTest() throws FileNotFoundException, ScriptException, NoSuchMethodException, WorkerException{
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

        final Failures failures = builderDoc.getFailures();
        
        final Boolean invokeFunction = (Boolean) invocable.invokeFunction("isFailureInOriginal",
                                                                          failures, failures.stream().findFirst().get());
        assertThat(invokeFunction, is(true));
    }
    
    @Test
    public void isFailureInOriginalFourTest() throws FileNotFoundException, ScriptException, NoSuchMethodException, WorkerException{
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

        final Failures failures = builderDoc.getFailures();
        
        final Document builderDocTwo = DocumentBuilder.configure().withFields()
            .addFieldValue("FAILURES", "")
            .addFieldValue("CAF_WORKFLOW_NAME", "example_workflow")
            .addFieldValue("example", "value from field")
            .addFieldValue("fieldHasValue", "This value")
            .documentBuilder()
            .withSubDocuments(DocumentBuilder.configure().withFields()
                .addFieldValue("field-should-exist", "action 2 requires this field to be present")
                .documentBuilder())
            .build();
        builderDocTwo.addFailure("error_id_new", null);
        builderDocTwo.addFailure("error_id_new", null);

        final Failures failuresTwo = builderDocTwo.getFailures();
        
        final Boolean invokeFunction = (Boolean) invocable.invokeFunction("isFailureInOriginal",
                                                                          failures, failuresTwo.stream().findFirst().get());
        assertThat(invokeFunction, is(false));
    }
    
    @Test
    public void isFailureInOriginalFileIdComparisonTest() throws FileNotFoundException, ScriptException, NoSuchMethodException,
                                                                 WorkerException, IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        // doc with one original failure an no subdocuments
        final Document document = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-no-subdoc.json").toString()).build();

        // add new failures
        document.addFailure("error_id_new", "error message");
        document.addFailure("error_id_new_2", "error message 2");

        // get all failures
        final List<Failure> listOfFailures = document.getFailures().stream().collect(toList());

        // reset and get only the original ones
        document.getFailures().reset();
        final Failures failures = document.getFailures();

        for (final Failure failure : listOfFailures) {
            if (failure.getFailureId().equals("original_fail_id_1")) {
                Boolean invokeFunction = (Boolean) invocable.invokeFunction("isFailureInOriginal", failures, failure);
                assertThat(invokeFunction, is(true));
            } else {
                Boolean invokeFunction = (Boolean) invocable.invokeFunction("isFailureInOriginal", failures, failure);
                assertThat(invokeFunction, is(false));
            }
        }
        
        // add a new failure with same id of the original one
        document.addFailure("original_fail_id_1", "I will not be readded");

        // get all failures
        final List<Failure> listOfFailuresTwo = document.getFailures().stream().collect(toList());

        // reset and get only the original ones
        document.getFailures().reset();
        final Failures failuresTwo = document.getFailures();

        for (final Failure failure : listOfFailuresTwo) {
            if (failure.getFailureId().equals("original_fail_id_1") && failure.getFailureMessage().equals("original message 1")) {
                final Boolean invokeFunction = (Boolean) invocable.invokeFunction("isFailureInOriginal", failuresTwo, failure);
                assertThat(invokeFunction, is(true));
            } else {
                final Boolean invokeFunction = (Boolean) invocable.invokeFunction("isFailureInOriginal", failuresTwo, failure);
                // false because different at message level
                assertThat(invokeFunction, is(false));
            }
        }
    }
    
    @Test
    public void isFailureInOriginalStackComparisonTest() throws FileNotFoundException, ScriptException, NoSuchMethodException,
                                                                 WorkerException, IOException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "test", "resources", "workflow-control-test.js")
            .toFile())));
        final Invocable invocable = (Invocable) nashorn;

        // doc with one original failure an no subdocuments
        final Document document = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-no-subdoc-with-stack.json").toString()).build();

        // add new failure with same id and message
        document.addFailure("original_fail_id_1", "original message 1");

        // get all failures
        final List<Failure> listOfFailures = document.getFailures().stream().collect(toList());

        // reset and get only the original ones
        document.getFailures().reset();
        final Failures failures = document.getFailures();

        for (final Failure failure : listOfFailures) {
            if (failure.getFailureId().equals("original_fail_id_1") && failure.getFailureMessage().equals("original message 1")
                && failure.getFailureStack() != null) {
                final Boolean invokeFunction = (Boolean) invocable.invokeFunction("isFailureInOriginal", failures, failure);
                assertThat(invokeFunction, is(true));
            } else {
                final Boolean invokeFunction = (Boolean) invocable.invokeFunction("isFailureInOriginal", failures, failure);
                assertThat(invokeFunction, is(false));
            }
        }

    }

}
