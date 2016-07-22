package com.github.t1.deployer.app;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.*;
import javax.ejb.*;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;

@Startup
@Singleton
@Slf4j
public class StartupDeploy {
    @Inject DeployerBoundary deployer;

    @Resource ManagedExecutorService executorService;

    @PostConstruct public void startup() {
        executorService.execute(deployer::post);
    }
}
