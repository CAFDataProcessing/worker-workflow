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

package com.github.cafdataprocessing.workflow;

import com.github.cafdataprocessing.workflow.model.Action;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class ActionTypeAdapter extends TypeAdapter<Action>
{
    private final Gson gson= new GsonBuilder().setPrettyPrinting().create();
    @Override
    public void write(JsonWriter out, Action action) throws IOException
    {
        final String queueName = StringUtils.isEmpty(System.getenv("CAF_WORKFLOW_ACTION_"
                    + action.getName().toUpperCase() + "_INPUT_QUEUE"))
                    ? action.getName() + "-in"
                    : System.getenv("CAF_WORKFLOW_ACTION_" + action.getName().toUpperCase() + "_INPUT_QUEUE");
        out.beginObject();
        out.name("name").value(action.getName());
        out.name("conditionFunction").value(action.getConditionFunction());
        out.name("customData");
        gson.toJson(action.getCustomData(), Map.class, out);
        out.name("scripts");
        gson.toJson(action.getScripts(), Object.class, out);
        out.name("queueName").value(queueName);
        out.endObject();
    }

    @Override
    public Action read(JsonReader in) throws IOException
    {
        throw new UnsupportedOperationException("Not supported for the class type Action");
    }

}
