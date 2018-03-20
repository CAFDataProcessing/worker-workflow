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

import com.github.cafdataprocessing.processing.service.client.ApiClient;
import com.github.cafdataprocessing.processing.service.client.ApiException;
import com.github.cafdataprocessing.processing.service.client.api.AdminApi;
import com.github.cafdataprocessing.processing.service.client.model.HealthStatus;
import com.github.cafdataprocessing.processing.service.client.model.HealthStatusDependencies;
import com.github.cafdataprocessing.workflow.constants.WorkflowWorkerConstants;
import com.github.cafdataprocessing.workflow.transform.WorkflowRetrievalException;
import com.github.cafdataprocessing.workflow.transform.WorkflowTransformerException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.HealthMonitor;
import com.hpe.caf.worker.document.model.Script;
import javax.script.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker that will examine task received for a workflow ID, retrieve that workflow using a processing API and generate
 * a JavaScript representation of the workflow that the Document can be executed against to determine the action to perform
 * on the document.
 */
public final class WorkflowWorker implements DocumentWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowWorker.class);
    private final String processingApiUrl;
    private final AdminApi workflowAdminApi;
    private final TransformedWorkflowCache workflowCache;

    /**
     * Instantiates a WorkflowWorker instance to process documents, evaluating them against the workflow referred to by
     * the document.
     * @param application application context for this worker, used to derive configuration and data store for the worker.
     */
    public WorkflowWorker(final Application application)
    {
        final DataStore dataStore = application.getService(DataStore.class);
        final WorkflowWorkerConfiguration workflowWorkerConfiguration = getWorkflowWorkerConfiguration(application);
        this.processingApiUrl = workflowWorkerConfiguration.getProcessingApiUrl();
        this.workflowAdminApi = getWorkflowAdminApi();
        this.workflowCache = new TransformedWorkflowCache(workflowWorkerConfiguration.getWorkflowCachePeriod(), dataStore,
                processingApiUrl);
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
            final HealthStatus healthStatus = workflowAdminApi.healthCheck();
            if(HealthStatus.StatusEnum.HEALTHY.equals(healthStatus.getStatus())){
                return;
            }
            final StringBuilder dependenciesStatusBuilder = new StringBuilder();
            for(final HealthStatusDependencies healthDependency: healthStatus.getDependencies()) {
                dependenciesStatusBuilder.append(" ");
                dependenciesStatusBuilder.append(healthDependency.getName());
                dependenciesStatusBuilder.append(":");
                dependenciesStatusBuilder.append(healthDependency.getStatus().toString());
            }
            healthMonitor.reportUnhealthy("Processing API communication is unhealthy. Service dependencies:"
                    +dependenciesStatusBuilder.toString());
        } catch (final Exception e) {
            LOG.error("Problem encountered when contacting Processing API to check health: ", e);
            healthMonitor.reportUnhealthy("Processing API communication is unhealthy: " + e.getMessage());
        }
    }

    /**
     * Retrieves a new AdminApi instance configured to talk to the currently set processing API for this worker.
     * @return AdminApi pointing to configured processing API for the worker.
     */
    private AdminApi getWorkflowAdminApi() {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(this.processingApiUrl);
        return new AdminApi(apiClient);
    }

    private static WorkflowWorkerConfiguration getWorkflowWorkerConfiguration(final Application application) {
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
     * Processes a single document. Retrieving the workflow it refers to, transforming that workflow to a runnable script,
     * evaluating the document against the workflow to determine where it should be sent to and storing the workflow
     * on the document so the next worker may re-evaluate the document once it has finished its action.
     *
     * @param document the document to be processed.
     * @throws InterruptedException if any thread has interrupted the current thread
     * @throws DocumentWorkerTransientException if the document could not be processed
     */
    @Override
    public void processDocument(final Document document) throws InterruptedException, DocumentWorkerTransientException
    {
        // Get the worker task properties passed in custom data
        final ExtractedProperties extractedProperties = CustomDataExtractor.extractPropertiesFromCustomData(document);
        if(!extractedProperties.isValid()) {
            LOG.warn("Custom data on document is not valid for this worker. Processing of this document will not " +
                    "proceed for this worker.");
            return;
        }

        // Get the specified workflow and transform it to a JavaScript representation
        final TransformWorkflowResult transformWorkflowResult = transformWorkflow(extractedProperties, document);
        if(transformWorkflowResult==null){
            LOG.warn("Failure during workflow transformation. Processing of this document will not proceed " +
                    "for this worker.");
            return;
        }

        // Add the workflow scripts to the document task.
        try
        {
            WorkflowProcessingScripts.setScripts(document, transformWorkflowResult.getTransformedWorkflow(),
                    transformWorkflowResult.getWorkflowStorageRef());
        } catch (final ScriptException e) {
            LOG.error("A failure occurred trying to add the scripts to the task.", e);
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.ADD_WORKFLOW_SCRIPTS_FAILED, e.getMessage());
        }
    }

    /**
     * Retrieves transformed workflow result based on extracted properties passed. If unable to retrieve a result then
     * a failure will be added to the passed {@code document} and {@code null} returned.
     * @param extractedProperties properties to use in workflow retrieval and transformation.
     * @param document document to update with failure details in event of any issues.
     * @return the transformed workflow result matching provided properties or null if there is a failure retrieving the
     * transformed workflow result.
     * @throws DocumentWorkerTransientException if there is a transient failure during workflow transformation.
     */
    private TransformWorkflowResult transformWorkflow(final ExtractedProperties extractedProperties, final Document document)
            throws DocumentWorkerTransientException {
        try {
            try {
                return workflowCache.getTransformWorkflowResult(extractedProperties.getWorkflowId(),
                        extractedProperties.getProjectId(), extractedProperties.getOutputPartialReference(),
                        extractedProperties.getTenantId());
            } catch (final ApiException firstException) {
                // may have been a transient API issue, check if the API is healthy
                final HealthStatus processingApiHealth = workflowAdminApi.healthCheck();
                if (!HealthStatus.StatusEnum.HEALTHY.equals(processingApiHealth.getStatus())) {
                    // processing API is unhealthy, throw a transient exception
                    LOG.info("Unable to transform workflow as processing API is unhealthy.");
                    throw new DocumentWorkerTransientException(
                            "Unable to transform workflow. Processing API is unhealthy.");
                }
                // API is healthy so attempt to retrieve transform result one more time (in case it was temporarily
                // unhealthy previously).
                LOG.info("Attempting to get transformed workflow a second time after ApiException was thrown.");
                return workflowCache.getTransformWorkflowResult(extractedProperties.getWorkflowId(),
                        extractedProperties.getProjectId(), extractedProperties.getOutputPartialReference(),
                        extractedProperties.getTenantId());
            }
        } catch (final DataStoreException e) {
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.STORE_WORKFLOW_FAILED, e.getMessage());
        } catch (final ApiException | WorkflowTransformerException e) {
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.WORKFLOW_TRANSFORM_FAILED, e.getMessage());
        } catch (final WorkflowRetrievalException e) {
            throw new DocumentWorkerTransientException(
                    "Unable to transform workflow. Processing API communication is unhealthy.", e);
        }
        return null;
    }
}
