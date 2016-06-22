package com.github.t1.deployer.container;

import lombok.NonNull;
import org.jboss.dmr.ModelNode;

abstract class AbstractCliItem {
    @NonNull private final CLI cli;

    protected AbstractCliItem(CLI cli) { this.cli = cli; }

    protected AbstractCliItem writeAttribute(String name, String value) {
        ModelNode request = createRequestWithAddress();
        request.get("operation").set("write-attribute");
        request.get("name").set(name);
        request.get("value").set(value);

        execute(request);

        return this;
    }

    protected abstract ModelNode createRequestWithAddress();

    protected ModelNode execute(ModelNode request) { return cli.execute(request); }
}
