package com.github.t1.deployer.app;

import com.github.t1.problem.WebApplicationApplicationException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class DataSourceDeployerTest extends AbstractDeployerTest {
    @Test
    public void shouldAddEmptyDataSources() {
        deploy(""
                + "data-sources:\n");
    }

    @Test
    public void shouldFailToAddDataSourcesWithoutItem() {
        givenDataSource("foo");

        Throwable thrown = catchThrowable(() -> deploy(""
                + "data-sources:\n"
                + "  foo:\n"));

        assertThat(thrown).hasStackTraceContaining("incomplete data-sources plan 'foo'");
    }

    @Test
    public void shouldFailToAddDataSourcesWithoutUri() {
        givenDataSource("foo");

        Throwable thrown = catchThrowable(() -> deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    driver: h2\n"));

        assertThat(thrown)
                .hasStackTraceContaining("field 'uri' for data-source 'foo' can only be null when undeploying");
    }

    @Test
    public void shouldAddDataSource() {
        DataSourceFixture foo = givenDataSource("foo").uri("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE\n"
                + "    driver: h2\n");

        foo.verifyAdded(audits);
    }

    @Test
    public void shouldAddDataSourcesWithDriverFromJdbcUri() {
        DataSourceFixture foo = givenDataSource("foo");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n");

        foo.verifyAdded(audits);
    }


    @Test
    public void shouldAddDataSourceWithJndiName() {
        DataSourceFixture foo = givenDataSource("foo");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    jndi-name: java:datasources/barDS\n"
                + "    driver: h2\n");

        foo.jndiName("java:datasources/barDS").verifyAdded(audits);
    }


    @Test
    public void shouldNotAddExistingDataSource() {
        givenDataSource("foo").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    driver: h2\n");

        // #after(): no add nor update
        assertThat(audits.getAudits()).isEmpty();
    }


    @Test
    public void shouldUpdateUri() {
        DataSourceFixture fixture = givenDataSource("foo").uri("jdbc:h2:mem:test-old").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:test-new\n");

        fixture.uri("jdbc:h2:mem:test-new").verifyUpdatedUriFrom("jdbc:h2:mem:test-old", audits);
    }

    @Test
    public void shouldUpdateJndiName() {
        DataSourceFixture fixture = givenDataSource("foo").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    jndi-name: java:/datasources/barDS\n");

        fixture.jndiName("java:/datasources/barDS").verifyUpdatedJndiNameFrom("java:/datasources/fooDS", audits);
    }


    @Test
    public void shouldUpdateDriver() {
        DataSourceFixture fixture = givenDataSource("foo").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    driver: bar\n");

        fixture.driver("bar").verifyUpdatedDrierNameFrom("h2", audits);
    }


    @Test
    public void shouldRemoveExistingDataSourceWhenStateIsUndeployed() {
        DataSourceFixture fixture = givenDataSource("foo").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    state: undeployed\n");

        fixture.verifyRemoved(audits);
    }


    @Test
    public void shouldRemoveNonExistingDataSourceWhenStateIsUndeployed() {
        givenDataSource("foo");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    state: undeployed\n");

        // #after(): not undeployed
        assertThat(audits.getAudits()).isEmpty();
    }

    @Test
    public void shouldRemoveDataSourceWhenManaged() {
        givenDataSource("foo1").deployed();
        DataSourceFixture app2 = givenDataSource("foo2").deployed();
        givenManaged("data-sources");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo1:\n"
                + "    uri: jdbc:h2:mem:foo1\n");

        // #after(): app1 not undeployed
        app2.verifyRemoved(audits);
    }

    @Test
    public void shouldRemoveDataSourceWhenAllManaged() {
        givenDataSource("foo1").deployed();
        DataSourceFixture app2 = givenDataSource("foo2").deployed();
        givenManaged("all");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo1:\n"
                + "    uri: jdbc:h2:mem:foo1\n");

        // #after(): app1 not undeployed
        app2.verifyRemoved(audits);
    }

    @Test
    public void shouldIgnorePinnedDataSourceWhenManaged() {
        givenManaged("all");
        givenDataSource("FOO").deployed();
        givenDataSource("BAR").deployed().pinned();
        DataSourceFixture baz = givenDataSource("BAZ").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  FOO:\n"
                + "    uri: jdbc:h2:mem:FOO\n");

        baz.verifyRemoved(audits);
    }

    @Test
    public void shouldFailToDeployPinnedDataSource() {
        givenDataSource("FOO").deployed().pinned();

        Throwable thrown = catchThrowable(() ->
                deploy(""
                        + "data-sources:\n"
                        + "  FOO:\n"
                        + "    uri: jdbc:h2:mem:FOO\n"));

        assertThat(thrown)
                .isInstanceOf(WebApplicationApplicationException.class)
                .hasMessageContaining("resource is pinned: data-source:deployed:FOO");
    }
}
