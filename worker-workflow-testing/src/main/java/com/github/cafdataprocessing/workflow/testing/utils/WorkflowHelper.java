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
package com.github.cafdataprocessing.workflow.testing.utils;

import com.github.cafdataprocessing.workflow.testing.models.ApplicationMock;
import com.github.cafdataprocessing.workflow.testing.models.ConfigurationSourceMock;
import com.github.cafdataprocessing.workflow.testing.models.DocumentMock;
import com.github.cafdataprocessing.workflow.testing.models.FieldsMock;
import com.github.cafdataprocessing.workflow.testing.models.InputMessageProcessorMock;
import com.github.cafdataprocessing.workflow.testing.models.SubdocumentMock;
import com.github.cafdataprocessing.workflow.testing.models.TaskMock;
import com.github.cafdataprocessing.workflow.testing.models.WorkerTaskDataMock;
import com.hpe.caf.api.worker.TaskSourceInfo;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.WorkerTaskData;
import com.hpe.caf.worker.document.config.DocumentWorkerConfiguration;
import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Failures;
import com.hpe.caf.worker.document.model.Fields;
import com.hpe.caf.worker.document.model.InputMessageProcessor;
import com.hpe.caf.worker.document.model.Subdocument;
import com.hpe.caf.worker.document.model.Subdocuments;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

public class WorkflowHelper
{
    private WorkflowHelper()
    {
    }

