package com.github.t1.deployer.model;

import lombok.*;
import lombok.experimental.Accessors;

import com.github.t1.log.LogLevel;

@Data
@Accessors(chain = true)
public class LoggerConfig implements Comparable<LoggerConfig> {
    public static final String NEW_LOGGER = "!";

    @NonNull
    String category;
    @NonNull
    LogLevel level;

    @Override
    public int compareTo(LoggerConfig that) {
        return this.category.compareToIgnoreCase(that.category);
    }
}
