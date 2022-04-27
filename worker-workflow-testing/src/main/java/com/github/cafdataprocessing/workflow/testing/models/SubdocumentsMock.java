/*
 * Copyright 2017-2022 Micro Focus or one of its affiliates.
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
import com.hpe.caf.worker.document.model.Subdocument;
import com.hpe.caf.worker.document.model.Subdocuments;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class SubdocumentsMock implements Subdocuments
{
    private final List<Subdocument> subdocuments;

    public SubdocumentsMock(List<Subdocument> subdocuments)
    {
        this.subdocuments = subdocuments;
    }

    public SubdocumentsMock()
    {
        this.subdocuments = new ArrayList<>();
    }

    @Override
    public Subdocument add(String reference)
    {
        final SubdocumentMock temp = new SubdocumentMock(reference, null, null, null, null, null, null, null, null);
        subdocuments.add(temp);
        return temp;
    }

    public void add(final Subdocument subdocument)
    {
        subdocuments.add(subdocument);
    }

    @Override
    public Subdocument get(int index)
    {
        return subdocuments.get(index);
    }

    @Override
    public Document getDocument()
    {
        return null;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public int size()
    {
        return subdocuments.size();
    }

    @Override
    public Stream<Subdocument> stream()
    {
        return subdocuments.stream();
    }

    @Override
    public Application getApplication()
    {
        return null;
    }

    @Override
    public Iterator<Subdocument> iterator()
    {
        return subdocuments.iterator();
    }

}
