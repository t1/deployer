package com.github.t1.deployer.container;

import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import java.util.*;

import static com.github.t1.deployer.container.CLI.*;
import static com.github.t1.deployer.container.LoggerCategory.*;
import static java.lang.Boolean.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Builder(builderMethodName = "doNotUseThisBuilder_UseTheBuildMethodWithCategoryAndContainer")
@Accessors(fluent = true, chain = true)
public class LoggerResource extends AbstractResource {
    @NonNull @Getter private final LoggerCategory category;

    @Singular
    private List<LogHandlerName> handlers = new ArrayList<>();
    private Boolean useParentHandlers;
    private LogLevel level;

    private LoggerResource(LoggerCategory category, LoggerContainer container) {
        super(container);
        this.category = category;
    }

    public static LoggerResourceBuilder builder(LoggerCategory category, LoggerContainer container) {
        return doNotUseThisBuilder_UseTheBuildMethodWithCategoryAndContainer().category(category).container(container);
    }

    public static class LoggerResourceBuilder {
        private LoggerContainer container;

        public LoggerResourceBuilder container(LoggerContainer container) {
            this.container = container;
            return this;
        }

        public LoggerResource build() {
            LoggerResource resource = new LoggerResource(category, container);
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

    public boolean isRoot() { return category.isRoot(); }

    public List<LogHandlerName> handlers() {
        assertDeployed();
        return handlers;
    }

    public boolean useParentHandlers() {
        assertDeployed();
        return (useParentHandlers == null) ? true : useParentHandlers;
    }

    public LogLevel level() {
        assertDeployed();
        return level;
    }


    public LoggerResource addLoggerHandler(LogHandlerName handler) {
        assertDeployed();
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("add-handler");
        request.get("name").set(handler.getValue());
        execute(request);
        return this;
    }

    public LoggerResource removeLoggerHandler(LogHandlerName handler) {
        assertDeployed();
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("remove-handler");
        request.get("name").set(handler.getValue());
        execute(request);
        return this;
    }

    public LoggerResource writeUseParentHandlers(boolean newUseParentHandlers) {
        assertDeployed();
        writeAttribute("use-parent-handlers", newUseParentHandlers);
        return this;
    }

    public LoggerResource writeLevel(LogLevel newLevel) {
        assertDeployed();
        writeAttribute("level", newLevel.name());
        return this;
    }


    @Override protected void readFrom(ModelNode response) {
        this.level = LogLevel.valueOf(response.get("level").asString());
        ModelNode useParentHandlersNode = response.get("use-parent-handlers");
        this.useParentHandlers = (useParentHandlersNode.isDefined()) ? useParentHandlersNode.asBoolean() : null;
        ModelNode handlersNode = response.get("handlers");
        this.handlers = (handlersNode.isDefined())
                ? handlersNode.asList().stream().map(ModelNode::asString).map(LogHandlerName::new).collect(toList())
                : emptyList();
    }


    @Override protected ModelNode createRequestWithAddress() {
        ModelNode request = new ModelNode();
        ModelNode logging = request.get("address").add("subsystem", "logging");
        if (category.isRoot())
            logging.add("root-logger", "ROOT");
        else
            logging.add("logger", category.getValue());
        return request;
    }

    @Override public void remove() {
        if (isRoot())
            throw new RuntimeException("can't remove root logger");
        execute(removeLogger());
    }

    private ModelNode removeLogger() {
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("remove");
        return request;
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

    public static List<LoggerResource> all(LoggerContainer container) {
        LoggerResource loggerResource = new LoggerResource(LoggerCategory.ANY, container);
        ModelNode request = readResource(loggerResource.createRequestWithAddress());
        List<LoggerResource> loggers =
                container.execute(request)
                         .asList().stream()
                         .map(node -> toLoggerResource(container, node.get("result"), category(node.get("address"))))
                         .collect(toList());
        Collections.sort(loggers, Comparator.comparing(LoggerResource::category));
        loggers.add(0, readRootLogger(container));
        return loggers;
    }

    private static LoggerCategory category(ModelNode address) {
        return LoggerCategory.of(address.get(1).get("logger").asString());
    }

    public static LoggerResource toLoggerResource(LoggerContainer container, ModelNode node, LoggerCategory category) {
        LoggerResource logger = new LoggerResource(category, container);
        logger.deployed = true;
        logger.readFrom(node);
        return logger;
    }

    private static LoggerResource readRootLogger(LoggerContainer container) {
        ModelNode requestWithAddress = new LoggerResource(ROOT, container).createRequestWithAddress();
        ModelNode root = container.execute(readResource(requestWithAddress));
        return toLoggerResource(container, root, ROOT);
    }
}
