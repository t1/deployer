package com.github.t1.deployer.app;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.nio.file.*;

import static javax.ws.rs.core.Response.Status.*;

@Path("/api")
@Slf4j
public class DeployerBoundary {
    private static final String ROOT_DEPLOYER_CONFIG = "root.deployer.config";

    @Inject
    Deployer deployer;

    @POST
    public Response post() {
        java.nio.file.Path root = Paths.get(System.getProperty("jboss.server.config.dir"), ROOT_DEPLOYER_CONFIG);

        if (!Files.isRegularFile(root))
            return Response.status(NOT_FOUND).entity(ROOT_DEPLOYER_CONFIG + " not found").build();

        log.debug("load config plan from: {}", root);
        ConfigurationPlan plan = ConfigurationPlan.load(root);

        deployer.run(plan);

        return Response.noContent().build();
    }
}
