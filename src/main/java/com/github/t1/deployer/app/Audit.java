package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.t1.deployer.container.DeploymentName;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Artifact;
import lombok.*;
import lombok.experimental.Accessors;

import static com.github.t1.deployer.app.Audit.Type.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static lombok.AccessLevel.*;

@Data
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor(access = PROTECTED)
public abstract class Audit {
    public enum Type {artifact}

    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class ArtifactAudit extends Audit {
        public static ArtifactAuditBuilder of(String groupId, String artifactId, String version) {
            return of(new GroupId(groupId), new ArtifactId(artifactId), new Version(version));
        }

        public static ArtifactAuditBuilder of(GroupId groupId, ArtifactId artifactId, Version version) {
            return ArtifactAudit.builder().groupId(groupId).artifactId(artifactId).version(version);
        }

        public static ArtifactAuditBuilder of(Artifact artifact) {
            return ArtifactAudit
                    .builder()
                    .groupId(artifact.getGroupId())
                    .artifactId(artifact.getArtifactId())
                    .version(artifact.getVersion());
        }

        public static class ArtifactAuditBuilder {
            @Setter
            private DeploymentState state;

            public ArtifactAuditBuilder name(String name) {
                return this.name(new DeploymentName(name));
            }

            public ArtifactAuditBuilder name(DeploymentName name) {
                this.name = name;
                return this;
            }

            public ArtifactAudit deployed() { return state(deployed).build(); }

            public ArtifactAudit undeployed() { return state(undeployed).build(); }

            private ArtifactAudit build() { return new ArtifactAudit(state, name, groupId, artifactId, version); }
        }

        @NonNull @JsonProperty DeploymentName name;
        @NonNull @JsonProperty GroupId groupId;
        @NonNull @JsonProperty ArtifactId artifactId;
        @NonNull @JsonProperty Version version;

        public ArtifactAudit(DeploymentState state, DeploymentName name, GroupId groupId, ArtifactId artifactId,
                Version version) {
            super(artifact, state);
            this.name = name;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }

    @NonNull @JsonProperty private final Type type;
    @NonNull @JsonProperty private final DeploymentState state;
}
