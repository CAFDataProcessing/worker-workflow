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
/* global Java, thisScript */
var UnsupportedOperationException = Java.type("java.lang.UnsupportedOperationException");
var RuntimeException = Java.type("java.lang.RuntimeException");
var ArrayList = Java.type("java.util.ArrayList");
var URL = Java.type("java.net.URL");
var MDC = Java.type("org.slf4j.MDC");
var UUID = Java.type("java.util.UUID");
var ScriptEngineType = Java.type("com.hpe.caf.worker.document.model.ScriptEngineType");

if(!ACTIONS){
    throw new UnsupportedOperationException ("Workflow script must define an ACTIONS object.");
}

function onProcessTask(e) {
    addMdcLoggingData(e);
    thisScript.install();
}

function addMdcLoggingData(e) {
    // The logging pattern we use uses a tenantId and a correlationId:
    // 
    // https://github.com/CAFapi/caf-logging/tree/v1.0.0#pattern
    // https://github.com/CAFapi/caf-logging/blob/v1.0.0/src/main/resources/logback.xml#L27
    //
    // This function adds a tenantId and correlationID to the MDC (http://logback.qos.ch/manual/mdc.html), so that log messages 
    // from workers in the workflow will contain these values.
    //
    // See also addMdcData in WorkflowWorker, which performs similar logic to ensure log messages from the workflow-worker itself also 
    // contain these values. 
    
    // Get MDC data from custom data.
    var tenantId = e.task.getCustomData("tenantId");
    var correlationId = e.task.getCustomData("correlationId");
    
    // Generate a random correlationId if it doesn't yet exist.
    if (!correlationId) {
        correlationId = UUID.randomUUID().toString();  
    }

    // Only if this worker is NOT a bulk worker; add tenantId and correlationId to the MDC.
    if (!isBulkWorker(e)) {  
        if (tenantId) {
            MDC.put("tenantId", tenantId);
        }
        MDC.put("correlationId", correlationId);
    }
    
    // Add MDC data to custom data so that its passed it onto the next worker.
    e.task.getResponse().getCustomData().put("tenantId", tenantId);
    e.task.getResponse().getCustomData().put("correlationId", correlationId);
}

function isBulkWorker(e) {
    var workflowActionField = e.rootDocument.getField("CAF_WORKFLOW_ACTION");
    if (!workflowActionField.hasValues()) {
        throw new UnsupportedOperationException("Document must contain field CAF_WORKFLOW_ACTION.");
    }
    return workflowActionField.getStringValues().get(0).indexOf("bulk") !== -1;
}

function onAfterProcessTask(eventObj) {
    routeTask(eventObj.rootDocument);
    removeMdcLoggingData();
}

function removeMdcLoggingData() {
    MDC.remove("tenantId");
    MDC.remove("correlationId");
}

function onBeforeProcessDocument(e) {
    //Get the action from ACTIONS, use the value of CAF_WORKFLOW_ACTION to know the name of the action
    if(!e.rootDocument.getField("CAF_WORKFLOW_ACTION").hasValues())
        throw new UnsupportedOperationException("Document must contain field CAF_WORKFLOW_ACTION.");
    var index = ACTIONS.map(function (x) {
                return x.name;
            }).indexOf(e.rootDocument.getField("CAF_WORKFLOW_ACTION").getStringValues().get(0));

    var action = ACTIONS[index];
    if (!action.conditionFunction) {
        return;
    }

    var args = extractArguments(e.rootDocument);
    eval(action.conditionFunction);
    e.cancel = ! condition(e.document, args);
}

function onProcessDocument(e) {
    if (e.application.getInputMessageProcessor().getProcessSubdocumentsSeparately()) {
        setWorkerVersion(e.document);
    } else {
        traverseDocumentForSettingWorkerVersion(e.document);
    }
}

function traverseDocumentForSettingWorkerVersion(document) {
    setWorkerVersion(document);
    document.getSubdocuments().forEach(function (subdoc) {
        traverseDocumentForSettingWorkerVersion(subdoc);
    });
}

function setWorkerVersion(document) {
    var versionFieldName = "PROCESSING_" + getCurrentWorkerName(document).toUpperCase() + "_VERSION";
    document.getField(versionFieldName).set(getCurrentWorkerVersion(document));
}

function onError(errorEventObj) {
    thisScript.install();
    var rootDoc = errorEventObj.rootDocument;
    var message = errorEventObj.error.getMessage();
    rootDoc.getFailures().add("UNHANDLED_ERROR", message, errorEventObj.error);
    var actionValues = errorEventObj.rootDocument.getField("CAF_WORKFLOW_ACTION").getStringValues();
    if (!actionValues.isEmpty() && !isLastAction(actionValues.get(0))) {
        errorEventObj.handled = true;
        traverseDocumentForFailures(rootDoc);
    }
    routeTask(errorEventObj.rootDocument);
}

