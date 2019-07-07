package com.github.t1.deployer.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import static lombok.AccessLevel.PRIVATE;

@Data @Accessors(chain = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
public class RootBundleConfig {
    GroupId groupId;
    ArtifactId artifactId;
    Version version;
    Classifier classifier;
    Boolean shutdownAfterBoot;
}
