package com.github.t1.deployer.container;

import com.github.t1.deployer.container.DataSourceResource.DataSourceResourceBuilder;
import com.github.t1.deployer.container.DeploymentResource.DeploymentResourceBuilder;
import com.github.t1.deployer.container.LogHandlerResource.LogHandlerResourceBuilder;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.deployer.model.*;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.management.ObjectName;
import java.nio.file.*;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
// @Logged
public class Container {
    public static final String CLI_DEBUG = Batch.class.getName() + "#DEBUG";

    public static void waitForMBean() {
        for (int i = 0; i < 10; i++) {
            ObjectName objectName = ModelControllerClientProducer.findManagementInterface();
            if (objectName != null)
                return;
            log.debug("waiting for MBean server");
            sleep(Duration.ofSeconds(1));
        }
    }

    @SneakyThrows(InterruptedException.class)
    private static void sleep(Duration duration) { Thread.sleep(duration.toMillis()); }

    @Inject Batch batch;

    public void waitForBoot() { batch.waitForBoot(); }

    public void shutdown() { batch.shutdown(); }

    public void suspend() { batch.suspend(); }

    public void reload() { batch.reload(); }

    public static Path getConfigDir() {
        return Stream.of(
                System.getenv("DEPLOYER_CONFIG_DIR"),
                System.getProperty("deployer.config.dir"),
                System.getProperty("jboss.server.config.dir")
        ).filter(Objects::nonNull)
                .peek(s -> log.debug("found config dir {}", s))
                .findFirst()
                .map(Paths::get)
                .orElseThrow(() -> new RuntimeException("no config dir configured"));
    }

    public LogHandlerResourceBuilder builderFor(LogHandlerType type, LogHandlerName name) {
        return LogHandlerResource.builder(type, name, batch);
    }

    public Stream<LogHandlerResource> allLogHandlers() { return LogHandlerResource.allHandlers(batch).stream(); }

    public LoggerResourceBuilder builderFor(LoggerCategory category) { return LoggerResource.builder(category, batch); }

    public Stream<LoggerResource> allLoggers() { return LoggerResource.allLoggers(batch).stream(); }

    public DataSourceResourceBuilder builderFor(DataSourceName name) { return DataSourceResource.builder(name, batch); }

    public Stream<DataSourceResource> allDataSources() { return DataSourceResource.allDataSources(batch).stream(); }

    public DeploymentResourceBuilder builderFor(DeploymentName name) { return DeploymentResource.builder(name, batch); }

    public Stream<DeploymentResource> allDeployments() { return DeploymentResource.allDeployments(batch); }

    public void startBatch() { batch.startBatch(); }

    public ProcessState commitBatch() { return batch.commitBatch(); }

    public void rollbackBatch() { batch.rollbackBatch(); }
}
