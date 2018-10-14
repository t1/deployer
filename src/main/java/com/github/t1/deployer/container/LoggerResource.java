package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LogHandlerName;
import com.github.t1.deployer.model.LoggerCategory;
import com.github.t1.log.LogLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import java.util.List;
import java.util.function.Supplier;

import static com.github.t1.deployer.model.LoggerCategory.ROOT;
import static com.github.t1.log.LogLevel.DEBUG;
import static com.github.t1.log.LogLevel.ERROR;
import static com.github.t1.log.LogLevel.INFO;
import static com.github.t1.log.LogLevel.OFF;
import static com.github.t1.log.LogLevel.TRACE;
import static com.github.t1.log.LogLevel.WARN;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.jboss.as.controller.client.helpers.Operations.createAddOperation;
import static org.jboss.as.controller.client.helpers.Operations.createAddress;
import static org.jboss.as.controller.client.helpers.Operations.createOperation;

@Slf4j
@Builder(builderMethodName = "do_not_call", buildMethodName = "get")
@Accessors(fluent = true, chain = true)
public final class LoggerResource extends AbstractResource<LoggerResource> {
    static LogLevel mapLogLevel(String level) {
        switch (level) {
        case "ALL":
            return LogLevel.ALL;
        case "ERROR":
        case "SEVERE":
        case "FATAL":
            return ERROR;
        case "WARN":
        case "WARNING":
            return WARN;
        case "INFO":
        case "CONFIG":
            return INFO;
        case "DEBUG":
        case "FINE":
        case "FINER":
            return DEBUG;
        case "TRACE":
        case "FINEST":
            return TRACE;
        case "OFF":
            return OFF;
        default:
            log.error("unmapped log level: '{}'", level);
            return WARN;
        }
    }


    @NonNull @Getter private final LoggerCategory category;

    @Singular
    private List<LogHandlerName> handlers;
    private Boolean useParentHandlers;
    private LogLevel level;

    private LoggerResource(LoggerCategory category, Batch batch) {
        super(batch);
        this.category = category;
    }

    public static LoggerResourceBuilder builder(LoggerCategory category, Batch batch) {
        LoggerResourceBuilder builder = new LoggerResourceBuilder().category(category);
        builder.batch = batch;
        return builder;
    }

    public static List<LoggerResource> allLoggers(Batch batch) {
        List<LoggerResource> loggers =
                batch.readResource(address(LoggerCategory.ALL))
                     .map(node -> toLoggerResource(category(node), batch, node.get("result")))
                     .sorted(comparing(LoggerResource::category))
                     .collect(toList());
        loggers.add(0, readRootLogger(batch));
        return loggers;
    }

    public boolean isDefaultUseParentHandlers() { return isRoot() || (useParentHandlers == handlers().isEmpty()); }


    public static class LoggerResourceBuilder implements Supplier<LoggerResource> {
        private Batch batch;

        @Override public LoggerResource get() {
            LoggerResource resource = new LoggerResource(category, batch);
            resource.useParentHandlers = this.useParentHandlers;
            resource.level = this.level;
            if (this.handlers != null)
                resource.handlers.addAll(this.handlers);
            return resource;
        }
    }

    @Override public String toString() {
        return "Logger:" + category + ":deployed=" + deployed + ":" + level
                + ":" + handlers + ((useParentHandlers == TRUE) ? "+" : "");
    }

    public boolean isNotRoot() { return !isRoot(); }

    public boolean isRoot() { return category.isRoot(); }

    public List<LogHandlerName> handlers() {
        checkDeployed();
        return handlers;
    }

    public Boolean useParentHandlers() {
        checkDeployed();
        return (useParentHandlers == null) ? isNotRoot() : useParentHandlers;
    }

    public LogLevel level() {
        checkDeployed();
        return level;
    }


    public void addLoggerHandler(LogHandlerName handler) {
        checkDeployed();
        ModelNode request = createOperation("add-handler", address());
        request.get("name").set(handler.getValue());
        addStep(request);
        this.handlers.add(handler);
    }

    public void removeLoggerHandler(LogHandlerName handler) {
        checkDeployed();
        ModelNode request = createOperation("remove-handler", address());
        request.get("name").set(handler.getValue());
        addStep(request);
        this.handlers.remove(handler);
    }

    public void updateUseParentHandlers(Boolean newUseParentHandlers) {
        checkDeployed();
        writeUseParentHandlers(newUseParentHandlers);
        this.useParentHandlers = newUseParentHandlers;
    }

    public void updateLevel(LogLevel newLevel) {
        checkDeployed();
        if (newLevel == null)
            newLevel = LogLevel.ALL;
        writeAttribute("level", newLevel.name());
        this.level = newLevel;
    }


    @Override protected void readFrom(ModelNode response) {
        this.level = (response.get("level").isDefined()) ? mapLogLevel(response.get("level").asString()) : null;

        this.useParentHandlers = (response.get("use-parent-handlers").isDefined())
                ? response.get("use-parent-handlers").asBoolean() : null;

        this.handlers = (response.get("handlers").isDefined())
                ? response.get("handlers")
                          .asList()
                          .stream()
                          .map(ModelNode::asString)
                          .map(LogHandlerName::new)
                          .collect(toList())
                : emptyList();
    }

    @Override protected ModelNode address() { return address(category); }

    private static ModelNode address(LoggerCategory category) {
        return createAddress("subsystem", "logging",
                category.isRoot() ? "root-logger" : "logger",
                category.isRoot() ? ROOT.getValue() : category.getValue());
    }

    @Override public void addRemoveStep() {
        if (isRoot())
            throw new RuntimeException("can't remove root logger");
        super.addRemoveStep();
    }

    @Override public void add() {
        if (isRoot())
            throw new RuntimeException("can't add root logger");

        ModelNode request = createAddOperation(address());
        if (level != null)
            request.get("level").set(level.name());
        for (LogHandlerName handler : handlers)
            request.get("handlers").add(handler.getValue());
        if (useParentHandlers != null)
            request.get("use-parent-handlers").set(useParentHandlers);

        addStep(request);

        this.deployed = true;
    }

    private static LoggerCategory category(ModelNode address) {
        return LoggerCategory.of(address.get("address").get(1).get("logger").asString());
    }

    public static LoggerResource toLoggerResource(LoggerCategory category, Batch batch, ModelNode node) {
        LoggerResource logger = new LoggerResource(category, batch);
        logger.readFrom(node);
        logger.deployed = true;
        return logger;
    }

    private static LoggerResource readRootLogger(Batch batch) {
        LoggerResource root = new LoggerResource(ROOT, batch);
        root.checkDeployed();
        assert root.isRoot();
        return root;
    }

    @Override public String getId() { return category.getValue(); }
}
