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
/* global Java, java, thisScript */

if(!ACTIONS){
    throw new java.lang.UnsupportedOperationException ("Workflow script must define an ACTIONS object.");
}

var URL = Java.type("java.net.URL");

function onProcessTask() {
    thisScript.install();
}

function onAfterProcessTask(eventObj) {
    routeTask(eventObj.rootDocument);
}

function onBeforeProcessDocument(e) {
    //Get the action from ACTIONS, use the value of CAF_WORKFLOW_ACTION to know the name of the action
    if(!e.rootDocument.getField("CAF_WORKFLOW_ACTION").hasValues())
        throw new java.lang.UnsupportedOperationException("Document must contain field CAF_WORKFLOW_ACTION.");
    var index = ACTIONS.map(function (x) {
                return x.name;
            }).indexOf(e.rootDocument.getField("CAF_WORKFLOW_ACTION").getStringValues().get(0));

    var action = ACTIONS[index];
    if (!action.conditionFunction) {
        return;
    }

    var arguments = extractArguments(e.rootDocument);

    e.cancel = ! eval(action.conditionFunction)(e.document, arguments);
}

function onError(errorEventObj) {
    // We will not mark the error as handled here. This will allow the document-worker framework to add the failure
    // itself rather than us duplicating the format of the failure value it constructs for non-script failure responses

    // Even though the action failed it still completed in terms of the document being sent for processing against the
    // action, so the action should be marked as completed
    routeTask(errorEventObj.rootDocument);
}

