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

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.extensibility.DocumentWorkerFactory;
import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.HealthMonitor;
import java.io.IOException;

/**
 * A factory to create workflow workers, passing them a configuration instance.
 */
public final class WorkflowWorkerFactory implements DocumentWorkerFactory
{
    @Override
    public DocumentWorker createDocumentWorker(final Application application)
    {
        try{
        return new WorkflowWorker(application);
        } catch(final IOException | ConfigurationException ex){
            return new DocumentWorker()
            {
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
