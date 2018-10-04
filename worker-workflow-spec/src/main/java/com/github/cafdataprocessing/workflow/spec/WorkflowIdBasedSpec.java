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
package com.github.cafdataprocessing.workflow.spec;

import java.util.Objects;

public final class WorkflowIdBasedSpec extends WorkflowSpec
{
    private final long workflowId;

    /**
     * Create the workflow specification object using the partial storage reference, project ID and workflow ID provided.
     *
     * @param outputPartialReference partial storage reference for the transformed workflow this key is to be associated with.
     * @param projectId project ID of the transformed workflow this key is to be associated with.
     * @param tenantId a tenant ID to use in evaluating the workflow.
     * @param workflowId workflow ID of the transformed workflow this key is to be associated with.
     */
    public WorkflowIdBasedSpec(final String outputPartialReference, final String projectId, final String tenantId,
                               final long workflowId)
    {
        super(outputPartialReference, projectId, tenantId);
        this.workflowId = workflowId;
    }

    public long getWorkflowId()
    {
        return workflowId;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(outputPartialReference, projectId, tenantId, workflowId);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) {
            return true;
        }

        if (!(o instanceof WorkflowIdBasedSpec)) {
            return false;
        }

        final WorkflowIdBasedSpec cacheKeyToCheck = (WorkflowIdBasedSpec) o;
        return Objects.equals(this.outputPartialReference, cacheKeyToCheck.getOutputPartialReference())
            && Objects.equals(this.projectId, cacheKeyToCheck.getProjectId())
            && Objects.equals(this.tenantId, cacheKeyToCheck.getTenantId())
            && this.workflowId == cacheKeyToCheck.getWorkflowId();
    }
}
