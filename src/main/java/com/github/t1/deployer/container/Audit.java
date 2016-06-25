package com.github.t1.deployer.container;

import com.github.t1.deployer.model.*;
import lombok.*;
import lombok.experimental.Accessors;

import static com.github.t1.deployer.container.Audit.Type.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static lombok.AccessLevel.*;

@Getter
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

        public static class ArtifactAuditBuilder {
            @Setter
            private DeploymentState state;

            public ArtifactAudit deployed() { return state(deployed).build(); }

            public ArtifactAudit undeployed() { return state(undeployed).build(); }

            private ArtifactAudit build() { return new ArtifactAudit(state, groupId, artifactId, version); }
        }

        @NonNull GroupId groupId;
        @NonNull ArtifactId artifactId;
        @NonNull Version version;

        public ArtifactAudit(DeploymentState state, GroupId groupId, ArtifactId artifactId, Version version) {
            super(artifact, state);
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }

    @NonNull private final Type type;
    @NonNull private final DeploymentState state;
}
