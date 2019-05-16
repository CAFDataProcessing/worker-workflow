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

public class Workflow {
    private List<SettingDefinition> settingDefinitions;
    private Map<String, Action> actions;
    private String workflowScript;
    private String storageReference;

    public String getStorageReferenceForWorkflowScript() {
        return storageReference;
    }

    public void setStorageReference(String storageReference) {
        this.storageReference = storageReference;
    }

    public Map<String, Action> getActions() {
        return actions;
    }

    public void setActions(Map<String, Action> actions) {
        this.actions = actions;
    }

    public String getWorkflowScript() {
        return workflowScript;
    }

    public void setWorkflowScript(String workflowScript) {
        this.workflowScript = workflowScript;
    }

    public List<SettingDefinition> getSettingDefinitions() {
        return settingDefinitions;
    }

    public void setSettingDefinitions(List<SettingDefinition> settingDefinitions) {
        this.settingDefinitions = settingDefinitions;
    }
}