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
