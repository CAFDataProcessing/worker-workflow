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

/**
 * Represents the properties that define a specific workflow
 */
public class WorkflowSpec
{
    final String outputPartialReference;
    final String projectId;
    final String tenantId;

    /**
     * Create the workflow specification object using the partial storage reference, project ID and workflow ID provided.
     *
     * @param outputPartialReference partial storage reference for the transformed workflow this key is to be associated with.
     * @param projectId project ID of the transformed workflow this key is to be associated with.
     * @param tenantId a tenant ID to use in evaluating the workflow.
     */
    public WorkflowSpec(final String outputPartialReference, final String projectId, final String tenantId)
    {
        this.outputPartialReference = outputPartialReference;
        this.projectId = projectId;
        this.tenantId = tenantId;
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

    @Override
    public int hashCode()
    {
        return Objects.hash(outputPartialReference, projectId, tenantId);
    }
}
