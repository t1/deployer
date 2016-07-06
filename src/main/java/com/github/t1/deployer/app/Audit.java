package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Artifact;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Map;

import static com.github.t1.deployer.app.Audit.ChangeType.*;
import static com.github.t1.deployer.app.Audit.Type.*;
import static com.github.t1.deployer.app.Audit.Type.logger;
import static lombok.AccessLevel.*;

@Data
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor(access = PROTECTED)
public abstract class Audit {
    public enum Type {artifact, logger}

    public enum ChangeType {added, updated, removed}

    @NonNull @JsonProperty private final Type type;
    @NonNull @JsonProperty private final ChangeType change;

    @Override public String toString() { return type + ":" + change; }

    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class ArtifactAudit extends Audit {
        @NonNull @JsonProperty private final DeploymentName name;
        @NonNull @JsonProperty private final GroupId groupId;
        @NonNull @JsonProperty private final ArtifactId artifactId;
        @NonNull @JsonProperty private final Version version;

        @Override public String toString() {
            return super.toString() + ":" + name + "->" + groupId + ":" + artifactId + ":" + version;
        }

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

        public static class ArtifactAuditBuilder extends ChangeBuilder<ArtifactAudit> {
            private ArtifactAuditBuilder() {}

            public ArtifactAuditBuilder name(String name) { return name(new DeploymentName(name)); }

            public ArtifactAuditBuilder name(DeploymentName name) {
                this.name = name;
                return this;
            }

            @Override protected ArtifactAudit build() {
                return new ArtifactAudit(change, name, groupId, artifactId, version);
            }
        }

        public ArtifactAudit(ChangeType change, DeploymentName name, GroupId groupId, ArtifactId artifactId,
                Version version) {
            super(artifact, change);
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
        @NonNull @JsonProperty private final LoggerCategory category;
        @JsonProperty private final LogLevel level;

        @Override public String toString() { return super.toString() + ":" + category + ":" + level; }

        public static LoggerAuditBuilder of(@NonNull LoggerCategory category) {
            return LoggerAudit.builder().category(category);
        }

        public static class LoggerAuditBuilder extends ChangeBuilder<LoggerAudit> {
            private LoggerAuditBuilder() {}

            @Override protected LoggerAudit build() { return new LoggerAudit(change, category, level); }
        }

        private LoggerAudit(ChangeType change, LoggerCategory category, LogLevel level) {
            super(logger, change);
            this.category = category;
            this.level = level;
        }
    }

    @JsonCreator
    public static Audit factory(Map<String, String> map) {
        String type = map.get("type");
        switch (type) {
        case "artifact": {
            ArtifactAudit.ArtifactAuditBuilder builder = ArtifactAudit
                    .of(map.get("groupId"), map.get("artifactId"), map.get("version"))
                    .name(map.get("name"));
            return build(map, builder);
        }
        case "logger": {
            LoggerAudit.LoggerAuditBuilder builder = LoggerAudit
                    .of(LoggerCategory.of(map.get("category")))
                    .level(map.containsKey("level") ? LogLevel.valueOf(map.get("level")) : null);
            return build(map, builder);
        }
        default:
            throw new IllegalArgumentException("unsupported audit type: '" + type + "'");
        }
    }

    private static Audit build(Map<String, String> map, ChangeBuilder<? extends Audit> builder) {
        String change = map.get("change");
        try {
            return builder.change(ChangeType.valueOf(change)).build();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unsupported audit change: '" + change + "'");
        }
    }

    private static abstract class ChangeBuilder<T> {
        @Setter
        protected ChangeType change;

        public T added() { return change(added).build(); }

        public T updated() { return change(updated).build(); }

        public T removed() { return change(removed).build(); }

        protected abstract T build();
    }
}
