/*
 * Copyright 2017-2022 Micro Focus or one of its affiliates.
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

import com.github.cafdataprocessing.workflow.model.Action;
import com.github.cafdataprocessing.workflow.model.Workflow;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.worker.document.model.Application;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowManager {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowManager.class);

    private final Map<String, Workflow> workflows;
    private final DataStore dataStore;

    public WorkflowManager(final Application application, final String workflowDirectory, final String contextScriptFilePath)
                           throws ConfigurationException {
        dataStore = application.getService(DataStore.class);
        workflows = getWorkflows(workflowDirectory, contextScriptFilePath);
    }

    public Workflow get(final String workflowName){
        return workflows.get(workflowName);
    }

    private static File getContextScriptFile(final String contextScriptFilePath) {
        return contextScriptFilePath != null ? new File(contextScriptFilePath) : null;
    }

    private Map<String, Workflow> getWorkflows(final String workflowsDirectory, final String contextScriptFilePath)
            throws ConfigurationException {

        final Map<String, Workflow> workflowMap = new HashMap<>();
        final Yaml yaml = new Yaml();
        final Gson gson = new GsonBuilder().registerTypeAdapter(Action.class, new ActionTypeAdapter())
            .setPrettyPrinting().create();

        final File dir = new File(workflowsDirectory);
        if(Strings.isNullOrEmpty(dir.toString())){
            throw new ConfigurationException(String.format("No workflows found in [%s].", workflowsDirectory));
        }
        final File contextScriptFile = getContextScriptFile(contextScriptFilePath);
        final FilenameFilter filter = (final File dir1, final String name) -> name.endsWith(".yaml");
        for (final File workflowFile : dir.listFiles(filter)) {

            if(!workflowFile.exists()){
                throw new RuntimeException(String.format("File [%s] does not exist.",
                        workflowFile.toPath().toAbsolutePath()));
            }

            try (final FileInputStream fis = new FileInputStream(workflowFile)) {
                final Workflow workflow = yaml.loadAs(fis, Workflow.class);

                validateWorkflow(workflow);

                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("var ACTIONS = %s;\n", gson.toJson(workflow.getActions())));

                final String contextScriptFileContent;
                if (contextScriptFile != null) {
                    if (contextScriptFile.exists()) {
                        contextScriptFileContent = FileUtils.readFileToString(contextScriptFile,
                                                                                           StandardCharsets.UTF_8);
                        stringBuilder.append(contextScriptFileContent);
                    } else {
                        LOG.warn("The context script file from the path {} does not exist.", contextScriptFilePath);
                        contextScriptFileContent = null;
                    }
                } else {
                    contextScriptFileContent = null;
                }

                try {
                    stringBuilder.append(Resources.toString(Resources.getResource("workflow-control.js"),
                            StandardCharsets.UTF_8));
                } catch (final IOException e) {
                    throw new RuntimeException("Could not obtain workflow-control.js");
                }

                try {
                    final String addFailures = Resources.toString(Resources.getResource("add-failures.js"),
                            StandardCharsets.UTF_8).replaceAll("`", "\\`");
                    stringBuilder.append("thisScriptObject = String.raw`\n").append(addFailures);
                    if(contextScriptFileContent != null) {
                        stringBuilder.append(contextScriptFileContent);
                    }
                    stringBuilder.append("\n`;");
                } catch (final IOException e) {
                    throw new RuntimeException("Could not obtain add add-failures.js");
                }

                workflow.setWorkflowScript(stringBuilder.toString());

                workflow.setStorageReference(
                        dataStore.store(workflow.getWorkflowScript().getBytes(StandardCharsets.UTF_8),
                                "workflow-scripts"));

                final String entryname = workflowFile.getName().replaceAll(".yaml$", "");
                workflowMap.put(entryname, workflow);
            }
            catch(final IOException ex){
                throw new ConfigurationException(
                        String.format("Could not access workflow [%s] in configured directory.",
                                workflowFile.toPath().toAbsolutePath(), ex));
            }
            catch (final DataStoreException ex){
                throw new ConfigurationException("Could not store workflow in configured datastore.", ex);
            }
        }

        if(workflowMap.isEmpty()){
            throw new ConfigurationException("No workflows available.");
        }

        return workflowMap;
    }

    private static void validateWorkflow(final Workflow workflow) throws ConfigurationException {

        final List<String> actionNames = new ArrayList<>();
        for(int index = 0; index < workflow.getActions().size(); index ++) {
            final Action action = workflow.getActions().get(index);
            if(Strings.isNullOrEmpty(action.getName())){
                throw new ConfigurationException(String.format("Action name is not defined for action [%s].", index));
            }
            if(actionNames.contains(action.getName())){
                throw new ConfigurationException(String.format("Duplicated action name [%s].", action.getName()));
            }
            actionNames.add(action.getName());
        }
    }
}
