package com.github.t1.deployer;

import static com.github.t1.deployer.ArtifactoryMock.*;
import static org.junit.Assert.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.io.*;
import java.net.URI;
import java.util.List;

import lombok.SneakyThrows;

import org.junit.*;
import org.junit.rules.ExpectedException;

public class RepositoryIT {
    @ClassRule
    public static DropwizardClientRule artifactory = new DropwizardClientRule(new ArtifactoryMock());

    private Repository repository() {
        ArtifactoryRepository repo = new ArtifactoryRepository();
        repo.rest = new RestClient(URI.create(artifactory.baseUri() + "/artifactory"));
        return repo;
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
            assertEquals(FOO + ".war", deployment.getName());
            assertEquals(fakeChecksumFor(FOO, version), deployment.getCheckSum());
            assertEquals(version, deployment.getVersion());
        }
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnavailable() {
        expectedException.expectMessage("rest error: expected 200 OK but got 500 Internal Server Error");

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

        assertEquals(CURRENT_FOO_VERSION, deployment.getVersion());
        // TODO check other fields
    }

    @Test
    public void shouldGetArtifact() {
        @SuppressWarnings("resource")
        InputStream inputStream = repository().getArtifactInputStream(fakeChecksumFor(FOO));

        String content = read(inputStream);

        assertEquals("foo-1.3.1.war-content@1.3.1", content);
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
