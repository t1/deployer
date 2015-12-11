package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import com.github.t1.deployer.model.LoggerConfig.LoggerConfigBuilder;
import com.github.t1.log.LogLevel;

import lombok.*;

@Data
@Builder(builderMethodName = "loggerPatch")
@NoArgsConstructor(access = PRIVATE, force = true)
@AllArgsConstructor
public class LoggerPatch {
    private LogLevel logLevel;

    public LoggerConfig apply(LoggerConfig logger) {
        LoggerConfigBuilder builder = logger.toBuilder();
        if (logLevel != null)
            builder.level(logLevel);
        return builder.build();
    }
}
