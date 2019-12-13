package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.Version;
import com.github.t1.jaxrsclienttest.JaxRsTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.net.URI;

import static com.github.t1.deployer.model.ArtifactType.war;
import static com.github.t1.deployer.repository.ArtifactoryMock.AMBIGUOUS_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JDEPEND_291_CHECKSUM;
import static com.github.t1.deployer.testtools.TestData.JOLOKIA_133_CHECKSUM;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class MavenCentralTest extends MavenCentralTestParent {
    @RegisterExtension static JaxRsTestExtension MOCK = new JaxRsTestExtension(new MavenCentralMock());

    @Override protected URI baseUri() { return MOCK.baseUri(); }

    @Path("/")
    public static class MavenCentralMock {
        @Path("/remotecontent")
        @GET public String remotecontent(@QueryParam("filepath") String filepath) {
            switch (filepath) {
                case "org/jolokia/jolokia-war/1.3.3/jolokia-war-1.3.3.war.sha1":
                    return JOLOKIA_133_CHECKSUM.hexString();
                case "jdepend/jdepend/2.9.1/jdepend-2.9.1.jar.sha1":
                    return JDEPEND_291_CHECKSUM.hexString();
                case "org/jolokia/jolokia-war/1.3.3/jolokia-war-1.3.3.pom":
                    return ""
                        + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<!--\n"
                        + "some comment\n"
                        + "  -->\n"
                        + "\n"
                        + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                        + "  <modelVersion>4.0.0</modelVersion>\n"
                        + "\n"
                        + "  <groupId>org.jolokia</groupId>\n"
                        + "  <artifactId>jolokia-war</artifactId>\n"
                        + "  <version>1.3.3</version>\n"
                        + "  <name>jolokia-war</name>\n"
                        + "  <packaging>war</packaging>\n"
                        + "  <description>agent as web application</description>\n"
                        + "\n"
                        + "  the rest of the pom is not being checked\n"
                        + "</project>\n"
                        ;
            }
            throw new NotFoundException();
        }

        @Path("/solrsearch/select")
        @Produces(APPLICATION_JSON)
        @GET public String solrsearch(
            @QueryParam("q") String q,
            @QueryParam("core") String core,
            @QueryParam("rows") int rows,
            @QueryParam("wt") String wt) {
            switch (q) {
                case "1:f6e5786754116cc8e1e9261b2a117701747b1259":
                    assertChecksumSearch(core, rows, wt);
                    return "{\n"
                        + "    \"response\": {\n"
                        + "        \"docs\": [\n"
                        + JOLOKIA_133_JSON
                        + "        ], \n"
                        + "        \"numFound\": 1, \n"
                        + "        \"start\": 0\n"
                        + "    }, \n"
                        + "    \"responseHeader\": {\n"
                        + "        \"QTime\": 0, \n"
                        + "        \"params\": {\n"
                        + "            \"fl\": \"id,g,a,v,p,ec,timestamp,tags\", \n"
                        + "            \"indent\": \"off\", \n"
                        + "            \"q\": \"1:\\\"f6e5786754116cc8e1e9261b2a117701747b1259\\\"\", \n"
                        + "            \"rows\": \"20\", \n"
                        + "            \"sort\": \"score desc,timestamp desc,g asc,a asc,v desc\", \n"
                        + "            \"version\": \"2.2\", \n"
                        + "            \"wt\": \"json\"\n"
                        + "        }, \n"
                        + "        \"status\": 0\n"
                        + "    }\n"
                        + "}";
                case "1:a255283f2278ad0fb638d56683a456a3ddd7331e":
                    assertChecksumSearch(core, rows, wt);
                    return "{\n"
                        + "    \"response\": {\n"
                        + "        \"docs\": [\n"
                        + JOLOKIA_133_JSON
                        + "        ]\n"
                        + "    }"
                        + "}";
                case "1:2222222222222222222222222222222222222222": // AMBIGUOUS_CHECKSUM
                    assertChecksumSearch(core, rows, wt);
                    return "{\n"
                        + "    \"response\": {\n"
                        + "        \"docs\": [\n"
                        + "            {\"v\": \"1.2.3\"},\n"
                        + "            {\"v\": \"1.3.2\"}\n"
                        + "        ]\n"
                        + "    }"
                        + "}";
                case "1:3333333333333333333333333333333333333333": // UNKNOWN_CHECKSUM
                    assertChecksumSearch(core, rows, wt);
                    return NO_HITS;
                case "g:no-versions AND a:no-versions-war":
                    assertGroupAndArtifactSearch(core, rows, wt);
                    return NO_HITS;
                case "g:org.jolokia AND a:jolokia-war":
                    assertGroupAndArtifactSearch(core, rows, wt);
                    return "{\n"
                        + "    \"response\": {\n"
                        + "        \"docs\": [\n"
                        + "            {\"v\": \"1.2.3\"},\n"
                        + "            {\"v\": \"1.3.2\"},\n"
                        + "            {\"v\": \"1.3.3\"},\n"
                        + "            {\"v\": \"1.3.4\"}\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
                case "g:jdepend AND a:jdepend":
                    assertGroupAndArtifactSearch(core, rows, wt);
                    return "{\n"
                        + "    \"response\": {\n"
                        + "        \"docs\": [\n"
                        + "            {\"v\": \"2.9.1\"},\n"
                        + "            {\"v\": \"2.7\"},\n"
                        + "            {\"v\": \"2.5\"},\n"
                        + "            {\"v\": \"2.3\"},\n"
                        + "            {\"v\": \"2.2\"}\n"
                        + "        ]\n"
                        + "    }\n"
                        + "}";
            }
            throw new NotFoundException();
        }

        private void assertChecksumSearch(@QueryParam("core") String core, @QueryParam("rows") int rows, @QueryParam("wt") String wt) {
            assertThat(core).isNull();
            assertThat(rows).isEqualTo(20);
            assertThat(wt).isEqualTo("json");
        }

        private void assertGroupAndArtifactSearch(@QueryParam("core") String core, @QueryParam("rows") int rows, @QueryParam("wt") String wt) {
            assertThat(core).isEqualTo("gav");
            assertThat(rows).isEqualTo(10000);
            assertThat(wt).isEqualTo("json");
        }

        private static final String JOLOKIA_133_JSON = ""
            + "            {\n"
            + "                \"a\": \"jolokia-war\", \n"
            + "                \"ec\": [\n"
            + "                    \".war\", \n"
            + "                    \".pom\"\n"
            + "                ], \n"
            + "                \"g\": \"org.jolokia\", \n"
            + "                \"id\": \"org.jolokia:jolokia-war:1.3.3\", \n"
            + "                \"p\": \"war\", \n"
            + "                \"tags\": [\n"
            + "                    \"application\", \n"
            + "                    \"agent\"\n"
            + "                ], \n"
            + "                \"timestamp\": 1455653012000, \n"
            + "                \"v\": \"1.3.3\"\n"
            + "            }\n";

        static final String NO_HITS = "{\n"
            + "    \"response\": {\n"
            + "        \"docs\": [\n"
            + "        ]\n"
            + "    }"
            + "}";
    }

    /** we'd need an ambiguous checksum in maven central to test this in MavenCentralIT */
    @Test void shouldFailToGetByAmbiguousChecksum() {
        Throwable thrown = catchThrowable(() -> repository.searchByChecksum(AMBIGUOUS_CHECKSUM));

        assertThat(thrown).isInstanceOf(RuntimeException.class).hasMessage("checksum not unique in repository: '" + AMBIGUOUS_CHECKSUM + "'");
    }

    /** Doesn't exist on Maven Central */
    @Test void shouldFailToResolveLatestArtifactWithoutVersions() {
        BadRequestException thrown = catchThrowableOfType(() ->
                repository.resolveArtifact(GroupId.of("no-versions"), ArtifactId.of("no-versions-war"), Version.of("LATEST"), war, null),
            BadRequestException.class);

        assertThat(thrown).hasMessageContaining("no versions found for no-versions:no-versions-war");
    }
}
