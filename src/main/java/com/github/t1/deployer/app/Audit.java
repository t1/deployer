package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.*;
import com.github.t1.deployer.container.DeploymentName;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Artifact;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Map;

import static com.github.t1.deployer.app.Audit.Type.*;
import static com.github.t1.deployer.app.Audit.Type.logger;
import static com.github.t1.deployer.model.DeploymentState.*;
import static lombok.AccessLevel.*;

@Data
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor(access = PROTECTED)
public abstract class Audit {
    public enum Type {artifact, logger}

    @NonNull @JsonProperty private final Type type;
    @NonNull @JsonProperty private final DeploymentState state;

    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class ArtifactAudit extends Audit {
        @NonNull @JsonProperty private final DeploymentName name;
        @NonNull @JsonProperty private final GroupId groupId;
        @NonNull @JsonProperty private final ArtifactId artifactId;
        @NonNull @JsonProperty private final Version version;

        public static ArtifactAuditBuilder of(String groupId, String artifactId, String version) {
            return ArtifactAudit.of(new GroupId(groupId), new ArtifactId(artifactId), new Version(version));
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

        public static class ArtifactAuditBuilder extends BuilderWithDeploymentState<ArtifactAudit> {
            private ArtifactAuditBuilder() {}

            @Override protected ArtifactAudit build() {
                return new ArtifactAudit(state, name, groupId, artifactId, version);
            }
        }

        public ArtifactAudit(DeploymentState state, DeploymentName name, GroupId groupId, ArtifactId artifactId,
                Version version) {
            super(artifact, state);
            this.name = name;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }

    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class LoggerAudit extends Audit {
        @NonNull @JsonProperty private final String category;
        @NonNull @JsonProperty private final LogLevel level;

        public static LoggerAuditBuilder of(@NonNull String category) {
            return LoggerAudit.builder().category(category);
        }

        public static class LoggerAuditBuilder extends BuilderWithDeploymentState<LoggerAudit> {
            private LoggerAuditBuilder() {}

            @Override protected LoggerAudit build() { return new LoggerAudit(state, category, level); }
        }

        private LoggerAudit(DeploymentState state, String category, LogLevel level) {
            super(logger, state);
            this.category = category;
            this.level = level;
        }
    }

    @JsonCreator
    public static Audit factory(Map<String, String> map) {
        String type = map.get("type");
        switch (type) {
        case "artifact": {
            ArtifactAudit.ArtifactAuditBuilder builder = ArtifactAudit.of(map.get("groupId"), map.get("artifactId"),
                    map.get("version")).name(new DeploymentName(map.get("name")));
            return build(map, builder);
        }
        case "logger": {
            LoggerAudit.LoggerAuditBuilder builder = LoggerAudit.of(map.get("category"))
                                                                .level(LogLevel.valueOf(map.get("level")));
            return map.get("state").equals("deployed") ? builder.deployed() : builder.undeployed();
        }
        default:
            throw new IllegalArgumentException("unsupported audit type: '" + type + "'");
        }
    }

    private static Audit build(Map<String, String> map, BuilderWithDeploymentState<? extends Audit> builder) {
        String state = map.get("state");
        switch (state) {
        case "deployed":
            return builder.deployed();
        case "undeployed":
            return builder.undeployed();
        default:
            throw new IllegalArgumentException("unsupported audit state: '" + state + "'");
        }
    }

    private static abstract class BuilderWithDeploymentState<T> {
        @Setter
        protected DeploymentState state;

        public T deployed() { return state(deployed).build(); }

        public T undeployed() { return state(undeployed).build(); }

        protected abstract T build();
    }
}
