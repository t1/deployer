package com.github.t1.deployer.tools;

import static com.github.t1.deployer.model.Config.ContainerConfig.*;
import static com.github.t1.deployer.model.Config.DeploymentListFileConfig.*;
import static com.github.t1.deployer.model.Config.RepositoryConfig.*;
import static com.github.t1.log.LogLevel.*;
import static com.github.t1.rest.RestContext.*;
import static com.github.t1.rest.fallback.JsonMessageBodyReader.*;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.nio.file.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.*;
import javax.management.*;

import org.jboss.as.controller.client.ModelControllerClient;

import com.github.t1.deployer.model.Config;
import com.github.t1.deployer.model.Config.*;
import com.github.t1.deployer.repository.Artifactory;
import com.github.t1.log.Logged;
import com.github.t1.rest.*;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Logged(level = DEBUG)
@ApplicationScoped
public class ConfigProducer implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String REPOSITORY_URI_PROPERTY = "deployer.repository.uri";
    private static final String CONTAINER_URI_PROPERTY = "deployer.container.uri";

    private static final String JBOSS_BASE = System.getProperty("jboss.server.base.dir");
    static Path CONFIG_FILE = Paths.get(JBOSS_BASE, "configuration", "deployer.war", "config.json").toAbsolutePath();

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

    private Config config;

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    @Produces
    Config produceConfig() {
        return config();
    }

    @Produces
    ModelControllerClient produceModelControllerClient() throws IOException {
        URI uri = config().container().uri();
        if (uri == null)
            uri = getContainerUriFromMBeans();
        if (uri == null)
            throw new RuntimeException("no container configured and no appropriate MBean found");
        log.info("connect to JBoss AS on: {}", uri);
        return createModelControllerClient(uri);
    }

    private URI getContainerUriFromMBeans() {
        ObjectName managementInterface = findManagementInterface();
        if (managementInterface == null)
            return null;
        return URI.create(boundScheme(managementInterface) //
                + "://" + getAttribute(managementInterface, "boundAddress", "localhost") //
                + ":" + getAttribute(managementInterface, "boundPort", "9990") //
        );
    }

    private ObjectName findManagementInterface() {
        for (ObjectName objectName : MANAGEMENT_INTERFACES)
            if (server.isRegistered(objectName)) {
                if ("true".equals(getAttribute(objectName, "bound", "false"))) {
                    log.trace("found registered and bound management interface {}", objectName);
                    return objectName;
                } else
                    log.trace("management interface {} is not bound", objectName);
            } else
                log.trace("management interface {} is not registered", objectName);
        return null;
    }

    private String boundScheme(ObjectName managementInterface) {
        log.trace("management interface: {}", managementInterface.getCanonicalName());
        String socketBinding = managementInterface.getKeyProperty("socket-binding");
        log.trace("socket binding: {}", socketBinding);
        if (socketBinding.startsWith(SOCKET_BINDING_PREFIX))
            socketBinding = socketBinding.substring(SOCKET_BINDING_PREFIX.length());
        return (socketBinding.equals("native")) ? "remote" : socketBinding + "-remoting";
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
        String host = uri.getHost();
        int port = uri.getPort();
        log.debug("create ModelControllerClient {}://{}:{}", uri.getScheme(), host, port);
        return ModelControllerClient.Factory.create(uri.getScheme(), host, port);
    }

    void disposeModelControllerClient(@Disposes ModelControllerClient client) throws IOException {
        client.close();
    }

    @Produces
    @Artifactory
    public RestContext produceRepositoryRestContext() {
        RestContext rest = REST;
        URI baseUri = config().repository().uri();
        if (baseUri == null)
            baseUri = URI.create("http://localhost:8081/artifactory");
        rest = rest.register("repository", baseUri);
        Credentials credentials = getRepositoryCredentials();
        if (credentials != null) {
            log.debug("put {} credentials for {}", credentials.userName(), baseUri);
            rest = rest.register(baseUri, credentials);
        }
        return rest;
    }

    @SneakyThrows(IOException.class)
    private Config config() {
        if (config == null)
            if (CONFIG_FILE != null && Files.isReadable(CONFIG_FILE)) {
                log.debug("read config from {}", CONFIG_FILE);
                config = MAPPER.readValue(Files.newBufferedReader(CONFIG_FILE), Config.class);
            } else {
                log.debug("no config file found at {}; use defaults", CONFIG_FILE);
                config = Config.config() //
                        .container(container() //
                                .uri(getUriSystemProperty(CONTAINER_URI_PROPERTY)) //
                                .build()) //
                        .repository(repository() //
                                .uri(getUriSystemProperty(REPOSITORY_URI_PROPERTY)) //
                                // no authorization from system properties...
                                // not secure
                                .build()) //
                        .deploymentListFileConfig(deploymentListFileConfig() //
                                .autoUndeploy(false) //
                                .build()) //
                        .build();
            }
        return config;
    }

    private URI getUriSystemProperty(String name) {
        if (System.getProperty(name) == null)
            return null;
        return URI.create(System.getProperty(name));
    }

    private Credentials getRepositoryCredentials() {
        Authentication authentication = config().repository().authentication();
        if (authentication == null)
            return null;
        String username = authentication.username();
        if (username == null)
            return null;
        String password = authentication.password();
        if (password == null)
            return null;
        return new Credentials(username, password);
    }

    public DeploymentListFileConfig produceDeploymentListFileConfig() {
        return config().deploymentListFileConfig();
    }
}
