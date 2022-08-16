package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LoggerAudit;
import com.github.t1.deployer.container.LoggerResource;
import com.github.t1.deployer.model.LogHandlerName;
import com.github.t1.deployer.model.LoggerPlan;
import com.github.t1.deployer.model.Plan;
import com.github.t1.log.LogLevel;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentState.deployed;

@Slf4j
class LoggerDeployer extends ResourceDeployer<LoggerPlan, Supplier<LoggerResource>, LoggerResource, LoggerAudit> {
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

    @Override protected LoggerAudit audit(LoggerResource logger) {
        return new LoggerAudit().category(logger.category());
    }

    @Override protected Supplier<LoggerResource> resourceBuilder(LoggerPlan plan) {
        LoggerResource resource = container.builderFor(plan.getCategory());
        return () -> resource;
    }

    @Override protected void update(LoggerResource resource, LoggerPlan plan, LoggerAudit audit) {
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

    @Override protected Supplier<LoggerResource> buildResource(LoggerPlan plan, LoggerAudit audit) {
        LoggerResource resource = super.buildResource(plan, audit).get();
        if (!plan.getHandlers().isEmpty())
            audit.change("handlers", null, plan.getHandlers());
        resource.handlers(plan.getHandlers());
        return () -> resource;
    }

    @Override protected void auditRemove(LoggerResource resource, LoggerAudit audit) {
        super.auditRemove(resource, audit);

        if (!resource.handlers().isEmpty())
            audit.change("handlers", resource.handlers(), null);
    }

    @Override protected void cleanup(LoggerResource resource) {
        if (resource.isNotRoot())
            super.cleanup(resource);
    }

    @Override public void read(Plan plan, LoggerResource logger) {
        plan.addLogger(new LoggerPlan(logger.category())
            .setState(deployed)
            .setLevel(logger.level())
            .setHandlers(logger.handlers())
            .setUseParentHandlers(logger.isDefaultUseParentHandlers() ? null : logger.useParentHandlers()));
    }
}
