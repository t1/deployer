package com.github.t1.deployer.repository;

import static ch.qos.logback.classic.Level.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.junit.Assert.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.io.*;
import java.net.URI;
import java.util.List;

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
        List<Deployment> deployments = repository().availableVersionsFor(fakeChecksumFor(FOO));

        for (int i = 0; i < deployments.size(); i++) {
            Deployment deployment = deployments.get(i);
            Version version = FOO_VERSIONS.get(i);

            assertEquals(FOO, deployment.getContextRoot());
            assertEquals(FOO + ".war", deployment.getName().getValue());
            assertEquals(fakeChecksumFor(FOO, version), deployment.getCheckSum());
            assertEquals(version, deployment.getVersion());
        }
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnavailable() {
        expectedException.expectMessage("expected status 200 OK but got 500 Internal Server Error");

        repository().getByChecksum(FAILING_CHECKSUM);
    }

    @Test
    public void shouldFailToSearchByChecksumWhenAmbiguous() {
        expectedException.expectMessage("checksum not unique");

        repository().getByChecksum(AMBIGUOUS_CHECKSUM);
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
