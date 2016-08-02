package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LoggerAudit;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.tools.Tools.*;

@Slf4j
public class LoggerDeployer extends AbstractDeployer<LoggerConfig, LoggerResource, LoggerAuditBuilder> {
    private final Container container;

    public LoggerDeployer(Container container, Audits audits) {
        super(audits);
        this.container = container;
    }

    @Override
    protected LoggerAuditBuilder buildAudit(LoggerResource logger) {
        return LoggerAudit.builder().category(logger.category());
    }

    @Override
    protected LoggerResource getResource(LoggerConfig plan) { return container.logger(plan.getCategory()).build(); }

    @Override
    protected void update(LoggerResource resource, LoggerConfig plan, LoggerAuditBuilder audit) {
        if (!Objects.equals(resource.level(), plan.getLevel())) {
            resource.writeLevel(plan.getLevel());
            audit.change("level", resource.level(), plan.getLevel());
        }
        if (!resource.isRoot() && !Objects.equals(resource.useParentHandlers(),
                nvl(plan.getUseParentHandlers(), true))) {
            resource.writeUseParentHandlers(plan.getUseParentHandlers());
            audit.change("useParentHandlers", resource.useParentHandlers(), plan.getUseParentHandlers());
        }
        if (!Objects.equals(resource.handlers(), plan.getHandlers())) {
            List<LogHandlerName> existing = new ArrayList<>(resource.handlers());
            for (LogHandlerName newHandler : plan.getHandlers())
                if (!existing.remove(newHandler)) {
                    resource.addLoggerHandler(newHandler);
                    audit.change("handler", null, newHandler);
                }
            for (LogHandlerName oldHandler : existing) {
                resource.removeLoggerHandler(oldHandler);
                audit.change("handler", oldHandler, null);
            }
        }
    }

    @Override protected LoggerResource buildResource(LoggerConfig plan, LoggerAuditBuilder audit) {
        audit.change("level", null, plan.getLevel())
             .change("useParentHandlers", null, plan.getUseParentHandlers());
        for (LogHandlerName handler : plan.getHandlers())
            audit.change("handler", null, handler);
        return container
                .logger(plan.getCategory())
                .level(plan.getLevel())
                .handlers(plan.getHandlers())
                .useParentHandlers(plan.getUseParentHandlers())
                .build();
    }

    @Override protected void auditRemove(LoggerResource resource, LoggerConfig plan,
            LoggerAuditBuilder audit) {
        audit.change("level", resource.level(), null)
             .change("useParentHandlers", resource.useParentHandlers(), null);
        for (LogHandlerName handler : resource.handlers())
            audit.change("handler", handler, null);
    }

    @Override public void read(ConfigurationPlanBuilder builder) {
        for (LoggerResource logger : container.allLoggers())
            builder.logger(LoggerConfig
                    .builder()
                    .category(logger.category())
                    .state(deployed)
                    .level(logger.level())
                    .handlers(logger.handlers())
                    .useParentHandlers(logger.useParentHandlers() ? null : logger.useParentHandlers())
                    .build());
    }
}
