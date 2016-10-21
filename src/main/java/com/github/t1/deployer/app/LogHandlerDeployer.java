package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LogHandlerAudit;
import com.github.t1.deployer.app.Audit.LogHandlerAudit.LogHandlerAuditBuilder;
import com.github.t1.deployer.app.Plan.*;
import com.github.t1.deployer.container.LogHandlerResource;
import com.github.t1.deployer.container.LogHandlerResource.LogHandlerResourceBuilder;
import com.github.t1.log.LogLevel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentState.*;
import static java.util.Collections.*;

@Slf4j
public class LogHandlerDeployer extends
        ResourceDeployer<LogHandlerPlan, LogHandlerResourceBuilder, LogHandlerResource, LogHandlerAuditBuilder> {
    public LogHandlerDeployer() {
        property("level", LogLevel.class, LogHandlerResource.class, LogHandlerPlan.class);
        property("format", String.class, LogHandlerResource.class, LogHandlerPlan.class);
        property("formatter", String.class, LogHandlerResource.class, LogHandlerPlan.class);
        property("encoding", String.class, LogHandlerResource.class, LogHandlerPlan.class);

        property("file", String.class, LogHandlerResource.class, LogHandlerPlan.class);
        property("suffix", String.class, LogHandlerResource.class, LogHandlerPlan.class);

        property("module", String.class, LogHandlerResource.class, LogHandlerPlan.class);
        this.<String>property("class")
                .resource(LogHandlerResource::class_)
                .plan(LogHandlerPlan::getClass_)
                .addTo(LogHandlerResourceBuilder::class_)
                .write(LogHandlerResource::updateClass);
    }

    @Override protected Stream<LogHandlerResource> getAll() { return container.allLogHandlers(); }

    @Override protected Stream<LogHandlerPlan> of(Plan plan) { return plan.logHandlers(); }

    @Override protected String getType() { return "log-handlers"; }

    @Override protected LogHandlerAuditBuilder auditBuilder(LogHandlerResource resource) {
        return LogHandlerAudit.builder().type(resource.type()).name(resource.name());
    }

    @Override protected LogHandlerResourceBuilder resourceBuilder(LogHandlerPlan plan) {
        return container.builderFor(plan.getType(), plan.getName());
    }

    @Override protected Predicate<LogHandlerResource> matches(LogHandlerPlan plan) { return plan.getName()::matches; }

    @Override
    protected void update(LogHandlerResource resource, LogHandlerPlan plan, LogHandlerAuditBuilder audit) {
        super.update(resource, plan, audit);

        if (!Objects.equals(resource.properties(), plan.getProperties())) {
            Set<String> existing = (resource.properties() == null) ? emptySet()
                    : new HashSet<>(resource.properties().keySet());
            for (String key : plan.getProperties().keySet()) {
                String newValue = plan.getProperties().get(key);
                if (existing.remove(key)) {
                    String oldValue = resource.properties().get(key);
                    if (!Objects.equals(oldValue, newValue)) {
                        resource.updateProperty(key, newValue);
                        audit.change("property/" + key, oldValue, newValue);
                    }
                } else {
                    resource.addProperty(key, newValue);
                    audit.change("property/" + key, null, newValue);
                }
            }
            for (String removedKey : existing) {
                String oldValue = resource.properties().get(removedKey);
                resource.removeProperty(removedKey);
                audit.change("property/" + removedKey, oldValue, null);
            }
        }
    }

    @Override protected LogHandlerResourceBuilder buildResource(LogHandlerPlan plan, LogHandlerAuditBuilder audit) {
        LogHandlerResourceBuilder builder = super.buildResource(plan, audit);

        plan.getProperties().forEach((key, value) -> audit.change("property/" + key, null, value));
        builder.properties(plan.getProperties());

        return builder;
    }

    @Override
    protected void auditRemove(LogHandlerResource resource, LogHandlerPlan plan, LogHandlerAuditBuilder audit) {
        super.auditRemove(resource, plan, audit);

        if (resource.properties() != null)
            resource.properties().forEach((key, value) -> audit.change("property/" + key, value, null));
    }

    @Override public void read(PlanBuilder plan) {
        container.allLogHandlers().forEach(handler -> {
            LogHandlerPlan.LogHandlerPlanBuilder logHandlerPlan = LogHandlerPlan
                    .builder()
                    .type(handler.type())
                    .name(handler.name())
                    .state(deployed)
                    .level(handler.level())
                    .format(handler.format())
                    .formatter(handler.formatter())
                    .encoding(handler.encoding())
                    .file(handler.file())
                    .suffix(handler.suffix())
                    .module(handler.module())
                    .class_(handler.class_());
            if (handler.properties() != null)
                handler.properties().forEach(logHandlerPlan::property);
            plan.logHandler(logHandlerPlan.build());
        });
    }
}
