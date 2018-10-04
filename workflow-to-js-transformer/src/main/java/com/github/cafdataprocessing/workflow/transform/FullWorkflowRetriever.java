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
import com.github.cafdataprocessing.processing.service.client.model.ExistingWorkflows;
import com.github.cafdataprocessing.processing.service.client.model.ProcessingRules;
import com.github.cafdataprocessing.workflow.spec.WorkflowIdBasedSpec;
import com.github.cafdataprocessing.workflow.spec.WorkflowNameBasedSpec;
import com.github.cafdataprocessing.workflow.spec.WorkflowSpec;
import com.github.cafdataprocessing.workflow.transform.exceptions.InvalidWorkflowSpecificationException;
import com.github.cafdataprocessing.workflow.transform.models.FullAction;
import com.github.cafdataprocessing.workflow.transform.models.FullProcessingRule;
import com.github.cafdataprocessing.workflow.transform.models.FullWorkflow;
import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.jersey.api.client.ClientHandlerException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Retrieves details of specified workflow and its children i.e. processing rules, rule conditions, actions, action conditions.
 */
public class FullWorkflowRetriever
{
    private final ProcessingApisProvider apisProvider;
    private final LoadingCache<WorkflowIdCacheKey, Long> workflowIdCache;

    /**
     * Creates a FullWorkflowRetriever using the provided ApiClient.
     *
     * @param apiClient for use in accessing processing-service APIs
     */
    public FullWorkflowRetriever(final ApiClient apiClient)
    {
        this(new ProcessingApisProvider(apiClient), null);
    }

    /**
     * Creates a FullWorkflowRetriever using the provided ProcessingApiProvider.
     *
     * @param apisProvider for use in accessing processing-service APIs
     */
    public FullWorkflowRetriever(final ProcessingApisProvider apisProvider, final String workflowIdsCachePeriodAsStr)
    {
        this.apisProvider = apisProvider;
        final Duration workflowCachePeriod = workflowIdsCachePeriodAsStr == null || workflowIdsCachePeriodAsStr.isEmpty()
            ? Duration.parse("PT5M")
            : Duration.parse(workflowIdsCachePeriodAsStr);
        this.workflowIdCache = CacheBuilder.newBuilder()
            .expireAfterWrite(workflowCachePeriod.get(ChronoUnit.SECONDS), TimeUnit.SECONDS)
            .build(new CacheLoader<WorkflowIdCacheKey, Long>()
            {
                @Override
                public Long load(final WorkflowIdCacheKey key) throws ApiException, InvalidWorkflowSpecificationException
                {
                    return getWorkflowId(key.getProjectId(), key.getWorkflowName());
                }
            });
    }

    /**
     * Uses the ID provided to retrieve a workflow, its processing rules, rule conditions, actions and action conditions.
     *
     * @param workflowSpec projectId value set for the workflow and children
     * @return the full details of the workflow with provided ID
     * @throws ApiException if certain failures occur communicating with the processing service to retrieve the workflow e.g. Invalid
     * requests will result in this exception.
     * @throws WorkflowRetrievalException if certain failures occur communicating with the processing service to retrieve the workflow.
     * e.g. The processing service not being contactable.
     */
    public FullWorkflow getFullWorkflow(final WorkflowSpec workflowSpec)
        throws ApiException, WorkflowRetrievalException, InvalidWorkflowSpecificationException
    {
        final String projectId = workflowSpec.getProjectId();
        final long workflowId;

        if (workflowSpec instanceof WorkflowIdBasedSpec) {
            final WorkflowIdBasedSpec workflowIdbasedSpec = (WorkflowIdBasedSpec) workflowSpec;
            workflowId = workflowIdbasedSpec.getWorkflowId();
        } else if (workflowSpec instanceof WorkflowNameBasedSpec) {
            final WorkflowNameBasedSpec workflowNameBasedSpec = (WorkflowNameBasedSpec) workflowSpec;
            workflowId = getWorkflowIdFromCache(projectId, workflowNameBasedSpec.getWorkflowName());
        } else {
            throw new RuntimeException("Unkown type of workflow spec has been passed for processing");
        }

        return getFullWorkflow(projectId, workflowId);
    }

