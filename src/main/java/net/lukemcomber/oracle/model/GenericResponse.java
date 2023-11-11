package net.lukemcomber.oracle.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.HashMap;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class GenericResponse {

    private String message;
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
