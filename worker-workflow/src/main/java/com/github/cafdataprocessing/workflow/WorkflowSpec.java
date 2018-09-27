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

import java.util.Objects;

/**
 * Represents the properties that define a specific workflow
 */
final class WorkflowSpec
{
    private final String outputPartialReference;
    private final String projectId;
    private final String tenantId;
    private final long workflowId;
    private final String workflowName;

    public WorkflowSpec(
        final String outputPartialReference,
        final String projectId,
        final String tenantId,
        final long workflowId)
    {
        this(outputPartialReference, projectId, tenantId, workflowId, null);
    }

    public WorkflowSpec(
        final String outputPartialReference,
        final String projectId,
        final String tenantId,
        final String workflowName)
    {
        this(outputPartialReference, projectId, tenantId, -1, workflowName);
    }

    /**
     * Create the workflow specification object using the partial storage reference, project ID and workflow ID provided.
     *
     * @param outputPartialReference partial storage reference for the transformed workflow this key is to be associated with.
     * @param projectId project ID of the transformed workflow this key is to be associated with.
     * @param tenantId a tenant ID to use in evaluating the workflow.
     * @param workflowId workflow ID of the transformed workflow this key is to be associated with.
     */
    public WorkflowSpec(
        final String outputPartialReference,
        final String projectId,
        final String tenantId,
        final long workflowId,
        final String workflowName)
    {
        this.outputPartialReference = outputPartialReference;
        this.projectId = projectId;
        this.tenantId = tenantId;
        this.workflowId = workflowId;
        this.workflowName = workflowName;
    }

    public String getOutputPartialReference()
    {
        return outputPartialReference;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public String getTenantId()
    {
        return tenantId;
    }

    public long getWorkflowId()
    {
        return workflowId;
    }

    public String getWorkflowName()
    {
        return workflowName;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) {
            return true;
        }

        if (!(o instanceof WorkflowSpec)) {
            return false;
        }

        final WorkflowSpec cacheKeyToCheck = (WorkflowSpec) o;
        return Objects.equals(this.outputPartialReference, cacheKeyToCheck.getOutputPartialReference())
            && Objects.equals(this.projectId, cacheKeyToCheck.getProjectId())
            && Objects.equals(this.tenantId, cacheKeyToCheck.getTenantId())
            && this.workflowId == cacheKeyToCheck.getWorkflowId();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(outputPartialReference, projectId, tenantId, workflowId);
    }
}
