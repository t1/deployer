package com.github.t1.deployer.container;

import com.github.t1.deployer.model.Age;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;

import java.util.Optional;

import static com.github.t1.deployer.container.CLI.*;
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
    @NonNull private final CLI cli;

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
        ModelNode response = cli.executeRaw(readResource);
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

    protected ModelNode execute(ModelNode request) { return cli.execute(request); }

    protected ModelNode execute(Operation operation) { return cli.execute(operation); }

    public void writeAttribute(String name, String value) { cli.writeAttribute(address(), name, value); }

    public void writeAttribute(String name, boolean value) { cli.writeAttribute(address(), name, value); }

    public void writeAttribute(String name, long value) { cli.writeAttribute(address(), name, value); }

    public void mapPut(String name, String key, String value) { cli.mapPut(address(), name, key, value); }

    protected void mapRemove(String name, String key) { cli.mapRemove(address(), name, key); }

    public abstract void add();

    public void remove() { execute(createRemoveOperation(address())); }

    public abstract String getId();

    public boolean matchesId(T that) { return this.getId().equals(that.getId()); }

    protected static String stringOrNull(ModelNode node, String name) {
        return getOptional(node, name).map(ModelNode::asString).orElse(null);
    }

    protected static Integer integerOrNull(ModelNode node, String name) {
        return getOptional(node, name).map(ModelNode::asInt).orElse(null);
    }

    protected static Age minutesOrNull(ModelNode node, String name) {
        return node.get(name).isDefined() ? Age.ofMinutes(node.get(name).asInt()) : null;
    }

    protected static Optional<ModelNode> getOptional(ModelNode node, String name) {
        return node.get(name).isDefined() ? Optional.of(node.get(name)) : Optional.empty();
    }
}
