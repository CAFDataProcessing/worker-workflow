<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:sxl="http://www.w3.org/1999/XSL/Transform" xmlns:xslt="http://www.w3.org/1999/XSL/Transform"
                xmlns:workflow_transform="com.github.cafdataprocessing.workflow.transform">
    <xsl:output method="text" omit-xml-declaration="yes" indent="no"/>
    
    <xsl:param name="projectId"/>
    <xsl:param name="tenantId"/>
    <xsl:param name="apiClient"/>

    <xsl:template match="/workflow">
// Workflow ID: <xsl:value-of select="details/id"/>
// Workflow Name: <xsl:value-of select="details/name"/>

/* global Java, java, thisScript */

var System = Java.type("java.lang.System");
var URL = Java.type("java.net.URL");
var ByteArray = Java.type("byte[]");

// Constants for return values
var ACTION_TO_EXECUTE = 'actionToExecute';
var CONDITIONS_NOT_MET = 'conditionsNotMet';
var ALREADY_EXECUTED = 'alreadyExecuted';

function onProcessTask() {
    thisScript.install();
}

function onAfterProcessTask(eventObj) {
    processDocument(eventObj.rootDocument);
}

function processDocument(document) {
    updateActionStatus(document);
<xsl:for-each select="processingRules/processingRule"><xsl:sort select="details/priority" data-type="number" order="ascending"/>
<xsl:if test="details/enabled = 'true'">
    // Rule Name: <xsl:value-of select="details/name"/>
    var ruleResult = rule_<xsl:value-of select="details/id"/>(document);
    if (ruleResult == ACTION_TO_EXECUTE)
        return;
</xsl:if>
</xsl:for-each>
}
<xsl:for-each select="processingRules/processingRule">
    <xsl:sort select="details/priority" data-type="number" order="ascending"/>
    <xsl:if test="details/enabled = 'true'">
        <xsl:call-template name="ruleFunction"/>
        <xsl:for-each select="actions/action">
            <xsl:sort select="details/order" data-type="number" order="ascending"/>
            <xsl:call-template name="actionFunction"/>
        </xsl:for-each>
    </xsl:if>
</xsl:for-each>

<xsl:call-template name="conditionEvaluationFunctions"/>
<xsl:call-template name="trackingFunctions"/>
<xsl:call-template name="utilityFunctions"/>

<xsl:call-template name="onErrorFunction"/>
    </xsl:template>

    <xsl:template name="ruleFunction">
<xsl:variable name="ruleId" select="details/id"/>
// Rule Name: <xsl:value-of select="details/name"/>
// Return ACTION_TO_EXECUTE if an action in the rule should be executed
function rule_<xsl:value-of select="$ruleId"/>(document) {
    if (isRuleCompleted(document, '<xsl:value-of select="$ruleId"/>')) {
        return ALREADY_EXECUTED;
    }

<xsl:if test="conditions/*">
    if (!<xsl:apply-templates select="conditions/condition"/>) {
        recordRuleCompleted(document, '<xsl:value-of select="$ruleId"/>');
        return CONDITIONS_NOT_MET;
    }

</xsl:if>
<xsl:for-each select="actions/action">
        <xsl:sort select="details/order" data-type="number" order="ascending"/>
    // Action Name: <xsl:value-of select="details/name"/>
    var actionResult = action_<xsl:value-of select="details/id"/>(document);
    if (actionResult == ACTION_TO_EXECUTE)
        return ACTION_TO_EXECUTE;

</xsl:for-each>
    recordRuleCompleted(document, '<xsl:value-of select="$ruleId"/>');
}
</xsl:template>

    <xsl:template name="actionFunction">
