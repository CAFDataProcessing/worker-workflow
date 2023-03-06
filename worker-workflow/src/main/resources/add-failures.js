/*
 * Copyright 2017-2023 Micro Focus or one of its affiliates.
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
function addFailures (document, failures, extractSourceCallback, action) {

    function extractFailureSubfields(document) {

        var rootDocument = document.getRootDocument();
        var failureSubfieldsField = rootDocument.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS");
        var failureSubfieldsJson = failureSubfieldsField.getStringValues().stream().findFirst()
            .orElse("{}");
        return JSON.parse(failureSubfieldsJson);
    }

    function getWorkflowAction(document) {
        var actionField = document.getRootDocument().getField("CAF_WORKFLOW_ACTION");
        if (actionField.hasValues()) {
            return actionField.getStringValues().get(0);
        } else {
            return 'UNKNOWN'
        }
    }

    var extraFailureFields = extractFailureSubfields(document);
    var workflowAction = action !== undefined ? action : getWorkflowAction(document);
    failures.stream().forEach(function(f){
        var component = extractSourceCallback !== undefined
            ? extractSourceCallback(f)
            : getCurrentWorkerName(document) + " " + getCurrentWorkerVersion(document);
        var isWarningFlag = (typeof isWarning === 'function') ? isWarning(f): false;
        var errorObject = {
            ID: f.getFailureId(),
            WORKFLOW_ACTION: workflowAction,
            COMPONENT: component,
            WORKFLOW_NAME: document.getRootDocument().getField("CAF_WORKFLOW_NAME").getStringValues().get(0),
            MESSAGE: f.getFailureMessage(),
            DATE: new Date().toISOString(),
            CORRELATION_ID: document.getCustomData("correlationId") || undefined
        };

        if (!isWarningFlag) {
            errorObject["STACK"] = f.getFailureStack() || undefined;
            if (extraFailureFields) {
                for (var key of Object.keys(extraFailureFields)) {
                    errorObject[key] = extraFailureFields[key];
                }
            }
            document.getField("FAILURES").add(JSON.stringify(errorObject));
        } else {
            document.getField("WARNINGS").add(JSON.stringify(errorObject));
        }
    });
}
