package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LoggerAudit;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.container.LoggerResource;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Plan.*;
import com.github.t1.log.LogLevel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentState.*;

@Slf4j
public class LoggerDeployer
        extends ResourceDeployer<LoggerPlan, LoggerResourceBuilder, LoggerResource, LoggerAuditBuilder> {
    public LoggerDeployer() {
        property("level", LogLevel.class, LoggerResource.class, LoggerPlan.class);
        property("use-parent-handlers", Boolean.class, LoggerResource.class, LoggerPlan.class)
                .writeFilter(LoggerResource::isNotRoot);
    }

    @Override protected String getType() { return "loggers"; }

    @Override protected Stream<LoggerResource> existingResources() { return container.allLoggers(); }

    @Override protected Stream<LoggerPlan> resourcesIn(Plan plan) { return plan.loggers(); }

    @Override protected LoggerAuditBuilder auditBuilder(LoggerResource logger) {
        return LoggerAudit.builder().category(logger.category());
    }

    @Override protected LoggerResourceBuilder resourceBuilder(LoggerPlan plan) {
        return container.builderFor(plan.getCategory());
    }

    @Override protected void update(LoggerResource resource, LoggerPlan plan, LoggerAuditBuilder audit) {
        super.update(resource, plan, audit);

        if (!Objects.equals(resource.handlers(), plan.getHandlers())) {
            List<LogHandlerName> existing = new ArrayList<>(resource.handlers());
            plan.getHandlers()
                .stream()
                .filter(newHandler -> !existing.remove(newHandler))
                .forEach(newHandler -> {
                    resource.addLoggerHandler(newHandler);
                    audit.change("handler", null, newHandler);
                });
            for (LogHandlerName oldHandler : existing) {
                resource.removeLoggerHandler(oldHandler);
                audit.change("handler", oldHandler, null);
            }
        }
    }

    @Override protected LoggerResourceBuilder buildResource(LoggerPlan plan, LoggerAuditBuilder audit) {
        LoggerResourceBuilder builder = super.buildResource(plan, audit);
        for (LogHandlerName handler : plan.getHandlers())
            audit.change("handler", null, handler);
        return builder.handlers(plan.getHandlers());
    }

    @Override protected void auditRegularRemove(LoggerResource resource, LoggerPlan plan, LoggerAuditBuilder audit) {
        super.auditRegularRemove(resource, plan, audit);

        for (LogHandlerName handler : resource.handlers())
            audit.change("handler", handler, null);
    }

    @Override protected void cleanupRemove(LoggerResource resource) {
        if (resource.isNotRoot())
            super.cleanupRemove(resource);
    }

    @Override public void read(PlanBuilder builder, LoggerResource logger) {
        builder.logger(LoggerPlan
                .builder()
                .category(logger.category())
                .state(deployed)
                .level(logger.level())
                .handlers(logger.handlers())
                .useParentHandlers(logger.isDefaultUseParentHandlers() ? null : logger.useParentHandlers())
                .build());
    }
}
