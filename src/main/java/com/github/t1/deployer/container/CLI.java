package com.github.t1.deployer.container;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.*;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.dmr.ModelNode;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.jboss.as.controller.client.helpers.ClientConstants.*;
import static org.jboss.as.controller.client.helpers.Operations.*;
import static org.wildfly.plugin.core.ServerHelper.*;

@Slf4j
public class CLI {
    private static final int STARTUP_TIMEOUT = 30;

    private static final OperationMessageHandler LOGGING = (severity, message) -> {
        switch (severity) {
        case ERROR:
            log.error(message);
        case WARN:
            log.warn(message);
            break;
        case INFO:
            log.info(message);
            break;
        }
    };

    @Inject ModelControllerClient client;


    @SneakyThrows({ InterruptedException.class, TimeoutException.class })
    public void waitForBoot() { waitForStandalone(client, STARTUP_TIMEOUT); }

    public ModelNode writeAttribute(ModelNode address, String name, String value) {
        return execute(createWriteAttributeOperation(address, name, value));
    }

    public ModelNode writeAttribute(ModelNode address, String name, boolean value) {
        return execute(createWriteAttributeOperation(address, name, value));
    }


    public ModelNode mapPut(ModelNode address, String name, String key, String value) {
        ModelNode request = createOperation("map-put", address);
        request.get("name").set(name);
        request.get("key").set(key);
        request.get("value").set(value);

        return execute(request);
    }

    public ModelNode mapRemove(ModelNode address, String name, String key) {
        ModelNode request = createOperation("map-remove", address);
        request.get("name").set(name);
        request.get("key").set(key);

        return execute(request);
    }


    public ModelNode execute(ModelNode request) {
        ModelNode result = executeRaw(request);
        checkOutcome(result);
        return result.get("result");
    }

    @SneakyThrows(IOException.class)
    public ModelNode executeRaw(ModelNode command) {
        log.debug("execute command {}", command);
        ModelNode result = client.execute(command, LOGGING);
        log.debug("response {}", result);
        return result;
    }

    public void checkOutcome(ModelNode result) {
        if (!isSuccessfulOutcome(result))
            fail(result);
    }

    public boolean fail(ModelNode result) {
        throw new RuntimeException("outcome " + result.get("outcome")
                + (result.hasDefined(FAILURE_DESCRIPTION) ? ": " + result.get(FAILURE_DESCRIPTION) : ""));
    }

    public static boolean isNotFoundMessage(ModelNode result) {
        String message = result.get("failure-description").toString();
        boolean jboss7start = message.startsWith("\"JBAS014807: Management resource");
        boolean jboss8start = message.startsWith("\"WFLYCTL0216: Management resource");
        boolean notFoundEnd = message.endsWith(" not found\"");
        boolean isNotFound = (jboss7start || jboss8start) && notFoundEnd;
        log.trace("is not found message: jboss7start:{} jboss8start:{} notFoundEnd:{} -> {}: [{}]", //
                jboss7start, jboss8start, notFoundEnd, isNotFound, message);
        return isNotFound;
    }

    public ServerDeploymentManager openServerDeploymentManager() {
        return ServerDeploymentManager.Factory.create(client);
    }
}
