package com.github.t1.deployer.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.nio.file.Path;

import static lombok.AccessLevel.*;

@Value
@Builder
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
public class KeyStoreConfig {
    private final Path path;

    private final String password;

    private final String alias;
}