    /**
     * Utility method to create a Nashorn engine with a predefined set of actions and the workflow-control.js loaded.
     *
     * @return an Invocable nashorn engine
     * @throws IOException
     * @throws ScriptException
     */
    public static Invocable createInvocableNashornEngineWithActionsAndWorkflowControl() throws IOException, ScriptException
    {
        final ScriptEngine nashorn = getScriptEngine();
        nashorn.eval("var actionFamilyHashing = {name: \"family_hashing\", terminateOnFailure: false};\n"
            + "var actionBulkIndexer = {name: \"bulk_indexer\", terminateOnFailure: true};\n"
            + "var actionElastic = {name: \"elastic\", terminateOnFailure: false};\n"
            + "var ACTIONS = [actionFamilyHashing, actionBulkIndexer, actionElastic];");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "main", "resources", "workflow-control.js")
            .toFile())));
        evalAddFailuresScript(nashorn);
        return (Invocable) nashorn;
    }

    private static void evalAddFailuresScript(final ScriptEngine engine) throws IOException, ScriptException
    {
        try(final InputStreamReader reader = new InputStreamReader(
                new FileInputStream(Paths.get("src", "main", "resources", "add-failures.js")
                .toFile()))) {
            final String addFailures = IOUtils.toString(reader);
            engine.eval("\nthisScriptObject = `\n" + addFailures +"`;\n");
        }
    }

    /**
     * Utility method to create a Nashorn engine with a predefined set of actions and the workflow-control.js loaded. 
     * This function will also eval any scripts passed to it as params.
     *
     * @param scripts variable number of scripts to eval
     * @return an Invocable nashorn engine
     * @throws IOException
     * @throws ScriptException
     */
    public static Invocable createInvocableNashornEngineWithActionsAndWorkflowControl(final String... scripts)
        throws IOException, ScriptException
    {
        final ScriptEngine nashorn = getScriptEngine();
        nashorn.eval("var actionFamilyHashing = {name: \"family_hashing\", terminateOnFailure: false};\n"
            + "var actionBulkIndexer = {name: \"bulk_indexer\", terminateOnFailure: true};\n"
            + "var actionElastic = {name: \"elastic\", terminateOnFailure: false};\n"
            + "var ACTIONS = [actionFamilyHashing, actionBulkIndexer, actionElastic];");
        nashorn.eval(new InputStreamReader(new FileInputStream(Paths.get("src", "main", "resources", "workflow-control.js")
            .toFile())));
        evalAddFailuresScript(nashorn);
        for(final String script : scripts){
            nashorn.eval(script);
        }
        return (Invocable) nashorn;
    }

    /**
     * Utility method to create a Nashorn engine that accepts optional strings to be eval and/or paths to files to be eval as well.
     *
     * @param codesToEval list of strings
     * @param filesToReadAndEval list of paths
     * @return an Invocable nashorn engine
     * @throws IOException
     * @throws ScriptException
     */
    public static Invocable createInvocableNashornEngine(final List<String> codesToEval, final List<Path> filesToReadAndEval)
        throws IOException, ScriptException
    {
        final ScriptEngine nashorn = getScriptEngine();
        if (CollectionUtils.isNotEmpty(codesToEval)) {
            for (final String code : codesToEval) {
                nashorn.eval(code);
            }
        }
        if (CollectionUtils.isNotEmpty(filesToReadAndEval)) {
            for (final Path inputFile : filesToReadAndEval) {
                nashorn.eval(new InputStreamReader(new FileInputStream(inputFile.toFile())));
            }
        }
        return (Invocable) nashorn;
    }

    /**
     * Utility method to create a document.
     *
     * @param reference just the reference of the main doc
     * @param fields values in fields
     * @param failures values in failures
     * @param subdocuments the subdocuments to be added
     * @param parentDoc the parent doc
     * @param rootDoc the root doc
     * @param includeApplication does the test need an application object?
     * @param inputMessageProcessor this param has only a meaning if the include application is true. If set to true, we assume that the
     * worker will handle all subdocuments for us, if false it will not, and the workflow-control script has to do it for us.
     * @return a Document
     */
    public static Document createDocument(final String reference, final Fields fields, final Failures failures,
                                          final Map<String, String> customData, final Subdocuments subdocuments, 
                                          final Document parentDoc, final Document rootDoc, final boolean includeApplication,
                                          final boolean inputMessageProcessor)
    {
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                          "to", tsi);
        final DocumentWorkerConfiguration dwc = new DocumentWorkerConfiguration();
        dwc.setWorkerName("worker-base");
        dwc.setWorkerVersion("1.0.0-SNAPSHOT");
        final ConfigurationSourceMock csm = new ConfigurationSourceMock(dwc);
        final TaskMock task;
        final Application application;
        final Map<String, String> docCustomData = (customData!=null)? customData : new HashMap<>();
        if (!includeApplication) {
            task = new TaskMock(docCustomData, rootDoc, null, wtd, null, null);
            application = null;
        } else {
            final InputMessageProcessor inputMessageProcessorTest = new InputMessageProcessorMock(inputMessageProcessor);
            application = new ApplicationMock(inputMessageProcessorTest, csm);
            task = new TaskMock(docCustomData, rootDoc, null, wtd, null, application);
        }
        final DocumentMock temp
            = new DocumentMock(reference, fields, task, docCustomData, failures, subdocuments, application, parentDoc, rootDoc);
        task.setDocument(temp);
        final Fields mockedFields = new FieldsMock(fields, null, temp);
        temp.setFields(mockedFields);
        temp.setRootDocument(temp);
        return temp;
    }

    /**
     * Utility method to create a subdocument.
     *
     * @param reference just the reference of the subdoc
     * @param fields values in fields
     * @param failures values in failures
     * @param subdocuments the subdocuments to be added
     * @param parentDoc the parent doc
     * @param rootDoc the root doc
     * @param includeApplication does the test need an application object?
     * @param inputMessageProcessor this param has only a meaning if the include application is true. If set to true, we assume that the
     * worker will handle all subdocuments for us, if false it will not, and the workflow-control script has to do it for us.
     * @return a Subdocument
     */
    public static Subdocument createSubdocument(final String reference, final Fields fields, final Failures failures,
                                                final Subdocuments subdocuments,
                                                final Document parentDoc, final Document rootDoc, final boolean includeApplication,
                                                final boolean inputMessageProcessor)
    {
        final TaskSourceInfo tsi = new TaskSourceInfo("source_name", "5");
        final WorkerTaskData wtd = new WorkerTaskDataMock("classifier", 2, TaskStatus.RESULT_SUCCESS, new byte[0], new byte[0], null,
                                                          "to", tsi);
        final DocumentWorkerConfiguration dwc = new DocumentWorkerConfiguration();
        dwc.setWorkerName("worker-base");
        dwc.setWorkerVersion("1.0.0-SNAPSHOT");
        final ConfigurationSourceMock csm = new ConfigurationSourceMock(dwc);
        final TaskMock task;
        final Application application;
        if (!includeApplication) {
            task = new TaskMock(new HashMap<>(), rootDoc, null, wtd, null, null);
            application = null;
        } else {
            final InputMessageProcessor inputMessageProcessorTest = new InputMessageProcessorMock(inputMessageProcessor);
            application = new ApplicationMock(inputMessageProcessorTest, csm);
            task = new TaskMock(new HashMap<>(), rootDoc, null, wtd, null, application);
        }
        final Subdocument temp
            = new SubdocumentMock(reference, fields, task, new HashMap<>(), failures, subdocuments, application, parentDoc, rootDoc);
        task.setDocument(temp);
        return temp;
    }

    private static ScriptEngine getScriptEngine() {
        return GraalJSScriptEngine.create(
            null,
            Context.newBuilder("js")
                .allowExperimentalOptions(true) // Needed for loading from classpath
                .allowHostAccess(HostAccess.ALL) // Allow JS access to public Java methods/members
                .allowHostClassLookup(s -> true) // Allow JS access to public Java classes
                .option("js.load-from-classpath", "true"));
    }
}
