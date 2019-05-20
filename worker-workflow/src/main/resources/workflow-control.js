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
    processDocument(eventObj.rootDocument);
}

function onError(errorEventObj) {
    // We will not mark the error as handled here. This will allow the document-worker framework to add the failure
    // itself rather than us duplicating the format of the failure value it constructs for non-script failure responses

    // Even though the action failed it still completed in terms of the document being sent for processing against the
    // action, so the action should be marked as completed
    processDocument(errorEventObj.rootDocument);
}

function processDocument(document) {
    var argumentsCustomData = document.getCustomData("CAF_WORKFLOW_SETTINGS");
    var argumentsField = document.getField("CAF_WORKFLOW_SETTINGS");
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
    var arguments = JSON.parse(argumentsJson);

    markPreviousActionAsCompleted(document);

    for (var index = 0; index < ACTIONS.length; index ++ ) {
        var action = ACTIONS[index];
        if (!isActionCompleted(document, action.name)) {
            if(!action.conditionFunction || eval(action.conditionFunction)(document)) {
                var actionDetails = {
                    queueName: action.queueName,
                    scripts: action.scripts,
                    customData: evalCustomData(arguments, action.customData)
                };

                document.getField('CAF_WORKFLOW_ACTION').add(action.name);
                applyActionDetails(document, actionDetails);
                break;
            }
        }
    }
}

function evalCustomData(arguments, customDataToEval){
    var regex = /d".*"|'.*'/g;
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
}

function isActionCompleted(document, actionId) {
    return document.getField('CAF_WORKFLOW_ACTIONS_COMPLETED').getStringValues().contains(actionId);
}

function applyActionDetails(document, actionDetails) {
    // Propagate the custom data if it exists
    var responseCustomData = actionDetails.customData ? actionDetails.customData : {};
    // Update document destination queue to that specified by action and pass appropriate settings and customData
    var queueToSet = actionDetails.queueName;
    var response = document.getTask().getResponse();
    response.successQueue.set(queueToSet);
    response.failureQueue.set(queueToSet);
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

//Field Conditions

function fieldExists(document, fieldName) {
    if (document.getField(fieldName).hasValues()) {
        return true;
    }
    if (document.hasSubdocuments())
    {
        return document.getSubdocuments().stream().filter(
            function (x) {
                return fieldExists(x, fieldName)
            }).findFirst().isPresent();
    } else
        return false;
}
