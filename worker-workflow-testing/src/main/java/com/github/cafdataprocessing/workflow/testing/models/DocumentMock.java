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
package com.github.cafdataprocessing.workflow.testing.models;

import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Failures;
import com.hpe.caf.worker.document.model.Field;
import com.hpe.caf.worker.document.model.Fields;
import com.hpe.caf.worker.document.model.Subdocuments;
import com.hpe.caf.worker.document.model.Task;
import java.util.Map;

public class DocumentMock implements Document
{
    private String reference;
    private Fields fields;
    private final Task task;
    private final Map<String, String> customData;
    private final Failures failures;
    private final Subdocuments subdocuments;
    private final Application application;
    private final Document parentDocument;
    private final Document rootDocument;

    public DocumentMock(final String reference, final Fields fields, final Task task, final Map<String, String> customData,
                        final Failures failures, final Subdocuments subdocuments, final Application application,
                        final Document parentDocument, final Document rootDocument)
    {
        this.reference = reference;
        this.fields = fields;
        this.task = task;
        this.customData = customData;
        this.failures = failures;
        this.subdocuments = subdocuments;
        this.application = application;
        this.parentDocument = parentDocument;
        this.rootDocument = rootDocument;
    }

    @Override
    public Task getTask()
    {
        return task;
    }

    @Override
    public String getReference()
    {
        return reference;
    }

    @Override
    public void setReference(final String reference)
    {
        this.reference = reference;
    }

    @Override
    public void resetReference()
    {
        this.reference = null;
    }

    @Override
    public Fields getFields()
    {
        return fields;
    }

    @Override
    public Field getField(final String fieldName)
    {
        return fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElseGet(() -> {
            final Field fieldMocked = new FieldMock(this, fieldName, application);
            final FieldsMock fields = (FieldsMock)this.fields;
            fields.addField(fieldMocked);
            return fieldMocked;
        });
    }

    @Override
    public String getCustomData(final String dataKey)
    {
        return customData.get(dataKey);
    }

    @Override
    public Failures getFailures()
    {
        return failures;
    }

    @Override
    public void addFailure(final String failureId, final String failureMessage)
    {
        this.failures.add(failureId, failureMessage);
    }

    @Override
    public Document getParentDocument()
    {
        return parentDocument;
    }

    @Override
    public Document getRootDocument()
    {
        return rootDocument;
    }

    @Override
    public Subdocuments getSubdocuments()
    {
        return subdocuments;
    }

    @Override
    public boolean hasSubdocuments()
    {
        if (subdocuments == null) {
            return false;
        } else {
            return !subdocuments.isEmpty();
        }
    }

    @Override
    public boolean hasChanges()
    {
        return true;
    }

    @Override
    public void reset()
    {
    }

    @Override
    public Application getApplication()
    {
        return application;
    }

    public void setFields(final Fields fields)
    {
        this.fields = fields;
    }

}
