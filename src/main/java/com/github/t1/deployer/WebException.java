package com.github.t1.deployer;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

public class WebException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public static WebApplicationException badRequest(String message) {
        return new WebException(BAD_REQUEST, message);
    }

    public static WebApplicationException notFound(String message) {
        return new WebException(NOT_FOUND, message);
    }

    public WebException(Status status, String message) {
        super(message, Response.status(status).type(TEXT_PLAIN_TYPE).entity(message).build());
    }
}
