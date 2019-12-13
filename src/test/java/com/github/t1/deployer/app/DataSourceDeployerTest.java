package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Age;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;

import javax.ws.rs.BadRequestException;

import static com.github.t1.deployer.app.Audit.DataSourceAudit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_RELOAD_REQUIRED;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_RESTART_REQUIRED;
import static org.mockito.quality.Strictness.LENIENT;

@MockitoSettings(strictness = LENIENT)
class DataSourceDeployerTest extends AbstractDeployerTests {
    @Test void shouldAddEmptyDataSources() {
        deployWithRootBundle(""
            + "data-sources:\n");
    }

    @Test void shouldFailToAddDataSourcesWithoutItem() {
        givenDataSource("foo");

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"));

        assertThat(thrown).hasStackTraceContaining("incomplete data-sources plan 'foo'");
    }

    @Test void shouldFailToAddDataSourcesWithoutUri() {
        givenDataSource("foo");

        Throwable thrown = catchThrowable(() -> deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    driver: h2\n"));

        assertThat(thrown)
            .hasStackTraceContaining("field 'uri' for data-source 'foo' can only be null when undeploying");
    }

    @Test void shouldAddDataSource() {
        DataSourceFixture foo = givenDataSource("foo").uri("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE\n"
            + "    driver: h2\n");

        foo.verifyAdded();
    }

    @Test void shouldAddDataSourcesWithDriverFromJdbcUri() {
        DataSourceFixture foo = givenDataSource("foo");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n");

        foo.verifyAdded();
    }


    @Test void shouldFallBackToDefaultDriverWhenNotJdbcUrn() {
        givenConfiguredVariable("default.data-source-driver", "bar");
        DataSourceFixture foo = givenDataSource("foo").uri("http://example.org");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: http://example.org\n");

        foo.driver("bar").verifyAdded();
    }


    @Test void shouldAddXaDataSource() {
        DataSourceFixture foo = givenDataSource("foo")
            .xa(true)
            .uri("jdbc:postgresql://my-db.server.lan:5432/foo")
            .driver("postgresql");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    xa: true\n"
            + "    uri: jdbc:postgresql://my-db.server.lan:5432/foo\n");

        foo.verifyAdded();
    }

    @Test void shouldAddDataSourceWithJndiName() {
        DataSourceFixture foo = givenDataSource("foo");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    jndi-name: java:datasources/barDS\n"
            + "    driver: h2\n");

