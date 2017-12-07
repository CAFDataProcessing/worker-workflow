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
import com.github.cafdataprocessing.workflow.constants.WorkflowWorkerConstants;
import com.github.cafdataprocessing.workflow.transform.WorkflowTransformer;
import com.github.cafdataprocessing.workflow.transform.WorkflowTransformerException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.exceptions.PostProcessingFailedException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Worker that will examine task received for a workflow ID, retrieve that workflow using a processing API and generate
 * a JavaScript representation of the workflow that the Document can be executed against to determine the action to perform
 * on the document.
 */
public class WorkflowWorker implements DocumentWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowWorker.class);
    private final DataStore dataStore;
    private final String processingApiUrl;
    private final AdminApi workflowAdminApi;
    private final TransformedWorkflowCache workflowCache;

    /**
     * Instantiates a WorkflowWorker instance to process documents, evaluating them against the workflow referred to by
     * the document.
     * @param workflowWorkerConfiguration configuration for this worker.
     * @param dataStore data store to use when storing data.
     */
    public WorkflowWorker(final WorkflowWorkerConfiguration workflowWorkerConfiguration, final DataStore dataStore)
    {
        this.dataStore = dataStore;
        this.processingApiUrl = workflowWorkerConfiguration.getProcessingApiUrl();
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(this.processingApiUrl);
        this.workflowAdminApi = new AdminApi(apiClient);
        this.workflowCache = new TransformedWorkflowCache(workflowWorkerConfiguration.getWorkflowCachePeriod());
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
            workflowAdminApi.healthCheck();
        } catch (ApiException e) {
            LOG.error("Problem encountered when contacting Processing API to check health: ", e);
            healthMonitor.reportUnhealthy("Processing API communication is unhealthy: " + e.getMessage());
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
        final String projectId = extractedProperties.getProjectId();
        final long workflowId = extractedProperties.getWorkflowId();

        // Get the specified workflow and transform it to a JavaScript representation
        TransformWorkflowResult transformWorkflowResult = workflowCache.getTransformWorkflowResult(workflowId, projectId);
        if(transformWorkflowResult==null) {
            ReentrantLock transformWorkflowLock = workflowCache.getTransformWorkflowLock(workflowId, projectId);
            transformWorkflowLock.lock();
            try {
                transformWorkflowResult = workflowCache.getTransformWorkflowResult(workflowId, projectId);
                if (transformWorkflowResult == null) {
                    transformWorkflowResult = transformWorkflow(document, extractedProperties);
                    if (transformWorkflowResult == null) {
                        LOG.warn("Failure during workflow transformation. Processing of this document will not proceed " +
                                "for this worker.");
                        return;
                    }
                    // Cache the transform result
                    workflowCache.addTransformWorkflowResult(workflowId, projectId, transformWorkflowResult);
                }
            }
            finally {
                transformWorkflowLock.unlock();
            }
        }

        // Evaluate the document against the workflow to execute the first matching action. If document has to be sent to another
        // worker its queue property will be updated to reflect this.
        try
        {
            WorkflowEvaluator.evaluate(document, transformWorkflowResult.getTransformedWorkflow(),
                    transformWorkflowResult.getWorkflowStorageRef());
        } catch (PostProcessingFailedException e) {
            LOG.error("A failure occurred trying to evaluate document against transformed Workflow.", e);
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.WORKFLOW_EVALUATION_FAILED, e.getMessage());
        }
    }

    /**
     * Store the provided workflow in the data store.
     * @param workflowJavaScript workflow to store.
     * @param outputPartialReference partial reference to use when storing.
     * @return storage reference for stored workflow.
     * @throws DataStoreException if there is a failure storing workflow.
     */
    private String storeWorkflow(String workflowJavaScript, String outputPartialReference)
            throws DataStoreException
    {
        return dataStore.store(workflowJavaScript.getBytes(Charset.forName("UTF-8")), outputPartialReference);
    }

    /**
     * Retrieves a workflow referenced in @{code extractedProperties} and creates a script representing that workflow,
     * storing it in the data store.
     * @param document the current document being processed. Any failures during transformation will be recorded on the document.
     * @param extractedProperties contains the properties required in order to transform and store the workflow.
     * @return the result of the workflow transformation or null if a failure occurred.
     */
    private TransformWorkflowResult transformWorkflow(final Document document, final ExtractedProperties extractedProperties) {
        final String workflowJavaScript;
        try {
            workflowJavaScript = WorkflowTransformer.retrieveAndTransformWorkflowToJavaScript(
                    extractedProperties.getWorkflowId(), extractedProperties.getProjectId(),
                    processingApiUrl);
        } catch (ApiException | WorkflowTransformerException e) {
            LOG.error("A failure occurred trying to transform Workflow to JavaScript representation.", e);
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.WORKFLOW_TRANSFORM_FAILED, e.getMessage());
            return null;
        }
        // Store the generated JavaScript in data store so it can be passed to other workers in a compact form
        final String workflowStorageRef;
        try {
            workflowStorageRef = storeWorkflow(workflowJavaScript, extractedProperties.getOutputPartialReference());
        }
        catch(DataStoreException e) {
            LOG.error("A failure occurred trying to store transformed workflow.", e);
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.STORE_WORKFLOW_FAILED, e.getMessage());
            return null;
        }
        return new TransformWorkflowResult(workflowJavaScript, workflowStorageRef);
    }
}
