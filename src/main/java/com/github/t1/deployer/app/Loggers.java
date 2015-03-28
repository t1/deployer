package com.github.t1.deployer.app;

import static com.github.t1.log.LogLevel.*;
import static javax.ws.rs.core.MediaType.*;

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

    public static URI newForm(UriInfo uriInfo) {
        return baseBuilder(uriInfo).path("/new").build();
    }

    @Inject
    Container container;
    @Inject
    Instance<LoggerResource> loggerResources;
    @Inject
    Instance<NewLoggerFormHtmlWriter> htmlForms;
    @Context
    UriInfo uriInfo;

    @GET
    @Path("/new")
    @Produces(TEXT_HTML)
    public String getNewLoggerForm() {
        return htmlForms.get().uriInfo(uriInfo).toString();
    }

    @GET
    public List<LoggerConfig> getAllLoggers() {
        return container.getLoggers();
    }

    @Path("{loggerName}")
    public LoggerResource getLogger(@PathParam("loggerName") String loggerName) {
        return loggerResources.get().logger(container.getLogger(loggerName));
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
