package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.lukemcomber.oracle.model.BasicResponse;

public class LogResponse extends BasicResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("log")
    public ArrayNode log;

}
