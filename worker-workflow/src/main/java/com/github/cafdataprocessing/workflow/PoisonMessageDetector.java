/*
 * Copyright 2017-2024 Open Text.
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

import com.hpe.caf.worker.document.model.Document;

public class PoisonMessageDetector
{
    private PoisonMessageDetector(){}

    public static boolean isPoisonDocument(final Document document)
    {
        // A poison document is a document that a downstream worker has redirected back
        // to the workflow worker. A document is considered poison if:
        //
        // 1. It has a a non-empty CAF_WORKFLOW_SETTINGS field.
        //
        // 2. It does NOT have a 'workflowName' inside it's custom data.
        //
        // Point (2) is important, it's not enough to just check for a non-empty 
        // CAF_WORKFLOW_SETTINGS field, since that makes it possible for a rogue agent to
        // stage documents that already contain CAF_WORKFLOW_SETTINGS field, which could
        // possibly be used to write to, or delete from, another tenant's index (if, for 
        // example, the CAF_WORKFLOW_SETTINGS contained a 'tenantId' that did not belong to
        // the rogue agent).
        //
        // Custom data cannot be controlled by a rogue agent, so if we can check that we have
        // a 'workflowName' inside custom data (which is a field that should always be present
        // in custom data, 'tenantId' may not be required in some cases), it gives some 
        // confidence that this is really a poison document, and not a document staged by
        // a rogue agent, and as such we can safely use the CAF_WORKFLOW_SETTINGS present
        // in the document and trust that the settings inside it are valid.
        return document.getField("CAF_WORKFLOW_SETTINGS").hasValues()
            && (document.getTask().getCustomData("workflowName") == null);
    }
}
