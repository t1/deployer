package com.github.t1.deployer.container;

import com.github.t1.deployer.model.Age;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;

import java.util.Optional;

import static com.github.t1.deployer.container.Batch.*;
import static org.jboss.as.controller.client.helpers.ClientConstants.*;
import static org.jboss.as.controller.client.helpers.Operations.*;

/**
 * Resources represent the configured state of the JavaEE container. They are responsible to {@link #add()},
 * {@link #remove()}, or update (using overloaded `updateSomething` methods) the current state in the container;
 * and to provide information about an existing resource with {@link #isDeployed()} and various fluent getters.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractResource<T extends AbstractResource<T>> {
    @NonNull private final Batch batch;

    protected Boolean deployed = null;

    protected void checkDeployed() {
        if (!isDeployed())
            throw new RuntimeException("not deployed '" + this + "'");
    }

    public boolean isDeployed() {
        if (deployed == null) {
            read(address());
        }
        return deployed;
    }

    protected void read(ModelNode address) {
        log.debug("isDeployed: {}", address);
        ModelNode readResource = createReadResourceOperation(address, true);
        ModelNode response = batch.executeRaw(readResource);
        if (response == null)
            throw new RuntimeException("read-resource not properly mocked: " + this);
        if (isSuccessfulOutcome(response)) {
            this.deployed = true;
            readFrom(response.get(RESULT));
        } else if (isNotFoundMessage(response)) {
            this.deployed = false;
        } else {
            log.error("failed: {}", response);
            throw new RuntimeException("outcome " + response.get("outcome").asString()
                    + ": " + getFailureDescription(response));
        }
    }

    protected abstract ModelNode address();

    protected abstract void readFrom(ModelNode result);

    protected void writeOp(ModelNode request) { batch.execute(request); }

    protected void writeOp(Operation operation) { batch.execute(operation); }

    protected void writeAttribute(String name, String value) { batch.writeAttr(address(), name, ModelNode::set, value); }

    protected void writeUseParentHandlers(Boolean value) {
        batch.writeAttr(address(), "use-parent-handlers", ModelNode::set, value);
    }

    protected void writeAttribute(String name, Integer value) { batch.writeAttr(address(), name, ModelNode::set, value); }

    protected void writeIdleTimeout(Long value) {
        batch.writeAttr(address(), "idle-timeout-minutes", ModelNode::set, value);
    }

    protected void writeProperty(String key, String value) { batch.writeProperty(address(), key, value); }

    protected void propertyRemove(String key) { batch.removeProperty(address(), key); }

    public abstract void add();

    public void remove() { writeOp(createRemoveOperation(address())); }

    public abstract String getId();

    public boolean matchesId(T that) { return this.getId().equals(that.getId()); }

    protected static String stringOrNull(ModelNode node, String name) {
        return getOptional(node, name).map(ModelNode::asString).orElse(null);
    }

    protected static Integer integerOrNull(ModelNode node, String name) {
        return getOptional(node, name).map(ModelNode::asInt).orElse(null);
    }

    @SuppressWarnings("SameParameterValue") protected static Age minutesOrNull(ModelNode node, String name) {
        return node.get(name).isDefined() ? Age.ofMinutes(node.get(name).asInt()) : null;
    }

    protected static Optional<ModelNode> getOptional(ModelNode node, String name) {
        return node.get(name).isDefined() ? Optional.of(node.get(name)) : Optional.empty();
    }
}
