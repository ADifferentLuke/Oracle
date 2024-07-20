package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.oracle.model.BasicResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse<T> extends BasicResponse {

    @JsonProperty("result")
    public T result;

}
