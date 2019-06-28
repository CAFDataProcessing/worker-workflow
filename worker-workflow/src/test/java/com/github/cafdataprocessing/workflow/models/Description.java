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

public class Description
{

    @JsonProperty("source")
    private String source;
    @JsonProperty("version")
    private Integer version;
    @JsonProperty("workflowName")
    private String workflowName;
    @JsonProperty("originalDescription")
    private String originalDescription;
    @JsonProperty("stack")
    private String stack;

    public String getSource()
    {
        return source;
    }

    public Integer getVersion()
    {
        return version;
    }

    public String getWorkflowName()
    {
        return workflowName;
    }

    public String getOriginalDescription()
    {
        return originalDescription;
    }

    public String getStack()
    {
        return stack;
    }

}
