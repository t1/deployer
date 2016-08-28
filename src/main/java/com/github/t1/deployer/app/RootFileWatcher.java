package com.github.t1.deployer.app;

import com.github.t1.deployer.tools.FileWatcher;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.*;
import javax.ejb.*;
import javax.inject.Inject;
import java.nio.file.Path;

import static com.github.t1.log.LogLevel.*;

@Slf4j
@Singleton
@Startup
@Logged(level = INFO)
public class RootFileWatcher {
    @Inject DeployerBoundary deployer;

    private FileWatcher fileWatcher;

    @PostConstruct
    void start() {
        deployer.applyAsync();

        Path rootBundle = deployer.getRootBundlePath();
        log.info("start file watcher on {}", rootBundle);
        fileWatcher = new FileWatcher(rootBundle, deployer::apply);
        fileWatcher.start();
    }

    @PreDestroy
    void stop() {
        log.info("stop file watcher");
        fileWatcher.shutdown();
    }
}
