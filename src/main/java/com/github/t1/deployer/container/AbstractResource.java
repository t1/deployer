package com.github.t1.deployer.container;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import static com.github.t1.deployer.container.CLI.*;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractResource {
    @NonNull private final CLI container;

    protected Boolean deployed = null;

    protected void assertDeployed() {
        if (!isDeployed())
            throw new RuntimeException("not deployed '" + this + "'");
    }

    public boolean isDeployed() {
        if (deployed == null) {
            ModelNode response = container.executeRaw(readResource(createRequestWithAddress()));
            String outcome = response.get("outcome").asString();
            if ("success".equals(outcome)) {
                this.deployed = true;
                readFrom(response.get("result"));
            } else if (isNotFoundMessage(response)) {
                this.deployed = false;
            } else {
                log.error("failed: {}", response);
                throw new RuntimeException("outcome " + outcome + ": " + response.get("failure-description"));
            }
        }
        return deployed;
    }

    protected abstract ModelNode createRequestWithAddress();

    protected abstract void readFrom(ModelNode result);

    protected void execute(ModelNode request) { container.execute(request); }

    protected void writeAttribute(String name, String value) {
        container.writeAttribute(createRequestWithAddress(), name, value);
    }

    protected void writeAttribute(String name, boolean value) {
        container.writeAttribute(createRequestWithAddress(), name, value);
    }

    public abstract void add();

    public abstract void remove();
}