<xsl:variable name="actionId" select="details/id"/>
// Action Name: <xsl:value-of select="details/name"/>
// Return CONDITIONS_NOT_MET if the document did not match the action conditions
function action_<xsl:value-of select="$actionId"/>(document) {
    if (isActionCompleted(document, '<xsl:value-of select="$actionId"/>')) {
        return ALREADY_EXECUTED;
    }
<xsl:if test="conditions/*">if (!<xsl:apply-templates select="conditions/condition"/>) return CONDITIONS_NOT_MET;</xsl:if>
    var actionDetails = <xsl:apply-templates select="details/settings"/>;

    recordActionToExecute(document, '<xsl:value-of select="$actionId"/>', actionDetails);
    return evaluateActionDetails(document, actionDetails);
}
</xsl:template>

    <xsl:template match="details/settings[../typeInternalName='DocumentWorkerHandler']"><xsl:call-template name="ChainedActionTypeTemplate"/></xsl:template>

    <xsl:template match="details/settings[../typeInternalName='CompositeDocumentWorkerHandler']"><xsl:call-template name="ChainedActionTypeTemplate"/></xsl:template>

    <xsl:template name="ChainedActionTypeTemplate" match="details/settings[../typeInternalName='ChainedActionType']">{
        internal_name: '<xsl:value-of select="../typeInternalName"/>',
        queueName: '<xsl:choose><xsl:when test="queueName != ''"><xsl:value-of  select="workflow_transform:TransformerFunctions.escapeForJavaScript(queueName)"/></xsl:when><xsl:otherwise><xsl:variable name="workerNameQueueEnvValue" select="workflow_transform:TransformerFunctions.getWorkerQueueFromEnvironment(workerName)"/><xsl:choose><xsl:when test="$workerNameQueueEnvValue != ''"><xsl:value-of select="$workerNameQueueEnvValue"/></xsl:when><xsl:otherwise><xsl:value-of select="workflow_transform:TransformerFunctions.escapeForJavaScript(concat(workerName, 'Input'))"/></xsl:otherwise></xsl:choose></xsl:otherwise></xsl:choose>',
        workerName: '<xsl:value-of select="workflow_transform:TransformerFunctions.escapeForJavaScript(workerName)"/>',
        <xsl:if test="customData/*">customData: {<xsl:apply-templates select="customData"/>},</xsl:if>
        scripts: [
            <xsl:apply-templates select="scripts"/>
        ]
    }</xsl:template>

    <xsl:template match="details/settings[../typeInternalName='FieldMappingPolicyType']"><xsl:call-template name="FieldMappingActionTypeTemplate"/></xsl:template>

    <xsl:template name="FieldMappingActionTypeTemplate" match="details/settings[../typeInternalName='FieldMappingActionType']">function() { executeFieldMapping(document, {
        <xsl:for-each select="mappings/*">
            '<xsl:value-of select="name()"/>' : '<xsl:value-of select="."/>'<xsl:if test="position() != last()">,</xsl:if>
        </xsl:for-each>
    });}</xsl:template>

    <xsl:template match="scripts">
        <xsl:for-each select="*">{
                name: '<xsl:value-of select="workflow_transform:TransformerFunctions.escapeForJavaScript(name)"/>'<xsl:if test="script or storageRef or url">, </xsl:if>
                <xsl:choose>
                    <xsl:when test="script">script: '<xsl:value-of select="workflow_transform:TransformerFunctions.escapeForJavaScript(script)"/>'</xsl:when>
                    <xsl:when test="storageRef">storageRef: '<xsl:value-of select="storageRef"/>'</xsl:when>
                    <xsl:when test="url">url: '<xsl:value-of select="url"/>'</xsl:when>
                </xsl:choose>
            }<xsl:if test="position() != last()">, </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="customData">
        <xsl:for-each select="*"><xsl:variable name="sourceData"><xsl:call-template name="customDataSource"/></xsl:variable>
            <xsl:choose>
                <xsl:when test="$sourceData != ''">'<xsl:value-of select="name(.)"/>': '<xsl:value-of select="$sourceData"/>'<xsl:if test="position() != last()">, </xsl:if></xsl:when>
                <xsl:when test="not(source)">'<xsl:value-of select="name(.)"/>': '<xsl:value-of select="workflow_transform:TransformerFunctions.escapeForJavaScript(text())"/>'<xsl:if test="position() != last()">, </xsl:if></xsl:when>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="customDataSource">
        <xsl:choose><xsl:when test="source = 'inlineJson' and data !=''"><xsl:call-template name="jsonDataSource"><xsl:with-param name="currentProperties" select="data/*"/></xsl:call-template></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="source = 'projectId'"><xsl:value-of select="$projectId"/></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="source = 'tenantId'"><xsl:value-of select="$tenantId"/></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="source = 'tenantData'"><xsl:value-of select="workflow_transform:TransformerFunctions.getTenantSpecificConfigValue($apiClient, $tenantId, key)"/></xsl:when></xsl:choose>
    </xsl:template>

    <xsl:template name="jsonDataSource"><xsl:param name="currentProperties"/>{<xsl:for-each select="$currentProperties">"<xsl:value-of select="name(.)"/>": <xsl:call-template name="jsonPropertyOutput"/><xsl:if test="position() != last()">, </xsl:if></xsl:for-each>}</xsl:template>

    <xsl:template name="jsonPropertyOutput">
        <xsl:choose>
            <xsl:when test="@class='list'">[<xsl:for-each select="*"><xsl:call-template name="jsonPropertyOutput"/><xsl:if test="position() != last()">, </xsl:if></xsl:for-each>]</xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="*">{<xsl:for-each select="*">"<xsl:value-of select="name()"/>":<xsl:call-template name="jsonPropertyOutput"/><xsl:if test="position() != last()">, </xsl:if></xsl:for-each>}</xsl:when>
                    <xsl:when test="@class='int'"><xsl:value-of select="text()"/></xsl:when>
                    <xsl:when test="@class='null'">null</xsl:when>
                    <xsl:when test="@class='boolean'"><xsl:value-of select="text()"/></xsl:when>
                    <xsl:otherwise>"<xsl:value-of select="workflow_transform:TransformerFunctions.escapeForJavaScript(text())"/>"</xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- fall back template outputting details of other action types -->
    <xsl:template priority="-.5" match="details/settings">{
        'internal_name' : '<xsl:value-of select="../typeInternalName"/>',
        'queueName': '<xsl:choose><xsl:when test="queueName != ''"><xsl:value-of select="workflow_transform:TransformerFunctions.escapeForJavaScript(queueName)"/></xsl:when><xsl:otherwise>default-input</xsl:otherwise></xsl:choose>'
        }</xsl:template>

    <xsl:template match="conditions/condition[additional/type='boolean'] | condition[additional/type='boolean'] | children/linked-hash-map[additional/type='boolean'] | children/condition[additional/type='boolean']">
        <xsl:choose>
            <xsl:when test="additional/operator='or'">(<xsl:for-each select="additional/children/linked-hash-map | additional/children/condition"><xsl:apply-templates select="."/><xsl:if test="position() != last()"> || </xsl:if></xsl:for-each>)</xsl:when>
            <xsl:when test="additional/operator='and'">(<xsl:for-each select="additional/children/linked-hash-map | additional/children/condition"><xsl:apply-templates select="."/><xsl:if test="position() != last()"> &amp;&amp; </xsl:if></xsl:for-each>)</xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="conditions/conditions[additional/type='not'] | condition[additional/type='not'] | children/linked-hash-map[additional/type='not'] | children/condition[additional/type='not']">(notCondition(<xsl:apply-templates select="additional/condition"/>))</xsl:template>

    <!-- template used to create JS invoking call to condition with a document and values to test for -->
    <xsl:template priority="-.5" match="conditions/condition | condition | children/linked-hash-map | children/condition">(<xsl:value-of select="additional/type"/>Condition_<xsl:value-of select="additional/operator"/> (document, '<xsl:value-of select="additional/field"/>','<xsl:value-of select="additional/value"/>'))</xsl:template>

    <xsl:template name="conditionEvaluationFunctions">
