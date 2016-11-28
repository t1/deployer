package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Plan;
import com.github.t1.deployer.model.Plan.PlanBuilder;

/** No-generics interface of {@link AbstractDeployer}, so injection works. */
interface Deployer {
    void read(PlanBuilder builder);

    void apply(Plan plan);
}
