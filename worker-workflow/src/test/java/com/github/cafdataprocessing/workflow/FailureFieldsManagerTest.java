/*
 * Copyright 2017-2020 Micro Focus or one of its affiliates.
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

import com.github.cafdataprocessing.workflow.testing.utils.WorkflowHelper;
import com.google.gson.Gson;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import static org.junit.Assert.*;
import java.nio.file.Paths;
import java.util.Objects;
import javax.script.Invocable;
import org.junit.Test;

public final class FailureFieldsManagerTest
{
    private final FailureFieldsManager failureFieldsManager = new FailureFieldsManager();
    private final Gson gson = new Gson();

    @Test
    public void testMultipleFailureSubFields() throws Exception
    {
        final Document document = DocumentBuilder.configure()
            .withCustomData()
            .add("extraFailuresSubfieldKey0", "AJP_JOB_RUN_ID")
            .add("extraFailuresSubfieldValue0", "1701")
            .add("extraFailuresSubfieldKey1", "AJP_WORK_UNIT_ID")
            .add("extraFailuresSubfieldValue1", "74656")
            .add("extraFailuresSubfieldKey2", "KEY_THREE")
            .add("extraFailuresSubfieldValue2", "74205")
            .documentBuilder()
            .withFields()
            .documentBuilder()
            .build();
        final String verificationString = "{\"AJP_JOB_RUN_ID\":\"1701\",\"AJP_WORK_UNIT_ID\":\"74656\",\"KEY_THREE\":\"74205\"}";
        failureFieldsManager.handleExtraFailureSubFields(document);

        assertTrue(document.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS").hasValues());
        assertTrue(document.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS")
            .getStringValues().stream().findFirst().get().equals(verificationString));

    }

    @Test
    public void testNoFailureSubFields() throws Exception
    {
        final Document document = DocumentBuilder.configure()
            .withCustomData()
            .documentBuilder()
            .withFields()
            .documentBuilder()
            .build();
        failureFieldsManager.handleExtraFailureSubFields(document);
        assertFalse(document.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS").hasValues());
    }

    /**
     * Simulating a poison message returning to the workflow worker for a second time due to a failure during processing.
     * The failure subfields should not be overwritten.
     */
    @Test
    public void testFailureSubFieldsAlreadyPresent() throws Exception
    {
        final Document document = DocumentBuilder.configure()
            .withCustomData()
            .add("extraFailuresSubfieldKey0", "AJP_JOB_RUN_ID")
            .add("extraFailuresSubfieldValue0", "21445")
            .add("extraFailuresSubfieldKey1", "AJP_WORK_UNIT_ID")
            .add("extraFailuresSubfieldValue1", "59650")
            .add("extraFailuresSubfieldKey2", "KEY_THREE")
            .add("extraFailuresSubfieldValue2", "72381")
            .documentBuilder()
            .withFields()
            .addFieldValue("CAF_WORKFLOW_SETTINGS", "Previously added field value")
            .addFieldValue("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS",
                           "{\"AJP_JOB_RUN_ID\":\"1701\",\"AJP_WORK_UNIT_ID\":\"74656\",\"KEY_THREE\":\"74205\"}")
            .documentBuilder()
            .build();
        failureFieldsManager.handleExtraFailureSubFields(document);
        final String verificationString = "{\"AJP_JOB_RUN_ID\":\"1701\",\"AJP_WORK_UNIT_ID\":\"74656\",\"KEY_THREE\":\"74205\"}";
        assertTrue(document.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS").hasValues());
        assertFalse(document.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS").hasChanges());
        assertTrue(document.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS")
            .getStringValues().stream().findFirst().get().equals(verificationString));
    }

    @Test
    public void callingAddFailuresFromOutsideScript() throws Exception
    {
        final Invocable invocable = WorkflowHelper.createInvocableNashornEngineWithActionsAndWorkflowControl(
            "function extractSource(failure){return \"kv\"}",
            "function testDocument(document, failures, source){thisScriptObject.addFailures(document, failures, extractSource, source);}");

        // doc with one original failure
        final Document builderDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-with-subdoc-with-stack.json").toString()).build();

        final Document document = WorkflowHelper.createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(),
                                                                null, builderDoc.getSubdocuments(), builderDoc, builderDoc, true, true);
        invocable.invokeFunction("testDocument", document, document.getFailures(), "on_premise");

        assertEquals(document.getField("FAILURES").getValues().stream().filter(x -> !x.getStringValue().isEmpty()).count(), 1L);

        final String firstFailure = document.getField("FAILURES").getValues()
            .stream()
            .filter(v -> !v.getStringValue().isEmpty())
            .map(v -> v.getStringValue())
            .findFirst()
            .get();
        final FailureRep failureRep = new FailureRep("original_fail_id_1", "super stack", "on_premise", "kv", "example_workflow",
                                                     "original message 1", null);
        assertEquals(failureRep, gson.fromJson(firstFailure, FailureRep.class));
    }
    
    @Test
    public void callingAddFailuresFromOutsideScriptForWarnings() throws Exception
    {
        final Invocable invocable = WorkflowHelper.createInvocableNashornEngineWithActionsAndWorkflowControl(
            "function extractSource(failure){return \"KV:13\"}",
            "function isWarning(failure) {var warnings = [\"KV:7\", \"KV:8\", \"KV:10\", \"KV:11\", \"KV:13\"]; return warnings.indexOf(failure.failureId) !== -1;}",
            "function testDocument(document, failures, source){thisScriptObject.addFailures(document, failures, extractSource, source);}");

        // doc with one original failure
        final Document builderDoc = DocumentBuilder.fromFile(
            Paths.get("src", "test", "resources", "input-document-with-subdoc-for-warning-test.json").toString()).build();

        final Document document = WorkflowHelper.createDocument("ref_1", builderDoc.getFields(), builderDoc.getFailures(),
                                                                null, builderDoc.getSubdocuments(), builderDoc, builderDoc, true, true);
        invocable.invokeFunction("testDocument", document, document.getFailures(), "on_premise");

        assertEquals(document.getField("WARNINGS").getValues().stream().filter(x -> !x.getStringValue().isEmpty()).count(), 1L);

        final String firstWarning = document.getField("WARNINGS").getValues()
            .stream()
            .filter(v -> !v.getStringValue().isEmpty())
            .map(v -> v.getStringValue())
            .findFirst()
            .get();
        WarningRep firstWarningRep = gson.fromJson(firstWarning, WarningRep.class);
        final WarningRep warningRep = new WarningRep("KV:13", "on_premise", "KV:13", "example_workflow",
                                                     "Failed to open KV stream: No reader available for this format", null);
        assertEquals(warningRep, firstWarningRep);
    }
}

