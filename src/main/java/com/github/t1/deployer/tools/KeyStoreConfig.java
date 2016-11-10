package com.github.t1.deployer.tools;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.Wither;

import java.nio.file.Path;

import static lombok.AccessLevel.*;

@Value
@Builder
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
@Wither
public class KeyStoreConfig {
    private final Path path;
    private final String type;
    private final String pass;
    private final String alias;
}
