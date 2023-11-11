package net.lukemcomber.oracle.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.image.PackedColorModel;

public class SeedCellLocation extends CellLocation{

    @JsonProperty("activated")
    public boolean activated;

    public SeedCellLocation() {
        super();
        activated = false;
    }
    public SeedCellLocation(final long x, final long y, final long z, final String type, final boolean activated ){
        super(x,y,z,type);
        this.activated = activated;
    }
}
