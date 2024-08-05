package net.lukemcomber.oracle.model.net;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude( JsonInclude.Include.NON_NULL)
public abstract class WorldOverview {

    @JsonProperty("id")
    public String id;

    @JsonProperty("name")
    public String name;
    @JsonProperty("width")
    public int width;
    @JsonProperty("height")
    public int height;
    @JsonProperty("depth")
    public int depth;

    @JsonProperty("active")
    public boolean active;
    @JsonProperty("interactive")
    public boolean interactive;

    @JsonProperty("totalTicks")
    public long totalTicks;

    @JsonProperty("currentTick")
    public long currentTick;
    @JsonProperty("totalDays")
    public long totalDays;

    @JsonProperty("currentOrganismCount")
    public long currentOrganismCount;

    @JsonProperty("totalOrganismCount")
    public long totalOrganismCount;

    @JsonProperty("properties")
    public Map<String,String> properties;

    @JsonProperty("initialPopulation")
    public List<String> initialPopulation;
}
