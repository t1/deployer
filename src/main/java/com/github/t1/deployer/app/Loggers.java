package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.LoggerConfig.*;
import static com.github.t1.log.LogLevel.*;

import java.net.URI;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.log.*;

@Logged(level = INFO)
@Path("/loggers")
public class Loggers {
    private static UriBuilder baseBuilder(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(Loggers.class);
    }

    public static URI base(UriInfo uriInfo) {
        return baseBuilder(uriInfo).build();
    }

    public static URI path(UriInfo uriInfo, LoggerConfig logger) {
        return baseBuilder(uriInfo) //
                .path(logger.getCategory()) //
                .build();
    }

    public static URI newLogger(UriInfo uriInfo) {
        return baseBuilder(uriInfo).path(NEW_LOGGER).build();
    }

    @Inject
    Container container;
    @Inject
    Instance<LoggerResource> loggerResources;
    @Context
    UriInfo uriInfo;

    @GET
    public List<LoggerConfig> getAllLoggers() {
        return container.getLoggers();
    }

    @GET
    @Path(NEW_LOGGER)
    public LoggerConfig newLogger() {
        return new LoggerConfig(NEW_LOGGER, "");
    }

    @Path("{loggerName}")
    public LoggerResource getLogger(@PathParam("loggerName") String loggerName) {
        LoggerResource loggerResource = loggerResources.get();
        if (!NEW_LOGGER.equals(loggerName))
            loggerResource.logger(container.getLogger(loggerName));
        return loggerResource;
    }

    @POST
    public Response post(@Context UriInfo uriInfo, //
            @NotNull @FormParam("category") String category, //
            @NotNull @FormParam("level") LogLevel level //
    ) {
        LoggerConfig newLogger = new LoggerConfig(category, level.name());
        container.add(newLogger);
        return Response.seeOther(Loggers.path(uriInfo, newLogger)).build();
    }
}
