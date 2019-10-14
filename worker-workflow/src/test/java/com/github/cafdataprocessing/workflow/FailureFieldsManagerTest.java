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

import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import static org.junit.Assert.*;
import org.junit.Test;

public final class FailureFieldsManagerTest
{
    private final FailureFieldsManager failureFieldsManager = new FailureFieldsManager();

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
}
