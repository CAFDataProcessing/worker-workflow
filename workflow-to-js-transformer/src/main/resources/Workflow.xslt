<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:sxl="http://www.w3.org/1999/XSL/Transform" xmlns:xslt="http://www.w3.org/1999/XSL/Transform"
                xmlns:workflow_transform="com.github.cafdataprocessing.workflow.transform">
    <xsl:output method="text" omit-xml-declaration="yes" indent="no"/>
    
    <xsl:param name="projectId"/>

    <xsl:template match="/workflow">
        var System  = Java.type("java.lang.System")
        var DataStore = Java.type("com.hpe.caf.api.worker.DataStore");
        var ByteArray = Java.type("byte[]");
        var postProcessingScriptPropertyName = "postProcessingScript";

        // Constants for return values
        var ACTION_TO_EXECUTE = 'actionToExecute';
        var CONDITIONS_NOT_MET = 'conditionsNotMet';
        var ALREADY_EXECUTED = 'alreadyExecuted';

        // Workflow ID: <xsl:value-of select="details/id"/>
        // Workflow Name: <xsl:value-of select="details/name"/>
        function processDocument(document){
            updateActionStatus(document);
            <xsl:for-each select="processingRules/processingRule"><xsl:sort select="details/priority" data-type="number" order="ascending"/>
                <xsl:if test="details/enabled = 'true'">
                    // Rule Name: <xsl:value-of select="details/name"/>
                    var ruleResult = rule_<xsl:value-of select="details/id"/> (document);
                    if(ruleResult==ACTION_TO_EXECUTE) return;
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
        function rule_<xsl:value-of select="$ruleId"/> (document){

            if(isRuleCompleted(document, '<xsl:value-of select="$ruleId"/>')){
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
                var actionResult = action_<xsl:value-of select="details/id"/> (document);
                if (actionResult==ACTION_TO_EXECUTE) return ACTION_TO_EXECUTE;
            </xsl:for-each>

            recordRuleCompleted(document, '<xsl:value-of select="$ruleId"/>');
        }
    </xsl:template>

    <xsl:template name="actionFunction">
        <xsl:variable name="actionId" select="details/id"/>
        // Action Name: <xsl:value-of select="details/name"/>
        // Return CONDITIONS_NOT_MET if the document did not match the action conditions
        function action_<xsl:value-of select="$actionId"/> (document){
            if(isActionCompleted(document, '<xsl:value-of select="$actionId"/>')){
                return ALREADY_EXECUTED;
            }
            <xsl:if test="conditions/*">if (!<xsl:apply-templates select="conditions/condition"/>) return CONDITIONS_NOT_MET;</xsl:if>
            var actionDetails = <xsl:apply-templates select="details/settings"/>;
            recordActionToExecute(document, '<xsl:value-of select="$actionId"/>', actionDetails);
            return evaluateActionDetails(document, actionDetails);
        }
    </xsl:template>

    <xsl:template match="details/settings[../typeInternalName='DocumentWorkerHandler']">{
        'internal_name' : '<xsl:value-of select="../typeInternalName"/>',
        'queueName' : '<xsl:choose><xsl:when test="queueName != ''"><xsl:value-of select="queueName"/></xsl:when><xsl:otherwise><xsl:variable name="workerNameQueueEnvValue" select="workflow_transform:TransformerFunctions.getWorkerQueueFromEnvironment(workerName)"/><xsl:choose><xsl:when test="$workerNameQueueEnvValue != ''"><xsl:value-of select="$workerNameQueueEnvValue"/></xsl:when><xsl:otherwise><xsl:value-of select="concat(workerName, 'Input')"/></xsl:otherwise></xsl:choose></xsl:otherwise></xsl:choose>',
        'workerName' : '<xsl:value-of select="workerName"/>',
        'fields' : [<xsl:for-each select="fields">'<xsl:value-of select="text()"/>'<xsl:if test="position() != last()">,
        </xsl:if></xsl:for-each>]<xsl:if test="customData/*">,
            'customData' : {<xsl:apply-templates select="customData"/>}
        </xsl:if>   }</xsl:template>

    <xsl:template match="details/settings[../typeInternalName='CompositeDocumentWorkerHandler']">{
        'internal_name' : '<xsl:value-of select="../typeInternalName"/>',
        'queueName' : '<xsl:choose><xsl:when test="queueName != ''"><xsl:value-of select="queueName"/></xsl:when><xsl:otherwise><xsl:variable name="workerNameQueueEnvValue" select="workflow_transform:TransformerFunctions.getWorkerQueueFromEnvironment(workerName)"/><xsl:choose><xsl:when test="$workerNameQueueEnvValue != ''"><xsl:value-of select="$workerNameQueueEnvValue"/></xsl:when><xsl:otherwise><xsl:value-of select="concat(workerName, 'Input')"/></xsl:otherwise></xsl:choose></xsl:otherwise></xsl:choose>',
        'workerName' : '<xsl:value-of select="workerName"/>'<xsl:if test="customData/*">,
            'customData' : {<xsl:apply-templates select="customData"/>}
        </xsl:if>   }</xsl:template>

    <xsl:template match="details/settings[../typeInternalName='ChainedActionType']">{
        'internal_name' : '<xsl:value-of select="../typeInternalName"/>',
        'queueName' : '<xsl:choose><xsl:when test="queueName != ''"><xsl:value-of select="queueName"/></xsl:when><xsl:otherwise><xsl:variable name="workerNameQueueEnvValue" select="workflow_transform:TransformerFunctions.getWorkerQueueFromEnvironment(workerName)"/><xsl:choose><xsl:when test="$workerNameQueueEnvValue != ''"><xsl:value-of select="$workerNameQueueEnvValue"/></xsl:when><xsl:otherwise><xsl:value-of select="concat(workerName, 'Input')"/></xsl:otherwise></xsl:choose></xsl:otherwise></xsl:choose>',
        'workerName' : '<xsl:value-of select="workerName"/>'<xsl:if test="customData/*">,
        'customData' : {<xsl:apply-templates select="customData"/>}
        </xsl:if>   }</xsl:template>

    <xsl:template match="details/settings[../typeInternalName='FieldMappingActionType']">function(){executeFieldMapping(document, {
        <xsl:for-each select="mappings/*">
            '<xsl:value-of select="name()"/>' : '<xsl:value-of select="."/>'<xsl:if test="position() != last()">,</xsl:if>
        </xsl:for-each>
    });}</xsl:template>

    <xsl:template match="customData">
        <xsl:for-each select="*"><xsl:variable name="sourceData"><xsl:call-template name="customDataSource"/></xsl:variable>
            <xsl:choose>
                <xsl:when test="$sourceData != ''">'<xsl:value-of select="name(.)"/>': '<xsl:value-of select="$sourceData"/>'<xsl:if test="position() != last()">, </xsl:if></xsl:when>
                <xsl:when test="not(source)">'<xsl:value-of select="name(.)"/>': '<xsl:value-of select="text()"/>'<xsl:if test="position() != last()">, </xsl:if></xsl:when>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="customDataSource">
        <xsl:choose><xsl:when test="source = 'inlineJson' and data !=''"><xsl:call-template name="jsonDataSource"><xsl:with-param name="currentProperties" select="data/*"/></xsl:call-template></xsl:when></xsl:choose>
        <xsl:choose><xsl:when test="source = 'projectId'"><xsl:value-of select="$projectId"/></xsl:when></xsl:choose>
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
                    <xsl:otherwise>"<xsl:value-of select="text()"/>"</xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- fall back template outputting details of other action types -->
    <xsl:template priority="-.5" match="details/settings">{
        'internal_name' : '<xsl:value-of select="../typeInternalName"/>',
        'queueName': '<xsl:choose><xsl:when test="queueName != ''"><xsl:value-of select="queueName"/></xsl:when><xsl:otherwise>default-input</xsl:otherwise></xsl:choose>'
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
        function evaluateValuesAgainstCondition(document, fieldName, expectedValue, evaluateFunction){
            if(!document.getField(fieldName).hasValues()){
                return false;
            }
            var fieldValues = document.getField(fieldName).getValues();
            for each(var fieldValue in fieldValues){
                var valueToEvaluate = getDocumentFieldValueAsString(fieldValue, document);
                if(evaluateFunction(expectedValue, valueToEvaluate) === true){
                    return true;
                }
            }
            return false;
        }

        function existsCondition_(document, fieldName) {
            return document.getField(fieldName).hasValues();
        }

        function stringCondition_is(document, fieldName, value) {
            return evaluateValuesAgainstCondition(document, fieldName, value, function(expectedValue, actualValue){
                if(actualValue.equalsIgnoreCase(expectedValue)){
                    return true;
                }
            });
        }

        function stringCondition_contains(document, fieldName, value) {
            return evaluateValuesAgainstCondition(document, fieldName, value, function(expectedValue, actualValue){
                if(actualValue.toUpperCase(java.util.Locale.getDefault())
                .contains(expectedValue.toUpperCase(java.util.Locale.getDefault()))){
                    return true;
                }
            });
        }

        function stringCondition_starts_with(document, fieldName, value) {
            return evaluateValuesAgainstCondition(document, fieldName, value, function(expectedValue, actualValue){
                if(actualValue.toUpperCase(java.util.Locale.getDefault())
                .startsWith(expectedValue.toUpperCase(java.util.Locale.getDefault()))){
                    return true;
                }
            });
        }

        function stringCondition_ends_with(document, fieldName, value) {
            return evaluateValuesAgainstCondition(document, fieldName, value, function(expectedValue, actualValue){
                if(actualValue.toUpperCase(java.util.Locale.getDefault())
                .endsWith(expectedValue.toUpperCase(java.util.Locale.getDefault()))){
                    return true;
                }
            });
        }

        function regexCondition_(document, fieldName, value) {
        }

        function dateCondition_before(document, fieldName, value) {
        }

        function dateCondition_after(document, fieldName, value) {
        }

        function dateCondition_on(document, fieldName, value) {
        }

        function numberCondition_gt(document, fieldName, value) {
        }

        function numberCondition_lt(document, fieldName, value) {
        }

        function numberCondition_eq(document, fieldName, value) {

        }

        function notCondition(aBoolean) {
            return !aBoolean;
        }
    </xsl:template>

    <xsl:template name="trackingFunctions">
        function isRuleCompleted(document, ruleId){
            return document.getField('CAF_PROCESSING_RULES_COMPLETED').getStringValues().contains(ruleId);
        }

        function isActionCompleted(document, actionId) {
            return document.getField('CAF_ACTIONS_COMPLETED').getStringValues().contains(actionId);
        }

        function recordRuleCompleted(document, ruleId){
            document.getField('CAF_PROCESSING_RULES_COMPLETED').add(ruleId);
        }

        function recordActionCompleted(document, actionId){
            document.getField('CAF_ACTIONS_COMPLETED').add(actionId);
        }

        function recordActionToExecute(document, actionId){
            document.getField('CAF_ACTION_TO_EXECUTE').add(actionId);
        }

        function updateActionStatus(document){
            // this may be the first time the document has been presented to the workflow
            if(!document.getField('CAF_ACTION_TO_EXECUTE').hasValues()){
                return;
            }
            recordActionCompleted(document, document.getField('CAF_ACTION_TO_EXECUTE').getStringValues().get(0));
            document.getField('CAF_ACTION_TO_EXECUTE').clear();
        }
    </xsl:template>

    <xsl:template name="utilityFunctions">
        // evaluate the determined details of an action, either executing the action against document or preparing the
        // document to execute the action
        function evaluateActionDetails(document, actionDetails){
            if(typeof actionDetails === 'function'){
                actionDetails();
                updateActionStatus(document);
                return ALREADY_EXECUTED;
            }
            // propagate the post-processing field to response custom data if it exists on the document custom data
            var responseCustomData = actionDetails.customData ? actionDetails.customData : {};
            if(!isEmpty(document.getCustomData(postProcessingScriptPropertyName))){
                responseCustomData[postProcessingScriptPropertyName] = document.getCustomData(postProcessingScriptPropertyName);
            }

            // Update document destination queue to that specified by action and pass appropriate settings and customData
            var queueToSet = !isEmpty(actionDetails.queueName) ? actionDetails.queueName : actionDetails.workerName+"Input";
            var response = document.getTask().getResponse();
            response.setQueueNameOverride(queueToSet);
            response.setCustomData(responseCustomData);
            return ACTION_TO_EXECUTE;
        }

        // executes the field mapping action on the document
        function executeFieldMapping(document, mappings){
            // get the field values to map (from the document)
            var documentFieldsValuesMap = {};
            for(var mappingKey in mappings) {
                var documentFieldToMapFrom =  document.getField(mappingKey);
                documentFieldsValuesMap[mappingKey] = documentFieldToMapFrom.getValues();
                documentFieldToMapFrom.clear();
            }

            // for each mapping add the original field value with the new key
            for(var mappingKey in mappings) {
                var mappingDestination = mappings[mappingKey];
                var documentFieldToMapTo = document.getField(mappingDestination);
                for each(var fieldValue in documentFieldsValuesMap[mappingKey]) {
                    if(fieldValue.isReference()){
                    documentFieldToMapTo.addReference(fieldValue.getReference());
                    }
                    else {
                    documentFieldToMapTo.add(fieldValue.getValue());
                    }
                }
            }
        }

        // Returns string representing value of a Document Worker FieldValue
        function getDocumentFieldValueAsString(fieldValue, document){
            if(!fieldValue.isReference()){
                return fieldValue.getStringValue();
            }
            var reference = fieldValue.getReference();
            var valueByteArrayStream = new java.io.ByteArrayOutputStream();
            var valueToReturn;
            var valueDataStoreStream;
            try {
                var store = document.getApplication().getService(DataStore.class);
                valueDataStoreStream = store.retrieve(reference);
                var valueBuffer = new ByteArray(1024);
                var valuePortionLength;
                while((valuePortionLength = valueDataStoreStream.read(valueBuffer)) != -1){
                    valueByteArrayStream.write(valueBuffer, 0, valuePortionLength);
                }
                valueToReturn = valueByteArrayStream.toString("UTF-8");
            }
            catch(e){
                throw new java.lang.RuntimeException("Failed to retrieve document field value using reference: "+ reference, e);
            }
            finally {
                valueByteArrayStream.close();
                if(valueDataStoreStream!==undefined){
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
            // we will not mark the error as handled here. This will allow the document-worker framework to add the failure
            // itself rather than us duplicating the format of the failure value it constructs for non-script failure responses

            // even though the action failed it still completed in terms of the document being sent for processing against the
            // action, so the action should be marked as completed
            processDocument(errorEventObj.rootDocument);
        }
    </xsl:template>

</xsl:stylesheet>
