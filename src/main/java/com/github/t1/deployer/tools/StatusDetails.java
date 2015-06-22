package com.github.t1.deployer.tools;

import static javax.ws.rs.core.Response.Status.*;

import java.util.UUID;

import javax.ejb.ApplicationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
public class StatusDetails {
    /** The ClientErrorException requires JAX-RS 2.0 and is not annotated as ApplicationException */
    @ApplicationException
    public static class WebException extends WebApplicationException {
        private static final long serialVersionUID = 1L;

        public WebException(Response response) {
            super(response);
        }
    }

    public static WebException badRequest(String message) {
        return webException(BAD_REQUEST, message);
    }

    public static WebException notFound(String message) {
        return webException(NOT_FOUND, message);
    }

    public static WebException webException(Status status, String message) {
        StatusDetails error = new StatusDetails(status, message);
        log.info("{}", error);
        return new WebException(error.toResponse());
    }

    private final UUID id = UUID.randomUUID();

    Status status;
    String type;

    public StatusDetails(Status status, String type) {
        this.status = status;
        this.type = type;
    }

    public Response toResponse() {
        return Response.status(status).entity(this).build();
    }

    @Override
    public String toString() {
        return status.getStatusCode() + " " + status.getReasonPhrase() + " [" + getId() + "] " + type;
    }

    public String toJson() {
        return "{" //
                + "\"id\":\"" + id + "\"," //
                + "\"status\":\"" + status + "\"," //
                + "\"type\":\"" + type + "\"" //
                + "}";
    }
}
