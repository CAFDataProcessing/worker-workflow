/*
 * Copyright 2017-2020 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.workflow.testing;

import java.util.HashMap;
import java.util.Map;

public class ActionExpectation {
    private String action;
    private final Map<String, String> customData = new HashMap<>();
    private String successQueue;
    private String failureQueue;
    private boolean terminateOnFailure;

    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    public String getSuccessQueue() {
        return successQueue;
    }

    public void setSuccessQueue(final String successQueue) {
        this.successQueue = successQueue;
    }

    public String getFailureQueue() {
        return failureQueue;
    }

    public void setFailureQueue(final String failureQueue) {
        this.failureQueue = failureQueue;
    }

    public boolean isTerminateOnFailure()
    {
        return terminateOnFailure;
    }

    public void setTerminateOnFailure(boolean terminateOnFailure)
    {
        this.terminateOnFailure = terminateOnFailure;
    }
}