// Common logic to evaluate each field value on a document against a provided criteria function.
// If the a field with the passed name has not values then false is returned.
function evaluateValuesAgainstCondition(document, fieldName, expectedValue, evaluateFunction) {
    if (!document.getField(fieldName).hasValues()) {
        return false;
    }
    var fieldValues = document.getField(fieldName).getValues();
    for each(var fieldValue in fieldValues) {
        var valueToEvaluate = getDocumentFieldValueAsString(fieldValue);
        if (evaluateFunction(expectedValue, valueToEvaluate) === true) {
            return true;
        }
    }
    return false;
}

function existsCondition_(document, fieldName) {
    return document.getField(fieldName).hasValues();
}

function stringCondition_is(document, fieldName, value) {
    return evaluateValuesAgainstCondition(document, fieldName, value, function (expectedValue, actualValue) {
        if (actualValue.equalsIgnoreCase(expectedValue)) {
            return true;
        }
    });
}

function stringCondition_contains(document, fieldName, value) {
    return evaluateValuesAgainstCondition(document, fieldName, value, function (expectedValue, actualValue) {
        if (actualValue.toUpperCase(java.util.Locale.getDefault())
                .contains(expectedValue.toUpperCase(java.util.Locale.getDefault()))) {
            return true;
        }
    });
}

function stringCondition_starts_with(document, fieldName, value) {
    return evaluateValuesAgainstCondition(document, fieldName, value, function (expectedValue, actualValue) {
        if (actualValue.toUpperCase(java.util.Locale.getDefault())
                .startsWith(expectedValue.toUpperCase(java.util.Locale.getDefault()))) {
            return true;
        }
    });
}

function stringCondition_ends_with(document, fieldName, value) {
    return evaluateValuesAgainstCondition(document, fieldName, value, function (expectedValue, actualValue) {
        if (actualValue.toUpperCase(java.util.Locale.getDefault())
                .endsWith(expectedValue.toUpperCase(java.util.Locale.getDefault()))) {
            return true;
        }
    });
}

function regexCondition_(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Regex is not supported");
}

function dateCondition_before(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Date before is not supported");
}

function dateCondition_after(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Date after is not supported");
}

function dateCondition_on(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Date on is not supported");
}

function numberCondition_gt(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Number greater than is not supported");
}

function numberCondition_lt(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Number less than is not supported");
}

function numberCondition_eq(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Number equal to is not supported");
}

function notCondition(aBoolean) {
    return !aBoolean;
}
</xsl:template>

    <xsl:template name="trackingFunctions">
