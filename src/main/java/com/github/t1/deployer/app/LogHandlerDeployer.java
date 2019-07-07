package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LogHandlerAudit;
import com.github.t1.deployer.container.LogHandlerResource;
import com.github.t1.deployer.model.LogHandlerPlan;
import com.github.t1.deployer.model.Plan;
import com.github.t1.log.LogLevel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentState.deployed;
import static java.util.Collections.emptySet;

@Slf4j
class LogHandlerDeployer extends
    ResourceDeployer<LogHandlerPlan, Supplier<LogHandlerResource>, LogHandlerResource, LogHandlerAudit> {
    public LogHandlerDeployer() {
        property(LogLevel.class, "level");
        property(String.class, "format");
        property(String.class, "formatter");
        property(String.class, "encoding");

        property(String.class, "file");
        property(String.class, "suffix");

        property(String.class, "module");
        this.<String>property("class")
            .resource(LogHandlerResource::class_)
            .plan(LogHandlerPlan::getClass_)
            .addTo((Supplier<LogHandlerResource> t, String u) -> {
                t.get().class_(u);
                return t;
            })
            .write(LogHandlerResource::updateClass);
    }

    private void property(Class<?> type, String name) {
        property(name, type, LogHandlerResource.class, LogHandlerPlan.class);
    }

    @Override protected String getType() { return "log-handlers"; }

    @Override protected Stream<LogHandlerResource> existingResources() { return container.allLogHandlers(); }

    @Override protected Stream<LogHandlerPlan> resourcesIn(Plan plan) { return plan.logHandlers(); }

    @Override protected LogHandlerAudit audit(LogHandlerResource resource) {
        return new LogHandlerAudit().setType(resource.type()).setName(resource.name());
    }

    @Override protected Supplier<LogHandlerResource> resourceBuilder(LogHandlerPlan plan) {
        LogHandlerResource logHandlerResource = container.builderFor(plan.getType(), plan.getName());
        return () -> logHandlerResource;
    }

    @Override
    protected void update(LogHandlerResource resource, LogHandlerPlan plan, LogHandlerAudit audit) {
        super.update(resource, plan, audit);

        if (!Objects.equals(resource.properties(), plan.getProperties())) {
            Set<String> existing = new HashSet<>((resource.properties() == null) ? emptySet() : resource.properties().keySet());
            for (String key : plan.getProperties().keySet()) {
                String newValue = plan.getProperties().get(key);
                if (existing.remove(key)) {
                    String oldValue = resource.properties().get(key);
                    if (!Objects.equals(oldValue, newValue)) {
                        resource.updateProperty(key, newValue);
                        audit.change("property:" + key, oldValue, newValue);
                    }
                } else {
                    resource.addProperty(key, newValue);
                    audit.change("property:" + key, null, newValue);
                }
            }
            for (String removedKey : existing) {
                String oldValue = resource.properties().get(removedKey);
                resource.removeProperty(removedKey);
                audit.change("property:" + removedKey, oldValue, null);
            }
        }
    }

    @Override protected Supplier<LogHandlerResource> buildResource(LogHandlerPlan plan, LogHandlerAudit audit) {
        Supplier<LogHandlerResource> builder = super.buildResource(plan, audit);

        plan.getProperties().forEach((key, value) -> audit.change("property:" + key, null, value));
        builder.get().properties(plan.getProperties());

        return builder;
    }

    @Override
    protected void auditRegularRemove(LogHandlerResource resource, LogHandlerPlan plan, LogHandlerAudit audit) {
        super.auditRegularRemove(resource, plan, audit);

        if (resource.properties() != null)
            resource.properties().forEach((key, value) -> audit.change("property:" + key, value, null));
    }

    @Override public void read(Plan plan, LogHandlerResource handler) {
        LogHandlerPlan logHandlerPlan = new LogHandlerPlan(handler.name())
            .setType(handler.type())
            .setState(deployed)
            .setLevel(handler.level())
            .setFormat(handler.format())
            .setFormatter(handler.formatter())
            .setEncoding(handler.encoding())
            .setFile(handler.file())
            .setSuffix(handler.suffix())
            .setModule(handler.module())
            .setClass_(handler.class_());
        if (handler.properties() != null)
            handler.properties().forEach(logHandlerPlan::addProperty);
        plan.addLogHandler(logHandlerPlan);
    }
}
