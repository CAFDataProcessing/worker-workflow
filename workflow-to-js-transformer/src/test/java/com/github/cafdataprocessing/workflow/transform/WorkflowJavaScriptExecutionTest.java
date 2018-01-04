/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.github.cafdataprocessing.workflow.transform;

import com.github.cafdataprocessing.processing.service.client.model.BooleanConditionAdditional;
import com.github.cafdataprocessing.processing.service.client.model.ExistingCondition;
import com.github.cafdataprocessing.processing.service.client.model.StringConditionAdditional;
import com.github.cafdataprocessing.workflow.transform.models.FullAction;
import com.github.cafdataprocessing.workflow.transform.models.FullProcessingRule;
import com.github.cafdataprocessing.workflow.transform.models.FullWorkflow;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Field;
import com.hpe.caf.worker.document.model.Task;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class for testing the generated workflow JavaScript executes against a document as expected.
 */
public class WorkflowJavaScriptExecutionTest {
    private static final String POST_PROCESSING_NAME = "postProcessingScript";
    private static final WorkflowComponentBuilder BUILDER = new WorkflowComponentBuilder();

    @Test(description = "Verifies string condition evaluation behaviour works as expected in workflow.")
    public void stringConditionTest() throws WorkerException, DataStoreException, IOException, ScriptException,
            WorkflowTransformerException, URISyntaxException, NoSuchMethodException {
        // build a workflow that uses string conditions
        String contentFieldName = "CONTENT";
        String contentIsValue = "CAT";
        ExistingCondition contentIsCondition = BUILDER.buildStringCondition("String is", contentFieldName, contentIsValue, 100,
                StringConditionAdditional.OperatorEnum.IS);
        FullAction stringIsAction = BUILDER.buildFullAction("String 'is' Action",
                new ArrayList<>(Arrays.asList(contentIsCondition)), 100, new HashMap<>());

        String contentStartsWithValue = "DOG";
        ExistingCondition contentStartsWithCondition = BUILDER.buildStringCondition("String starts with", contentFieldName,
                contentStartsWithValue, 100, StringConditionAdditional.OperatorEnum.STARTS_WITH);
        FullAction stringStartsWithAction = BUILDER.buildFullAction("String 'starts with' Action",
                new ArrayList<>(Arrays.asList(contentStartsWithCondition)), 200, new HashMap<>());

        String contentEndsWithValue = "mouse";
        ExistingCondition contentEndsWithCondition = BUILDER.buildStringCondition("String ends with", contentFieldName,
                contentEndsWithValue, 100, StringConditionAdditional.OperatorEnum.ENDS_WITH);
        FullAction stringEndsWithAction = BUILDER.buildFullAction("String 'ends with",
                new ArrayList<>(Arrays.asList(contentEndsWithCondition)), 300, new HashMap<>());

        String startsAndEndsWith_fieldName = "BOOLEAN_STRING_TEST";
        String startsAndEndsWith_startsWithValue = "rat";
        String startsAndEndsWith_endsWithValue = "there";
        ExistingCondition startsAndEndsWith_startsCondition = BUILDER.buildStringCondition("String 'starts with' rat",
                startsAndEndsWith_fieldName, startsAndEndsWith_startsWithValue, 100,
                StringConditionAdditional.OperatorEnum.STARTS_WITH);
        ExistingCondition startsAndEndsWith_endsWithCondition = BUILDER.buildStringCondition("String 'ends with' there",
                startsAndEndsWith_fieldName, startsAndEndsWith_endsWithValue, 200,
                StringConditionAdditional.OperatorEnum.ENDS_WITH);
        ExistingCondition startsAndEndsWith_booleanCondition = BUILDER.buildBooleanCondition("Starts and ends with", 100,
                BooleanConditionAdditional.OperatorEnum.AND, new ArrayList<>(Arrays.asList(startsAndEndsWith_startsCondition,
                        startsAndEndsWith_endsWithCondition)));
        FullAction booleanStartsAndEnds = BUILDER.buildFullAction("Starts and ends with",
                new ArrayList<>(Arrays.asList(startsAndEndsWith_booleanCondition)), 400, new HashMap<>());

        FullProcessingRule rule = BUILDER.buildFullProcessingRule("String conditions rule", true, 100,
                new ArrayList<>(Arrays.asList(stringIsAction, stringStartsWithAction, stringEndsWithAction, booleanStartsAndEnds)),
                new ArrayList<>());
        FullWorkflow workflow = BUILDER.buildFullWorkflow("String conditions workflow", new ArrayList<>(Arrays.asList(rule)));

        TestServices testServices = TestServices.createDefault();
        DataStore store = testServices.getDataStore();
        String dataStoreValue = "DOG IN THE HOUSE.";
        String dataStoreRef = store.store(dataStoreValue.getBytes(), "test");
        String byteArrValue = "There is a mouse";

        Document testDocument_1 = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue(startsAndEndsWith_fieldName, "rat over there")
                .addFieldValue(contentFieldName, "CAT")
                .addFieldValue(contentFieldName, byteArrValue.getBytes())
                .addFieldValue(contentFieldName, "This ends with mouse")
                .documentBuilder().build();
        testDocument_1.getField(contentFieldName).addReference(dataStoreRef);

        final Invocable invocable = getInvocableWorkflowJavaScriptFromFullWorkflow(workflow);
        invocable.invokeFunction("processDocument", testDocument_1);
        checkActionIdToExecute(testDocument_1, Long.toString(stringIsAction.getActionId()));

        // invoke again and the STARTS_WITH action should now be returned as a match
        invocable.invokeFunction("processDocument", testDocument_1);
        checkActionIdToExecute(testDocument_1, Long.toString(stringStartsWithAction.getActionId()));

        // invoke again and the ENDS_WITH action should now be returned as a match
        invocable.invokeFunction("processDocument", testDocument_1);
        checkActionIdToExecute(testDocument_1, Long.toString(stringEndsWithAction.getActionId()));

        // invoke again and the boolean condition containing STARTS_WITH and ENDS_WITH action
        // should now be returned as a match
        invocable.invokeFunction("processDocument", testDocument_1);
        checkActionIdToExecute(testDocument_1, Long.toString(booleanStartsAndEnds.getActionId()));
    }



