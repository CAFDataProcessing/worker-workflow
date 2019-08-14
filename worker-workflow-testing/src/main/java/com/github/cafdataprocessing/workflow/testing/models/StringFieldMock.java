package com.github.cafdataprocessing.workflow.testing.models;

import com.hpe.caf.worker.document.fieldvalues.NonReferenceFieldValue;
import java.nio.charset.StandardCharsets;

public class StringFieldMock extends NonReferenceFieldValue
{

    private final String data;

    public StringFieldMock(final String data)
    {
        super(null, null);
        this.data = data;
    }

    @Override
    public byte[] getValue()
    {
        return data.getBytes(StandardCharsets.UTF_8);
    }

}
