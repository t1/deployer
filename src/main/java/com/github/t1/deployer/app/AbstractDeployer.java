package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.AuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.AbstractConfig;
import com.github.t1.deployer.container.AbstractResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractDeployer<PLAN extends AbstractConfig, RESOURCE extends AbstractResource, AUDIT extends AuditBuilder> {
    private final Audits audits;

    public void apply(PLAN plan) {
        RESOURCE resource = getResource(plan);
        log.debug("apply {} to {}", plan, resource);
        AUDIT audit = buildAudit(resource);
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
                auditRemove(resource, plan, audit);
                audits.audit(audit.removed());
            } else {
                log.info("resource already removed: {}", plan);
            }
            return;
        }
        throw new UnsupportedOperationException("unhandled case: " + plan.getState());
    }

    protected abstract AUDIT buildAudit(RESOURCE resource);

    protected abstract RESOURCE getResource(PLAN plan);

    protected abstract void update(RESOURCE resource, PLAN plan, AUDIT audit);

    protected abstract RESOURCE buildResource(PLAN plan, AUDIT audit);

    protected abstract void auditRemove(RESOURCE resource, PLAN plan, AUDIT audit);

    /**
     * This method is called after all planned resources have been visited, so managed resources can clean up,
     * e.g. deployments that are not in the plan get undeployed.
     */
    public void cleanup(Audits audits) {}
}
