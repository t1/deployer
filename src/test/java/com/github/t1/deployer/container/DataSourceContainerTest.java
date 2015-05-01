package com.github.t1.deployer.container;

import static com.github.t1.deployer.TestData.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import lombok.SneakyThrows;

import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.DataSourceConfig;
import com.github.t1.deployer.repository.Repository;

@RunWith(MockitoJUnitRunner.class)
public class DataSourceContainerTest {
    // /subsystem=datasources:read-resource(recursive=true)

    // "data-source" => {
    // "ExampleDS" => {
    // "allow-multiple-users" => false,
    // "connection-url" => "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    // "driver-name" => "h2",
    // "enabled" => true,
    // "jndi-name" => "java:jboss/datasources/ExampleDS",
    // "jta" => true,
    // "password" => "sa",
    // "set-tx-query-timeout" => false,
    // "share-prepared-statements" => false,
    // "spy" => false,
    // "track-statements" => "NOWARN",
    // "use-ccm" => true,
    // "use-fast-fail" => false,
    // "use-java-context" => true,
    // "user-name" => "sa",
    // "validate-on-match" => false,
    // },
    // "DefaultDS" => {
    // "allow-multiple-users" => false,
    // "connection-url" => "jdbc:postgresql://localhost:5432/rdohna",
    // "driver-name" => "postgresql-9.3-1101.jdbc41.jar",
    // "enabled" => true,
    // "jndi-name" => "java:jboss/datasources/DefaultDS",
    // "jta" => true,
    // "set-tx-query-timeout" => false,
    // "share-prepared-statements" => false,
    // "spy" => false,
    // "track-statements" => "NOWARN",
    // "use-ccm" => true,
    // "use-fast-fail" => false,
    // "use-java-context" => true,
    // "validate-on-match" => false,
    // }
    // }

    // "jdbc-driver" => {"h2" => {
    // "driver-module-name" => "com.h2database.h2",
    // "driver-name" => "h2",
    // "driver-xa-datasource-class-name" => "org.h2.jdbcx.JdbcDataSource",
    // }}


    // ///////////////////////////////////////////////////////////////////////////////////


    // /subsystem=datasources/data-source=*:read-resource(recursive=true)

    // [
    // {
    // "address" => [
    // ("subsystem" => "datasources"),
    // ("data-source" => "ExampleDS")
    // ],
    // "outcome" => "success",
    // "result" => {
    // "allow-multiple-users" => false,
    // "connection-url" => "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    // "driver-name" => "h2",
    // "enabled" => true,
    // "jndi-name" => "java:jboss/datasources/ExampleDS",
    // "jta" => true,
    // "password" => "sa",
    // "set-tx-query-timeout" => false,
    // "share-prepared-statements" => false,
    // "spy" => false,
    // "track-statements" => "NOWARN",
    // "use-ccm" => true,
    // "use-fast-fail" => false,
    // "use-java-context" => true,
    // "user-name" => "sa",
    // "validate-on-match" => false
    // }
    // },
    // {
    // "address" => [
    // ("subsystem" => "datasources"),
    // ("data-source" => "DefaultDS")
    // ],
    // "outcome" => "success",
    // "result" => {
    // "allow-multiple-users" => false,
    // "connection-url" => "jdbc:postgresql://localhost:5432/rdohna",
    // "driver-name" => "postgresql-9.3-1101.jdbc41.jar",
    // "enabled" => true,
    // "jndi-name" => "java:jboss/datasources/DefaultDS",
    // "jta" => true,
    // "set-tx-query-timeout" => false,
    // "share-prepared-statements" => false,
    // "spy" => false,
    // "track-statements" => "NOWARN",
    // "use-ccm" => true,
    // "use-fast-fail" => false,
    // "use-java-context" => true,
    // "validate-on-match" => false
    // }
    // }
    // ]


