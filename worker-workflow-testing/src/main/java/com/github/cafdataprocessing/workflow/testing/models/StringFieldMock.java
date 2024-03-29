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
