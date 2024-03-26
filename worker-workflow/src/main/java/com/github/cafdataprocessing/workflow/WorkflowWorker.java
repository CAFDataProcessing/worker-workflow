/*
 * Copyright 2017-2024 Open Text.
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

import com.github.cafdataprocessing.workflow.model.Workflow;
import com.google.common.base.Strings;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Field;
import com.hpe.caf.worker.document.model.HealthMonitor;
import com.hpe.caf.worker.document.model.ResponseCustomData;
import com.hpe.caf.worker.document.model.Task;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import org.slf4j.MDC;

/**
 * Worker that will examine task received for a workflow name, it will then look for a yaml file with the same
 * name on disk and add it to the task along with any settings required for the workflow.
 */
public final class WorkflowWorker implements DocumentWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowWorker.class);
    private static final String TENANT_ID_KEY = "tenantId";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String SETTINGS_SERVICE_LAST_UPDATE_TIME_MILLIS_KEY = "settingsServiceLastUpdateTimeMillis";
    private final WorkflowManager workflowManager;
    private final ScriptManager scriptManager;
    private final ArgumentsManager argumentsManager;
    private final FailureFieldsManager failureFieldsManager;

    /**
     * Instantiates a WorkflowWorker instance to process documents, evaluating them against the workflow referred to by
     * the document.
     * @param workflowWorkerConfiguration The worker's configuration
     * @param workflowManager Retrieves workflows from disk and stores them in the datastore
     * @param scriptManager Applies the scripts to the documents task object
     * @param argumentsManager Processes settings definitions and retrieves values from custom data, document fields or
     *                        the settings service
     * @param failureFieldsManager Processes the extra failure subfields that should be used during the workflow
     * @throws ConfigurationException when workflow directory is not set
     */
    public WorkflowWorker(final WorkflowWorkerConfiguration workflowWorkerConfiguration,
                          final WorkflowManager workflowManager,
                          final ScriptManager scriptManager,
                          final ArgumentsManager argumentsManager,
                          final FailureFieldsManager failureFieldsManager
                          )
            throws ConfigurationException
    {
        final String workflowsDirectory = workflowWorkerConfiguration.getWorkflowsDirectory();
        if(workflowsDirectory == null){
            throw new ConfigurationException("No workflow storage directory was set. Unable to load available workflows.");
        }

        this.workflowManager = workflowManager;
        this.scriptManager = scriptManager;
        this.argumentsManager = argumentsManager;
        this.failureFieldsManager = failureFieldsManager;
    }

    /**
     * This method provides an opportunity for the worker to report if it has any problems which would prevent it
     * processing documents correctly. If the worker is healthy then it should simply return without calling the
     * health monitor.
     *
     * @param healthMonitor used to report the health of the application
     */
    @Override
    public void checkHealth(final HealthMonitor healthMonitor)
    {
        try {
            argumentsManager.checkHealth();
        } catch (final Exception e) {
            LOG.error("Problem encountered when contacting Settings Service to check health: ", e);
            healthMonitor.reportUnhealthy("Settings Service communication is unhealthy: " + e.getMessage());
        }
    }

    /**
     * Processes a single document. Retrieving the workflow it refers to, evaluating the document against the workflow
     * to determine where it should be sent to and storing the workflow on the document so the next worker may
     * re-evaluate the document once it has finished its action.
     *
     * @param document the document to be processed.
     * @throws DocumentWorkerTransientException if the document could not be processed
     */
    @Override
    public void processDocument(final Document document) throws DocumentWorkerTransientException
    {
        addMdcLoggingData(document.getTask());
        
        // Get the workflow specification passed in
        final String customDataWorkflowName = document.getCustomData("workflowName");
        final Field fieldWorkflowName = document.getField("CAF_WORKFLOW_NAME");

        if(!Strings.isNullOrEmpty(customDataWorkflowName)){
            fieldWorkflowName.set(customDataWorkflowName);
        }

        if(!fieldWorkflowName.hasValues()){
            LOG.error(String.format("Workflow could not be retrieved from custom data for document [%s].",
                    document.getReference()));
            document.addFailure("WORKFLOW_NOT_SPECIFIED", "Workflow could not be retrieved from custom data.");
            return;
        }

        if(fieldWorkflowName.getValues().size()>1){
            LOG.error(String.format("Multiple workflows [%s] supplied in CAF_WORKFLOW_NAME field for document [%s].",
                    String.join(",", fieldWorkflowName.getStringValues()),
                    document.getReference()));
            document.addFailure("WORKFLOW_MULTIPLE_WORKFLOWS",
                    "More than one workflow name was found in CAF_WORKFLOW_NAME field.");
        }

        final String workflowName = fieldWorkflowName.getStringValues().get(0);

        final Workflow workflow = workflowManager.get(workflowName);
        if (workflow == null) {
            final String errorMessage = String.format("Workflow [%s] is not available for document [%s].",
                    workflowName, document.getReference());
            LOG.error(errorMessage);
            document.addFailure("WORKFLOW_NOT_FOUND", errorMessage);
            return;
        }

        final Optional<Long> settingsServiceLastUpdateTimeMillisOpt;
        try {
            settingsServiceLastUpdateTimeMillisOpt = getSettingsServiceLastUpdateTimeMillis(document);
        } catch (final NumberFormatException e) {
            final String errorMessage = String.format(
                "Custom data property [%s] for document [%s] could not be converted to an instance of Long [%s]",
                SETTINGS_SERVICE_LAST_UPDATE_TIME_MILLIS_KEY, document.getReference(), e.getMessage());
            LOG.error(errorMessage);
            document.addFailure("WORKFLOW_CUSTOM_DATA_INVALID", errorMessage);
            return;
        }

        failureFieldsManager.handleExtraFailureSubFields(document);
        argumentsManager.addArgumentsToDocument(workflow.getArguments(), document, settingsServiceLastUpdateTimeMillisOpt);

        try {
            scriptManager.applyScriptToDocument(workflow, document);
        } catch (final ScriptException e) {
            LOG.error(String.format("ScriptException for document [%s].\n%s\n", document.getReference(), e.toString()));
            document.addFailure("WORKFLOW_SCRIPT_EXCEPTION", e.getMessage());
        }
    }
    
    private void addMdcLoggingData(final Task task)
    {
        // The logging pattern we use uses a tenantId and a correlationId:
        // 
        // https://github.com/CAFapi/caf-logging/tree/v1.0.0#pattern
        // https://github.com/CAFapi/caf-logging/blob/v1.0.0/src/main/resources/logback.xml#L27
        //
        // This function adds a tenantId and correlationID to the MDC (http://logback.qos.ch/manual/mdc.html), so that log messages from 
        // *this* worker (workflow-worker) will contain these values.
        //
        // See also addMdcData in workflow-control.js, which performs similar logic to ensure log messages from *subsequent* workers in 
        // the workflow also contain these values. 

        // Get MDC data from custom data, creating a correlationId if it doesn't yet exist.
        final String tenantId = task.getCustomData(TENANT_ID_KEY);
        final String correlationId = WorkflowWorker.getOrCreateCorrelationId(task);

        // Add tenantId and correlationId to the MDC.
        if (tenantId != null) {
           MDC.put(TENANT_ID_KEY, tenantId); 
        }
        MDC.put(CORRELATION_ID_KEY, correlationId);

        // Add MDC data to custom data so that its passed it onto the next worker.
        final ResponseCustomData responseCustomData = task.getResponse().getCustomData();
        responseCustomData.put(TENANT_ID_KEY, tenantId);
        responseCustomData.put(CORRELATION_ID_KEY, correlationId);
    }
    
    private static String getOrCreateCorrelationId(final Task task)
    {
        final String correlationId = task.getCustomData(CORRELATION_ID_KEY);

        return (correlationId == null)
            ? UUID.randomUUID().toString()
            : correlationId;
    }

    private static Optional<Long> getSettingsServiceLastUpdateTimeMillis(final Document document) throws NumberFormatException
    {
        final String settingsServiceLastUpdateTimeString = document.getCustomData(SETTINGS_SERVICE_LAST_UPDATE_TIME_MILLIS_KEY);
        if (Strings.isNullOrEmpty(settingsServiceLastUpdateTimeString)) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(settingsServiceLastUpdateTimeString));
    }
}
