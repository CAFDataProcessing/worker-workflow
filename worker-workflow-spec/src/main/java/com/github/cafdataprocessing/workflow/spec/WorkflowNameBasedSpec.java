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

public final class WorkflowNameBasedSpec extends WorkflowSpec
{
    private final String workflowName;

    /**
     * Create the workflow specification object using the partial storage reference, project ID and workflow ID provided.
     *
     * @param outputPartialReference partial storage reference for the transformed workflow this key is to be associated with.
     * @param projectId project ID of the transformed workflow this key is to be associated with.
     * @param tenantId a tenant ID to use in evaluating the workflow.
     * @param workflowName workflow name of the transformed workflow this key is to be associated with.
     */
    public WorkflowNameBasedSpec(final String outputPartialReference, final String projectId, final String tenantId,
                                 final String workflowName)
    {
        super(outputPartialReference, projectId, tenantId);
        this.workflowName = workflowName;
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

        if (!(o instanceof WorkflowNameBasedSpec)) {
            return false;
        }

        final WorkflowNameBasedSpec cacheKeyToCheck = (WorkflowNameBasedSpec) o;
        return Objects.equals(this.outputPartialReference, cacheKeyToCheck.getOutputPartialReference())
            && Objects.equals(this.projectId, cacheKeyToCheck.getProjectId())
            && Objects.equals(this.tenantId, cacheKeyToCheck.getTenantId())
            && this.workflowName.equals(cacheKeyToCheck.getWorkflowName());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(outputPartialReference, projectId, tenantId, workflowName);
    }
}
