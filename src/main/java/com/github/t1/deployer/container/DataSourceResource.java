package com.github.t1.deployer.container;

import com.github.t1.deployer.model.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

import static com.github.t1.deployer.model.DataSourceName.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static org.jboss.as.controller.client.helpers.Operations.*;

/** @see javax.annotation.sql.DataSourceDefinition */
@Slf4j
@Getter
@Accessors(fluent = true, chain = true)
@Builder(builderMethodName = "do_not_call", buildMethodName = "get")
@SuppressWarnings("unused")
public class DataSourceResource extends AbstractResource<DataSourceResource> {
    private final DataSourceName name;
    private Boolean xa;
    private String driver;
    private String jndiName;
    private URI uri;

    private String userName;
    private String password;

    private Integer minPoolSize;
    private Integer initialPoolSize;
    private Integer maxPoolSize;
    private Age maxPoolAge;

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
        ModelNode nonXaRequest = createReadResourceOperation(address(ALL, false), true);
        ModelNode xaRequest = createReadResourceOperation(address(ALL, true), true);

        List<ModelNode> all = new ArrayList<>();
        all.addAll(cli.execute(nonXaRequest).asList());
        all.addAll(cli.execute(xaRequest).asList());

        return all.stream()
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
            dataSource.xa = xa;
            dataSource.uri = uri;
            dataSource.jndiName = jndiName;
            dataSource.driver = driver;

            dataSource.userName = userName;
            dataSource.password = password;

            dataSource.minPoolSize = minPoolSize;
            dataSource.initialPoolSize = initialPoolSize;
            dataSource.maxPoolSize = maxPoolSize;
            dataSource.maxPoolAge = maxPoolAge;

            return dataSource;
        }
    }

    private boolean isXa() { return (xa == null) ? false : xa; }

    @Override public String toString() {
        return name + ":" + jndiName + ":" + driver + ":" + uri +
                ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed");
    }

    @Override protected ModelNode address() { return address(name, isXa()); }

    private static ModelNode address(DataSourceName name, boolean xa) {
        return Operations.createAddress("subsystem", "datasources", (xa ? "xa-" : "") + "data-source", name.getValue());
    }

    public void updateXa(Boolean newXa) {
        checkDeployed();
        remove();
        this.xa = newXa;
        add();
    }

    public void updateUri(URI newUri) {
        checkDeployed();
        writeAttribute("uri", newUri.toString());
        this.uri = newUri;
    }

    public void updateJndiName(String newJndiName) {
        checkDeployed();
        writeAttribute("jndi-name", newJndiName);
        this.jndiName = newJndiName;
    }

    public void updateDriver(String newDriver) {
        checkDeployed();
        writeAttribute("driver-name", newDriver);
        this.driver = newDriver;
    }

    public void updateUserName(String newUserName) {
        checkDeployed();
        writeAttribute("user-name", newUserName);
        this.userName = newUserName;
    }

    public void updatePassword(String newPassword) {
        checkDeployed();
        writeAttribute("password", newPassword);
        this.password = newPassword;
    }

    public void updateMinPoolSize(Integer newMinPoolSize) {
        checkDeployed();
        writeAttribute("min-pool-size", newMinPoolSize);
        this.minPoolSize = newMinPoolSize;
    }

    public void updateInitialPoolSize(Integer newInitialPoolSize) {
        checkDeployed();
        writeAttribute("initial-pool-size", newInitialPoolSize);
        this.initialPoolSize = newInitialPoolSize;
    }

    public void updateMaxPoolSize(Integer newMaxPoolSize) {
        checkDeployed();
        writeAttribute("max-pool-size", newMaxPoolSize);
        this.maxPoolSize = newMaxPoolSize;
    }

    public void updateMaxAge(Age newMaxAge) {
        checkDeployed();
        writeAttribute("idle-timeout-minutes", newMaxAge.asMinutes());
        this.maxPoolAge = newMaxAge;
    }

    @Override protected void readFrom(ModelNode result) {
        this.uri = URI.create(result.get("connection-url").asString());
        this.jndiName = result.get("jndi-name").asString();
        this.driver = result.get("driver-name").asString();

        this.userName = stringOrNull(result, "user-name");
        this.password = stringOrNull(result, "password");

        this.minPoolSize = integerOrNull(result, "min-pool-size");
        this.initialPoolSize = integerOrNull(result, "initial-pool-size");
        this.maxPoolSize = integerOrNull(result, "max-pool-size");
        this.maxPoolAge = minutesOrNull(result, "idle-timeout-minutes");
    }

    private static Age minutesOrNull(ModelNode node, String name) {
        return node.get(name).isDefined() ? Age.ofMinutes(node.get(name).asInt()) : null;
    }

    @Override public void add() {
        log.debug("add data-source {}", name);
        ModelNode request = createAddOperation(address());

        if (uri != null)
            request.get("connection-url").set(uri.toString());
        request.get("jndi-name").set(jndiName);
        request.get("driver-name").set(driver);

        if (userName != null)
            request.get("user-name").set(userName);
        if (password != null)
            request.get("password").set(password);

        if (minPoolSize != null)
            request.get("min-pool-size").set(minPoolSize);
        if (initialPoolSize != null)
            request.get("initial-pool-size").set(initialPoolSize);
        if (maxPoolSize != null)
            request.get("max-pool-size").set(maxPoolSize);
        if (maxPoolAge != null)
            request.get("idle-timeout-minutes").set(maxPoolAge.asMinutes());

        execute(request);

        this.deployed = true;
    }

    @Override public String getId() { return name.getValue(); }
}
