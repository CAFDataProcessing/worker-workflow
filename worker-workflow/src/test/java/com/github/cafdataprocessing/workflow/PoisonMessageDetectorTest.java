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

public final class PoisonMessageDetectorTest
{
    @Test
    public void testNonPoisonDocument() throws Exception
    {
        final Document document = DocumentBuilder.configure()
            .withCustomData()
            .add("workflowName", "ingestionWorkflow")
            .documentBuilder()
            .withFields()
            .addFieldValue("CAF_WORKFLOW_SETTINGS", "Passed by agent job processor")
            .documentBuilder()
            .build();

        assertFalse(PoisonMessageDetector.isPoisonDocument(document));
    }

    @Test
    public void testPoisonDocument() throws Exception
    {
        final Document document = DocumentBuilder.configure()
            .withCustomData()
            .documentBuilder()
            .withFields()
            .addFieldValue("CAF_WORKFLOW_SETTINGS", "Passed by bad actor")
            .documentBuilder()
            .build();
        assertTrue(PoisonMessageDetector.isPoisonDocument(document));
    }
}
