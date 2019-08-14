package com.github.cafdataprocessing.workflow.testing.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessingWorkerVersion
{

    @JsonProperty("NAME")
    private String name;
    @JsonProperty("VERSION")
    private String version;

    /**
     * No args constructor for use in serialization
     *
     */
    public ProcessingWorkerVersion()
    {
    }

    /**
     *
     * @param data
     */
    public ProcessingWorkerVersion(String data, String version)
    {
        super();
        this.name = data;
        this.version = version;
    }

    @JsonProperty("NAME")
    public String getName()
    {
        return name;
    }

    @JsonProperty("NAME")
    public void setName(String name)
    {
        this.name = name;
    }

    @JsonProperty("VERSION")
    public String getVersion()
    {
        return version;
    }

    @JsonProperty("VERSION")
    public void setVersion(String version)
    {
        this.version = version;
    }

}