function isRuleCompleted(document, ruleId) {
    return document.getField('CAF_WORKFLOW_RULES_COMPLETED').getStringValues().contains(ruleId);
}

function isActionCompleted(document, actionId) {
    return document.getField('CAF_WORKFLOW_ACTIONS_COMPLETED').getStringValues().contains(actionId);
}

function recordRuleCompleted(document, ruleId) {
    document.getField('CAF_WORKFLOW_RULES_COMPLETED').add(ruleId);
}

function recordActionCompleted(document, actionId) {
    document.getField('CAF_WORKFLOW_ACTIONS_COMPLETED').add(actionId);
}

function recordActionToExecute(document, actionId) {
    document.getField('CAF_WORKFLOW_ACTION').add(actionId);
}

function updateActionStatus(document) {
    // This may be the first time the document has been presented to the workflow
    if (!document.getField('CAF_WORKFLOW_ACTION').hasValues()) {
        return;
    }
    recordActionCompleted(document, document.getField('CAF_WORKFLOW_ACTION').getStringValues().get(0));
    document.getField('CAF_WORKFLOW_ACTION').clear();
}
</xsl:template>

    <xsl:template name="utilityFunctions">
// Evaluate the determined details of an action, either executing the action against document or preparing the
// document to execute the action
function evaluateActionDetails(document, actionDetails) {
    if (typeof actionDetails === 'function') {
        actionDetails();
        updateActionStatus(document);
        return ALREADY_EXECUTED;
    }

    // Propagate the custom data if it exists
    var responseCustomData = actionDetails.customData ? actionDetails.customData : {};

    // Update document destination queue to that specified by action and pass appropriate settings and customData
    var queueToSet = !isEmpty(actionDetails.queueName) ? actionDetails.queueName : actionDetails.workerName + "Input";
    var response = document.getTask().getResponse();
    response.successQueue.set(queueToSet);
    response.failureQueue.set(queueToSet);
    response.customData.putAll(responseCustomData);

    // Add any scripts specified on the action
    if (actionDetails.scripts.length != 0) {
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
        }
    }

    return ACTION_TO_EXECUTE;
}

// Executes the field mapping action on the document
function executeFieldMapping(document, mappings) {
    // Get the field values to map (from the document)
    var documentFieldsValuesMap = {};
    for (var mappingKey in mappings) {
        var documentFieldToMapFrom = document.getField(mappingKey);
        documentFieldsValuesMap[mappingKey] = documentFieldToMapFrom.getValues();
        documentFieldToMapFrom.clear();
    }

    // For each mapping add the original field value with the new key
    for (var mappingKey in mappings) {
        var mappingDestination = mappings[mappingKey];
        var documentFieldToMapTo = document.getField(mappingDestination);
        for each(var fieldValue in documentFieldsValuesMap[mappingKey]) {
            if (fieldValue.isReference()) {
                documentFieldToMapTo.addReference(fieldValue.getReference());
            } else {
                documentFieldToMapTo.add(fieldValue.getValue());
            }
        }
    }
}

// Returns string representing value of a Document Worker FieldValue
function getDocumentFieldValueAsString(fieldValue) {
    if (!fieldValue.isReference()) {
        return fieldValue.getStringValue();
    }
    var reference = fieldValue.getReference();
    var valueByteArrayStream = new java.io.ByteArrayOutputStream();
    var valueToReturn;
    var valueDataStoreStream;
    try {
        valueDataStoreStream = fieldValue.openInputStream();
        var valueBuffer = new ByteArray(1024);
        var valuePortionLength;
        while ((valuePortionLength = valueDataStoreStream.read(valueBuffer)) != -1) {
            valueByteArrayStream.write(valueBuffer, 0, valuePortionLength);
        }
        valueToReturn = valueByteArrayStream.toString("UTF-8");
    } catch (e) {
        throw new java.lang.RuntimeException("Failed to retrieve document field value using reference: " + reference, e);
    } finally {
        valueByteArrayStream.close();
        if (valueDataStoreStream !== undefined) {
            valueDataStoreStream.close();
        }
    }
    return valueToReturn;
}

// Returns true if a string value is null, undefined or empty
function isEmpty(stringToCheck) {
    return (!stringToCheck || 0 === stringToCheck.length);
}
</xsl:template>

    <xsl:template name="onErrorFunction">
function onError(errorEventObj) {
    // We will not mark the error as handled here. This will allow the document-worker framework to add the failure
    // itself rather than us duplicating the format of the failure value it constructs for non-script failure responses

    // Even though the action failed it still completed in terms of the document being sent for processing against the
    // action, so the action should be marked as completed
    processDocument(errorEventObj.rootDocument);
}
</xsl:template>

</xsl:stylesheet>
