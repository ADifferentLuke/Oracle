package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateSteppableWorld extends CreateWorldRequest {

    @JsonProperty(value = "ticksPerTurn")
    public long ticksPerTurn;
}
