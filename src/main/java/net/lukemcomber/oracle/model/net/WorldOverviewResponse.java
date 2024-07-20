package net.lukemcomber.oracle.model.net;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


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

    @JsonProperty("active")
    public boolean active;

    @JsonProperty("interactive")
    public boolean interactive;
}
