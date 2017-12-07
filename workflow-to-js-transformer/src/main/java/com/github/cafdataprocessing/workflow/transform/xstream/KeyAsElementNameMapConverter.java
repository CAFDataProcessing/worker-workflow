/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.github.cafdataprocessing.workflow.transform.xstream;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Iterator;
import java.util.Map;

/**
 * An extension to the XStream NamedMapConverter that allows specifying that a key should be used for the generated
 * element name with its value as the content of the element.
 */
public class KeyAsElementNameMapConverter extends NamedMapConverter
{
    private final Class valueType;

    public KeyAsElementNameMapConverter(Mapper mapper, String valueName, Class valueType) {
        super(mapper, null, "name", String.class, valueName, valueType);
        this.valueType = valueType;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
        final Map map = (Map)source;

        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            final Object key = entry.getKey();
            final Object value = entry.getValue();
            writeItem((String) key, valueType, value, context, writer);
        }
    }
}
