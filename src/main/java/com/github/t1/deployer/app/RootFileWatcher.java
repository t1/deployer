package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Config;
import com.github.t1.deployer.tools.FileWatcher;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Set;

import static com.github.t1.deployer.app.Trigger.fileChange;
import static com.github.t1.deployer.app.Trigger.startup;
import static com.github.t1.log.LogLevel.INFO;
import static java.util.Collections.emptyMap;

@Slf4j
@Singleton
@Startup
@Logged(level = INFO)
public class RootFileWatcher {
    @Inject DeployerBoundary deployer;
    @Inject @Config("triggers") Set<Trigger> triggers;

    private FileWatcher fileWatcher;

    @PostConstruct
    void start() {
        if (triggers.contains(startup))
            deployer.applyAsync(startup);
        if (triggers.contains(fileChange))
            startFileWatcher();
    }

    private void startFileWatcher() {
        Path rootBundle = deployer.getRootBundlePath();
        log.info("start file watcher on {}", rootBundle);
        fileWatcher = new FileWatcher(rootBundle, () -> deployer.apply(fileChange, emptyMap()));
        fileWatcher.start();
    }

    @PreDestroy
    void stop() {
        if (fileWatcher != null) {
            log.info("stop file watcher");
            fileWatcher.shutdown();
        }
    }
}
