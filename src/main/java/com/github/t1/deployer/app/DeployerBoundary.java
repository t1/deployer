package com.github.t1.deployer.app;

import com.github.t1.deployer.container.AuditLog;
import com.github.t1.deployer.container.AuditLog.Watcher;
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
    public static final String ROOT_DEPLOYER_CONFIG = "root.deployer.config";

    public static java.nio.file.Path getConfigPath() {
        return Paths.get(System.getProperty("jboss.server.config.dir"), ROOT_DEPLOYER_CONFIG);
    }

    @Inject Deployer deployer;
    @Inject AuditLog auditLog;

    @POST
    public Response post() {
        java.nio.file.Path root = getConfigPath();

        if (!Files.isRegularFile(root))
            return Response.status(NOT_FOUND).entity(ROOT_DEPLOYER_CONFIG + " not found").build();

        log.debug("load config plan from: {}", root);

        try (Watcher audits = auditLog.watching()) {
            deployer.run(root);

            return Response.ok(audits.getAudits()).build();
        }
    }
}