        foo.jndiName("java:datasources/barDS").verifyAdded();
    }

    @Test void shouldAddDataSourceWithUserNamePassword() {
        DataSourceFixture foo = givenDataSource("foo");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    user-name: foo\n"
            + "    password: bar\n");

        foo.userName("foo").password("bar").verifyAdded();
    }

    @Test void shouldAddDataSourceWithMinPoolSize() {
        DataSourceFixture foo = givenDataSource("foo");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    user-name: foo\n"
            + "    pool:\n"
            + "      min: 3\n");

        foo.userName("foo").minPoolSize(3).verifyAdded();
    }

    @Test void shouldAddDataSourceWithInitialPoolSize() {
        DataSourceFixture foo = givenDataSource("foo");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    user-name: foo\n"
            + "    pool:\n"
            + "      initial: 5\n");

        foo.userName("foo").initialPoolSize(5).verifyAdded();
    }

    @Test void shouldAddDataSourceWithMaxPoolSize() {
        DataSourceFixture foo = givenDataSource("foo");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    user-name: foo\n"
            + "    pool:\n"
            + "      max: 10\n");

        foo.userName("foo").maxPoolSize(10).verifyAdded();
    }

    @Test void shouldAddDataSourceWithMaxAge() {
        DataSourceFixture foo = givenDataSource("foo");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    user-name: foo\n"
            + "    pool:\n"
            + "      max-age: 10 min\n");

        foo.userName("foo").maxAge(Age.ofMinutes(10)).verifyAdded();
    }


    @Test void shouldNotAddExistingDataSource() {
        DataSourceFixture foo = givenDataSource("foo").deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n");

        foo.verifyUnchanged();
    }


    @Test void shouldUpdateUri() {
        DataSourceFixture fixture = givenDataSource("foo").uri("jdbc:h2:mem:test-old").deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:test-new\n");

        fixture.uri("jdbc:h2:mem:test-new").verifyUpdatedUriFrom("jdbc:h2:mem:test-old");
    }

    @Test void shouldUpdateJndiName() {
        DataSourceFixture fixture = givenDataSource("foo").deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    jndi-name: java:/datasources/bar\n");

        fixture.jndiName("java:/datasources/bar").verifyUpdatedJndiNameFrom("java:/datasources/foo");
    }

    @Test void shouldUpdateDriver() {
        DataSourceFixture fixture = givenDataSource("foo").deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    driver: bar\n");

        fixture.driver("bar").verifyUpdatedDriverNameFrom("h2");
    }

    @Test void shouldUpdateUserName() {
        DataSourceFixture fixture = givenDataSource("foo").userName("bar").deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    user-name: baz\n");

        fixture.userName("baz").verifyUpdatedUserNameFrom("bar");
    }

    @Test void shouldUpdatePassword() {
        DataSourceFixture fixture = givenDataSource("foo").password("bar").deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    password: baz\n");

        fixture.password("baz").verifyUpdatedPasswordFrom("bar");
    }

    @Test void shouldUpdateMinPoolSize() {
        DataSourceFixture fixture = givenDataSource("foo").minPoolSize(1).deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    pool:\n"
            + "      min: 2\n");

        fixture.minPoolSize(2).verifyUpdatedMinPoolSizeFrom(1);
    }

    @Test void shouldUpdateInitialPoolSize() {
        DataSourceFixture fixture = givenDataSource("foo").initialPoolSize(1).deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    pool:\n"
            + "      initial: 2\n");

        fixture.initialPoolSize(2).verifyUpdatedInitialPoolSizeFrom(1);
    }

    @Test void shouldUpdateMaxPoolSize() {
        DataSourceFixture fixture = givenDataSource("foo").maxPoolSize(1).deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    pool:\n"
            + "      max: 2\n");

        fixture.maxPoolSize(2).verifyUpdatedMaxPoolSizeFrom(1);
    }

    @Test void shouldUpdateMaxAge() {
        DataSourceFixture fixture = givenDataSource("foo").maxAge(Age.ofMinutes(2)).deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    pool:\n"
            + "      max-age: 3 min\n");

        fixture.maxAge(Age.ofMinutes(3)).verifyUpdatedMaxAgeFrom(Age.ofMinutes(2));
    }

    @Test void shouldFailWhenReloadIsRequired() {
        DataSourceFixture fixture = givenDataSource("foo")
            .maxAge(Age.ofMinutes(2))
            .deployed()
            .processState(CONTROLLER_PROCESS_STATE_RELOAD_REQUIRED);

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    pool:\n"
            + "      max-age: 3 min\n");

        fixture.maxAge(Age.ofMinutes(3)).verifyUpdatedMaxAgeFrom(Age.ofMinutes(2));
        fixture.verifyReloadRequired();
    }

    @Test void shouldFailWhenRestartIsRequired() {
        DataSourceFixture fixture = givenDataSource("foo")
            .maxAge(Age.ofMinutes(2))
            .deployed()
            .processState(CONTROLLER_PROCESS_STATE_RESTART_REQUIRED);

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    pool:\n"
            + "      max-age: 3 min\n");

        fixture.maxAge(Age.ofMinutes(3)).verifyUpdatedMaxAgeFrom(Age.ofMinutes(2));
        fixture.verifyRestartRequired();
    }

    @Test void shouldUpdateXaToTrue() {
        DataSourceFixture fixture = givenDataSource("foo")
            .uri("jdbc:postgresql://my-db.server.lan:5432/foo")
            .driver("postgresql")
            .deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:postgresql://my-db.server.lan:5432/foo\n"
            + "    xa: true\n");

        fixture.verifyRemoveCli();
        fixture.xa(true);
        fixture.verifyAddCli();

        assertThat(boundary.audits.getAudits()).containsExactly(
            new DataSourceAudit().setName(fixture.getName()).change("xa", null, true).changed());
    }


    @Test void shouldUpdateXaToFalse() {
        DataSourceFixture fixture = givenDataSource("foo")
            .xa(true)
            .uri("jdbc:postgresql://my-db.server.lan/foo")
            .driver("postgresql")
            .deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:postgresql://my-db.server.lan/foo\n"
            + "    xa: false\n");

        fixture.verifyRemoveCli();
        fixture.xa(false);
        fixture.verifyAddCli();

        assertThat(boundary.audits.getAudits()).containsExactly(
            new DataSourceAudit().setName(fixture.getName()).change("xa", true, null).changed());
    }


    @Test void shouldRemoveExistingDataSourceWhenStateIsUndeployed() {
        DataSourceFixture fixture = givenDataSource("foo").deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    state: undeployed\n");

        fixture.verifyRemoved();
    }

    @Test void shouldRemoveNonExistingDataSourceWhenStateIsUndeployed() {
        DataSourceFixture foo = givenDataSource("foo");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo:\n"
            + "    uri: jdbc:h2:mem:foo\n"
            + "    state: undeployed\n");

        foo.verifyUnchanged();
    }

    @Test void shouldRemoveDataSourceWhenManaged() {
        DataSourceFixture foo1 = givenDataSource("foo1").deployed();
        DataSourceFixture foo2 = givenDataSource("foo2").deployed();
        givenManaged("data-sources");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo1:\n"
            + "    uri: jdbc:h2:mem:foo1\n");

        foo1.verifyUnchanged();
        foo2.verifyRemoved();
    }

    @Test void shouldRemoveDataSourceWhenAllManaged() {
        DataSourceFixture foo1 = givenDataSource("foo1").deployed();
        DataSourceFixture foo2 = givenDataSource("foo2").deployed();
        givenManaged("all");

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  foo1:\n"
            + "    uri: jdbc:h2:mem:foo1\n");

        foo1.verifyUnchanged();
        foo2.verifyRemoved();
    }


    @Test void shouldIgnorePinnedDataSourceWhenManaged() {
        givenManaged("all");
        DataSourceFixture foo = givenDataSource("FOO").deployed();
        DataSourceFixture bar = givenDataSource("BAR").deployed().pinned();
        DataSourceFixture baz = givenDataSource("BAZ").deployed();

        deployWithRootBundle(""
            + "data-sources:\n"
            + "  FOO:\n"
            + "    uri: jdbc:h2:mem:FOO\n");

        foo.verifyUnchanged();
        bar.verifyUnchanged();
        baz.verifyRemoved();
    }

    @Test void shouldFailToDeployPinnedDataSource() {
        givenDataSource("FOO").deployed().pinned();

        Throwable thrown = catchThrowable(() ->
            deployWithRootBundle(""
                + "data-sources:\n"
                + "  FOO:\n"
                + "    uri: jdbc:h2:mem:FOO\n"));

        assertThat(thrown)
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("resource is pinned: data-source:deployed:FOO");
    }
}
