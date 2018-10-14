package com.github.t1.deployer.testtools;

import com.github.t1.deployer.model.LogHandlerType;
import org.assertj.core.api.Condition;
import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.stream.Collector;

import static org.jboss.as.controller.client.helpers.ClientConstants.COMPOSITE;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.jboss.as.controller.client.helpers.ClientConstants.STEPS;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;

public class ModelNodeTestTools {
    public static ModelNode readLoggerRequest(String name) { return readResourceRequest("logging", "logger", name); }

    public static ModelNode readLogHandlerRequest(LogHandlerType type, String name) {
        return readResourceRequest("logging", type.getHandlerTypeName(), name);
    }

    public static ModelNode readDeploymentRequest(String name) { return readResourceRequest(null, "deployment", name); }

    public static ModelNode readAllDeploymentsRequest() { return readResourceRequest(null, "deployment", "*"); }

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
        return Collector.of(ModelNode::new, ModelNode::add, ModelNodeTestTools::addAll);
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

    public static ModelNode success(String processState) { return success(null, processState); }

    public static ModelNode success(ModelNode outcome) { return success(outcome, null); }

    public static ModelNode success(ModelNode outcome, String processState) {
        ModelNode wrapper = new ModelNode();
        wrapper.get(OUTCOME).set(SUCCESS);
        if (outcome != null)
            wrapper.get(RESULT).set(outcome);
        if (processState != null)
            wrapper.get("response-headers").set(toModelNode("{\n"
                    + "    'operation-requires-reload' => true,\n"
                    + "    'process-state' => '" + processState + "'\n"
                    + "}\n"));
        return wrapper;
    }

    public static Condition<Operation> operation(Operation expected) {
        return new Condition<>(actual -> actual.getOperation().equals(expected.getOperation()),
                "operation matching " + expected.getOperation());
    }

    public static Condition<Operation> step(ModelNode expected) {
        return new Condition<>(actual -> {
            assert actual.getOperation().get(OP).asString().equals(COMPOSITE);
            return actual.getOperation().get(STEPS).asList().contains(expected);
        },
                "composite operation containing " + expected);
    }
}
