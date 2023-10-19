package net.lukemcomber.oracle.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CellInfo {

    @JsonProperty("x")
    public long x;
    @JsonProperty("y")
    public long y;
    @JsonProperty("z")
    public long z;
    @JsonProperty("type")
    public String type;

    public CellInfo() {
        x = 0;
        y = 0;
        z = 0;
        type = "unknown";
    }
    public CellInfo( final long x, final long y, final long z, final String type ){
       this.x = x;
       this.y = y;
       this.z = z;
       this.type = type;
    }
}
