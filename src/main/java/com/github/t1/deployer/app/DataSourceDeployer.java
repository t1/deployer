package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.DataSourceAudit;
import com.github.t1.deployer.app.Audit.DataSourceAudit.DataSourceAuditBuilder;
import com.github.t1.deployer.container.DataSourceResource;
import com.github.t1.deployer.container.DataSourceResource.DataSourceResourceBuilder;
import com.github.t1.deployer.model.*;
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
        property("user-name", String.class);
        property("password", String.class);
    }

    private void property(String name, Class<?> type) {
        property(name, type, DataSourceResource.class, DataSourcePlan.class);
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
                .state(deployed)
                .uri(resource.uri())
                .jndiName(resource.jndiName())
                .driver(resource.driver())
                .userName(resource.userName())
                .password(resource.password());
        builder.dataSource(dataSourcePlan.build());
    }
}
