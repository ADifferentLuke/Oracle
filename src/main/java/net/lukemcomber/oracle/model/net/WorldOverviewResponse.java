package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.oracle.model.GenericResponse;

public class WorldOverviewResponse extends GenericResponse {

    @JsonProperty("id")
    public String id;
    @JsonProperty("width")
    public int width;
    @JsonProperty("height")
    public int height;
    @JsonProperty("depth")
    public int depth;
}
