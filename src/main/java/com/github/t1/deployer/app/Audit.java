package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Artifact;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.*;

import static com.github.t1.deployer.app.Audit.Operation.*;
import static com.github.t1.deployer.app.Audit.Type.*;
import static lombok.AccessLevel.*;

@Data
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor(access = PROTECTED)
public abstract class Audit {
    public enum Type {artifact, logger}

    public enum Operation {add, change, remove}

    @Value
    public static class Change<T> {
        @NonNull @JsonProperty private final String name;
        @JsonProperty private final T oldValue;
        @JsonProperty private final T newValue;

        @Override public String toString() { return name + ":" + oldValue + "->" + newValue; }
    }

    @NonNull @JsonProperty private final Type type;
    @NonNull @JsonProperty private final Operation operation;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @NonNull @JsonProperty private final List<Change<?>> updates;

    @Override public String toString() { return type + ":" + operation; }

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
                return new ArtifactAudit(change, updates, name, groupId, artifactId, version);
            }
        }

        public ArtifactAudit(Operation change, List<Change<?>> updates, DeploymentName name, GroupId groupId,
                ArtifactId artifactId,
                Version version) {
            super(artifact, change, updates);
            this.name = name;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }

    @lombok.Value
    @lombok.Builder
    @lombok.EqualsAndHashCode(callSuper = true)
    public static class LoggerAudit extends Audit {
        @NonNull @JsonProperty private final LoggerCategory category;

        @Override public String toString() { return super.toString() + ":" + category + ":" + super.updates; }

        public static LoggerAuditBuilder of(@NonNull LoggerCategory category) {
            return LoggerAudit.builder().category(category);
        }

        public static class LoggerAuditBuilder extends ChangeBuilder<LoggerAudit> {
            private LoggerAuditBuilder() {}

            @Override protected LoggerAudit build() { return new LoggerAudit(change, updates, category); }

            public LoggerAuditBuilder update(LogLevel oldLevel, LogLevel newLevel) {
                update(new Change<>("level", oldLevel, newLevel));
                return this;
            }
        }

        private LoggerAudit(Operation change, List<Change<?>> updates, LoggerCategory category) {
            super(logger, change, updates);
            this.category = category;
        }
    }

    @JsonCreator
    public static Audit factory(JsonNode node) {
        Operation operation = Operation.valueOf(node.get("operation").asText());
        String type = node.get("type").asText();
        switch (type) {
        case "artifact": {
            ArtifactAudit.ArtifactAuditBuilder builder = ArtifactAudit
                    .of(node.get("groupId").asText(), node.get("artifactId").asText(), node.get("version").asText())
                    .name(node.get("name").asText());
            return build(operation, builder);
        }
        case "logger": {
            LoggerAuditBuilder builder = LoggerAudit.of(LoggerCategory.of(node.get("category").asText()));
            if (node.has("updates")) {
                for (JsonNode item : node.get("updates")) {
                    String fieldName = item.get("name").asText();
                    if ("level".equals(fieldName))
                        builder.update(toLevel(item.get("oldValue")), toLevel(item.get("newValue")));
                    else
                        throw new UnsupportedOperationException("unknown update type: [" + fieldName + "]");
                }
            }
            return build(operation, builder);
        }
        default:
            throw new IllegalArgumentException("unsupported audit type: '" + type + "'");
        }
    }

    private static LogLevel toLevel(JsonNode node) { return (node == null) ? null : LogLevel.valueOf(node.asText()); }

    private static Audit build(Operation change, ChangeBuilder<? extends Audit> builder) {
        try {
            return builder.change(change).build();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unsupported audit change: '" + change + "'");
        }
    }

    private static abstract class ChangeBuilder<T extends Audit> {
        @Setter
        protected Operation change;
        protected List<Change<?>> updates = new ArrayList<>();

        public T added() { return change(add).build(); }

        public T updated() { return change(Operation.change).build(); }

        public T removed() { return change(remove).build(); }

        public void update(Change<?> update) { updates.add(update); }

        public List<Change<?>> updates() { return updates; }

        protected abstract T build();
    }
}
