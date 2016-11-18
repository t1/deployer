package com.github.t1.deployer.container;

import com.github.t1.deployer.model.*;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import java.util.*;
import java.util.function.Supplier;

import static com.github.t1.deployer.model.LoggerCategory.*;
import static com.github.t1.log.LogLevel.*;
import static java.lang.Boolean.*;
import static java.util.Collections.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static org.jboss.as.controller.client.helpers.Operations.*;

@Slf4j
@Builder(builderMethodName = "do_not_call", buildMethodName = "get")
@Accessors(fluent = true, chain = true)
public class LoggerResource extends AbstractResource<LoggerResource> {
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
    private List<LogHandlerName> handlers = new ArrayList<>();
    private Boolean useParentHandlers;
    private LogLevel level;

    private LoggerResource(LoggerCategory category, CLI cli) {
        super(cli);
        this.category = category;
    }

    public static LoggerResourceBuilder builder(LoggerCategory category, CLI cli) {
        LoggerResourceBuilder builder = new LoggerResourceBuilder().category(category);
        builder.cli = cli;
        return builder;
    }

    public static List<LoggerResource> allLoggers(CLI cli) {
        ModelNode request = createReadResourceOperation(address(LoggerCategory.ALL), true);
        List<LoggerResource> loggers =
                cli.execute(request)
                   .asList().stream()
                   .map(node -> toLoggerResource(category(node), cli, node.get("result")))
                   .sorted(comparing(LoggerResource::category))
                   .collect(toList());
        loggers.add(0, readRootLogger(cli));
        return loggers;
    }

    public boolean isDefaultUseParentHandlers() { return isRoot() || (useParentHandlers == handlers().isEmpty()); }


    public static class LoggerResourceBuilder implements Supplier<LoggerResource> {
        private CLI cli;

        @Override public LoggerResource get() {
            LoggerResource resource = new LoggerResource(category, cli);
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
        return (useParentHandlers == null) ? true : useParentHandlers;
    }

    public LogLevel level() {
        checkDeployed();
        return level;
    }


    public void addLoggerHandler(LogHandlerName handler) {
        checkDeployed();
        ModelNode request = createOperation("add-handler", address());
        request.get("name").set(handler.getValue());
        execute(request);
        this.handlers.add(handler);
    }

    public void removeLoggerHandler(LogHandlerName handler) {
        checkDeployed();
        ModelNode request = createOperation("remove-handler", address());
        request.get("name").set(handler.getValue());
        execute(request);
        this.handlers.remove(handler);
    }

    public void updateUseParentHandlers(Boolean newUseParentHandlers) {
        checkDeployed();
        writeAttribute("use-parent-handlers", newUseParentHandlers);
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

    @Override public void remove() {
        if (isRoot())
            throw new RuntimeException("can't remove root logger");
        super.remove();
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

        execute(request);

        this.deployed = true;
    }

    private static LoggerCategory category(ModelNode address) {
        return LoggerCategory.of(address.get("address").get(1).get("logger").asString());
    }

    public static LoggerResource toLoggerResource(LoggerCategory category, CLI cli, ModelNode node) {
        LoggerResource logger = new LoggerResource(category, cli);
        logger.readFrom(node);
        logger.deployed = true;
        return logger;
    }

    private static LoggerResource readRootLogger(CLI cli) {
        LoggerResource root = new LoggerResource(ROOT, cli);
        root.checkDeployed();
        assert root.isRoot();
        return root;
    }

    @Override public String getId() { return category.getValue(); }
}
