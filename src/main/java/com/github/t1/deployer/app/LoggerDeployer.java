package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LoggerAudit;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.container.LoggerResource;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Plan.PlanBuilder;
import com.github.t1.log.LogLevel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.*;

import static com.github.t1.deployer.model.DeploymentState.*;

@Slf4j
class LoggerDeployer extends ResourceDeployer<LoggerPlan, LoggerResourceBuilder, LoggerResource, LoggerAuditBuilder> {
    public LoggerDeployer() {
        property("level", LogLevel.class);
        property("use-parent-handlers", Boolean.class);
    }

    private <T> Property<T> property(String name, Class<T> type) {
        return property(name, type, LoggerResource.class, LoggerPlan.class);
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
            List<LogHandlerName> oldHandlers = new ArrayList<>(resource.handlers());
            List<LogHandlerName> newHandlers = plan
                    .getHandlers()
                    .stream()
                    .filter(newHandler -> !oldHandlers.remove(newHandler))
                    .collect(Collectors.toList());
            newHandlers.forEach(resource::addLoggerHandler);
            oldHandlers.forEach(resource::removeLoggerHandler);
            audit.change("handlers",
                    (oldHandlers.isEmpty()) ? null : oldHandlers,
                    (newHandlers.isEmpty()) ? null : newHandlers);
        }
    }

    @Override protected LoggerResourceBuilder buildResource(LoggerPlan plan, LoggerAuditBuilder audit) {
        LoggerResourceBuilder builder = super.buildResource(plan, audit);
        if (!plan.getHandlers().isEmpty())
            audit.change("handlers", null, plan.getHandlers());
        return builder.handlers(plan.getHandlers());
    }

    @Override protected void auditRemove(LoggerResource resource, LoggerAuditBuilder audit) {
        super.auditRemove(resource, audit);

        if (!resource.handlers().isEmpty())
            audit.change("handlers", resource.handlers(), null);
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