    @InjectMocks
    DataSourceContainer container;
    @Mock
    ModelControllerClient client;
    @Mock
    Repository repository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @SneakyThrows(IOException.class)
    private void givenDataSources(String... dataSources) {
        // when(client.execute(any(ModelNode.class), any(OperationMessageHandler.class))) //
        // .thenReturn(new ModelNode()); // fallback
        when(client.execute(eq(readDataSourceCli("*")), any(OperationMessageHandler.class))) //
                .thenReturn(ModelNode.fromString(successCli(readDataSourcesCliResult(dataSources))));
        for (String dataSource : dataSources) {
            when(client.execute(eq(readDataSourceCli(dataSource)), any(OperationMessageHandler.class))) //
                    .thenReturn(ModelNode.fromString("{" + dataSourceCliResult(dataSource) + "}"));
        }
    }

    private static ModelNode readDataSourceCli(String name) {
        ModelNode node = new ModelNode();
        node.get("address").add("subsystem", "datasources").add("data-source", name);
        node.get("operation").set("read-resource");
        node.get("recursive").set(true);
        return node;
    }

    private String readDataSourcesCliResult(String... dataSources) {
        StringBuilder out = new StringBuilder();
        out.append("[");
        for (String dataSource : dataSources) {
            if (out.length() > 1)
                out.append(", ");
            out.append("{") //
                    .append("\"address\" => [") //
                    .append("(\"subsystem\" => \"datasources\"),") //
                    .append("(\"data-source\" => \"").append(dataSource).append("\")") //
                    .append("],") //
                    .append(dataSourceCliResult(dataSource)) //
                    .append("}");
        }
        out.append("]");
        return out.toString();
    }

    private String dataSourceCliResult(String dataSource) {
        StringBuilder out = new StringBuilder();
        // successCli(
        out.append("")
                .append("\"outcome\" => \"success\",")
                .append("\"result\" => {")
                .append("\"allow-multiple-users\" => false,")
                .append("\"connection-url\" => \"jdbc:h2:mem:" + dataSource
                        + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE\",") //
                // "driver-name" => "h2",
                // "enabled" => true,
                .append("\"jndi-name\" => \"java:jboss/datasources/" + dataSource + "\"") //
                // "jta" => true,
                // "user-name" => "sa",
                // "password" => "sa",
                // "set-tx-query-timeout" => false,
                // "share-prepared-statements" => false,
                // "spy" => false,
                // "track-statements" => "NOWARN",
                // "use-ccm" => true,
                // "use-fast-fail" => false,
                // "use-java-context" => true,
                // "validate-on-match" => false
                .append("}");
        return out.toString();
    }

    @Test
    public void shouldGetNoDataSource() {
        givenDataSources();

        List<DataSourceConfig> dataSources = container.getDataSources();

        assertEquals(emptyList(), dataSources);
    }

    @Test
    public void shouldGetOneDataSource() {
        givenDataSources("foo");

        List<DataSourceConfig> dataSources = container.getDataSources();

        assertEquals(1, dataSources.size());
        assertEquals("foo", dataSources.get(0).getName());
        assertEquals("jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", dataSources.get(0).getUri().toString());
    }

    @Test
    public void shouldGetTwoDataSources() {
        givenDataSources("foo", "bar");

        List<DataSourceConfig> dataSources = container.getDataSources();

        assertEquals(2, dataSources.size());
        assertEquals("foo", dataSources.get(0).getName());
        assertEquals("jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", dataSources.get(0).getUri().toString());
        assertEquals("bar", dataSources.get(1).getName());
        assertEquals("jdbc:h2:mem:bar;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", dataSources.get(1).getUri().toString());
    }

    @Test
    public void shouldHaveOneDataSource() {
        givenDataSources("foo");

        assertTrue(container.hasDataSource("foo"));
    }

    @Test
    public void shouldHaveTwoDataSources() {
        givenDataSources("foo", "bar");

        assertTrue(container.hasDataSource("foo"));
        assertTrue(container.hasDataSource("bar"));
    }
}
