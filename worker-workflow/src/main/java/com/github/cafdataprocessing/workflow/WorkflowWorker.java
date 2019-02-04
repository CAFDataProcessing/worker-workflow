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

import com.google.gson.Gson;
import com.github.cafdataprocessing.workflow.constants.WorkflowWorkerConstants;
import com.github.cafdataprocessing.workflow.model.WorkflowSettings;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.HealthMonitor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker that will examine task received for a workflow name, it will then look for a javascript file with the same name on disk and add
 * it to the task along with any settings required for the workflow.
 */
public final class WorkflowWorker implements DocumentWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowWorker.class);
    private final WorkflowSettingsRetriever workflowSettingsRetriever;
    private final Map<String, String> availableWorkflows = new HashMap<>();
    private final Map<String, String> availableStoredWorkflows = new HashMap<>();
    private final Map<String, WorkflowSettings> workflowSettings = new HashMap<>();
    private final DataStore dataStore;

    /**
     * Instantiates a WorkflowWorker instance to process documents, evaluating them against the workflow referred to by the document.
     *
     * @param application application context for this worker, used to derive configuration and data store for the worker.
     * @throws IOException when the worker is unable to load the workflow scripts
     * @throws ConfigurationException when workflow directory is not set
     */
    public WorkflowWorker(final Application application) throws IOException, ConfigurationException
    {
        dataStore = application.getService(DataStore.class);
        final Map<String, String> workflowSettingsJson = new HashMap<>();
        final WorkflowWorkerConfiguration workflowWorkerConfiguration = getWorkflowWorkerConfiguration(application);
        final String workflowsDirectory = workflowWorkerConfiguration.getWorkflowsDirectory();
        if(workflowsDirectory == null){
            throw new ConfigurationException("No workflow storage directory was set. Unable to load available workflows.");
        }
        createMapFromFiles(workflowsDirectory, "workflow.js", availableWorkflows);
        createMapFromFiles(workflowsDirectory, "workflowsettings.js", workflowSettingsJson);
        deserializeSettings(workflowSettingsJson);
        this.workflowSettingsRetriever = new WorkflowSettingsRetriever();
        verifyWorkflows();
    }

    private void createMapFromFiles(final String workflowsDirectory, final String fileNameSuffix, final Map<String, String> mapToPopulate)
        throws IOException
    {
        final File dir = new File(workflowsDirectory);
        final FilenameFilter filter = (final File dir1, final String name) -> name.endsWith(fileNameSuffix);
        final String[] workflows = dir.list(filter);
        for (final String filename : workflows) {
            try (FileInputStream fis = new FileInputStream(new File(workflowsDirectory + "/" + filename))) {
                final String entryname = filename.endsWith("settings.js")
                    ? filename.replace("settings.js", "")
                    : filename.replace(".js", "");
                mapToPopulate.put(entryname, IOUtils.toString(fis, StandardCharsets.UTF_8));
            }
        }
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
            workflowSettingsRetriever.checkHealth();
        } catch (final Exception e) {
            LOG.error("Problem encountered when contacting Settings Service to check health: ", e);
            healthMonitor.reportUnhealthy("Settings Service communication is unhealthy: " + e.getMessage());
        }
    }

    private static WorkflowWorkerConfiguration getWorkflowWorkerConfiguration(final Application application)
    {
        try {
            return application
                .getService(ConfigurationSource.class)
                .getConfiguration(WorkflowWorkerConfiguration.class);
        } catch (final ConfigurationException e) {
            LOG.warn("Unable to load WorkflowWorkerConfiguration.");
            return new WorkflowWorkerConfiguration();
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
    public void processDocument(final Document document) throws InterruptedException, DocumentWorkerTransientException
    {
        // Get the workflow specification passed in
        final String workflowName = document.getCustomData("workflowName");
        final String workflowScript = availableWorkflows.get(workflowName);
        if (workflowName == null || workflowScript == null) {
            LOG.warn("Custom data on document is not valid for this worker. Processing of this document will not "
                + "proceed for this worker.");
            return;
        }

        // Add the workflow scripts to the document task.
        try {
            // Retrieve required workflow settings
            workflowSettingsRetriever.retrieveWorkflowSettings(workflowSettings.get(workflowName), document);
            final String workflowStorageRef = availableStoredWorkflows.get(workflowName) != null
                ? availableStoredWorkflows.get(workflowName)
                : storeWorkflow(workflowName, availableWorkflows.get(workflowName), document);
            WorkflowProcessingScripts.setScripts(
                document,
                availableWorkflows.get(workflowName),
                workflowStorageRef);
        } catch (final com.microfocus.darwin.settings.client.ApiException ex) {
            document.addFailure("API_EXCEPTION", ex.getMessage());
        } catch (final DataStoreException ex) {
            LOG.error("A failure occurred trying to store workflow in data store.", ex);
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.STORE_WORKFLOW_FAILED, ex.getMessage());
        } catch (final ScriptException ex) {
            LOG.error("A failure occurred trying to add the scripts to the task.", ex);
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.ADD_WORKFLOW_SCRIPTS_FAILED, ex.getMessage());
        }
    }

    private void deserializeSettings(final Map<String, String> workflowSettingsJson)
    {
        final Gson gson = new Gson();
        for (final Map.Entry<String, String> entry : workflowSettingsJson.entrySet()) {
            workflowSettings.put(entry.getKey(),
                                 gson.fromJson(entry.getValue(), WorkflowSettings.class));
        }
    }

    private String storeWorkflow(final String workflowName, final String workflowjs, final Document document) throws DataStoreException
    {
        final String outputPartialReference = document.getCustomData("outputPartialReference") != null
            ? document.getCustomData("outputPartialReference")
            : "";
        final String storageRef = dataStore.store(workflowjs.getBytes(StandardCharsets.UTF_8), outputPartialReference);
        availableStoredWorkflows.put(workflowName, storageRef);
        return storageRef;
    }

    private void verifyWorkflows() throws ConfigurationException
    {
        if(workflowSettings.size() != availableWorkflows.size()){
            throw new ConfigurationException("Configuration mismatch, number of settings files does not match number of workflows.");
        }
        if(availableWorkflows.isEmpty()){
            throw new ConfigurationException("No workflows available.");
        }
    }
}
