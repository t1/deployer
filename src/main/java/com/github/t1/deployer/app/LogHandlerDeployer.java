package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.LogHandlerAudit;
import com.github.t1.deployer.app.Audit.LogHandlerAudit.LogHandlerAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class LogHandlerDeployer extends AbstractDeployer<LogHandlerConfig, LogHandlerResource, LogHandlerAuditBuilder> {
    private final Container loggers;

    public LogHandlerDeployer(Container loggers, Audits audits) {
        super(audits);
        this.loggers = loggers;
    }

    @Override protected LogHandlerAuditBuilder buildAudit(LogHandlerResource resource) {
        return LogHandlerAudit.builder().type(resource.type()).name(resource.name());
    }

    @Override protected LogHandlerResource getResource(LogHandlerConfig plan) {
        return loggers.logHandler(plan.getType(), plan.getName()).build();
    }

    @Override
    protected void update(LogHandlerResource resource, LogHandlerConfig plan, LogHandlerAuditBuilder audit) {
        if (!Objects.equals(resource.level(), plan.getLevel())) {
            resource.updateLevel(plan.getLevel());
            audit.change("level", resource.level(), plan.getLevel());
        }
        if (!Objects.equals(resource.file(), plan.getFile())) {
            resource.updateFile(plan.getFile());
            audit.change("file", resource.file(), plan.getFile());
        }
        if (!Objects.equals(resource.suffix(), plan.getSuffix())) {
            resource.updateSuffix(plan.getSuffix());
            audit.change("suffix", resource.suffix(), plan.getSuffix());
        }
        if (!Objects.equals(resource.format(), plan.getFormat())) {
            resource.updateFormat(plan.getFormat());
            audit.change("format", resource.format(), plan.getFormat());
        }
        if (!Objects.equals(resource.formatter(), plan.getFormatter())) {
            resource.updateFormatter(plan.getFormatter());
            audit.change("formatter", resource.formatter(), plan.getFormatter());
        }
    }

    @Override
    protected LogHandlerResource buildResource(LogHandlerConfig plan, LogHandlerAuditBuilder audit) {
        audit.change("level", null, plan.getLevel())
             .change("file", null, plan.getFile())
             .change("suffix", null, plan.getSuffix());
        if (plan.getFormat() != null)
            audit.change("format", null, plan.getFormat());
        if (plan.getFormatter() != null)
            audit.change("formatter", null, plan.getFormatter());
        return loggers.logHandler(plan.getType(), plan.getName())
                      .level(plan.getLevel())
                      .file(plan.getFile())
                      .suffix(plan.getSuffix())
                      .format(plan.getFormat())
                      .formatter(plan.getFormatter())
                      .build();
    }

    @Override protected void auditRemove(LogHandlerResource resource, LogHandlerConfig plan,
            LogHandlerAuditBuilder audit) {
        audit.change("level", resource.level(), null)
             .change("file", resource.file(), null)
             .change("suffix", resource.suffix(), null);
        if (resource.format() != null)
            audit.change("format", resource.format(), null);
        if (resource.formatter() != null)
            audit.change("formatter", resource.formatter(), null);
    }

    @Override public void read(ConfigurationPlanBuilder builder) {
        // TODO implement
    }
}
