package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateAutoWorldRequest extends CreateWorldRequest{

    @JsonProperty("maxDays")
    public long maxDays;
    @JsonProperty("tickDelay")
    public long tickDelay;

    @JsonProperty("epochs")
    public int epochs;

    @JsonProperty("initialPopulation")
    public int initialPopulation;

    @JsonProperty("reusePopulation")
    public int reusePopulation;

}
