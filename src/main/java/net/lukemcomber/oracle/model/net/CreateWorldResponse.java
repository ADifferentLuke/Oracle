package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.oracle.model.GenericResponse;

public class CreateWorldResponse extends GenericResponse {

    @JsonProperty("id")
    public String id;

}
