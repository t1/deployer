package com.github.t1.deployer.repository;

import static ch.qos.logback.classic.Level.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import lombok.SneakyThrows;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.*;

import com.github.t1.deployer.model.*;

public class RepositoryIT {
    @ClassRule
    public static DropwizardClientRule artifactory = new DropwizardClientRule(new ArtifactoryMock());

    private Repository repository() {
        ArtifactoryRepository repo = new ArtifactoryRepository();
        repo.baseUri = URI.create(artifactory.baseUri() + "/artifactory");
        repo.init();
        return repo;
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setLogLevels() {
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
    public void shouldGetAvailableVersions() {
        Map<Version, CheckSum> versions = repository().availableVersionsFor(fakeChecksumFor(FOO));

        assertEquals(FOO_VERSIONS, new ArrayList<>(versions.keySet()));
        for (Entry<Version, CheckSum> entry : versions.entrySet()) {
            assertEquals(fakeChecksumFor(FOO, entry.getKey()), entry.getValue());
        }
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnavailable() {
        try {
            repository().getByChecksum(FAILING_CHECKSUM);
        } catch (RuntimeException e) {
            assertThat(e.getCause().getMessage(),
                    containsString("expected status 200 OK but got 500 Internal Server Error"));
        }
    }

    @Test
    public void shouldFailToSearchByChecksumWhenAmbiguous() {
        try {
            repository().getByChecksum(AMBIGUOUS_CHECKSUM);
        } catch (RuntimeException e) {
            assertThat(e.getCause().getMessage(), containsString("checksum not unique"));
        }
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnknown() {
        Deployment deployment = repository().getByChecksum(UNKNOWN_CHECKSUM);

        assertNull(deployment);
    }

    @Test
    public void shouldSearchByChecksum() {
        Deployment deployment = repository().getByChecksum(fakeChecksumFor(FOO));

        assertEquals(FOO, deployment.getContextRoot());
        assertEquals(FOO_WAR, deployment.getName());
        assertEquals(fakeChecksumFor(FOO, CURRENT_FOO_VERSION), deployment.getCheckSum());
        assertEquals(CURRENT_FOO_VERSION, deployment.getVersion());
    }

    @Test
    public void shouldGetArtifact() {
        @SuppressWarnings("resource")
        InputStream inputStream = repository().getArtifactInputStream(fakeChecksumFor(FOO));

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
