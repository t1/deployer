package com.github.t1.deployer.repository;

import ch.qos.logback.classic.*;
import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import io.dropwizard.testing.junit.DropwizardClientRule;
import lombok.SneakyThrows;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.List;

import static ch.qos.logback.classic.Level.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static com.github.t1.rest.RestContext.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class RepositoryIT {
    private static ArtifactoryMock ARTIFACTORY_MOCK = new ArtifactoryMock();

    @ClassRule
    public static DropwizardClientRule ARTIFACTORY = new DropwizardClientRule(ARTIFACTORY_MOCK);
    private final URI baseUri = URI.create(ARTIFACTORY.baseUri() + "/artifactory");
    private RestContext config = REST.register("repository", baseUri);
    private final ArtifactoryRepository repository = new ArtifactoryRepository(config);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void init() {
        setLogLevel("org.apache.http.wire", DEBUG);
        // setLogLevel("com.github.t1.rest", DEBUG);
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
    public void shouldGetReleases() {
        List<Release> releases = repository.releasesFor(fakeChecksumFor(FOO));

        assertEquals(FOO_VERSIONS.size(), releases.size());
        for (Release entry : releases)
            assertEquals(fakeChecksumFor(FOO, entry.getVersion()), entry.getCheckSum());
        assertThat(releases).extracting(r -> r.getVersion().toString()) //
                .containsOnly( //
                        "1.3.12", //
                        "1.3.10", //
                        "1.3.2", //
                        "1.3.1", //
                        "1.3.0", //
                        "1.2.1.1", //
                        "1.2.1", //
                        "1.2.1-SNAPSHOT", //
                        "1.2.0" //
        );
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnavailable() {
        Deployment deployment = repository.getByChecksum(FAILING_CHECKSUM);

        assertEquals("error", deployment.getVersion().toString());
        assertEquals(FAILING_CHECKSUM, deployment.getCheckSum());
        assertNull(deployment.getContextRoot());
        assertNull(deployment.getName());
        assertNull(deployment.getReleases());
    }

    @Test
    public void shouldFailToSearchByChecksumWhenAmbiguous() {
        Deployment deployment = repository.getByChecksum(AMBIGUOUS_CHECKSUM);

        assertEquals("error", deployment.getVersion().toString());
        assertEquals(AMBIGUOUS_CHECKSUM, deployment.getCheckSum());
        assertNull(deployment.getContextRoot());
        assertNull(deployment.getName());
        assertNull(deployment.getReleases());
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnknown() {
        Deployment deployment = repository.getByChecksum(UNKNOWN_CHECKSUM);

        assertEquals("unknown", deployment.getVersion().toString());
        assertEquals(UNKNOWN_CHECKSUM, deployment.getCheckSum());
        assertNull(deployment.getContextRoot());
        assertNull(deployment.getName());
        assertNull(deployment.getReleases());
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

            Deployment deployment = new ArtifactoryRepository(config).getByChecksum(fakeChecksumFor(FOO));

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

    @Test
    public void shouldFetchArtifact() throws Exception {
        GroupId groupId = new GroupId("org.jolokia");
        ArtifactId artifactId = new ArtifactId("jolokia-war");
        Version version = new Version("1.3.3");

        Artifact artifact = repository.buildArtifact(groupId, artifactId, version);

        assertThat(artifact.getGroupId()).isEqualTo(groupId);
        assertThat(artifact.getArtifactId()).isEqualTo(artifactId);
        assertThat(artifact.getVersion()).isEqualTo(version);
        assertThat(artifact.getSha1()).isEqualTo(CheckSum.fromString("F6E5786754116CC8E1E9261B2A117701747B1259"));
    }
}
