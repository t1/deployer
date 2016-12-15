package com.github.t1.deployer.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import static lombok.AccessLevel.*;

@Value
@Builder(toBuilder = true)
@NoArgsConstructor(access = PRIVATE, force = true)
@AllArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
public class RootBundleConfig {
    GroupId groupId;
    ArtifactId artifactId;
    Version version;
    Classifier classifier;
    Boolean shutdownAfterBoot;
}
