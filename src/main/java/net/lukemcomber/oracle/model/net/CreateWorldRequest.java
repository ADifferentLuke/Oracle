package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateWorldRequest {

    @JsonProperty("world")
    public String worldType;
    @JsonProperty("height")
    public long height;
    @JsonProperty("width")
    public long width;
    @JsonProperty("depth")
    public long depth;
    @JsonProperty("ticksPerDay")
    public long ticksPerDay;
    @JsonProperty("ticksPerTurn")
    public long ticksPerTurn;

    @JsonProperty("zoo")
    public List<String> zoology = new LinkedList<>();
}
