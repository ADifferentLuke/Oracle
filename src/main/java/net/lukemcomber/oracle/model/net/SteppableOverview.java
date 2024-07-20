package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SteppableOverview extends WorldOverview {

    @JsonProperty("turnsPerTick")
    public long turnsPerTick;
}
