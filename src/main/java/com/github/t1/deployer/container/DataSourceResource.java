package com.github.t1.deployer.container;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

import static com.github.t1.deployer.container.DataSourceName.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static org.jboss.as.controller.client.helpers.Operations.*;

@Slf4j
@Getter
@Accessors(fluent = true, chain = true)
@Builder(builderMethodName = "do_not_call", buildMethodName = "get")
@SuppressWarnings("unused")
public class DataSourceResource extends AbstractResource<DataSourceResource> {
    private final DataSourceName name;
    private URI uri;
    private String jndiName;

    private DataSourceResource(@NonNull DataSourceName name, @NonNull CLI cli) {
        super(cli);
        this.name = name;
    }

    public static DataSourceResourceBuilder builder(DataSourceName name, CLI cli) {
        DataSourceResourceBuilder builder = new DataSourceResourceBuilder();
        builder.cli = cli;
        return builder.name(name);
    }

    public static List<DataSourceResource> allDataSources(CLI cli) {
        ModelNode all = createReadResourceOperation(address(ALL), true);
        return cli.execute(all)
                  .asList().stream()
                  .map(node -> toDataSourceResource(name(node), cli, node.get("result")))
                  .sorted(comparing(DataSourceResource::name))
                  .collect(toList());
    }

    private static DataSourceName name(ModelNode node) {
        return new DataSourceName(node.get("address").get(1).get("data-source").asString());
    }

    private static DataSourceResource toDataSourceResource(DataSourceName name, CLI cli, ModelNode node) {
        DataSourceResource dataSource = new DataSourceResource(name, cli);
        dataSource.readFrom(node);
        dataSource.deployed = true;
        return dataSource;
    }

    public static class DataSourceResourceBuilder implements Supplier<DataSourceResource> {
        private CLI cli;

        @Override public DataSourceResource get() {
            DataSourceResource dataSource = new DataSourceResource(name, cli);
            dataSource.uri = uri;
            dataSource.jndiName = jndiName;
            return dataSource;
        }
    }

    @Override public String toString() {
        return name + ":" + jndiName + ":" + uri + ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed");
    }

    @Override protected ModelNode address() { return address(name); }

    private static ModelNode address(DataSourceName name) {
        return Operations.createAddress("subsystem", "datasources", "data-source", name.getValue());
    }

    public void updateUri(URI newUri) {
        checkDeployed();
        writeAttribute("uri", newUri.toString());
    }

    public void updateJndiName(String newJndiName) {
        checkDeployed();
        writeAttribute("jndi-name", newJndiName);
    }

    @Override protected void readFrom(ModelNode result) {
        this.uri = URI.create(result.get("connection-url").asString());
        this.jndiName = result.get("jndi-name").asString();
    }

    @Override public void add() {
        log.debug("add data-source {}", name);
        ModelNode request = createAddOperation(address());

        if (uri != null)
            request.get("connection-url").set(uri.toString());
        request.get("jndi-name").set(jndiName);

        execute(request);

        this.deployed = true;
    }

    @Override public String getId() { return name.getValue(); }
}
