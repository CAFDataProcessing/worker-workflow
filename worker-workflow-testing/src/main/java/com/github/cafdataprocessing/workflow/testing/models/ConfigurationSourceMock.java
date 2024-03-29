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
package com.github.cafdataprocessing.workflow.testing.models;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.worker.document.config.DocumentWorkerConfiguration;

public class ConfigurationSourceMock implements ConfigurationSource
{
    private final DocumentWorkerConfiguration documentWorkerConfiguration;

    public ConfigurationSourceMock(final DocumentWorkerConfiguration documentWorkerConfiguration)
    {
        this.documentWorkerConfiguration = documentWorkerConfiguration;
    }

    @Override
    public <T> T getConfiguration(Class<T> configClass) throws ConfigurationException
    {
        return (T) documentWorkerConfiguration;
    }

}
