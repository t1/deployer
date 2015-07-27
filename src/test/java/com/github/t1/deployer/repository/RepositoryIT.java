package com.github.t1.deployer.repository;

import static ch.qos.logback.classic.Level.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static com.github.t1.rest.RestContext.*;
import static org.junit.Assert.*;

import java.io.*;
import java.net.URI;
import java.util.List;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;

import ch.qos.logback.classic.*;
import io.dropwizard.testing.junit.DropwizardClientRule;
import lombok.SneakyThrows;

public class RepositoryIT {
    private static ArtifactoryMock ARTIFACTORY_MOCK = new ArtifactoryMock();

    @ClassRule
    public static DropwizardClientRule ARTIFACTORY = new DropwizardClientRule(ARTIFACTORY_MOCK);
    private final URI baseUri = URI.create(ARTIFACTORY.baseUri() + "/artifactory");
    private RestContext config = REST.register("artifactory", baseUri);
    private final ArtifactoryRepository repository = new ArtifactoryRepository(config);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void init() {
        setLogLevel("org.apache.http.wire", DEBUG);
        setLogLevel("com.github.t1.rest", DEBUG);
        setLogLevel("com.github.t1.deployer", DEBUG);
    }

    private void setLogLevel(String loggerName, Level level) {
        ((Logger) LoggerFactory.getLogger(loggerName)).setLevel(level);
    }

    @Before
    public void before() {
        ArtifactoryMock.FAKES = true;
    }

    @After
    public void after() {
        ArtifactoryMock.FAKES = false;
    }

    @Test
    public void shouldGetAvailableVersions() {
        List<VersionInfo> versions = repository.availableVersionsFor(fakeChecksumFor(FOO));

        assertEquals(FOO_VERSIONS.size(), versions.size());
        for (VersionInfo entry : versions) {
            assertEquals(fakeChecksumFor(FOO, entry.getVersion()), entry.getCheckSum());
        }
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnavailable() {
        Deployment deployment = repository.getByChecksum(FAILING_CHECKSUM);

        assertEquals("error", deployment.getVersion().toString());
        assertEquals(FAILING_CHECKSUM, deployment.getCheckSum());
        assertNull(deployment.getContextRoot());
        assertNull(deployment.getName());
        assertNull(deployment.getAvailableVersions());
    }

    @Test
    public void shouldFailToSearchByChecksumWhenAmbiguous() {
        Deployment deployment = repository.getByChecksum(AMBIGUOUS_CHECKSUM);

        assertEquals("error", deployment.getVersion().toString());
        assertEquals(AMBIGUOUS_CHECKSUM, deployment.getCheckSum());
        assertNull(deployment.getContextRoot());
        assertNull(deployment.getName());
        assertNull(deployment.getAvailableVersions());
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnknown() {
        Deployment deployment = repository.getByChecksum(UNKNOWN_CHECKSUM);

        assertEquals("unknown", deployment.getVersion().toString());
        assertEquals(UNKNOWN_CHECKSUM, deployment.getCheckSum());
        assertNull(deployment.getContextRoot());
        assertNull(deployment.getName());
        assertNull(deployment.getAvailableVersions());
    }

    @Test
    public void shouldSearchByChecksum() {
        Deployment deployment = repository.getByChecksum(fakeChecksumFor(FOO));

        assertEquals(FOO, deployment.getContextRoot());
        assertEquals(FOO_WAR, deployment.getName());
        assertEquals(fakeChecksumFor(FOO, CURRENT_FOO_VERSION), deployment.getCheckSum());
        assertEquals(CURRENT_FOO_VERSION, deployment.getVersion());
    }

    @Test
    public void shouldSearchByChecksumWithAuthorization() {
        try {
            config = config.register(baseUri, new Credentials("foo", "bar"));
            ARTIFACTORY_MOCK.setRequireAuthorization(true);

            Deployment deployment = repository.getByChecksum(fakeChecksumFor(FOO));

            assertEquals(FOO, deployment.getContextRoot());
            assertEquals(FOO_WAR, deployment.getName());
            assertEquals(fakeChecksumFor(FOO, CURRENT_FOO_VERSION), deployment.getCheckSum());
            assertEquals(CURRENT_FOO_VERSION, deployment.getVersion());
        } finally {
            ARTIFACTORY_MOCK.setRequireAuthorization(false);
        }
    }

    @Test
    public void shouldGetArtifact() {
        @SuppressWarnings("resource")
        InputStream inputStream = repository.getArtifactInputStream(fakeChecksumFor(FOO));

        assertEquals("foo-1.3.1.war@1.3.1", read(inputStream));
    }

    @SneakyThrows(IOException.class)
    private String read(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder out = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            out.append(line).append('\n');
        }
        return out.toString().trim();
    }
}
