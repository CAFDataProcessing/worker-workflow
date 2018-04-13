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
package com.github.cafdataprocessing.workflow;

/**
 * Represents workflow worker properties extracted from custom data of a document.
 */
final class ExtractedProperties
{
    private final String outputPartialReference;
    private final String projectId;
    private final String tenantId;
    private final long workflowId;

    /**
     * Initialize ExtractedProperties instance describing properties pulled from a document.
     *
     * @param valid indicates if all the properties this instance contains are valid for use.
     * @param outputPartialReference the output partial reference to use for data storage.
     * @param projectId the project ID workflow is associated with.
     * @param tenantId a tenant ID to use in evaluating the workflow.
     * @param workflowId the workflow ID to retrieve for the document.
     */
    public ExtractedProperties(final String outputPartialReference, final String projectId,
                               final String tenantId, final long workflowId)
    {
        this.outputPartialReference = outputPartialReference;
        this.projectId = projectId;
        this.tenantId = tenantId;
        this.workflowId = workflowId;
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
}