    @Test(description = "Tests that when custom data in workflow XML specifies a source of inlineJson that the data is " +
            "added to the response options as a serialized string when the action is executed.")
    public void inlineJsonCustomDataTest()
            throws WorkerException, IOException, ScriptException, WorkflowTransformerException,
            URISyntaxException, NoSuchMethodException, DataStoreException {
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_3.xml");
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        TestServices testServices = TestServices.createDefault();
        DataStore store = testServices.getDataStore();
        String postProcessingScriptRef = store.store(workflowJSStr.getBytes(), "test");

        Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .withCustomData()
                .add(POST_PROCESSING_NAME, postProcessingScriptRef)
                .documentBuilder().build();
        invocable.invokeFunction("processDocument", document);

        // expecting action on first enabled rule to be marked for execution
        checkActionIdToExecute(document, "10");
        Map<String, String> returnedCustomData = document.getTask().getResponseOptions().getCustomData();

        // check that the simple string property has been set
        String simpleProperty = returnedCustomData.get("another_prop");
        Assert.assertEquals(simpleProperty, "second value", "Returned simple string property should have expected value.");

        // check that the property that specified an invalid source was not set
        String invalidProperty = returnedCustomData.get("invalid_prop");
        Assert.assertNull(invalidProperty, "Expecting property with an invalid source to not have been returned on " +
                "custom data.");

        String jsonProperty = returnedCustomData.get("test_prop");
        Assert.assertNotNull(jsonProperty, "The inline json data property should not be null on response custom data.");
        // test that deserializable string is output
        JSONObject deserializedObject = new JSONObject(jsonProperty);
        JSONObject topLevelProperty = ((JSONObject) deserializedObject.get("myObject"));
        Assert.assertNotNull(topLevelProperty, "A node 'myObject' should exist.");
        String myKeyProperty = topLevelProperty.getString("myKey");
        Assert.assertEquals(myKeyProperty, " myValue", "Expecting 'myKey' property to be expected value.");
        JSONArray intArrayProperty = topLevelProperty.getJSONArray("intArray");
        Assert.assertNotNull(intArrayProperty, "Expecting int array to have been returned.");
        List<Integer> expectedIntValues = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        for(int intArrayIndex=0; intArrayIndex < intArrayProperty.length(); intArrayIndex++){
            int returnedIntValue = intArrayProperty.getInt(intArrayIndex);
            Assert.assertTrue(expectedIntValues.contains(returnedIntValue), "Expected returned value to be present in " +
                    "list of expected values.");
            expectedIntValues.remove(new Integer(returnedIntValue));
        }
        Assert.assertTrue(expectedIntValues.isEmpty(), "Expected all values to have been matched in returned array.");
        JSONArray myArrayProperty = topLevelProperty.getJSONArray("myArray");
        Assert.assertEquals(myArrayProperty.length(), 4, "Expecting 4 entries returned on 'myArray'.");
        for(int myArrayIndex=0; myArrayIndex < myArrayProperty.length(); myArrayIndex++){
            JSONObject currentArrayEntry = myArrayProperty.getJSONObject(myArrayIndex);
            switch (currentArrayEntry.keys().next()){
                case "myNullValue":
                    Assert.assertTrue(currentArrayEntry.isNull("myNullValue"),
                            "Value set on 'myNullValue' should be null.");
                    break;
                case "myStringValue":
                    Assert.assertEquals(currentArrayEntry.getString("myStringValue"), "My String",
                            "Value set on 'myStringValue' should be as expected." );
                    break;
                case "myBoolean":
                    Assert.assertEquals(currentArrayEntry.getBoolean("myBoolean"), true,
                            "Value set on 'myBoolean' should be expected value.");
                    break;
                case "myInteger":
                    Assert.assertEquals(currentArrayEntry.getInt("myInteger"), 1,
                            "Value set on 'myInteger' should be expected value.");
                    break;
                default:
                    Assert.fail("Unrecognized key in objects returned on myArray property.");
                    break;
            }
        }

    }

