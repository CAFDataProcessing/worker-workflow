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

import com.github.cafdataprocessing.processing.service.client.ApiException;
import com.github.cafdataprocessing.workflow.transform.WorkflowRetrievalException;
import com.github.cafdataprocessing.workflow.transform.WorkflowTransformer;
import com.github.cafdataprocessing.workflow.transform.WorkflowTransformerException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Caches transformed workflow representations for reuse.
 */
final class TransformedWorkflowCache
{
    private static final Logger LOG = LoggerFactory.getLogger(TransformedWorkflowCache.class);

    private final DataStore dataStore;
    private final String processingApiUrl;
    private final LoadingCache<TransformedWorkflowCacheKey, TransformWorkflowResult> workflowCache;

    /**
     * Initialize a TransformedWorkflowCache instance with each entry set to expire after the provided duration.
     * @param workflowCachePeriodAsStr an ISO-8601 time duration string. If passed as {@code null} or empty a default value of
     *                                 5 minutes will be used.
     * @param dataStore data store to use when storing transformed workflows.
     * @param processingApiUrl URL of a processing API that can be contacted when loading a workflow for transformation.
     * @throws DateTimeParseException if {@code workflowCachePeriodAsStr} is not a valid Java time duration.
     */
    public TransformedWorkflowCache(final String workflowCachePeriodAsStr,
                                    final DataStore dataStore,
                                    final String processingApiUrl) throws DateTimeParseException {
        final Duration workflowCachePeriod = workflowCachePeriodAsStr==null || workflowCachePeriodAsStr.isEmpty()
                ? Duration.parse("PT5M")
                : Duration.parse(workflowCachePeriodAsStr);
        this.dataStore = dataStore;
        this.processingApiUrl = processingApiUrl;
        workflowCache = CacheBuilder.newBuilder()
                .expireAfterWrite(workflowCachePeriod.get(ChronoUnit.SECONDS), TimeUnit.SECONDS)
                .build(new CacheLoader<TransformedWorkflowCacheKey, TransformWorkflowResult>() {
                    @Override
                    public TransformWorkflowResult load(final TransformedWorkflowCacheKey key)
                            throws ApiException, DataStoreException, WorkflowRetrievalException,
                            WorkflowTransformerException {
                        return transformWorkflow(key);
                    }
                });
    }

    /**
     * Retrieves the result of a workflow transformation using combination of the provided workflow ID, project ID and
     * partial storage reference as a key. If no entry is found for the key then an attempt will be made to transform the
     * workflow with that result being returned.
     * @param workflowId ID of workflow indicating the transformed workflow result to retrieve.
     * @param projectId project ID indicating the transformed workflow result to retrieve.
     * @param outputPartialReference partial reference used in storing transformed workflow.
     * @param tenantId a tenant ID to use in evaluating the workflow.
     * @return returns the transformed workflow result associated with the workflow ID, project ID & partial reference.
     * @throws ApiException if certain failures occur communicating with the processing service to retrieve the workflow
     * during load of workflow for transformation.
     * e.g. Invalid requests will result in this exception.
     * @throws WorkflowRetrievalException if certain failures occur communicating with the processing service to
     * retrieve the workflow during load of workflow for transformation. e.g. The processing service not being contactable.
     * @throws DataStoreException if a failure occurs storing a transformed workflow during load.
     * @throws UncheckedExecutionException if an unchecked exception occurs during load of transformed workflow result
     * that is not expected by this method.
     * @throws WorkflowTransformerException if a failure occurs during transformation of workflow during load.
     */
    public TransformWorkflowResult getTransformWorkflowResult(final long workflowId, final String projectId,
                                                              final String outputPartialReference,
                                                              final String tenantId)
            throws ApiException, DataStoreException, WorkflowRetrievalException, WorkflowTransformerException {
        final TransformedWorkflowCacheKey cacheKey =
                buildCacheKey(workflowId, projectId, outputPartialReference, tenantId);
        try {
            return workflowCache.get(cacheKey);
        }
        catch(final ExecutionException e) {
            // throw specific exception types that caused ExecutionException if they are known to be thrown during cache
            // load
            final Throwable cause = e.getCause();
            if(cause instanceof ApiException ) {
                throw (ApiException) cause;
            }
            if(cause instanceof DataStoreException) {
                throw (DataStoreException) cause;
            }
            if(cause instanceof WorkflowRetrievalException) {
                throw (WorkflowRetrievalException) cause;
            }
            if(cause instanceof WorkflowTransformerException) {
                throw (WorkflowTransformerException) cause;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a cache key from the provided workflow ID, project ID and partial storage reference.
     * @param workflowId workflow ID to use in cache key construction.
     * @param projectId project ID to use in cache key construction.
     * @param outputPartialReference partial reference to use in cache key construction.
     * @param tenantId a tenant ID to use in evaluating the workflow.
     * @return constructed cache key.
     */
    private static TransformedWorkflowCacheKey buildCacheKey(final long workflowId, final String projectId,
                                                             final String outputPartialReference,
                                                             final String tenantId) {
        return new TransformedWorkflowCacheKey(outputPartialReference, projectId, tenantId, workflowId);
    }

    /**
     * Store the provided workflow in the data store.
     * @param workflowJavaScript workflow to store.
     * @param outputPartialReference partial reference to use when storing.
     * @return storage reference for stored workflow.
     * @throws DataStoreException if there is a failure storing workflow.
     */
    private String storeWorkflow(final String workflowJavaScript, final String outputPartialReference)
            throws DataStoreException
    {
        return dataStore.store(workflowJavaScript.getBytes(StandardCharsets.UTF_8), outputPartialReference);
    }

    /**
     * Retrieves a workflow referenced in @{code extractedProperties} and creates a script representing that workflow,
     * storing it in the data store.
     * @param cacheKey contains the properties required in order to transform and store the workflow.
     * @return the result of the workflow transformation.
     * @throws ApiException if certain failures occur communicating with the processing service to retrieve the workflow
     * e.g. Invalid requests will result in this exception.
     * @throws DataStoreException if a failure occurs storing transformed workflow.
     * @throws WorkflowTransformerException if a failure occurs during transformation of the workflow.
     * @throws WorkflowRetrievalException if certain failures occur communicating with the processing service to
     * retrieve the workflow. e.g. The processing service not being contactable.
     */
    private TransformWorkflowResult transformWorkflow(final TransformedWorkflowCacheKey cacheKey)
            throws ApiException, DataStoreException, WorkflowRetrievalException, WorkflowTransformerException {
        final String workflowJavaScript;
        try {
            workflowJavaScript = WorkflowTransformer.retrieveAndTransformWorkflowToJavaScript(
                    cacheKey.getWorkflowId(), cacheKey.getProjectId(),
                    processingApiUrl, cacheKey.getTenantId());
        } catch (final ApiException | WorkflowRetrievalException | WorkflowTransformerException e) {
            LOG.error("A failure occurred trying to transform Workflow to JavaScript representation.", e);
            throw e;
        }
        // Store the generated JavaScript in data store so it can be passed to other workers in a compact form
        final String workflowStorageRef;
        try {
            workflowStorageRef = storeWorkflow(workflowJavaScript, cacheKey.getOutputPartialReference());
        }
        catch(final DataStoreException e) {
            LOG.error("A failure occurred trying to store transformed workflow.", e);
            throw e;
        }
        return new TransformWorkflowResult(workflowJavaScript, workflowStorageRef);
    }
}
