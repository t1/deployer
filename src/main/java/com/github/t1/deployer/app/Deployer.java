package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Plan.PlanBuilder;

/** No-generics interface of {@link AbstractDeployer}, so injection works. */
public interface Deployer {
    void read(PlanBuilder builder);

    void apply(Plan plan);
}
