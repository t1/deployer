package com.github.t1.deployer;

import static javax.ws.rs.core.Response.Status.*;
import static javax.xml.bind.DatatypeConverter.*;

import java.io.IOException;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

@Slf4j
public class DeploymentsInfo {
    static final ModelNode READ_DEPLOYMENTS = ModelNode.fromJSONString("{\n" //
            + "    \"address\" : [{\n" //
            + "        \"deployment\" : \"*\"\n" //
            + "    }],\n" //
            + "    \"operation\" : \"read-resource\",\n" //
            + "    \"recursive\" : true\n" //
            + "}");

    private static final OperationMessageHandler LOGGING = new OperationMessageHandler() {
        @Override
        public void handleReport(MessageSeverity severity, String message) {
            switch (severity) {
                case ERROR:
                    log.error(message);
                case WARN:
                    log.warn(message);
                    break;
                case INFO:
                    log.info(message);
                    break;
            }
        }
    };

    @Inject
    VersionsGateway versionsGateway;

    @Inject
    ModelControllerClient client;

    public Deployment getDeploymentByContextRoot(String contextRoot) {
        List<Deployment> deployments = getDeployments();
        for (Deployment deployment : deployments) {
            if (deployment.getContextRoot().equals("/" + contextRoot)) {
                return deployment;
            }
        }
        log.debug("found {} deployments", deployments.size());
        for (Deployment deployment : deployments) {
            log.debug("found deployment {}", deployment);
        }
        log.debug("no deployment found with context root [{}]", contextRoot);
        throw new WebApplicationException(NOT_FOUND);
    }

    public List<Deployment> getDeployments() {
        ModelNode cliDeployments = getCliDeployments();
        String outcome = cliDeployments.get("outcome").asString();
        if (!"success".equals(outcome))
            throw new RuntimeException("outcome " + outcome + ": " + cliDeployments.get("failure-description"));
        return deploymentsIn(cliDeployments.get("result"));
    }

    private List<Deployment> deploymentsIn(ModelNode cliDeploymentsResult) {
        List<Deployment> list = new ArrayList<>();

        for (ModelNode cliDeploymentMatch : cliDeploymentsResult.asList()) {
            ModelNode cliDeployment = cliDeploymentMatch.get("result");
            String contextRoot = getContextRoot(cliDeployment);
            String hash = printHexBinary(cliDeployment.get("content").get(0).get("hash").asBytes());
            Version version = versionsGateway.searchByChecksum(hash);
            log.debug("{} -> {} -> {}", contextRoot, hash, version);
            list.add(new Deployment(contextRoot, version));
        }

        return list;
    }

    private String getContextRoot(ModelNode cliDeployment) {
        ModelNode subsystems = cliDeployment.get("subsystem");
        // JBoss 8 uses 'undertow' while JBoss 7 uses 'web'
        ModelNode web = (subsystems.has("web")) ? subsystems.get("web") : subsystems.get("undertow");
        return web.get("context-root").asString();
    }

    @SneakyThrows(IOException.class)
    private ModelNode getCliDeployments() {
        return client.execute(READ_DEPLOYMENTS, LOGGING);
    }
}
