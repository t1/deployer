package com.github.t1.deployer.app;

import static com.github.t1.log.LogLevel.*;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.container.LoggerContainer;
import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.log.*;

@Api
@Logged(level = INFO)
public class LoggerResource {
    private LoggerConfig logger;

    @Inject
    LoggerContainer container;

    public LoggerResource logger(LoggerConfig logger) {
        this.logger = logger;
        return this;
    }

    @GET
    public LoggerConfig self() {
        return logger;
    }

    @GET
    @Path("category")
    public String getCategory() {
        return logger.getCategory();
    }

    @GET
    @Path("level")
    public LogLevel getLevel() {
        return logger.getLevel();
    }

    @POST
    public Response post(@Context UriInfo uriInfo, @FormParam("action") String action,
            @FormParam("level") LogLevel level) {
        if ("delete".equals(action)) {
            delete();
            return Response.seeOther(Loggers.base(uriInfo)).build();
        }
        container.update(logger.setLevel(level));
        return Response.noContent().build();
    }

    @DELETE
    public void delete() {
        container.remove(logger);
    }

    @Override
    @Logged(level = OFF)
    public String toString() {
        return "LoggerResource:" + logger;
    }
}
