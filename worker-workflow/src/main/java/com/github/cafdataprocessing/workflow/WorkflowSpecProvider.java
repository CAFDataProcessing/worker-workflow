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

import com.github.cafdataprocessing.workflow.constants.WorkflowWorkerConstants;
import com.github.cafdataprocessing.workflow.spec.WorkflowIdBasedSpec;
import com.github.cafdataprocessing.workflow.spec.WorkflowNameBasedSpec;
import com.github.cafdataprocessing.workflow.spec.WorkflowSpec;
import com.github.cafdataprocessing.workflow.spec.InvalidWorkflowSpecException;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a document this class returns specified properties from the document's custom data.
 */
final class WorkflowSpecProvider
{
    private final static Logger LOG = LoggerFactory.getLogger(WorkflowSpecProvider.class);

    private WorkflowSpecProvider()
    {
    }

    /**
     * Retrieves Workflow Worker specific properties from the custom data or fields of the document. If any required properties are not
     * present then a failure will be recorded on the document and an exception will be thrown.
     * <p>
     * Important Side Effect: If the data is provided via custom data then it will be set into the fields so that it can be examined later
     * in the workflow.
     *
     * @param document document to retrieve properties from and potentially update with failures if any are missing.
     * @return the result containing properties extracted from the custom data or fields
     */
    public static WorkflowSpec fromDocument(final Document document)
        throws InvalidWorkflowSpecException
    {
        final String outputPartialReference;
        final String projectId;
        final String tenantId;
        boolean customDataValid = true;

        outputPartialReference = WorkflowSpecProvider.getSetOutputPartialReference(document);
        if (outputPartialReference == null || outputPartialReference.isEmpty()) {
            LOG.debug("No output partial reference value passed to worker.");
        }

        projectId = WorkflowSpecProvider.getSetProjectId(document);
        if (projectId == null || projectId.isEmpty()) {
            LOG.error("No project ID value passed to worker.");
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.INVALID_CUSTOM_DATA,
                                "No project ID value passed to worker.");
            customDataValid = false;
        }

        tenantId = WorkflowSpecProvider.getSetTenantId(document);
        if (tenantId == null || tenantId.isEmpty()) {
            LOG.error("No tenant ID value passed to worker.");
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.INVALID_CUSTOM_DATA,
                                "No tenant ID value passed to worker.");
            customDataValid = false;
        }

        Long extractedWorkflowId;
        final WorkflowSpec workflowSpec;
        try {
            extractedWorkflowId = getSetWorkflowId(document);
        } catch (final NumberFormatException e) {
            LOG.error("Failed to read passed workflow ID as a number.", e);
            document.addFailure(WorkflowWorkerConstants.ErrorCodes.INVALID_CUSTOM_DATA, e.getMessage());
            extractedWorkflowId = null;
            customDataValid = false;
        }

        if (extractedWorkflowId == null) {
            final String workflowName = getSetWorkflowName(document);
            if (workflowName == null) {
                LOG.error("No workflow ID or name value passed to worker.");
                document.addFailure(WorkflowWorkerConstants.ErrorCodes.INVALID_CUSTOM_DATA,
                                    "No workflow ID or name value passed to worker.");
                customDataValid = false;
            }
            workflowSpec = new WorkflowNameBasedSpec(outputPartialReference, projectId, tenantId, workflowName);
        } else {
            workflowSpec = new WorkflowIdBasedSpec(outputPartialReference, projectId, tenantId, extractedWorkflowId);
        }

        if (customDataValid) {
            return workflowSpec;
        } else {
            throw new InvalidWorkflowSpecException();
        }
    }

    /**
     * Gets the output partial reference property from provided document.
     *
     * @param document document to examine for property.
     * @return the output partial reference property or null if it is not present.
     */
    private static String getSetOutputPartialReference(final Document document)
    {
        return getSetCustomDataField(
            document,
            WorkflowWorkerConstants.CustomData.OUTPUT_PARTIAL_REFERENCE,
            "CAF_WORKFLOW_OUTPUT_PARTIAL_REFERENCE");
    }

    /**
     * Gets the project ID property from provided document.
     *
     * @param document document to examine for property.
     * @return the project ID property or null if it is not present.
     */
    private static String getSetProjectId(final Document document)
    {
        return getSetCustomDataField(
            document,
            WorkflowWorkerConstants.CustomData.PROJECT_ID,
            "CAF_WORKFLOW_PROJECT_ID");
    }

    /**
     * Gets the tenant ID property from provided document.
     *
     * @param document document to examine for property.
     * @return the tenant ID property or null if it is not present.
     */
    private static String getSetTenantId(final Document document)
    {
        return getSetCustomDataField(
            document,
            WorkflowWorkerConstants.CustomData.TENANT_ID,
            "CAF_WORKFLOW_TENANT_ID");
    }

    /**
     * Gets the workflow ID property from provided document.
     *
     * @param document document to examine for property.
     * @return the workflow ID property or null if it is not present.
     * @throws NumberFormatException if the workflow ID property on the document is not a valid long.
     */
    private static Long getSetWorkflowId(final Document document) throws NumberFormatException
    {
        final String workflowIdStr = getSetCustomDataField(
            document,
            WorkflowWorkerConstants.CustomData.WORKFLOW_ID,
            "CAF_WORKFLOW_ID");
        if (workflowIdStr == null || workflowIdStr.isEmpty()) {
            return null;
        }
        return Long.parseLong(workflowIdStr);
    }

    /**
     * Gets the workflow name property from provided document.
     *
     * @param document document to examine for property.
     * @return the workflow name property or null if it is not present.
     */
    private static String getSetWorkflowName(final Document document) throws NumberFormatException
    {
        final String workflowName = getSetCustomDataField(
            document,
            WorkflowWorkerConstants.CustomData.WORKFLOW_NAME,
            "CAF_WORKFLOW_NAME");
        if (workflowName == null || workflowName.isEmpty()) {
            return null;
        }
        return workflowName;
    }

    /**
     * Gets the value either from the specified custom data entry or from the specified document field.
     * <p>
     * Important Side Effect: If the value is retrieved from the custom data entry then it is also set in the document field.
     */
    private static String getSetCustomDataField(
        final Document document,
        final String customDataKey,
        final String documentFieldName
    )
    {
        final String customDataValue = document.getCustomData(customDataKey);

        final Field field = document.getField(documentFieldName);
        if (customDataValue == null) {
            return getFirstStringValue(field);
        } else {
            field.set(customDataValue);
            return customDataValue;
        }
    }

    /**
     * Returns one of the the non-reference field values that contains a valid UTF-8 encoded string, or {@code null} if the field does not
     * contain such a value.
     *
     * @param field the field to read the value from
     * @return one of the non-reference string field values
     */
    private static String getFirstStringValue(final Field field)
    {
        if (!field.hasValues()) {
            return null;
        }

        return field.getValues()
            .stream()
            .filter(fieldValue -> (!fieldValue.isReference()) && fieldValue.isStringValue())
            .map(fieldValue -> fieldValue.getStringValue())
            .findFirst()
            .orElse(null);
    }
}
