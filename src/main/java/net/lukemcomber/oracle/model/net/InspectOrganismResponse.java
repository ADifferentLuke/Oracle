package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.oracle.model.GenericResponse;
import net.lukemcomber.oracle.model.OrganismDetails;

public class InspectOrganismResponse extends GenericResponse {

    @JsonProperty("organism")
    public OrganismDetails organism;
}
