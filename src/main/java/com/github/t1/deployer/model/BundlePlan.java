package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.ImmutableMap;
import lombok.*;

import java.util.*;

import static java.util.Collections.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class BundlePlan extends AbstractArtifactPlan {
    @NonNull @JsonIgnore private final BundleName name;
    @NonNull @Singular private final Map<String, Map<Expressions.VariableName, String>> instances;

    @Override public String getId() { return name.getValue(); }

    public static class BundlePlanBuilder extends AbstractArtifactPlanBuilder<BundlePlanBuilder> {
        @Override public BundlePlan build() {
            AbstractArtifactPlan a = super.build();
            Map<String, Map<Expressions.VariableName, String>> instances = buildInstances(instances$key, instances$value);
            return new BundlePlan(name, instances,
                    a.state, a.groupId, a.artifactId, a.version, a.classifier, a.checksum);
        }

        private Map<String, Map<Expressions.VariableName, String>> buildInstances(
                List<String> keys, List<Map<Expressions.VariableName, String>> list) {
            if (keys == null)
                return emptyMap();
            Map<String, Map<Expressions.VariableName, String>> instances = new LinkedHashMap<>();
            for (int i = 0; i < keys.size(); i++)
                instances.put(keys.get(i), list.get(i));
            return instances;
        }
    }

    private BundlePlan(BundleName name, Map<String, Map<Expressions.VariableName, String>> instances, DeploymentState state,
            GroupId groupId, ArtifactId artifactId, Version version, Classifier classifier, Checksum checksum) {
        super(state, groupId, artifactId, version, classifier, checksum);
        this.name = name;
        this.instances = instances;
    }


    public static BundlePlan fromJson(BundleName name, JsonNode node) {
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
        } else {
            builder.instance(null, ImmutableMap.of());
        }
        return builder.build();
    }

    private static Map<Expressions.VariableName, String> toVariableMap(JsonNode node) {
        ImmutableMap.Builder<Expressions.VariableName, String> builder = ImmutableMap.builder();
        node.fields().forEachRemaining(field
                -> builder.put(new Expressions.VariableName(field.getKey()), field.getValue().asText()));
        return builder.build();
    }

    @Override public String toString() {
        return "bundle:" + name + ":" + super.toString() + (noInstances() ? "" : ":" + instances);
    }

    private boolean noInstances() {
        return instances.size() == 1 && instances.containsKey(null) && instances.get(null).isEmpty();
    }
}
