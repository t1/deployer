package com.github.t1.deployer.container;

import com.github.t1.deployer.container.LogHandlerResource.LogHandlerResourceBuilder;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Stateless;
import java.util.List;

@Slf4j
@Logged
@Stateless
public class LoggerContainer extends CLI {
    public List<LoggerResource> allLoggers() { return LoggerResource.all(this); }

    public LoggerResourceBuilder logger(LoggerCategory category) { return LoggerResource.builder(category, this); }

    public LogHandlerResourceBuilder handler(LoggingHandlerType type, LogHandlerName name) {
        return LogHandlerResource.builder(type, name, this);
    }
}
