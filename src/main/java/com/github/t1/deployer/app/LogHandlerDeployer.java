package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LogHandlerAudit;
import com.github.t1.deployer.app.Audit.LogHandlerAudit.LogHandlerAuditBuilder;
import com.github.t1.deployer.app.Plan.*;
import com.github.t1.deployer.container.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentState.*;
import static java.util.Collections.*;

@Slf4j
public class LogHandlerDeployer extends AbstractDeployer<LogHandlerPlan, LogHandlerResource, LogHandlerAuditBuilder> {
    @Inject Container container;
    private List<LogHandlerResource> remaining;

    @Override protected void init() { this.remaining = container.allLogHandlers(); }

    @Override protected Stream<LogHandlerPlan> of(Plan plan) { return plan.logHandlers(); }

    @Override protected String getType() { return "log-handlers"; }

    @Override protected LogHandlerAuditBuilder auditBuilder(LogHandlerResource resource) {
        return LogHandlerAudit.builder().type(resource.type()).name(resource.name());
    }

    @Override protected LogHandlerResource getResource(LogHandlerPlan plan) {
        return container.logHandler(plan.getType(), plan.getName()).build();
    }

    @Override
    protected void update(LogHandlerResource resource, LogHandlerPlan plan, LogHandlerAuditBuilder audit) {
        boolean removed = remaining.removeIf(plan.getName()::matches);
        assert removed : "expected [" + resource + "] to be in existing " + remaining;

        if (!Objects.equals(resource.level(), plan.getLevel())) {
            resource.updateLevel(plan.getLevel());
            audit.change("level", resource.level(), plan.getLevel());
        }
        if (!Objects.equals(resource.format(), plan.getFormat())) {
            resource.updateFormat(plan.getFormat());
            audit.change("format", resource.format(), plan.getFormat());
        }
        if (!Objects.equals(resource.formatter(), plan.getFormatter())) {
            resource.updateFormatter(plan.getFormatter());
            audit.change("formatter", resource.formatter(), plan.getFormatter());
        }
        if (!Objects.equals(resource.encoding(), plan.getEncoding())) {
            resource.updateEncoding(plan.getEncoding());
            audit.change("encoding", resource.encoding(), plan.getEncoding());
        }
        if (!Objects.equals(resource.file(), plan.getFile())) {
            resource.updateFile(plan.getFile());
            audit.change("file", resource.file(), plan.getFile());
        }
        if (!Objects.equals(resource.suffix(), plan.getSuffix())) {
            resource.updateSuffix(plan.getSuffix());
            audit.change("suffix", resource.suffix(), plan.getSuffix());
        }
        if (!Objects.equals(resource.module(), plan.getModule())) {
            resource.updateModule(plan.getModule());
            audit.change("module", resource.module(), plan.getModule());
        }
        if (!Objects.equals(resource.class_(), plan.getClass_())) {
            resource.updateClass(plan.getClass_());
            audit.change("class", resource.class_(), plan.getClass_());
        }
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

    @Override
    protected Supplier<LogHandlerResource> buildResource(LogHandlerPlan plan, LogHandlerAuditBuilder audit) {
        audit.change("level", null, plan.getLevel());
        audit.change("format", null, plan.getFormat());
        audit.change("formatter", null, plan.getFormatter());
        audit.change("encoding", null, plan.getEncoding());

        audit.change("file", null, plan.getFile());
        audit.change("suffix", null, plan.getSuffix());

        audit.change("module", null, plan.getModule());
        audit.change("class", null, plan.getClass_());

        plan.getProperties().forEach((key, value) -> audit.change("property/" + key, null, value));

        return container.logHandler(plan.getType(), plan.getName())
                        .level(plan.getLevel())
                        .format(plan.getFormat())
                        .formatter(plan.getFormatter())
                        .encoding(plan.getEncoding())
                        .file(plan.getFile())
                        .suffix(plan.getSuffix())
                        .module(plan.getModule())
                        .class_(plan.getClass_())
                        .properties(plan.getProperties());
    }

    @Override
    protected void auditRemove(LogHandlerResource resource, LogHandlerPlan plan, LogHandlerAuditBuilder audit) {
        audit.change("level", resource.level(), null);
        audit.change("format", resource.format(), null);
        audit.change("formatter", resource.formatter(), null);
        audit.change("encoding", resource.encoding(), null);
        audit.change("file", resource.file(), null);
        audit.change("suffix", resource.suffix(), null);
        audit.change("module", resource.module(), null);
        audit.change("class", resource.class_(), null);
        if (resource.properties() != null)
            resource.properties().forEach((key, value) -> audit.change("property/" + key, value, null));
    }

    @Override public void cleanup() {
        for (LogHandlerResource handler : remaining) {
            LogHandlerAuditBuilder audit = LogHandlerAudit.builder().name(handler.name()).type(handler.type());
            auditRemove(handler, null, audit); // we can call this here only because it doesn't access the plan!
            audits.add(audit.removed());
            handler.remove();
        }
    }

    @Override public void read(PlanBuilder plan) {
        for (LogHandlerResource handler : container.allLogHandlers()) {
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
        }
    }
}
