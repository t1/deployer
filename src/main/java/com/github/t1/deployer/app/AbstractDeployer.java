package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.AuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.AbstractConfig;
import com.github.t1.deployer.container.AbstractResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractDeployer<PLAN extends AbstractConfig, RESOURCE extends AbstractResource> {
    private final Audits audits;

    public void apply(PLAN plan) {
        RESOURCE resource = getResource(plan);
        log.debug("apply {} to {}", plan, resource);
        AuditBuilder audit = buildAudit(resource);
        switch (plan.getState()) {
        case deployed:
            if (resource.isDeployed()) {
                update(resource, plan, audit);

                Audit updated = audit.changed();
                if (updated.changeCount() > 0)
                    audits.audit(updated);
                else
                    log.info("resource already up-to-date: {}", plan);
            } else {
                resource = buildResource(plan, audit);
                resource.add();
                audits.audit(audit.added());
            }
            return;
        case undeployed:
            if (resource.isDeployed()) {
                resource.remove();
                auditRemove(resource, audit);
                audits.audit(audit.removed());
            } else {
                log.info("resource already removed: {}", plan);
            }
            return;
        }
        throw new UnsupportedOperationException("unhandled case: " + plan.getState());
    }

    protected abstract AuditBuilder buildAudit(RESOURCE resource);

    protected abstract RESOURCE getResource(PLAN plan);

    protected abstract void update(RESOURCE resource, PLAN plan, AuditBuilder audit);

    protected abstract RESOURCE buildResource(PLAN plan, AuditBuilder audit);

    protected abstract void auditRemove(RESOURCE resource, AuditBuilder audit);
}
