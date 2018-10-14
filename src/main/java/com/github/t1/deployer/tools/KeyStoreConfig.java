package com.github.t1.deployer.tools;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;

import static lombok.AccessLevel.PRIVATE;

@Value
@Builder
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
@Wither
public class KeyStoreConfig {
    private final String path;
    private final String type;
    private final String pass;
    private final String alias;
}
