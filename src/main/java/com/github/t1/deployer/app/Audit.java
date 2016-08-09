package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.deployer.container.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.*;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.*;
import static com.github.t1.deployer.app.Audit.Operation.*;
import static com.github.t1.deployer.tools.Tools.*;
import static lombok.AccessLevel.*;

@Data
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor(access = PROTECTED)
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes({ @Type(Audit.ArtifactAudit.class), @Type(Audit.LoggerAudit.class), @Type(Audit.LogHandlerAudit.class) })
@JsonInclude(NON_EMPTY)
@JsonNaming(KebabCaseStrategy.class)
@SuppressWarnings("ClassReferencesSubclass")
public abstract class Audit {
    public enum Operation {add, change, remove}

    @Value
    @JsonNaming(KebabCaseStrategy.class)
    public static class Change {
        @JsonCreator public static Change fromJson(JsonNode node) {
            return new Change(getText(node, "name"), getText(node, "old-value"), getText(node, "new-value"));
        }

        @NonNull @JsonProperty private final String name;
        @JsonProperty private final String oldValue;
        @JsonProperty private final String newValue;

        @Override public String toString() { return name + ":" + oldValue + "->" + newValue; }
    }

    private static String getText(JsonNode node, String fieldName) {
        return (node.has(fieldName) && !node.get(fieldName).isNull()) ? node.get(fieldName).asText() : null;
    }

    @NonNull @JsonProperty private final Operation operation;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @JsonProperty private final List<Change> changes;

    @Override public String toString() { return getType() + ":" + operation; }

    @JsonIgnore public String getType() { return getClass().getAnnotation(JsonTypeName.class).value(); }

    public int changeCount() { return (changes == null) ? 0 : changes.size(); }

    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("artifact")
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class ArtifactAudit extends Audit {
        @NonNull @JsonProperty private final DeploymentName name;

        @Override public String toString() {
            return super.toString() + ":" + name + ((super.changes == null) ? "" : ":" + super.changes);
        }

        public static class ArtifactAuditBuilder extends AuditBuilder<ArtifactAudit> {
            public ArtifactAuditBuilder name(String name) { return name(new DeploymentName(name)); }

            public ArtifactAuditBuilder name(DeploymentName name) {
                this.name = name;
                return this;
            }

            @Override protected ArtifactAudit build() {
                return new ArtifactAudit(operation, changes, name);
            }
        }

        public ArtifactAudit(Operation operation, List<Change> changes, DeploymentName name) {
            super(operation, changes);
            this.name = name;
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

    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("log-handler")
    @NoArgsConstructor(access = PRIVATE, force = true)
    public static class LogHandlerAudit extends Audit {
        @NonNull @JsonProperty private final LogHandlerType type;
        @NonNull @JsonProperty private final LogHandlerName name;

        @Override public String toString() { return super.toString() + ":" + type + ":" + name + ":" + super.changes; }

        public static class LogHandlerAuditBuilder extends AuditBuilder<LogHandlerAudit> {
            private LogHandlerAuditBuilder() {}

            @Override protected LogHandlerAudit build() { return new LogHandlerAudit(operation, changes, type, name); }
        }

        private LogHandlerAudit(Operation operation, List<Change> changes, LogHandlerType type,
                LogHandlerName name) {
            super(operation, changes);
            this.type = type;
            this.name = name;
        }
    }

    @SuppressWarnings("deprecation")
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
            changes.add(new Change(name, toStringOrNull(oldValue), toStringOrNull(newValue)));
            return this;
        }

        /** only to be called from {@link #added()}, {@link #changed()}, or {@link #removed()}. */
        @Deprecated
        protected abstract T build();
    }
}
