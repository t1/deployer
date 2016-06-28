package com.github.t1.deployer.app;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.nio.file.*;
import java.util.List;

import static java.lang.String.*;
import static java.util.stream.Collectors.*;
import static javax.ws.rs.core.Response.Status.*;

@Path("/api")
@Slf4j
public class DeployerBoundary {
    public static final String ROOT_DEPLOYER_CONFIG = "root.deployer.config";

    public static java.nio.file.Path getConfigPath() {
        return Paths.get(System.getProperty("jboss.server.config.dir"), ROOT_DEPLOYER_CONFIG);
    }

    @Inject Deployer deployer;

    @POST
    public Response post() {
        java.nio.file.Path root = getConfigPath();

        if (!Files.isRegularFile(root))
            return Response.status(NOT_FOUND).entity(ROOT_DEPLOYER_CONFIG + " not found").build();

        log.debug("load config plan from: {}", root);

        List<Audit> audits = deployer.run(root).asList();

        if (log.isDebugEnabled())
            log.debug("deployed:\n- {}", join("\n- ", audits.stream().map(Audit::toString).collect(toList())));

        return Response.ok(audits).build();
    }
}
