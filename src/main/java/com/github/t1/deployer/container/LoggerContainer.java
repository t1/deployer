package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.log.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import javax.ejb.Stateless;
import java.util.*;

import static com.github.t1.deployer.model.LoggerConfig.*;
import static com.github.t1.log.LogLevel.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Logged(level = INFO)
@Stateless
public class LoggerContainer extends AbstractContainer {
    List<LoggerConfig> getLoggers() {
        List<LoggerConfig> loggers =
                execute(readLogger("*")).asList().stream().map(node -> toLogger(node, "")).collect(toList());
        Collections.sort(loggers);
        loggers.add(0, toLogger(executeRaw(readLogger(ROOT)), ""));
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

    private LoggerConfig toLogger(ModelNode response, String defaultCategory) {
        String name = getCategory(response, defaultCategory);
        LogLevel level = LogLevel.valueOf(response.get("result").get("level").asString());
        return new LoggerConfig(name, level);
    }

    private String getCategory(ModelNode response, String defaultCategory) {
        ModelNode category = response.get("result").get("category");
        if (category.isDefined()) // JBoss 8+
            return category.asString();
        if (response.get("address").isDefined())
            return getAddress(response).asString(); // JBoss 7
        // fall back for root logger or getLogger (which has only the requested category in JBoss 7)
        return defaultCategory;
    }

    private ModelNode getAddress(ModelNode response) {
        return response.get("address").asList().get(1).get("logger");
    }

    boolean hasLogger(LoggerConfig logger) {
        return hasLogger(logger.getCategory());
    }

    boolean hasLogger(String category) {
        ModelNode result = executeRaw(readLogger(category));
        return isOutcomeFound(result);
    }

    LoggerConfig getLogger(String category) {
        ModelNode result = executeRaw(readLogger(category));
        String outcome = result.get("outcome").asString();
        if ("success".equals(outcome)) {
            return toLogger(result, category);
        } else if (isNotFoundMessage(result)) {
            throw new RuntimeException("no logger '" + category + "'");
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

    void remove(LoggerConfig logger) {
        if (logger.isRoot())
            throw new RuntimeException("can't remove root logger");
        execute(removeLogger(logger));
    }

    private static ModelNode removeLogger(LoggerConfig logger) {
        ModelNode request = loggerAddress(logger.getCategory());
        request.get("operation").set("remove");
        return request;
    }

    void update(LoggerConfig logger) {
        ModelNode request = loggerAddress(logger.getCategory());
        request.get("operation").set("write-attribute");
        request.get("name").set("level");
        request.get("value").set(logger.getLevel().name());

        ModelNode result = execute(request);
        log.debug("result: {}", result);
    }

    public LogHandler getHandler(String name) {
        return new LogHandler(name, this::execute);
    }
}