function routeTask(rootDocument) {

    var arguments = extractArguments(rootDocument);

    var previousAction = markPreviousActionAsCompleted(rootDocument);
    var terminateOnFailure = getTerminateOnFailure(previousAction);	

    for (var index = 0; index < ACTIONS.length; index ++ ) {
        var action = ACTIONS[index];
        if (!isActionCompleted(rootDocument, action.name)) {
            if(!action.conditionFunction || anyDocumentMatches(action.conditionFunction, rootDocument, arguments)) {
                var actionDetails = {
                    queueName: action.queueName,
                    scripts: action.scripts,
                    customData: evalCustomData(arguments, action.customData)
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
    var argumentsCustomData = rootDocument.getCustomData("CAF_WORKFLOW_SETTINGS");
    var argumentsField = rootDocument.getField("CAF_WORKFLOW_SETTINGS");
    var argumentsJson = argumentsCustomData
        ? argumentsCustomData
        : argumentsField.getStringValues().stream().findFirst()
            .orElseThrow(function () {
                throw new java.lang.UnsupportedOperationException
                ("Document must contain field CAF_WORKFLOW_SETTINGS.");
            });

    if (argumentsJson === undefined) {
        throw new java.lang.UnsupportedOperationException("Document must contain field CAF_WORKFLOW_SETTINGS.");
    }

    return JSON.parse(argumentsJson);
}

function anyDocumentMatches(conditionFunction, document, arguments) {

    if (eval(conditionFunction)(document, arguments)){
        return true;
    }

    return document.getSubdocuments().stream().anyMatch(
        function (d) {
            return anyDocumentMatches(conditionFunction, d, arguments);
        });
}

function evalCustomData(arguments, customDataToEval){
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
                customData[customDataField] = arguments[cd];
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
    response.successQueue.set(queueToSet);
    if (!terminateOnFailure){
        response.failureQueue.set(queueToSet);
    }   
    response.customData.putAll(responseCustomData);

    // Add any scripts specified on the action
    if (actionDetails.scripts && actionDetails.scripts.length != 0) {
        for each(var scriptToAdd in actionDetails.scripts) {
            var scriptObjectAdded = document.getTask().getScripts().add();
            scriptObjectAdded.setName(scriptToAdd.name);

            if (scriptToAdd.script !== undefined) {
                scriptObjectAdded.setScriptInline(scriptToAdd.script);
            } else if (scriptToAdd.storageRef !== undefined) {
                scriptObjectAdded.setScriptByReference(scriptToAdd.storageRef);
            } else if (scriptToAdd.url !== undefined) {
                scriptObjectAdded.setScriptByUrl(new URL(scriptToAdd.url));
            } else {
                throw new java.lang.RuntimeException("Invalid script definition on action. No valid script value source.");
            }

            scriptObjectAdded.install();
        }
    }
}

function onAfterProcessDocument(e) {
    if (!e.application.getInputMessageProcessor().getProcessSubdocumentsSeparately()) {
        //print("I am false");
        traverseDocumentForFailures(e.document);
    } else {
        processFailures(e.document);
    }
}

function traverseDocumentForFailures(document) {
    //print("Document reference \t\t\t" + document.getReference());
    if (document.getRootDocument() != null) {
        if (document.getRootDocument() != document) {
            //print("The root document is different from this document \t\t\t" + document.getRootDocument().getReference());
            traverseDocumentForFailures(document.getRootDocument());
        }
    }
    //print("Parent doc: \t\t\t" + document.getParentDocument());
    if (document.getParentDocument() != null) {
        //print("The parent doc is not null: \t\t\t" + document.getParentDocument().getReference());
        traverseDocumentForFailures(document.getParentDocument());
    }
    //print("I am going to process subdocuments");
    processSubdocumentFailures(document.getSubdocuments());
    processFailures(document);
}

function processSubdocumentFailures(subdocuments) {
    for each(var subdoc in subdocuments) {
        if (subdoc.hasSubdocuments()) {
            //print("Subdocument " + subdoc.getReference() + " has subdocs that I am going to process");
            processSubdocumentFailures(subdoc.getSubdocuments());
        } else {
            //print("Subdocument " + subdoc.getReference() + " does not have subdocs; so I am going to process failures");
            processFailures(subdoc);
        }
    }
}

function processFailures(document) {
    //print("I am processing the failures of: \t\t\t" + document.getReference());
    if (document.getFailures().isChanged()) {

        var listOfFailures = new java.util.ArrayList();
        document.getFailures().stream().forEach(function (failure) {
            listOfFailures.add(failure);
        });

        document.getFailures().reset();

        var listOfOriginalFailures = new java.util.ArrayList();
        document.getFailures().stream().forEach(function (failure) {
            listOfOriginalFailures.add(failure);
        });

        for each (var f in listOfFailures) {
            if (!isFailureInOriginal(listOfOriginalFailures, f)) {
                var source = document.getTask().getService(com.hpe.caf.api.worker.WorkerTaskData.class).getSourceInfo().getName();
                var numericVersion = document.getTask().getService(com.hpe.caf.api.worker.WorkerTaskData.class).getSourceInfo().getVersion();
                var message = {
                    ID: f.getFailureId(),
                    STACK: f.getFailureStack() || undefined,
                    WORKFLOW_ACTION: document.rootDocument.getField("CAF_WORKFLOW_ACTION").getStringValues().get(0),
                    VERSION: source.trim() + " " + numericVersion.trim(),
                    WORKFLOW_NAME: document.getField("CAF_WORKFLOW_NAME").getStringValues().get(0),
                    MESSAGE: f.getFailureMessage(),
                    DATE: new Date().toLocaleString('en-US')
                };
                document.getField("FAILURES").add(JSON.stringify(message));
            }
        }
    }
}

function isFailureInOriginal(listOfOriginalFailures, newFailure) {
    for each(var failure in listOfOriginalFailures) {
        if (newFailure.getFailureId() == failure.getFailureId()
                && newFailure.getFailureMessage() == failure.getFailureMessage()
                && newFailure.getFailureStack() == failure.getFailureStack()) {
            return true;
        }
    }
    return false;
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

function fieldHasStringValue(document, fieldName, value) {

    var fieldValues = document.getField(fieldName).getValues();
    if (fieldValues)
    {
        for each(var fieldValue in fieldValues) {
            if (fieldValue.isStringValue() && fieldValue.getStringValue() === value) {
                return true;
            }
        }
    }

    return false;
}
