package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.google.common.collect.ImmutableMap;
import lombok.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@JsonNaming(KebabCaseStrategy.class)
public class BundlePlan extends AbstractArtifactPlan {
    @NonNull @JsonIgnore private final BundleName name;
    @NonNull @Singular private final Map<String, Map<VariableName, String>> instances;

    @Override public String getId() { return name.getValue(); }

    public static class BundlePlanBuilder extends AbstractArtifactPlanBuilder<BundlePlanBuilder> {
        @Override public BundlePlan build() {
            AbstractArtifactPlan a = super.build();
            Map<String, Map<VariableName, String>> instances = buildInstances(instances$key, instances$value);
            return new BundlePlan(name, instances,
                    a.state, a.groupId, a.artifactId, a.version, a.classifier, a.checksum);
        }

        private Map<String, Map<VariableName, String>> buildInstances(
                List<String> keys, List<Map<VariableName, String>> list) {
            if (keys == null)
                return emptyMap();
            Map<String, Map<VariableName, String>> instances = new LinkedHashMap<>();
            for (int i = 0; i < keys.size(); i++)
                instances.put(keys.get(i), list.get(i));
            return instances;
        }
    }

    private BundlePlan(BundleName name, Map<String, Map<VariableName, String>> instances, DeploymentState state,
            GroupId groupId, ArtifactId artifactId, Version version, Classifier classifier, Checksum checksum) {
        super(state, groupId, artifactId, version, classifier, checksum);
        this.name = name;
        this.instances = instances;
    }


    static BundlePlan fromJson(BundleName name, JsonNode node) {
        if (node.isNull())
            throw new Plan.PlanLoadingException("incomplete bundles plan '" + name + "'");
        BundlePlanBuilder builder = builder().name(name);
        AbstractArtifactPlan.fromJson(node, builder, name.getValue(), null);
        if (node.has("instances") && !node.get("instances").isNull()) {
            Iterator<Map.Entry<String, JsonNode>> instances = node.get("instances").fields();
            while (instances.hasNext()) {
                Map.Entry<String, JsonNode> next = instances.next();
                builder.instance(next.getKey(), toVariableMap(next.getValue()));
            }
        }
        return builder.build();
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
