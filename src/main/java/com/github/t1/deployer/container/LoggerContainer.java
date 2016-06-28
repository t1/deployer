package com.github.t1.deployer.container;

import com.github.t1.deployer.model.LoggingHandlerType;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Stateless;
import java.util.List;

import static com.github.t1.log.LogLevel.*;

@Slf4j
@Logged(level = INFO)
@Stateless
public class LoggerContainer extends CLI {
    public List<LoggerResource> allLoggers() { return LoggerResource.all(this); }

    public LoggerResource logger(String category) { return new LoggerResource(category, this); }

    public LogHandler handler(LoggingHandlerType type, String name) { return new LogHandler(name, type, this); }
}
