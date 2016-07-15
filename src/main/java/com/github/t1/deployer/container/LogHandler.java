package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import java.util.Objects;

import static com.github.t1.deployer.container.CLI.*;
import static lombok.AccessLevel.*;

@Slf4j
@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@Accessors(fluent = true, chain = true)
public class LogHandler {
    @NonNull private final LogHandlerName name;
    @NonNull private final LoggingHandlerType type;

    @NonNull private final CLI cli;

    private Boolean deployed = null;

    private LogLevel level;
    private String file;
    private String suffix;
    private String format;
    private String formatter;

    @Override public String toString() {
        return type + ":" + name + ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed")
                + ":" + level + ":" + file + ":" + suffix
                + (format == null ? "" : ":" + format)
                + (formatter == null ? "" : ":" + formatter);
    }

    private void assertDeployed() {
        if (!isDeployed())
            throw new RuntimeException("no log handler '" + name + "'");
    }

    public boolean isDeployed() {
        if (deployed == null) {
            ModelNode request = readResource(createRequestWithAddress());

            ModelNode response = cli.executeRaw(request);
            deployed = cli.hasOutcomeFound(response);
            if (deployed) {
                ModelNode result = response.get("result");
                level = LogLevel.valueOf(getString(result, "level"));
                file = getString(result, "file");
                suffix = getString(result, "suffix");
                format = getString(result, "format");
                formatter = getString(result, "named-formatter");
            }
        }
        return deployed;
    }

    public String getString(ModelNode node, String name) {
        ModelNode value = node.get(name);
        return (value.isDefined()) ? value.asString() : null;
    }

    public LogHandler correctLevel(LogLevel newLevel) {
        assertDeployed();
        if (level.equals(newLevel))
            return this;
        log.debug("correct level of {} from {} to {}", name, level, newLevel);
        return writeAttribute("level", newLevel.name());
    }

    public LogHandler correctFile(String newFile) {
        assertDeployed();
        if (Objects.equals(file, newFile))
            return this;
        log.debug("correct file of {} from {} to {}", name, file, newFile);
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("write-attribute");
        request.get("name").set("file");
        request.get("value").get("path").set(newFile);

        cli.execute(request);

        return this;
    }

    public LogHandler correctSuffix(String newSuffix) {
        assertDeployed();
        if (Objects.equals(suffix, newSuffix))
            return this;
        log.debug("correct suffix of {} from {} to {}", name, suffix, newSuffix);
        return writeAttribute("suffix", newSuffix);
    }

    public LogHandler correctFormat(String newFormat) {
        assertDeployed();
        if (Objects.equals(format, newFormat))
            return this;
        log.debug("correct format of {} from {} to {}", name, format, newFormat);
        return writeAttribute("formatter", newFormat);
    }

    public LogHandler correctFormatter(String newFormatter) {
        assertDeployed();
        if (Objects.equals(formatter, newFormatter))
            return this;
        log.debug("correct formatter of {} from {} to {}", name, formatter, newFormatter);
        return writeAttribute("named-formatter", newFormatter);
    }

    private LogHandler writeAttribute(String name, String value) {
        cli.writeAttribute(createRequestWithAddress(), name, value);
        return this;
    }

    private ModelNode createRequestWithAddress() {
        ModelNode request = new ModelNode();
        request.get("address")
               .add("subsystem", "logging")
               .add(type.getTypeName(), name.getValue());
        return request;
    }

    public LogHandler add() {
        log.debug("add log handler {}", name);
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("add");
        request.get("file").get("path").set(file);
        request.get("file").get("relative-to").set("jboss.server.log.dir");
        request.get("suffix").set(suffix);
        request.get("formatter").set(format);

        cli.execute(request);

        return this;
    }

    public void remove() {
        cli.execute(removeLogHandler());
    }

    private ModelNode removeLogHandler() {
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("remove");
        return request;
    }
}
