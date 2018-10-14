package com.github.t1.deployer.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

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
