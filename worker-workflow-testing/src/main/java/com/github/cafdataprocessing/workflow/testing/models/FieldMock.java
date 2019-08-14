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
import com.hpe.caf.worker.document.model.Field;
import com.hpe.caf.worker.document.model.FieldValues;
import java.util.ArrayList;
import java.util.List;

public class FieldMock implements Field
{

    private List<String> values;
    private final List<String> originalValuesBeforeChanges;
    private final Document documentThisFieldIsAssociatedWith;
    private final String name;
    private boolean hasChanged;
    private final Application application;

    public FieldMock(final List<String> values,
                     final Document documentThisFieldIsAssociatedWith, final String name,
                     final Application application)
    {
        this.values = values;
        this.originalValuesBeforeChanges = new ArrayList<>(values);
        this.documentThisFieldIsAssociatedWith = documentThisFieldIsAssociatedWith;
        this.name = name;
        this.hasChanged = false;
        this.application = application;
    }

    public FieldMock(final Document documentThisFieldIsAssociatedWith, final String name,
                     final Application application)
    {
        this.values = new ArrayList<>();
        this.originalValuesBeforeChanges = new ArrayList<>(values);
        this.documentThisFieldIsAssociatedWith = documentThisFieldIsAssociatedWith;
        this.name = name;
        this.hasChanged = false;
        this.application = application;
    }

    @Override
    public void add(String data)
    {
        this.values.add(data);
        this.hasChanged = true;
    }

    @Override
    public void add(byte[] data)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addReference(String dataRef)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear()
    {
        this.values.clear();
        this.hasChanged = true;
    }

    @Override
    public Document getDocument()
    {
        return this.documentThisFieldIsAssociatedWith;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public List<String> getStringValues()
    {
        return this.values;
    }

    @Override
    public FieldValues getValues()
    {
        return new FieldValuesMock(this, values);
    }

    @Override
    public boolean hasChanges()
    {
        return this.hasChanged;
    }

    @Override
    public boolean hasValues()
    {
        return !this.values.isEmpty();
    }

    @Override
    public void set(String data)
    {
        this.values.clear();
        this.values.add(data);
        this.hasChanged = true;
    }

    @Override
    public void set(byte[] data)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setReference(String dataRef)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void reset()
    {
        this.values = new ArrayList<>(originalValuesBeforeChanges);
        this.hasChanged = false;
    }

    @Override
    public Application getApplication()
    {
        return this.application;
    }

}
