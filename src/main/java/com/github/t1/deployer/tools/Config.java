package com.github.t1.deployer.tools;

import static com.github.t1.log.LogLevel.*;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.nio.file.*;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.*;
import javax.management.*;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.jboss.as.controller.client.ModelControllerClient;

import com.github.t1.deployer.repository.Artifactory;
import com.github.t1.log.Logged;

@Slf4j
@Logged(level = TRACE)
@ApplicationScoped
public class Config implements Serializable {
    private static final String MBEAN_PREFIX = "jboss.as:socket-binding-group=standard-sockets,socket-binding=";

    private static final long serialVersionUID = 1L;

    private static final String ARTIFACTORY_URI_PROPERTY = "deployer.artifactory.uri";
    private static final String CONTAINER_URI_PROPERTY = "deployer.container.uri";

    private static final String JBOSS_BASE = System.getProperty("jboss.server.base.dir");
    private static final Path CONFIG_FILE = Paths.get(JBOSS_BASE, "security", "deployer.war", "credentials.properties")
            .toAbsolutePath();

    private Properties properties;

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    @Produces
    ModelControllerClient produceModelControllerClient() throws IOException {
        int boundPort = getBoundPort();
        URI uri = getUriProperty(CONTAINER_URI_PROPERTY, "http-remoting://localhost:" + boundPort);
        log.trace("JBoss AS admin: {}", uri);
        assert "http-remoting".equals(uri.getScheme());
        InetAddress host = InetAddress.getByName(uri.getHost());
        int port = uri.getPort();
        log.info("create client to JBoss AS on {}:{}", host, port);
        return ModelControllerClient.Factory.create(host, port);
    }

    private int getBoundPort() {
        Integer value = getBoundPort("management-native");
        if (value == null)
            value = getBoundPort("management-http");
        if (value == null)
            value = 9990;
        return value;
    }

    private Integer getBoundPort(String name) {
        ObjectName objectName = objectName(name);
        try {
            if (!server.isRegistered(objectName)) {
                log.debug("MBean {} is not registered", objectName);
                return null;
            }
            Object value = server.getAttribute(objectName, "boundPort");
            log.trace("got MBean {} boundPort: {}", objectName, value);
            return (Integer) value;
        } catch (JMException e) {
            log.error("could not get boundPort from " + objectName, e);
            return null;
        }
    }

    private ObjectName objectName(String name) {
        try {
            return new ObjectName(MBEAN_PREFIX + name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    void closeModelControllerClient(@Disposes ModelControllerClient client) throws IOException {
        client.close();
    }

    @Produces
    @Artifactory
    public URI produceArtifactoryUri() {
        return getUriProperty(ARTIFACTORY_URI_PROPERTY, "http://localhost:8081/artifactory");
    }

    private URI getUriProperty(String propertyName, String defaultUri) {
        String value = properties().getProperty(propertyName);
        if (value == null)
            value = System.getProperty(propertyName);
        if (value == null)
            value = defaultUri;
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
