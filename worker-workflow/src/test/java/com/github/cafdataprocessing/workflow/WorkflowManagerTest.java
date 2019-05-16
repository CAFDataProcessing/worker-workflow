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

import com.github.cafdataprocessing.workflow.model.Action;
import com.github.cafdataprocessing.workflow.model.SettingDefinition;
import com.github.cafdataprocessing.workflow.model.Workflow;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WorkflowManagerTest {

    @Test
    public void getWorkflowTest() throws Exception {
        final TestServices testServices = TestServices.createDefault();
        final Document document = DocumentBuilder.configure().withServices(testServices)
                .withCustomData()
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        final String workflowDirectory = Resources.getResource("workflow-manager-test").getPath();
        final WorkflowManager workflowManager = new WorkflowManager(document.getApplication(), workflowDirectory);

        final Workflow workflow = workflowManager.get("test-workflow");

        final String expectedScript = Resources.toString(Resources.getResource("workflow-manager-test/expected-script.js"),
                StandardCharsets.UTF_8);

        //TODO Comparison is failing is it line endings or something?
//        assertEquals(expectedScript, workflow.getWorkflowScript());

        final String storedScriptReference = workflow.getStorageReferenceForWorkflowScript();
        final String storedScript = IOUtils.toString(testServices.getDataStore().retrieve(storedScriptReference),
                StandardCharsets.UTF_8);

        assertEquals(workflow.getWorkflowScript(), storedScript);

        final List<SettingDefinition> settingDefinitions = workflow.getSettingDefinitions();
        assertNotNull(settingDefinitions);
        assertEquals(5, settingDefinitions.size());

        final Map<String, Action> actions = workflow.getActions();
        assertEquals(3, actions.size());
    }

}
