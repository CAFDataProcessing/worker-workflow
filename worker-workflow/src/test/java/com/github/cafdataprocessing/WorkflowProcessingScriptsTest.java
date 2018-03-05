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
package com.github.cafdataprocessing;

import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Script;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptException;
import org.junit.Assert;
import org.junit.Test;

public class WorkflowProcessingScriptsTest
{
    private static final String TEST_SCRIPT = "function(){print('This is a test!')}";
    private static final String TEST_STORAGE_REF = "TestStorageRef1";
    private static final String TEMP_WORKFLOW_SCRIPT = "temp-workflow.js";
    private static final String WORKFLOW_SCRIPT = "workflow.js";

    @Test
    public void addScriptsTest() throws WorkerException, ScriptException, IOException
    {
        final Document doc = getDocument();
        WorkflowProcessingScripts.setScripts(doc, TEST_SCRIPT, TEST_STORAGE_REF);
        Assert.assertTrue(!doc.getTask().getScripts().isEmpty());
        for (final Script script : doc.getTask().getScripts()) {
            switch (script.getName()) {
                case TEMP_WORKFLOW_SCRIPT:
                    Assert.assertTrue(script.isLoaded());
                    Assert.assertTrue(!script.isInstalled());
                    Assert.assertTrue(script.getScript().equals(TEST_SCRIPT));
                    break;
                case WORKFLOW_SCRIPT:
                    Assert.assertTrue(!script.isLoaded());
                    Assert.assertTrue(script.isInstalled());
                    break;
                default:
                    Assert.fail();
                    break;
            }
        }
    }

    private static Document getDocument() throws WorkerException
    {
        final Map<String, List<DocumentWorkerFieldValue>> fields = new HashMap<>();
        final Document document = DocumentBuilder.configure().withFields(fields).build();
        document.setReference("TestDoc");
        return document;
    }
}
