package com.github.t1.deployer.model;

import com.github.t1.deployer.model.LoggerConfig.LoggerConfigBuilder;
import com.github.t1.log.LogLevel;

import io.swagger.annotations.ApiModel;
import lombok.*;

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

    public LoggerConfig apply(LoggerConfig logger) {
        LoggerConfigBuilder builder = logger.copy();
        if (logLevel != null)
            builder.level(logLevel);
        return builder.build();
    }
}
