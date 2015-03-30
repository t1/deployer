package com.github.t1.deployer.model;

import lombok.*;

@Data
public class LoggerConfig {
    public static final String NEW_LOGGER = "!";

    @NonNull
    String category;
    @NonNull
    String level;
}
