package com.github.t1.deployer.container;

import com.github.t1.log.Logged;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

import javax.inject.Inject;
import java.io.IOException;

import static com.github.t1.log.LogLevel.*;

@Slf4j
@Logged(level = INFO)
public class CLI {
    private static final OperationMessageHandler LOGGING = (severity, message) -> {
        switch (severity) {
        case ERROR:
            log.error(message);
        case WARN:
            log.warn(message);
            break;
        case INFO:
            log.info(message);
            break;
        }
    };

    public static ModelNode readResource(ModelNode request) {
        request.get("operation").set("read-resource");
        request.get("recursive").set(true);
        return request;
    }

    @Inject
    public ModelControllerClient client;

    public ModelNode execute(ModelNode request) {
        ModelNode result = executeRaw(request);
        checkOutcome(result);
        return result.get("result");
    }

    @SneakyThrows(IOException.class)
    public ModelNode executeRaw(ModelNode command) {
        log.debug("execute command {}", command);
        ModelNode result = client.execute(command, LOGGING);
        log.debug("response {}", result);
        return result;
    }

    public boolean isNotFoundMessage(ModelNode result) {
        String message = result.get("failure-description").toString();
        boolean jboss7start = message.startsWith("\"JBAS014807: Management resource");
        boolean jboss8start = message.startsWith("\"WFLYCTL0216: Management resource");
        boolean notFoundEnd = message.endsWith(" not found\"");
        boolean isNotFound = (jboss7start || jboss8start) && notFoundEnd;
        log.trace("is not found message: jboss7start:{} jboss8start:{} notFoundEnd:{} -> {}: [{}]", //
                jboss7start, jboss8start, notFoundEnd, isNotFound, message);
        return isNotFound;
    }

    public void checkOutcome(ModelNode result) {
        String outcome = result.get("outcome").asString();
        if (!"success".equals(outcome)) {
            log.error("failed: {}", result);
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
        }
    }

    public boolean isOutcomeFound(ModelNode result) {
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
}
