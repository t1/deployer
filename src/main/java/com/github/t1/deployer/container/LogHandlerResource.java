package com.github.t1.deployer.container;

import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.github.t1.deployer.container.CLI.*;
import static com.github.t1.deployer.container.LogHandlerName.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Builder(builderMethodName = "doNotCallThisBuilderExternally")
@Accessors(fluent = true, chain = true)
public class LogHandlerResource extends AbstractResource {
    private static final String DEFAULT_FORMAT = "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n";

    @NonNull @Getter private final LoggingHandlerType type;
    @NonNull @Getter private final LogHandlerName name;

    private LogLevel level;
    private String file;
    private String suffix;
    private String format;
    private String formatter;

    private LogHandlerResource(LoggingHandlerType type, LogHandlerName name, CLI cli) {
        super(cli);
        this.type = type;
        this.name = name;
    }

    public static LogHandlerResourceBuilder builder(LoggingHandlerType type, LogHandlerName name, CLI cli) {
        return doNotCallThisBuilderExternally().cli(cli).type(type).name(name);
    }

    public static List<LogHandlerResource> allHandlers(CLI cli) {
        return Arrays.stream(LoggingHandlerType.values()).flatMap(type -> {
            ModelNode request = readResource(new LogHandlerResource(type, ALL, cli).createRequestWithAddress());
            return cli.execute(request)
                      .asList().stream()
                      .map(node -> toLoggerResource(type(node), name(node), cli, node.get("result")));
        }).collect(toList());
    }

    private static LoggingHandlerType type(ModelNode node) {
        return LoggingHandlerType.valueOfTypeName(new ArrayList<>(node.get("address").get(1).keys()).get(0));
    }

    private static LogHandlerName name(ModelNode node) {
        return new LogHandlerName(node.get("address").get(1).get(type(node).getTypeName()).asString());
    }

    private static LogHandlerResource toLoggerResource(LoggingHandlerType type, LogHandlerName name, CLI cli,
            ModelNode node) {
        LogHandlerResource logger = new LogHandlerResource(type, name, cli);
        logger.readFrom(node);
        logger.deployed = true;
        return logger;
    }

    public static class LogHandlerResourceBuilder {
        private CLI cli;

        public LogHandlerResourceBuilder cli(CLI cli) {
            this.cli = cli;
            return this;
        }

        public LogHandlerResource build() {
            LogHandlerResource resource = new LogHandlerResource(type, name, cli);
            resource.level = level;
            resource.file = file;
            resource.suffix = suffix;
            resource.format = format;
            resource.formatter = formatter;
            return resource;
        }
    }

    public LogLevel level() {
        checkDeployed();
        return level;
    }

    public String file() {
        checkDeployed();
        return file;
    }

    public String suffix() {
        checkDeployed();
        return suffix;
    }

    public String format() {
        checkDeployed();
        return format;
    }

    public String formatter() {
        checkDeployed();
        return formatter;
    }

    @Override public String toString() {
        return type + ":" + name + ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed")
                + ":" + level + ":" + file + ":" + suffix
                + (format == null ? "" : ":" + format)
                + (formatter == null ? "" : ":" + formatter);
    }

    public void updateLevel(LogLevel newLevel) {
        checkDeployed();
        writeAttribute("level", newLevel.name());
    }

    public void updateFile(String newFile) {
        checkDeployed();
        writeAttribute("file", newFile);
    }

    public void updateSuffix(String newSuffix) {
        checkDeployed();
        writeAttribute("suffix", newSuffix);
    }

    public void updateFormat(String newFormat) {
        checkDeployed();
        writeAttribute("format", newFormat);
    }

    public void updateFormatter(String newFormatter) {
        checkDeployed();
        writeAttribute("named-formatter", newFormatter);
    }

    @Override protected ModelNode createRequestWithAddress() {
        ModelNode request = new ModelNode();
        request.get("address")
               .add("subsystem", "logging")
               .add(type.getTypeName(), name.getValue());
        return request;
    }

    @Override protected void readFrom(ModelNode result) {
        this.level = (result.get("level").isDefined()) ? LogLevel.valueOf(result.get("level").asString()) : null;
        this.file = (result.get("file").isDefined()) ? result.get("file").get("path").asString() : null;
        this.suffix = getString(result, "suffix");
        this.format = readFormat(result);
        this.formatter = getString(result, "named-formatter");
    }

    @Nullable private String readFormat(ModelNode result) {
        ModelNode node = result.get("formatter");
        return (node.isDefined() && !DEFAULT_FORMAT.equals(node.asString())) ? node.asString() : null;
    }

    private static String getString(ModelNode node, String name) {
        ModelNode value = node.get(name);
        return (value.isDefined()) ? value.asString() : null;
    }

    @Override public void add() {
        log.debug("add log handler {}", name);
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("add");
        if (level != null)
            request.get("level").set(level.name());
        request.get("file").get("path").set(file);
        request.get("file").get("relative-to").set("jboss.server.log.dir");
        request.get("suffix").set(suffix);
        if (format != null)
            request.get("format").set(format);
        if (formatter != null)
            request.get("named-formatter").set(formatter);

        execute(request);

        this.deployed = true;
    }
}
