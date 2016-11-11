package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import static com.github.t1.deployer.model.ArtifactType.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class DeployablePlan extends AbstractArtifactPlan {
    @NonNull @JsonIgnore private final DeploymentName name;
    @NonNull private final ArtifactType type;
    private final String error;

    @Override public String getId() { return name.getValue(); }

    public static class DeployablePlanBuilder extends AbstractArtifactPlanBuilder<DeployablePlanBuilder> {
        @Override public DeployablePlan build() {
            AbstractArtifactPlan a = super.build();
            return new DeployablePlan(name, type,
                    a.state, a.groupId, a.artifactId, a.version, a.classifier, a.checksum, error);
        }
    }

    private DeployablePlan(DeploymentName name, ArtifactType type, DeploymentState state,
            GroupId groupId, ArtifactId artifactId, Version version, Classifier classifier, Checksum checksum,
            String error) {
        super(state, groupId, artifactId, version, classifier, checksum);
        this.name = name;
        this.type = type;
        this.error = error;
    }

    public static DeployablePlan fromJson(DeploymentName name, JsonNode node) {
        if (node.isNull())
            throw new Plan.PlanLoadingException("incomplete deployables plan '" + name + "'");
        DeployablePlanBuilder builder = builder().name(name);
        AbstractArtifactPlan.fromJson(node, builder, name.getValue(), "«CURRENT»");
        Plan.apply(node, "type", builder::type, ArtifactType::valueOf, "default.deployable-type or «war»");
        return builder.build().verify();
    }

    private DeployablePlan verify() {
        if (getType() == bundle)
            throw new Plan.PlanLoadingException("a deployable may not be of type 'bundle'; use 'bundles' plan instead.");
        return this;
    }

    @Override public String toString() {
        return "deployment:" + name + ":" + super.toString()
                + ":" + type
                + ((getChecksum() == null) ? "" : ":" + getChecksum())
                + ((error == null) ? "" : ": ### " + error + " ###");
    }
}
