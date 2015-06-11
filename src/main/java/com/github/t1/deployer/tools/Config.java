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
@Logged(level = DEBUG)
@ApplicationScoped
public class Config implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String ARTIFACTORY_URI_PROPERTY = "deployer.artifactory.uri";
    private static final String CONTAINER_URI_PROPERTY = "deployer.container.uri";

    private static final String JBOSS_BASE = System.getProperty("jboss.server.base.dir");
    private static final Path CONFIG_FILE = Paths.get(JBOSS_BASE, "security", "deployer.war", "credentials.properties")
            .toAbsolutePath();

    private static final String SOCKET_BINDING_PREFIX = "management-";
    private static final String SOCKET_BINDING = "jboss.as:socket-binding-group=standard-sockets,socket-binding="
            + SOCKET_BINDING_PREFIX;
    private static final ObjectName[] MANAGEMENT_INTERFACES = { //
            objectName(SOCKET_BINDING + "native"), //
                    objectName(SOCKET_BINDING + "https"), //
                    objectName(SOCKET_BINDING + "http"), //
            };

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
        URI uri = getUriProperty(CONTAINER_URI_PROPERTY, defaultUri());
        log.info("connect to JBoss AS on: {}", uri);
        return createModelControllerClient(uri);
    }

    private String defaultUri() {
        ObjectName managementInterface = findManagementInterface();
        return boundScheme(managementInterface) //
                + "://" + getAttribute(managementInterface, "boundAddress", "localhost") //
                + ":" + getAttribute(managementInterface, "boundPort", "9990");
    }

    private String boundScheme(ObjectName managementInterface) {
        log.trace("management interface: {}", managementInterface.getCanonicalName());
        String socketBinding = managementInterface.getKeyProperty("socket-binding");
        log.trace("socket binding: {}", socketBinding);
        if (socketBinding.startsWith(SOCKET_BINDING_PREFIX))
            socketBinding = socketBinding.substring(SOCKET_BINDING_PREFIX.length());
        return (socketBinding.equals("native")) ? "remote" : socketBinding;
    }

    private ObjectName findManagementInterface() {
        for (ObjectName objectName : MANAGEMENT_INTERFACES) {
            if (server.isRegistered(objectName)) {
                if ("true".equals(getAttribute(objectName, "bound", "false"))) {
                    log.trace("found registered and bound management interface {}", objectName);
                    return objectName;
                } else {
                    log.trace("management interface {} is not bound", objectName);
                }
            } else {
                log.trace("management interface {} is not registered", objectName);
            }
        }
        return null;
    }

    private String getAttribute(ObjectName objectName, String attributeName, String defaultValue) {
        try {
            Object value = server.getAttribute(objectName, attributeName);
            if (value == null)
                return defaultValue;
            log.trace("{}#{} = {}", objectName, attributeName, value);
            return value.toString();
        } catch (JMException e) {
            log.error("could not get " + attributeName + " from " + objectName, e);
            return null;
        }
    }

    private ModelControllerClient createModelControllerClient(URI uri) throws UnknownHostException {
        InetAddress host = InetAddress.getByName(uri.getHost());
        int port = uri.getPort();
        log.debug("create ModelControllerClient {}://{}:{}", uri.getScheme(), host, port);
        return ModelControllerClient.Factory.create(uri.getScheme(), host, port);
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
