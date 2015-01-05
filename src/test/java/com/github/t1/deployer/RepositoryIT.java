package com.github.t1.deployer;

import static com.github.t1.deployer.TestData.*;
import static org.junit.Assert.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.junit.*;
import org.junit.rules.ExpectedException;

public class RepositoryIT {
    private static final String FAILING_CHECKSUM = "1111111111111111111111111111111111111111";
    private static final String AMBIGUOUS_CHECKSUM = "2222222222222222222222222222222222222222";
    private static final String UNKNOWN_CHECKSUM = "3333333333333333333333333333333333333333";

    @Path("/artifactory/api")
    public static class ArtifactoryResource {
        @Context
        UriInfo uriInfo;

        private UriBuilder base(String path) {
            return uriInfo.getBaseUriBuilder().path(ArtifactoryResource.class).path(path);
        }

        @GET
        @Path("/search/checksum")
        @Produces("application/vnd.org.jfrog.artifactory.search.checksumsearchresult+json")
        public String searchChecksum(@QueryParam("md5") String md5) {
            return "{\"results\": [" + resultsFor(md5) + "]}";
        }

        private String resultsFor(String md5) {
            switch (md5) {
                case FOO_CHECKSUM:
                    return uriJar(FOO, CURRENT_FOO_VERSION);
                case BAR_CHECKSUM:
                    return uriJar(BAR, CURRENT_BAR_VERSION);
                case FAILING_CHECKSUM:
                    throw new RuntimeException("error in repo");
                case AMBIGUOUS_CHECKSUM:
                    return uriJar("x", null) + "," + uriJar("y", null);
                case UNKNOWN_CHECKSUM:
                default:
                    return "";
            }
        }

        private String uriJar(String name, Version version) {
            return "{\"uri\":\""
                    + base("storage/libs-release-local/" + name + "/" + version + "/" + name + "-" + version + ".jar")
                    + "\"}";
        }

        @GET
        @Path("/storage/{repository}/{path:.*}")
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
    }

    @ClassRule
    public static DropwizardClientRule artifactory = new DropwizardClientRule(new ArtifactoryResource());

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
        expectedException.expectMessage("error from artifactory");

        repository().searchByChecksum(FAILING_CHECKSUM);
    }

    @Test
    public void shouldFailToSearchByChecksumWhenAmbiguous() {
        expectedException.expectMessage("checksum not unique");

        repository().searchByChecksum(AMBIGUOUS_CHECKSUM);
    }

    @Test
    public void shouldFailToSearchByChecksumWhenUnknown() {
        expectedException.expectMessage("checksum not found");

        repository().searchByChecksum(UNKNOWN_CHECKSUM);
    }

    @Test
    public void shouldSearchByChecksum() {
        Version version = repository().searchByChecksum(FOO_CHECKSUM);

        assertEquals(CURRENT_FOO_VERSION, version);
    }
}
