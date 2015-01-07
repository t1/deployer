package com.github.t1.deployer;

import static com.github.t1.deployer.TestData.*;
import static org.junit.Assert.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import lombok.SneakyThrows;

import org.junit.*;
import org.junit.rules.ExpectedException;

public class RepositoryIT {
    private static final CheckSum FAILING_CHECKSUM = CheckSum.ofHexString("1111111111111111111111111111111111111111");
    private static final CheckSum AMBIGUOUS_CHECKSUM = CheckSum.ofHexString("2222222222222222222222222222222222222222");
    private static final CheckSum UNKNOWN_CHECKSUM = CheckSum.ofHexString("3333333333333333333333333333333333333333");

    @Path("/artifactory")
    public static class ArtifactoryMock {
        @Context
        UriInfo uriInfo;

        private UriBuilder base(String path) {
            return uriInfo.getBaseUriBuilder().path(ArtifactoryMock.class).path(path);
        }

        @GET
        @Path("/api/search/checksum")
        @Produces("application/vnd.org.jfrog.artifactory.search.checksumsearchresult+json")
        public String searchChecksum(@QueryParam("md5") String md5) {
            return "{\"results\": [" + resultsFor(CheckSum.ofHexString(md5)) + "]}";
        }

        private String resultsFor(CheckSum md5) {
            if (checksumFor(FOO).equals(md5)) {
                return uriJar(FOO, CURRENT_FOO_VERSION);
            } else if (checksumFor(BAR).equals(md5)) {
                return uriJar(BAR, CURRENT_BAR_VERSION);
            } else if (FAILING_CHECKSUM.equals(md5)) {
                throw new RuntimeException("error in repo");
            } else if (AMBIGUOUS_CHECKSUM.equals(md5)) {
                return uriJar("x", null) + "," + uriJar("y", null);
            } else { // UNKNOWN_CHECKSUM or anything else
                return "";
            }
        }

        private String uriJar(String name, Version version) {
            String path = "/libs-release-local/" + name + "/" + version + "/" + name + "-" + version + ".jar";
            return "{" //
                    + "\"uri\":\"" + base("api/storage" + path) + "\",\n" //
                    + "\"downloadUri\" : \"" + base(path) + "\"\n" //
                    + "}";
        }

        @GET
        @Path("/api/storage/{repository}/{path:.*}")
        @Produces("application/vnd.org.jfrog.artifactory.storage.folderinfo+json")
        public String listFiles(@PathParam("repository") String repository, @PathParam("path") String path) {
            return "{\n" //
                    + "   \"path\" : \"" + path + "\",\n" //
                    + "   \"createdBy\" : \"kirk\",\n" //
                    + "   \"repo\" : \"" + repository + "\",\n" //
                    + "   \"modifiedBy\" : \"spock\",\n" //
                    + "   \"created\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                    + "   \"lastUpdated\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                    + "   \"children\" : [\n" //
                    + versions(path) //
                    + "      {\n" //
                    + "         \"folder\" : false,\n" //
                    + "         \"uri\" : \"/maven-metadata.xml\"\n" //
                    + "      }\n" //
                    + "   ],\n" //
                    + "   \"lastModified\" : \"2014-04-02T16:21:31.385+02:00\",\n" //
                    + "   \"uri\" : \"" + base(repository + path) + "\"\n" //
                    + "}\n" //
            ;
        }

        private String versions(String path) {
            StringBuilder out = new StringBuilder();
            for (Version version : availableVersionsFor(path)) {
                out.append(version(version.getVersion()));
            }
            return out.toString();
        }

        private String version(String version) {
            return "      {\n" //
                    + "         \"folder\" : true,\n" //
                    + "         \"uri\" : \"/" + version + "\"\n" //
                    + "      },\n" //
            ;
        }

        @GET
        @Path("/{repository}/{path:.*}")
        public InputStream getFiles(@SuppressWarnings("unused") @PathParam("repository") String repository,
                @PathParam("path") String pathString) {
            java.nio.file.Path path = Paths.get(pathString);
            int n = path.getNameCount();
            String name = path.getName(n - 1).toString();
            Version version = new Version(path.getName(n - 2).toString());
            return inputStreamFor(name, version);
        }
    }

    @ClassRule
    public static DropwizardClientRule artifactory = new DropwizardClientRule(new ArtifactoryMock());

    private Repository repository() {
        return new ArtifactoryRepository(artifactory.baseUri());
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
        expectedException.expectMessage("checksum not found");

        repository().getVersionByChecksum(UNKNOWN_CHECKSUM);
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

        assertEquals("foo-1.3.1.jar-content@1.3.1", content);
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
