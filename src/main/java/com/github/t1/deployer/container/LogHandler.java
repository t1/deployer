package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import java.util.function.Function;

import static lombok.AccessLevel.*;

@Slf4j
@Data
@Builder()
@AllArgsConstructor(access = PRIVATE)
public class LogHandler {
    public static class LogHandlerBuilder {
        public LogHandlerBuilder() {}

        public LogHandlerBuilder(String name, LoggingHandlerType type, Function<ModelNode, ModelNode> execute) {
            this.name = name;
            this.type = type;
            this.execute = execute;
        }
    }

    @NonNull private final String name;
    @NonNull private final LoggingHandlerType type;

    @NonNull private final Function<ModelNode, ModelNode> execute;

    private LogLevel level;
    private String file;
    private String suffix;
    private String formatter;

    public LogHandler write() {
        ModelNode request = createRequastWithAddress();

        request.get("operation").set("write-attribute");
        request.get("name").set("level");
        request.get("value").set(level.name());

        execute.apply(request);

        return this;
    }

    private ModelNode createRequastWithAddress() {
        ModelNode request = new ModelNode();
        request.get("address")
               .add("subsystem", "logging")
               .add(type.getTypeName(), name);
        return request;
    }

    public LogHandler add() {
        ModelNode request = createRequastWithAddress();
        request.get("operation").set("add");
        request.get("file").get("path").set(file);
        request.get("file").get("relative-to").set("jboss.server.log.dir");
        request.get("suffix").set(suffix);
        request.get("formatter").set(formatter);

        execute.apply(request);

        return this;
    }
}
