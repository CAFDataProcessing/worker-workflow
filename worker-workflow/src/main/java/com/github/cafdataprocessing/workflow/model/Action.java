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
package com.github.cafdataprocessing.workflow.model;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class Action {
    @NotNull
    private String name;
    private String conditionFunction;
    private Map<String, String> customData;
    private Script[] scripts;
    private boolean terminateOnFailure;
    private boolean applyMessagePrioritization;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConditionFunction() {
        return conditionFunction;
    }

    public void setConditionFunction(final String conditionFunction) {
        this.conditionFunction = conditionFunction;
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    public void setCustomData(final Map<String, String> customData) {
        this.customData = customData;
    }

    public Script[] getScripts() {
        return scripts;
    }

    public void setScripts(final Script[] scripts) {
        this.scripts = scripts;
    }

    public boolean isTerminateOnFailure()
    {
        return terminateOnFailure;
    }

    public void setTerminateOnFailure(boolean terminateOnFailure)
    {
        this.terminateOnFailure = terminateOnFailure;
    }

    public boolean isApplyMessagePrioritization() {
        return applyMessagePrioritization;
    }

    public void setApplyMessagePrioritization(boolean applyMessagePrioritization) {
        this.applyMessagePrioritization = applyMessagePrioritization;
    }
}
