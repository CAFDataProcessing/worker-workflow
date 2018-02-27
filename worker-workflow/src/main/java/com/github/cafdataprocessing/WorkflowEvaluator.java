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

import com.hpe.caf.worker.document.JavaScriptDocumentPostProcessor;
import com.hpe.caf.worker.document.exceptions.PostProcessingFailedException;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Script;


/**
 * Evaluates a JavaScript workflow against a document, updating document based on matched actions in the workflow
 */
final class WorkflowEvaluator
{
    private WorkflowEvaluator(){}

    /**
     * Evaluates the provided document against the JavaScript workflow and records the workflow storage reference on
     * the document for execution in post processing.
     * @param document The document to evaluate against the workflow. May be modified by evaluation
     *                 e.g. fields indicating action being executed, the queue to send document to, the post processing
     *                 script to execute.
     * @param workflowAsJavaScript JavaScript representation of a workflow.
     * @param workflowStorageRef storage reference for the workflow passed. Will be recorded as a post processing script
     *                           if none is already set.
     * @throws PostProcessingFailedException if there is a failure in workflow script execution.
     */
    public static void evaluate(final Document document, final String workflowAsJavaScript, final String workflowStorageRef)
            throws PostProcessingFailedException
    {
        final JavaScriptDocumentPostProcessor jsPostProcessor = new JavaScriptDocumentPostProcessor(workflowAsJavaScript);
        jsPostProcessor.postProcessDocument(document);
        setWorkflowPostProcessingScript(document, workflowStorageRef);
    }

    /**
     * Sets the post processing on provided document to use workflow in provided storage reference.
     * If a workflow for post processing is already set on the document then it will be replaced with the new value.
     * @param document Document to set post processing information on.
     * @param workflowStorageReference DataStore reference for a workflow that can be executed in post processing.
     */
    private static void setWorkflowPostProcessingScript(final Document document, final String workflowStorageReference)
    {
        final Script script = document.getTask().getScripts().add();
        script.setScriptByReference(workflowStorageReference);
        script.setName("postProcessingScript");
    }
}
