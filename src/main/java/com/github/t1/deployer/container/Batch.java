package com.github.t1.deployer.container;

import com.github.t1.deployer.model.ProcessState;
import com.github.t1.problemdetail.Extension;
import com.github.t1.problemdetail.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.github.t1.deployer.container.Batch.TypeEnum.DATA_SOURCE;
import static com.github.t1.deployer.container.Batch.TypeEnum.DEPLOYABLE;
import static com.github.t1.deployer.container.Batch.TypeEnum.LOGGER;
import static com.github.t1.deployer.container.Batch.TypeEnum.LOG_HANDLER;
import static com.github.t1.deployer.container.Batch.TypeEnum.UNKNOWN;
import static com.github.t1.deployer.container.Container.CLI_DEBUG;
import static com.github.t1.deployer.model.ProcessState.reloadRequired;
import static com.github.t1.deployer.model.ProcessState.restartRequired;
import static com.github.t1.deployer.model.ProcessState.running;
import static java.util.Locale.US;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_RELOAD_REQUIRED;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_RESTART_REQUIRED;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_RUNNING;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_STARTING;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_STOPPING;
import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.client.helpers.ClientConstants.STEPS;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import static org.jboss.as.controller.client.helpers.Operations.createOperation;
import static org.jboss.as.controller.client.helpers.Operations.createReadResourceOperation;
import static org.jboss.as.controller.client.helpers.Operations.isSuccessfulOutcome;
import static org.wildfly.plugin.core.ServerHelper.waitForStandalone;

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


    @SneakyThrows({InterruptedException.class, TimeoutException.class})
    public void waitForBoot() {
        log.info("wait for boot");
        waitForStandalone(client, STARTUP_TIMEOUT);
        log.info("boot done");
    }

    public void suspend() {
        executeEmptyOperation("suspend");
    }

    public void reload() {
        executeEmptyOperation("reload");
    }

    public void shutdown() {
        executeEmptyOperation("shutdown");
    }

    private void executeEmptyOperation(String operation) {
        log.info(operation);
        ModelNode shutdown = Operations.createOperation(operation, new ModelNode().setEmptyList());
        ModelNode result = executeRaw(shutdown);
        if (!isSuccessfulOutcome(result))
            log.error("{} -> {}", operation, result);
    }


    public int addInputStreamAndReturnIndex(InputStream inputStream) {
        int index = batch.getInputStreamCount();
        batch.addInputStream(inputStream);
        return index;
    }

    public <T> void writeAttr(ModelNode address, String name, BiFunction<ModelNode, T, ModelNode> set, T value) {
        ModelNode request = createOperation(WRITE_ATTRIBUTE_OPERATION, address);
        request.get(NAME).set(name);
        if (value != null)
            set.apply(request.get(VALUE), value);
        addStep(request);
    }


    public void writeProperty(ModelNode address, String key, String value) {
        ModelNode request = createOperation("map-put", address);
        request.get("name").set("property");
        request.get("key").set(key);
        request.get("value").set(value);

        addStep(request);
    }

    public void removeProperty(ModelNode address, String key) {
        ModelNode request = createOperation("map-remove", address);
        request.get("name").set("property");
        request.get("key").set(key);

        addStep(request);
    }


    public Stream<ModelNode> readResource(ModelNode address) {
        ModelNode result = executeRaw(createReadResourceOperation(address, true));
        checkResponse(result);
        return result.get("result").asList().stream();
    }

    public void addStep(ModelNode request) {
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
                throw new UnexpectedlyStillStartingException();
            case CONTROLLER_PROCESS_STATE_STOPPING:
                throw new UnexpectedlyAlreadyStoppingException();
        }
        throw new UnexpectedProcessStateException(processState);
    }

    public void fail(ModelNode result) {
        String detail = "outcome " + result.get(OUTCOME)
            + (result.hasDefined(FAILURE_DESCRIPTION) ? ": " + result.get(FAILURE_DESCRIPTION) : "");
        throw new BadRequestException(detail);
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
        this.batch = CompositeOperationBuilder.create(true);
    }

    public void rollbackBatch() {
        if (this.batch == null)
            throw new IllegalStateException("no batch " + id + " started");
        log.debug("--------- rollback batch {}", id);
        this.batch = null;
    }

    @SneakyThrows(IOException.class)
    public ProcessState commitBatch() {
        if (this.batch == null)
            throw new IllegalStateException("no batch " + id + " started");
        log.debug("--------- commit batch {}", id);
        Operation operation = batch.build();
        logCli("------------------------------\n{}\n------------------------------", operation.getOperation());
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
     * <p>
     * - add log-handlers
     * - add loggers
     * - add data-sources
     * - add deployables
     * - all updates
     * - remove deployables
     * - remove data-sources
     * - remove loggers
     * - remove log-handlers
     * <p>
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
        ADD(1),
        ADD_HANDLER(1),
        WRITE_ATTRIBUTE(1),
        MAP_PUT(1),
        MAP_REMOVE(1),
        FULL_REPLACE_DEPLOYMENT(1),
        UNDEPLOY(-1),
        REMOVE(-1),
        REMOVE_HANDLER(-1);

        @Getter @Accessors(fluent = true)
        private final int factor;
    }

    enum TypeEnum {LOG_HANDLER, LOGGER, DATA_SOURCE, DEPLOYABLE, UNKNOWN}

    private static OperationEnum operation(ModelNode node) {
        return OperationEnum.valueOf(node.get(OP).asString().toUpperCase(US).replace('-', '_'));
    }

    private static TypeEnum type(ModelNode node) {
        ModelNode address = node.get(ADDRESS);
        if (address.asPropertyList().isEmpty())
            return UNKNOWN;
        switch (address.asPropertyList().get(0).getName()) {
            case "deployment":
                return DEPLOYABLE;
            case "subsystem":
                switch (address.asPropertyList().get(0).getValue().asString()) {
                    case "logging":
                        switch (address.asPropertyList().get(1).getName()) {
                            case "logger":
                                return LOGGER;
                            case "console-handler":
                            case "custom-handler":
                            case "periodic-rotating-file-handler":
                                return LOG_HANDLER;
                        }
                    case "datasources":
                        switch (address.asPropertyList().get(1).getName()) {
                            case "data-source":
                            case "xa-data-source":
                                return DATA_SOURCE;
                        }
                }
        }
        throw new IllegalArgumentException("unsupported node type: " + address);
    }

    @Status(BAD_REQUEST)
    private static class UnexpectedlyStillStartingException extends RuntimeException {}

    @Status(BAD_REQUEST)
    private static class UnexpectedlyAlreadyStoppingException extends RuntimeException {}

    @Status(BAD_REQUEST) @AllArgsConstructor
    private static class UnexpectedProcessStateException extends RuntimeException {
        @Extension String processState;
    }
}
