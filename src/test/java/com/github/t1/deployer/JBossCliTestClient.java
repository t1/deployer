package com.github.t1.deployer;

import java.net.InetAddress;

import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

@Slf4j
public class JBossCliTestClient {
    public static void main(String[] args) throws Exception {
        InetAddress host = InetAddress.getByName("127.0.0.1");
        int port = 9990;
        log.info("connect to JBoss AS on {}:{}", host, port);
        try (ModelControllerClient client = ModelControllerClient.Factory.create(host, port)) {
            ModelNode op = new ModelNode();
            op.get("operation").set("read-resource-description");

            ModelNode address = op.get("address");
            address.add("subsystem", "web");
            address.add("connector", "http");

            op.get("recursive").set(true);
            op.get("operations").set(true);

            ModelNode returnVal = client.execute(op);
            log.info("{}", returnVal.get("result").toString());
        }
    }
}
