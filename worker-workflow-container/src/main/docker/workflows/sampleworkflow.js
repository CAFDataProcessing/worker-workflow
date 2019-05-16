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

var ACTIONS = {
    family_hashing: function (document, settings) {
        return {
            queueName: 'dataprocessing-family-hashing-in',
            scripts: [
                {
                    name: 'recordProcessingTimes.js', script: 'function onProcessTask(e) {\n  var startTime = new Date();\n  e.rootDocument.getField(\"LONG_FAMILY_HASHING_START_TIME\").set(startTime.getTime());\n  e.rootDocument.getField(\"ENRICHMENT_TIME\").set(Math.round(startTime.getTime() \/ 1000));\n}\nfunction onAfterProcessTask(e) {\n  var endTime = new Date();\n  e.rootDocument.getField(\"LONG_FAMILY_HASHING_END_TIME\").set(endTime.getTime());\n}\nfunction onError(e) {\n  var failedTime = new Date();\n  e.rootDocument.getField(\"LONG_FAMILY_HASHING_FAILED_TIME\").set(failedTime.getTime());\n}\n'
                }
            ]
        };
    },
    lang_detect: function (document, settings) {
        if (!(fieldExists(document, 'CONTENT_PRIMARY'))) return null;
        return {
            queueName: 'dataprocessing-langdetect-in',
            customData: {'fieldSpecs': 'CONTENT_PRIMARY'},
            scripts: [
                {
                    name: 'recordProcessingTimes.js', script: 'function onProcessTask(e) {\n  var startTime = new Date();\n  e.rootDocument.getField(\"LONG_LANGUAGE_DETECTION_START_TIME\").set(startTime.getTime());\n}\nfunction onAfterProcessTask(e) {\n  var endTime = new Date();\n  e.rootDocument.getField(\"LONG_LANGUAGE_DETECTION_END_TIME\").set(endTime.getTime());\n}\nfunction onError(e) {\n  var failedTime = new Date();\n  e.rootDocument.getField(\"LONG_LANGUAGE_DETECTION_FAILED_TIME\").set(failedTime.getTime());\n}\n'
                }
            ]
        };
    },
    bulk_index: function (document, settings) {
        return {
            queueName: 'bulk-index',
            scripts: [
            ]
        };
    }
};
