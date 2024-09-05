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

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.extensibility.DocumentWorkerFactory;
import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.HealthMonitor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A factory to create workflow workers, passing them a configuration instance.
 */
public final class WorkflowWorkerFactory implements DocumentWorkerFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowWorkerFactory.class);
    private static AtomicBoolean oomGeneratorInited = new AtomicBoolean(false);

    private static final List<byte[]> list = new ArrayList<>();

    public static class OOMGenerator implements Runnable {
        @Override
        public void run() {

            while(true) {
                // Allocate 1MB of memory and add it to the list
                byte[] bytes = new byte[1024 * 1024];
                list.add(bytes);

                System.out.println("Allocated " + list.size() + "MB");
            }
        }
    }

    private static void initOOMGenerator()
    {
        if (!oomGeneratorInited.get()) {
            LOG.info("CAOIMHE --- Will execute OOM in 3 minutes");
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule(new OOMGenerator(), 3, TimeUnit.MINUTES);
            oomGeneratorInited.set(true);
        }
    }
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
