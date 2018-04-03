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

import com.github.cafdataprocessing.processing.service.client.ApiException;
import com.github.cafdataprocessing.processing.service.client.ApiClient;
import com.github.cafdataprocessing.processing.service.client.model.BooleanConditionAdditional;
import com.github.cafdataprocessing.processing.service.client.model.EffectiveTenantConfig;
import com.github.cafdataprocessing.processing.service.client.model.EffectiveTenantConfigValue;
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
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Field;
import com.hpe.caf.worker.document.model.FieldValue;
import com.hpe.caf.worker.document.model.FieldValues;
import com.hpe.caf.worker.document.model.Script;
import com.hpe.caf.worker.document.model.Scripts;
import com.hpe.caf.worker.document.model.Task;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import com.sun.jersey.api.client.ClientHandlerException;
import org.apache.commons.io.IOUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import org.mockito.Mockito;

/**
 * Class for testing the generated workflow JavaScript executes against a document as expected.
 */
public class WorkflowJavaScriptExecutionTest {
    private static final String PROJECT_ID = "ProjectId_Test_Value_1234567890";
    private static final String TENANT_ID = "123456789";
    private static final WorkflowComponentBuilder BUILDER = new WorkflowComponentBuilder();

    @Test(description = "Tests that a script specified on an action is added to the scripts property on the set response after " +
            "document is evaluated against the workflow..")
    public void addScriptsFromAction()
            throws WorkerException, URISyntaxException, IOException, ApiException, WorkflowTransformerException,
            ScriptException, NoSuchMethodException, DataStoreException {
        final Map<String, Object> customDataInput = new HashMap<>();
        final String testPropertyKey = "testProperty";
        final String testPropertyValue = UUID.randomUUID().toString();
        customDataInput.put(testPropertyKey, testPropertyValue);

        final Map<String, Object> settingsInput = new HashMap<>();
        settingsInput.put("customData", customDataInput);

        // add an inline script
        final String expectedScriptFieldValue_1 = UUID.randomUUID().toString();
        final String expectedScriptFieldName_1 = UUID.randomUUID().toString();
        final String testScriptValue_1 = "function run(document){document.getField(\""+expectedScriptFieldName_1+"\").set" +
                "(\""+expectedScriptFieldValue_1+"\");}";
        final String testScriptName_1 = "testScript_1";
        final Map<String, String> scriptEntry_1 = new HashMap<>();
        scriptEntry_1.put("name", testScriptName_1);
        scriptEntry_1.put("script", testScriptValue_1);

        // add a storage reference script
        final String testScriptStoredValue = UUID.randomUUID().toString();
        final TestServices testServices = TestServices.createDefault();
        final String testScriptValue_2 = testServices.getDataStore()
                .store(testScriptStoredValue.getBytes(StandardCharsets.UTF_8), "test");
        final String testScriptName_2 = "testScript_2";
        final Map<String, String> scriptEntry_2 = new HashMap<>();
        scriptEntry_2.put("name", testScriptName_2);
        scriptEntry_2.put("storageRef", testScriptValue_2);

        // add a uri script
        final URL testDataUrl = Thread.currentThread().getContextClassLoader().getResource("urlTestData.txt");
        final String testScriptValue_3 = IOUtils.toString(testDataUrl);
        final String testScriptUrl = testDataUrl.toString();
        final String testScriptName_3 = "testScript_3";
        final Map<String, String> scriptEntry_3 = new HashMap<>();
        scriptEntry_3.put("name", testScriptName_3);
        scriptEntry_3.put("url", testScriptUrl);

        final List scriptsList = new ArrayList<>(Arrays.asList(scriptEntry_1, scriptEntry_2, scriptEntry_3));
        settingsInput.put("scripts", scriptsList);

        final FullAction testAction = BUILDER.buildFullAction("Script addition action",
                new ArrayList<>(), 100, settingsInput);

        final FullProcessingRule rule = BUILDER.buildFullProcessingRule("Test script addition rule", true, 100,
                new ArrayList<>(Arrays.asList(testAction)),
                new ArrayList<>());
        final FullWorkflow workflow = BUILDER.buildFullWorkflow("Test script addition workflow", new ArrayList<>(Arrays.asList(rule)));


        final Document testDocument_1 = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value")
                .documentBuilder().build();

        final Invocable invocable = getInvocableWorkflowJavaScriptFromFullWorkflow(workflow);
        invocable.invokeFunction("processDocument", testDocument_1);
        checkActionIdToExecute(testDocument_1, Long.toString(testAction.getActionId()));
        final Task returnedTask = testDocument_1.getTask();
        final Map<String, String> returnedCustomData = returnedTask.getResponse().getCustomData();
        final String returnedTestPropertyValue = returnedCustomData.get(testPropertyKey);
        Assert.assertEquals(returnedTestPropertyValue, testPropertyValue, "Returned property that was set on custom data should" +
                " be as expected.");

        final Scripts returnedScripts = returnedTask.getScripts();
        Assert.assertEquals(returnedScripts.size(), scriptsList.size(), "Returned task should have expected number of scripts.");
        final Script returnedScript_1 = returnedScripts.get(0);

        checkGeneralScriptProperties(returnedScript_1, testScriptName_1, testScriptValue_1);
        final ScriptEngineManager engineManager = new ScriptEngineManager();
        final ScriptEngine engine = engineManager.getEngineByName("nashorn");
        engine.eval(returnedScript_1.getScript());
        final Invocable invocableTestScript_1 = (Invocable) engine;
        invocableTestScript_1.invokeFunction("run", testDocument_1);

        final List<String> testFieldValues = testDocument_1.getField(expectedScriptFieldName_1).getStringValues();
        Assert.assertEquals(testFieldValues.size(), 1, "Expecting the inline script to have added one value on a specific field" +
                ".");
        Assert.assertEquals(testFieldValues.get(0), expectedScriptFieldValue_1, "Expected inline script to have set a specific " +
                "value on a field.");

        final Script returnedScript_2 = returnedScripts.get(1);
        checkGeneralScriptProperties(returnedScript_2, testScriptName_2, testScriptStoredValue);

        final Script returnedScript_3 = returnedScripts.get(2);
        checkGeneralScriptProperties(returnedScript_3, testScriptName_3, testScriptValue_3);
    }

