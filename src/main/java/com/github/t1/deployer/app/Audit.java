package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Artifact;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.*;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.*;
import static com.github.t1.deployer.app.Audit.Operation.*;
import static lombok.AccessLevel.*;

@Data
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor(access = PROTECTED)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes({ @Type(Audit.ArtifactAudit.class), @Type(Audit.LoggerAudit.class) })
@JsonInclude(NON_EMPTY)
@SuppressWarnings("ClassReferencesSubclass")
public abstract class Audit {
    public enum Operation {add, change, remove}

    @Value
    public static class Change {
        @NonNull @JsonProperty private final String name;
        @JsonProperty private final String oldValue;
        @JsonProperty private final String newValue;

        @Override public String toString() { return name + ":" + oldValue + "->" + newValue; }
    }

    @NonNull @JsonProperty private final Operation operation;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @JsonProperty private final List<Change> changes;

    @Override public String toString() { return getClass().getSimpleName() + ":" + operation; }

    public int changeCount() { return (changes == null) ? 0 : changes.size(); }

    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("artifact")
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class ArtifactAudit extends Audit {
        @NonNull @JsonProperty private final DeploymentName name;
        @NonNull @JsonProperty private final GroupId groupId;
        @NonNull @JsonProperty private final ArtifactId artifactId;
        @NonNull @JsonProperty private final Version version;

        @Override public String toString() {
            return super.toString() + ":" + name + "->" + groupId + ":" + artifactId + ":" + version;
        }

        public static ArtifactAuditBuilder of(Artifact artifact) {
            return ArtifactAudit
                    .builder()
                    .groupId(artifact.getGroupId())
                    .artifactId(artifact.getArtifactId())
                    .version(artifact.getVersion());
        }

        public static class ArtifactAuditBuilder extends AuditBuilder<ArtifactAudit> {
            public ArtifactAuditBuilder name(String name) { return name(new DeploymentName(name)); }

            public ArtifactAuditBuilder name(DeploymentName name) {
                this.name = name;
                return this;
            }

            @Override protected ArtifactAudit build() {
                return new ArtifactAudit(operation, changes, name, groupId, artifactId, version);
            }
        }

        public ArtifactAudit(Operation operation, List<Change> changes, DeploymentName name, GroupId groupId,
                ArtifactId artifactId,
                Version version) {
            super(operation, changes);
            this.name = name;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }

    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("logger")
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class LoggerAudit extends Audit {
        @NonNull @JsonProperty private final LoggerCategory category;

        @Override public String toString() { return super.toString() + ":" + category + ":" + super.changes; }

        public static LoggerAuditBuilder of(@NonNull LoggerCategory category) {
            return LoggerAudit.builder().category(category);
        }

        public static class LoggerAuditBuilder extends AuditBuilder<LoggerAudit> {
            private LoggerAuditBuilder() {}

            @Override protected LoggerAudit build() { return new LoggerAudit(operation, changes, category); }
        }

        private LoggerAudit(Operation operation, List<Change> changes, LoggerCategory category) {
            super(operation, changes);
            this.category = category;
        }
    }

    public static abstract class AuditBuilder<T extends Audit> {
        @Setter
        protected Operation operation;
        protected List<Change> changes;

        public T added() { return operation(add).build(); }

        public T changed() { return operation(change).build(); }

        public T removed() { return operation(remove).build(); }

        public <U> AuditBuilder change(String name, U oldValue, U newValue) {
            if (changes == null)
                changes = new ArrayList<>();
            changes.add(new Change(name, toString(oldValue), toString(newValue)));
            return this;
        }

        private static String toString(Object value) { return (value == null) ? null : value.toString(); }

        protected abstract T build();
    }
}
