package com.github.cafdataprocessing.workflow;

import com.github.cafdataprocessing.workflow.model.Workflow;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.worker.document.model.Application;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WorkflowManager {

    private final Map<String, Workflow> workflows;
    private final DataStore dataStore;

    public WorkflowManager(final Application application, String workflowDirectory) throws ConfigurationException {
        dataStore = application.getService(DataStore.class);
        workflows = getWorkflows(workflowDirectory);
    }

    public Workflow get(final String workflowName){
        return workflows.get(workflowName);
    }

    private Map<String, Workflow> getWorkflows(final String workflowsDirectory) throws ConfigurationException {

        final Map<String, Workflow> workflowMap = new HashMap<>();
        final Yaml yaml = new Yaml();
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        final Gson gson = new GsonBuilder().create();

        final File dir = new File(workflowsDirectory);
        if(Strings.isNullOrEmpty(dir.toString())){
            throw new ConfigurationException(String.format("No workflows found in [%s].", workflowsDirectory));
        }
        final FilenameFilter filter = (final File dir1, final String name) -> name.endsWith(".yaml");
        final String[] workflows = dir.list(filter);
        for (final String filename : workflows) {
            try (FileInputStream fis = new FileInputStream(new File(workflowsDirectory + "/" + filename))) {
                final Workflow workflow = yaml.loadAs(fis, Workflow.class);

                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("var ACTIONS = %s;\n", gson.toJson(workflow.getActions())));
                try {
                    stringBuilder.append(Resources.toString(Resources.getResource("workflow-control.js"),
                            StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException("Could not obtain workflow-control.js");
                }
                workflow.setWorkflowScript(stringBuilder.toString());

                workflow.setStorageReference(
                        dataStore.store(workflow.getWorkflowScript().getBytes(StandardCharsets.UTF_8),
                                "workflow-scripts"));

                final String entryname = filename.replaceAll(".yaml$", "");
                workflowMap.put(entryname, workflow);
            }
            catch(final IOException ex){
                throw new ConfigurationException("Could not access workflow in configured directory", ex);
            }
            catch (final DataStoreException ex){
                throw new ConfigurationException("Could not store workflow in configured datastote", ex);
            }
        }

        if(workflowMap.isEmpty()){
            throw new ConfigurationException("No workflows available.");
        }

        return workflowMap;
    }

}
