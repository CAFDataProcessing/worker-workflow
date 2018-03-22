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

import com.github.cafdataprocessing.workflow.constants.WorkflowWorkerConstants;
import com.hpe.caf.worker.document.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a document this class returns specified properties from the document's custom data.
 */
final class CustomDataExtractor
{
    private final static Logger LOG = LoggerFactory.getLogger(CustomDataExtractor.class);

    private CustomDataExtractor(){}

    /**
     * Retrieves Workflow Worker specific properties from custom data of document. If any required properties are not
     * present then a failure will be recorded on the document and the returned result will indicate the properties are
     * not valid.
     * @param document document to retrieve properties from and potentially update with failures if any are missing.
     * @return the result containing properties extracted from the custom data and an indicator of whether all properties
     * are valid.
     */
    public static ExtractedProperties extractPropertiesFromCustomData(final Document document) {
        final String outputPartialReference;
        final String projectId;
        final String tenantId;
        long workflowId = -1;
        boolean customDataValid = true;

        outputPartialReference = CustomDataExtractor.getOutputPartialReference(document);
        if(outputPartialReference==null || outputPartialReference.isEmpty()) {
            LOG.debug("No output partial reference value passed to worker in custom data.");
        }

        projectId = CustomDataExtractor.getProjectId(document);
        if(projectId==null || projectId.isEmpty()) {
            LOG.error("No project ID value passed to worker in custom data.");
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.INVALID_CUSTOM_DATA,
                    "No project ID value passed to worker in custom data.");
            customDataValid = false;
        }

        tenantId = CustomDataExtractor.getTenantId(document);
        if(tenantId==null || tenantId.isEmpty()) {
            LOG.error("No tenant ID value passed to worker in custom data.");
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.INVALID_CUSTOM_DATA,
                    "No tenant ID value passed to worker in custom data.");
            customDataValid = false;
        }

        try {
            final Long extractedWorkflowId = CustomDataExtractor.getWorkflowId(document);
            if(extractedWorkflowId==null) {
                LOG.error("No workflow ID value passed to worker in custom data.");
                document.addFailure(WorkflowWorkerConstants.ErrorCodes.INVALID_CUSTOM_DATA,
                        "No workflow ID value passed to worker in custom data.");
                customDataValid = false;
            }
            else {
                workflowId = extractedWorkflowId;
            }
        }
        catch(final NumberFormatException e) {
            LOG.error("Failed to read passed workflow ID as a number.", e);
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.INVALID_CUSTOM_DATA, e.getMessage());
            customDataValid = false;
        }
        return new ExtractedProperties(customDataValid, outputPartialReference, projectId, tenantId, workflowId);
    }

    /**
     * Gets the output partial reference property from provided document.
     * @param document document to examine for property.
     * @return the output partial reference property or null if it is not present.
     */
    private static String getOutputPartialReference(final Document document) {
        return document.getCustomData(WorkflowWorkerConstants.CustomData.OUTPUT_PARTIAL_REFERENCE);
    }

    /**
     * Gets the project ID property from provided document.
     * @param document document to examine for property.
     * @return the project ID property or null if it is not present.
     */
    private static String getProjectId(final Document document) {
        return document.getCustomData(WorkflowWorkerConstants.CustomData.PROJECT_ID);
    }

    /**
     * Gets the tenant ID property from provided document.
     * @param document document to examine for property.
     * @return the tenant ID property or null if it is not present.
     */
    private static String getTenantId(final Document document) {
        return document.getCustomData(WorkflowWorkerConstants.CustomData.TENANT_ID);
    }

    /**
     * Gets the workflow ID property from provided document.
     * @param document document to examine for property.
     * @return the workflow ID property or null if it is not present.
     * @throws NumberFormatException if the workflow ID property on the document is not a valid long.
     */
    private static Long getWorkflowId(final Document document) throws NumberFormatException {
        final String workflowIdStr = document.getCustomData(WorkflowWorkerConstants.CustomData.WORKFLOW_ID);
        if(workflowIdStr == null  || workflowIdStr.isEmpty()) {
            return null;
        }
        return Long.parseLong(workflowIdStr);
    }
}