    @Test(description = "Tests that a rule that has enabled set to 'false' does not get executed against a document.")
    public void notEnabledRuleNotExecutedTest()
            throws WorkerException, IOException, ScriptException, WorkflowTransformerException,
            URISyntaxException, NoSuchMethodException, DataStoreException {
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_3.xml");
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        TestServices testServices = TestServices.createDefault();
        DataStore store = testServices.getDataStore();
        String postProcessingScriptRef = store.store(workflowJSStr.getBytes(), "test");
        Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .withCustomData()
                .add(POST_PROCESSING_NAME, postProcessingScriptRef)
                .documentBuilder().build();

        invocable.invokeFunction("processDocument", document);

        // expecting action on first enabled rule to be marked for execution
        checkActionIdToExecute(document, "10");

        // invoke again which should cause next appropriate action to be marked for execution and first rule marked as
        // completed
        invocable.invokeFunction("processDocument", document);
        checkActionIdToExecute(document, "12");
        checkActionsCompleted(document, Arrays.asList("10"));
        checkRulesCompleted(document, Arrays.asList("5"));

        // invoke again which should cause no more actions to be marked for execution and third rule marked as
        // completed, with second rule being skipped entirely as it is not enabled
        invocable.invokeFunction("processDocument", document);
        Assert.assertTrue(document.getField("CAF_ACTION_TO_EXECUTE").getStringValues().isEmpty(),
                "Expecting no further actions to execute on the document.");
        checkActionsCompleted(document, Arrays.asList("10", "12"));
        checkRulesCompleted(document, Arrays.asList("5", "7"));
    }

    @Test(description = "Tests that the processing field is not set on response data if it is not passed on the custom " +
            "data of the document and that it is set if it is passed.")
    public void checkPostProcessingFieldSetAsExpected() throws WorkerException, IOException, ScriptException,
            WorkflowTransformerException, URISyntaxException, NoSuchMethodException, DataStoreException {
        Document documentWithoutScript = DocumentBuilder.configure()
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .withCustomData()
                .documentBuilder().build();
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_1.xml");
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        invocable.invokeFunction("processDocument", documentWithoutScript);

        Task returnedTask = documentWithoutScript.getTask();
        Map<String, String> returnedCustomData = returnedTask.getResponseOptions().getCustomData();
        Assert.assertNull(returnedCustomData, "Expecting no custom data to be set in response options " +
                " when post processing script not passed to processDocument method.");

        TestServices testServices = TestServices.createDefault();
        DataStore store = testServices.getDataStore();
        String postProcessingScriptRef = store.store(workflowJSStr.getBytes(), "test");
        Document documentWithScript = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .withCustomData()
                .add(POST_PROCESSING_NAME, postProcessingScriptRef)
                .documentBuilder().build();
        invocable.invokeFunction("processDocument", documentWithScript);
        String secondReturnedScriptValue =
                documentWithScript.getTask().getResponseOptions().getCustomData().get(POST_PROCESSING_NAME);
        Assert.assertEquals(secondReturnedScriptValue, postProcessingScriptRef, "Expecting post processing script to be set in response options " +
                " when it has been passed on document custom data.");
    }

