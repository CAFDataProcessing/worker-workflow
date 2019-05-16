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

import com.github.cafdataprocessing.workflow.model.Workflow;
import com.google.common.base.Strings;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.HealthMonitor;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

/**
 * Worker that will examine task received for a workflow name, it will then look for a javascript file with the same name on disk and add
 * it to the task along with any settings required for the workflow.
 */
public final class WorkflowWorker implements DocumentWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowWorker.class);
    private final WorkflowManager workflowManager;
    private final ScriptManager scriptManager;
    private final SettingsManager settingsManager;

    /**
     * Instantiates a WorkflowWorker instance to process documents, evaluating them against the workflow referred to by the document.
     *
     * @param application application context for this worker, used to derive configuration and data store for the worker.
     * @throws IOException when the worker is unable to load the workflow scripts
     * @throws ConfigurationException when workflow directory is not set
     */
    public WorkflowWorker(final Application application,
                          final WorkflowWorkerConfiguration workflowWorkerConfiguration)
            throws ConfigurationException
    {
        final String workflowsDirectory = workflowWorkerConfiguration.getWorkflowsDirectory();
        if(workflowsDirectory == null){
            throw new ConfigurationException("No workflow storage directory was set. Unable to load available workflows.");
        }

        this.workflowManager = new WorkflowManager(application, workflowsDirectory);
        this.scriptManager = new ScriptManager();
        this.settingsManager = new SettingsManager();
    }

    /**
     * This method provides an opportunity for the worker to report if it has any problems which would prevent it processing documents
     * correctly. If the worker is healthy then it should simply return without calling the health monitor.
     *
     * @param healthMonitor used to report the health of the application
     */
    @Override
    public void checkHealth(final HealthMonitor healthMonitor)
    {
        try {
            settingsManager.checkHealth();
        } catch (final Exception e) {
            LOG.error("Problem encountered when contacting Settings Service to check health: ", e);
            healthMonitor.reportUnhealthy("Settings Service communication is unhealthy: " + e.getMessage());
        }
    }

    /**
     * Processes a single document. Retrieving the workflow it refers to, evaluating the document against the workflow to determine where
     * it should be sent to and storing the workflow on the document so the next worker may re-evaluate the document once it has finished
     * its action.
     *
     * @param document the document to be processed.
     * @throws InterruptedException if any thread has interrupted the current thread
     * @throws DocumentWorkerTransientException if the document could not be processed
     */
    @Override
    public void processDocument(final Document document) throws DocumentWorkerTransientException
    {
        // Get the workflow specification passed in
        final String workflowName = document.getCustomData("workflowName");

        if (Strings.isNullOrEmpty(workflowName)) {
            final String errorMessage = String.format("No 'workflowName' in customData of document [%s].",
                    document.getReference());
            LOG.error(errorMessage);
            document.addFailure("NO_WORKFLOW", errorMessage);
            return;
        }

        final Workflow workflow = workflowManager.get(workflowName);
        if (workflow == null) {
            final String errorMessage = String.format("Workflow [%s] is not available for document [%s].",
                    workflowName, document.getReference());
            LOG.error(errorMessage);
            document.addFailure("WORKFLOW_NOT_FOUND", errorMessage);
            return;
        }

        settingsManager.applySettingsCustomData(workflow.getSettingsForCustomData(), document);

        try {
            scriptManager.applyScriptToDocument(workflow, document);
        } catch (ScriptException e) {
            document.addFailure("SCRIPT_EXCEPTION", e.getMessage());
        }
    }
}
