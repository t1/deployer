package com.github.t1.deployer;

import java.net.InetAddress;

import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

@Slf4j
public class DeployerTest {
    public static void main(String[] args) throws Exception {
        // Model model = read("pom.xml");
        //
        // for (Dependency dependency : model.getDependencies()) {
        // System.out.println(dependency);
        // }
        InetAddress host = InetAddress.getByName("localhost");
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

    // private static Model read(String fileName) {
    // try (FileReader fileReader = new FileReader(fileName)) {
    // return new MavenXpp3Reader().read(fileReader);
    // } catch (IOException | XmlPullParserException e) {
    // throw new RuntimeException(e);
    // }
    // }

    // curl -v "https://artifactory.1and1.org/artifactory/api/search/versions?g=groupid&a=artifactid"
    // {
    // "results" : [
    // {
    // "version" : "1.3.2",
    // "integration" : false
    // },
    // {
    // "version" : "1.3.1",
    // "integration" : false
    // },
    // {
    // "version" : "1.3.0",
    // "integration" : false
    // },
    // {
    // "integration" : true,
    // "version" : "1.2.8-SNAPSHOT"
    // },
    // {
    // "version" : "1.2.7",
    // "integration" : false
    // },
    // {
    // "version" : "1.2.6",
    // "integration" : false
    // }
    // ]
    // }

}
