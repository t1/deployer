package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import static com.github.t1.deployer.container.CLI.*;
import static lombok.AccessLevel.*;

@Slf4j
@Data
@Builder()
@AllArgsConstructor(access = PRIVATE)
public class LogHandler {
    public static class LogHandlerBuilder {
        @SuppressWarnings("unused") public LogHandlerBuilder() {}

        public LogHandlerBuilder(String name, LoggingHandlerType type, CLI cli) {
            this.name = name;
            this.type = type;
            this.cli = cli;
        }
    }

    @NonNull private final String name;
    @NonNull private final LoggingHandlerType type;

    @NonNull private final CLI cli;

    private LogLevel level;
    private String file;
    private String suffix;
    private String formatter;

    public boolean isDeployed() {
        ModelNode request = readResource(createRequestWithAddress());

        ModelNode result = cli.executeRaw(request);
        return cli.isOutcomeFound(result);
    }

    public LogHandler write() {
        ModelNode request = createRequestWithAddress();

        request.get("operation").set("write-attribute");
        request.get("name").set("level");
        request.get("value").set(level.name());

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
