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
        return new ArtifactoryRepository(URI.create(artifactory.baseUri() + "/artifactory"), null);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldGetAvailableVersions() {
        List<Version> versions = repository().availableVersionsFor(checksumFor(FOO));

        assertEquals(FOO_VERSIONS, versions);
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnavailable() {
        expectedException.expectMessage("error from repository");

        repository().getVersionByChecksum(FAILING_CHECKSUM);
    }

    @Test
    public void shouldFailToSearchByChecksumWhenAmbiguous() {
        expectedException.expectMessage("checksum not unique");

        repository().getVersionByChecksum(AMBIGUOUS_CHECKSUM);
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnknown() {
        Version version = repository().getVersionByChecksum(UNKNOWN_CHECKSUM);

        assertNull(version);
    }

    @Test
    public void shouldSearchByChecksum() {
        Version version = repository().getVersionByChecksum(checksumFor(FOO));

        assertEquals(CURRENT_FOO_VERSION, version);
    }

    @Test
    public void shouldGetArtifact() {
        @SuppressWarnings("resource")
        InputStream inputStream = repository().getArtifactInputStream(checksumFor(FOO));

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
