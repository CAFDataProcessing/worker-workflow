package com.github.cafdataprocessing.workflow.model;

import java.util.List;
import java.util.Map;

public class Workflow {
    private List<SettingDefinition> settingDefinitions;


    private WorkflowSettings settingsForCustomData;
    private Map<String, Action> actions;
    private String workflowScript;
    private String storageReference;

    public WorkflowSettings getSettingsForCustomData() {
        return settingsForCustomData;
    }

    public void setSettingsForCustomData(WorkflowSettings settingsForCustomData) {
        this.settingsForCustomData = settingsForCustomData;
    }

    public String getStorageReferenceForWorkflowScript() {
        return storageReference;
    }

    public void setStorageReference(String storageReference) {
        this.storageReference = storageReference;
    }

    public Map<String, Action> getActions() {
        return actions;
    }

    public void setActions(Map<String, Action> actions) {
        this.actions = actions;
    }

    public String getWorkflowScript() {
        return workflowScript;
    }

    public void setWorkflowScript(String workflowScript) {
        this.workflowScript = workflowScript;
    }

    public List<SettingDefinition> getSettingDefinitions() {
        return settingDefinitions;
    }

    public void setSettingDefinitions(List<SettingDefinition> settingDefinitions) {
        this.settingDefinitions = settingDefinitions;
    }
}
