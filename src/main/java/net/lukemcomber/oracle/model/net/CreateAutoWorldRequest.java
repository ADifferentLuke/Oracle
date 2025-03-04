package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateAutoWorldRequest extends CreateWorldRequest{

    @JsonProperty("maxDays")
    public long maxDays;
    @JsonProperty("tickDelay")
    public int tickDelay;

    @JsonProperty("epochs")
    public int epochs;

    @JsonProperty("filterPath")
    public String filterPath;

    @JsonProperty("initialPopulation")
    public int initialPopulation;

    @JsonProperty("reusePopulation")
    public int reusePopulation;

    @JsonProperty("name")
    public String name;

}
