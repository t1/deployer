package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

@Slf4j
@Builder(builderMethodName = "doNotCallThisBuilderExternally")
@Accessors(fluent = true, chain = true)
public class LogHandlerResource extends AbstractResource {
    @NonNull @Getter private final LoggingHandlerType type;
    @NonNull @Getter private final LogHandlerName name;

    private LogLevel level;
    private String file;
    private String suffix;
    private String format;
    private String formatter;

    private LogHandlerResource(CLI cli, LoggingHandlerType type, LogHandlerName name) {
        super(cli);
        this.type = type;
        this.name = name;
    }

    public static LogHandlerResourceBuilder builder(LoggingHandlerType type, LogHandlerName name, CLI cli) {
        return doNotCallThisBuilderExternally().cli(cli).type(type).name(name);
    }

    public static class LogHandlerResourceBuilder {
        private CLI cli;

        public LogHandlerResourceBuilder cli(CLI cli) {
            this.cli = cli;
            return this;
        }

        public LogHandlerResource build() {
            LogHandlerResource resource = new LogHandlerResource(cli, type, name);
            resource.level = level;
            resource.file = file;
            resource.suffix = suffix;
            resource.format = format;
            resource.formatter = formatter;
            return resource;
        }
    }

    public LogLevel level() {
        assertDeployed();
        return level;
    }

    public String file() {
        assertDeployed();
        return file;
    }

    public String suffix() {
        assertDeployed();
        return suffix;
    }

    public String format() {
        assertDeployed();
        return format;
    }

    public String formatter() {
        assertDeployed();
        return formatter;
    }

    @Override public String toString() {
        return type + ":" + name + ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed")
                + ":" + level + ":" + file + ":" + suffix
                + (format == null ? "" : ":" + format)
                + (formatter == null ? "" : ":" + formatter);
    }

    public void updateLevel(LogLevel newLevel) {
        assertDeployed();
        writeAttribute("level", newLevel.name());
    }

    public void updateFile(String newFile) {
        assertDeployed();
        writeAttribute("file", newFile);
    }

    public void updateSuffix(String newSuffix) {
        assertDeployed();
        writeAttribute("suffix", newSuffix);
    }

    public void updateFormat(String newFormat) {
        assertDeployed();
        writeAttribute("format", newFormat);
    }

    public void updateFormatter(String newFormatter) {
        assertDeployed();
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
        level = LogLevel.valueOf(getString(result, "level"));
        file = getString(result, "file");
        suffix = getString(result, "suffix");
        format = getString(result, "format");
        formatter = getString(result, "named-formatter");
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
