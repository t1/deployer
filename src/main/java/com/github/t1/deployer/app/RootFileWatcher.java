package com.github.t1.deployer.app;

import com.github.t1.deployer.tools.FileWatcher;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.*;
import javax.ejb.*;
import javax.inject.Inject;
import java.nio.file.Path;

import static com.github.t1.deployer.app.Trigger.*;
import static com.github.t1.log.LogLevel.*;
import static java.util.Collections.*;

@Slf4j
@Singleton
@Startup
@Logged(level = INFO)
class RootFileWatcher {
    @Inject DeployerBoundary deployer;

    private FileWatcher fileWatcher;

    @PostConstruct
    void start() {
        deployer.applyAsync(startup);

        Path rootBundle = deployer.getRootBundlePath();
        log.info("start file watcher on {}", rootBundle);
        fileWatcher = new FileWatcher(rootBundle, () -> deployer.apply(fileChange, emptyMap()));
        fileWatcher.start();
    }

    @PreDestroy
    void stop() {
        log.info("stop file watcher");
        fileWatcher.shutdown();
    }
}
