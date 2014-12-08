package com.github.t1.deployer;

import static javax.xml.bind.DatatypeConverter.*;

import java.io.IOException;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

@Slf4j
public class DeploymentsInfo {
    static final ModelNode READ_DEPLOYMENTS = ModelNode.fromJSONString("{" //
            + "\"operation\" : \"read-resource\"," //
            + "\"address\" : [{" //
            + "\"/deployment\" : \"*\"" //
            + "}]," //
            + "\"recursive\" : true" //
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
        Optional<Deployment> optional = getDeployments().stream() //
                .filter((d) -> d.getContextRoot().equals("/" + contextRoot)) //
                .findAny();
        return optional.orElseThrow(() -> {
            return new NotFoundException("deployment with context root [" + contextRoot + "]");
        });
    }

    public List<Deployment> getDeployments() {
        if (versionsGateway != null)
            return Arrays.asList( //
                    new Deployment("/foo", new Version("1.3.1")), //
                    new Deployment("/bar", new Version("0.3")) //
                    );
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
            String contextRoot = cliDeployment.get("subsystem").get("undertow").get("context-root").asString();
            String hash = printHexBinary(cliDeployment.get("content").get(0).get("hash").asBytes());
            Version version = versionsGateway.searchByChecksum(hash);
            log.info("{} -> {} -> {}", contextRoot, hash, version);
            list.add(new Deployment(contextRoot, version));
        }

        return list;
    }

    @SneakyThrows(IOException.class)
    private ModelNode getCliDeployments() {
        return client.execute(READ_DEPLOYMENTS, LOGGING);
    }
}