    private long getWorkflowIdFromCache(final String projectId, final String workflowName)
        throws ApiException, InvalidWorkflowSpecificationException
    {
        // Put together the cache key
        final WorkflowIdCacheKey key = new WorkflowIdCacheKey(projectId, workflowName);

        // Retrieve the workflow id from the cache
        try {
            return workflowIdCache.get(key);
        } catch (final ExecutionException ex) {
            final Throwable cause = ex.getCause();

            if (cause instanceof ApiException) {
                throw (ApiException) cause;
            } else if (cause instanceof InvalidWorkflowSpecificationException) {
                throw (InvalidWorkflowSpecificationException) cause;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Uses the ID provided to retrieve a workflow, its processing rules, rule conditions, actions and action conditions.
     *
     * @param projectId projectId value set for the workflow and children
     * @param workflowId ID of the workflow to return details for
     * @return the full details of the workflow with provided ID
     * @throws ApiException if certain failures occur communicating with the processing service to retrieve the workflow e.g. Invalid
     * requests will result in this exception.
     * @throws WorkflowRetrievalException if certain failures occur communicating with the processing service to retrieve the workflow.
     * e.g. The processing service not being contactable.
     */
    private FullWorkflow getFullWorkflow(String projectId, long workflowId) throws ApiException, WorkflowRetrievalException
    {

        final WorkflowsApi workflowsApi = this.apisProvider.getWorkflowsApi();
        final ExistingWorkflow retrievedWorkflow;
        final List<FullProcessingRule> fullProcessingRules;
        try {
            retrievedWorkflow = workflowsApi.getWorkflow(projectId, workflowId);
            fullProcessingRules = buildFullProcessingRules(projectId, workflowId);
        } catch (final ClientHandlerException e) {
            throw new WorkflowRetrievalException("Failure retrieving the full workflow using processing service.", e);
        }
        return new FullWorkflow(retrievedWorkflow, fullProcessingRules);
    }

    private List<FullProcessingRule> buildFullProcessingRules(String projectId, long workflowId) throws ApiException
    {
        final ProcessingRulesApi rulesApi = this.apisProvider.getProcessingRulesApi();
        final List<FullProcessingRule> fullProcessingRules = new ArrayList<>();
        int pageNum = 1;
        final int pageSize = 100;
        while (true) {
            final ProcessingRules retrievedProcessingRulesResult = rulesApi.getRules(projectId, workflowId, pageNum, pageSize);
            final List<ExistingProcessingRule> retrievedProcessingRules = retrievedProcessingRulesResult.getRules();
            for (ExistingProcessingRule retrievedProcessingRule : retrievedProcessingRules) {
                fullProcessingRules.add(buildFullProcessingRule(
                    projectId,
                    workflowId,
                    retrievedProcessingRule
                ));
            }
            if (retrievedProcessingRulesResult.getTotalHits() <= pageSize * pageNum) {
                break;
            }
            pageNum++;
        }
        return fullProcessingRules;
    }

    private FullProcessingRule buildFullProcessingRule(
        String projectId,
        long workflowId,
        ExistingProcessingRule existingProcessingRule
    ) throws ApiException
    {
        final long processingRuleId = existingProcessingRule.getId();
        final ProcessingRulesConditionsApi rulesConditionsApi = this.apisProvider.getRulesConditionsApi();
        final List<ExistingCondition> ruleConditions = new ArrayList<>();
        int pageNum = 1;
        final int pageSize = 100;
        while (true) {
            final ExistingConditions retrievedConditionsResult
                = rulesConditionsApi.getRuleConditions(projectId, workflowId, processingRuleId, pageNum, pageSize);
            ruleConditions.addAll(retrievedConditionsResult.getConditions());
            if (retrievedConditionsResult.getTotalHits() <= pageSize * pageNum) {
                break;
            }
            pageNum++;
        }
        final List<FullAction> fullActions = new ArrayList<>();
        pageNum = 1;
        final ActionsApi actionsApi = this.apisProvider.getActionsApi();
        while (true) {
            final ExistingActions retrievedActionsResult
                = actionsApi.getActions(projectId, workflowId, processingRuleId, pageNum, pageSize);
            for (ExistingAction retrievedAction : retrievedActionsResult.getActions()) {
                fullActions.add(buildFullAction(projectId, workflowId, processingRuleId,
                                                retrievedAction));
            }
            if (retrievedActionsResult.getTotalHits() <= pageSize * pageNum) {
                break;
            }
            pageNum++;
        }
        return new FullProcessingRule(existingProcessingRule, fullActions, ruleConditions);
    }

    private FullAction buildFullAction(
        String projectId,
        long workflowId,
        long processingRuleId,
        ExistingAction existingAction
    ) throws ApiException
    {
        final long actionId = existingAction.getId();
        final ActionConditionsApi actionConditionsApi = this.apisProvider.getActionConditionsApi();
        final List<ExistingCondition> actionConditions = new ArrayList<>();

        int pageNum = 1;
        final int pageSize = 100;
        while (true) {
            final ExistingConditions retrievedConditionsResult
                = actionConditionsApi.getActionConditions(
                    projectId, workflowId, processingRuleId, actionId, pageNum, pageSize
                );
            actionConditions.addAll(retrievedConditionsResult.getConditions());
            if (retrievedConditionsResult.getTotalHits() <= pageSize * pageNum) {
                break;
            }
            pageNum++;
        }
        return new FullAction(existingAction, actionConditions);
    }

    private long getWorkflowId(final String projectId, final String workflowName)
        throws ApiException, InvalidWorkflowSpecificationException
    {
        final WorkflowsApi workflowsApi = apisProvider.getWorkflowsApi();
        final ExistingWorkflows existingWorkflows = workflowsApi.getWorkflows(projectId, 1, 100);
        return existingWorkflows.getWorkflows().stream()
            .filter(wf -> wf.getName().equals(workflowName))
            .map(ExistingWorkflow::getId)
            .findAny()
            .orElseThrow(() -> new InvalidWorkflowSpecificationException("The name of the workflow provided could not be resolved."));
    }

    private static final class WorkflowIdCacheKey
    {
        private final String projectId;
        private final String workflowName;

        public WorkflowIdCacheKey(final String projectId, final String workflowName)
        {
            this.projectId = projectId;
            this.workflowName = workflowName;
        }

        public String getProjectId()
        {
            return this.projectId;
        }

        public String getWorkflowName()
        {
            return this.workflowName;
        }

        @Override
        public boolean equals(final Object obj)
        {
            if (!(obj instanceof WorkflowIdCacheKey)) {
                return false;
            }

            final WorkflowIdCacheKey other = (WorkflowIdCacheKey) obj;

            return Objects.equal(this.projectId, other.projectId)
                && Objects.equal(this.workflowName, other.workflowName);
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash = 83 * hash + java.util.Objects.hashCode(projectId);
            hash = 83 * hash + java.util.Objects.hashCode(workflowName);
            return hash;
        }
    }
}
