package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
@JsonNaming(KebabCaseStrategy.class)
public class BundlePlan extends AbstractArtifactPlan {
    @NonNull @JsonIgnore private final BundleName name;
    private final Map<String, Map<VariableName, String>> instances = new LinkedHashMap<>();

    @Override public String getId() { return name.getValue(); }

    @Override public BundlePlan setState(DeploymentState state) { super.setState(state); return this; }

    @Override public BundlePlan setGroupId(GroupId groupId) { super.setGroupId(groupId); return this; }

    @Override public BundlePlan setArtifactId(ArtifactId artifactId) { super.setArtifactId(artifactId); return this; }

    @Override public BundlePlan setVersion(Version version) { super.setVersion(version); return this; }

    @Override public BundlePlan setClassifier(Classifier classifier) { super.setClassifier(classifier); return this; }

    @Override public BundlePlan setChecksum(Checksum checksum) { super.setChecksum(checksum); return this; }

    public BundlePlan instance(String key, Map<VariableName, String> value) {
        instances.put(key, value);
        return this;
    }


    static BundlePlan fromJson(BundleName name, JsonNode node) {
        if (node.isNull())
            throw new Plan.PlanLoadingException("incomplete bundles plan '" + name + "'");
        BundlePlan plan = new BundlePlan(name);
        AbstractArtifactPlan.fromJson(node, plan, name.getValue(), null);
        if (node.has("instances") && !node.get("instances").isNull()) {
            Iterator<Map.Entry<String, JsonNode>> instances = node.get("instances").fields();
            while (instances.hasNext()) {
                Map.Entry<String, JsonNode> next = instances.next();
                plan.instances.put(next.getKey(), toVariableMap(next.getValue()));
            }
        }
        return plan;
    }

    private static Map<VariableName, String> toVariableMap(JsonNode node) {
        ImmutableMap.Builder<VariableName, String> builder = ImmutableMap.builder();
        node.fields().forEachRemaining(field
            -> builder.put(new VariableName(field.getKey()), field.getValue().asText()));
        return builder.build();
    }

    @Override public String toString() {
        return "bundle:" + name + ":" + super.toString()
            + ":" + actualInstances().map(Map.Entry::toString).collect(joining());
    }

    /** If there are no explicit instances, assume an implicit dummy set without name nor vars */
    public Stream<Map.Entry<String, Map<VariableName, String>>> actualInstances() {
        Map<String, Map<VariableName, String>> instances
            = this.instances.isEmpty() ? singletonMap(null, emptyMap()) : this.instances;
        return instances.entrySet().stream();
    }
}
