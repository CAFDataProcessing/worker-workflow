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

import com.github.cafdataprocessing.processing.service.client.model.*;
import com.github.cafdataprocessing.workflow.transform.models.FullAction;
import com.github.cafdataprocessing.workflow.transform.models.FullProcessingRule;
import com.github.cafdataprocessing.workflow.transform.models.FullWorkflow;

import java.util.List;

/**
 * Builds workflow objects for use in tests.
 */
public class WorkflowComponentBuilder {
    private static long conditionIdCounter = 1;
    private static long actionIdCounter = 1;
    private static long ruleIdCounter = 1;
    private static long workflowIdCounter = 1;

    public FullWorkflow buildFullWorkflow(String name, List<FullProcessingRule> rules){
        ExistingWorkflow workflow = new ExistingWorkflow();
        workflow.setId(workflowIdCounter++);
        workflow.setName(name);
        return new FullWorkflow(workflow, rules);
    }

    public FullProcessingRule buildFullProcessingRule(String name, boolean enabled, int priority,
                                                       List<FullAction> actions, List<ExistingCondition> ruleConditions){
        ExistingProcessingRule rule = new ExistingProcessingRule();
        rule.setId(ruleIdCounter++);
        rule.setName(name);
        rule.setEnabled(enabled);
        rule.setPriority(priority);
        return new FullProcessingRule(rule, actions, ruleConditions);
    }

    public FullAction buildFullAction(String name, List<ExistingCondition> conditions, int order, Object settings){
        ExistingAction action = new ExistingAction();
        action.setId(actionIdCounter++);
        action.setName(name);
        action.setOrder(order);
        action.setTypeId(1L);
        action.setTypeInternalName("ChainedActionType");
        action.setSettings(settings);
        return new FullAction(action, conditions);
    }

    public FullAction buildFullAction(String name, List<ExistingCondition> conditions, int order, Object settings,
                                      String typeInternalName){
        ExistingAction action = new ExistingAction();
        action.setId(actionIdCounter++);
        action.setName(name);
        action.setOrder(order);
        action.setTypeId(1L);
        action.setTypeInternalName(typeInternalName);
        action.setSettings(settings);
        return new FullAction(action, conditions);
    }

    public ExistingCondition buildStringCondition(String name, String field, String value,
                                                   int order,
                                                   StringConditionAdditional.OperatorEnum operator){
        ExistingCondition condition = new ExistingCondition();
        condition.setId(conditionIdCounter++);
        condition.setName(name);
        StringConditionAdditional additional = new StringConditionAdditional();
        additional.setOperator(operator);
        additional.setValue(value);
        additional.setType(ConditionCommon.TypeEnum.STRING);
        additional.setField(field);
        additional.setOrder(order);
        condition.setAdditional(additional);
        return condition;
    }

    public ExistingCondition buildBooleanCondition(String name, int order,
                                                    BooleanConditionAdditional.OperatorEnum operator,
                                                    List<Condition> children){
        ExistingCondition condition = new ExistingCondition();
        condition.setId(conditionIdCounter++);
        condition.setName(name);
        BooleanConditionAdditional additional = new BooleanConditionAdditional();
        additional.setType(ConditionCommon.TypeEnum.BOOLEAN);
        additional.setOrder(order);
        additional.setOperator(operator);
        additional.setChildren(children);
        condition.setAdditional(additional);
        return condition;
    }
}
