/*
 * Copyright 2017-2021 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.workflow;

import com.google.gson.Gson;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Field;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FailureFieldsManager
{
    private static final Logger LOG = LoggerFactory.getLogger(FailureFieldsManager.class);
    private final Gson gson;

    public FailureFieldsManager()
    {
        this.gson = new Gson();
    }

    public void handleExtraFailureSubFields(final Document document)
    {
        if (!PoisonMessageDetector.isPoisonDocument(document)) {
            final Field failuresField = document.getField("CAF_WORKFLOW_EXTRA_FAILURE_SUBFIELDS");
            final Map<String, String> extraFailureSubfields = retrieveExtraFailureSubfields(document);
            failuresField.clear();
            if (!extraFailureSubfields.isEmpty()) {
                final String cafWorkflowExtraFailureSubfieldsJson = gson.toJson(extraFailureSubfields);
                failuresField.add(cafWorkflowExtraFailureSubfieldsJson);
            }
        }
    }

    private static Map<String, String> retrieveExtraFailureSubfields(final Document document)
    {
        final Map<String, String> failureSubfields = new HashMap<>();

        //Loop continually until no other failure subfields are found
        int failureSubfieldCount = 0;
        while (true) {
            final String failureSubfieldKey = document.getCustomData("extraFailuresSubfieldKey" + failureSubfieldCount);
            if (failureSubfieldKey == null) {
                break;
            }
            final String failureSubfieldValue = document.getCustomData("extraFailuresSubfieldValue" + failureSubfieldCount);
            if (failureSubfieldValue == null) {
                LOG.warn("Unable to add failure subfield {} to extra failure subfields as it has no value.", failureSubfieldKey);
                continue;
            }
            failureSubfields.put(failureSubfieldKey, failureSubfieldValue);
            failureSubfieldCount++;
        }
        return failureSubfields;
    }
}
