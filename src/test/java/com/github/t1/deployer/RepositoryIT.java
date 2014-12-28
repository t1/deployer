package com.github.t1.deployer;


public class RepositoryIT {
    // @Path("/artifactory/api")
    // @AllArgsConstructor
    // public static class ArtifactoryResource {
    // @GET
    // @Path("/search/checksum")
    // @Produces(APPLICATION_JSON)
    // public String searchChecksum(@QueryParam("md5") String md5) {
    // return "{\"results\": [{" //
    // + "\"uri\":\"http://localhost:8080/artifactory/api/storage/" + pathFor(md5) + "\"" //
    // + "}]}";
    // }
    //
    // private String pathFor(String md5) {
    // switch (md5) {
    // case FOO_CHECKSUM:
    // return "libs-release-local/foo/" + CURRENT_FOO_VERSION + "/foo-" + CURRENT_FOO_VERSION + ".jar";
    // case BAR_CHECKSUM:
    // return "libs-release-local/bar/" + CURRENT_BAR_VERSION + "/bar-" + CURRENT_BAR_VERSION + ".jar";
    // default:
    // return "unknown";
    // }
    // }
    //
    // @GET
    // @Path("/search/versions")
    // @Produces(APPLICATION_JSON)
    // public String searchVersions(@QueryParam("g") String groupId, @QueryParam("a") String artifactId) {
    // if ("foo-group".equals(groupId) && "foo-artifact".equals(artifactId))
    // return "{" //
    // + "\"results\" : [" //
    // + "{\"version\" : \"1.3.10\"}," //
    // + "{\"version\" : \"1.3.2\"}," //
    // + "{\"version\" : \"1.3.1\"}," //
    // + "{\"version\" : \"1.3.0\"}," //
    // + "{\"version\" : \"1.2.1\",\"integration\" : false}," //
    // + "{\"integration\" : true,\"version\" : \"1.2.1-SNAPSHOT\"}," //
    // + "{\"version\" : \"1.2.0\"}" //
    // + "]" //
    // + "}";
    // return "{\"results\":[]}";
    // }
    // }
    //
    // @ClassRule
    // public static DropwizardClientRule artifactory = new DropwizardClientRule(new ArtifactoryResource());
    //
    // private Repository repository() {
    // return new Repository(artifactory.baseUri());
    // }
    //
    // @Test
    // public void shouldSearchVersions() {
    // List<Version> versions = repository().searchVersions("foo-group", "foo-artifact");
    //
    // assertEquals(FOO_VERSIONS, versions);
    // }
    //
    // @Test
    // public void shouldSearchByChecksum() {
    // Version version = repository().searchByChecksum(FOO_CHECKSUM);
    //
    // assertEquals(CURRENT_FOO_VERSION, version);
    // }
}
