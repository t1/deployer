package com.github.t1.deployer.app;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.inject.Inject;

@Startup
@Singleton
public class StartupDeploy {
    @Inject DeployerBoundary deployer;

    @PostConstruct public void startup() { deployer.applyAsync(); }
}
