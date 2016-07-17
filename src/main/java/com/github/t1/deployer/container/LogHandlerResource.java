package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

@Slf4j
@Getter
@Builder(builderMethodName = "doNotUseThisBuilder_UseTheBuildMethodWithTypeAndNameAndContainer")
@Accessors(fluent = true, chain = true)
public class LogHandlerResource extends AbstractResource {
    @NonNull private final LoggingHandlerType type;
    @NonNull private final LogHandlerName name;

    private Boolean deployed = null;

    private LogLevel level;
    private String file;
    private String suffix;
    private String format;
    private String formatter;

    private LogHandlerResource(LoggerContainer container, LoggingHandlerType type, LogHandlerName name) {
        super(container);
        this.type = type;
        this.name = name;
    }

    public static LogHandlerResourceBuilder builder(LoggingHandlerType type, LogHandlerName name,
            LoggerContainer container) {
        return doNotUseThisBuilder_UseTheBuildMethodWithTypeAndNameAndContainer()
                .container(container).type(type).name(name);
    }

    public static class LogHandlerResourceBuilder {
        private LoggerContainer container;

        public LogHandlerResourceBuilder container(LoggerContainer container) {
            this.container = container;
            return this;
        }

        public LogHandlerResource build() {
            LogHandlerResource resource = new LogHandlerResource(container, type, name);
            resource.level = level;
            resource.file = file;
            resource.suffix = suffix;
            resource.format = format;
            resource.formatter = formatter;
            return resource;
        }
    }

    @Override public String toString() {
        return type + ":" + name + ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed")
                + ":" + level + ":" + file + ":" + suffix
                + (format == null ? "" : ":" + format)
                + (formatter == null ? "" : ":" + formatter);
    }

    public void writeLevel(LogLevel newLevel) {
        assertDeployed();
        writeAttribute("level", newLevel.name());
    }

    public void writeFile(String newFile) {
        assertDeployed();
        writeAttribute("file", newFile);
    }

    public void writeSuffix(String newSuffix) {
        assertDeployed();
        writeAttribute("suffix", newSuffix);
    }

    public void writeFormat(String newFormat) {
        assertDeployed();
        writeAttribute("format", newFormat);
    }

    public void writeFormatter(String newFormatter) {
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
