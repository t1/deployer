package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Age;
import com.github.t1.problem.WebApplicationApplicationException;
import org.junit.Test;

import static com.github.t1.deployer.model.ProcessState.reloadRequired;
import static com.github.t1.deployer.model.ProcessState.restartRequired;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_RELOAD_REQUIRED;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_RESTART_REQUIRED;

public class DataSourceDeployerTest extends AbstractDeployerTests {
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
    public void shouldFallBackToDefaultDriverWhenNotJdbcUrn() {
        givenConfiguredVariable("default.data-source-driver", "bar");
        DataSourceFixture foo = givenDataSource("foo").uri("http://example.org");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: http://example.org\n");

        foo.driver("bar").verifyAdded(audits);
    }


    @Test
    public void shouldAddXaDataSource() {
        DataSourceFixture foo = givenDataSource("foo")
                .xa(true)
                .uri("jdbc:postgresql://my-db.server.lan:5432/foo")
                .driver("postgresql");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    xa: true\n"
                + "    uri: jdbc:postgresql://my-db.server.lan:5432/foo\n");

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
    public void shouldAddDataSourceWithUserNamePassword() {
        DataSourceFixture foo = givenDataSource("foo");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    user-name: foo\n"
                + "    password: bar\n");

        foo.userName("foo").password("bar").verifyAdded(audits);
    }

    @Test
    public void shouldAddDataSourceWithMinPoolSize() {
        DataSourceFixture foo = givenDataSource("foo");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    user-name: foo\n"
                + "    pool:\n"
                + "      min: 3\n");

        foo.userName("foo").minPoolSize(3).verifyAdded(audits);
    }

    @Test
    public void shouldAddDataSourceWithInitialPoolSize() {
        DataSourceFixture foo = givenDataSource("foo");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    user-name: foo\n"
                + "    pool:\n"
                + "      initial: 5\n");

        foo.userName("foo").initialPoolSize(5).verifyAdded(audits);
    }

    @Test
    public void shouldAddDataSourceWithMaxPoolSize() {
        DataSourceFixture foo = givenDataSource("foo");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    user-name: foo\n"
                + "    pool:\n"
                + "      max: 10\n");

        foo.userName("foo").maxPoolSize(10).verifyAdded(audits);
    }

    @Test
    public void shouldAddDataSourceWithMaxAge() {
        DataSourceFixture foo = givenDataSource("foo");

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    user-name: foo\n"
                + "    pool:\n"
                + "      max-age: 10 min\n");

        foo.userName("foo").maxAge(Age.ofMinutes(10)).verifyAdded(audits);
    }


    @Test
    public void shouldNotAddExistingDataSource() {
        givenDataSource("foo").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n");

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
                + "    jndi-name: java:/datasources/bar\n");

        fixture.jndiName("java:/datasources/bar").verifyUpdatedJndiNameFrom("java:/datasources/foo", audits);
    }

    @Test
    public void shouldUpdateDriver() {
        DataSourceFixture fixture = givenDataSource("foo").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    driver: bar\n");

        fixture.driver("bar").verifyUpdatedDriverNameFrom("h2", audits);
    }

    @Test
    public void shouldUpdateUserName() {
        DataSourceFixture fixture = givenDataSource("foo").userName("bar").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    user-name: baz\n");

        fixture.userName("baz").verifyUpdatedUserNameFrom("bar", audits);
    }

    @Test
    public void shouldUpdatePassword() {
        DataSourceFixture fixture = givenDataSource("foo").password("bar").deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    password: baz\n");

        fixture.password("baz").verifyUpdatedPasswordFrom("bar", audits);
    }

    @Test
    public void shouldUpdateMinPoolSize() {
        DataSourceFixture fixture = givenDataSource("foo").minPoolSize(1).deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    pool:\n"
                + "      min: 2\n");

        fixture.minPoolSize(2).verifyUpdatedMinPoolSizeFrom(1, audits);
    }

    @Test
    public void shouldUpdateInitialPoolSize() {
        DataSourceFixture fixture = givenDataSource("foo").initialPoolSize(1).deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    pool:\n"
                + "      initial: 2\n");

        fixture.initialPoolSize(2).verifyUpdatedInitialPoolSizeFrom(1, audits);
    }

    @Test
    public void shouldUpdateMaxPoolSize() {
        DataSourceFixture fixture = givenDataSource("foo").maxPoolSize(1).deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    pool:\n"
                + "      max: 2\n");

        fixture.maxPoolSize(2).verifyUpdatedMaxPoolSizeFrom(1, audits);
    }

    @Test
    public void shouldUpdateMaxAge() {
        DataSourceFixture fixture = givenDataSource("foo").maxAge(Age.ofMinutes(2)).deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    pool:\n"
                + "      max-age: 3 min\n");

        fixture.maxAge(Age.ofMinutes(3)).verifyUpdatedMaxAgeFrom(Age.ofMinutes(2), audits);
    }

    @Test
    public void shouldFailWhenReloadIsRequired() {
        DataSourceFixture fixture = givenDataSource("foo")
                .maxAge(Age.ofMinutes(2))
                .deployed()
                .processState(CONTROLLER_PROCESS_STATE_RELOAD_REQUIRED);

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    pool:\n"
                + "      max-age: 3 min\n");

        fixture.maxAge(Age.ofMinutes(3)).verifyUpdatedMaxAgeFrom(Age.ofMinutes(2), audits);
        assertThat(audits.getProcessState()).isEqualTo(reloadRequired);
    }

    @Test
    public void shouldFailWhenRestartIsRequired() {
        DataSourceFixture fixture = givenDataSource("foo")
                .maxAge(Age.ofMinutes(2))
                .deployed()
                .processState(CONTROLLER_PROCESS_STATE_RESTART_REQUIRED);

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:h2:mem:foo\n"
                + "    pool:\n"
                + "      max-age: 3 min\n");

        fixture.maxAge(Age.ofMinutes(3)).verifyUpdatedMaxAgeFrom(Age.ofMinutes(2), audits);
        assertThat(audits.getProcessState()).isEqualTo(restartRequired);
    }

    @Test
    public void shouldUpdateXaToTrue() {
        DataSourceFixture fixture = givenDataSource("foo")
                .uri("jdbc:postgresql://my-db.server.lan:5432/foo")
                .driver("postgresql")
                .deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:postgresql://my-db.server.lan:5432/foo\n"
                + "    xa: true\n");

        fixture.verifyRemoveCli();
        fixture.xa(true);
        fixture.verifyAddCli();

        assertThat(audits.getAudits()).containsExactly(
                Audit.DataSourceAudit.of(fixture.getName()).change("xa", null, true).changed());
    }


    @Test
    public void shouldUpdateXaToFalse() {
        DataSourceFixture fixture = givenDataSource("foo")
                .xa(true)
                .uri("jdbc:postgresql://my-db.server.lan/foo")
                .driver("postgresql")
                .deployed();

        Audits audits = deploy(""
                + "data-sources:\n"
                + "  foo:\n"
                + "    uri: jdbc:postgresql://my-db.server.lan/foo\n"
                + "    xa: false\n");

        fixture.verifyRemoveCli();
        fixture.xa(false);
        fixture.verifyAddCli();

        assertThat(audits.getAudits()).containsExactly(
                Audit.DataSourceAudit.of(fixture.getName()).change("xa", true, null).changed());
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
