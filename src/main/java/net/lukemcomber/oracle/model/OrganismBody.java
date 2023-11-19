package net.lukemcomber.oracle.model;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

public class OrganismBody {

    @JsonProperty("alive")
    private boolean alive;

    @JsonProperty("id")
    private String id;
    @JsonProperty("cells")
    private List<CellLocation> cells;
    public OrganismBody() {
        alive = false;
        cells = new LinkedList<>();
        id = null;
    }

    public String getId(){
       return id;
    }
    public void setId(final String id){
        this.id = id;
    }

    public boolean isAlive(){
        return alive;
    }
    public List<CellLocation> getCells(){
        return cells;
    }

    public void setAlive( final boolean b ){
        alive = b;
    }

    public void addCell( final CellLocation c ){
        cells.add(c);
    }
}
