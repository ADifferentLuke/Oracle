package net.lukemcomber.oracle.model.net;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.oracle.model.GenericResponse;
import net.lukemcomber.oracle.model.OrganismDetails;

public class InspectOrganismResponse extends GenericResponse {

    @JsonProperty("organism")
    public OrganismDetails organism;
}
