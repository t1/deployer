package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import static com.github.t1.deployer.model.DeploymentState.deployed;
import static com.github.t1.deployer.model.DeploymentState.undeployed;
import static com.github.t1.deployer.model.Plan.apply;

@Data @Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(KebabCaseStrategy.class)
public abstract class AbstractArtifactPlan implements Plan.AbstractPlan {
    private DeploymentState state;
    private GroupId groupId;
    private ArtifactId artifactId;
    private Version version;
    private Classifier classifier;
    private Checksum checksum;

    public static void fromJson(JsonNode node, AbstractArtifactPlan builder, String defaultArtifactId, String defaultVersion) {
        apply(node, "state", builder::setState, DeploymentState::valueOf);
        apply(node, "group-id", builder::setGroupId, GroupId::of, "default.group-id");
        apply(node, "artifact-id", builder::setArtifactId, ArtifactId::new, "«" + defaultArtifactId + "»");
        apply(node, "version", builder::setVersion, Version::new, defaultVersion);
        apply(node, "classifier", builder::setClassifier, Classifier::new);
        apply(node, "checksum", builder::setChecksum, Checksum::fromString);
        verify(builder);
    }

    private static void verify(AbstractArtifactPlan builder) {
        if (builder.groupId == null && builder.state != undeployed)
            throw new Plan.PlanLoadingException("the `group-id` can only be null when undeploying");
    }

    @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

    @Override public String toString() {
        return getState() + ":" + groupId + ":" + artifactId + ":" + version
            + ((classifier == null) ? "" : ":" + classifier);
    }
}
