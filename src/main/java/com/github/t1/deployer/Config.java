package com.github.t1.deployer;

import java.io.IOException;
import java.net.*;

import javax.enterprise.inject.*;

import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.ModelControllerClient;

import com.github.t1.log.Logged;

@Slf4j
@Logged
public class Config {
    @Produces
    ModelControllerClient produceModelControllerClient() throws IOException {
        InetAddress host = InetAddress.getByName("localhost");
        int port = 9999;
        log.info("create client to JBoss AS on {}:{}", host, port);
        return ModelControllerClient.Factory.create(host, port);
    }

    void closeModelControllerClient(@Disposes ModelControllerClient client) throws IOException {
        client.close();
    }

    private static final String SYSTEM_PROPERTY_NAME = "deployer.artifactory.uri";
    private static final String DEFAULT_URI = "http://localhost:8081/artifactory";

    @Produces
    @Artifactory
    public URI produceArtifactoryUri() {
        return URI.create(System.getProperty(SYSTEM_PROPERTY_NAME, DEFAULT_URI));
    }
}
