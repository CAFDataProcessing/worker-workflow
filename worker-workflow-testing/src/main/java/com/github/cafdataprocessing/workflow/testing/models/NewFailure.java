/*
 * Copyright 2017-2022 Micro Focus or one of its affiliates.
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

import com.fasterxml.jackson.annotation.JsonProperty;

public class NewFailure
{

    @JsonProperty("ID")
    private String failureId;
    @JsonProperty("MESSAGE")
    private String message;
    @JsonProperty("STACK")
    private String stack;
    @JsonProperty("COMPONENT")
    private String component;
    @JsonProperty("WORKFLOW_NAME")
    private String workflowName;
    @JsonProperty("WORKFLOW_ACTION")
    private String workflowAction;
    @JsonProperty("DATE")
    private String date;
    @JsonProperty("CORRELATION_ID")
    private String correlationId;
    public String getFailureId()
    {
        return failureId;
    }

    public String getDate()
    {
        return date;
    }

    public String getStack()
    {
        return stack;
    }

    public String getMessage()
    {
        return message;
    }

    public String getComponent()
    {
        return component;
    }

    public String getWorkflowName()
    {
        return workflowName;
    }

    public String getWorkflowAction()
    {
        return workflowAction;
    }

    public String getCorrelationId()
    {
        return correlationId;
    }

    
}
