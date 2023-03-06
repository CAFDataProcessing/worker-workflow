/*
 * Copyright 2017-2023 Open Text.
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
package com.github.cafdataprocessing.workflow.testing.models;

import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.BatchSizeController;
import com.hpe.caf.worker.document.model.InputMessageProcessor;
import com.hpe.caf.worker.document.model.ServiceLocator;

public class ApplicationMock implements Application
{
    private final InputMessageProcessor inputMessageProcessor;
    private final ConfigurationSource configurationSource;

    public ApplicationMock(final InputMessageProcessor inputMessageProcessor, final ConfigurationSource configurationSource)
    {
        this.inputMessageProcessor = inputMessageProcessor;
        this.configurationSource = configurationSource;
    }

    @Override
    public BatchSizeController getBatchSizeController()
    {
        return null;
    }

    @Override
    public InputMessageProcessor getInputMessageProcessor()
    {
        return inputMessageProcessor;
    }

    @Override
    public <S> S getService(Class<S> service)
    {
        return (S) configurationSource;
    }

    @Override
    public ServiceLocator getServiceLocator()
    {
        return null;
    }

    @Override
    public Application getApplication()
    {
        return null;
    }

    public ConfigurationSource getConfigurationSource()
    {
        return configurationSource;
    }

    @Override
    public String getName()
    {
        return "application-worker-base";
    }

    @Override
    public String getVersion()
    {
        return "1.0.0-SNAPSHOT-APPLICATION";
    }

}
