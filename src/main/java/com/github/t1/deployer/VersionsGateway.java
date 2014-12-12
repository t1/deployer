package com.github.t1.deployer;

import static java.util.Arrays.*;

import java.net.URI;
import java.nio.file.*;
import java.util.List;

import lombok.*;

public class VersionsGateway {
    // private static final URI ARTIFACTORY = URI.create("http://localhost:8081");
    //
    // private final ResteasyWebTarget api;
    //
    // public VersionsGateway() {
    // this(ARTIFACTORY);
    // }
    //
    // public VersionsGateway(URI baseUri) {
    // this.api = new ResteasyClientBuilder().build().target(baseUri).path("/artifactory/api");
    // }

    @Data
    @NoArgsConstructor
    private static class VersionListResult {
        List<Version> results;
    }

    public List<Version> searchVersions(String groupId, String artifactId) {
        switch (groupId + ":" + artifactId) {
            case "deployer-group:deployer-artifact":
                return asList(new Version("1.2.3"), new Version("1.2.4"), new Version("1.2.5"));
        }
        return asList();
        // Response response = api //
        // .path("/search/versions") //
        // .queryParam("g", groupId) //
        // .queryParam("a", artifactId) //
        // .request().get();
        //
        // if (OK.getStatusCode() != response.getStatus())
        // throw new RuntimeException("error from artifactory: " + response.getStatusInfo());
        // return response.readEntity(VersionListResult.class).getResults();
    }

    @Data
    @NoArgsConstructor
    private static class ChecksumSearchResult {
        List<ChecksumSearchResultItem> results;
    }

    @Data
    @NoArgsConstructor
    private static class ChecksumSearchResultItem {
        URI uri;
    }

    public String searchByChecksum(String md5sum) {
        return "1.2.3";
        // Response response = api //
        // .path("/search/checksum") //
        // .queryParam("md5", md5sum) //
        // .request() //
        // .get();
        //
        // if (OK.getStatusCode() != response.getStatus())
        // throw new RuntimeException("error from artifactory: " + response.getStatusInfo());
        // List<ChecksumSearchResultItem> results = response.readEntity(ChecksumSearchResult.class).getResults();
        // if (results.size() == 0)
        // throw new RuntimeException("checksum not found in artifactory: " + md5sum);
        // if (results.size() > 1)
        // throw new RuntimeException("checksum not unique in artifactory: " + md5sum);
        // return version(results.get(0).getUri());
    }

    /**
     * It's not really nice to get the version out of the repo path, but where else would I get it? Even the
     * <code>X-Result-Detail</code> header doesn't provide it directly.
     */
    private Version version(URI uri) {
        Path path = Paths.get(uri.getPath());
        int length = path.getNameCount();
        return new Version(path.getName(length - 2).toString());
    }
}
