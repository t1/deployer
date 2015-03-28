package com.github.t1.deployer.model;

import lombok.*;

@Data
public class LoggerConfig {
    @NonNull
    String category;
    @NonNull
    String level;
}
