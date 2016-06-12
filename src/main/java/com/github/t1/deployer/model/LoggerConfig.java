package com.github.t1.deployer.model;

import com.github.t1.log.LogLevel;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import static lombok.AccessLevel.*;

@Value
@Builder(toBuilder = true)
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@ToString(of = { "category", "level" })
public class LoggerConfig implements Comparable<LoggerConfig> {
    public static final String ROOT = "ROOT";
    public static final String NEW_LOGGER = "!";

    @NonNull
    String category;

    @NonNull
    LogLevel level;

    @Override
    public int compareTo(@NotNull LoggerConfig that) {
        return this.category.compareToIgnoreCase(that.category);
    }

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
