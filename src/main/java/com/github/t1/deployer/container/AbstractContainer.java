package com.github.t1.deployer.container;

import static com.github.t1.log.LogLevel.*;

import java.io.IOException;

import javax.ejb.Stateless;
import javax.inject.Inject;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;

import com.github.t1.log.Logged;

@Slf4j
@Logged(level = INFO)
@Stateless
abstract class AbstractContainer {
    private static final OperationMessageHandler LOGGING = new OperationMessageHandler() {
        @Override
        public void handleReport(MessageSeverity severity, String message) {
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
        }
    };

    @Inject
    ModelControllerClient client;

    @SneakyThrows(IOException.class)
    protected ModelNode execute(ModelNode command) {
        log.debug("execute command {}", command);
        ModelNode result = client.execute(command, LOGGING);
        log.trace("-> {}", result);
        return result;
    }

    protected void checkOutcome(ModelNode result) {
        String outcome = result.get("outcome").asString();
        if (!"success".equals(outcome)) {
            log.error("failed: {}", result);
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
        }
    }
}
