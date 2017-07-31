package com.github.t1.deployer.model;

import lombok.*;

import java.util.*;
import java.util.regex.Matcher;

import static com.github.t1.deployer.model.Expressions.*;

@Value
@Builder
public class BundleTree {
    String name, groupId, artifactId, version, classifier;
    @Singular Set<Variable> variables;
    @Singular List<BundleTree> bundles;

    @Value
    @Builder
    public static class Variable {
        String name;
        boolean mandatory;

        private static Variable fromExpression(String expression) {
            return builder().name(expression).build();
        }
    }

    public static BundleTree from(Plan plan) {
        BundleTreeBuilder builder = BundleTree.builder();
        convertVariables(plan, builder);
        convertBundles(plan, builder);
        return builder.build();
    }

    private static void convertVariables(Plan plan, BundleTreeBuilder builder) {
        Matcher matcher = VAR.matcher(plan.toYaml());
        while (matcher.find())
            builder.variable(Variable.fromExpression(matcher.group(1)));
    }

    private static void convertBundles(Plan plan, BundleTreeBuilder builder) {
        plan.bundles().forEach(bundle -> builder
                .bundle(BundleTree.builder()
                                  .name(bundle.getName().getValue())
                                  .groupId(bundle.getGroupId().getValue())
                                  .artifactId(bundle.getArtifactId().getValue())
                                  .version(bundle.getVersion().getValue())
                                  .classifier(bundle.getClassifier() == null ? null : bundle.getClassifier().getValue())
                                  .build()));
    }
}
