package com.github.t1.deployer.container;

import com.github.t1.log.LogLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class LogHandler {
    private final String name;
    private final Function<ModelNode, ModelNode> execute;

    public void setLevel(LogLevel level) {
        ModelNode request = newModelNode();

        request.get("operation").set("write-attribute");
        request.get("name").set("level");
        request.get("value").set(level.name());

        ModelNode result = execute.apply(request);
        log.debug("result: {}", result);
    }

    @NotNull private ModelNode newModelNode() {
        ModelNode request = new ModelNode();
        ModelNode logging = request.get("address").add("subsystem", "logging");
        if ("CONSOLE".equals(name))
            logging.add("console-handler", "CONSOLE");
        else
            logging.add("periodic-rotating-file-handler", name);
        return request;
    }
}
