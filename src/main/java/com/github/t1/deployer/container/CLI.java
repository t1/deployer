package com.github.t1.deployer.container;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.jboss.as.controller.client.helpers.ClientConstants.*;
import static org.jboss.as.controller.client.helpers.Operations.*;
import static org.wildfly.plugin.core.ServerHelper.*;

@Slf4j
class CLI {
    private static final boolean DEBUG = Boolean.getBoolean(CLI.class.getName() + "#DEBUG");
    private static final int STARTUP_TIMEOUT = 30;

    private static final OperationMessageHandler LOGGING = (severity, message) -> {
        switch (severity) {
        case ERROR:
            log.error(message);
            break;
        case WARN:
            log.warn(message);
            break;
        case INFO:
            log.info(message);
            break;
        }
    };

    @Inject public ModelControllerClient client;

    private CompositeOperationBuilder batch;


    @SneakyThrows({ InterruptedException.class, TimeoutException.class })
    public void waitForBoot() {
        log.info("wait for boot");
        waitForStandalone(client, STARTUP_TIMEOUT);
        log.info("boot done");
    }


    public <T> void writeAttr(ModelNode address, String name, BiFunction<ModelNode, T, ModelNode> set, T value) {
        ModelNode request = createOperation(WRITE_ATTRIBUTE_OPERATION, address);
        request.get(NAME).set(name);
        if (value != null)
            set.apply(request.get(VALUE), value);
        execute(request);
    }


    public void writeProperty(ModelNode address, String key, String value) {
        ModelNode request = createOperation("map-put", address);
        request.get("name").set("property");
        request.get("key").set(key);
        request.get("value").set(value);

        execute(request);
    }

    public void removeProperty(ModelNode address, String key) {
        ModelNode request = createOperation("map-remove", address);
        request.get("name").set("property");
        request.get("key").set(key);

        execute(request);
    }


    public Stream<ModelNode> readResource(ModelNode address) {
        ModelNode result = executeRaw(createReadResourceOperation(address, true));
        checkOutcome(result);
        return result.get("result").asList().stream();
    }

    public void execute(ModelNode request) {
        if (batch != null) {
            batch.addStep(request);
        } else {
            ModelNode result = executeRaw(request);
            checkOutcome(result);
        }
    }

    @SneakyThrows(IOException.class)
    public ModelNode executeRaw(ModelNode command) {
        logCli("execute command {}", command);
        ModelNode result = client.execute(command, LOGGING);
        logCli("response {}", result);
        return result;
    }

    @SneakyThrows(IOException.class)
    public void execute(Operation operation) {
        if (batch != null) {
            batch.addStep(operation.getOperation());
            operation.getInputStreams().forEach(inputStream -> batch.addInputStream(inputStream));
            batch.setAutoCloseStreams(operation.isAutoCloseStreams());
        } else {
            logCli("execute operation {}", operation.getOperation());
            ModelNode result = client.execute(operation, LOGGING);
            logCli("response {}", result);
            checkOutcome(result);
        }
    }

    public void checkOutcome(ModelNode result) {
        if (!isSuccessfulOutcome(result))
            fail(result);
    }

    public void fail(ModelNode result) {
        throw new RuntimeException("outcome " + result.get("outcome")
                + (result.hasDefined(FAILURE_DESCRIPTION) ? ": " + result.get(FAILURE_DESCRIPTION) : ""));
    }

    public static boolean isNotFoundMessage(ModelNode result) {
        String message = result.get("failure-description").toString();
        boolean jboss7start = message.startsWith("\"JBAS014807: Management resource");
        boolean jboss8start = message.startsWith("\"WFLYCTL0216: Management resource");
        boolean notFoundEnd = message.endsWith(" not found\"");
        boolean isNotFound = (jboss7start || jboss8start) && notFoundEnd;
        log.trace("is not found message: jboss7start:{} jboss8start:{} notFoundEnd:{} -> {}: [{}]",
                jboss7start, jboss8start, notFoundEnd, isNotFound, message);
        return isNotFound;
    }

    /** see debug-cli in reference.md */
    private void logCli(String format, Object arg) {
        if (DEBUG)
            log.debug(format, arg);
    }

    public void startBatch() {
        if (this.batch != null)
            throw new IllegalStateException("already started a batch");
        this.batch = CompositeOperationBuilder.create();
    }

    @SneakyThrows(IOException.class)
    public void commitBatch() {
        if (this.batch == null)
            throw new IllegalStateException("no batch started");
        Operation operation = batch.build();
        assert operation.getOperation().has(STEPS);
        if (operation.getOperation().get(STEPS).has(0)) {
            log.debug("execute batch");
            ModelNode result = client.execute(operation, LOGGING);
            logCli("response {}", result);
            checkOutcome(result);
        } else {
            log.debug("no batch to execute");
        }
        this.batch = null;
    }

    public void rollbackBatch() {
        if (this.batch == null)
            throw new IllegalStateException("no batch started");
        this.batch = null;
    }
}
