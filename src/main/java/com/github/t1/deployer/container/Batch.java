package com.github.t1.deployer.container;

import com.github.t1.deployer.model.ProcessState;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.github.t1.deployer.container.Container.*;
import static com.github.t1.deployer.model.ProcessState.*;
import static com.github.t1.problem.WebException.*;
import static java.util.Locale.*;
import static org.jboss.as.controller.client.helpers.ClientConstants.*;
import static org.jboss.as.controller.client.helpers.Operations.*;
import static org.wildfly.plugin.core.ServerHelper.*;

@Slf4j
@RequestScoped
class Batch {
    private static int nextId = 0;
    private final int id = nextId++;

    private static final boolean DEBUG = Boolean.getBoolean(CLI_DEBUG);
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

    @Inject ModelControllerClient client;

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
        checkResponse(result);
        return result.get("result").asList().stream();
    }

    public void execute(ModelNode request) {
        assert batch != null : "batch " + id + " not started";

        batch.addStep(request);
    }

    @SneakyThrows(IOException.class)
    public ModelNode executeRaw(ModelNode command) {
        logCli("execute command {}", command);
        ModelNode result = client.execute(command, LOGGING);
        logCli("response {}", result);
        return result;
    }

    public void execute(Operation operation) {
        assert batch != null : "batch " + id + " not started";
        assert COMPOSITE.equals(operation.getOperation().get(OP).asString());
        assert operation.getOperation().get(ADDRESS).asList().isEmpty();

        operation.getOperation().get(STEPS).asList().forEach(step -> batch.addStep(step));
        operation.getInputStreams().forEach(inputStream -> batch.addInputStream(inputStream));
        batch.setAutoCloseStreams(operation.isAutoCloseStreams());
    }

    public ProcessState checkResponse(ModelNode result) {
        if (!isSuccessfulOutcome(result))
            fail(result);

        String processState = processState(result);
        if (processState == null)
            return running;
        switch (processState) {
        case CONTROLLER_PROCESS_STATE_RUNNING:
            return running;
        case CONTROLLER_PROCESS_STATE_RELOAD_REQUIRED:
            return reloadRequired;
        case CONTROLLER_PROCESS_STATE_RESTART_REQUIRED:
            return restartRequired;
        case CONTROLLER_PROCESS_STATE_STARTING:
            throw badRequest("unexpectedly still starting");
        case CONTROLLER_PROCESS_STATE_STOPPING:
            throw badRequest("unexpectedly already stopping");
        }
        throw badRequest("unexpected process-state: " + processState);
    }

    public void fail(ModelNode result) {
        String detail = "outcome " + result.get("outcome")
                + (result.hasDefined(FAILURE_DESCRIPTION) ? ": " + result.get(FAILURE_DESCRIPTION) : "");
        throw badRequest(detail);
    }

    private String processState(ModelNode result) {
        if (!result.has(RESPONSE_HEADERS))
            return null;
        ModelNode headers = result.get(RESPONSE_HEADERS);
        if (!headers.has("process-state"))
            return null;
        return headers.get("process-state").asString();
    }

    public static boolean isNotFoundMessage(ModelNode result) {
        String message = result.get("failure-description").toString();
        @SuppressWarnings("SpellCheckingInspection")
        boolean jboss7start = message.startsWith("\"JBAS014807: Management resource");
        @SuppressWarnings("SpellCheckingInspection")
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
            throw new IllegalStateException("already started batch " + id);
        log.debug("--------- start batch {}", id);
        this.batch = CompositeOperationBuilder.create();
    }

    public void rollbackBatch() {
        if (this.batch == null)
            throw new IllegalStateException("no batch " + id + " started");
        log.debug("--------- rollback batch " + id);
        this.batch = null;
    }

    @SneakyThrows(IOException.class)
    public ProcessState commitBatch() {
        if (this.batch == null)
            throw new IllegalStateException("no batch " + id + " started");
        log.debug("--------- commit batch {}", id);
        Operation operation = batch.build();
        assert operation.getOperation().has(STEPS);
        ProcessState processState;
        if (operation.getOperation().get(STEPS).has(0)) {
            sortSteps(operation.getOperation().get(STEPS));
            logCli("execute batch: {}", operation.getOperation());
            ModelNode result = client.execute(operation, LOGGING);
            logCli("response {}", result);
            processState = checkResponse(result);
        } else {
            processState = running;
            log.debug("no batch to execute");
        }
        this.batch = null;
        return processState;
    }

    /**
     * We sort the steps to prevent dependency problems like loggers depending on log-handlers and so that deployables
     * can use their loggers when they are deployed.
     *
     * - add log-handlers
     * - add loggers
     * - add data-sources
     * - add deployables
     * - all updates
     * - remove deployables
     * - remove data-sources
     * - remove loggers
     * - remove log-handlers
     *
     * We can't reasonably do this ordering from within the deployers, as they do the adding _and_ the removing.
     */
    private void sortSteps(ModelNode steps) {
        List<ModelNode> list = new ArrayList<>(steps.asList());
        list.sort(Comparator.comparing(Batch::operation)
                            .thenComparing(node -> operation(node).factor() * type(node).ordinal()));
        steps.set(list);
    }

    @SuppressWarnings("unused")
    @RequiredArgsConstructor
    private enum OperationEnum {
        ADD(1), WRITE_ATTRIBUTE(1), MAP_PUT(1), MAP_REMOVE(1), FULL_REPLACE_DEPLOYMENT(1), UNDEPLOY(-1), REMOVE(-1);

        @Getter @Accessors(fluent = true)
        private final int factor;
    }

    private enum TypeEnum {LOG_HANDLER, LOGGER, DATA_SOURCE, DEPLOYABLE}

    private static OperationEnum operation(ModelNode node) {
        return OperationEnum.valueOf(node.get(OP).asString().toUpperCase(US).replace('-', '_'));
    }

    private static TypeEnum type(ModelNode node) {
        ModelNode address = node.get(ADDRESS);
        switch (address.asPropertyList().get(0).getName()) {
        case "deployment":
            return TypeEnum.DEPLOYABLE;
        case "subsystem":
            switch (address.asPropertyList().get(0).getValue().asString()) {
            case "logging":
                switch (address.asPropertyList().get(1).getName()) {
                case "logger":
                    return TypeEnum.LOGGER;
                case "console-handler":
                case "custom-handler":
                case "periodic-rotating-file-handler":
                    return TypeEnum.LOG_HANDLER;
                }
            case "datasources":
                switch (address.asPropertyList().get(1).getName()) {
                case "data-source":
                case "xa-data-source":
                    return TypeEnum.DATA_SOURCE;
                }
            }
        }
        throw new IllegalArgumentException("unsupported node type: " + address);
    }
}
