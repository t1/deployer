package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import com.fasterxml.jackson.annotation.*;
import com.github.t1.log.LogLevel;

import io.swagger.annotations.ApiModel;
import lombok.*;

@Value
@Builder(toBuilder = true)
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@ToString(of = { "category", "level" })
@ApiModel
public class LoggerConfig implements Comparable<LoggerConfig> {
    public static final String ROOT = "ROOT";
    public static final String NEW_LOGGER = "!";

    @NonNull
    @JsonProperty
    String category;

    @NonNull
    @JsonProperty
    LogLevel level;

    @Override
    public int compareTo(LoggerConfig that) {
        return this.category.compareToIgnoreCase(that.category);
    }

    @JsonIgnore
    public boolean isNew() {
        return NEW_LOGGER.equals(category);
    }

    public boolean isRoot() {
        return category.isEmpty();
    }

    public String getCategory() {
        return (category == null) ? null : (category.isEmpty()) ? ROOT : category;
    }
}
