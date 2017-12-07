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

import com.github.cafdataprocessing.processing.service.client.model.ExistingCondition;
import com.github.cafdataprocessing.processing.service.client.model.ExistingProcessingRule;

import java.util.List;
import java.util.Objects;

/**
 * Represents a Processing Rules and its rule conditions, action condition and actions.
 */
public class FullProcessingRule {
    private final ExistingProcessingRule details;
    private final List<FullAction> actions;
    private final List<ExistingCondition> conditions;

    /**
     * Creates an instance of a FullProcessingRule using the provided processing rule details.
     * @param processingRule details of processing rule this object should represent. Cannot be null.
     * @param actions full details of actions on this rule. Cannot be null.
     * @param ruleConditions rule conditions set on this rule. Cannot be null.
     * @throws NullPointerException when {@code processingRule}, {@code actions} or {@code ruleConditions} is null.
     */
    public FullProcessingRule(ExistingProcessingRule processingRule, List<FullAction> actions,
                              List<ExistingCondition> ruleConditions) throws NullPointerException{
        Objects.requireNonNull(processingRule);
        Objects.requireNonNull(actions);
        Objects.requireNonNull(ruleConditions);
        this.details = processingRule;
        this.actions = actions;
        this.conditions = ruleConditions;
    }

    public List<FullAction> getActions(){
        return actions;
    }

    public ExistingProcessingRule getProcessingRule(){
        return details;
    }

    public long getProcessingRuleId(){
        return details.getId();
    }

    public List<ExistingCondition> getRuleConditions(){
        return conditions;
    }
}
