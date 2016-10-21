package com.github.t1.deployer.container;

import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import java.util.*;
import java.util.function.Supplier;

import static com.github.t1.deployer.container.CLI.*;
import static com.github.t1.deployer.container.LoggerCategory.*;
import static com.github.t1.log.LogLevel.*;
import static java.lang.Boolean.*;
import static java.util.Collections.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Builder(builderMethodName = "doNotCallThisBuilderExternally")
@Accessors(fluent = true, chain = true)
public class LoggerResource extends AbstractResource {
    public static LogLevel mapLogLevel(String level) {
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
        return doNotCallThisBuilderExternally().category(category).container(cli);
    }

    public static List<LoggerResource> allLoggers(CLI cli) {
        ModelNode request = readResource(new LoggerResource(LoggerCategory.ALL, cli).createRequestWithAddress());
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

        public LoggerResourceBuilder container(CLI cli) {
            this.cli = cli;
            return this;
        }

        @Override public LoggerResource get() { return build(); }

        public LoggerResource build() {
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
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("add-handler");
        request.get("name").set(handler.getValue());
        execute(request);
    }

    public void removeLoggerHandler(LogHandlerName handler) {
        checkDeployed();
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("remove-handler");
        request.get("name").set(handler.getValue());
        execute(request);
    }

    public void updateUseParentHandlers(Boolean newUseParentHandlers) {
        checkDeployed();
        writeAttribute("use-parent-handlers", newUseParentHandlers);
    }

    public void updateLevel(LogLevel newLevel) {
        checkDeployed();
        if (newLevel == null)
            newLevel = LogLevel.ALL;
        writeAttribute("level", newLevel.name());
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

    @Override protected ModelNode createRequestWithAddress() {
        ModelNode request = new ModelNode();
        ModelNode logging = request.get("address").add("subsystem", "logging");
        if (category.isRoot())
            logging.add("root-logger", ROOT.getValue());
        else
            logging.add("logger", category.getValue());
        return request;
    }

    @Override public void remove() {
        if (isRoot())
            throw new RuntimeException("can't remove root logger");
        super.remove();
    }

    @Override public void add() {
        if (isRoot())
            throw new RuntimeException("can't add root logger");

        ModelNode request = createRequestWithAddress();
        request.get("operation").set("add");
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
}
