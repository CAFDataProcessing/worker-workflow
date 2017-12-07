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

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.extensibility.DocumentWorkerFactory;
import com.hpe.caf.worker.document.model.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory to create workflow workers, passing them a configuration instance.
 */
public class WorkflowWorkerFactory implements DocumentWorkerFactory {
    private final static Logger LOG = LoggerFactory.getLogger(WorkflowWorkerFactory.class);
    private WorkflowWorkerConfiguration workflowWorkerConfiguration = null;

    @Override
    public DocumentWorker createDocumentWorker(final Application application) {
        if(workflowWorkerConfiguration == null) {
            try {
                workflowWorkerConfiguration = application
                        .getService(ConfigurationSource.class)
                        .getConfiguration(WorkflowWorkerConfiguration.class);
            } catch (ConfigurationException e) {
                LOG.warn("Unable to load WorkflowWorkerConfiguration. Specific Workflow Worker Worker configuration "
                        + "will not be passed to the worker.");
                workflowWorkerConfiguration = new WorkflowWorkerConfiguration();
            }
        }
        return new WorkflowWorker(workflowWorkerConfiguration, application.getService(DataStore.class));
    }
}
