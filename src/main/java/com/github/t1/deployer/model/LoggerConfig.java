package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.*;
import com.github.t1.log.LogLevel;

import io.swagger.annotations.ApiModel;
import lombok.*;

@Value
@Builder
@AllArgsConstructor
@ApiModel
public class LoggerConfig implements Comparable<LoggerConfig> {
    public static final String NEW_LOGGER = "!";

    @NonNull
    @JsonProperty
    String category;

    @NonNull
    @JsonProperty
    LogLevel level;

    @SuppressWarnings("unused")
    private LoggerConfig() {
        this.category = null;
        this.level = null;
    }

    public LoggerConfigBuilder copy() {
        return builder().category(category).level(level);
    }

    @Override
    public int compareTo(LoggerConfig that) {
        return this.category.compareToIgnoreCase(that.category);
    }

    @JsonIgnore
    public boolean isNew() {
        return NEW_LOGGER.equals(category);
    }
}
