package net.lukemcomber.oracle.model.net;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.lukemcomber.genetics.SteppableEcosystem;

import java.util.LinkedList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = DEDUCTION)
@JsonSubTypes( {@JsonSubTypes.Type( CreateSteppableWorld.class), @JsonSubTypes.Type( CreateAutoWorldRequest.class)})
public abstract class CreateWorldRequest {

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

    @JsonProperty("name")
    public String name;
    @JsonProperty("zoo")
    public List<String> zoology = new LinkedList<>();
}
