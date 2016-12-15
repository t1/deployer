package com.github.t1.deployer.container;

import com.github.t1.deployer.container.DataSourceResource.DataSourceResourceBuilder;
import com.github.t1.deployer.container.DeploymentResource.DeploymentResourceBuilder;
import com.github.t1.deployer.container.LogHandlerResource.LogHandlerResourceBuilder;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.management.ObjectName;
import java.nio.file.*;
import java.util.stream.Stream;

@Slf4j
@Logged
public class Container {
    public static final String CLI_DEBUG = Batch.class.getName() + "#DEBUG";

    @SneakyThrows(InterruptedException.class)
    public static void waitForMBean() {
        for (int i = 0; i < 10; i++) {
            ObjectName objectName = ContainerProducer.findManagementInterface();
            if (objectName != null)
                return;
            log.debug("waiting for MBean server");
            //noinspection MagicNumber
            Thread.sleep(1000L);
        }
    }

    @Inject Batch batch;

    public void waitForBoot() { batch.waitForBoot(); }

    public void shutdown() { batch.shutdown(); }

    public static Path getConfigDir() { return Paths.get(System.getProperty("jboss.server.config.dir")); }

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
