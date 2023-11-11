package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.oracle.model.GenericResponse;
import net.lukemcomber.oracle.model.OrganismBody;

import java.util.LinkedList;
import java.util.List;

public class EcologyResponse extends GenericResponse {

    @JsonProperty("totalTicks")
    public long totalTicks;
    @JsonProperty("totalDays")
    public long totalDays;
    @JsonProperty("currentTick")
    public int currentTick;

    @JsonProperty("organisms")
    public List<OrganismBody> organismBodies = new LinkedList<>();

}
