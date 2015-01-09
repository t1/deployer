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
    private static final String URI_SYSTEM_PROPERTY = "deployer.artifactory.uri";
    private static final long serialVersionUID = 1L;
    private static final String JBOSS_BASE = System.getProperty("jboss.server.base.dir");
    private static final Path CONFIG_FILE = Paths.get(JBOSS_BASE, "security", "deployer.war", "credentials.properties")
            .toAbsolutePath();

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
        String value = properties().getProperty(URI_SYSTEM_PROPERTY);
        if (value == null)
            value = System.getProperty(URI_SYSTEM_PROPERTY);
        if (value == null)
            value = "http://localhost:8081/artifactory";
        return URI.create(value);
    }

    @SneakyThrows(IOException.class)
    private Properties properties() {
        if (properties == null) {
            log.debug("read config from {}", CONFIG_FILE);
            properties = new Properties();
            if (Files.isReadable(CONFIG_FILE))
                properties.load(Files.newInputStream(CONFIG_FILE));
            else
                log.debug("no config file found at {}; use defaults", CONFIG_FILE);
        }
        return properties;
    }

    @Produces
    @Artifactory
    public UsernamePasswordCredentials produceArtifactoryCredentials() {
        String username = properties().getProperty("deployer.artifactory.username");
        if (username == null)
            return null;
        String password = properties().getProperty("deployer.artifactory.password");
        if (password == null)
            return null;
        return new UsernamePasswordCredentials(username, password);
    }
}
