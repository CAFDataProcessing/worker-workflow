/*
 * Copyright 2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
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
    var cafWorkflowSettingsJson = document.getCustomData("CAF_WORKFLOW_SETTINGS")
        ? document.getCustomData("CAF_WORKFLOW_SETTINGS")
        : document.getField("CAF_WORKFLOW_SETTINGS").getStringValues().stream().findFirst()
            .orElseThrow(function () {
                throw new java.lang.UnsupportedOperationException
                ("Document must contain field CAF_WORKFLOW_SETTINGS.");
            });

    if (cafWorkflowSettingsJson === undefined) {
        throw new java.lang.UnsupportedOperationException("Document must contain field CAF_WORKFLOW_SETTINGS.");
    }
    var settings = JSON.parse(cafWorkflowSettingsJson);

    markPreviousActionAsCompleted(document);

    for (var actionId in ACTIONS) {
        var action = ACTIONS[actionId];
        if (!isActionCompleted(document, actionId)) {
            if(action.conditionFunction && action.conditionFunction(document)) {
                var actionDetails = {
                    queueName: action.queueName,
                    scripts: action.scripts,
                    customData: evalCustomData(settings, action.customData)
                };

                document.getField('CAF_WORKFLOW_ACTION').add(actionId);
                applyActionDetails(document, actionDetails);
                break;
            }
        }
    }
}

function evalCustomData(settings, customDataToEval){
    var customData = {};
    for(var customDataField in customDataToEval){
        customData[customDataField] = eval(customDataToEval[customDataField]);
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

function resolveValue(value, defaultValue) {
    var v = value ? value : defaultValue;
    if (v === undefined) {
        throw new java.lang.RuntimeException("Unable to determine which value to assign to custom data as " +
            "both possibilities are undefined");
    }
    return v;
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
