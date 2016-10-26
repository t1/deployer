package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.AuditBuilder;
import com.github.t1.deployer.app.Plan.*;
import com.github.t1.deployer.container.AbstractResource;
import com.github.t1.deployer.model.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractDeployer<PLAN extends AbstractPlan, RESOURCE extends AbstractResource, AUDIT extends AuditBuilder> {
    @Inject Audits audits;
    @Inject @Config("managed.resources") List<String> managedResourceNames;
    @Inject @Config("pinned") Map<String, List<String>> pinnedResourceNames;

    public abstract void read(PlanBuilder builder);

    protected void init() {}

    public void apply(Plan plan) {
        if (log.isDebugEnabled())
            log.debug("apply {} -> {}", of(plan).collect(toList()), this.getClass().getSimpleName());
        init();
        of(plan).forEach(this::apply);
        if (isManaged())
            cleanup();
    }

    protected abstract Stream<PLAN> of(Plan plan);

    protected boolean isPinned(String name) {
        return pinnedResourceNames.getOrDefault(getType(), emptyList()).contains(name);
    }

    public boolean isManaged() {
        return managedResourceNames != null
                && (managedResourceNames.equals(singletonList("all")) || managedResourceNames.contains(getType()));
    }

    protected abstract String getType();

    public void apply(PLAN plan) {
        RESOURCE resource = getResource(plan);
        log.debug("apply {} to {}", plan, resource);
        AUDIT audit = auditBuilder(resource);
        switch (plan.getState()) {
        case deployed:
            if (resource.isDeployed()) {
                update(resource, plan, audit);

                Audit updated = audit.changed();
                if (updated.changeCount() > 0)
                    audits.add(updated);
                else
                    log.info("resource already up-to-date: {}", plan);
            } else {
                resource = buildResource(plan, audit).get();
                resource.add();
                audits.add(audit.added());
            }
            return;
        case undeployed:
            if (resource.isDeployed()) {
                resource.remove();
                auditRemove(resource, plan, audit);
                audits.add(audit.removed());
            } else {
                log.info("resource already removed: {}", plan);
            }
            return;
        }
        throw new UnsupportedOperationException("unhandled case: " + plan.getState());
    }

    protected abstract AUDIT auditBuilder(RESOURCE resource);

    protected abstract RESOURCE getResource(PLAN plan);

    protected abstract void update(RESOURCE resource, PLAN plan, AUDIT audit);

    protected abstract Supplier<RESOURCE> buildResource(PLAN plan, AUDIT audit);

    protected abstract void auditRemove(RESOURCE resource, PLAN plan, AUDIT audit);

    /**
     * This method is called after all planned resources have been visited, so managed resources can clean up,
     * e.g. deployments that are not in the plan get undeployed.
     */
    public abstract void cleanup();
}
