package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LoggerAudit;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.tools.Tools.*;
import static java.util.stream.Collectors.*;

@Slf4j
public class LoggerDeployer extends AbstractDeployer<LoggerConfig, LoggerResource, LoggerAuditBuilder> {
    private static final String USE_PARENT_HANDLERS = "use-parent-handlers";

    @Inject Container container;
    private List<LoggerResource> remaining;

    @Override protected void init() {
        this.remaining = container.allLoggers().stream().filter(logger -> !logger.isRoot()).collect(toList());
    }

    @Override protected Stream<LoggerConfig> of(ConfigurationPlan plan) { return plan.loggers(); }

    @Override protected String getType() { return "loggers"; }

    @Override
    protected LoggerAuditBuilder buildAudit(LoggerResource logger) {
        return LoggerAudit.builder().category(logger.category());
    }

    @Override
    protected LoggerResource getResource(LoggerConfig plan) { return container.logger(plan.getCategory()).build(); }

    @Override
    protected void update(LoggerResource resource, LoggerConfig plan, LoggerAuditBuilder audit) {
        boolean removed = remaining.removeIf(plan.getCategory()::matches);
        assert removed : "expected [" + resource + "] to be in existing " + remaining;

        if (!Objects.equals(resource.level(), plan.getLevel())) {
            resource.writeLevel(plan.getLevel());
            audit.change("level", resource.level(), plan.getLevel());
        }
        if (!resource.isRoot() && !Objects.equals(resource.useParentHandlers(),
                nvl(plan.getUseParentHandlers(), true))) {
            resource.writeUseParentHandlers(plan.getUseParentHandlers());
            audit.change(USE_PARENT_HANDLERS, resource.useParentHandlers(), plan.getUseParentHandlers());
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
             .change(USE_PARENT_HANDLERS, null, plan.getUseParentHandlers());
        for (LogHandlerName handler : plan.getHandlers())
            audit.change("handler", null, handler);
        return container
                .logger(plan.getCategory())
                .level(plan.getLevel())
                .handlers(plan.getHandlers())
                .useParentHandlers(plan.getUseParentHandlers())
                .build();
    }

    @Override protected void auditRemove(LoggerResource resource, LoggerConfig plan, LoggerAuditBuilder audit) {
        audit.change("level", resource.level(), null)
             .change(USE_PARENT_HANDLERS, resource.useParentHandlers(), null);
        for (LogHandlerName handler : resource.handlers())
            audit.change("handler", handler, null);
    }

    @Override public void cleanup(Audits audits) {
        for (LoggerResource logger : remaining) {
            LoggerAuditBuilder audit = LoggerAudit.builder().category(logger.category());
            audit.change("level", logger.level(), null);
            audit.change(USE_PARENT_HANDLERS, logger.useParentHandlers(), null);
            for (LogHandlerName handler : logger.handlers())
                audit.change("handler", handler, null);
            audits.add(audit.removed());
            logger.remove();
        }
    }

    @Override public void read(ConfigurationPlanBuilder builder) {
        for (LoggerResource logger : container.allLoggers())
            builder.logger(LoggerConfig
                    .builder()
                    .category(logger.category())
                    .state(deployed)
                    .level(logger.level())
                    .handlers(logger.handlers())
                    .useParentHandlers(logger.isDefaultUseParentHandlers() ? null : logger.useParentHandlers())
                    .build());
    }
}