    @Test(description = "Tests that if a script is specified without one of the recognized script/storageRef/url properties " +
            "that an appropriate exception is thrown during JS execution.")
    public void addInvalidScriptOnAnAction()
            throws WorkerException, URISyntaxException, IOException, ApiException, WorkflowTransformerException,
            ScriptException, NoSuchMethodException {
        final Map<String, Object> settingsInput = new HashMap<>();
        // add an inline script
        final String expectedScriptFieldValue_1 = UUID.randomUUID().toString();
        final String testScriptValue_1 = "function run(){document.getField(\"TEST\").set(\""+expectedScriptFieldValue_1+"\");}";
        final String testScriptName_1 = "testScript_1";
        final Map<String, String> scriptEntry_1 = new HashMap<>();
        scriptEntry_1.put("name", testScriptName_1);
        scriptEntry_1.put("junk", testScriptValue_1);

        final List scriptsList = new ArrayList<>(Arrays.asList(scriptEntry_1));
        settingsInput.put("scripts", scriptsList);

        final FullAction testAction = BUILDER.buildFullAction("Script addition action",
                new ArrayList<>(), 100, settingsInput);

        final FullProcessingRule rule = BUILDER.buildFullProcessingRule("Test script addition rule", true, 100,
                new ArrayList<>(Arrays.asList(testAction)),
                new ArrayList<>());
        final FullWorkflow workflow = BUILDER.buildFullWorkflow("Test script addition workflow", new ArrayList<>(Arrays.asList(rule)));


        final Document testDocument_1 = DocumentBuilder.configure()
                .withFields()
                .addFieldValue("test", "string_value")
                .documentBuilder().build();

        final Invocable invocable = getInvocableWorkflowJavaScriptFromFullWorkflow(workflow);
        try {
            invocable.invokeFunction("processDocument", testDocument_1);
            Assert.fail("Exception should have been thrown during processDocument call due to invalid script definition.");
        }
        catch(final ScriptException e) {
            Assert.assertEquals(e.getCause().getCause().getClass(), RuntimeException.class, "Expecting script exception cause to be a " +
                    "RuntimeException thrown due to invalid script definition.");
        }
    }

    @Test(description = "Tests how a script that contains a single quote character on an action is handled during execution of " +
            "workflow.")
    public void addScriptWithSingleQuoteOnAction()
            throws WorkerException, URISyntaxException, IOException, ApiException, WorkflowTransformerException,
            ScriptException, NoSuchMethodException, DataStoreException {
        final Map<String, Object> customDataInput = new HashMap<>();
        final String testPropertyKey = "testProperty";
        final String testPropertyValue = UUID.randomUUID().toString();
        customDataInput.put(testPropertyKey, testPropertyValue);

        final Map<String, Object> settingsInput = new HashMap<>();
        settingsInput.put("customData", customDataInput);

        // add an inline script
        final String expectedScriptFieldValue_1 = UUID.randomUUID().toString();
        final String testScriptValue_1 =
                "function run(){document.getField('TEST').set('This < \\n > & '"+expectedScriptFieldValue_1+"');}";
        final String testScriptName_1 = "testScript_1";
        final Map<String, String> scriptEntry_1 = new HashMap<>();
        scriptEntry_1.put("name", testScriptName_1);
        scriptEntry_1.put("script", testScriptValue_1);

        final List scriptsList = new ArrayList<>(Arrays.asList(scriptEntry_1));
        settingsInput.put("scripts", scriptsList);

        final FullAction testAction = BUILDER.buildFullAction("Script addition action",
                new ArrayList<>(), 100, settingsInput);

        final FullProcessingRule rule = BUILDER.buildFullProcessingRule("Test script addition rule", true, 100,
                new ArrayList<>(Arrays.asList(testAction)),
                new ArrayList<>());
        final FullWorkflow workflow = BUILDER.buildFullWorkflow("Test script addition workflow", new ArrayList<>(Arrays.asList(rule)));

        final Document testDocument_1 = DocumentBuilder.configure()
                .withFields()
                .addFieldValue("test", "string_value")
                .documentBuilder().build();

        try {
            final Invocable invocable = getInvocableWorkflowJavaScriptFromFullWorkflow(workflow);
            Assert.fail("This is expected to throw an exception until SCMOD-3725 is complete.");
        }
        catch(final ScriptException e) {
            // TODO when SCMOD0-3725 is complete the below code should be uncommeted as it is expected the single quotes causing
            // TODO the script exception will be escaped and the script will be evaluated sucessfully.
        }

        /*
        invocable.invokeFunction("processDocument", testDocument_1);
        checkActionIdToExecute(testDocument_1, Long.toString(testAction.getActionId()));
        final Task returnedTask = testDocument_1.getTask();
        final Map<String, String> returnedCustomData = returnedTask.getResponse().getCustomData();
        final String returnedTestPropertyValue = returnedCustomData.get(testPropertyKey);
        Assert.assertEquals(returnedTestPropertyValue, testPropertyValue, "Returned property that was set on custom data should" +
                " be as expected.");

        final Scripts returnedScripts = returnedTask.getScripts();
        Assert.assertEquals(returnedScripts.size(), scriptsList.size(), "Returned task should have expected number of scripts.");
        final Script returnedScript_1 = returnedScripts.get(0);
        checkGeneralScriptProperties(returnedScript_1, testScriptName_1, testScriptValue_1);*/

        // TODO add execution of the script and checking of the field added
    }

    private void checkGeneralScriptProperties(final Script actualScript, final String expectedName, final String expectedValue)
            throws IOException {
        Assert.assertEquals(actualScript.getName(), expectedName, "Expecting the name of script to match expected value.");
        Assert.assertEquals(actualScript.getScript(), expectedValue, "Expecting the script set to match expected script " +
                "value.");
        Assert.assertTrue(actualScript.isInstalled(), "Expecting the script to be installed.");
        Assert.assertFalse(actualScript.isLoaded(), "Expecting the script to not be loaded.");
    }

