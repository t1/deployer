package com.github.t1.deployer.container;

import static com.github.t1.log.LogLevel.*;
import static com.github.t1.ramlap.ProblemDetail.*;

import java.util.*;

import javax.ejb.Stateless;

import org.jboss.dmr.ModelNode;

import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.log.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Logged(level = INFO)
@Stateless
public class LoggerContainer extends AbstractContainer {
    public List<LoggerConfig> getLoggers() {
        List<LoggerConfig> loggers = new ArrayList<>();
        for (ModelNode cliLoggerMatch : readAllLoggers())
            loggers.add(toLogger(cliLoggerMatch.get("result")));
        Collections.sort(loggers);
        return loggers;
    }

    private List<ModelNode> readAllLoggers() {
        ModelNode result = execute(readLogger("*"));
        checkOutcome(result);
        return result.get("result").asList();
    }

    private static ModelNode readLogger(String name) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("logger", name);
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private LoggerConfig toLogger(ModelNode cliLogger) {
        String name = cliLogger.get("category").asString();
        LogLevel level = LogLevel.valueOf(cliLogger.get("level").asString());
        return new LoggerConfig(name, level);
    }

    public boolean hasLogger(LoggerConfig logger) {
        return hasLogger(logger.getCategory());
    }

    public boolean hasLogger(String category) {
        ModelNode result = execute(readLogger(category));
        String outcome = result.get("outcome").asString();
        if ("success".equals(outcome)) {
            return true;
        } else if (isNotFoundMessage(result)) {
            return false;
        } else {
            log.error("failed: {}", result);
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
        }
    }

    public LoggerConfig getLogger(String category) {
        ModelNode result = execute(readLogger(category));
        String outcome = result.get("outcome").asString();
        if ("success".equals(outcome)) {
            return toLogger(result.get("result"));
        } else if (isNotFoundMessage(result)) {
            throw notFound("no logger '" + category + "'");
        } else {
            log.error("failed: {}", result);
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
        }
    }

    public void add(LoggerConfig logger) {
        ModelNode result = execute(addLogger(logger));
        checkOutcome(result);
    }

    private static ModelNode addLogger(LoggerConfig logger) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("logger", logger.getCategory());
        node.get("operation").set("add");
        node.get("level").set(logger.getLevel().name());
        return node;
    }

    public void remove(LoggerConfig logger) {
        ModelNode result = execute(removeLogger(logger));
        checkOutcome(result);
    }

    private static ModelNode removeLogger(LoggerConfig logger) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("logger", logger.getCategory());
        node.get("operation").set("remove");
        return node;
    }

    public void update(LoggerConfig logger) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "logging").add("logger", logger.getCategory());
        node.get("operation").set("write-attribute");
        node.get("name").set("level");
        node.get("value").set(logger.getLevel().name());

        ModelNode result = execute(node);
        checkOutcome(result);
        log.debug("result: {}", result);
    }
}
