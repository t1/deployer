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

    private LogLevel level;
    private String file;
    private String suffix;
    private String formatter;

    public boolean isDeployed() {
        ModelNode request = readResource(createRequestWithAddress());

        ModelNode response = cli.executeRaw(request);
        boolean isDeployed = cli.isOutcomeFound(response);
        if (isDeployed) {
            level = LogLevel.valueOf(response.get("result").get("level").asString());
            file = response.get("result").get("file").asString();
            suffix = response.get("result").get("suffix").asString();
            formatter = response.get("result").get("formatter").asString();
        }
        return isDeployed;
    }

    public LogHandler correctLevel(LogLevel newLevel) {
        if (level.equals(newLevel))
            return this;
        return writeAttribute("level", newLevel.name());
    }

    public LogHandler correctFile(String newFile) {
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
        if (suffix.equals(newSuffix))
            return this;
        return writeAttribute("suffix", newSuffix);
    }

    public LogHandler correctFormatter(String newFormatter) {
        if (formatter.equals(newFormatter))
            return this;
        return writeAttribute("formatter", newFormatter);
    }

    private LogHandler writeAttribute(String name, String value) {
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("write-attribute");
        request.get("name").set(name);
        request.get("value").set(value);

        cli.execute(request);

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
        request.get("formatter").set(formatter);

        cli.execute(request);

        return this;
    }
}
