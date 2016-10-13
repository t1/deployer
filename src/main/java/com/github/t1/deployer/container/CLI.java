package com.github.t1.deployer.container;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.*;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.dmr.ModelNode;

import javax.inject.Inject;
import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
public class CLI {
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


    public static ModelNode readResource(ModelNode request) {
        request.get("operation").set("read-resource");
        request.get("recursive").set(true);
        return request;
    }

    public ModelNode writeAttribute(ModelNode request, String name, String value) {
        return writeAttribute(request, name, node -> node.set(value));
    }

    public ModelNode writeAttribute(ModelNode request, String name, boolean value) {
        return writeAttribute(request, name, node -> node.set(value));
    }

    private ModelNode writeAttribute(ModelNode request, String name, Consumer<ModelNode> setValue) {
        try {
            request.get("operation").set("write-attribute");
            request.get("name").set(name);
            setValue.accept(request.get("value"));

            return execute(request);
        } catch (RuntimeException e) {
            throw new RuntimeException("can't write [" + request.get("address") + "][" + name + "]", e);
        }
    }


    public ModelNode mapPut(ModelNode request, String name, String key, String value) {
        request.get("operation").set("map-put");
        request.get("name").set(name);
        request.get("key").set(key);
        request.get("value").set(value);

        return execute(request);
    }


    public ModelNode mapRemove(ModelNode request, String name, String key) {
        request.get("operation").set("map-remove");
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
        String outcome = result.get("outcome").asString();
        if (!"success".equals(outcome))
            fail(result);
    }

    public boolean fail(ModelNode result) {
        throw new RuntimeException("outcome " + result.get("outcome")
                + (result.hasDefined("failure-description") ? ": " + result.get("failure-description") : ""));
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
