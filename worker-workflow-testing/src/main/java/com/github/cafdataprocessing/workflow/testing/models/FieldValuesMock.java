package com.github.cafdataprocessing.workflow.testing.models;

import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Field;
import com.hpe.caf.worker.document.model.FieldValue;
import com.hpe.caf.worker.document.model.FieldValues;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class FieldValuesMock implements FieldValues
{

    private final Field field;
    private final List<FieldValue> fieldValues;

    public FieldValuesMock(final Field field, final List<String> values)
    {
        this.field = field;
        this.fieldValues = new ArrayList<>();
        for (final String value : values) {
            fieldValues.add(new StringFieldMock(value));
        }
    }

    @Override
    public Field getField()
    {
        return this.field;
    }

    @Override
    public boolean isEmpty()
    {
        return fieldValues.isEmpty();
    }

    @Override
    public int size()
    {
        return this.fieldValues.size();
    }

    @Override
    public Stream<FieldValue> stream()
    {
        return this.fieldValues.stream();
    }

    @Override
    public Application getApplication()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<FieldValue> iterator()
    {
        return this.fieldValues.iterator();
    }

}
