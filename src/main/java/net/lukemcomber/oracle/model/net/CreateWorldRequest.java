package net.lukemcomber.oracle.model.net;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


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
