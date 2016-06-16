package com.github.t1.deployer.container;

import com.github.t1.deployer.model.DataSourceConfig;
import com.github.t1.log.Logged;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import javax.ejb.Stateless;
import java.net.URI;
import java.util.*;

import static com.github.t1.log.LogLevel.*;

@Slf4j
@Logged(level = INFO)
@Stateless
public class DataSourceContainer extends AbstractContainer {
    List<DataSourceConfig> getDataSources() {
        List<DataSourceConfig> dataSources = new ArrayList<>();
        for (ModelNode cliDataSourceMatch : readAllDataSources()) {
            String name = cliDataSourceMatch.get("address").asObject().get("data-source").asString();
            dataSources.add(toDataSource(name, cliDataSourceMatch.get("result")));
        }
        return dataSources;
    }

    private List<ModelNode> readAllDataSources() {
        return execute(readDataSource("*")).asList();
    }

    private static ModelNode readDataSource(String name) {
        ModelNode request = new ModelNode();
        request.get("address").add("subsystem", "datasources").add("data-source", name);
        return readResource(request);
    }

    private DataSourceConfig toDataSource(String name, ModelNode node) {
        return DataSourceConfig.builder() //
                .name(name) //
                .jndiName(node.get("jndi-name").asString()) //
                .driver(node.get("driver-name").asString()) //
                .uri(URI.create(node.get("connection-url").asString())) //
                .user(node.get("user-name").asString()) //
                .password("************") // don't provide the real password
                .build();
    }

    boolean hasDataSource(String dataSourceName) {
        ModelNode result = executeRaw(readDataSource(dataSourceName));
        return isOutcomeFound(result);
    }

    public DataSourceConfig getDataSource(String dataSourceName) {
        ModelNode result = executeRaw(readDataSource(dataSourceName));
        String outcome = result.get("outcome").asString();
        if ("success".equals(outcome)) {
            return toDataSource(dataSourceName, result.get("result"));
        } else if (isNotFoundMessage(result)) {
            throw new RuntimeException("no data source '" + dataSourceName + "'");
        } else {
            log.error("failed: {}", result);
            throw new RuntimeException("outcome " + outcome + ": " + result.get("failure-description"));
        }
    }

    public void add(DataSourceConfig dataSource) {
        execute(addDataSource(dataSource));
    }

    private static ModelNode addDataSource(DataSourceConfig dataSource) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "datasource").add("name", dataSource.getName());
        node.get("operation").set("add");
        return node;
    }

    public void remove(String dataSourceName) {
        execute(removeDataSource(dataSourceName));
    }

    private static ModelNode removeDataSource(String dataSourceName) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "datasource").add("name", dataSourceName);
        node.get("operation").set("remove");
        return node;
    }

    public void update(DataSourceConfig dataSource) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "datasource").add("name", dataSource.getName());
        node.get("operation").set("write-attribute");
        ModelNode result = execute(node);
        System.out.println(result);
    }
}
