package com.github.t1.deployer;

import static javax.ws.rs.core.Response.Status.*;
import static javax.xml.bind.DatatypeConverter.*;

import java.io.*;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

import com.github.t1.log.Logged;

@Slf4j
@Logged
public class DeploymentsContainer {
    static final ModelNode READ_DEPLOYMENTS = ModelNode.fromJSONString("{\n" //
            + "    \"address\" : [{\n" //
            + "        \"deployment\" : \"*\"\n" //
            + "    }],\n" //
            + "    \"operation\" : \"read-resource\",\n" //
            + "    \"recursive\" : true\n" //
            + "}");
    static final ModelNode ADD = ModelNode.fromJSONString("{\n" //
            + "    \"operation\" : \"composite\",\n" //
            + "    \"address\" : [],\n" //
            + "    \"steps\" : [\n" //
            + "        {\n" //
            + "            \"operation\" : \"add\",\n" //
            + "            \"address\" : [{\n" //
            + "                \"deployment\" : \"foo.war\"\n" //
            + "            }],\n" //
            + "            \"runtime-name\" : \"foo.war\",\n" //
            + "            \"content\" : [{\"input-stream-index\" : 0}]\n" //
            + "        },\n" //
            + "        {\n" //
            + "            \"operation\" : \"deploy\",\n" //
            + "            \"address\" : [{\n" //
            + "                \"deployment\" : \"foo.war\"\n" //
            + "            }]\n" //
            + "        }\n" //
            + "    ],\n" //
            + "    \"operation-headers\" : {\"rollback-on-runtime-failure\" : true}\n" //
            + "}\n" //
    );
    static final ModelNode FULL_REPLACE = ModelNode.fromJSONString("{\n" //
            + "    \"operation\" : \"composite\",\n" //
            + "    \"address\" : [],\n" //
            + "    \"steps\" : [\n" //
            + "        {\n" //
            + "            \"operation\" : \"full-replace-deployment\",\n" //
            + "            \"address\" : [],\n" //
            + "            \"name\" : \"foo.war\",\n" //
            + "            \"runtime-name\" : \"foo.war\",\n" //
            + "            \"content\" : [{\"input-stream-index\" : 0}]\n" //
            + "        }\n" //
            + "    ],\n" //
            + "    \"operation-headers\" : {\"rollback-on-runtime-failure\" : true}\n" //
            + "}\n" //
    );
    static final ModelNode UNDEPLOY = ModelNode.fromJSONString("{\n" //
            + "    \"operation\" : \"composite\",\n" //
            + "    \"address\" : [],\n" //
            + "    \"steps\" : [\n" //
            + "        {\n" //
            + "            \"operation\" : \"undeploy\",\n" //
            + "            \"address\" : [{\n" //
            + "                \"deployment\" : \"foo.war\"\n" //
            + "            }],\n" //
            + "        },\n" //
            + "        {\n" //
            + "            \"operation\" : \"remove\",\n" //
            + "            \"address\" : [{\n" //
            + "                \"deployment\" : \"foo.war\"\n" //
            + "            }]\n" //
            + "        }\n" //
            + "    ],\n" //
            + "    \"operation-headers\" : {\"rollback-on-runtime-failure\" : true}\n" //
            + "}\n" //
    );

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
    ModelControllerClient client;

    @Inject
    VersionsGateway versionsGateway;

    @SneakyThrows(IOException.class)
    private ModelNode execute(ModelNode command) {
        log.debug("execute command {}", command);
        ModelNode result = client.execute(command, LOGGING);
        log.debug("-> {}", result);
        return result;
    }

    @SneakyThrows(IOException.class)
    private ModelNode execute(Operation operation) {
        log.debug("execute operation {}", operation.getOperation());
        ModelNode result = client.execute(operation, LOGGING);
        log.debug("-> {}", result);
        return result;
    }

    private void checkOutcome(ModelNode result) {
        String outcome = result.get("outcome").asString();
        if (!"success".equals(outcome))
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
    }

    public Deployment getDeploymentByContextRoot(String contextRoot) {
        List<Deployment> deployments = getAllDeployments();
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

    public List<Deployment> getAllDeployments() {
        ModelNode result = execute(READ_DEPLOYMENTS);
        checkOutcome(result);
        return deploymentsIn(result.get("result"));
    }

    private List<Deployment> deploymentsIn(ModelNode cliDeploymentsResult) {
        List<Deployment> list = new ArrayList<>();

        for (ModelNode cliDeploymentMatch : cliDeploymentsResult.asList()) {
            ModelNode cliDeployment = cliDeploymentMatch.get("result");
            String name = cliDeployment.get("name").asString();
            String contextRoot = getContextRoot(cliDeployment);
            String hash = printHexBinary(cliDeployment.get("content").get(0).get("hash").asBytes());
            log.debug("{} -> {} -> {}", name, contextRoot, hash);
            list.add(new Deployment(this, versionsGateway, name, contextRoot, hash));
        }

        return list;
    }

    private String getContextRoot(ModelNode cliDeployment) {
        ModelNode subsystems = cliDeployment.get("subsystem");
        // JBoss 8 uses 'undertow' while JBoss 7 uses 'web'
        ModelNode web = (subsystems.has("web")) ? subsystems.get("web") : subsystems.get("undertow");
        return web.get("context-root").asString();
    }

    public void deploy(String contextRoot, InputStream deployment) {
        OperationBuilder builder = new OperationBuilder(FULL_REPLACE);
        builder.addInputStream(deployment);

        ModelNode result = execute(builder.build());

        checkOutcome(result);
    }

    public void undeploy(String contextRoot) {
        ModelNode result = execute(UNDEPLOY);
        checkOutcome(result);
    }
}
