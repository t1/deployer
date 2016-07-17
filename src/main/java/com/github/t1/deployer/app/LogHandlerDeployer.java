package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.ConfigurationPlan.LogHandlerConfig;
import com.github.t1.deployer.container.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class LogHandlerDeployer extends AbstractDeployer<LogHandlerConfig, LogHandlerResource> {
    private final LoggerContainer loggers;

    public LogHandlerDeployer(LoggerContainer loggers, Audits audits) {
        super(audits);
        this.loggers = loggers;
    }

    @Override protected AuditBuilder buildAudit(LogHandlerResource resource) {
        return LogHandlerAudit.builder().type(resource.type()).name(resource.name());
    }

    @Override protected LogHandlerResource getResource(LogHandlerConfig plan) {
        return loggers.handler(plan.getType(), plan.getName()).build();
    }

    @Override
    protected void update(LogHandlerResource resource, LogHandlerConfig plan, AuditBuilder audit) {
        if (!Objects.equals(resource.level(), plan.getLevel())) {
            resource.writeLevel(plan.getLevel());
            audit.change("level", resource.level(), plan.getLevel());
        }
        if (!Objects.equals(resource.file(), plan.getFile())) {
            resource.writeFile(plan.getFile());
            audit.change("file", resource.file(), plan.getFile());
        }
        if (!Objects.equals(resource.suffix(), plan.getSuffix())) {
            resource.writeSuffix(plan.getSuffix());
            audit.change("suffix", resource.suffix(), plan.getSuffix());
        }
        if (!Objects.equals(resource.format(), plan.getFormat())) {
            resource.writeFormat(plan.getFormat());
            audit.change("format", resource.format(), plan.getFormat());
        }
        if (!Objects.equals(resource.formatter(), plan.getFormatter())) {
            resource.writeFormatter(plan.getFormatter());
            audit.change("formatter", resource.formatter(), plan.getFormatter());
        }
    }

    @Override
    protected LogHandlerResource buildResource(LogHandlerConfig plan, AuditBuilder audit) {
        return loggers.handler(plan.getType(), plan.getName())
                      .file(plan.getFile())
                      .level(plan.getLevel())
                      .suffix(plan.getSuffix())
                      .format(plan.getFormat())
                      .formatter(plan.getFormatter())
                      .build();
    }

    @Override protected void auditRemove(LogHandlerResource resource, AuditBuilder audit) {
        audit.change("level", resource.level(), null)
             .change("file", resource.file(), null)
             .change("suffix", resource.suffix(), null);
        if (resource.format() != null)
            audit.change("format", resource.format(), null);
        if (resource.formatter() != null)
            audit.change("formatter", resource.formatter(), null);
    }
}
