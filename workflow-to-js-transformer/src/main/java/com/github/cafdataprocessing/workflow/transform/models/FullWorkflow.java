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
package com.github.cafdataprocessing.workflow.transform.models;

import com.github.cafdataprocessing.processing.service.client.model.ExistingWorkflow;

import java.util.List;
import java.util.Objects;

/**
 * Represents a Workflow and its children such as Processing Rules, conditions and Actions.
 */
public class FullWorkflow {
    private final ExistingWorkflow details;
    private final List<FullProcessingRule> processingRules;

    /**
     * Creates an instance of a FullWorkflow using the provided workflow details.
     * @param workflow details of workflow this object represents. Cannot be null.
     * @param processingRules full details of processing rules on this workflow. Cannot be null.
     * @throws NullPointerException when {@code workflow} or {@code processingRules} is null.
     */
    public FullWorkflow(ExistingWorkflow workflow, List<FullProcessingRule> processingRules) throws NullPointerException {
        Objects.requireNonNull(workflow);
        Objects.requireNonNull(processingRules);
        this.details = workflow;
        this.processingRules = processingRules;
    }

    public ExistingWorkflow getDetails(){
        return details;
    }

    public List<FullProcessingRule> getProcessingRules(){
        return processingRules;
    }

    /**
     * Returns the ID of the workflow.
     * @return ID of the workflow.
     */
    public Long getWorkflowId(){
        return details.getId();
    }
}
