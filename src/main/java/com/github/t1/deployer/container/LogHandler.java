package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import static com.github.t1.deployer.container.CLI.*;
import static lombok.AccessLevel.*;

@Slf4j
@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@Accessors(fluent = true, chain = true)
public class LogHandler {
    @NonNull private final String name;
    @NonNull private final LoggingHandlerType type;

    @NonNull private final CLI cli;

    private Boolean deployed = null;

    private LogLevel level;
    private String file;
    private String suffix;
    private String format;

    private void assertDeployed() {
        if (!isDeployed())
            throw new RuntimeException("no log handler '" + name + "'");
    }

    public boolean isDeployed() {
        if (deployed == null) {
            ModelNode request = readResource(createRequestWithAddress());

            ModelNode response = cli.executeRaw(request);
            deployed = cli.isOutcomeFound(response);
            if (deployed) {
                level = LogLevel.valueOf(response.get("result").get("level").asString());
                file = response.get("result").get("file").asString();
                suffix = response.get("result").get("suffix").asString();
                format = response.get("result").get("formatter").asString();
            }
        }
        return deployed;
    }

    public LogHandler correctLevel(LogLevel newLevel) {
        assertDeployed();
        if (level.equals(newLevel))
            return this;
        return writeAttribute("level", newLevel.name());
    }

    public LogHandler correctFile(String newFile) {
        assertDeployed();
        if (file.equals(newFile))
            return this;
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("write-attribute");
        request.get("name").set("file");
        request.get("value").get("path").set(newFile);

        cli.execute(request);

        return this;
    }

    public LogHandler correctSuffix(String newSuffix) {
        assertDeployed();
        if (suffix.equals(newSuffix))
            return this;
        return writeAttribute("suffix", newSuffix);
    }

    public LogHandler correctFormat(String newFormat) {
        assertDeployed();
        if (format.equals(newFormat))
            return this;
        return writeAttribute("formatter", newFormat);
    }

    private LogHandler writeAttribute(String name, String value) {
        cli.writeAttribute(createRequestWithAddress(), name, value);
        return this;
    }

    private ModelNode createRequestWithAddress() {
        ModelNode request = new ModelNode();
        request.get("address")
               .add("subsystem", "logging")
               .add(type.getTypeName(), name);
        return request;
    }

    public LogHandler add() {
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("add");
        request.get("file").get("path").set(file);
        request.get("file").get("relative-to").set("jboss.server.log.dir");
        request.get("suffix").set(suffix);
        request.get("formatter").set(format);

        cli.execute(request);

        return this;
    }
}
