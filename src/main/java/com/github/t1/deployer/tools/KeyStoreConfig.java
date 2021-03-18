package com.github.t1.deployer.tools;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

import static lombok.AccessLevel.PRIVATE;

@Data @Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
@With
public class KeyStoreConfig {
    private String path;
    private String type;
    private String pass;
    private String alias;
}
