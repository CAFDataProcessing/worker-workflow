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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Caches transformed workflow representations for reuse.
 */
final class TransformedWorkflowCache
{
    private final ConcurrentHashMap<TransformedWorkflowCacheKey, ReentrantLock> TRANSFORM_WORKFLOW_LOCKS = new ConcurrentHashMap<>();
    private final Cache<TransformedWorkflowCacheKey, TransformWorkflowResult> workflowCache;

    /**
     * Initialize a TransformedWorkflowCache instance with each entry set to expire after the provided duration.
     * @param workflowCachePeriodAsStr an ISO-8601 time duration string. If passed as {@code null} or empty a default value of
     *                                 5 minutes will be used.
     * @throws DateTimeParseException if {@code workflowCachePeriodAsStr} is not a valid Java time duration.
     */
    public TransformedWorkflowCache(final String workflowCachePeriodAsStr) throws DateTimeParseException {
        final Duration workflowCachePeriod = workflowCachePeriodAsStr==null || workflowCachePeriodAsStr.isEmpty()
                ? Duration.parse("PT5M")
                : Duration.parse(workflowCachePeriodAsStr);
        workflowCache = CacheBuilder.newBuilder()
                .expireAfterWrite(workflowCachePeriod.get(ChronoUnit.SECONDS), TimeUnit.SECONDS)
                .build();
    }

    /**
     * Store a transformed workflow in the cache, using the provided workflow ID and project ID as the key it will be
     * retrievable by.
     * @param workflowId ID of the workflow the transformed object represents.
     * @param projectId project ID of the workflow the transformed object represents.
     * @param transformWorkflowResult transform result to store in cache for later retrieval.
     */
    public void addTransformWorkflowResult(final long workflowId, final String projectId,
                                           final TransformWorkflowResult transformWorkflowResult) {
        workflowCache.put(buildCacheKey(workflowId, projectId), transformWorkflowResult);
    }

    /**
     * Retrieve a lock object associated with a cache entry using the combination of the provided workflow ID and project ID
     * as the key for the lock.
     * @param workflowId ID of workflow associated with the lock.
     * @param projectId project ID associated with the lock.
     * @return the lock for the project ID and workflow ID combination. If no entry exists for the constructed
     * key then it will be created.
     */
    public ReentrantLock getTransformWorkflowLock(final long workflowId, final String projectId) {
            return TRANSFORM_WORKFLOW_LOCKS.computeIfAbsent(buildCacheKey(workflowId, projectId), key -> new ReentrantLock());
    }

    /**
     * Retrieves the result of a workflow transformation using combination of the provided workflow ID and project ID
     * as a key.
     * @param workflowId ID of workflow indicating the transformed workflow result to retrieve.
     * @param projectId project ID indicating the transformed workflow result to retrieve.
     * @return returns the transformed workflow result associated with the workflow ID and project ID or {@code null} if
     * there is no match found.
     */
    public TransformWorkflowResult getTransformWorkflowResult(final long workflowId, final String projectId) {
        return workflowCache.getIfPresent(buildCacheKey(workflowId, projectId));
    }

    /**
     * Builds a cache key from the provided workflow ID and project ID.
     * @param workflowId workflow ID to use in cache key construction.
     * @param projectId project ID to use in cache key construction.
     * @return constructed cache key.
     */
    private static TransformedWorkflowCacheKey buildCacheKey(final long workflowId, final String projectId) {
        return new TransformedWorkflowCacheKey(projectId, workflowId);
    }
}
