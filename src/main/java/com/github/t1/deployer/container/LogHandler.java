package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.LogLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class LogHandler {
    private final String name;
    private final LoggingHandlerType type;
    private final Function<ModelNode, ModelNode> execute;

    public LogHandler writeLevel(LogLevel level) {
        ModelNode request = address();

        request.get("operation").set("write-attribute");
        request.get("name").set("level");
        request.get("value").set(level.name());

        execute.apply(request);

        return this;
    }

    private ModelNode address() {
        return new ModelNode()
                .get("address")
                .add("subsystem", "logging")
                .add(type.getTypeName(), name);
    }

    public LogHandler add() {
        ModelNode request = address();

        request.get("operation").set("add");
        execute.apply(request);

        return this;
    }
}
