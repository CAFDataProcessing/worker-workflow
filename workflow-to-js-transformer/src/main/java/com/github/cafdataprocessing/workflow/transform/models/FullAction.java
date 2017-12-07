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

import com.github.cafdataprocessing.processing.service.client.model.ExistingAction;
import com.github.cafdataprocessing.processing.service.client.model.ExistingCondition;

import java.util.List;
import java.util.Objects;

/**
 * Represents a processing service action and its action conditions
 */
public class FullAction {
    private final ExistingAction details;
    private final List<ExistingCondition> conditions;

    /**
     * Creates an instance of a FullAction using the provided action details.
     * @param action details of action this object should represent. Cannot be null.
     * @param actionConditions conditions that apply to the action. Cannot be null.
     * @throws NullPointerException when {@code action} or {@code actionConditions} is null.
     */
    public FullAction(ExistingAction action, List<ExistingCondition> actionConditions){
        Objects.requireNonNull(action);
        Objects.requireNonNull(actionConditions);
        this.details = action;
        this.conditions = actionConditions;
    }

    public ExistingAction getAction(){
        return details;
    }

    public List<ExistingCondition> getActionConditions(){
        return conditions;
    }

    public long getActionId(){
        return details.getId();
    }
}
