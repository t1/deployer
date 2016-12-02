package com.github.t1.deployer.container;

import com.github.t1.deployer.container.DataSourceResource.DataSourceResourceBuilder;
import com.github.t1.deployer.container.DeploymentResource.DeploymentResourceBuilder;
import com.github.t1.deployer.container.LogHandlerResource.LogHandlerResourceBuilder;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.log.Logged;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.management.ObjectName;
import java.nio.file.*;
import java.util.stream.Stream;

@Slf4j
@Logged
@Stateless
@SuppressWarnings("deprecation")
public class Container {
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

    /** @deprecated Only to be used privately or in tests */
    @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated @Getter @Inject public CLI cli;

    public void waitForBoot() { cli.waitForBoot(); }

    public static Path getConfigDir() { return Paths.get(System.getProperty("jboss.server.config.dir")); }

    public LogHandlerResourceBuilder builderFor(LogHandlerType type, LogHandlerName name) {
        return LogHandlerResource.builder(type, name, cli);
    }

    public Stream<LogHandlerResource> allLogHandlers() { return LogHandlerResource.allHandlers(cli).stream(); }

    public LoggerResourceBuilder builderFor(LoggerCategory category) { return LoggerResource.builder(category, cli); }

    public Stream<LoggerResource> allLoggers() { return LoggerResource.allLoggers(cli).stream(); }

    public DataSourceResourceBuilder builderFor(DataSourceName name) { return DataSourceResource.builder(name, cli); }

    public Stream<DataSourceResource> allDataSources() { return DataSourceResource.allDataSources(cli).stream(); }

    public DeploymentResourceBuilder builderFor(DeploymentName name) { return DeploymentResource.builder(name, cli); }

    public Stream<DeploymentResource> allDeployments() { return DeploymentResource.allDeployments(cli); }

    public void startBatch() { cli.startBatch(); }

    public void commitBatch() { cli.commitBatch(); }

    public void rollbackBatch() { cli.rollbackBatch(); }
}
