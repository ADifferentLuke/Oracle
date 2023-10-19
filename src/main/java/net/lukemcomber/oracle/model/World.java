package net.lukemcomber.oracle.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class World {
    @JsonProperty("totalTicks")
    public long totalTicks;
    @JsonProperty("totalDays")
    public long totalDays;
    @JsonProperty("currentTick")
    public int currentTick;
    @JsonProperty("cells")
    public List<CellInfo> cells = new LinkedList<>();
}
