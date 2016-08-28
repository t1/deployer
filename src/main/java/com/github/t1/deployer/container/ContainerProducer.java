package com.github.t1.deployer.container;

import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.ModelControllerClient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.*;
import javax.management.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.*;

import static com.github.t1.log.LogLevel.*;

@Slf4j
@Logged(level = DEBUG)
@ApplicationScoped
public class ContainerProducer implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String SOCKET_BINDING_PREFIX = "management-";
    private static final String SOCKET_BINDING =
            "jboss.as:socket-binding-group=standard-sockets,socket-binding=" + SOCKET_BINDING_PREFIX;
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

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    @Produces
    ModelControllerClient produceModelControllerClient() throws IOException {
        URI uri = getContainerUriFromMBeans();
        log.info("connect to JBoss AS on: {}", uri);
        return createModelControllerClient(uri);
    }

    private URI getContainerUriFromMBeans() {
        ObjectName managementInterface = findManagementInterface();
        if (managementInterface == null)
            throw new RuntimeException("no appropriate MBean found");
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
}
