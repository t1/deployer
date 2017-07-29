package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.AuditBuilder;
import com.github.t1.deployer.container.AbstractResource;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Plan.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.t1.problem.WebException.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractDeployer<
        PLAN extends AbstractPlan,
        RESOURCE extends AbstractResource<RESOURCE>,
        AUDIT extends AuditBuilder>
        implements Deployer {
    @Inject Audits audits;
    @Inject @Config("managed.resources") List<String> managedResourceNames;
    @Inject @Config("pinned.resources") Map<String, List<String>> pinnedResourceNames;

    private List<RESOURCE> remaining;

    /* ------------------------------------------------------------------------------------------------------------ */
    @Override public void read(PlanBuilder builder) {
        unpinnedResources().forEach(resource -> read(builder, resource));
    }

    private Stream<RESOURCE> unpinnedResources() {
        return existingResources().filter(resource -> !isPinned(resource.getId()));
    }

    protected abstract Stream<RESOURCE> existingResources();

    protected boolean isPinned(String name) {
        return pinnedResourceNames.getOrDefault(getType(), emptyList()).contains(name);
    }

    protected abstract String getType();

    protected abstract void read(PlanBuilder builder, RESOURCE resource);


    /* ------------------------------------------------------------------------------------------------------------ */
    @Override public void apply(Plan plan) {
        if (log.isDebugEnabled())
            log.debug("apply {} -> {}", resourcesIn(plan).collect(toList()), this.getClass().getSimpleName());
        this.remaining = unpinnedResources().collect(toList());

        resourcesIn(plan).forEach(this::apply);

        if (isManaged())
            remaining.forEach(this::cleanup);
    }

    protected abstract Stream<PLAN> resourcesIn(Plan plan);

    public void apply(PLAN plan) {
        if (isPinned(plan.getId()))
            throw badRequest("resource is pinned: " + plan);

        RESOURCE resource = readResource(plan);
        log.debug("apply {} to {}", plan, resource);
        AUDIT audit = auditBuilder(resource);
        switch (plan.getState()) {
        case deployed:
            if (resource.isDeployed()) {
                removeFromRemaining(resource);
                update(resource, plan, audit);
                auditChange(plan, audit);
            } else {
                resource = buildResource(plan, audit).get();
                if (resource != null) {
                    resource.add();
                    audits.add(audit.added());
                }
            }
            return;
        case undeployed:
            if (resource.isDeployed()) {
                removeFromRemaining(resource);
                auditRegularRemove(resource, plan, audit);
                resource.addRemoveStep();
                audits.add(audit.removed());
            } else {
                log.info("resource already removed: {}", plan);
            }
            return;
        }
        throw new UnsupportedOperationException("unhandled case: " + plan.getState());
    }

    protected abstract RESOURCE readResource(PLAN plan);

    protected abstract AUDIT auditBuilder(RESOURCE resource);

    private void removeFromRemaining(RESOURCE resource) {
        boolean removed = remaining.removeIf(resource::matchesId);
        assert removed : "expected [" + resource + "] to be in " + remaining;
    }

    protected abstract void update(RESOURCE resource, PLAN plan, AUDIT audit);

    private void auditChange(PLAN plan, AUDIT audit) {
        Audit changed = audit.changed();
        if (changed.changeCount() > 0)
            audits.add(changed);
        else
            log.info("resource already up-to-date: {}", plan);
    }

    protected abstract Supplier<RESOURCE> buildResource(PLAN plan, AUDIT audit);

    protected abstract void auditRegularRemove(RESOURCE resource, PLAN plan, AUDIT audit);

    protected abstract void cleanup(RESOURCE resource);

    public boolean isManaged() {
        return managedResourceNames.equals(singletonList("all")) || managedResourceNames.contains(getType());
    }
}
