package com.github.t1.deployer.testtools;

import com.github.t1.deployer.model.LogHandlerType;
import org.assertj.core.api.Condition;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.dmr.*;

import java.util.stream.Collector;

import static org.jboss.as.controller.client.helpers.ClientConstants.*;

public class ModelNodeTools {
    public static ModelNode readLoggerRequest(String name) { return readResourceRequest("logging", "logger", name); }

    public static ModelNode readLogHandlerRequest(LogHandlerType type, String name) {
        return readResourceRequest("logging", type.getHandlerTypeName(), name);
    }

    public static ModelNode readDeploymentRequest(String name) { return readResourceRequest(null, "deployment", name); }

    public static ModelNode readDatasourceRequest(String name, boolean xa) {
        return readResourceRequest("datasources", dataSource(xa), name);
    }

    public static String dataSource(boolean xa) { return (xa ? "xa-" : "") + "data-source"; }


    public static ModelNode readResourceRequest(String subsystem, String type, Object name) {
        return readResourceRequest(address(subsystem, type, name));
    }

    public static ModelNode readResourceRequest(String address) {
        return toModelNode(""
                + "{\n"
                + "    'operation' => 'read-resource',\n"
                + address
                + "    'recursive' => true\n"
                + "}");
    }

    public static String address(String subsystem, String type, Object name) {
        return ""
                + "    'address' => [\n"
                + ((subsystem == null) ? "" : "        ('subsystem' => '" + subsystem + "'),\n")
                + "        ('" + type + "' => '" + name + "')\n"
                + "    ],\n";
    }

    public static ModelNode definedPropertiesOf(ModelNode in) {
        return in.asPropertyList().stream()
                 .filter(property -> property.getValue().isDefined())
                 .map((Property property) -> new ModelNode().set(property.getName(), property.getValue()))
                 .collect(toModelNode());
    }

    public static Collector<ModelNode, ?, ModelNode> toModelNode() {
        return Collector.of(ModelNode::new, ModelNode::add, ModelNodeTools::addAll);
    }

    public static ModelNode addAll(ModelNode left, ModelNode right) {
        right.asList().forEach(node -> left.add(right));
        return left;
    }

    public static ModelNode toModelNode(String string) { return ModelNode.fromString(string.replace('\'', '\"')); }

    public static Condition<ModelNode> property(String key, String value) {
        return new Condition<>((ModelNode node) -> node.asObject().get(key).asString().equals(value),
                "property '%s' equal to '%s'", key, value);
    }

    public static ModelNode success() { return success(null); }

    public static ModelNode success(ModelNode outcome) {
        ModelNode wrapper = new ModelNode();
        wrapper.get(OUTCOME).set(SUCCESS);
        if (outcome != null)
            wrapper.get(RESULT).set(outcome);
        return wrapper;
    }

    public static Operation batchOperation(String steps) { return batch(toModelNode(steps)); }

    public static Operation batch(ModelNode steps) {
        CompositeOperationBuilder builder = CompositeOperationBuilder.create();
        builder.addStep(steps);
        return builder.build();
    }

    public static String batch(String body) {
        return "{\n"
                + "    \"operation\" => \"composite\",\n"
                + "    \"address\" => [],\n"
                + "    \"rollback-on-runtime-failure\" => true,\n"
                + "    \"steps\" => ["
                + body
                + "]\n"
                + "}";
    }

    public static Condition<Operation> operation(Operation op) {
        return new Condition<>(operation -> operation.getOperation().equals(op.getOperation()),
                "operation matching " + op.getOperation());
    }

    public static Condition<Operation> step(ModelNode step) {
        return new Condition<>(operation -> {
            assert operation.getOperation().get(OP).asString().equals(COMPOSITE);
            return operation.getOperation().get(STEPS).asList().contains(step);
        },
                "composite operation containing " + step);
    }
}
