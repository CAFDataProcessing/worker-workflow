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
package com.github.cafdataprocessing.workflow.models.testing.actions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScriptMock
{

    @JsonProperty("name")
    private String name;
    @JsonProperty("script")
    private String script;

    public ScriptMock()
    {
    }

    public ScriptMock(final String name, final String script)
    {
        super();
        this.name = name;
        this.script = script;
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

    @JsonProperty("script")
    public String getScript()
    {
        return script;
    }

    @JsonProperty("script")
    public void setScript(final String script)
    {
        this.script = script;
    }

}
