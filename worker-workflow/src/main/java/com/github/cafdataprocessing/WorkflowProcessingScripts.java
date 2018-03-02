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

import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Script;
import javax.script.ScriptException;

/**
 * Adds a JavaScript workflow to a document's task
 */
final class WorkflowProcessingScripts
{
    private WorkflowProcessingScripts()
    {
    }

    /**
     * Sets the workflow script used in evaluating a document on the task, also sets a temporary script used for evaluating which worker
     * to send the document to next. The temporary script is used to remove the need for datastore retrieval of the script before
     * processing.
     *
     * @param document The document used to provide access to the task.
     * @param workflowAsJavaScript JavaScript representation of a workflow.
     * @param workflowStorageRef storage reference for the workflow passed.
     * @throws ScriptException if there is a failure in workflow script loading.
     */
    public static void setScripts(final Document document, final String workflowAsJavaScript, final String workflowStorageRef)
        throws ScriptException
    {
        // Add temporary script to the task using the setScriptInline setter.
        setWorkflowProcessingScripts(document, workflowAsJavaScript, ScriptType.InlineScript, "temp-workflow.js", false);
        // Add persistant script to the task using the setScriptByReference setter.
        setWorkflowProcessingScripts(document, workflowAsJavaScript, ScriptType.StorageReference, "workflow.js", true);
    }

    /**
     * Sets a script on the task using two different methods, referencing the script by its storage reference and adding the script using
     * an inline setter.
     *
     * @param document Document to set post processing information on.
     * @param script The script to add to task, this can either be a string representation of the script or a storage reference for the
     * script in the datastore.
     * @param scriptType The type of add that should be performed when adding the script to the task.
     * @param scriptName The name to give the script when adding it to the task.
     * @param persist This is whether or not the script should be passed on in the response.
     * @throws ScriptException if there is a failure in workflow script loading.
     */
    private static void setWorkflowProcessingScripts(final Document document, final String script, final ScriptType scriptType,
                                                     final String scriptName, final Boolean persist) throws ScriptException
    {
        final Script scriptToAdd = document.getTask().getScripts().add();
        switch (scriptType) {
            case StorageReference: {
                scriptToAdd.setScriptByReference(script);
                break;
            }
            case InlineScript: {
                scriptToAdd.setScriptInline(script);
                break;
            }
            default: {
                throw new ScriptException("No valid script type passed.");
            }
        }

        scriptToAdd.setName(scriptName);
        scriptToAdd.load();
        if (!persist) {
            scriptToAdd.uninstall();
        }
    }

}

enum ScriptType
{
    StorageReference,
    InlineScript
}
