package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Plan;

/** No-generics interface of {@link AbstractDeployer}, so injection works. */
interface Deployer {
    void read(Plan plan);

    void apply(Plan plan);
}
