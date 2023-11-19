package net.lukemcomber.oracle.model;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.genetics.biology.Gene;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganismDetails {

    @JsonProperty("name")
    public String name;

    @JsonProperty("genome")
    public String genome;

    @JsonProperty("age")
    public Integer age;
    @JsonProperty("energy")
    public Integer energy;
    @JsonProperty("genus")
    public String genus;
    @JsonProperty("parentId")
    public String parentId;
    @JsonProperty("genes")
    public List<Gene> genes;
}
