package com.github.t1.deployer.model;

import io.swagger.annotations.ApiModel;
import lombok.*;

import com.github.t1.deployer.model.LoggerConfig.LoggerConfigBuilder;
import com.github.t1.log.LogLevel;

@Data
@AllArgsConstructor
@Builder(builderMethodName = "loggerPatch")
@ApiModel("patch for changing a logger")
public class LoggerPatch {
    private LogLevel logLevel;

    @SuppressWarnings("unused")
    private LoggerPatch() {
        this.logLevel = null;
    }

    public LoggerConfig on(LoggerConfig logger) {
        LoggerConfigBuilder builder = logger.copy();
        if (logLevel != null)
            builder.level(logLevel);
        return builder.build();
    }
}
