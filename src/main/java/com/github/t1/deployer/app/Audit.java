package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.deployer.model.DataSourceName;
import com.github.t1.deployer.model.DeploymentName;
import com.github.t1.deployer.model.LogHandlerName;
import com.github.t1.deployer.model.LogHandlerType;
import com.github.t1.deployer.model.LoggerCategory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static com.github.t1.deployer.app.Audit.Operation.add;
import static com.github.t1.deployer.app.Audit.Operation.change;
import static com.github.t1.deployer.app.Audit.Operation.remove;
import static com.github.t1.deployer.tools.Tools.toStringOrNull;

@Data
@Accessors(fluent = true, chain = true)
@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes({
    @Type(Audit.DeployableAudit.class),
    @Type(Audit.LoggerAudit.class),
    @Type(Audit.LogHandlerAudit.class),
    @Type(Audit.DataSourceAudit.class),
})
@JsonInclude(NON_EMPTY)
@JsonNaming(KebabCaseStrategy.class)
@SuppressWarnings("ClassReferencesSubclass")
public abstract class Audit {
    @JsonProperty private Operation operation;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @JsonProperty private List<Change> changes;

    public enum Operation {add, change, remove}

    @Value
    @JsonNaming(KebabCaseStrategy.class)
    public static class Change {
        @JsonCreator public static Change fromJson(JsonNode node) {
            return new Change(getText(node, "name"), getText(node, "old-value"), getText(node, "new-value"));
        }

        @JsonProperty private String name;
        @JsonProperty private String oldValue;
        @JsonProperty private String newValue;

        @Override public String toString() { return name + ":" + oldValue + "->" + newValue; }
    }

    private static String getText(JsonNode node, String fieldName) {
        return (node.has(fieldName) && !node.get(fieldName).isNull()) ? node.get(fieldName).asText() : null;
    }

    @Override public String toString() { return getTypeName() + ":" + operation; }

    @JsonIgnore private String getTypeName() { return getClass().getAnnotation(JsonTypeName.class).value(); }

    int changeCount() { return (changes == null) ? 0 : changes.size(); }

    @Data @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("deployable")
    public static class DeployableAudit extends Audit {
        @JsonProperty private DeploymentName name;

        public DeployableAudit setName(String name) { return setName(new DeploymentName(name)); }

        public DeployableAudit setName(DeploymentName name) { this.name = name; return this; }

        @Override public String toString() {
            return super.toString() + ":" + name + ((super.changes == null) ? "" : ":" + super.changes);
        }
    }

    @Data @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("logger")
    public static class LoggerAudit extends Audit {
        @JsonProperty private LoggerCategory category;

        @Override public String toString() { return super.toString() + ":" + category + ":" + super.changes; }
    }

    @Data @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("log-handler")
    public static class LogHandlerAudit extends Audit {
        @JsonProperty private LogHandlerType type;
        @JsonProperty private LogHandlerName name;

        @Override public String toString() { return super.toString() + ":" + type + ":" + name + ":" + super.changes; }
    }

    @Data @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("data-source")
    public static class DataSourceAudit extends Audit {
        @JsonProperty private DataSourceName name;

        @Override public String toString() { return super.toString() + ":" + name + ":" + super.changes; }
    }

    Audit added() { return operation(add); }

    Audit changed() { return operation(change); }

    Audit removed() { return operation(remove); }

    public <U> Audit change(String name, @Nullable U oldValue, @Nullable U newValue) {
        String oldString = toStringOrNull(oldValue);
        String newString = toStringOrNull(newValue);
        if (!Objects.equals(oldString, newString))
            changeRaw(name, oldString, newString);
        return this;
    }

    Audit changeRaw(String name, String oldString, String newString) {
        if (changes == null)
            changes = new ArrayList<>();
        changes.add(new Change(name, oldString, newString));
        return this;
    }
}
