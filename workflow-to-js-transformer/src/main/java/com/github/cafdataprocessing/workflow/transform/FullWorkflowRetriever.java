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
import com.github.cafdataprocessing.processing.service.client.ApiException;
import com.github.cafdataprocessing.processing.service.client.api.ActionConditionsApi;
import com.github.cafdataprocessing.processing.service.client.api.ActionsApi;
import com.github.cafdataprocessing.processing.service.client.api.ProcessingRulesApi;
import com.github.cafdataprocessing.processing.service.client.api.ProcessingRulesConditionsApi;
import com.github.cafdataprocessing.processing.service.client.api.WorkflowsApi;
import com.github.cafdataprocessing.processing.service.client.model.ExistingAction;
import com.github.cafdataprocessing.processing.service.client.model.ExistingActions;
import com.github.cafdataprocessing.processing.service.client.model.ExistingCondition;
import com.github.cafdataprocessing.processing.service.client.model.ExistingConditions;
import com.github.cafdataprocessing.processing.service.client.model.ExistingProcessingRule;
import com.github.cafdataprocessing.processing.service.client.model.ExistingWorkflow;
import com.github.cafdataprocessing.processing.service.client.model.ProcessingRules;
import com.github.cafdataprocessing.workflow.transform.models.FullAction;
import com.github.cafdataprocessing.workflow.transform.models.FullProcessingRule;
import com.github.cafdataprocessing.workflow.transform.models.FullWorkflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves details of specified workflow and its children
 * i.e. processing rules, rule conditions, actions, action conditions.
 */
public class FullWorkflowRetriever {
    private final ProcessingApisProvider apisProvider;

    /**
     * Creates a FullWorkflowRetriever using the provided ApiClient.
     * @param apiClient for use in accessing processing-service APIs
     */
    public FullWorkflowRetriever(final ApiClient apiClient){
        this(new ProcessingApisProvider(apiClient));
    }

    /**
     * Creates a FullWorkflowRetriever using the provided ProcessingApiProvider.
     * @param apisProvider for use in accessing processing-service APIs
     */
    public FullWorkflowRetriever(final ProcessingApisProvider apisProvider){
        this.apisProvider = apisProvider;
    }

    /**
     * Uses the ID provided to retrieve a workflow, its processing rules, rule conditions, actions and action conditions.
     * @param projectId projectId value set for the workflow and children
     * @param workflowId ID of the workflow to return details for
     * @return the full details of the workflow with provided ID
     * @throws ApiException if error occurs communicating with processing service
     */
    public FullWorkflow getFullWorkflow(String projectId, long workflowId) throws ApiException {

        final WorkflowsApi workflowsApi = this.apisProvider.getWorkflowsApi();
        final ExistingWorkflow retrievedWorkflow = workflowsApi.getWorkflow(projectId, workflowId);
        final List<FullProcessingRule> fullProcessingRules = buildFullProcessingRules(projectId, workflowId);
        return new FullWorkflow(retrievedWorkflow, fullProcessingRules);
    }

    private List<FullProcessingRule> buildFullProcessingRules(String projectId, long workflowId) throws ApiException {
        final ProcessingRulesApi rulesApi = this.apisProvider.getProcessingRulesApi();
        final List<FullProcessingRule> fullProcessingRules = new ArrayList<>();
        int pageNum = 1;
        final int pageSize = 100;
        while(true) {
            final ProcessingRules retrievedProcessingRulesResult = rulesApi.getRules(projectId, workflowId, pageNum, pageSize);
            final List<ExistingProcessingRule> retrievedProcessingRules = retrievedProcessingRulesResult.getRules();
            for (ExistingProcessingRule retrievedProcessingRule: retrievedProcessingRules){
                fullProcessingRules.add(buildFullProcessingRule(
                        projectId,
                        workflowId,
                        retrievedProcessingRule
                ));
            }
            if(retrievedProcessingRulesResult.getTotalHits() <= pageSize*pageNum){
                break;
            }
            pageNum++;
        }
        return fullProcessingRules;
    }

    private FullProcessingRule buildFullProcessingRule(String projectId, long workflowId,
                                                       ExistingProcessingRule existingProcessingRule) throws ApiException {
        final long processingRuleId = existingProcessingRule.getId();
        final ProcessingRulesConditionsApi rulesConditionsApi = this.apisProvider.getRulesConditionsApi();
        final List<ExistingCondition> ruleConditions = new ArrayList<>();
        int pageNum = 1;
        final int pageSize = 100;
        while(true){
            final ExistingConditions retrievedConditionsResult = rulesConditionsApi.getRuleConditions(projectId,
                    workflowId, processingRuleId,
                    pageNum, pageSize);
            ruleConditions.addAll(retrievedConditionsResult.getConditions());
            if(retrievedConditionsResult.getTotalHits() <= pageSize*pageNum){
                break;
            }
            pageNum++;
        }
        final List<FullAction> fullActions = new ArrayList<>();
        pageNum = 1;
        final ActionsApi actionsApi = this.apisProvider.getActionsApi();
        while(true){
            final ExistingActions retrievedActionsResult =
                    actionsApi.getActions(projectId, workflowId, processingRuleId, pageNum, pageSize);
            for(ExistingAction retrievedAction: retrievedActionsResult.getActions()){
                fullActions.add(buildFullAction(projectId, workflowId, processingRuleId,
                        retrievedAction));
            }
            if(retrievedActionsResult.getTotalHits() <= pageSize*pageNum){
                break;
            }
            pageNum++;
        }
        return new FullProcessingRule(existingProcessingRule, fullActions, ruleConditions);
    }

    private FullAction buildFullAction(String projectId, long workflowId, long processingRuleId,
                                       ExistingAction existingAction) throws ApiException {
        final long actionId = existingAction.getId();
        final ActionConditionsApi actionConditionsApi = this.apisProvider.getActionConditionsApi();
        final List<ExistingCondition> actionConditions = new ArrayList<>();

        int pageNum = 1;
        final int pageSize = 100;
        while(true) {
            final ExistingConditions retrievedConditionsResult =
                    actionConditionsApi.getActionConditions(
                            projectId, workflowId, processingRuleId, actionId, pageNum, pageSize
                    );
            actionConditions.addAll(retrievedConditionsResult.getConditions());
            if(retrievedConditionsResult.getTotalHits() <= pageSize*pageNum){
                break;
            }
            pageNum++;
        }
        return new FullAction(existingAction, actionConditions);
    }
}