    @Test(description = "Test that a fresh document passed into workflow has correct action to execute added to its fields" +
            " and on subsequent calls the completed fields are correctly updated. Other fields to facilitate progress also" +
            " should be passed.")
    public void actionProgressFieldsTest() throws IOException, ScriptException, URISyntaxException, WorkerException,
            WorkflowTransformerException, NoSuchMethodException, DataStoreException {
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_1.xml");
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        TestServices testServices = TestServices.createDefault();
        DataStore store = testServices.getDataStore();
        String postProcessingScriptRef = store.store(workflowJSStr.getBytes(), "test");
        Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .withCustomData()
                .add(POST_PROCESSING_NAME, postProcessingScriptRef)
                .documentBuilder().build();

        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "1");

        // invoke again which should cause next action to be marked for execution
        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "4");
        checkActionsCompleted(document, Arrays.asList("1"));
        checkPostProcessingSet(document, postProcessingScriptRef);

        // invoke again to verify that all rules are marked as completed and no further actions to execute are returned
        invocable.invokeFunction("processDocument", document);

        Assert.assertTrue(document.getField("CAF_ACTION_TO_EXECUTE").getStringValues().isEmpty(),
                "Expecting no further actions to execute on the document.");
        checkActionsCompleted(document, Arrays.asList("1", "4"));
        checkRulesCompleted(document, Arrays.asList("1", "2"));

        //invoke again to verify that repeated calls after all rules completed doesn't alter the fields
        invocable.invokeFunction("processDocument", document);

        Assert.assertTrue(document.getField("CAF_ACTION_TO_EXECUTE").getStringValues().isEmpty(),
                "Expecting no further actions to execute on the document on second call after completion.");
        checkActionsCompleted(document, Arrays.asList("1", "4"));
        checkRulesCompleted(document, Arrays.asList("1", "2"));
    }

    @Test(description = "Testst that custom data is correctly set from each action to execute, including that the set " +
            "custom data can be serialized.")
    public void handleCustomData() throws WorkerException, ScriptException, NoSuchMethodException,
            WorkflowTransformerException, IOException, URISyntaxException, CodecException, DataStoreException {
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_1.xml");
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        TestServices testServices = TestServices.createDefault();
        DataStore store = testServices.getDataStore();
        String postProcessingScriptRef = store.store(workflowJSStr.getBytes(), "test");
        Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("CONTENT", "string_value").documentBuilder()
                .withCustomData()
                .add(POST_PROCESSING_NAME, postProcessingScriptRef)
                .documentBuilder().build();

        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "1");

        // check that custom data that was set can be serialized (Nashorn ScriptObjectMirror will fail to serialize
        // if it ends up in custom data)
        Codec codec = new JsonCodec();
        byte[] serializedCustomData = codec.serialise(document.getTask().getResponseOptions().getCustomData());
        Assert.assertNotNull(serializedCustomData, "Custom data from first call should have been serialized.");

        // invoke again which should cause next action to be marked for execution
        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "2");
        checkActionsCompleted(document, Arrays.asList("1"));
        checkPostProcessingSet(document, postProcessingScriptRef);
        serializedCustomData = codec.serialise(document.getTask().getResponseOptions().getCustomData());
        Assert.assertNotNull(serializedCustomData, "Custom data from second call should have been serialized.");

        // invoke again to start final rule and verify all completed actions and rules are persisted
        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "3");
        checkActionsCompleted(document, Arrays.asList("1", "2"));
        serializedCustomData = codec.serialise(document.getTask().getResponseOptions().getCustomData());
        Assert.assertNotNull(serializedCustomData, "Custom data from third call should have been serialized.");
        HashMap deserializedCustomData = codec.deserialise(serializedCustomData, HashMap.class);
        Assert.assertNotNull(deserializedCustomData, "Deserialized custom data should not be null.");
        Object postProcessReturnedObj = deserializedCustomData.get(POST_PROCESSING_NAME);
        Assert.assertEquals((String) postProcessReturnedObj, postProcessingScriptRef,
                "Post processing set on deserialized custom data from response options should have expected value.");
        Object grammarMapReturnedObj = deserializedCustomData.get("GRAMMAR_MAP");
        Assert.assertEquals((String) grammarMapReturnedObj, "{pii.xml: []}",
                "Grammar map set on deserialized custom data from response options should have expected value.");
    }

    private void checkActionIdToExecute(Document document, String expectedActionId){
        Assert.assertEquals(document.getField("CAF_ACTION_TO_EXECUTE").getStringValues().size(), 1,
                "Expecting next action to execute to have been added to the document and any previous values to have been removed.");
        String returnedActionIdToExecute = document.getField("CAF_ACTION_TO_EXECUTE").getStringValues().get(0);
        Assert.assertEquals(returnedActionIdToExecute, expectedActionId,
                "Expecting action ID of "+expectedActionId+" to have been matched against test document.");
    }

    private void checkActionsCompleted(Document document, List<String> expectedActionIds){
        Field actionsCompletedField = document.getField("CAF_ACTIONS_COMPLETED");
        Assert.assertEquals(actionsCompletedField.getStringValues().size(), expectedActionIds.size(),
                "Completed actions should be the expected size.");
        for(String expectedActionId: expectedActionIds) {
            Assert.assertTrue( actionsCompletedField.getStringValues().contains(expectedActionId),
                    "Expected action: "+expectedActionId+" to have been marked as completed.");
        }
    }

    private void checkPostProcessingSet(Document document, String expectedPostProcessingValue){
        String actualPostProcessingValue = document.getTask().getResponseOptions().getCustomData()
                .get(POST_PROCESSING_NAME);
        Assert.assertNotNull(actualPostProcessingValue,
                "Expecting post processing field to have been set on task response options.");
        Assert.assertEquals(actualPostProcessingValue, expectedPostProcessingValue, "Post processing " +
                "field value should be as expected.");
    }

    private void checkRulesCompleted(Document document, List<String> expectedRuleIds){
        Field rulesCompletedField = document.getField("CAF_PROCESSING_RULES_COMPLETED");
        Assert.assertEquals(rulesCompletedField.getStringValues().size(), expectedRuleIds.size(),
                "Completed actions should be the expected size.");
        for(String expectedRuleId: expectedRuleIds) {
            Assert.assertTrue( rulesCompletedField.getStringValues().contains(expectedRuleId),
                    "Expected rule: "+expectedRuleId+" to have been marked as completed.");
        }
    }

    private Invocable getInvocableWorkflowJavaScriptFromFullWorkflow(FullWorkflow workflow)
            throws WorkflowTransformerException, ScriptException, URISyntaxException, IOException {
        String workflowAsXml = WorkflowTransformer.transformFullWorkflowToXml(workflow);
        String workflowAsJS = WorkflowTransformer.transformXmlWorkflowToJavaScript(workflowAsXml);
        return getInvocableWorkflowJavaScriptFromJS(workflowAsJS);
    }

    private Invocable getInvocableWorkflowJavaScriptFromJS(String workflowAsJS)
            throws ScriptException {
        final ScriptEngineManager engineManager = new ScriptEngineManager();
        final ScriptEngine engine = engineManager.getEngineByName("nashorn");
        engine.eval(workflowAsJS);
        return (Invocable) engine;
    }

    private Invocable getInvocableWorkflowJavaScriptFromJSResource(String workflowAsJSResourceIdentifier)
            throws ScriptException, URISyntaxException, IOException {
        URL testWorkflowJsUrl = this.getClass().getResource(workflowAsJSResourceIdentifier);
        Path workflowJsPath = Paths.get(testWorkflowJsUrl.toURI());
        String workflowAsJS = new String(
                Files.readAllBytes(workflowJsPath), StandardCharsets.UTF_8);
        return getInvocableWorkflowJavaScriptFromJS(workflowAsJS);
    }

    private Invocable getInvocableWorkflowJavaScriptFromXML(String workflowXmlResourceIdentifier)
            throws URISyntaxException, IOException,
            ScriptException, WorkflowTransformerException {
        String workflowAsJS = getWorkflowJavaScriptFromXML(workflowXmlResourceIdentifier);
        return getInvocableWorkflowJavaScriptFromJS(workflowAsJS);
    }

    private String getWorkflowJavaScriptFromXML(String workflowXmlResourceIdentifier)
            throws IOException, URISyntaxException, WorkflowTransformerException {
        URL testWorkflowXml = this.getClass().getResource(workflowXmlResourceIdentifier);
        Path workflowXmlPath = Paths.get(testWorkflowXml.toURI());

        return WorkflowTransformer.transformXmlWorkflowToJavaScript(new String(
                Files.readAllBytes(workflowXmlPath), StandardCharsets.UTF_8));
    }
}
