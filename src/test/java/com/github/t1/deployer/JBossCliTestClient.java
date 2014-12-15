package com.github.t1.deployer;

import static org.jboss.as.controller.client.helpers.ClientConstants.*;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.*;

import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

@Slf4j
public class JBossCliTestClient {
    public static void main(String[] args) throws Exception {
        InetAddress host = InetAddress.getByName("127.0.0.1");
        int port = 9990;
        log.info("connect to JBoss AS on {}:{}", host, port);

        try (ModelControllerClient client = ModelControllerClient.Factory.create(host, port)) {
            ModelNode composite = ModelNode.fromJSONString("{\n" //
                    + "    \"operation\" : \"composite\",\n" //
                    + "    \"address\" : [],\n" //
                    + "    \"steps\" : [\n" //
                    // + "        {\n" //
                    // + "            \"operation\" : \"add\",\n" //
                    // + "            \"address\" : [{\n" //
                    // + "                \"deployment\" : \"foo.war\"\n" //
                    // + "            }],\n" //
                    // + "            \"runtime-name\" : \"foo.war\",\n" //
                    // + "            \"content\" : [{\"input-stream-index\" : 0}]\n" //
                    // + "        },\n" //
                    // + "        {\n" //
                    // + "            \"operation\" : \"deploy\",\n" //
                    // + "            \"address\" : [{\n" //
                    // + "                \"deployment\" : \"foo.war\"\n" //
                    // + "            }]\n" //
                    // + "        },\n" //
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
            // ModelNode composite = new ModelNode();
            // composite.get(OP).set(COMPOSITE);
            // composite.get(OP_ADDR).setEmptyList();
            // ModelNode steps = composite.get(STEPS);
            // steps.setEmptyList();
            // composite.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(true);
            //
            // ModelNode step = new ModelNode();
            // configureDeploymentOperation(step, ADD, "foo.war");
            // step.get(RUNTIME_NAME).set("foo.war");
            // step.get(CONTENT).get(0).get(INPUT_STREAM_INDEX).set(0);
            // steps.add(step);
            //
            // step = new ModelNode();
            // configureDeploymentOperation(step, DEPLOYMENT_DEPLOY_OPERATION, "foo.war");
            // steps.add(step);
            //
            // step = new ModelNode();
            // step.get(OP).set(DEPLOYMENT_FULL_REPLACE_OPERATION);
            // step.get(OP_ADDR).setEmptyList();
            // step.get(NAME).set("foo.war");
            // step.get(RUNTIME_NAME).set("foo.war");
            // // builder.addFileAsAttachment(new File("foo.war"));
            // step.get(CONTENT).get(0).get(INPUT_STREAM_INDEX).set(0);
            // steps.add(step);

            // System.out.println("--------------------");
            // System.out.println(composite.toJSONString(false));
            // System.out.println("--------------------");

            OperationBuilder builder = new OperationBuilder(composite);
            builder.addInputStream(Files.newInputStream(Paths.get("foo.war")));

            execute(client, builder.build());
        }
    }

    private static void execute(ModelControllerClient client, Operation operation) throws IOException {
        log.info("operation {}", operation);
        ModelNode returnVal = client.execute(operation);
        log.info("-> {}", returnVal);
    }

    private static void configureDeploymentOperation(ModelNode op, String operationName, String uniqueName) {
        op.get(OP).set(operationName);
        op.get(OP_ADDR).add(DEPLOYMENT, uniqueName);
    }
}
