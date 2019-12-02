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

import com.github.cafdataprocessing.workflow.model.Workflow;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Script;
import com.hpe.caf.worker.document.model.Scripts;

import javax.script.ScriptException;

public class ScriptManager {
    /**
     * Sets the workflow script used in evaluating a document on the task, also sets a temporary script used for evaluating which worker
     * to send the document to next. The temporary script is used to remove the need for datastore retrieval of the script before
     * processing.
     *
     * @param workflow The workflow.
     * @param document The document used to provide access to the task.
     * @throws ScriptException if there is a failure in workflow script loading.
     */
    public void applyScriptToDocument(final Workflow workflow, final Document document) throws ScriptException {
        final Scripts scripts = document.getTask().getScripts();

        // Add temporary script to the task using the setScriptInline setter.
        final Script tempWorkflowScript = scripts.add();
        tempWorkflowScript.setName("temp-workflow.js");
        tempWorkflowScript.setScriptInline(workflow.getWorkflowScript());
        tempWorkflowScript.load();

        // Add persistent script to the task using the setScriptByReference setter.
        final Script workflowScript = scripts.add();
        workflowScript.setName("workflow.js");
        workflowScript.setScriptByReference(workflow.getStorageReferenceForWorkflowScript());
        workflowScript.install();
    }
}
