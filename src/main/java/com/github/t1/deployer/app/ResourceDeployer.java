package com.github.t1.deployer.app;

import com.github.t1.deployer.container.AbstractResource;
import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.Plan.AbstractPlan;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.t1.deployer.model.Password.CONCEALED;

@Slf4j
abstract class ResourceDeployer<
    PLAN extends AbstractPlan,
    BUILDER extends Supplier<RESOURCE>,
    RESOURCE extends AbstractResource<RESOURCE>,
    AUDIT extends Audit>
    extends AbstractDeployer<PLAN, RESOURCE, AUDIT> {

    class Property<TYPE> {
        private final String name;
        private Function<RESOURCE, TYPE> resource;
        private Function<PLAN, TYPE> plan;
        private BiFunction<BUILDER, TYPE, BUILDER> addTo;
        private BiConsumer<RESOURCE, TYPE> write;
        private boolean confidential = false;

        Property(String name) { this.name = name; }

        void update(RESOURCE resource, PLAN plan, AUDIT audit) {
            TYPE before = from(resource);
            TYPE after = from(plan);
            if (!Objects.equals(before, after)) {
                write(resource, plan);
                audit.changeRaw(name(), conceal(before), conceal(after));
            }
        }

        String conceal(Object value) {
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

        public String name() {return this.name;}

        public Function<RESOURCE, TYPE> resource() {return this.resource;}

        public Function<PLAN, TYPE> plan() {return this.plan;}

        BiFunction<BUILDER, TYPE, BUILDER> addTo() {return this.addTo;}

        public BiConsumer<RESOURCE, TYPE> write() {return this.write;}

        public Property<TYPE> resource(Function<RESOURCE, TYPE> resource) {
            this.resource = resource;
            return this;
        }

        public Property<TYPE> plan(Function<PLAN, TYPE> plan) {
            this.plan = plan;
            return this;
        }

        Property<TYPE> addTo(BiFunction<BUILDER, TYPE, BUILDER> addTo) {
            this.addTo = addTo;
            return this;
        }

        public Property<TYPE> write(BiConsumer<RESOURCE, TYPE> write) {
            this.write = write;
            return this;
        }

        @SuppressWarnings("UnusedReturnValue") Property<TYPE> confidential() {
            this.confidential = true;
            return this;
        }

        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Property))
                return false;
            final Property that = (Property) o;
            if (!that.canEqual(this))
                return false;
            if (!Objects.equals(this.name(), that.name()))
                return false;
            if (!Objects.equals(this.resource(), that.resource()))
                return false;
            if (!Objects.equals(this.plan(), that.plan()))
                return false;
            if (!Objects.equals(this.addTo(), that.addTo()))
                return false;
            if (!Objects.equals(this.write(), that.write()))
                return false;
            if (this.confidential != that.confidential)
                return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $name = this.name();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final Object $resource = this.resource();
            result = result * PRIME + ($resource == null ? 43 : $resource.hashCode());
            final Object $plan = this.plan();
            result = result * PRIME + ($plan == null ? 43 : $plan.hashCode());
            final Object $addTo = this.addTo();
            result = result * PRIME + ($addTo == null ? 43 : $addTo.hashCode());
            final Object $write = this.write();
            result = result * PRIME + ($write == null ? 43 : $write.hashCode());
            result = result * PRIME + (this.confidential ? 79 : 97);
            return result;
        }

        boolean canEqual(Object other) {return other instanceof ResourceDeployer.Property;}

        public String toString() {return "ResourceDeployer.Property(name=" + this.name() + ", resource=" + this.resource() + ", plan=" + this.plan() + ", addTo=" + this.addTo() + ", write=" + this.write() + ", confidential=" + this.confidential + ")";}
    }

    @Inject Container container;

    private final List<Property<?>> properties = new ArrayList<>();


    protected <TYPE> Property<TYPE> property(String name, Class<TYPE> type, Class<RESOURCE> resource, Class<PLAN> plan) {
        return this.<TYPE>property(name)
            .resource(function(resource, toMethodName(name, false), type))
            .plan(function(plan, ((boolean.class.equals(type)) ? "is" : "get") + toMethodName(name, true), type))
            .addTo(addTo(resource, type, name))
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

    @NotNull private <TYPE> BiFunction<BUILDER, TYPE, BUILDER> addTo(Class<RESOURCE> resource, Class<TYPE> type, String name) {
        Class<BUILDER> builderClass;
        try {
            builderClass = resourceBuilder(resource);
        } catch (ClassNotFoundException e) {
            //noinspection unchecked
            builderClass = (Class<BUILDER>) (Class) resource;
        }
        return addTo(builderClass, toMethodName(name, false), type);
    }

    @SuppressWarnings("unchecked") private Class<BUILDER> resourceBuilder(Class<RESOURCE> resource) throws ClassNotFoundException {
        return (Class<BUILDER>) Class.forName(resource.getName() + "$" + resource.getSimpleName() + "Builder");
    }

    private <TYPE> BiFunction<BUILDER, TYPE, BUILDER> addTo(Class<BUILDER> methodContainer, String name, Class<?>... paramTypes) {
        Method method = method(methodContainer, name, methodContainer, paramTypes);
        return (BUILDER builder, TYPE type) -> {
            try {
                if (methodContainer.isInstance(builder)) {
                    return methodContainer.cast(method.invoke(builder, type));
                } else {
                    method.invoke(builder.get(), type);
                    return builder;
                }
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
    private static <T, R> Method method(Class<T> methodContainer, String name, Class<R> returnType, Class<?>... paramTypes) {
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

    @Override protected void cleanup(RESOURCE resource) {
        log.info("cleanup remaining {}", resource);
        AUDIT audit = audit(resource);
        auditRemove(resource, audit);
        audits.add(audit.removed());

        resource.addRemoveStep();
    }

    protected void auditRemove(RESOURCE resource, AUDIT audit) {
        properties.forEach(property -> audit.change(property.name(), property.conceal(property.from(resource)), null));
    }
}
