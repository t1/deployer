package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.ConfigurationPlan.LoggerConfig;
import com.github.t1.deployer.container.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.github.t1.deployer.model.Tools.*;

@Slf4j
public class LoggerDeployer extends AbstractDeployer<LoggerConfig, LoggerResource> {
    private final LoggerContainer loggers;

    public LoggerDeployer(LoggerContainer loggers, Audits audits) {
        super(audits);
        this.loggers = loggers;
    }

    @Override
    protected AuditBuilder buildAudit(LoggerResource logger) {
        return LoggerAudit.builder().category(logger.category());
    }

    @Override
    protected LoggerResource getResource(LoggerConfig plan) { return loggers.logger(plan.getCategory()).build(); }

    @Override
    protected void update(LoggerResource logger, LoggerConfig plan, AuditBuilder audit) {
        if (!Objects.equals(logger.level(), plan.getLevel())) {
            logger.writeLevel(plan.getLevel());
            audit.change("level", logger.level(), plan.getLevel());
        }
        if (!logger.isRoot() && !Objects.equals(logger.useParentHandlers(),
                nvl(plan.getUseParentHandlers(), true))) {
            logger.writeUseParentHandlers(plan.getUseParentHandlers());
            audit.change("useParentHandlers", logger.useParentHandlers(), plan.getUseParentHandlers());
        }
        if (!Objects.equals(logger.handlers(), plan.getHandlers())) {
            List<LogHandlerName> existing = new ArrayList<>(logger.handlers());
            for (LogHandlerName newHandler : plan.getHandlers())
                if (!existing.remove(newHandler)) {
                    logger.addLoggerHandler(newHandler);
                    audit.change("handlers", null, newHandler);
                }
            for (LogHandlerName oldHandler : existing) {
                logger.removeLoggerHandler(oldHandler);
                audit.change("handlers", oldHandler, null);
            }
        }
    }

    @Override protected LoggerResource buildResource(LoggerConfig plan, AuditBuilder audit) {
        LoggerResource logger = loggers
                .logger(plan.getCategory())
                .level(plan.getLevel())
                .handlers(plan.getHandlers())
                .useParentHandlers(plan.getUseParentHandlers())
                .build();
        audit.change("level", null, plan.getLevel())
             .change("useParentHandlers", null, plan.getUseParentHandlers());
        for (LogHandlerName handler : plan.getHandlers())
            audit.change("handlers", null, handler);
        return logger;
    }

    @Override protected void auditRemove(LoggerResource resource, AuditBuilder audit) {
        audit.change("level", resource.level(), null)
             .change("useParentHandlers", resource.useParentHandlers(), null);
        for (LogHandlerName handler : resource.handlers())
            audit.change("handlers", handler, null);
    }
}
