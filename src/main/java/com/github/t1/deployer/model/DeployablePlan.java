package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import static com.github.t1.deployer.model.ArtifactType.bundle;
import static com.github.t1.deployer.model.Plan.apply;

@Data @Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
@JsonNaming(KebabCaseStrategy.class)
public class DeployablePlan extends AbstractArtifactPlan {
    @NonNull @JsonIgnore private DeploymentName name;
    private ArtifactType type;
    private String error;

    @Override public String getId() { return name.getValue(); }

    @Override public DeployablePlan setState(DeploymentState state) { super.setState(state); return this; }

    @Override public DeployablePlan setGroupId(GroupId groupId) { super.setGroupId(groupId); return this; }

    @Override public DeployablePlan setArtifactId(ArtifactId artifactId) { super.setArtifactId(artifactId); return this; }

    @Override public DeployablePlan setVersion(Version version) { super.setVersion(version); return this; }

    @Override public DeployablePlan setClassifier(Classifier classifier) { super.setClassifier(classifier); return this; }

    @Override public DeployablePlan setChecksum(Checksum checksum) { super.setChecksum(checksum); return this; }

    static DeployablePlan fromJson(DeploymentName name, JsonNode node) {
        DeployablePlan plan = new DeployablePlan(name);
        AbstractArtifactPlan.fromJson(node, plan, name + ".state or «deployed»", name.getValue(), name + ".version or «CURRENT»");
        apply(node, "type", plan::setType, ArtifactType::valueOf, "default.deployable-type or «war»");
        return plan.verify();
    }

    private DeployablePlan verify() {
        if (getType() == bundle)
            throw new Plan.PlanLoadingException(
                "a deployable may not be of type 'bundle'; use 'bundles' plan instead.");
        return this;
    }

    @Override public String toString() {
        return "deployment:" + name + ":" + super.toString() + ":" + type
            + ((getChecksum() == null) ? "" : ":" + getChecksum())
            + ((error == null) ? "" : ": ### " + error + " ###");
    }
}
