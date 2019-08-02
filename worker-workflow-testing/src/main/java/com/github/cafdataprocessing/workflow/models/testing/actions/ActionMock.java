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
package com.github.cafdataprocessing.workflow.models.testing.actions;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionMock
{

    @JsonProperty("name")
    private String name;
    @JsonProperty("queueName")
    private String queueName;
    @JsonProperty("customData")
    private CustomDataMock customData;
    @JsonProperty("scripts")
    private List<ScriptMock> scripts = null;
    @JsonProperty("terminateOnFailure")
    private boolean terminateOnFailure = true;

    public ActionMock()
    {
    }

    public ActionMock(final String name, final String queueName, final CustomDataMock customData, final List<ScriptMock> scripts,
                      final boolean terminateOnFailure)
    {
        super();
        this.name = name;
        this.queueName = queueName;
        this.customData = customData;
        this.scripts = scripts;
        this.terminateOnFailure = terminateOnFailure;
    }

    @JsonProperty("name")
    public String getName()
    {
        return name;
    }

    @JsonProperty("name")
    public void setName(final String name)
    {
        this.name = name;
    }

    @JsonProperty("queueName")
    public String getQueueName()
    {
        return queueName;
    }

    @JsonProperty("queueName")
    public void setQueueName(final String queueName)
    {
        this.queueName = queueName;
    }

    @JsonProperty("customData")
    public CustomDataMock getCustomData()
    {
        return customData;
    }

    @JsonProperty("customData")
    public void setCustomData(final CustomDataMock customData)
    {
        this.customData = customData;
    }

    @JsonProperty("scripts")
    public List<ScriptMock> getScripts()
    {
        return scripts;
    }

    @JsonProperty("scripts")
    public void setScripts(final List<ScriptMock> scripts)
    {
        this.scripts = scripts;
    }

    @JsonProperty("terminateOnFailure")
    public boolean isTerminateOnFailure()
    {
        return terminateOnFailure;
    }

    @JsonProperty("terminateOnFailure")
    public void setTerminateOnFailure(boolean terminateOnFailure)
    {
        this.terminateOnFailure = terminateOnFailure;
    }

}
