package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.AuditBuilder;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.Plan.AbstractPlan;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import static com.github.t1.deployer.model.Password.*;

@Slf4j
abstract class ResourceDeployer<
        PLAN extends AbstractPlan,
        BUILDER extends Supplier<RESOURCE>,
        RESOURCE extends AbstractResource<RESOURCE>,
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
        private boolean confidential = false;

        protected void update(RESOURCE resource, PLAN plan, AUDIT audit) {
            if (writeFilter.test(resource)) {
                TYPE before = from(resource);
                TYPE after = from(plan);
                if (!Objects.equals(before, after)) {
                    write(resource, plan);
                    audit.changeRaw(name(), conceal(before), conceal(after));
                }
            }
        }

        protected String conceal(Object value) {
            return (value == null) ? null : confidential ? CONCEALED : value.toString();
        }

        private TYPE from(RESOURCE resource) { return this.resource.apply(resource); }

        private TYPE from(PLAN plan) { return this.plan.apply(plan); }

        private void addTo(BUILDER builder, PLAN plan, AUDIT audit) {
            TYPE newValue = from(plan);
            this.addTo.apply(builder, newValue);
            audit.change(name, null, conceal(newValue));
        }

        private void write(RESOURCE resource, PLAN plan) { this.write.accept(resource, from(plan)); }
    }

    @Inject Container container;

    private final List<Property<?>> properties = new ArrayList<>();


    @SneakyThrows(ReflectiveOperationException.class)
    protected <TYPE> Property<TYPE> property(String name, Class<TYPE> type, Class<RESOURCE> resource,
            Class<PLAN> plan) {
        @SuppressWarnings("unchecked")
        Class<BUILDER> resourceBuilder = (Class<BUILDER>) Class.forName(
                resource.getName() + "$" + resource.getSimpleName() + "Builder");
        return this.<TYPE>property(name)
                .resource(function(resource, toMethodName(name, false), type))
                .plan(function(plan, ((boolean.class.equals(type)) ? "is" : "get") + toMethodName(name, true), type))
                .addTo(addTo(resourceBuilder, toMethodName(name, false), type))
                .write(update(resource, "update" + toMethodName(name, true), type));
    }

    private String toMethodName(String name, boolean initCap) {
        StringBuilder out = new StringBuilder();
        for (char c : name.toCharArray())
            if ('-' == c) {
                initCap = true;
            } else if (initCap) {
                initCap = false;
                out.append(Character.toUpperCase(c));
            } else {
                out.append(c);
            }
        return out.toString();
    }

    private static <T, R> Function<T, R> function(Class<T> methodContainer, String name, Class<R> returnType) {
        Method method = method(methodContainer, name, returnType);
        return i -> {
            try {
                return returnType.cast(method.invoke(i));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("can't invoke", e);
            }
        };
    }

    private static <T, R> BiFunction<T, R, T> addTo(Class<T> methodContainer, String name, Class<?>... paramTypes) {
        Method method = method(methodContainer, name, methodContainer, paramTypes);
        return (i, j) -> {
            try {
                return methodContainer.cast(method.invoke(i, j));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("can't invoke", e);
            }
        };
    }

    private static <T, R> BiConsumer<T, R> update(Class<T> methodContainer, String name, Class<?>... paramTypes) {
        Method method = method(methodContainer, name, void.class, paramTypes);
        return (i, j) -> {
            try {
                method.invoke(i, j);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("can't invoke '" + name + "' in " + methodContainer.getSimpleName(), e);
            }
        };
    }

    @SneakyThrows(NoSuchMethodException.class)
    private static <T, R> Method method(Class<T> methodContainer, String name, Class<R> returnType,
            Class<?>... paramTypes) {
        Method method = methodContainer.getMethod(name, paramTypes);
        assert method.getReturnType().equals(returnType)
                : "expected return type " + returnType + " but found " + method.getReturnType()
                + " in " + methodContainer.getSimpleName() + "#" + name;
        return method;
    }

    protected <TYPE> Property<TYPE> property(String name) {
        Property<TYPE> property = new Property<>(name);
        properties.add(property);
        return property;
    }

    @Override protected RESOURCE readResource(PLAN plan) { return resourceBuilder(plan).get(); }

    protected abstract BUILDER resourceBuilder(PLAN plan);

    @Override protected void update(RESOURCE resource, PLAN plan, AUDIT audit) {
        properties.forEach(property -> property.update(resource, plan, audit));
    }

    @Override protected BUILDER buildResource(PLAN plan, AUDIT audit) {
        BUILDER builder = resourceBuilder(plan);
        properties.forEach(property -> property.addTo(builder, plan, audit));
        return builder;
    }

    @Override protected void auditRegularRemove(RESOURCE resource, PLAN plan, AUDIT audit) {
        auditRemove(resource, audit);
    }

    @Override protected void cleanupRemove(RESOURCE resource) {
        AUDIT audit = auditBuilder(resource);
        auditRemove(resource, audit);
        audits.add(audit.removed());

        resource.remove();
    }

    private void auditRemove(RESOURCE resource, AUDIT audit) {
        properties.forEach(property -> audit.change(property.name(), property.conceal(property.from(resource)), null));
    }
}
