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
package com.github.cafdataprocessing.workflow.models;

import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Field;
import com.hpe.caf.worker.document.model.Fields;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;

public class FieldsMock implements Fields
{
    private List<Field> fields;
    private final List<Field> originalFields;
    private final Application application;
    private final Document document;

    public FieldsMock(final List<Field> fields, final Application application, final Document document)
    {
        this.fields = fields;
        this.originalFields = new ArrayList<>(fields);
        this.application = application;
        this.document = document;
    }

    public FieldsMock(final Fields fields, final Application application, final Document document)
    {
        this.fields = fields.stream().collect(toList());
        this.originalFields = new ArrayList<>(this.fields);
        this.application = application;
        this.document = document;
    }

    @Override
    public Field get(String fieldName)
    {
        return fields.stream().filter(f -> f.getName().equals(fieldName)).findAny().orElseGet(() -> {
            return new FieldMock(document, fieldName, application);
        });
    }

    @Override
    public Document getDocument()
    {
        return this.document;
    }

    @Override
    public void reset()
    {
        this.fields = new ArrayList<>(originalFields);
    }

    @Override
    public Stream<Field> stream()
    {
        return this.fields.stream();
    }

    @Override
    public Application getApplication()
    {
        return this.application;
    }

    @Override
    public Iterator<Field> iterator()
    {
        return this.fields.iterator();
    }

    public void addField(Field field)
    {
        this.fields.add(field);
    }

}
