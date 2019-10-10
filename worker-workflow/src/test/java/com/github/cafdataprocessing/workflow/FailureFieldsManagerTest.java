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
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

public final class FailureFieldsManagerTest
{
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
        final Map<String, String> failureSubfields = FailureFieldsManager.retrieveExtraFailureSubfields(document);
        assertFalse(failureSubfields.isEmpty());
        assertTrue(failureSubfields.containsKey("AJP_JOB_RUN_ID"));
        assertTrue(failureSubfields.get("AJP_JOB_RUN_ID").equals("1701"));
        assertTrue(failureSubfields.containsKey("AJP_WORK_UNIT_ID"));
        assertTrue(failureSubfields.get("AJP_WORK_UNIT_ID").equals("74656"));
        assertTrue(failureSubfields.containsKey("KEY_THREE"));
        assertTrue(failureSubfields.get("KEY_THREE").equals("74205"));

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
        final Map<String, String> failureSubfields = FailureFieldsManager.retrieveExtraFailureSubfields(document);
        assertTrue(failureSubfields.isEmpty());
    }
}
