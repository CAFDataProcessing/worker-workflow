/*
 * Copyright 2017-2024 Open Text.
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

import com.github.cafdataprocessing.workflow.model.Workflow;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Script;
import com.hpe.caf.worker.document.model.Scripts;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class ScriptManagerTest {

    // @Test
    public void applyScriptsTest() throws Exception {

        final ScriptManager scriptManager = new ScriptManager();
        final Workflow workflow = new Workflow();
        final TestServices testServices = TestServices.createDefault();
        final String testScript = "var a = 'example';";

        workflow.setWorkflowScript(testScript);
        final String storedScriptReference = testServices.getDataStore().store(
                new ByteArrayInputStream(workflow.getWorkflowScript().getBytes(StandardCharsets.UTF_8)), "partial");

        workflow.setStorageReference(storedScriptReference);

        final Document document = DocumentBuilder.configure().withServices(testServices)
                .withCustomData()
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        scriptManager.applyScriptToDocument(workflow, document);

        final Scripts scripts = document.getTask().getScripts();
        assertEquals(2, scripts.size());

        final Script inlineScript = scripts.get(0);
        assertEquals("temp-workflow.js", inlineScript.getName());
        assertEquals(testScript, inlineScript.getScript());

        final Script referencedScript = scripts.get(1);
        assertEquals("workflow.js", referencedScript.getName());
        assertEquals(testScript, referencedScript.getScript());
    }
}
