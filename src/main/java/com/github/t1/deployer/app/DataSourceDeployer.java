package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.DataSourceAudit;
import com.github.t1.deployer.app.Audit.DataSourceAudit.DataSourceAuditBuilder;
import com.github.t1.deployer.container.DataSourceResource;
import com.github.t1.deployer.container.DataSourceResource.DataSourceResourceBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.DataSourcePlan.PoolPlan;
import com.github.t1.deployer.model.Plan.PlanBuilder;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentState.*;

@Slf4j
public class DataSourceDeployer extends
        ResourceDeployer<DataSourcePlan, DataSourceResourceBuilder, DataSourceResource, DataSourceAuditBuilder> {
    public DataSourceDeployer() {
        property("uri", URI.class);
        property("jndi-name", String.class);
        property("driver", String.class);
        property("xa", Boolean.class);
        property("user-name", String.class).confidential(true);
        property("password", String.class).confidential(true);
        this.<Integer>property("pool:min")
                .resource(DataSourceResource::minPoolSize)
                .plan(plan -> (plan.getPool() == null) ? null : plan.getPool().getMin())
                .addTo(DataSourceResourceBuilder::minPoolSize)
                .write(DataSourceResource::updateMinPoolSize);
        this.<Integer>property("pool:initial")
                .resource(DataSourceResource::initialPoolSize)
                .plan(plan -> (plan.getPool() == null) ? null : plan.getPool().getInitial())
                .addTo(DataSourceResourceBuilder::initialPoolSize)
                .write(DataSourceResource::updateInitialPoolSize);
        this.<Integer>property("pool:max")
                .resource(DataSourceResource::maxPoolSize)
                .plan(plan -> (plan.getPool() == null) ? null : plan.getPool().getMax())
                .addTo(DataSourceResourceBuilder::maxPoolSize)
                .write(DataSourceResource::updateMaxPoolSize);
        this.<Age>property("pool:max-age")
                .resource(DataSourceResource::maxPoolAge)
                .plan(plan -> (plan.getPool() == null) ? null : plan.getPool().getMaxAge())
                .addTo(DataSourceResourceBuilder::maxPoolAge)
                .write(DataSourceResource::updateMaxAge);
    }

    private <T> Property<T> property(String name, Class<T> type) {
        return property(name, type, DataSourceResource.class, DataSourcePlan.class);
    }

    @Override protected String getType() { return "data-sources"; }

    @Override protected Stream<DataSourceResource> existingResources() { return container.allDataSources(); }

    @Override protected Stream<DataSourcePlan> resourcesIn(Plan plan) { return plan.dataSources(); }

    @Override protected DataSourceAuditBuilder auditBuilder(DataSourceResource resource) {
        return DataSourceAudit.builder().name(resource.name());
    }

    @Override protected DataSourceResourceBuilder resourceBuilder(DataSourcePlan plan) {
        return container.builderFor(plan.getName());
    }

    @Override public void read(PlanBuilder builder, DataSourceResource resource) {
        DataSourcePlan.DataSourcePlanBuilder dataSourcePlan = DataSourcePlan
                .builder()
                .name(resource.name())
                .xa(resource.xa())
                .state(deployed)
                .uri(resource.uri())
                .jndiName(resource.jndiName())
                .driver(resource.driver())
                .userName(resource.userName())
                // hide: .password(resource.password())
                .pool(PoolPlan
                        .builder()
                        .min(resource.minPoolSize())
                        .initial(resource.initialPoolSize())
                        .max(resource.maxPoolSize())
                        .maxAge(resource.maxPoolAge())
                        .build());
        builder.dataSource(dataSourcePlan.build());
    }
}