function routeTask(rootDocument) {

    var args = extractArguments(rootDocument);

    var previousAction = markPreviousActionAsCompleted(rootDocument);
    var terminateOnFailure = getTerminateOnFailure(previousAction);	

    for (var index = 0; index < ACTIONS.length; index ++ ) {
        var action = ACTIONS[index];
        if (!isActionCompleted(rootDocument, action.name)) {
            if(!action.conditionFunction || anyDocumentMatches(action.conditionFunction, rootDocument, args)) {
                var actionDetails = {
                    queueName: action.queueName,
                    scripts: action.scripts,
                    customData: evalCustomData(args, action.customData)
                };

                rootDocument.getField('CAF_WORKFLOW_ACTION').add(action.name);
                applyActionDetails(rootDocument, actionDetails, terminateOnFailure);
                break;
            }
        }
    }
}

function getTerminateOnFailure(previousAction)
{
    if (previousAction) {
        var previousIndex = ACTIONS.map(function (a) {
            return a.name;
        }).indexOf(previousAction);
        return	ACTIONS[previousIndex].terminateOnFailure;
    }
    return false;
}

function extractArguments(document){

    var rootDocument = document.getRootDocument();
    var argumentsJson = rootDocument.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst()
            .orElseThrow(function () {
                throw new UnsupportedOperationException
                ("Document must contain field CAF_WORKFLOW_SETTINGS.");
            });

    if (argumentsJson === undefined) {
        throw new UnsupportedOperationException("Document must contain field CAF_WORKFLOW_SETTINGS.");
    }

    return JSON.parse(argumentsJson);
}

function extractFailureSubfields(document) {

    var rootDocument = document.getRootDocument();
    var failureSubfieldsField = rootDocument.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS");
    var failureSubfieldsJson = failureSubfieldsField.getStringValues().stream().findFirst()
            .orElse("{}");
    return JSON.parse(failureSubfieldsJson);
}

