/*
 * Copyright 2017-2021 Micro Focus or one of its affiliates.
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

public class Workflow {
    private List<ArgumentDefinition> arguments;
    private List<Action> actions;
    private String workflowScript;
    private String storageReference;

    public String getStorageReferenceForWorkflowScript() {
        return storageReference;
    }

    public void setStorageReference(final String storageReference) {
        this.storageReference = storageReference;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(final List<Action> actions) {
        this.actions = actions;
    }

    public String getWorkflowScript() {
        return workflowScript;
    }

    public void setWorkflowScript(final String workflowScript) {
        this.workflowScript = workflowScript;
    }

    public List<ArgumentDefinition> getArguments() {
        return arguments;
    }

    public void setArguments(final List<ArgumentDefinition> arguments) {
        this.arguments = arguments;
    }
}
