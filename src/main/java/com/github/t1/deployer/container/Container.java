package com.github.t1.deployer.container;

import com.github.t1.deployer.model.DataSourceName;
import com.github.t1.deployer.model.DeploymentName;
import com.github.t1.deployer.model.LogHandlerName;
import com.github.t1.deployer.model.LogHandlerType;
import com.github.t1.deployer.model.LoggerCategory;
import com.github.t1.deployer.model.ProcessState;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.management.ObjectName;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
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

    public LogHandlerResource builderFor(LogHandlerType type, LogHandlerName name) { return new LogHandlerResource(type, name, batch); }

    public Stream<LogHandlerResource> allLogHandlers() { return LogHandlerResource.allHandlers(batch).stream(); }

    public LoggerResource builderFor(LoggerCategory category) { return new LoggerResource(category, batch); }

    public Stream<LoggerResource> allLoggers() { return LoggerResource.allLoggers(batch).stream(); }

    public DataSourceResource builderFor(DataSourceName name) { return new DataSourceResource(name, batch); }

    public Stream<DataSourceResource> allDataSources() { return DataSourceResource.allDataSources(batch).stream(); }

    public DeploymentResource builderFor(DeploymentName name) { return new DeploymentResource(name, batch); }

    public Stream<DeploymentResource> allDeployments() { return DeploymentResource.allDeployments(batch); }

    public void startBatch() { batch.startBatch(); }

    public ProcessState commitBatch() { return batch.commitBatch(); }

    public void rollbackBatch() { batch.rollbackBatch(); }
}