final class FailureRep extends Object
{
    private final String ID;
    private final String STACK;
    private final String WORKFLOW_ACTION;
    private final String COMPONENT;
    private final String WORKFLOW_NAME;
    private final String MESSAGE;
    private final String DATE;

    public FailureRep(final String ID, final String Stack, final String WORKFLOW_ACTION, final String COMPONENT,
                      final String WORKFLOW_NAME, final String MESSAGE, final String DATE)
    {
        this.ID = ID;
        this.STACK = Stack;
        this.WORKFLOW_ACTION = WORKFLOW_ACTION;
        this.COMPONENT = COMPONENT;
        this.WORKFLOW_NAME = WORKFLOW_NAME;
        this.MESSAGE = MESSAGE;
        this.DATE = DATE;
    }

    @Override
    public boolean equals(final Object obj)
    {
        final FailureRep passedRep = (FailureRep) obj;
        return this.ID.equals(passedRep.ID) && this.STACK.equals(passedRep.STACK)
            && this.WORKFLOW_ACTION.equals(passedRep.WORKFLOW_ACTION)
            && this.WORKFLOW_NAME.equals(passedRep.WORKFLOW_NAME)
            && this.COMPONENT.equals(passedRep.COMPONENT)
            && this.MESSAGE.equals(passedRep.MESSAGE);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.ID);
        hash = 67 * hash + Objects.hashCode(this.STACK);
        hash = 67 * hash + Objects.hashCode(this.WORKFLOW_ACTION);
        hash = 67 * hash + Objects.hashCode(this.COMPONENT);
        hash = 67 * hash + Objects.hashCode(this.WORKFLOW_NAME);
        hash = 67 * hash + Objects.hashCode(this.MESSAGE);
        return hash;
    }
}

final class WarningRep extends Object
{
    private final String ID;
    private final String WORKFLOW_ACTION;
    private final String COMPONENT;
    private final String WORKFLOW_NAME;
    private final String MESSAGE;
    private final String DATE;

    public WarningRep(final String ID, final String WORKFLOW_ACTION, final String COMPONENT,
                      final String WORKFLOW_NAME, final String MESSAGE, final String DATE)
    {
        this.ID = ID;
        this.WORKFLOW_ACTION = WORKFLOW_ACTION;
        this.COMPONENT = COMPONENT;
        this.WORKFLOW_NAME = WORKFLOW_NAME;
        this.MESSAGE = MESSAGE;
        this.DATE = DATE;
    }

    @Override
    public boolean equals(final Object obj)
    {
        final WarningRep passedRep = (WarningRep) obj;
        return this.ID.equals(passedRep.ID) 
            && this.WORKFLOW_ACTION.equals(passedRep.WORKFLOW_ACTION)
            && this.WORKFLOW_NAME.equals(passedRep.WORKFLOW_NAME)
            && this.COMPONENT.equals(passedRep.COMPONENT)
            && this.MESSAGE.equals(passedRep.MESSAGE);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.ID);
        hash = 67 * hash + Objects.hashCode(this.WORKFLOW_ACTION);
        hash = 67 * hash + Objects.hashCode(this.COMPONENT);
        hash = 67 * hash + Objects.hashCode(this.WORKFLOW_NAME);
        hash = 67 * hash + Objects.hashCode(this.MESSAGE);
        return hash;
    }
}

