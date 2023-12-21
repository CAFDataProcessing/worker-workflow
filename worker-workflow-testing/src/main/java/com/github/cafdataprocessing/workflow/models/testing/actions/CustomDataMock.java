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
package com.github.cafdataprocessing.workflow.models.testing.actions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomDataMock
{

    @JsonProperty("tenantId")
    private String tenantId;

    public CustomDataMock()
    {
    }

    public CustomDataMock(final String tenantId)
    {
        super();
        this.tenantId = tenantId;
    }

    @JsonProperty("tenantId")
    public String getTenantId()
    {
        return tenantId;
    }

    @JsonProperty("tenantId")
    public void setTenantId(final String tenantId)
    {
        this.tenantId = tenantId;
    }

}
