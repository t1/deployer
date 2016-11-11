package com.github.t1.deployer.container;

import com.github.t1.deployer.container.DataSourceResource.DataSourceResourceBuilder;
import com.github.t1.deployer.container.DeploymentResource.DeploymentResourceBuilder;
import com.github.t1.deployer.container.LogHandlerResource.LogHandlerResourceBuilder;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.nio.file.*;
import java.util.stream.Stream;

@Slf4j
@Logged
@Stateless
public class Container {
    @Inject public CLI cli;

    public void waitForBoot() { cli.waitForBoot(); }

    public Path getConfigDir() { return Paths.get(System.getProperty("jboss.server.config.dir")); }

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
}
