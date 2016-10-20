package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.AuditBuilder;
import com.github.t1.deployer.app.Plan.AbstractPlan;
import com.github.t1.deployer.container.*;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.inject.Inject;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

abstract class ResourceDeployer<
        PLAN extends AbstractPlan,
        BUILDER extends Supplier<RESOURCE>,
        RESOURCE extends AbstractResource,
        AUDIT extends AuditBuilder>
        extends AbstractDeployer<PLAN, RESOURCE, AUDIT> {

    @Data
    @Accessors(fluent = true, chain = true)
    class Property<TYPE> {
        private final String name;
        private Function<RESOURCE, TYPE> resource;
        private Function<PLAN, TYPE> plan;
        private BiFunction<BUILDER, TYPE, BUILDER> addTo;
        private BiConsumer<RESOURCE, TYPE> write;
        private Predicate<RESOURCE> writeFilter = r -> true;

        protected void update(RESOURCE resource, PLAN plan, AUDIT audit) {
            if (writeFilter.test(resource) && !Objects.equals(from(resource), from(plan))) {
                write(resource, plan);
                audit.change(name(), from(resource), from(plan));
            }
        }

        private TYPE from(RESOURCE resource) { return this.resource.apply(resource); }

        private TYPE from(PLAN plan) { return this.plan.apply(plan); }

        private void addTo(BUILDER builder, PLAN plan, AUDIT audit) {
            TYPE newValue = from(plan);
            this.addTo.apply(builder, newValue);
            audit.change(name, null, newValue);
        }

        private void write(RESOURCE resource, PLAN plan) { this.write.accept(resource, from(plan)); }
    }

    @Inject Container container;
    private List<RESOURCE> remaining;

    private final List<Property<?>> properties = new ArrayList<>();


    protected <TYPE> Property<TYPE> property(String name) {
        Property<TYPE> property = new Property<>(name);
        property(property);
        return property;
    }

    protected ResourceDeployer<PLAN, BUILDER, RESOURCE, AUDIT> property(Property<?> property) {
        properties.add(property);
        return this;
    }

    @Override protected void init() {
        this.remaining = getAll().collect(toList());
    }

    protected abstract Stream<RESOURCE> getAll();

    @Override protected RESOURCE getResource(PLAN plan) { return resourceBuilder(plan).get(); }

    protected abstract BUILDER resourceBuilder(PLAN plan);

    @Override protected void update(RESOURCE resource, PLAN plan, AUDIT audit) {
        boolean removed = remaining.removeIf(matches(plan));
        assert removed : "expected [" + resource + "] to be in existing " + remaining;

        properties.forEach(property -> property.update(resource, plan, audit));
    }

    protected abstract Predicate<RESOURCE> matches(PLAN plan);

    @Override protected BUILDER buildResource(PLAN plan, AUDIT audit) {
        BUILDER builder = resourceBuilder(plan);
        properties.forEach(property -> property.addTo(builder, plan, audit));
        return builder;
    }

    @Override protected void auditRemove(RESOURCE resource, PLAN plan, AUDIT audit) {
        properties.forEach(property -> audit.change(property.name(), property.from(resource), null));
    }

    @Override public void cleanup() { remaining.forEach(this::remove); }

    protected void remove(RESOURCE resource) {
        AUDIT audit = auditBuilder(resource);
        properties.forEach(property -> audit.change(property.name(), property.from(resource), null));
        audits.add(audit.removed());

        resource.remove();
    }
}
