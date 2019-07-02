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
package com.github.cafdataprocessing.workflow.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NewFailure
{

    @JsonProperty("id")
    private String failureId;
    @JsonProperty("originalDescription")
    private String description;
    @JsonProperty("stack")
    private String stack;
    @JsonProperty("version")
    private String version;
    @JsonProperty("workflowName")
    private String workflowName;
    @JsonProperty("source")
    private String source;

    public String getFailureId()
    {
        return failureId;
    }


    public String getStack()
    {
        return stack;
    }

    public String getDescription()
    {
        return description;
    }

    public String getVersion()
    {
        return version;
    }

    public String getWorkflowName()
    {
        return workflowName;
    }

    public String getSource()
    {
        return source;
    }

    
}
