package com.github.t1.deployer.container;

import static com.github.t1.deployer.tools.WebException.*;
import static com.github.t1.log.LogLevel.*;

import java.net.URI;
import java.util.*;

import javax.ejb.Stateless;

import lombok.extern.slf4j.Slf4j;

import org.jboss.dmr.ModelNode;

import com.github.t1.deployer.model.DataSourceConfig;
import com.github.t1.log.Logged;

@Slf4j
@Logged(level = INFO)
@Stateless
public class DataSourceContainer extends AbstractContainer {
    public List<DataSourceConfig> getDataSources() {
        List<DataSourceConfig> dataSources = new ArrayList<>();
        for (ModelNode cliDataSourceMatch : readAllDataSources()) {
            String name = cliDataSourceMatch.get("address").asObject().get("data-source").asString();
            dataSources.add(toDataSource(name, cliDataSourceMatch.get("result")));
        }
        return dataSources;
    }

    private List<ModelNode> readAllDataSources() {
        ModelNode result = execute(readDataSource("*"));
        checkOutcome(result);
        return result.get("result").asList();
    }

    private static ModelNode readDataSource(String name) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "datasources").add("data-source", name);
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private DataSourceConfig toDataSource(String name, ModelNode cliDataSource) {
        URI uri = URI.create(cliDataSource.get("connection-url").asString());
        return DataSourceConfig.builder().name(name).uri(uri).build();
    }

    public boolean hasDataSource(String dataSourceName) {
        ModelNode result = execute(readDataSource(dataSourceName));
        String outcome = result.get("outcome").asString();
        if ("success".equals(outcome)) {
            return true;
        } else if (isNotFoundMessage(result)) {
            return false;
        } else {
            log.error("failed: {}", result);
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
        }
    }

    public DataSourceConfig getDataSource(String dataSourceName) {
        ModelNode result = execute(readDataSource(dataSourceName));
        String outcome = result.get("outcome").asString();
        if ("success".equals(outcome)) {
            return toDataSource(dataSourceName, result.get("result"));
        } else if (isNotFoundMessage(result)) {
            throw notFound("no data source '" + dataSourceName + "'");
        } else {
            log.error("failed: {}", result);
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
        }
    }

    private boolean isNotFoundMessage(ModelNode result) {
        String failureDescription = result.get("failure-description").toString();
        return failureDescription.contains("JBAS014807: Management resource")
                && failureDescription.contains("not found");
    }

    public void add(DataSourceConfig dataSource) {
        ModelNode result = execute(addDataSource(dataSource));
        checkOutcome(result);
    }

    private static ModelNode addDataSource(DataSourceConfig dataSource) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "datasource").add("name", dataSource.getName());
        node.get("operation").set("add");
        return node;
    }

    public void remove(DataSourceConfig dataSource) {
        ModelNode result = execute(removeDataSource(dataSource));
        checkOutcome(result);
    }

    private static ModelNode removeDataSource(DataSourceConfig dataSource) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "datasource").add("name", dataSource.getName());
        node.get("operation").set("remove");
        return node;
    }

    public void update(DataSourceConfig dataSource) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "datasource").add("name", dataSource.getName());
        node.get("operation").set("write-attribute");

        ModelNode result = execute(node);
        checkOutcome(result);
        System.out.println(result);
    }
}