function anyDocumentMatches(conditionFunction, document, args){

    //Test the condition string defines a function called 'condition'
    if(! conditionFunction.match(/function\s+condition\s*\(/)) {
        return false; //Should this be an exception?
    }
    eval(conditionFunction);
    if (condition(document, args)) {
        return true;
    }

    return document.getSubdocuments().stream().anyMatch(
        function (d) {
            return anyDocumentMatches(conditionFunction, d, args);
        });
}

function evalCustomData(args, customDataToEval){
    var regex = /".*"|'.*'/g;
    var customData = {};
    if (!customDataToEval) {
        return customData;
    }
    for(var customDataField in customDataToEval){
        var cd = customDataToEval[customDataField];
        if (typeof cd === 'string') {
            if (cd.match(regex)) {
                customData[customDataField] = eval(cd);
            }
            else {
                customData[customDataField] = args[cd];
            }
        }
    }
    return customData;
}

function markPreviousActionAsCompleted(document) {
    // Does the CAF_WORKFLOW_ACTION contain the id of action that has been completed.
    if (!document.getField('CAF_WORKFLOW_ACTION').hasValues()) {
        return;
    }

    var previousActionId = document.getField('CAF_WORKFLOW_ACTION').getStringValues().get(0);
    document.getField('CAF_WORKFLOW_ACTIONS_COMPLETED').add(previousActionId);
    document.getField('CAF_WORKFLOW_ACTION').clear();
    return previousActionId;
}

function isActionCompleted(document, actionId) {
    return document.getField('CAF_WORKFLOW_ACTIONS_COMPLETED').getStringValues().contains(actionId);
}

function applyActionDetails(document, actionDetails, terminateOnFailure) {
    // Propagate the custom data if it exists
    var responseCustomData = actionDetails.customData ? actionDetails.customData : {};
    // Update document destination queue to that specified by action and pass appropriate settings and customData
    var queueToSet = actionDetails.queueName;
    var response = document.getTask().getResponse();
    response.getSuccessQueue().set(queueToSet);
    if (!terminateOnFailure){
        response.getFailureQueue().set(queueToSet);
    }   
    response.getCustomData().putAll(responseCustomData);

    // Add any scripts specified on the action
    if (actionDetails.scripts && actionDetails.scripts.length != 0) {
        for (var scriptToAdd of actionDetails.scripts) {
            var scriptObjectAdded = document.getTask().getScripts().add();
            scriptObjectAdded.setName(scriptToAdd.name);

            var scriptEngine = ScriptEngineType.NASHORN;
            if (scriptToAdd.engine !== undefined) {
                scriptEngine = ScriptEngineType.valueOf(scriptToAdd.engine);
            }

            if (scriptToAdd.script !== undefined) {
                scriptObjectAdded.setScriptInline(scriptToAdd.script, scriptEngine);
            } else if (scriptToAdd.storageRef !== undefined) {
                scriptObjectAdded.setScriptByReference(scriptToAdd.storageRef, scriptEngine);
            } else if (scriptToAdd.url !== undefined) {
                scriptObjectAdded.setScriptByUrl(new URL(scriptToAdd.url), scriptEngine);
            } else {
                throw new RuntimeException("Invalid script definition on action. No valid script value source.");
            }

            scriptObjectAdded.install();
        }
    }
}

function onAfterProcessDocument(e) {
    if (fieldExists(e.rootDocument, "CAF_WORKFLOW_ACTION") &&
            !getTerminateOnFailure(e.rootDocument.getField("CAF_WORKFLOW_ACTION").getStringValues().get(0)) &&
            !isLastAction(e.rootDocument.getField("CAF_WORKFLOW_ACTION").getStringValues().get(0))) {
        if (!e.application.getInputMessageProcessor().getProcessSubdocumentsSeparately()) {
            traverseDocumentForFailures(e.document);
        } else {
            processFailures(e.document);
        }
    }
}

function traverseDocumentForFailures(document) {
    processFailures(document);
    document.getSubdocuments().forEach(function (subdoc){
        traverseDocumentForFailures(subdoc);
    });
}

function processFailures(document) {
    if (document.getFailures().isChanged()) {

        var listOfFailures = new ArrayList();
        document.getFailures().stream().forEach(function (failure) {
            listOfFailures.add(failure);
        });

        document.getFailures().reset();

        var listOfOriginalFailures = new ArrayList();
        document.getFailures().stream().forEach(function (failure) {
            listOfOriginalFailures.add(failure);
        });
        var newFailures = new ArrayList();
        listOfFailures.stream().forEach(function(failure) {
           if(!isFailureInOriginal(listOfOriginalFailures, failure)){
               newFailures.add(failure);
           }
        });
        eval(thisScriptObject);
        addFailures(document, newFailures);
    }
}

function isFailureInOriginal(listOfOriginalFailures, newFailure) {
    for (var failure of listOfOriginalFailures) {
        if (newFailure.getFailureId() === failure.getFailureId()
                && newFailure.getFailureMessage() === failure.getFailureMessage()
                && newFailure.getFailureStack() === failure.getFailureStack()) {
            return true;
        }
    }
    return false;
}


function isLastAction(action) {
    return ACTIONS[ACTIONS.length - 1 ].name === action;
}

function getCurrentWorkerName(document) {
    return document.getApplication().getName()
            || document.getApplication().getService(com.hpe.caf.api.ConfigurationSource.class)
            .getConfiguration(com.hpe.caf.worker.document.config.DocumentWorkerConfiguration.class).getWorkerName();
}

function getCurrentWorkerVersion(document) {
    return document.getApplication().getVersion()
            || document.getApplication().getService(com.hpe.caf.api.ConfigurationSource.class)
            .getConfiguration(com.hpe.caf.worker.document.config.DocumentWorkerConfiguration.class).getWorkerVersion();
}

//Field Conditions

function fieldExists(document, fieldName) {
    return document.getField(fieldName).hasValues();
}

function isEmptyMap(mapValue) {
    if(!mapValue)
        return true;
    var jsonString= mapValue.replace(/\s/g, '');    
    return jsonString === '{}';
}

function isValidGrammarSet(grammarSet) {
    if(typeof grammarSet === 'string') {
        if(grammarSet) {
            return true;
        }
    }
    return false;
}

function isFieldValueEqualToValue(fieldValue, value) {
    if (fieldValue.isStringValue() && fieldValue.getStringValue() === value) {
        return true;
    }
    return false;
}

function fieldHasStringValue(document, fieldName, value) {
    var fieldValues = document.getField(fieldName).getValues();
    for(const fieldValue of fieldValues) {
        return isFieldValueEqualToValue(fieldValue, value);
    }
}

function fieldHasAnyStringValue(document, fieldName, values) {
    var fieldValues = document.getField(fieldName).getValues();

    for(const fieldValue of fieldValues) {
        for (const value of values) {
            if(isFieldValueEqualToValue(fieldValue, value)) {
                return true;
            }
        }
    }
    return false;
}
