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
package com.github.cafdataprocessing.workflow.model;

import java.util.List;
import java.util.Map;

public final class WorkflowSettings
{
    private List<String> taskSettings;
    private Map<String, RepoConfigSource> repositorySettings;
    private List<String> tenantSettings;

    /**
     * @return the taskSettings
     */
    public List<String> getTaskSettings()
    {
        return taskSettings;
    }

    /**
     * @param taskSettings the taskSettings to set
     */
    public void setTaskSettings(final List<String> taskSettings)
    {
        this.taskSettings = taskSettings;
    }

    /**
     * @return the repositorySettings
     */
    public Map<String, RepoConfigSource> getRepositorySettings()
    {
        return repositorySettings;
    }

    /**
     * @param repositorySettings the repositorySettings to set
     */
    public void setRepositorySettings(final Map<String, RepoConfigSource> repositorySettings)
    {
        this.repositorySettings = repositorySettings;
    }

    /**
     * @return the tenantSettings
     */
    public List<String> getTenantSettings()
    {
        return tenantSettings;
    }

    /**
     * @param tenantSettings the tenantSettings to set
     */
    public void setTenantSettings(final List<String> tenantSettings)
    {
        this.tenantSettings = tenantSettings;
    }
}
