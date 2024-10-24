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

import com.github.cafapi.common.api.ConfigurationException;
import com.github.cafapi.common.api.ConfigurationSource;
import com.github.cafdataprocessing.worker.document.exceptions.DocumentWorkerTransientException;
import com.github.cafdataprocessing.worker.document.extensibility.DocumentWorker;
import com.github.cafdataprocessing.worker.document.extensibility.DocumentWorkerFactory;
import com.github.cafdataprocessing.worker.document.model.Application;
import com.github.cafdataprocessing.worker.document.model.Document;
import com.github.cafdataprocessing.worker.document.model.HealthMonitor;

/**
 * A factory to create workflow workers, passing them a configuration instance.
 */
public final class WorkflowWorkerFactory implements DocumentWorkerFactory
{
    @Override
    public DocumentWorker createDocumentWorker(final Application application)
    {
        try{
            final WorkflowWorkerConfiguration workflowWorkerConfiguration = application
                    .getService(ConfigurationSource.class)
                    .getConfiguration(WorkflowWorkerConfiguration.class);

            return new WorkflowWorker(workflowWorkerConfiguration,
                    new WorkflowManager(application,
                                        workflowWorkerConfiguration.getWorkflowsDirectory(),
                                        workflowWorkerConfiguration.getContextScriptFilePath()),
                    new ScriptManager(),
                    new ArgumentsManager(workflowWorkerConfiguration.getSettingsServiceUrl()),
                    new FailureFieldsManager());

        } catch(final ConfigurationException ex){
            return new DocumentWorker()
            {
                @Override
                public void checkLiveness(HealthMonitor healthMonitor)
                {
                    healthMonitor.reportUnhealthy("Unable to load workflows");
                }

                @Override
                public void checkHealth(HealthMonitor healthMonitor)
                {
                    healthMonitor.reportUnhealthy("Unable to load workflows");
                }

                @Override
                public void processDocument(Document document) throws InterruptedException, DocumentWorkerTransientException
                {
                    throw new RuntimeException("Worker unhealthy and unable to process message.");
                }
            };
        }
    }
}
