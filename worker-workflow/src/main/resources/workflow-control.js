/*
 * Copyright 2017-2025 Open Text.
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
console.log("=== WORKFLOW SCRIPT INITIALIZATION ===");

var UnsupportedOperationException = Java.type("java.lang.UnsupportedOperationException");
console.log("UnsupportedOperationException loaded:", UnsupportedOperationException);

var RuntimeException = Java.type("java.lang.RuntimeException");
console.log("RuntimeException loaded:", RuntimeException);

var ArrayList = Java.type("java.util.ArrayList");
console.log("ArrayList loaded:", ArrayList);

var URL = Java.type("java.net.URL");
console.log("URL loaded:", URL);

var MDC = Java.type("org.slf4j.MDC");
console.log("MDC loaded:", MDC);

var UUID = Java.type("java.util.UUID");
console.log("UUID loaded:", UUID);

var ScriptEngineType = Java.type("com.github.cafdataprocessing.workers.document.model.ScriptEngineType");
console.log("ScriptEngineType loaded:", ScriptEngineType);

var System = Java.type("java.lang.System");
console.log("System loaded:", System);

console.log("Checking ACTIONS object...");
if(!ACTIONS){
    console.log("ERROR: ACTIONS object is null or undefined");
    throw new UnsupportedOperationException ("Workflow script must define an ACTIONS object.");
}
console.log("ACTIONS object exists:", ACTIONS);
console.log("ACTIONS length:", ACTIONS.length);

function onProcessTask(e) {
    console.log("");
    console.log("=== onProcessTask STARTED ===");
    console.log("Event object:", e);
    console.log("Task:", e.task);

    addMdcLoggingData(e);
    console.log("addMdcLoggingData completed");

    thisScript.install();
    console.log("thisScript.install() completed");
    console.log("=== onProcessTask ENDED ===");
}

function addMdcLoggingData(e) {
    console.log("");
    console.log("=== addMdcLoggingData STARTED ===");
    console.log("Event parameter:", e);

    // The logging pattern we use uses a tenantId and a correlationId:
    //
    // https://github.com/CAFapi/caf-logging/tree/v1.0.0#pattern
    // https://github.com/CAFapi/caf-logging/blob/v1.0.0/src/main/resources/logback.xml#L27
    //
    // This function adds a tenantId to the MDC (http://logback.qos.ch/manual/mdc.html), so that log messages
    // from workers in the workflow will contain these values.
    // Additionally, code in worker-framework adds the correlationId to the MDC context.
    //
    // See also addMdcData in WorkflowWorker, which performs similar logic to ensure log messages from the workflow-worker itself also
    // contain these values.

    // Get MDC data from custom data.
    var tenantId = e.task.getCustomData("tenantId");
    console.log("Retrieved tenantId from custom data:", tenantId);

    // Only if this worker is NOT a bulk worker; add tenantId to the MDC.
    var bulkWorkerCheck = isBulkWorker(e);
    console.log("Is bulk worker check result:", bulkWorkerCheck);

    if (!bulkWorkerCheck) {
        console.log("Not a bulk worker, checking tenantId...");
        if (tenantId) {
            console.log("Adding tenantId to MDC:", tenantId);
            MDC.put("tenantId", tenantId);
            console.log("MDC.put completed for tenantId");
        } else {
            console.log("tenantId is null/empty, not adding to MDC");
        }
    } else {
        console.log("Is bulk worker, skipping MDC tenantId addition");
    }

    // Add MDC data to custom data so that its passed it onto the next worker.
    var responseCustomData = e.task.getResponse().getCustomData();
    console.log("Response custom data object:", responseCustomData);

    responseCustomData.put("tenantId", tenantId);
    console.log("Added tenantId to response custom data:", tenantId);
    console.log("=== addMdcLoggingData ENDED ===");
}

function isBulkWorker(e) {
    console.log("");
    console.log("=== isBulkWorker STARTED ===");
    console.log("Event parameter:", e);
    console.log("Root document:", e.rootDocument);

    var workflowActionField = e.rootDocument.getField("CAF_WORKFLOW_ACTION");
    console.log("Retrieved CAF_WORKFLOW_ACTION field:", workflowActionField);
    console.log("Field has values:", workflowActionField.hasValues());

    if (!workflowActionField.hasValues()) {
        console.log("ERROR: CAF_WORKFLOW_ACTION field has no values");
        throw new UnsupportedOperationException("Document must contain field CAF_WORKFLOW_ACTION.");
    }

    var stringValues = workflowActionField.getStringValues();
    console.log("CAF_WORKFLOW_ACTION string values:", stringValues);

    var firstValue = stringValues.get(0);
    console.log("First CAF_WORKFLOW_ACTION value:", firstValue);

    var bulkIndex = firstValue.indexOf("bulk");
    console.log("Index of 'bulk' in action:", bulkIndex);

    var isBulk = bulkIndex !== -1;
    console.log("Is bulk worker result:", isBulk);
    console.log("=== isBulkWorker ENDED ===");
    return isBulk;
}

function onAfterProcessTask(eventObj) {
    console.log("");
    console.log("=== onAfterProcessTask STARTED ===");
    console.log("Event object:", eventObj);
    console.log("Root document:", eventObj.rootDocument);

    routeTask(eventObj.rootDocument);
    console.log("routeTask completed");

    removeMdcLoggingData();
    console.log("removeMdcLoggingData completed");
    console.log("=== onAfterProcessTask ENDED ===");
}

function removeMdcLoggingData() {
    console.log("");
    console.log("=== removeMdcLoggingData STARTED ===");
    MDC.remove("tenantId");
    console.log("Removed tenantId from MDC");
    console.log("=== removeMdcLoggingData ENDED ===");
}

function onBeforeProcessDocument(e) {
    console.log("");
    console.log("=== onBeforeProcessDocument STARTED ===");
    console.log("Event object:", e);
    console.log("Root document:", e.rootDocument);

    //Get the action from ACTIONS, use the value of CAF_WORKFLOW_ACTION to know the name of the action
    var workflowActionField = e.rootDocument.getField("CAF_WORKFLOW_ACTION");
    console.log("CAF_WORKFLOW_ACTION field:", workflowActionField);
    console.log("Field has values:", workflowActionField.hasValues());

    if(!workflowActionField.hasValues()) {
        console.log("ERROR: CAF_WORKFLOW_ACTION field has no values");
        throw new UnsupportedOperationException("Document must contain field CAF_WORKFLOW_ACTION.");
    }

    var actionStringValues = workflowActionField.getStringValues();
    console.log("Action string values:", actionStringValues);

    var currentActionName = actionStringValues.get(0);
    console.log("Current action name:", currentActionName);

    var actionNames = ACTIONS.map(function (x) {
        console.log("Mapping action:", x);
        console.log("Action name:", x.name);
        return x.name;
    });
    console.log("All action names:", actionNames);

    var index = actionNames.indexOf(currentActionName);
    console.log("Index of current action in ACTIONS array:", index);

    var action = ACTIONS[index];
    console.log("Selected action object:", action);
    console.log("Action has condition function:", !!action.conditionFunction);

    if (!action.conditionFunction) {
        console.log("No condition function, exiting onBeforeProcessDocument");
        return;
    }

    var args = extractArguments(e.rootDocument);
    console.log("Extracted arguments:", args);

    console.log("Evaluating condition function:", action.conditionFunction);
    eval(action.conditionFunction);
    console.log("Condition function evaluated");

    var conditionResult = condition(e.document, args);
    console.log("Condition function result:", conditionResult);

    e.cancel = !conditionResult;
    console.log("Event cancel set to:", e.cancel);
    console.log("=== onBeforeProcessDocument ENDED ===");
}

function onProcessDocument(e) {
    console.log("");
    console.log("=== onProcessDocument STARTED ===");
    console.log("Event object:", e);
    console.log("Application:", e.application);

    var processSubdocsSeparately = e.application.getInputMessageProcessor().getProcessSubdocumentsSeparately();
    console.log("Process subdocuments separately:", processSubdocsSeparately);

    if (processSubdocsSeparately) {
        console.log("Processing single document for worker version");
        setWorkerVersion(e.document);
        console.log("setWorkerVersion completed for single document");
    } else {
        console.log("Traversing document tree for worker version");
        traverseDocumentForSettingWorkerVersion(e.document);
        console.log("traverseDocumentForSettingWorkerVersion completed");
    }
    console.log("=== onProcessDocument ENDED ===");
}

function traverseDocumentForSettingWorkerVersion(document) {
    console.log("");
    console.log("=== traverseDocumentForSettingWorkerVersion STARTED ===");
    console.log("Document:", document);

    setWorkerVersion(document);
    console.log("setWorkerVersion completed for current document");

    var subdocuments = document.getSubdocuments();
    console.log("Subdocuments:", subdocuments);
    console.log("Number of subdocuments:", subdocuments.size());

    subdocuments.forEach(function (subdoc) {
        console.log("Processing subdocument:", subdoc);
        traverseDocumentForSettingWorkerVersion(subdoc);
        console.log("Completed processing subdocument");
    });
    console.log("=== traverseDocumentForSettingWorkerVersion ENDED ===");
}

function setWorkerVersion(document) {
    console.log("");
    console.log("=== setWorkerVersion STARTED ===");
    console.log("Document:", document);

    var currentWorkerName = getCurrentWorkerName(document);
    console.log("Current worker name:", currentWorkerName);

    var versionFieldName = "PROCESSING_" + currentWorkerName.toUpperCase() + "_VERSION";
    console.log("Version field name:", versionFieldName);

    var currentWorkerVersion = getCurrentWorkerVersion(document);
    console.log("Current worker version:", currentWorkerVersion);

    var versionField = document.getField(versionFieldName);
    console.log("Version field:", versionField);

    versionField.set(currentWorkerVersion);
    console.log("Set worker version field to:", currentWorkerVersion);
    console.log("=== setWorkerVersion ENDED ===");
}

function onError(errorEventObj) {
    console.log("");
    console.log("=== onError STARTED ===");
    console.log("Error event object:", errorEventObj);
    console.log("Error:", errorEventObj.error);
    console.log("Error message:", errorEventObj.error.getMessage());

    thisScript.install();
    console.log("thisScript.install() completed");

    var rootDoc = errorEventObj.rootDocument;
    console.log("Root document:", rootDoc);

    var message = errorEventObj.error.getMessage();
    console.log("Error message extracted:", message);

    var failures = rootDoc.getFailures();
    console.log("Document failures:", failures);

    failures.add("UNHANDLED_ERROR", message, errorEventObj.error);
    console.log("Added UNHANDLED_ERROR to failures");

    var actionField = errorEventObj.rootDocument.getField("CAF_WORKFLOW_ACTION");
    console.log("CAF_WORKFLOW_ACTION field:", actionField);

    var actionValues = actionField.getStringValues();
    console.log("Action values:", actionValues);
    console.log("Action values is empty:", actionValues.isEmpty());

    if (!actionValues.isEmpty()) {
        var firstActionValue = actionValues.get(0);
        console.log("First action value:", firstActionValue);

        var isLast = isLastAction(firstActionValue);
        console.log("Is last action:", isLast);

        if (!isLast) {
            console.log("Not last action, setting handled to true");
            errorEventObj.handled = true;
            console.log("Error event handled set to:", errorEventObj.handled);

            traverseDocumentForFailures(rootDoc);
            console.log("traverseDocumentForFailures completed");
        }
    }

    routeTask(errorEventObj.rootDocument);
    console.log("routeTask completed in onError");
    console.log("=== onError ENDED ===");
}

function routeTask(rootDocument) {
    console.log("");
    console.log("=== routeTask STARTED ===");
    console.log("Root document:", rootDocument);

    var args = extractArguments(rootDocument);
    console.log("Extracted arguments:", args);

    var previousAction = markPreviousActionAsCompleted(rootDocument);
    console.log("Previous action:", previousAction);

    var terminateOnFailure = getTerminateOnFailure(previousAction);
    console.log("Terminate on failure:", terminateOnFailure);

    console.log("Starting ACTIONS loop, total actions:", ACTIONS.length);
    for (var index = 0; index < ACTIONS.length; index++) {
        console.log("Processing action at index:", index);
        var action = ACTIONS[index];
        console.log("Current action:", action);
        console.log("Action name:", action.name);

        var actionCompleted = isActionCompleted(rootDocument, action.name);
        console.log("Action completed:", actionCompleted);

        if (!actionCompleted) {
            console.log("Action not completed, checking conditions");

            var hasConditionFunction = !!action.conditionFunction;
            console.log("Has condition function:", hasConditionFunction);

            var shouldExecute = true;
            if (hasConditionFunction) {
                var documentMatches = anyDocumentMatches(action.conditionFunction, rootDocument, args);
                console.log("Any document matches condition:", documentMatches);
                shouldExecute = documentMatches;
            }
            console.log("Should execute action:", shouldExecute);

            if (shouldExecute) {
                console.log("Executing action:", action.name);

                var actionDetails = {
                    queueName: action.queueName,
                    scripts: action.scripts,
                    customData: evalCustomData(args, action.customData)
                };
                console.log("Action details created:", actionDetails);

                var workflowActionField = rootDocument.getField('CAF_WORKFLOW_ACTION');
                console.log("CAF_WORKFLOW_ACTION field:", workflowActionField);

                workflowActionField.add(action.name);
                console.log("Added action name to CAF_WORKFLOW_ACTION:", action.name);

                applyActionDetails(rootDocument, actionDetails, terminateOnFailure);
                console.log("applyActionDetails completed");

                var applyPrioritization = action.applyMessagePrioritization;
                console.log("Apply message prioritization:", applyPrioritization);

                var wmpEnabled = isCafWmpEnabled();
                console.log("CAF WMP enabled:", wmpEnabled);

                if (applyPrioritization && wmpEnabled) {
                    console.log("Applying message prioritization");

                    var response = rootDocument.getTask().getResponse();
                    console.log("Response object:", response);

                    var originalQueueName = response.getSuccessQueue().getName();
                    console.log("Original queue name:", originalQueueName);

                    var reroutedSuffix = '';
                    console.log("Initial rerouted suffix:", reroutedSuffix);

                    var tenantId = rootDocument.getCustomData("tenantId");
                    console.log("Tenant ID from custom data:", tenantId);

                    if(tenantId !== null && tenantId !== '') {
                        reroutedSuffix += "/" + tenantId;
                        console.log("Added tenant ID to suffix, new suffix:", reroutedSuffix);
                    }

                    var workflowName;
                    var fieldWorkflowName = rootDocument.getField("CAF_WORKFLOW_NAME");
                    console.log("CAF_WORKFLOW_NAME field:", fieldWorkflowName);
                    console.log("Field has values:", fieldWorkflowName !== null && fieldWorkflowName.hasValues());

                    if(fieldWorkflowName !== null && fieldWorkflowName.hasValues()) {
                        workflowName = fieldWorkflowName.getStringValues().get(0);
                        console.log("Workflow name from field:", workflowName);
                    } else {
                        workflowName = rootDocument.getCustomData("workflowName");
                        console.log("Workflow name from custom data:", workflowName);
                    }
                    console.log("Final workflow name:", workflowName);

                    if(workflowName !== null && workflowName !== '') {
                        reroutedSuffix += "/" + workflowName;
                        console.log("Added workflow name to suffix, final suffix:", reroutedSuffix);
                    }

                    if(reroutedSuffix !== '') {
                        var newQueueName = originalQueueName + "»" + reroutedSuffix;
                        console.log("New queue name:", newQueueName);

                        response.getSuccessQueue().set(newQueueName);
                        console.log("Set success queue to:", newQueueName);
                    }
                }
                console.log("Breaking out of actions loop");
                break;
            } else {
                console.log("Skipping action due to condition not met");
            }
        } else {
            console.log("Skipping completed action:", action.name);
        }
    }
    console.log("=== routeTask ENDED ===");
}

function getTerminateOnFailure(previousAction) {
    console.log("");
    console.log("=== getTerminateOnFailure STARTED ===");
    console.log("Previous action:", previousAction);

    if (previousAction) {
        console.log("Finding previous action in ACTIONS array");
        var actionNames = ACTIONS.map(function (a) {
            console.log("Action in map:", a.name);
            return a.name;
        });
        console.log("All action names:", actionNames);

        var previousIndex = actionNames.indexOf(previousAction);
        console.log("Previous action index:", previousIndex);

        if (previousIndex >= 0) {
            var terminateOnFailure = ACTIONS[previousIndex].terminateOnFailure;
            console.log("Terminate on failure from previous action:", terminateOnFailure);
            console.log("=== getTerminateOnFailure ENDED ===");
            return terminateOnFailure;
        }
    }
    console.log("No previous action or not found, returning false");
    console.log("=== getTerminateOnFailure ENDED ===");
    return false;
}

function extractArguments(document) {
    console.log("");
    console.log("=== extractArguments STARTED ===");
    console.log("Document:", document);

    var rootDocument = document.getRootDocument();
    console.log("Root document:", rootDocument);

    var workflowSettingsField = rootDocument.getField("CAF_WORKFLOW_SETTINGS");
    console.log("CAF_WORKFLOW_SETTINGS field:", workflowSettingsField);

    var stringValues = workflowSettingsField.getStringValues();
    console.log("String values:", stringValues);

    var stream = stringValues.stream();
    console.log("String values stream:", stream);

    try {
        var argumentsJson = stream.findFirst().orElseThrow(function () {
            console.log("ERROR: No CAF_WORKFLOW_SETTINGS found");
            throw new UnsupportedOperationException("Document must contain field CAF_WORKFLOW_SETTINGS.");
        });
        console.log("Arguments JSON string:", argumentsJson);
    } catch (error) {
        console.log("Error getting arguments JSON:", error);
        throw error;
    }

    if (argumentsJson === undefined) {
        console.log("ERROR: Arguments JSON is undefined");
        throw new UnsupportedOperationException("Document must contain field CAF_WORKFLOW_SETTINGS.");
    }

    var parsedArgs = JSON.parse(argumentsJson);
    console.log("Parsed arguments:", parsedArgs);
    console.log("=== extractArguments ENDED ===");
    return parsedArgs;
}

function extractFailureSubfields(document) {
    console.log("");
    console.log("=== extractFailureSubfields STARTED ===");
    console.log("Document:", document);

    var rootDocument = document.getRootDocument();
    console.log("Root document:", rootDocument);

    var failureSubfieldsField = rootDocument.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS");
    console.log("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS field:", failureSubfieldsField);

    var stringValues = failureSubfieldsField.getStringValues();
    console.log("Failure subfields string values:", stringValues);

    var stream = stringValues.stream();
    console.log("String values stream:", stream);

    var failureSubfieldsJson = stream.findFirst().orElse("{}");
    console.log("Failure subfields JSON (or default):", failureSubfieldsJson);

    var parsedSubfields = JSON.parse(failureSubfieldsJson);
    console.log("Parsed failure subfields:", parsedSubfields);
    console.log("=== extractFailureSubfields ENDED ===");
    return parsedSubfields;
}

function anyDocumentMatches(conditionFunction, document, args) {
    console.log("");
    console.log("=== anyDocumentMatches STARTED ===");
    console.log("Condition function:", conditionFunction);
    console.log("Document:", document);
    console.log("Arguments:", args);

    //Test the condition string defines a function called 'condition'
    var conditionRegex = /function\s+condition\s*\(/;
    console.log("Condition regex:", conditionRegex);

    var regexMatch = conditionFunction.match(conditionRegex);
    console.log("Regex match result:", regexMatch);

    if(!regexMatch) {
        console.log("Condition function doesn't match expected pattern, returning false");
        return false; //Should this be an exception?
    }

    console.log("Evaluating condition function");
    eval(conditionFunction);
    console.log("Condition function evaluated");

    var conditionResult = condition(document, args);
    console.log("Condition result for current document:", conditionResult);

    if (conditionResult) {
        console.log("Condition matched for current document, returning true");
        console.log("=== anyDocumentMatches ENDED ===");
        return true;
    }

    console.log("Checking subdocuments...");
    var subdocuments = document.getSubdocuments();
    console.log("Subdocuments:", subdocuments);

    var stream = subdocuments.stream();
    console.log("Subdocuments stream:", stream);

    var anyMatch = stream.anyMatch(function (d) {
        console.log("Checking subdocument:", d);
        var subResult = anyDocumentMatches(conditionFunction, d, args);
        console.log("Subdocument match result:", subResult);
        return subResult;
    });
    console.log("Any subdocument matches:", anyMatch);
    console.log("=== anyDocumentMatches ENDED ===");
    return anyMatch;
}

function evalCustomData(args, customDataToEval) {
    console.log("");
    console.log("=== evalCustomData STARTED ===");
    console.log("Arguments:", args);
    console.log("Custom data to eval:", customDataToEval);

    var regex = /".*"|'.*'/g;
    console.log("String regex:", regex);

    var customData = {};
    console.log("Initialized custom data object:", customData);

    if (!customDataToEval) {
        console.log("No custom data to evaluate, returning empty object");
        console.log("=== evalCustomData ENDED ===");
        return customData;
    }

    for(var customDataField in customDataToEval) {
        console.log("Processing custom data field:", customDataField);

        var cd = customDataToEval[customDataField];
        console.log("Custom data value:", cd);
        console.log("Custom data type:", typeof cd);

        if (typeof cd === 'string') {
            console.log("Custom data is string, checking regex match");

            var regexMatch = cd.match(regex);
            console.log("Regex match result:", regexMatch);

            if (regexMatch) {
                console.log("String matches regex, evaluating as literal");
                var evalResult = eval(cd);
                console.log("Eval result:", evalResult);
                customData[customDataField] = evalResult;
            } else {
                console.log("String doesn't match regex, getting from args");
                var argValue = args[cd];
                console.log("Argument value:", argValue);
                customData[customDataField] = argValue;
            }
        } else {
            console.log("Custom data is not string, skipping");
        }
        console.log("Final custom data field value:", customData[customDataField]);
    }
    console.log("Final custom data object:", customData);
    console.log("=== evalCustomData ENDED ===");
    return customData;
}

function markPreviousActionAsCompleted(document) {
    console.log("");
    console.log("=== markPreviousActionAsCompleted STARTED ===");
    console.log("Document:", document);

    // Does the CAF_WORKFLOW_ACTION contain the id of action that has been completed.
    var workflowActionField = document.getField('CAF_WORKFLOW_ACTION');
    console.log("CAF_WORKFLOW_ACTION field:", workflowActionField);
    console.log("Field has values:", workflowActionField.hasValues());

    if (!workflowActionField.hasValues()) {
        console.log("No workflow action values, returning undefined");
        console.log("=== markPreviousActionAsCompleted ENDED ===");
        return;
    }

    var stringValues = workflowActionField.getStringValues();
    console.log("Workflow action string values:", stringValues);

    var previousActionId = stringValues.get(0);
    console.log("Previous action ID:", previousActionId);

    var completedActionsField = document.getField('CAF_WORKFLOW_ACTIONS_COMPLETED');
    console.log("CAF_WORKFLOW_ACTIONS_COMPLETED field:", completedActionsField);

    completedActionsField.add(previousActionId);
    console.log("Added previous action to completed actions:", previousActionId);

    workflowActionField.clear();
    console.log("Cleared CAF_WORKFLOW_ACTION field");

    console.log("=== markPreviousActionAsCompleted ENDED ===");
    return previousActionId;
}

function isActionCompleted(document, actionId) {
    console.log("");
    console.log("=== isActionCompleted STARTED ===");
    console.log("Document:", document);
    console.log("Action ID to check:", actionId);

    var completedActionsField = document.getField('CAF_WORKFLOW_ACTIONS_COMPLETED');
    console.log("CAF_WORKFLOW_ACTIONS_COMPLETED field:", completedActionsField);

    var stringValues = completedActionsField.getStringValues();
    console.log("Completed actions string values:", stringValues);

    var isCompleted = stringValues.contains(actionId);
    console.log("Action is completed:", isCompleted);
    console.log("=== isActionCompleted ENDED ===");
    return isCompleted;
}

function applyActionDetails(document, actionDetails, terminateOnFailure) {
    console.log("");
    console.log("=== applyActionDetails STARTED ===");
    console.log("Document:", document);
    console.log("Action details:", actionDetails);
    console.log("Terminate on failure:", terminateOnFailure);

    // Propagate the custom data if it exists
    var responseCustomData = actionDetails.customData ? actionDetails.customData : {};
    console.log("Response custom data:", responseCustomData);

    // Update document destination queue to that specified by action and pass appropriate settings and customData
    var queueToSet = actionDetails.queueName;
    console.log("Queue to set:", queueToSet);

    var response = document.getTask().getResponse();
    console.log("Task response:", response);

    var successQueue = response.getSuccessQueue();
    console.log("Success queue:", successQueue);

    successQueue.set(queueToSet);
    console.log("Set success queue to:", queueToSet);

    var failureQueue = response.getFailureQueue();
    console.log("Current failure queue:", failureQueue);
    if (!terminateOnFailure) {
        console.log("Not terminating on failure, setting failure queue");

        failureQueue.set(queueToSet);
        console.log("Set failure queue to:", queueToSet);
    } else {
        console.log("Terminating on failure, not changing failure queue");
    }

    var responseCustomDataMap = response.getCustomData();
    console.log("Response custom data map:", responseCustomDataMap);

    responseCustomDataMap.putAll(responseCustomData);
    console.log("Added custom data to response");

    // Add any scripts specified on the action
    var scripts = actionDetails.scripts;
    console.log("Action scripts:", scripts);
    console.log("Scripts exist:", !!(scripts && scripts.length !== 0));

    if (scripts && scripts.length !== 0) {
        console.log("Processing", scripts.length, "scripts");

        for (var scriptToAdd of scripts) {
            console.log("Processing script:", scriptToAdd);
            console.log("Script name:", scriptToAdd.name);

            var taskScripts = document.getTask().getScripts();
            console.log("Task scripts collection:", taskScripts);

            var scriptObjectAdded = taskScripts.add();
            console.log("Added new script object:", scriptObjectAdded);

            scriptObjectAdded.setName(scriptToAdd.name);
            console.log("Set script name to:", scriptToAdd.name);

            const strScriptEngine = scriptToAdd.engine;
            console.log("Script engine string:", strScriptEngine);

            if (!strScriptEngine) {
                console.log("ERROR: Script engine not specified");
                throw new RuntimeException("Invalid script definition on action. Script engine not specified.");
            }

            const scriptEngine = ScriptEngineType.valueOf(strScriptEngine);
            console.log("Script engine type:", scriptEngine);

            console.log("Checking script source type...");
            console.log("Has script inline:", scriptToAdd.script !== undefined);
            console.log("Has storage ref:", scriptToAdd.storageRef !== undefined);
            console.log("Has URL:", scriptToAdd.url !== undefined);

            if (scriptToAdd.script !== undefined) {
                console.log("Setting script inline:", scriptToAdd.script);
                scriptObjectAdded.setScriptInline(scriptToAdd.script, scriptEngine);
                console.log("Script set inline");
            } else if (scriptToAdd.storageRef !== undefined) {
                console.log("Setting script by reference:", scriptToAdd.storageRef);
                scriptObjectAdded.setScriptByReference(scriptToAdd.storageRef, scriptEngine);
                console.log("Script set by reference");
            } else if (scriptToAdd.url !== undefined) {
                console.log("Setting script by URL:", scriptToAdd.url);

                var urlObject = new URL(scriptToAdd.url);
                console.log("URL object:", urlObject);

                scriptObjectAdded.setScriptByUrl(urlObject, scriptEngine);
                console.log("Script set by URL");
            } else {
                console.log("ERROR: No valid script value source");
                throw new RuntimeException("Invalid script definition on action. No valid script value source.");
            }

            scriptObjectAdded.install();
            console.log("Script installed");
        }
        console.log("All scripts processed");
    } else {
        console.log("No scripts to process");
    }
    console.log("=== applyActionDetails ENDED ===");
}

function isCafWmpEnabled() {
    console.log("");
    console.log("=== isCafWmpEnabled STARTED ===");

    var cafWmpEnabledString = System.getenv("CAF_WMP_ENABLED");
    console.log("CAF_WMP_ENABLED environment variable:", cafWmpEnabledString);

    var isEnabled = cafWmpEnabledString !== null && cafWmpEnabledString.toLowerCase() === "true";
    console.log("CAF WMP enabled result:", isEnabled);
    console.log("=== isCafWmpEnabled ENDED ===");
    return isEnabled;
}

function onAfterProcessDocument(e) {
    console.log("");
    console.log("=== onAfterProcessDocument STARTED ===");
    console.log("Event object:", e);
    console.log("Root document:", e.rootDocument);
    console.log("Document:", e.document);
    console.log("Application:", e.application);

    var workflowActionExists = fieldExists(e.rootDocument, "CAF_WORKFLOW_ACTION");
    console.log("CAF_WORKFLOW_ACTION field exists:", workflowActionExists);

    if (workflowActionExists) {
        var workflowActionField = e.rootDocument.getField("CAF_WORKFLOW_ACTION");
        console.log("CAF_WORKFLOW_ACTION field:", workflowActionField);

        var stringValues = workflowActionField.getStringValues();
        console.log("Workflow action string values:", stringValues);

        var currentAction = stringValues.get(0);
        console.log("Current action:", currentAction);

        var terminateOnFailure = getTerminateOnFailure(currentAction);
        console.log("Terminate on failure:", terminateOnFailure);

        var isLast = isLastAction(currentAction);
        console.log("Is last action:", isLast);

        var shouldProcessFailures = !terminateOnFailure && !isLast;
        console.log("Should process failures:", shouldProcessFailures);

        if (shouldProcessFailures) {
            console.log("Processing failures...");

            var processSubdocsSeparately = e.application.getInputMessageProcessor().getProcessSubdocumentsSeparately();
            console.log("Process subdocuments separately:", processSubdocsSeparately);

            if (!processSubdocsSeparately) {
                console.log("Traversing document tree for failures");
                traverseDocumentForFailures(e.document);
                console.log("traverseDocumentForFailures completed");
            } else {
                console.log("Processing failures for single document");
                processFailures(e.document);
                console.log("processFailures completed");
            }
        } else {
            console.log("Not processing failures due to conditions");
        }
    } else {
        console.log("CAF_WORKFLOW_ACTION field doesn't exist");
    }
    console.log("=== onAfterProcessDocument ENDED ===");
}

function traverseDocumentForFailures(document) {
    console.log("");
    console.log("=== traverseDocumentForFailures STARTED ===");
    console.log("Document:", document);

    processFailures(document);
    console.log("processFailures completed for current document");

    var subdocuments = document.getSubdocuments();
    console.log("Subdocuments:", subdocuments);
    console.log("Number of subdocuments:", subdocuments.size());

    subdocuments.forEach(function (subdoc) {
        console.log("Processing failures for subdocument:", subdoc);
        traverseDocumentForFailures(subdoc);
        console.log("Completed processing failures for subdocument");
    });
    console.log("=== traverseDocumentForFailures ENDED ===");
}

function processFailures(document) {
    console.log("");
    console.log("=== processFailures STARTED ===");
    console.log("Document:", document);

    var failures = document.getFailures();
    console.log("Document failures:", failures);
    console.log("Failures changed:", failures.isChanged());

    if (failures.isChanged()) {
        console.log("Failures have changed, processing...");

        var listOfFailures = new ArrayList();
        console.log("Created new ArrayList for failures:", listOfFailures);

        var failuresStream = failures.stream();
        console.log("Failures stream:", failuresStream);

        failuresStream.forEach(function (failure) {
            console.log("Adding failure to list:", failure);
            console.log("Failure ID:", failure.getFailureId());
            console.log("Failure message:", failure.getFailureMessage());
            listOfFailures.add(failure);
        });
        console.log("All failures added to list, total:", listOfFailures.size());

        failures.reset();
        console.log("Failures reset");

        var listOfOriginalFailures = new ArrayList();
        console.log("Created new ArrayList for original failures:", listOfOriginalFailures);

        var originalFailuresStream = failures.stream();
        console.log("Original failures stream:", originalFailuresStream);

        originalFailuresStream.forEach(function (failure) {
            console.log("Adding original failure to list:", failure);
            listOfOriginalFailures.add(failure);
        });
        console.log("All original failures added to list, total:", listOfOriginalFailures.size());

        var newFailures = new ArrayList();
        console.log("Created new ArrayList for new failures:", newFailures);

        var newFailuresStream = listOfFailures.stream();
        console.log("New failures stream:", newFailuresStream);

        newFailuresStream.forEach(function(failure) {
            console.log("Checking if failure is in original list:", failure);

            var isInOriginal = isFailureInOriginal(listOfOriginalFailures, failure);
            console.log("Failure is in original:", isInOriginal);

            if(!isInOriginal) {
                console.log("Adding new failure:", failure);
                newFailures.add(failure);
            }
        });
        console.log("New failures identified, total:", newFailures.size());

        console.log("Evaluating thisScriptObject");
        eval(thisScriptObject);
        console.log("thisScriptObject evaluated");

        addFailures(document, newFailures);
        console.log("addFailures completed");
    } else {
        console.log("No failures changed, skipping processing");
    }
    console.log("=== processFailures ENDED ===");
}

function isFailureInOriginal(listOfOriginalFailures, newFailure) {
    console.log("");
    console.log("=== isFailureInOriginal STARTED ===");
    console.log("Original failures list size:", listOfOriginalFailures.size());
    console.log("New failure:", newFailure);
    console.log("New failure ID:", newFailure.getFailureId());
    console.log("New failure message:", newFailure.getFailureMessage());
    console.log("New failure stack:", newFailure.getFailureStack());

    for (var failure of listOfOriginalFailures) {
        console.log("Comparing with original failure:", failure);
        console.log("Original failure ID:", failure.getFailureId());
        console.log("Original failure message:", failure.getFailureMessage());
        console.log("Original failure stack:", failure.getFailureStack());

        var idMatch = newFailure.getFailureId() === failure.getFailureId();
        console.log("ID match:", idMatch);

        var messageMatch = newFailure.getFailureMessage() === failure.getFailureMessage();
        console.log("Message match:", messageMatch);

        var stackMatch = newFailure.getFailureStack() === failure.getFailureStack();
        console.log("Stack match:", stackMatch);

        if (idMatch && messageMatch && stackMatch) {
            console.log("Failure found in original list, returning true");
            console.log("=== isFailureInOriginal ENDED ===");
            return true;
        }
    }
    console.log("Failure not found in original list, returning false");
    console.log("=== isFailureInOriginal ENDED ===");
    return false;
}

function isLastAction(action) {
    console.log("");
    console.log("=== isLastAction STARTED ===");
    console.log("Action to check:", action);
    console.log("Total actions:", ACTIONS.length);

    if (ACTIONS.length > 0) {
        var lastAction = ACTIONS[ACTIONS.length - 1];
        console.log("Last action in array:", lastAction);
        console.log("Last action name:", lastAction.name);

        var isLast = lastAction.name === action;
        console.log("Is last action result:", isLast);
        console.log("=== isLastAction ENDED ===");
        return isLast;
    }
    console.log("No actions in array, returning false");
    console.log("=== isLastAction ENDED ===");
    return false;
}

function getCurrentWorkerName(document) {
    console.log("");
    console.log("=== getCurrentWorkerName STARTED ===");
    console.log("Document:", document);

    var application = document.getApplication();
    console.log("Application:", application);

    var applicationName = application.getName();
    console.log("Application name:", applicationName);

    if (applicationName) {
        console.log("Using application name as worker name:", applicationName);
        console.log("=== getCurrentWorkerName ENDED ===");
        return applicationName;
    }

    console.log("No application name, getting from configuration...");
    var configService = application.getService(com.github.cafapi.common.api.ConfigurationSource.class);
    console.log("Configuration service:", configService);

    var config = configService.getConfiguration(com.github.cafdataprocessing.workers.document.config.DocumentWorkerConfiguration.class);
    console.log("Document worker configuration:", config);

    var workerName = config.getWorkerName();
    console.log("Worker name from config:", workerName);
    console.log("=== getCurrentWorkerName ENDED ===");
    return workerName;
}

function getCurrentWorkerVersion(document) {
    console.log("");
    console.log("=== getCurrentWorkerVersion STARTED ===");
    console.log("Document:", document);

    var application = document.getApplication();
    console.log("Application:", application);

    var applicationVersion = application.getVersion();
    console.log("Application version:", applicationVersion);

    if (applicationVersion) {
        console.log("Using application version as worker version:", applicationVersion);
        console.log("=== getCurrentWorkerVersion ENDED ===");
        return applicationVersion;
    }

    console.log("No application version, getting from configuration...");
    var configService = application.getService(com.github.cafapi.common.api.ConfigurationSource.class);
    console.log("Configuration service:", configService);

    var config = configService.getConfiguration(com.github.cafdataprocessing.workers.document.config.DocumentWorkerConfiguration.class);
    console.log("Document worker configuration:", config);

    var workerVersion = config.getWorkerVersion();
    console.log("Worker version from config:", workerVersion);
    console.log("=== getCurrentWorkerVersion ENDED ===");
    return workerVersion;
}

//Field Conditions

function fieldExists(document, fieldName) {
    console.log("");
    console.log("=== fieldExists STARTED ===");
    console.log("Document:", document);
    console.log("Field name:", fieldName);

    var field = document.getField(fieldName);
    console.log("Field:", field);

    var hasValues = field.hasValues();
    console.log("Field has values:", hasValues);
    console.log("=== fieldExists ENDED ===");
    return hasValues;
}

function isEmptyMap(mapValue) {
    console.log("");
    console.log("=== isEmptyMap STARTED ===");
    console.log("Map value:", mapValue);

    if(!mapValue) {
        console.log("Map value is falsy, returning true");
        console.log("=== isEmptyMap ENDED ===");
        return true;
    }

    var jsonString = mapValue.replace(/\s/g, '');
    console.log("JSON string without spaces:", jsonString);

    var isEmpty = jsonString === '{}';
    console.log("Is empty map:", isEmpty);
    console.log("=== isEmptyMap ENDED ===");
    return isEmpty;
}

function isFieldValueEqualToValue(fieldValue, value) {
    console.log("");
    console.log("=== isFieldValueEqualToValue STARTED ===");
    console.log("Field value:", fieldValue);
    console.log("Value to compare:", value);
    console.log("Field value is string:", fieldValue.isStringValue());

    if (fieldValue.isStringValue()) {
        var stringValue = fieldValue.getStringValue();
        console.log("Field string value:", stringValue);

        var isEqual = stringValue === value;
        console.log("Values are equal:", isEqual);
        console.log("=== isFieldValueEqualToValue ENDED ===");
        return isEqual;
    }
    console.log("Field value is not string, returning false");
    console.log("=== isFieldValueEqualToValue ENDED ===");
    return false;
}

function fieldHasStringValue(document, fieldName, value) {
    console.log("");
    console.log("=== fieldHasStringValue STARTED ===");
    console.log("Document:", document);
    console.log("Field name:", fieldName);
    console.log("Value to find:", value);

    var field = document.getField(fieldName);
    console.log("Field:", field);

    var fieldValues = field.getValues();
    console.log("Field values:", fieldValues);
    console.log("Number of values:", fieldValues.size());

    for(const fieldValue of fieldValues) {
        console.log("Checking field value:", fieldValue);

        var isEqual = isFieldValueEqualToValue(fieldValue, value);
        console.log("Field value equals target:", isEqual);

        if (isEqual) {
            console.log("Found matching value, returning true");
            console.log("=== fieldHasStringValue ENDED ===");
            return true;
        }
    }
    console.log("No matching value found, returning false");
    console.log("=== fieldHasStringValue ENDED ===");
    return false;
}

function fieldHasAnyStringValue(document, fieldName, values) {
    console.log("");
    console.log("=== fieldHasAnyStringValue STARTED ===");
    console.log("Document:", document);
    console.log("Field name:", fieldName);
    console.log("Values to find:", values);

    var field = document.getField(fieldName);
    console.log("Field:", field);

    var fieldValues = field.getValues();
    console.log("Field values:", fieldValues);
    console.log("Number of field values:", fieldValues.size());

    for(const fieldValue of fieldValues) {
        console.log("Checking field value:", fieldValue);

        for (const value of values) {
            console.log("Comparing with target value:", value);

            var isEqual = isFieldValueEqualToValue(fieldValue, value);
            console.log("Values are equal:", isEqual);

            if(isEqual) {
                console.log("Found matching value, returning true");
                console.log("=== fieldHasAnyStringValue ENDED ===");
                return true;
            }
        }
    }
    console.log("No matching values found, returning false");
    console.log("=== fieldHasAnyStringValue ENDED ===");
    return false;
}

console.log("=== WORKFLOW SCRIPT LOADED SUCCESSFULLY ===");
