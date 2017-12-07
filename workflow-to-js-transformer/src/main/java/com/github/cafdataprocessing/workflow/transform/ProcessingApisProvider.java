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
package com.github.cafdataprocessing.workflow.transform;

import com.github.cafdataprocessing.processing.service.client.ApiClient;
import com.github.cafdataprocessing.processing.service.client.api.ActionConditionsApi;
import com.github.cafdataprocessing.processing.service.client.api.ActionTypesApi;
import com.github.cafdataprocessing.processing.service.client.api.ActionsApi;
import com.github.cafdataprocessing.processing.service.client.api.ProcessingRulesApi;
import com.github.cafdataprocessing.processing.service.client.api.ProcessingRulesConditionsApi;
import com.github.cafdataprocessing.processing.service.client.api.WorkflowsApi;

/**
 * Wrapper class allowing easy passing of multiple instantiated processing API classes
 */
public class ProcessingApisProvider {
    private final WorkflowsApi workflowsApi;
    private final ProcessingRulesApi processingRulesApi;
    private final ActionsApi actionsApi;
    private final ActionTypesApi actionTypesApi;
    private final ActionConditionsApi actionConditionsApi;
    private final ProcessingRulesConditionsApi rulesConditionsApi;

    public ProcessingApisProvider(final ApiClient apiClient){
        workflowsApi = new WorkflowsApi(apiClient);
        processingRulesApi = new ProcessingRulesApi(apiClient);
        actionsApi = new ActionsApi(apiClient);
        actionTypesApi = new ActionTypesApi(apiClient);
        actionConditionsApi = new ActionConditionsApi(apiClient);
        rulesConditionsApi = new ProcessingRulesConditionsApi(apiClient);
    }

    public ActionsApi getActionsApi() {
        return actionsApi;
    }

    public ActionConditionsApi getActionConditionsApi() {
        return actionConditionsApi;
    }

    public ActionTypesApi getActionTypesApi() {
        return actionTypesApi;
    }

    public ProcessingRulesApi getProcessingRulesApi() {
        return processingRulesApi;
    }

    public ProcessingRulesConditionsApi getRulesConditionsApi() {
        return rulesConditionsApi;
    }

    public WorkflowsApi getWorkflowsApi() {
        return workflowsApi;
    }
}
