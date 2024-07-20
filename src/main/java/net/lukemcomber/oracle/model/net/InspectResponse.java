package net.lukemcomber.oracle.model.net;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.genetics.model.SpatialCoordinates;
import net.lukemcomber.oracle.model.BasicResponse;

import java.util.HashMap;
import java.util.Map;

public class InspectResponse extends BasicResponse {

    @JsonProperty("coordinates")
    private SpatialCoordinates coordinates = null;
    @JsonProperty("environment")
    private Map<String,String> environment = null;
    @JsonProperty("occupant")
    private Map<String,String> occupant = null;

    public SpatialCoordinates getCoordinates(){
        return coordinates;
    }
    public void setCoordinates(final SpatialCoordinates coordinates){
        this.coordinates = coordinates;
    }

    public Map<String,String> getEnvironment(){
        return environment;
    }
    public void addEnvironmentValue(final String key, String value){
        if( null == environment){
            environment = new HashMap<>();
        }
        environment.put(key, value);
    }

    public Map<String,String> getOccupant(){
        return occupant;
    }
    public void addOccupantValue(final String key, final String vaule){
        if( null == occupant){
            occupant = new HashMap<>();
        }
        occupant.put(key, vaule);
    }

}
