package com.github.t1.deployer.container;

import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.DataSourceName;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

import java.lang.Boolean;
import java.net.URI;
import java.util.ArrayList;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DataSourceName.*;
import static java.lang.Boolean.*;
import static java.util.Arrays.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static org.jboss.as.controller.client.helpers.Operations.*;

/** @see javax.annotation.sql.DataSourceDefinition */
@Slf4j
@Getter
@Accessors(fluent = true, chain = true)
@Builder(builderMethodName = "do_not_call", buildMethodName = "get")
@SuppressWarnings("unused")
public final class DataSourceResource extends AbstractResource<DataSourceResource> {
    private final DataSourceName name;
    private boolean xa;
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
        return Stream.concat(
                readDataSources(cli, false),
                readDataSources(cli, true))
                     .sorted(comparing(DataSourceResource::name))
                     .collect(toList());
    }

    private static Stream<DataSourceResource> readDataSources(CLI cli, boolean xa) {
        return cli.readResource(address(ALL, xa))
                  .map(node -> toDataSourceResource(name(node, xa), cli, node.get("result"), xa));
    }

    private static DataSourceName name(ModelNode node, boolean xa) {
        ModelNode address = node.get("address").get(1);
        ModelNode dataSource = xa ? address.get("xa-data-source") : address.get("data-source");
        return new DataSourceName(dataSource.asString());
    }

    private static DataSourceResource toDataSourceResource(DataSourceName name, CLI cli, ModelNode node, boolean xa) {
        DataSourceResource dataSource = new DataSourceResource(name, cli);
        dataSource.readFrom(node);
        dataSource.xa = xa;
        dataSource.deployed = true;
        return dataSource;
    }

    public static class DataSourceResourceBuilder implements Supplier<DataSourceResource> {
        private CLI cli;

        public DataSourceResourceBuilder xa(Boolean value) {
            xa = (value == TRUE);
            return this;
        }

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

    public Boolean xa() { return xa ? TRUE : null; }

    @Override public String toString() {
        return name + ":" + jndiName + ":" + driver + ":" + uri + ":" + (xa ? "xa" : "non-xa")
                + ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed");
    }

    @Override public boolean isDeployed() {
        if (deployed == null) {
            read(address(name, false));
            if (deployed == FALSE) {
                read(address(name, true));
            }
        }
        return deployed;
    }

    @Override protected ModelNode address() { return address(name, xa); }

    private static ModelNode address(DataSourceName name, boolean xa) {
        return Operations.createAddress(addressStrings(name, xa));
    }

    private static List<String> addressStrings(DataSourceName name, boolean xa) {
        return asList("subsystem", "datasources", (xa ? "xa-" : "") + "data-source", name.getValue());
    }

    public void updateXa(Boolean newXa) {
        checkDeployed();
        remove();
        this.xa = (newXa == TRUE);
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
        writeIdleTimeout(newMaxAge.asMinutes());
        this.maxPoolAge = newMaxAge;
    }

    @Override protected void readFrom(ModelNode result) {
        this.uri = URI.create(readUri(result));
        this.xa = result.has("xa-datasource-properties");
        this.jndiName = result.get("jndi-name").asString();
        this.driver = result.get("driver-name").asString();

        this.userName = stringOrNull(result, "user-name");
        this.password = stringOrNull(result, "password");

        this.minPoolSize = integerOrNull(result, "min-pool-size");
        this.initialPoolSize = integerOrNull(result, "initial-pool-size");
        this.maxPoolSize = integerOrNull(result, "max-pool-size");
        this.maxPoolAge = minutesOrNull(result, "idle-timeout-minutes");
    }

    private String readUri(ModelNode result) {
        if (result.has("connection-url"))
            return result.get("connection-url").asString();
        ModelNode properties = result.get("xa-datasource-properties");
        return "jdbc:" + result.get("driver-name").asString()
                + "://" + properties.get("ServerName").get("value").asString()
                + (properties.has("PortNumber") ? ":" + properties.get("PortNumber").get("value").asInt() : "")
                + "/" + properties.get("DatabaseName").get("value").asString();
    }

    @Override public void add() {
        log.debug("add data-source {}", name);
        ModelNode addDataSource = createAddOperation(address());

        if (!xa)
            addDataSource.get("connection-url").set(uri.toString());
        addDataSource.get("jndi-name").set(jndiName);
        addDataSource.get("driver-name").set(driver);

        if (userName != null)
            addDataSource.get("user-name").set(userName);
        if (password != null)
            addDataSource.get("password").set(password);

        if (minPoolSize != null)
            addDataSource.get("min-pool-size").set(minPoolSize);
        if (initialPoolSize != null)
            addDataSource.get("initial-pool-size").set(initialPoolSize);
        if (maxPoolSize != null)
            addDataSource.get("max-pool-size").set(maxPoolSize);
        if (maxPoolAge != null)
            addDataSource.get("idle-timeout-minutes").set(maxPoolAge.asMinutes());

        if (xa) {
            CompositeOperationBuilder composite = CompositeOperationBuilder.create();
            composite.addStep(addDataSource);
            URI uri = (this.uri.getScheme().equals("jdbc")) ? URI.create(this.uri.getSchemeSpecificPart()) : this.uri;
            composite.addStep(addXaProperty("ServerName", uri.getHost()));
            if (uri.getPort() >= 0)
                composite.addStep(addXaProperty("PortNumber", Integer.toString(uri.getPort())));
            composite.addStep(addXaProperty("DatabaseName", databaseName(uri)));
            writeOp(composite.build());
        } else {
            writeOp(addDataSource);
        }

        this.deployed = true;
    }

    private String databaseName(URI uri) {
        String databaseName = uri.getPath();
        if (databaseName.startsWith("/"))
            databaseName = databaseName.substring(1);
        return databaseName;
    }

    private ModelNode addXaProperty(String propertyName, String value) {
        ModelNode request = createAddOperation(xaPropertiesAddress(propertyName));
        request.get("value").set(value);
        return request;
    }

    private ModelNode xaPropertiesAddress(String propertyName) {
        List<String> address = new ArrayList<>(addressStrings(name, xa));
        address.add("xa-datasource-properties");
        address.add(propertyName);
        return Operations.createAddress(address);
    }

    @Override public String getId() { return name.getValue(); }
}
