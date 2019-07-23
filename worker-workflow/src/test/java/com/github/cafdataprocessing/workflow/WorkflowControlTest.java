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
import com.github.cafdataprocessing.workflow.models.actions.ActionMock;
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
import com.hpe.caf.worker.document.scripting.events.DocumentEventObject;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import static com.spotify.hamcrest.jackson.IsJsonMissing.jsonMissing;
import static com.spotify.hamcrest.jackson.IsJsonNull.jsonNull;
import static com.spotify.hamcrest.jackson.IsJsonObject.jsonObject;
import static com.spotify.hamcrest.jackson.IsJsonStringMatching.isJsonStringMatching;
import static com.spotify.hamcrest.jackson.IsJsonText.jsonText;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import static java.util.stream.Collectors.toList;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.IsNull.nullValue;
import org.junit.Test;

public class WorkflowControlTest
{
    @Test
    public void processFailuresTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // test the processFailures() function with a single failure and no original ones

        // get an invocable Nashorn engine
        final Invocable invocable = createInvocableNashornEngine();

        // get a base document used to fill in the basic structure
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
        // add one failure
        builderDoc.addFailure("error_id_1", "message 1");

        // create the various mocked objects to create the document that will be processed
        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), null, null, null,
                                                 false, false);

        invocable.invokeFunction("processFailures", document);

        assertThat(document.getFailures().size(), is(equalTo((0))));

        assertThat(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));

        final String mainFailure = document.getField("FAILURES").getValues()
            .stream()
            .filter(v -> !v.getStringValue().isEmpty())
            .map(v -> v.getStringValue())
            .findFirst()
            .get();

        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));
    }

    @Test
    public void multipleFailuresPositiveTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // test processFailures() function with multiple failures and no original ones

        final Invocable invocable = createInvocableNashornEngine();

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
        // add 2 failures
        builderDoc.addFailure("error_id_1", "message 1");
        builderDoc.addFailure("error_id_2", "message 2");

        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), null, builderDoc, builderDoc,
                                                 false, true);

        invocable.invokeFunction("processFailures", document);

        assertThat(document.getFailures().size(), is(equalTo((0))));

        assertThat(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((2L))));

        final String firstFailure = document.getField("FAILURES").getValues()
            .stream()
            .filter(v -> !v.getStringValue().isEmpty() && v.getStringValue().contains("message 1"))
            .map(v -> v.getStringValue())
            .findFirst()
            .get();

        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_1")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 1")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        final String secondFailure = document.getField("FAILURES").getValues()
            .stream()
            .filter(v -> !v.getStringValue().isEmpty() && v.getStringValue().contains("message 2"))
            .map(v -> v.getStringValue())
            .findFirst()
            .get();

        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_2")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 2")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));
    }

    @Test(expected = NoSuchElementException.class)
    public void failuresNegativeNoFailuresFieldTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // this method fails because the FAILURES field is not present
        final Invocable invocable = createInvocableNashornEngine();

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

        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), null, builderDoc, builderDoc,
                                                 false, true);

        invocable.invokeFunction("processFailures", document);
    }

    @Test(expected = NoSuchElementException.class)
    public void failuresNegativeNoWorkflowNameFieldTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // this method fails because there is not the CAF_WORKFLOW_NAME field
        final Invocable invocable = createInvocableNashornEngine();

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

        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), null, builderDoc, builderDoc,
                                                 false, true);

        invocable.invokeFunction("processFailures", document);
    }

    @Test(expected = NoSuchElementException.class)
    public void failuresNegativeNoWorkflowActionFieldTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // this method fails because there is not a CAF_WORKFLOW_ACTION field
        final Invocable invocable = createInvocableNashornEngine();

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

        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), null, builderDoc, builderDoc,
                                                 false, true);

        invocable.invokeFunction("processFailures", document);
    }

    @Test
    public void isFailureInOriginalTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // test for the isFailureInOriginal() function
        final Invocable invocable = createInvocableNashornEngine();

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
    public void isFailureInOriginalFalseTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // the function return false, because the failure is not in the original list
        final Invocable invocable = createInvocableNashornEngine();

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
        builderDocTwo.addFailure("error_id_new2", null);

        final Failures failuresTwo = builderDocTwo.getFailures();

        final Boolean invokeFunction = (Boolean) invocable.invokeFunction("isFailureInOriginal",
                                                                          failures, failuresTwo.stream().findFirst().get());
        assertThat(invokeFunction, is(false));
    }

    @Test
    public void isFailureInOriginalFileIdComparisonTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // checks that isFailureInOriginal() returns false even if the ids are the same (but the messages are different)

        final Invocable invocable = createInvocableNashornEngine();

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
    public void isFailureInOriginalStackComparisonTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // checks that isFailureInOriginal() returns false even if the ids and the messages are the same (but the stacks are different)

        final Invocable invocable = createInvocableNashornEngine();

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
                // false because the original failure has some value in the stack
                assertThat(invokeFunction, is(false));
            }
        }
    }

    @Test
    public void onAfterProcessDocumentSingleDocTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // test onAfterProcessDocument() with a single document, no need to call processSubdocumentFailures() and a single failure
        final Invocable invocable = createInvocableNashornEngine();

        final Document builderDoc = DocumentBuilder.configure().withFields()
            .addFieldValues("CAF_WORKFLOW_ACTION", "super_action")
            .addFieldValue("CAF_WORKFLOW_NAME", "example_workflow")
            .addFieldValue("FAILURES", "")
            .addFieldValue("example", "value from field")
            .addFieldValue("fieldHasValue", "This value")
            .documentBuilder()
            .build();
        builderDoc.addFailure("error_id_1", "message 1");

        // processSubdocumentFailures() not called
        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), null, null, null,
                                                 true, true);
        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);

        assertThat(document.getFailures().size(), is(equalTo((0))));

        assertThat(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));
        final ObjectMapper mapper = new ObjectMapper();
        final NewFailure failureMessage = mapper.readValue(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).findFirst().get().getStringValue(), NewFailure.class);
        assertThat(failureMessage.getFailureId(), is(equalTo("error_id_1")));
        assertThat(failureMessage.getStack(), is(nullValue()));
        assertThat(failureMessage.getMessage(), is(equalTo("message 1")));
        assertThat(failureMessage.getVersion(), is(equalTo("source_name 5")));
        assertThat(failureMessage.getWorkflowName(), is(equalTo("example_workflow")));
        assertThat(failureMessage.getWorkflowAction(), is(equalTo("super_action")));
        assertThat(failureMessage.getDate(), is(not(isEmptyString())));
    }

    @Test
    public void onAfterProcessDocumentSingleDocWithOriginalFailuresTest() throws ScriptException, NoSuchMethodException,
                                                                                 WorkerException, IOException
    {
        // test onAfterProcessDocument() with a single document, no need to call processSubdocumentFailures(), a single failure and an
        // original one
        final Invocable invocable = createInvocableNashornEngine();

        // doc with one original failure an no subdocuments
        final Document builderDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-no-subdoc-with-stack.json").toString()).build();
        builderDoc.addFailure("error_id_1", "message 1");

        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), null, builderDoc, builderDoc,
                                                 true, true);
        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);

        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   containsInAnyOrder("original_fail_id_1"));
        // the original message is the same because we are only processing new failures
        assertThat(document.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   containsInAnyOrder("original message 1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));

        final String firstFailure = document.getField("FAILURES").getValues()
            .stream()
            .filter(v -> !v.getStringValue().isEmpty())
            .map(v -> v.getStringValue())
            .findFirst()
            .get();

        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_1")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 1")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));
    }

    @Test
    public void onAfterProcessDocumentSubdocTest() throws ScriptException, NoSuchMethodException, WorkerException, IOException
    {
        // test onAfterProcessDocument() with a document with subdocuments, no need to call processSubdocumentFailures(),
        // a single failure and an original one
        final Invocable invocable = createInvocableNashornEngine();

        // doc with one original failure and subdocuments
        final Document builderDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-with-subdoc-with-stack.json").toString()).build();
        builderDoc.addFailure("error_id_1", "message 1");

        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), builderDoc.getSubdocuments(),
                                                 builderDoc, builderDoc, true, true);
        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);

        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   containsInAnyOrder("original_fail_id_1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   containsInAnyOrder("original message 1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));

        final String firstFailure = document.getField("FAILURES").getValues()
            .stream()
            .filter(v -> !v.getStringValue().isEmpty())
            .map(v -> v.getStringValue())
            .findFirst()
            .get();

        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_1")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 1")))));
        assertThat(firstFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        // retrieve the subdocs and check that the original failures are still there
        final Subdocument firstSubdoc = document.getSubdocuments().stream().filter(s -> s.getReference().equals("ref_1_subdoc"))
            .findFirst().get();
        final Subdocument secondSubdoc = document.getSubdocuments().stream().filter(s -> s.getReference().equals("ref_2_subdoc"))
            .findFirst().get();
        assertThat(firstSubdoc.getFailures().size(), is(equalTo(1)));
        assertThat(firstSubdoc.getFailures().stream().map(f -> f.getFailureId()).findFirst().get(),
                   is(equalTo("original_fail_subdoc_id_1")));
        assertThat(firstSubdoc.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((0L))));

        assertThat(secondSubdoc.getFailures().size(), is(equalTo(1)));
        assertThat(secondSubdoc.getFailures().stream().map(f -> f.getFailureId()).findFirst().get(),
                   is(equalTo("original_fail_subdoc_id_2")));
        assertThat(secondSubdoc.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((0L))));
    }

    @Test
    public void onAfterProcessDocumentMultipleLevelOfSubdocsTest() throws ScriptException, NoSuchMethodException,
                                                                          WorkerException, IOException
    {
        // test onAfterProcessDocument() with a document with subdocuments, no need to call processSubdocumentFailures(),
        // some original failures at various levels and new ones added at all levels
        final Invocable invocable = createInvocableNashornEngine();

        // doc with one original failure subdocuments
        final Document builderDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-with-multiple-levels-subdoc-with-stack.json").toString()).build();
        // add one new failure to the root doc
        builderDoc.addFailure("error_id_1", "message 1");

        final Subdocuments subdocuments = builderDoc.getSubdocuments();
        // add a failure to the lowest inner level
        subdocuments.stream()
            .filter(sub -> sub.getReference().equals("ref_level_2_sub_2"))
            .flatMap(doc -> doc.getSubdocuments().stream())
            .filter(sub2 -> sub2.getReference().equals("ref_level_3_sub_1"))
            .flatMap(doc2 -> doc2.getSubdocuments().stream())
            .filter(sub3 -> sub3.getReference().equals("ref_level_4_sub_1"))
            .findFirst()
            .get()
            .addFailure("level_4_id", "level 4 failure");
        //add one to the third level
        subdocuments.stream()
            .filter(sub -> sub.getReference().equals("ref_level_2_sub_2"))
            .flatMap(doc -> doc.getSubdocuments().stream())
            .filter(sub2 -> sub2.getReference().equals("ref_level_3_sub_2"))
            .findFirst()
            .get()
            .addFailure("level_3_id", "level 3 failure");
        //add one to the level two
        subdocuments.stream()
            .filter(sub -> sub.getReference().equals("ref_level_2_sub_2"))
            .findFirst()
            .get()
            .addFailure("level_2_id", "level 2 failure");

        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), subdocuments, builderDoc,
                                                 builderDoc, true, true);
        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);

        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   containsInAnyOrder("original_fail_id_1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   containsInAnyOrder("original message 1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));
        final String mainFailure = document.getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        //level 2
        final Subdocument firstSubdocLevel2 = document.getSubdocuments()
            .stream()
            .filter(s -> s.getReference().equals("ref_level_2_sub_1"))
            .findFirst()
            .get();
        final Subdocument secondSubdocLevel2 = document.getSubdocuments()
            .stream()
            .filter(s -> s.getReference().equals("ref_level_2_sub_2"))
            .findFirst()
            .get();
        assertThat(firstSubdocLevel2.getFailures().size(), is(equalTo(1)));
        assertThat(firstSubdocLevel2.getFailures().stream().map(f -> f.getFailureId()).findFirst().get(),
                   is(equalTo("original_fail_subdoc_id_1")));
        assertThat(firstSubdocLevel2.getFailures().stream().map(f -> f.getFailureMessage()).findFirst().get(),
                   is(equalTo("original message 1")));
        assertThat(firstSubdocLevel2.getFailures().stream().map(f -> f.getFailureStack()).findFirst().get(),
                   is(equalTo("super stack")));
        assertThat(secondSubdocLevel2.getFailures().size(), is(equalTo(2)));
        assertThat(secondSubdocLevel2.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   hasItems("original_fail_subdoc_id_2", "level_2_id"));
        assertThat(secondSubdocLevel2.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   hasItems("level 2 failure", "original message 1"));
        assertThat(secondSubdocLevel2.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack", null));

        assertThat(firstSubdocLevel2.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((0L))));

        // the next one is 0, because we are assuming that the worker will process each subdocument, and we are not calling 
        // processSubdocumentFailures() in this test
        assertThat(secondSubdocLevel2.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((0L))));

        // level 3
        final Subdocument firstSubdocLevel3 = secondSubdocLevel2.getSubdocuments().stream()
            .filter(s -> s.getReference().equals("ref_level_3_sub_1")).findFirst().get();
        assertThat(firstSubdocLevel3.getFailures().size(), is(equalTo(1)));
        assertThat(firstSubdocLevel3.getFailures().stream().map(f -> f.getFailureId()).findFirst().get(),
                   is(equalTo("original_fail_level_3_sub1")));
        assertThat(firstSubdocLevel3.getFailures().stream().map(f -> f.getFailureMessage()).findFirst().get(),
                   is(equalTo("original message 1")));
        assertThat(firstSubdocLevel3.getFailures().stream().map(f -> f.getFailureStack()).findFirst().get(),
                   is(equalTo("super stack")));
        final Subdocument secondSubdocLevel3 = secondSubdocLevel2.getSubdocuments().stream()
            .filter(s -> s.getReference().equals("ref_level_3_sub_2")).findFirst().get();
        assertThat(secondSubdocLevel3.getFailures().size(), is(equalTo(2)));
        assertThat(secondSubdocLevel3.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   hasItems("original_fail_level_3_sub2", "level_3_id"));
        assertThat(secondSubdocLevel3.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   hasItems("level 3 failure", "original message 1"));
        assertThat(secondSubdocLevel3.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack", null));

        assertThat(firstSubdocLevel3.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((0L))));

        // the next one is 0, because we are assuming that the worker will process each subdocument, and we are not calling 
        // processSubdocumentFailures() in this test
        assertThat(secondSubdocLevel3.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((0L))));

        // level 4
        final Subdocument firstSubdocLevel4 = firstSubdocLevel3.getSubdocuments().stream()
            .filter(s -> s.getReference().equals("ref_level_4_sub_1")).findFirst().get();
        assertThat(firstSubdocLevel4.getFailures().size(), is(equalTo(2)));
        assertThat(firstSubdocLevel4.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   hasItems("original_fail_level_4_sub1", "level_4_id"));

        assertThat(firstSubdocLevel4.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   hasItems("original_fail_level_4_sub1", "level_4_id"));
        assertThat(firstSubdocLevel4.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   hasItems("level 4 failure", "original message 1"));
        assertThat(firstSubdocLevel4.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack", null));

        // the next one is 0, because we are assuming that the worker will process each subdocument, and we are not calling 
        // processSubdocumentFailures() in this test
        assertThat(firstSubdocLevel4.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((0L))));
    }

    @Test
    public void onAfterProcessDocumentSubDocNotProcessedSeparatelyTest() throws ScriptException, NoSuchMethodException,
                                                                                WorkerException, IOException
    {
        // test onAfterProcessDocument() with a document with subdocuments, it WILL call processSubdocumentFailures(),
        // some original failures at various levels and new ones added at all levels
        final Invocable invocable = createInvocableNashornEngine();

        // doc with one original failure and NO subdos
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

        // create the subdocuments
        final Subdocument subdocOne = createSubdocument("subd_ref_1", builderForFailuresOne.getFields(),
                                                        builderForFailuresOne.getFailures(), null,
                                                        null, null, true, false);

        final Subdocument subdocTwo = createSubdocument("subd_ref_2", builderForFailuresTwo.getFields(),
                                                        builderForFailuresTwo.getFailures(), null,
                                                        null, null, true, false);

        final Subdocuments subdocuments = new SubdocumentsMock(Arrays.asList(subdocOne, subdocTwo));

        // create the test document that wil contain subdocuments
        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), subdocuments, null,
                                                 null, true, false);

        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);

        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream()
            .map(f -> f.getFailureId()).collect(toList()), containsInAnyOrder("original_fail_id_1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   containsInAnyOrder("original message 1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));
        final String mainFailure = document.getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        // subdocuments
        final Subdocument firstSubdoc = document.getSubdocuments()
            .stream()
            .filter(s -> s.getReference().equals("subd_ref_1"))
            .findFirst()
            .get();
        assertThat(firstSubdoc.getFailures().size(), is(equalTo(1)));
        assertThat(firstSubdoc.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   hasItems("original_fail_id_2_sub"));
        assertThat(firstSubdoc.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   hasItems("original message 2"));
        assertThat(firstSubdoc.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(firstSubdoc.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));

        final String secondFailure = firstSubdoc.getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_2")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 2")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        final Subdocument secondSubdoc = document.getSubdocuments()
            .stream()
            .filter(s -> s.getReference().equals("subd_ref_2"))
            .findFirst()
            .get();
        assertThat(secondSubdoc.getFailures().size(), is(equalTo(1)));
        assertThat(secondSubdoc.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   hasItems("original_fail_id_3_sub"));
        assertThat(secondSubdoc.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   hasItems("original message 3"));
        assertThat(secondSubdoc.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(secondSubdoc.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));

        final String thirdFailure = secondSubdoc.getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_3")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 3")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        final int sum = document.getFailures().size() + document.getSubdocuments().stream().mapToInt(s -> s.getFailures().size()).sum();
        assertThat(sum, is(equalTo(3)));

        final int sumField = document.getField("FAILURES").getValues().size()
            + document.getSubdocuments().stream().mapToInt(s -> s.getField("FAILURES").getValues().size()).sum();
        assertThat(sumField, is(equalTo(3)));
    }

    @Test
    public void onAfterProcessDocumentSubDocNotProcessedSeparatelyWithoutSubdocsTest() throws ScriptException, NoSuchMethodException,
                                                                                              WorkerException, IOException
    {
        // test onAfterProcessDocument() with a document WITHOUT subdocuments, it WILL call processSubdocumentFailures(),
        // some original failures at various levels and new ones added at all levels
        final Invocable invocable = createInvocableNashornEngine();

        // doc with one original failure and NO subdos
        final Document builderDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-no-subdoc-with-stack.json").toString()).build();
        builderDoc.addFailure("error_id_1", "message 1");

        // create the test document that will NOT contain subdocuments
        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), null, null,
                                                 null, true, false);

        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);

        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream()
            .map(f -> f.getFailureId()).collect(toList()), containsInAnyOrder("original_fail_id_1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   containsInAnyOrder("original message 1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));
        final String mainFailure = document.getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));
    }

    @Test
    public void onAfterProcessDocumentSubDocNotProcessedSeparatelyRealRootAndParentTest() throws ScriptException, NoSuchMethodException,
                                                                                                 WorkerException, IOException
    {
        // test onAfterProcessDocument() with a document with subdocuments, it WILL call processSubdocumentFailures(),
        // some original failures at various levels and new ones added at all levels
        final Invocable invocable = createInvocableNashornEngine();

        // doc with one original failure and NO subdos
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

        // create the subdocuments
        final Subdocument subdocOne = createSubdocument("subd_ref_1", builderForFailuresOne.getFields(),
                                                        builderForFailuresOne.getFailures(), null,
                                                        null, null, true, false);

        final Subdocument subdocTwo = createSubdocument("subd_ref_2", builderForFailuresTwo.getFields(),
                                                        builderForFailuresTwo.getFailures(), null,
                                                        null, null, true, false);

        final Subdocuments subdocuments = new SubdocumentsMock(Arrays.asList(subdocOne, subdocTwo));

        // root document
        final Document rootDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-root-document-no-subdoc-with-stack.json").toString()).build();
        rootDoc.addFailure("error_id_root", "message root 1");
        final Document rootDocument = createDocument("ref_root", rootDoc.getFields(), rootDoc.getFailures(), null, null,
                                                 null, true, false);
        // root document
        final Document parentDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-parent-document-no-subdoc-with-stack.json").toString()).build();
        parentDoc.addFailure("error_id_parent", "message parent 1");
        final Document parentDocument = createDocument("ref_parent", parentDoc.getFields(), parentDoc.getFailures(), null,
                                                       null, null, true, false);

        // create the test document that wil contain subdocuments
        final Document document = createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(), subdocuments, parentDocument,
                                                 rootDocument, true, false);

        final DocumentEventObject documentEventObject = new DocumentEventObject(document);
        //documentEventObject.rootDocument.
        invocable.invokeFunction("onAfterProcessDocument", documentEventObject);

        assertThat(document.getFailures().size(), is(equalTo((1))));
        assertThat(document.getFailures().stream()
            .map(f -> f.getFailureId()).collect(toList()), containsInAnyOrder("original_fail_id_1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   containsInAnyOrder("original message 1"));
        assertThat(document.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(document.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));
        final String mainFailure = document.getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 1")))));
        assertThat(mainFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        // subdocuments
        final Subdocument firstSubdoc = document.getSubdocuments()
            .stream()
            .filter(s -> s.getReference().equals("subd_ref_1"))
            .findFirst()
            .get();
        assertThat(firstSubdoc.getFailures().size(), is(equalTo(1)));
        assertThat(firstSubdoc.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   hasItems("original_fail_id_2_sub"));
        assertThat(firstSubdoc.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   hasItems("original message 2"));
        assertThat(firstSubdoc.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(firstSubdoc.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));

        final String secondFailure = firstSubdoc.getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_2")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 2")))));
        assertThat(secondFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        final Subdocument secondSubdoc = document.getSubdocuments()
            .stream()
            .filter(s -> s.getReference().equals("subd_ref_2"))
            .findFirst()
            .get();
        assertThat(secondSubdoc.getFailures().size(), is(equalTo(1)));
        assertThat(secondSubdoc.getFailures().stream().map(f -> f.getFailureId()).collect(toList()),
                   hasItems("original_fail_id_3_sub"));
        assertThat(secondSubdoc.getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   hasItems("original message 3"));
        assertThat(secondSubdoc.getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(secondSubdoc.getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));

        final String thirdFailure = secondSubdoc.getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_3")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message 3")))));
        assertThat(thirdFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        final int sum = document.getFailures().size() + document.getSubdocuments().stream().mapToInt(s -> s.getFailures().size()).sum();
        assertThat(sum, is(equalTo(3)));

        final int sumField = document.getField("FAILURES").getValues().size()
            + document.getSubdocuments().stream().mapToInt(s -> s.getField("FAILURES").getValues().size()).sum();
        assertThat(sumField, is(equalTo(3)));

        // root doc
        assertThat(document.getRootDocument().getFailures().size(), is(equalTo((0))));

        assertThat(document.getRootDocument().getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));
        final String rootFailure = document.getRootDocument().getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(rootFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_root")))));
        assertThat(rootFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(rootFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(rootFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(rootFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(rootFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message root 1")))));
        assertThat(rootFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));

        // parent doc
        assertThat(document.getParentDocument().getFailures().size(), is(equalTo((1))));
        assertThat(document.getParentDocument().getFailures().stream()
            .map(f -> f.getFailureId()).collect(toList()), containsInAnyOrder("original_fail_parent_doc"));
        assertThat(document.getParentDocument().getFailures().stream().map(f -> f.getFailureMessage()).collect(toList()),
                   containsInAnyOrder("original message parent doc"));
        assertThat(document.getParentDocument().getFailures().stream().map(f -> f.getFailureStack()).collect(toList()),
                   containsInAnyOrder("super stack"));

        assertThat(document.getParentDocument().getField("FAILURES").getValues()
            .stream().filter(x -> !x.getStringValue().isEmpty()).count(), is(equalTo((1L))));
        final String parentFailure = document.getParentDocument().getField("FAILURES").getValues()
            .stream()
            .map(i -> i.getStringValue())
            .findFirst()
            .get();

        assertThat(parentFailure,
                   isJsonStringMatching(jsonObject().where("ID", is(jsonText("error_id_parent")))));
        assertThat(parentFailure,
                   isJsonStringMatching(jsonObject().where("STACK", is(jsonMissing()))));
        assertThat(parentFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_ACTION", is(jsonText("super_action")))));
        assertThat(parentFailure,
                   isJsonStringMatching(jsonObject().where("VERSION", is(jsonText("source_name 5")))));
        assertThat(parentFailure,
                   isJsonStringMatching(jsonObject().where("WORKFLOW_NAME", is(jsonText("example_workflow")))));
        assertThat(parentFailure,
                   isJsonStringMatching(jsonObject().where("MESSAGE", is(jsonText("message parent 1")))));
        assertThat(parentFailure,
                   isJsonStringMatching(jsonObject().where("DATE", is(not(jsonNull())))));
    }

    private Invocable createInvocableNashornEngine() throws IOException, ScriptException
    {
        final ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        final ObjectMapper mapper = new ObjectMapper();
        final ActionMock action = mapper.readValue(Paths.get("src", "test", "resources", "action.json").toFile(), ActionMock.class);
        nashorn.getContext().setAttribute("ACTIONS", action, ScriptContext.ENGINE_SCOPE);
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "main", "resources", "workflow-control.js")
            .toFile())));
        return (Invocable) nashorn;
    }

    /**
     * Utility method to create a document.
     *
     * @param reference just the reference of the main doc
     * @param fields values in fields
     * @param failures values in failures
     * @param subdocuments the subdocuments to be added
     * @param parentDoc the parent doc
     * @param rootDoc the root doc
     * @param includeApplication does the test need an application object?
     * @param inputMessageProcessor this param has only a meaning if the include application is true. If set to true, we assume that the
     * worker will handle all subdocuments for us, if false it will not, and the workflow-control script has to do it for us.
     * @return a Document
     */
    private Document createDocument(final String reference, final Fields fields, final Failures failures, final Subdocuments subdocuments,
                                    final Document parentDoc, final Document rootDoc, final boolean includeApplication,
                                    final boolean inputMessageProcessor)
    {
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                          "to", tsi);
        final TaskMock task;
        if (!includeApplication) {
            task = new TaskMock(new HashMap<>(), rootDoc, null, wtd, null, null);
        } else {
            final InputMessageProcessor inputMessageProcessorTest = new InputMessageProcessorMock(inputMessageProcessor);
            final Application application = new ApplicationMock(inputMessageProcessorTest);
            task = new TaskMock(new HashMap<>(), rootDoc, null, wtd, null, application);
        }
        final Document temp
            = new DocumentMock(reference, fields, task, new HashMap<>(), failures, subdocuments, null, parentDoc, rootDoc);
        task.setDocument(temp);
        return temp;
    }

    /**
     * Utility method to create a subdocument.
     *
     * @param reference just the reference of the subdoc
     * @param fields values in fields
     * @param failures values in failures
     * @param subdocuments the subdocuments to be added
     * @param parentDoc the parent doc
     * @param rootDoc the root doc
     * @param includeApplication does the test need an application object?
     * @param inputMessageProcessor this param has only a meaning if the include application is true. If set to true, we assume that the
     * worker will handle all subdocuments for us, if false it will not, and the workflow-control script has to do it for us.
     * @return a Subdocument
     */
    private Subdocument createSubdocument(final String reference, final Fields fields, final Failures failures,
                                          final Subdocuments subdocuments,
                                          final Document parentDoc, final Document rootDoc, final boolean includeApplication,
                                          final boolean inputMessageProcessor)
    {
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                          "to", tsi);
        final TaskMock task;
        final Application application;
        if (!includeApplication) {
            task = new TaskMock(new HashMap<>(), rootDoc, null, wtd, null, null);
            application = null;
        } else {
            final InputMessageProcessor inputMessageProcessorTest = new InputMessageProcessorMock(inputMessageProcessor);
            application = new ApplicationMock(inputMessageProcessorTest);
            task = new TaskMock(new HashMap<>(), rootDoc, null, wtd, null, application);
        }
        final Subdocument temp
            = new SubdocumentMock(reference, fields, task, new HashMap<>(), failures, subdocuments, application, parentDoc, rootDoc);
        task.setDocument(temp);
        return temp;
    }
}
