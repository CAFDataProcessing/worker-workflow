package com.github.cafdataprocessing.workflow.model;

import java.util.Map;

public class Action {
    private String queueName;
    private String conditionFunction;
    private Map<String, String> customData;
    private Script[] scripts;

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getConditionFunction() {
        return conditionFunction;
    }

    public void setConditionFunction(String conditionFunction) {
        this.conditionFunction = conditionFunction;
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    public void setCustomData(Map<String, String> customData) {
        this.customData = customData;
    }

    public Script[] getScripts() {
        return scripts;
    }

    public void setScripts(Script[] scripts) {
        this.scripts = scripts;
    }
}
