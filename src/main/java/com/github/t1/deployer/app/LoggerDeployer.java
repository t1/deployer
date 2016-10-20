package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LoggerAudit;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.app.Plan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.log.LogLevel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentState.*;

@Slf4j
public class LoggerDeployer
        extends ResourceDeployer<LoggerPlan, LoggerResourceBuilder, LoggerResource, LoggerAuditBuilder> {
    public LoggerDeployer() {
        this.<LogLevel>property("level")
                .resource(LoggerResource::level)
                .plan(LoggerPlan::getLevel)
                .addTo(LoggerResourceBuilder::level)
                .write(LoggerResource::writeLevel);
        this.<Boolean>property("use-parent-handlers")
                .resource(LoggerResource::useParentHandlers)
                .plan(LoggerPlan::getUseParentHandlers)
                .addTo(LoggerResourceBuilder::useParentHandlers)
                .write(LoggerResource::writeUseParentHandlers)
                .writeFilter(LoggerResource::isNotRoot);
    }

    @Override protected Stream<LoggerResource> getAll() { return container.allLoggers(); }

    @Override protected Stream<LoggerPlan> of(Plan plan) { return plan.loggers(); }

    @Override protected String getType() { return "loggers"; }

    @Override protected LoggerAuditBuilder auditBuilder(LoggerResource logger) {
        return LoggerAudit.builder().category(logger.category());
    }

    @Override protected LoggerResourceBuilder resourceBuilder(LoggerPlan plan) {
        return container.builderFor(plan.getCategory());
    }

    @Override protected Predicate<LoggerResource> matches(LoggerPlan plan) { return plan.getCategory()::matches; }

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

    @Override protected void remove(LoggerResource resource) {
        if (resource.isNotRoot())
            super.remove(resource);
    }

    @Override protected void auditRemove(LoggerResource resource, LoggerPlan plan, LoggerAuditBuilder audit) {
        super.auditRemove(resource, plan, audit);
        for (LogHandlerName handler : resource.handlers())
            audit.change("handler", handler, null);
    }

    @Override public void read(PlanBuilder builder) {
        getAll().forEach(logger ->
                builder.logger(LoggerPlan
                        .builder()
                        .category(logger.category())
                        .state(deployed)
                        .level(logger.level())
                        .handlers(logger.handlers())
                        .useParentHandlers(logger.isDefaultUseParentHandlers() ? null : logger.useParentHandlers())
                        .build()));
    }
}
