package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import static com.github.t1.deployer.container.CLI.*;

@Slf4j
@Data
@RequiredArgsConstructor
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
            level(LogLevel.valueOf(response.get("result").get("level").asString()));
        }
        return isDeployed;
    }

    public LogHandler write() {
        ModelNode request = createRequestWithAddress();

        if (level != null)
            writeAttribute(request, "level", level.name());

        cli.execute(request);

        return this;
    }

    public void writeAttribute(ModelNode request, String name, String value) {
        request.get("operation").set("write-attribute");
        request.get("name").set(name);
        request.get("value").set(value);
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
