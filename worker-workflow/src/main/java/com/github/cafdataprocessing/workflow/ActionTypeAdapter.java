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
                    ? action.getName().replaceAll("-", "") + "-in"
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
