package net.lukemcomber.oracle.model;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.HashMap;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class GenericResponse {

    private String message;
    @JsonIgnore
    private HttpStatus statusCode;

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public HttpStatus getStatusCode(){
        return statusCode;
    }

    public void setStatusCode(final HttpStatus code){
        this.statusCode = code;
    }
}
