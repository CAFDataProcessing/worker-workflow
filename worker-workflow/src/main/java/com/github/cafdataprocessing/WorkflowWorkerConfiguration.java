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
package com.github.cafdataprocessing;

import com.hpe.caf.api.Configuration;
import javax.validation.constraints.NotNull;

/**
 * Configuration specific to the workflow worker
 */
@Configuration
public final class WorkflowWorkerConfiguration {
    /**
     * URL to a processing API that workflows should be retrieved via.
     */
    @NotNull
    private String processingApiUrl;
    /**
     * The time that cached workflows should be retained after being added to the cache. Should be in ISO-8601 time
     * duration format.
     */
    private String workflowCachePeriod;

    public String getProcessingApiUrl() {
        return processingApiUrl;
    }

    public String getWorkflowCachePeriod() {
        return workflowCachePeriod;
    }

    public void setProcessingApiUrl(final String processingApiUrl) {
        this.processingApiUrl = processingApiUrl;
    }

    public void setWorkflowCachePeriod(final String workflowCachePeriod) { this.workflowCachePeriod = workflowCachePeriod; }
}
