package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.oracle.model.GenericResponse;
;;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdvanceWorldResponse extends GenericResponse {

    @JsonProperty("active")
    public Boolean active;
    @JsonProperty("ticks")
    public Long ticksMade;
}
