package com.github.t1.deployer.container;

import com.github.t1.deployer.container.DeploymentResource.DeploymentResourceBuilder;
import com.github.t1.deployer.container.LogHandlerResource.LogHandlerResourceBuilder;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;

@Slf4j
@Logged
@Stateless
public class Container {
    @Inject public CLI cli;

    public LogHandlerResourceBuilder logHandler(LogHandlerType type, LogHandlerName name) {
        return LogHandlerResource.builder(type, name, cli);
    }

    public List<LogHandlerResource> allLogHandlers() { return LogHandlerResource.allHandlers(cli); }

    public LoggerResourceBuilder logger(LoggerCategory category) { return LoggerResource.builder(category, cli); }

    public List<LoggerResource> allLoggers() { return LoggerResource.allLoggers(cli); }

    public DeploymentResourceBuilder deployment(DeploymentName name) { return DeploymentResource.builder(name, cli); }

    public List<DeploymentResource> allDeployments() { return DeploymentResource.allDeployments(cli); }
}
