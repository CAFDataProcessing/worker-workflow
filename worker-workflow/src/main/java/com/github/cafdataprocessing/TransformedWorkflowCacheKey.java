package com.github.cafdataprocessing;

import java.util.Objects;

/**
 * Represents a key to a transformed workflow cache entry
 */
final class TransformedWorkflowCacheKey {
    private final String projectId;
    private final long workflowId;

    /**
     * Create a transformed workflow cache key using the project ID and workflow ID provided.
     * @param projectId project ID of the transformed workflow this key is to be associated with.
     * @param workflowId workflow ID of the transformed workflow this key is to be associated with.
     */
    public TransformedWorkflowCacheKey(final String projectId, final long workflowId) {
        this.projectId = projectId;
        this.workflowId = workflowId;
    }

    public String getProjectId() {
        return projectId;
    }

    public long getWorkflowId() {
        return workflowId;
    }

    @Override
    public boolean equals(final Object  o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        TransformedWorkflowCacheKey cacheKeyToCheck = (TransformedWorkflowCacheKey) o;
        return Objects.equals(this.projectId, cacheKeyToCheck.getProjectId()) &&
                Objects.equals(this.workflowId, cacheKeyToCheck.getWorkflowId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, workflowId);
    }
}
