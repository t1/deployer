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
    private static final long serialVersionUID = 1L;

    private static final String ARTIFACTORY_URI_PROPERTY = "deployer.artifactory.uri";
    private static final String CONTAINER_URI_PROPERTY = "deployer.container.uri";

    private static final String JBOSS_BASE = System.getProperty("jboss.server.base.dir");
    private static final Path CONFIG_FILE = Paths.get(JBOSS_BASE, "security", "deployer.war", "credentials.properties")
            .toAbsolutePath();

    private static final String SOCKET_BINDING = "jboss.as:socket-binding-group=standard-sockets,socket-binding=";
    private static final ObjectName MANAGEMENT_HTTP = objectName(SOCKET_BINDING + "management-http");
    private static final ObjectName MANAGEMENT_NATIVE = objectName(SOCKET_BINDING + "management-native");

    private static final ObjectName JBOSS_MANAGEMENT = objectName("jboss.as:management-root=server");

    private static ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private Properties properties;

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    @Produces
    ModelControllerClient produceModelControllerClient() throws IOException {
        int boundPort = getBoundPort();
        URI uri = getUriProperty(CONTAINER_URI_PROPERTY, defaultScheme() + "://localhost:" + boundPort);
        log.debug("JBoss AS admin: {}", uri);
        InetAddress host = InetAddress.getByName(uri.getHost());
        int port = uri.getPort();
        log.info("create client to JBoss AS: {}{}:{}", uri.getScheme(), host, port);
        return ModelControllerClient.Factory.create(uri.getScheme(), host, port);
    }

    private String defaultScheme() {
        return getJbossVersion().startsWith("7.") ? "remote" : "http-remoting";
    }

    private String getJbossVersion() {
        try {
            Object jbossVersion = server.getAttribute(JBOSS_MANAGEMENT, "releaseVersion");
            log.debug("found JBoss version {}", jbossVersion);
            return jbossVersion.toString();
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
    }

    private int getBoundPort() {
        Integer value = getBoundPort(MANAGEMENT_NATIVE);
        if (value == null)
            value = getBoundPort(MANAGEMENT_HTTP);
        if (value == null)
            value = 9990;
        return value;
    }

    private Integer getBoundPort(ObjectName objectName) {
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
        if (value != null) {
            log.debug("use property from config file {}: {}", propertyName, value);
        } else {
            value = System.getProperty(propertyName);
            if (value != null) {
                log.debug("use system property {}: {}", propertyName, value);
            } else {
                value = defaultUri;
                log.debug("use default property {}: {}", propertyName, value);
            }
        }
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
