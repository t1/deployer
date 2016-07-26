package com.github.t1.deployer.app;

import com.github.t1.deployer.tools.FileWatcher;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.*;
import javax.ejb.*;
import javax.inject.Inject;
import java.nio.file.Path;

@Slf4j
@Singleton
@Startup
public class RootFileWatcher {
    @Inject DeployerBoundary deployer;

    private FileWatcher fileWatcher;

    @PostConstruct
    void start() {
        log.info("startup");
        deployer.applyAsync();

        Path rootBundle = DeployerBoundary.getConfigPath();
        log.info("start file watcher on {}", rootBundle);
        fileWatcher = new FileWatcher(rootBundle, () -> deployer.applyAsync());
        fileWatcher.start();
    }

    @PreDestroy
    void stop() {
        log.info("stop file watcher");
        fileWatcher.shutdown();
    }
}
