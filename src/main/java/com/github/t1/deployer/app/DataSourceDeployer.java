package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.DataSourceAudit;
import com.github.t1.deployer.container.DataSourceResource;
import com.github.t1.deployer.model.Age;
import com.github.t1.deployer.model.DataSourcePlan;
import com.github.t1.deployer.model.DataSourcePlan.PoolPlan;
import com.github.t1.deployer.model.Plan;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentState.deployed;

@Slf4j
class DataSourceDeployer extends
    ResourceDeployer<DataSourcePlan, Supplier<DataSourceResource>, DataSourceResource, DataSourceAudit> {
    public DataSourceDeployer() {
        property("uri", URI.class);
        property("jndi-name", String.class);
        property("driver", String.class);
        property("xa", Boolean.class);
        property("user-name", String.class).confidential();
        property("password", String.class).confidential();
        this.<Integer>property("pool:min")
            .resource(DataSourceResource::minPoolSize)
            .plan(plan -> (plan.getPool() == null) ? null : plan.getPool().getMin())
            // TODO inline the supplier and addTo
            .addTo((Supplier<DataSourceResource> t, Integer u) -> {
                t.get().minPoolSize(u);
                return t;
            })
            .write(DataSourceResource::updateMinPoolSize);
        this.<Integer>property("pool:initial")
            .resource(DataSourceResource::initialPoolSize)
            .plan(plan -> (plan.getPool() == null) ? null : plan.getPool().getInitial())
            .addTo((Supplier<DataSourceResource> t, Integer u) -> {
                t.get().initialPoolSize(u);
                return t;
            })
            .write(DataSourceResource::updateInitialPoolSize);
        this.<Integer>property("pool:max")
            .resource(DataSourceResource::maxPoolSize)
            .plan(plan -> (plan.getPool() == null) ? null : plan.getPool().getMax())
            .addTo((Supplier<DataSourceResource> t, Integer u) -> {
                t.get().maxPoolSize(u);
                return t;
            })
            .write(DataSourceResource::updateMaxPoolSize);
        this.<Age>property("pool:max-age")
            .resource(DataSourceResource::maxPoolAge)
            .plan(plan -> (plan.getPool() == null) ? null : plan.getPool().getMaxAge())
            .addTo((Supplier<DataSourceResource> t, Age u) -> {
                t.get().maxPoolAge(u);
                return t;
            })
            .write(DataSourceResource::updateMaxAge);
    }

    private <T> Property<T> property(String name, Class<T> type) {
        return property(name, type, DataSourceResource.class, DataSourcePlan.class);
    }

    @Override protected String getType() { return "data-sources"; }

    @Override protected Stream<DataSourceResource> existingResources() { return container.allDataSources(); }

    @Override protected Stream<DataSourcePlan> resourcesIn(Plan plan) { return plan.dataSources(); }

    @Override protected DataSourceAudit audit(DataSourceResource resource) {
        return new DataSourceAudit().name(resource.name());
    }

    @Override protected Supplier<DataSourceResource> resourceBuilder(DataSourcePlan plan) {
        DataSourceResource dataSourceResource = container.builderFor(plan.getName());
        return () -> dataSourceResource;
    }

    @Override public void read(Plan plan, DataSourceResource resource) {
        plan.addDataSource(new DataSourcePlan(resource.name())
            .setXa(resource.xa())
            .setState(deployed)
            .setUri(resource.uri())
            .setJndiName(resource.jndiName())
            .setDriver(resource.driver())
            .setUserName(resource.userName())
            // hide: .password(resource.password())
            .setPool(new PoolPlan()
                .setMin(resource.minPoolSize())
                .setInitial(resource.initialPoolSize())
                .setMax(resource.maxPoolSize())
                .setMaxAge(resource.maxPoolAge())));
    }
}
