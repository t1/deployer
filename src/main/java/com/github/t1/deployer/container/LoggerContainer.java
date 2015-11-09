package com.github.t1.deployer.container;

import static com.github.t1.deployer.model.LoggerConfig.*;
import static com.github.t1.log.LogLevel.*;
import static com.github.t1.ramlap.ProblemDetail.*;
import static java.util.stream.Collectors.*;

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
        List<LoggerConfig> loggers =
                execute(readLogger("*")).asList().stream().map(node -> toLogger(node.get("result"))).collect(toList());
        Collections.sort(loggers);
        loggers.add(0, toLogger(execute(readLogger(ROOT))));
        return loggers;
    }

    private static ModelNode readLogger(String name) {
        return readResource(loggerAddress(name));
    }

    private static ModelNode loggerAddress(String name) {
        ModelNode request = new ModelNode();
        ModelNode logging = request.get("address").add("subsystem", "logging");
        if (ROOT.equals(name) || name.isEmpty())
            logging.add("root-logger", "ROOT");
        else
            logging.add("logger", name);
        return request;
    }

    private LoggerConfig toLogger(ModelNode cliLogger) {
        ModelNode category = cliLogger.get("category");
        String name = (category.isDefined()) ? category.asString() : "";
        LogLevel level = LogLevel.valueOf(cliLogger.get("level").asString());
        return new LoggerConfig(name, level);
    }

    public boolean hasLogger(LoggerConfig logger) {
        return hasLogger(logger.getCategory());
    }

    public boolean hasLogger(String category) {
        ModelNode result = executeRaw(readLogger(category));
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
        ModelNode result = executeRaw(readLogger(category));
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
        if (logger.isRoot())
            throw new RuntimeException("can't add root logger");
        execute(addLogger(logger));
    }

    private static ModelNode addLogger(LoggerConfig logger) {
        ModelNode request = loggerAddress(logger.getCategory());
        request.get("operation").set("add");
        request.get("level").set(logger.getLevel().name());
        return request;
    }

    public void remove(LoggerConfig logger) {
        if (logger.isRoot())
            throw new RuntimeException("can't remove root logger");
        execute(removeLogger(logger));
    }

    private static ModelNode removeLogger(LoggerConfig logger) {
        ModelNode request = loggerAddress(logger.getCategory());
        request.get("operation").set("remove");
        return request;
    }

    public void update(LoggerConfig logger) {
        ModelNode request = loggerAddress(logger.getCategory());
        request.get("operation").set("write-attribute");
        request.get("name").set("level");
        request.get("value").set(logger.getLevel().name());

        ModelNode result = execute(request);
        log.debug("result: {}", result);
    }
}
