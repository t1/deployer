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

    private String file;
    private String suffix;
    private String formatter;


    public LogHandler writeLevel(LogLevel level) {
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

    public LogHandler file(String file) {
        this.file = file;
        return this;
    }

    public LogHandler suffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public LogHandler formatter(String formatter) {
        this.formatter = formatter;
        return this;
    }
}
