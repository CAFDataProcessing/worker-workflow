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

import com.hpe.caf.api.Configuration;
import javax.validation.constraints.NotNull;

/**
 * Configuration specific to the workflow worker
 */
@Configuration
public final class WorkflowWorkerConfiguration
{
    /**
     * Directory to use when attempting to load workflow scripts.
     */
    @NotNull
    private String workflowsDirectory;

    /**
     * The URL for the settings service.
     */
    @NotNull
    private String settingsServiceUrl;

    private String contextScriptFilePath;

    public String getContextScriptFilePath()
    {
        return contextScriptFilePath;
    }

    public void setContextScriptFilePath(final String contextScriptFilePath)
    {
        this.contextScriptFilePath = contextScriptFilePath;
    }

    public String getWorkflowsDirectory()
    {
        return workflowsDirectory;
    }

    public void setWorkflowsDirectory(final String workflowsDirectory)
    {
        this.workflowsDirectory = workflowsDirectory;
    }

    public String getSettingsServiceUrl() {
        return settingsServiceUrl;
    }

    public void setSettingsServiceUrl(final String settingsServiceUrl) {
        this.settingsServiceUrl = settingsServiceUrl;
    }
}
