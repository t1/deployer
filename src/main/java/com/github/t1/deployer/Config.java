package com.github.t1.deployer;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.*;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.jboss.as.controller.client.ModelControllerClient;

import com.github.t1.log.Logged;

@Slf4j
@Logged
@ApplicationScoped
public class Config implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Path PATH = Paths.get("artifactory.properties").toAbsolutePath();

    private Properties properties;

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

    @Produces
    @Artifactory
    public URI produceArtifactoryUri() {
        return URI.create(properties().getProperty("deployer.artifactory.uri", "http://localhost:8081/artifactory"));
    }

    @SneakyThrows(IOException.class)
    private Properties properties() {
        if (properties == null) {
            log.debug("read config from {}", PATH);
            properties = new Properties();
            properties.load(Files.newInputStream(PATH));
        }
        return properties;
    }

    @Produces
    @Artifactory
    public UsernamePasswordCredentials produceArtifactoryCredentials() {
        return new UsernamePasswordCredentials( //
                properties().getProperty("deployer.artifactory.username"), //
                properties().getProperty("deployer.artifactory.password"));
    }
}