    @Test(description = "Tests that a workflow containing an action with boolean conditions is evaluated as expected.")
    public void booleanConditionCheck()
            throws ScriptException, WorkflowTransformerException, IOException, URISyntaxException,
            NoSuchMethodException, WorkerException, DataStoreException, ApiException {
        final ApiClient apiClient = getMockApiClient(null, null);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_2.xml", apiClient);
        final String familyHashingActionId = "29";
        final String langDetectActionId = "30";
        final String entityExtractActionId = "31";
        final String outputActionId = "32";

        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        final TestServices testServices = TestServices.createDefault();
        final Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("CONTENT", "test")
                .addFieldValue("DOC_FORMAT_CODE", "345")
                .addFieldValue("DOC_CLASS_CODE", "9")
                .addFieldValue("test", "string_value").documentBuilder()
                .build();

        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, familyHashingActionId);

        // invoke again which should cause next action to be marked for execution
        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, langDetectActionId);
        checkActionsCompleted(document, Arrays.asList(familyHashingActionId));

        // invoke again which should cause next action to be marked for execution
        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, entityExtractActionId);
        checkActionsCompleted(document, Arrays.asList(familyHashingActionId, langDetectActionId));

        // verify that if a document that does not meet the criteria is passed that it does not try to execute the action
        final Document document_doesntMatchEntityExtract = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("CONTENT", "test")
                .addFieldValue("DOC_FORMAT_CODE", "345")
                .addFieldValue("DOC_CLASS_CODE", "8")
                .addFieldValue("test", "string_value").documentBuilder()
                .build();

        invocable.invokeFunction("processDocument", document_doesntMatchEntityExtract);

        checkActionIdToExecute(document_doesntMatchEntityExtract, familyHashingActionId);

        // invoke again which should cause next action to be marked for execution
        invocable.invokeFunction("processDocument", document_doesntMatchEntityExtract);

        checkActionIdToExecute(document_doesntMatchEntityExtract, langDetectActionId);
        checkActionsCompleted(document_doesntMatchEntityExtract, Arrays.asList(familyHashingActionId));

        // invoke again which should cause Entity Extract Action to not be executed as doc does not meet the conditions
        invocable.invokeFunction("processDocument", document_doesntMatchEntityExtract);

        checkActionIdToExecute(document_doesntMatchEntityExtract, outputActionId);
        checkActionsCompleted(document_doesntMatchEntityExtract, Arrays.asList(familyHashingActionId, langDetectActionId));
    }

    @Test(description = "Tests that the field mapping action is executed as expected on a document and that the next action " +
            "is queued for execution with its settings.")
    public void fieldMappingActionTest() throws WorkerException, URISyntaxException, IOException,
            WorkflowTransformerException, ScriptException, NoSuchMethodException, ApiException {
        final Map<String, Object> fieldMappingSettings = new HashMap<>();
        final Map<String, String> mappings = new HashMap<>();
        final String abcFieldKey = "abc";
        final String abcFieldValue = UUID.randomUUID().toString();
        final String defFieldKey = "def";
        final String defFieldValue_1 = UUID.randomUUID().toString();
        final String defFieldValue_2 = UUID.randomUUID().toString();
        final String ghiFieldKey = "ghi";
        final String ghiFieldValue = UUID.randomUUID().toString();
        final String jklFieldKey = "jkl";
        final String jklFieldValue = UUID.randomUUID().toString();
        final String mnoFieldKey = "mno";
        final String pqrFieldKey = "pqr";
        final String pqrFieldValueStr = UUID.randomUUID().toString();
        final String pqrFieldValueRef = UUID.randomUUID().toString();
        final String stuFieldKey = "stu";
        final String stuFieldValue = UUID.randomUUID().toString();
        final String vwxFieldKey = "vwx";
        final String vwxFieldValue = UUID.randomUUID().toString();
        mappings.put(abcFieldKey, defFieldKey);
        mappings.put(defFieldKey, abcFieldKey);
        mappings.put(ghiFieldKey, defFieldKey);
        mappings.put(jklFieldKey, mnoFieldKey);
        mappings.put(pqrFieldKey, stuFieldKey);
        fieldMappingSettings.put("mappings", mappings);

        final FullAction fieldMappingAction =
                BUILDER.buildFullAction("Field Mapping Action", new ArrayList<>(Arrays.asList()), 100,
                        fieldMappingSettings, "FieldMappingActionType");

        final Map<String, Object> entityExtractSettings = new HashMap<>();
        final Map<String, String> entityExtractCustomData = new HashMap<>();
        final String entityExtractOpModeKey = "OPERATION_MODE";
        final String entityExtractOpModeValue = "DETECT";
        entityExtractCustomData.put(entityExtractOpModeKey, entityExtractOpModeValue);
        final String entityExtractGrammarMapKey = "GRAMMAR_MAP";
        final String enityExtractGrammarMapValue = "{pii.xml: []}";
        entityExtractCustomData.put(entityExtractGrammarMapKey, enityExtractGrammarMapValue);
        entityExtractSettings.put("customData", entityExtractCustomData);
        entityExtractSettings.put("workerName", "entityextractworkerhandler");

        final String entityExtractQueueName = UUID.randomUUID().toString();
        entityExtractSettings.put("queueName", entityExtractQueueName);

        final FullAction entityExtractAction =
                BUILDER.buildFullAction("Entity Extract Action", new ArrayList<>(Arrays.asList()), 200,
                        entityExtractSettings, "ChainedActionType");

        final FullProcessingRule rule =
                BUILDER.buildFullProcessingRule("Processing Rule", true, 100,
                        new ArrayList<>(Arrays.asList(fieldMappingAction, entityExtractAction)),
                        new ArrayList<>(Arrays.asList()));
        final FullWorkflow workflow = BUILDER.buildFullWorkflow("Workflow", new ArrayList<>(Arrays.asList(rule)));
        final TestServices testServices = TestServices.createDefault();
        final Document testDocument_1 = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue(abcFieldKey, abcFieldValue)
                .addFieldValue(defFieldKey, defFieldValue_1)
                .addFieldValue(defFieldKey, defFieldValue_2)
                .addFieldValue(ghiFieldKey, ghiFieldValue)
                .addFieldValue(jklFieldKey, jklFieldValue)
                .addFieldValue(pqrFieldKey, pqrFieldValueStr)
                .addFieldValues(pqrFieldKey, DocumentWorkerFieldEncoding.storage_ref, pqrFieldValueRef)
                .addFieldValue(stuFieldKey, stuFieldValue)
                .addFieldValue(vwxFieldKey, vwxFieldValue)
                .documentBuilder().build();

        final ApiClient apiClient = getMockApiClient(null, null);
        final Invocable invocable = getInvocableWorkflowJavaScriptFromFullWorkflow(workflow, apiClient);
        invocable.invokeFunction("processDocument", testDocument_1);
        // the entity extract action should be the action listed for execution as field mapping will have occurred
        // inside the script
        checkActionIdToExecute(testDocument_1, Long.toString(entityExtractAction.getActionId()));

        // check that the response options have been set as expected
        final String setQueueName = testDocument_1.getTask().getResponse().getQueueNameOverride();
        Assert.assertEquals(setQueueName, entityExtractQueueName, "Queue name should have been set to expected queue.");
        final Map<String, String> setCustomData = testDocument_1.getTask().getResponse().getCustomData();
        Assert.assertTrue(setCustomData.containsKey(entityExtractOpModeKey),
                "Custom data should have the entity extract operation mode key.");
        final String setOpModeValue = (String) setCustomData.get(entityExtractOpModeKey);
        Assert.assertEquals(setOpModeValue, entityExtractOpModeValue,
                "Entity Extract Op mode value on returned custom data should have been set to expected value.");
        Assert.assertTrue(setCustomData.containsKey(entityExtractGrammarMapKey),
                "Custom data should have the entity extract grammar map key.");
        final String setGrammarMapValue = (String) setCustomData.get(entityExtractGrammarMapKey);
        Assert.assertEquals(setGrammarMapValue, enityExtractGrammarMapValue,
                "Entity Extract Grammar Map value on returned custom data should have been set to expected value.");

        // verify that fields have been remapped as expected
        // abc value & ghi value should now be in the def field
        final Field updatedDefField = testDocument_1.getField(defFieldKey);
        final FieldValues updatedDefFieldValues = updatedDefField.getValues();
        boolean abcValuePresent = false;
        boolean ghiValuePresent = false;
        Assert.assertEquals(updatedDefFieldValues.size(), 2, "Expecting two values to be present in 'def' field after mapping.");
        for(final FieldValue updatedDefFieldValue: updatedDefFieldValues) {
            final String fieldValueStr = updatedDefFieldValue.getStringValue();
            if(abcFieldValue.equals(fieldValueStr)){
                abcValuePresent = true;
            }
            if(ghiFieldValue.equals(fieldValueStr)){
                ghiValuePresent = true;
            }
        }
        // 'abc' and 'ghi' field values should now be in 'dhi'
        Assert.assertTrue(abcValuePresent, "Value from 'abc' field was not present in 'def' field.");
        Assert.assertTrue(ghiValuePresent, "Value from 'ghi' field was not present in 'def' field.");
        final Field updatedGhiField = testDocument_1.getField(ghiFieldKey);
        Assert.assertTrue(!updatedGhiField.hasValues(), "'ghi' field should have no values after mapping.");

        // both 'def' values should now be in the 'abc' field
        final Field updatedAbcField = testDocument_1.getField(abcFieldKey);
        Assert.assertEquals(updatedAbcField.getValues().size(), 2, "'abc' field should have 2 values after mapping.");
        final List<String> updatedAbcStrValues = updatedAbcField.getStringValues();
        Assert.assertTrue(updatedAbcStrValues.contains(defFieldValue_1), "'def' value 1 should have been mapped to 'abc'");
        Assert.assertTrue(updatedAbcStrValues.contains(defFieldValue_2), "'def' value 2 should have been mapped to 'abc'");

        // 'jkl' values should be in 'mno' which was previously empty
        final Field updatedJklField = testDocument_1.getField(jklFieldKey);
        Assert.assertTrue(!updatedJklField.hasValues(), "'jkl' field should have no values after mapping.");
        final Field updatedMnoField = testDocument_1.getField(mnoFieldKey);
        Assert.assertEquals(updatedMnoField.getValues().size(), 1, "'mno' field should have 1 value after mapping.");
        final List<String> updatedMnoValues = updatedMnoField.getStringValues();
        Assert.assertTrue(updatedMnoValues.contains(jklFieldValue), "'jkl' value should have been mapped to 'mno'");

        // 'pqr' string and reference values should be added to 'stu' which was previously populated
        final Field updatedPqrField = testDocument_1.getField(pqrFieldKey);
        Assert.assertTrue(!updatedPqrField.hasValues(), "'pqr' field should have no values after mapping.");
        final Field updatedStuField = testDocument_1.getField(stuFieldKey);
        final List<String> updatedStuStrValues = updatedStuField.getStringValues();
        final FieldValues updatedStuValues = updatedStuField.getValues();
        Assert.assertEquals(updatedStuValues.size(), 3, "'stu' field should have 3 values after mapping.");
        Assert.assertEquals(updatedStuStrValues.size(), 2, "'stu' field should have 2 string values after mapping.");
        Assert.assertTrue(updatedStuStrValues.contains(pqrFieldValueStr),
                "'pqr' string value should have been mapped to 'stu'");
        Assert.assertTrue(updatedStuStrValues.contains(stuFieldValue),
                "Original 'stu' string value should still be present on 'stu'");

        boolean stuReferenceFound = false;
        final Iterator<FieldValue> stuValuesIterator = updatedStuValues.iterator();
        while(stuValuesIterator.hasNext()){
            final FieldValue updatedStuFieldValue = stuValuesIterator.next();
            if(!updatedStuFieldValue.isReference()){
                continue;
            }
            else {
                stuReferenceFound = true;
            }
            final String stuFieldReferenceValue = updatedStuFieldValue.getReference();
            Assert.assertEquals(stuFieldReferenceValue, pqrFieldValueRef,
                    "'pqr' reference value should have been mapped to 'stu'.");
        }
        Assert.assertTrue(stuReferenceFound, "A reference should be present in the values of 'stu'.");
    }

    @Test(description = "Verifies string condition evaluation behaviour works as expected in workflow.")
    public void stringConditionTest() throws WorkerException, DataStoreException, IOException, ScriptException,
            WorkflowTransformerException, URISyntaxException, NoSuchMethodException, ApiException {
        // build a workflow that uses string conditions
        final String contentFieldName = "CONTENT";
        final String contentIsValue = "CAT";
        final ExistingCondition contentIsCondition = BUILDER.buildStringCondition("String is", contentFieldName, contentIsValue, 100,
                StringConditionAdditional.OperatorEnum.IS);
        final FullAction stringIsAction = BUILDER.buildFullAction("String 'is' Action",
                new ArrayList<>(Arrays.asList(contentIsCondition)), 100, new HashMap<>());

        final String contentStartsWithValue = "DOG";
        final ExistingCondition contentStartsWithCondition = BUILDER.buildStringCondition("String starts with", contentFieldName,
                contentStartsWithValue, 100, StringConditionAdditional.OperatorEnum.STARTS_WITH);
        final FullAction stringStartsWithAction = BUILDER.buildFullAction("String 'starts with' Action",
                new ArrayList<>(Arrays.asList(contentStartsWithCondition)), 200, new HashMap<>());

        final String contentEndsWithValue = "mouse";
        final ExistingCondition contentEndsWithCondition = BUILDER.buildStringCondition("String ends with", contentFieldName,
                contentEndsWithValue, 100, StringConditionAdditional.OperatorEnum.ENDS_WITH);
        final FullAction stringEndsWithAction = BUILDER.buildFullAction("String 'ends with",
                new ArrayList<>(Arrays.asList(contentEndsWithCondition)), 300, new HashMap<>());

        final String startsAndEndsWith_fieldName = "BOOLEAN_STRING_TEST";
        final String startsAndEndsWith_startsWithValue = "rat";
        final String startsAndEndsWith_endsWithValue = "there";
        final ExistingCondition startsAndEndsWith_startsCondition = BUILDER.buildStringCondition("String 'starts with' rat",
                startsAndEndsWith_fieldName, startsAndEndsWith_startsWithValue, 100,
                StringConditionAdditional.OperatorEnum.STARTS_WITH);
        final ExistingCondition startsAndEndsWith_endsWithCondition = BUILDER.buildStringCondition("String 'ends with' there",
                startsAndEndsWith_fieldName, startsAndEndsWith_endsWithValue, 200,
                StringConditionAdditional.OperatorEnum.ENDS_WITH);
        final ExistingCondition startsAndEndsWith_booleanCondition = BUILDER.buildBooleanCondition("Starts and ends with", 100,
                BooleanConditionAdditional.OperatorEnum.AND, new ArrayList<>(Arrays.asList(startsAndEndsWith_startsCondition,
                        startsAndEndsWith_endsWithCondition)));
        final FullAction booleanStartsAndEnds = BUILDER.buildFullAction("Starts and ends with",
                new ArrayList<>(Arrays.asList(startsAndEndsWith_booleanCondition)), 400, new HashMap<>());

        final FullProcessingRule rule = BUILDER.buildFullProcessingRule("String conditions rule", true, 100,
                new ArrayList<>(Arrays.asList(stringIsAction, stringStartsWithAction, stringEndsWithAction, booleanStartsAndEnds)),
                new ArrayList<>());
        final FullWorkflow workflow = BUILDER.buildFullWorkflow("String conditions workflow", new ArrayList<>(Arrays.asList(rule)));

        final TestServices testServices = TestServices.createDefault();
        final DataStore store = testServices.getDataStore();
        final String dataStoreValue = "DOG IN THE HOUSE.";
        final String dataStoreRef = store.store(dataStoreValue.getBytes(), "test");
        final String byteArrValue = "There is a mouse";

        final Document testDocument_1 = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue(startsAndEndsWith_fieldName, "rat over there")
                .addFieldValue(contentFieldName, "CAT")
                .addFieldValue(contentFieldName, byteArrValue.getBytes())
                .addFieldValue(contentFieldName, "This ends with mouse")
                .documentBuilder().build();
        testDocument_1.getField(contentFieldName).addReference(dataStoreRef);

        final ApiClient apiClient = getMockApiClient(null, null);
        final Invocable invocable = getInvocableWorkflowJavaScriptFromFullWorkflow(workflow, apiClient);
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
            URISyntaxException, NoSuchMethodException, DataStoreException, ApiException {
        final ApiClient apiClient = getMockApiClient(null, null);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_3.xml", apiClient);
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        final TestServices testServices = TestServices.createDefault();

        final Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .build();
        invocable.invokeFunction("processDocument", document);

        // expecting action on first enabled rule to be marked for execution
        checkActionIdToExecute(document, "10");
        final Map<String, String> returnedCustomData = document.getTask().getResponse().getCustomData();

        // check that the simple string property has been set
        final String simpleProperty = returnedCustomData.get("another_prop");
        Assert.assertEquals(simpleProperty, "second value", "Returned simple string property should have expected value.");

        // check that the property that specified an invalid source was not set
        final String invalidProperty = returnedCustomData.get("invalid_prop");
        Assert.assertNull(invalidProperty, "Expecting property with an invalid source to not have been returned on " +
                "custom data.");

        final String jsonProperty = returnedCustomData.get("test_prop");
        Assert.assertNotNull(jsonProperty, "The inline json data property should not be null on response custom data.");
        // test that deserializable string is output
        final JSONObject deserializedObject = new JSONObject(jsonProperty);
        final JSONObject topLevelProperty = ((JSONObject) deserializedObject.get("myObject"));
        Assert.assertNotNull(topLevelProperty, "A node 'myObject' should exist.");
        final String myKeyProperty = topLevelProperty.getString("myKey");
        Assert.assertEquals(myKeyProperty, " myValue", "Expecting 'myKey' property to be expected value.");
        final JSONArray intArrayProperty = topLevelProperty.getJSONArray("intArray");
        Assert.assertNotNull(intArrayProperty, "Expecting int array to have been returned.");
        final List<Integer> expectedIntValues = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        for(int intArrayIndex=0; intArrayIndex < intArrayProperty.length(); intArrayIndex++){
            final int returnedIntValue = intArrayProperty.getInt(intArrayIndex);
            Assert.assertTrue(expectedIntValues.contains(returnedIntValue), "Expected returned value to be present in " +
                    "list of expected values.");
            expectedIntValues.remove(new Integer(returnedIntValue));
        }
        Assert.assertTrue(expectedIntValues.isEmpty(), "Expected all values to have been matched in returned array.");
        final JSONArray myArrayProperty = topLevelProperty.getJSONArray("myArray");
        Assert.assertEquals(myArrayProperty.length(), 4, "Expecting 4 entries returned on 'myArray'.");
        for(int myArrayIndex=0; myArrayIndex < myArrayProperty.length(); myArrayIndex++){
            final JSONObject currentArrayEntry = myArrayProperty.getJSONObject(myArrayIndex);
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

    @Test(description = "Tests that when custom data in workflow XML specifies a source of tenantId that the data is added to " +
            "the response options as a string when the action is executed.")
    public void tenantIdCustomDataTest() throws WorkerException, ScriptException, NoSuchMethodException, WorkflowTransformerException, IOException, URISyntaxException, ApiException {
        final Map<String, Object> customDataInput = new HashMap<>();
        final String testPropertyKey = "testProperty";
        final Map<String, Object> testPropertyValue = new HashMap<>();
        testPropertyValue.put("source", "tenantId");
        customDataInput.put(testPropertyKey, testPropertyValue);

        final Map<String, Object> settingsInput = new HashMap<>();
        settingsInput.put("customData", customDataInput);

        final FullAction testAction = BUILDER.buildFullAction("TenantId Action",
                new ArrayList<>(), 100, settingsInput);

        final FullProcessingRule rule = BUILDER.buildFullProcessingRule("Test rule", true, 100,
                new ArrayList<>(Arrays.asList(testAction)),
                new ArrayList<>());
        final FullWorkflow workflow = BUILDER.buildFullWorkflow("Test workflow", new ArrayList<>(Arrays.asList(rule)));

        final TestServices testServices = TestServices.createDefault();
        final Document testDocument_1 = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value")
                .documentBuilder().build();

        final ApiClient apiClient = getMockApiClient(null, null);
        final Invocable invocable = getInvocableWorkflowJavaScriptFromFullWorkflow(workflow, apiClient);
        invocable.invokeFunction("processDocument", testDocument_1);
        checkActionIdToExecute(testDocument_1, Long.toString(testAction.getActionId()));
        final Map<String, String> returnedCustomData = testDocument_1.getTask().getResponse().getCustomData();

        final String returnedTestPropertyValue = returnedCustomData.get(testPropertyKey);
        Assert.assertEquals(returnedTestPropertyValue, TENANT_ID,
                "TenantId value set on custom data property should match expected value");
    }

    @Test(description = "Tests that when custom data in workflow XML specifies a source of projectId that the data is " +
            "added to the response options as a string when the action is executed.")
    public void projectIdCustomDataTest()
            throws WorkerException, IOException, ScriptException, WorkflowTransformerException,
            URISyntaxException, NoSuchMethodException, DataStoreException, ApiException {
        final ApiClient apiClient = getMockApiClient(null, null);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_4.xml", apiClient);
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        final TestServices testServices = TestServices.createDefault();

        final Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .build();
        invocable.invokeFunction("processDocument", document);

        // expecting action on first enabled rule to be marked for execution
        checkActionIdToExecute(document, "10");
        final Map<String, String> returnedCustomData = document.getTask().getResponse().getCustomData();

        // check that the simple string property has been set
        final String simpleProperty = returnedCustomData.get("another_prop");
        Assert.assertEquals(simpleProperty, "second value", "Returned simple string property should have expected value.");

        // check that the property that specified an invalid source was not set
        final String invalidProperty = returnedCustomData.get("invalid_prop");
        Assert.assertNull(invalidProperty, "Expecting property with an invalid source to not have been returned on " +
                "custom data.");

        final String projectId = returnedCustomData.get("test_projectId_prop");
        Assert.assertEquals(PROJECT_ID, projectId, "ProjectId should match expected value");

        final String jsonProperty = returnedCustomData.get("test_prop");
        Assert.assertNotNull(jsonProperty, "The inline json data property should not be null on response custom data.");
        // test that deserializable string is output
        final JSONObject deserializedObject = new JSONObject(jsonProperty);
        final JSONObject topLevelProperty = ((JSONObject) deserializedObject.get("myObject"));
        Assert.assertNotNull(topLevelProperty, "A node 'myObject' should exist.");
        final String myKeyProperty = topLevelProperty.getString("myKey");
        Assert.assertEquals(myKeyProperty, " myValue", "Expecting 'myKey' property to be expected value.");
        final JSONArray intArrayProperty = topLevelProperty.getJSONArray("intArray");
        Assert.assertNotNull(intArrayProperty, "Expecting int array to have been returned.");
        final List<Integer> expectedIntValues = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        for(int intArrayIndex=0; intArrayIndex < intArrayProperty.length(); intArrayIndex++){
            final int returnedIntValue = intArrayProperty.getInt(intArrayIndex);
            Assert.assertTrue(expectedIntValues.contains(returnedIntValue), "Expected returned value to be present in " +
                    "list of expected values.");
            expectedIntValues.remove(new Integer(returnedIntValue));
        }
        Assert.assertTrue(expectedIntValues.isEmpty(), "Expected all values to have been matched in returned array.");
        final JSONArray myArrayProperty = topLevelProperty.getJSONArray("myArray");
        Assert.assertEquals(myArrayProperty.length(), 4, "Expecting 4 entries returned on 'myArray'.");
        for(int myArrayIndex=0; myArrayIndex < myArrayProperty.length(); myArrayIndex++){
            final JSONObject currentArrayEntry = myArrayProperty.getJSONObject(myArrayIndex);
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

    @Test(expectedExceptions = NullPointerException.class, description = "Tests that when custom data in workflow XML specifies a source "
        + "of projectId and the projectId is Null, an Exception is thrown")
    public void nullProjectIdCustomDataTest()
            throws WorkerException, IOException, ScriptException, WorkflowTransformerException,
            URISyntaxException, NoSuchMethodException, DataStoreException, ApiException {
        final ApiClient apiClient = getMockApiClient(null, null);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_4.xml", null, null, apiClient);
        Assert.fail("NullPointerExcepion should have been thrown before this point.");
    }

    @Test(expectedExceptions = ApiException.class, description = "Tests that when custom data in workflow XML specifies a source "
          + "of tenantData and no connection can be made with the processing service, an Exception is thrown")
    public void apiExceptionCustomDataTest()
        throws WorkerException, IOException, ScriptException, WorkflowTransformerException,
               URISyntaxException, NoSuchMethodException, DataStoreException, ApiException
    {
        final ClientHandlerException ex = new ClientHandlerException("Simulating inability to contact the data processing service");
        final ApiClient apiClient = getMockApiClientThatWillThrow(ex);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_5.xml", apiClient);
        
        Assert.fail("ApiException should have been thrown before this point.");
    }

    @Test(expectedExceptions = WorkflowTransformerException.class, description = "Tests that when custom data in workflow XML specifies a source "
          + "of tenantData and no config can be found with the provided key, an WorkflowTransformerException is thrown")
    public void notFoundExceptionCustomDataTest()
        throws WorkerException, IOException, ScriptException, WorkflowTransformerException,
               URISyntaxException, NoSuchMethodException, DataStoreException, ApiException
    {
        final ApiException ex = new ApiException(404, "Simulating inability to find the tenant config.");
        final ApiClient apiClient = getMockApiClientThatWillThrow(ex);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_5.xml", apiClient);
        
        Assert.fail("WorkflowTransformerException should have been thrown before this point.");
    }

    @Test(description = "Tests that when custom data in workflow XML specifies a source "
          + "of tenantData it is resolved correctly by the worker.")
    public void tenantDataCustomDataTest()
        throws WorkerException, IOException, ScriptException, WorkflowTransformerException,
               URISyntaxException, NoSuchMethodException, DataStoreException, ApiException
    {
        final String key = "ee.grammarMap";
        final String value = "{\"pii.xml\": []}";
        final ApiClient apiClient = getMockApiClient(value, key);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_5.xml", apiClient);
                final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        final TestServices testServices = TestServices.createDefault();

        final Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .build();
        invocable.invokeFunction("processDocument", document);

        final Map<String, String> returnedCustomData = document.getTask().getResponse().getCustomData();
        Assert.assertTrue(returnedCustomData.get(key).equals(value), 
                          "Value should match the value supplied to the mock api client.");
    }

    @Test(description = "Tests that a rule that has enabled set to 'false' does not get executed against a document.")
    public void notEnabledRuleNotExecutedTest()
            throws WorkerException, IOException, ScriptException, WorkflowTransformerException,
            URISyntaxException, NoSuchMethodException, DataStoreException, ApiException {
        final ApiClient apiClient = getMockApiClient(null, null);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_3.xml", apiClient);
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        final TestServices testServices = TestServices.createDefault();
        final Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .build();

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

    @Test(description = "Test that a fresh document passed into workflow has correct action to execute added to its fields" +
            " and on subsequent calls the completed fields are correctly updated. Other fields to facilitate progress also" +
            " should be passed.")
    public void actionProgressFieldsTest() throws IOException, ScriptException, URISyntaxException, WorkerException,
            WorkflowTransformerException, NoSuchMethodException, DataStoreException, ApiException {
        final ApiClient apiClient = getMockApiClient(null, null);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_1.xml", apiClient);
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        final TestServices testServices = TestServices.createDefault();
        final Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("test", "string_value").documentBuilder()
                .build();

        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "1");

        // invoke again which should cause next action to be marked for execution
        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "4");
        checkActionsCompleted(document, Arrays.asList("1"));

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
            WorkflowTransformerException, IOException, URISyntaxException, CodecException, DataStoreException, ApiException {
        final ApiClient apiClient = getMockApiClient(null, null);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_1.xml", apiClient);
        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        final TestServices testServices = TestServices.createDefault();
        final Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("CONTENT", "string_value").documentBuilder()
                .build();

        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "1");

        // check that custom data that was set can be serialized (Nashorn ScriptObjectMirror will fail to serialize
        // if it ends up in custom data)
        final Codec codec = new JsonCodec();
        byte[] serializedCustomData = codec.serialise(document.getTask().getResponse().getCustomData());
        Assert.assertNotNull(serializedCustomData, "Custom data from first call should have been serialized.");

        // invoke again which should cause next action to be marked for execution
        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "2");
        checkActionsCompleted(document, Arrays.asList("1"));
        serializedCustomData = codec.serialise(document.getTask().getResponse().getCustomData());
        Assert.assertNotNull(serializedCustomData, "Custom data from second call should have been serialized.");

        // invoke again to start final rule and verify all completed actions and rules are persisted
        invocable.invokeFunction("processDocument", document);

        checkActionIdToExecute(document, "3");
        checkActionsCompleted(document, Arrays.asList("1", "2"));
        serializedCustomData = codec.serialise(document.getTask().getResponse().getCustomData());
        Assert.assertNotNull(serializedCustomData, "Custom data from third call should have been serialized.");
        final HashMap deserializedCustomData = codec.deserialise(serializedCustomData, HashMap.class);
        Assert.assertNotNull(deserializedCustomData, "Deserialized custom data should not be null.");
        final Object grammarMapReturnedObj = deserializedCustomData.get("GRAMMAR_MAP");
        Assert.assertEquals((String) grammarMapReturnedObj, "{pii.xml: []}",
                "Grammar map set on deserialized custom data from response options should have expected value.");
    }

    @Test(description = "Verify that if the onError function is called that the next action to perform on the document is " +
            "determined.")
    public void onErrorTest() throws WorkerException, ScriptException, WorkflowTransformerException, IOException, URISyntaxException, DataStoreException, NoSuchMethodException, ApiException {
        final ApiClient apiClient = getMockApiClient(null, null);
        final String workflowJSStr = getWorkflowJavaScriptFromXML("/test_workflow_2.xml", apiClient);
        final String familyHashingActionId = "29";
        final String langDetectActionId = "30";

        final Invocable invocable = getInvocableWorkflowJavaScriptFromJS(workflowJSStr);
        final TestServices testServices = TestServices.createDefault();
        final Document document = DocumentBuilder.configure()
                .withServices(testServices)
                .withFields()
                .addFieldValue("CONTENT", "test")
                .addFieldValue("DOC_FORMAT_CODE", "345")
                .addFieldValue("DOC_CLASS_CODE", "9")
                .addFieldValue("test", "string_value")
                .addFieldValue("CAF_ACTION_TO_EXECUTE", familyHashingActionId)
                .documentBuilder()
                .build();

        // TODO change this to ErrorEventObject class from worker-document once those changes aer ready and the xslt has been
        // TODO updated to use that version of worker-document
        final Map<String, Object> errorObj = new HashMap<>();
        errorObj.put("rootDocument", document);
        invocable.invokeFunction("onError", errorObj);

        // verify that the document has been marked with the next action to execute
        checkActionIdToExecute(document, langDetectActionId);
        // even though the action failed it has still completed execution for the document so check it has been marked as
        // completed
        checkActionsCompleted(document, Arrays.asList("29"));
    }

    private void checkActionIdToExecute(final Document document, final String expectedActionId){
        Assert.assertEquals(document.getField("CAF_ACTION_TO_EXECUTE").getStringValues().size(), 1,
                "Expecting next action to execute to have been added to the document and any previous values to have been removed.");
        final String returnedActionIdToExecute = document.getField("CAF_ACTION_TO_EXECUTE").getStringValues().get(0);
        Assert.assertEquals(returnedActionIdToExecute, expectedActionId,
                "Expecting action ID of "+expectedActionId+" to have been matched against test document.");
    }

    private void checkActionsCompleted(final Document document, final List<String> expectedActionIds){
        final Field actionsCompletedField = document.getField("CAF_ACTIONS_COMPLETED");
        Assert.assertEquals(actionsCompletedField.getStringValues().size(), expectedActionIds.size(),
                "Completed actions should be the expected size.");
        for(final String expectedActionId: expectedActionIds) {
            Assert.assertTrue( actionsCompletedField.getStringValues().contains(expectedActionId),
                    "Expected action: "+expectedActionId+" to have been marked as completed.");
        }
    }

    private void checkRulesCompleted(final Document document, final List<String> expectedRuleIds){
        final Field rulesCompletedField = document.getField("CAF_PROCESSING_RULES_COMPLETED");
        Assert.assertEquals(rulesCompletedField.getStringValues().size(), expectedRuleIds.size(),
                "Completed actions should be the expected size.");
        for(final String expectedRuleId: expectedRuleIds) {
            Assert.assertTrue( rulesCompletedField.getStringValues().contains(expectedRuleId),
                    "Expected rule: "+expectedRuleId+" to have been marked as completed.");
        }
    }

    private Invocable getInvocableWorkflowJavaScriptFromFullWorkflow(final FullWorkflow workflow, final ApiClient apiClient)
            throws WorkflowTransformerException, ScriptException, URISyntaxException, IOException, ApiException {
        final String workflowAsXml = WorkflowTransformer.transformFullWorkflowToXml(workflow);
        final String workflowAsJS = WorkflowTransformer.transformXmlWorkflowToJavaScript(workflowAsXml, PROJECT_ID, TENANT_ID, apiClient);
        return getInvocableWorkflowJavaScriptFromJS(workflowAsJS);
    }

    private Invocable getInvocableWorkflowJavaScriptFromJS(final String workflowAsJS)
            throws ScriptException {
        final ScriptEngineManager engineManager = new ScriptEngineManager();
        final ScriptEngine engine = engineManager.getEngineByName("nashorn");
        engine.eval(workflowAsJS);
        return (Invocable) engine;
    }

    private Invocable getInvocableWorkflowJavaScriptFromJSResource(final String workflowAsJSResourceIdentifier)
            throws ScriptException, URISyntaxException, IOException {
        final URL testWorkflowJsUrl = this.getClass().getResource(workflowAsJSResourceIdentifier);
        final Path workflowJsPath = Paths.get(testWorkflowJsUrl.toURI());
        final String workflowAsJS = new String(
                Files.readAllBytes(workflowJsPath), StandardCharsets.UTF_8);
        return getInvocableWorkflowJavaScriptFromJS(workflowAsJS);
    }

    private Invocable getInvocableWorkflowJavaScriptFromXML(final String workflowXmlResourceIdentifier)
            throws URISyntaxException, IOException,
            ScriptException, WorkflowTransformerException, ApiException {
        final ApiClient apiClient = getMockApiClient("", "ee.grammarMap");
        final String workflowAsJS = getWorkflowJavaScriptFromXML(workflowXmlResourceIdentifier, apiClient);
        return getInvocableWorkflowJavaScriptFromJS(workflowAsJS);
    }

    private String getWorkflowJavaScriptFromXML(final String workflowXmlResourceIdentifier, final ApiClient apiClient)
            throws IOException, URISyntaxException, WorkflowTransformerException, ApiException {
        final URL testWorkflowXml = this.getClass().getResource(workflowXmlResourceIdentifier);
        final Path workflowXmlPath = Paths.get(testWorkflowXml.toURI());

        return WorkflowTransformer.transformXmlWorkflowToJavaScript(new String(
                Files.readAllBytes(workflowXmlPath), StandardCharsets.UTF_8), PROJECT_ID, TENANT_ID, apiClient);
    }

    private String getWorkflowJavaScriptFromXML(final String workflowXmlResourceIdentifier, final String projectId,
                                                final String tenantId, final ApiClient apiClient)
            throws IOException, URISyntaxException, WorkflowTransformerException, ApiException {
        final URL testWorkflowXml = this.getClass().getResource(workflowXmlResourceIdentifier);
        final Path workflowXmlPath = Paths.get(testWorkflowXml.toURI());

        return WorkflowTransformer.transformXmlWorkflowToJavaScript(new String(
                Files.readAllBytes(workflowXmlPath), StandardCharsets.UTF_8), projectId, tenantId, apiClient);
    }

    private ApiClient getMockApiClient(final String returnValue, final String key) throws ApiException
    {
        final ApiClient client = Mockito.mock(ApiClient.class);
        if (returnValue != null && key != null) {
            final EffectiveTenantConfig config = new EffectiveTenantConfig();
            config.setKey(key);
            config.setValue(returnValue);
            config.setValueType(EffectiveTenantConfigValue.ValueTypeEnum.DEFAULT);
            Mockito.when(client.escapeString(anyString())).thenReturn("");
            Mockito.when(client.selectHeaderAccept(any())).thenReturn("");
            Mockito.when(client.selectHeaderContentType(any())).thenReturn("");
            Mockito.when(client.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(config);
        }
        return client;
    }

    private ApiClient getMockApiClientThatWillThrow(final Exception ex) throws ApiException
    {
        final ApiClient client = Mockito.mock(ApiClient.class);
        Mockito.when(client.escapeString(anyString())).thenReturn("");
        Mockito.when(client.selectHeaderAccept(any())).thenReturn("");
        Mockito.when(client.selectHeaderContentType(any())).thenReturn("");
        Mockito.when(client.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(ex);
        return client;
    }
}
