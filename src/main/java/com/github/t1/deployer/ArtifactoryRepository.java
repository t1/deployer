package com.github.t1.deployer;

import static javax.ws.rs.core.Response.Status.*;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import lombok.*;

public class ArtifactoryRepository implements Repository {
    private final Client webClient = ClientBuilder.newClient();
    private final WebTarget api;

    public ArtifactoryRepository(URI baseUri) {
        this.api = webClient.target(baseUri).path("/artifactory/api");
    }

    @PreDestroy
    void close() {
        webClient.close();
    }

    @Data
    @NoArgsConstructor
    @org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class FolderInfo {
        List<FolderInfoChild> children;
    }

    @Data
    @NoArgsConstructor
    @org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class FolderInfoChild {
        boolean folder;
        URI uri;
    }

    @Override
    public List<Version> availableVersionsFor(String checkSum) {
        URI uri = getUriByChecksum(checkSum);
        UriBuilder uriBuilder = UriBuilder.fromUri(uri).replacePath(versionsFolder(uri));

        Response response = webClient.target(uriBuilder) //
                .request().get();

        if (OK.getStatusCode() != response.getStatus())
            throw new RuntimeException("error from artifactory: " + response.getStatusInfo() + ": " + uriBuilder);
        return versionsIn(response.readEntity(FolderInfo.class));
    }

    private String versionsFolder(URI uri) {
        Path path = path(uri);
        int length = path.getNameCount();
        return path.subpath(0, length - 2).toString();
    }

    private List<Version> versionsIn(FolderInfo folderInfo) {
        List<Version> result = new ArrayList<>();
        for (FolderInfoChild child : folderInfo.getChildren()) {
            if (child.isFolder()) {
                String version = child.getUri().getPath();
                if (version.startsWith("/"))
                    version = version.substring(1);
                result.add(new Version(version));
            }
        }
        return result;
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

    private URI getUriByChecksum(String md5sum) {
        Response response = api //
                .path("/search/checksum") //
                .queryParam("md5", md5sum) //
                .request() //
                .get();

        if (OK.getStatusCode() != response.getStatus())
            throw new RuntimeException("error from artifactory: " + response.getStatusInfo());
        List<ChecksumSearchResultItem> results = response.readEntity(ChecksumSearchResult.class).getResults();
        if (results.size() == 0)
            throw new RuntimeException("checksum not found in artifactory: " + md5sum);
        if (results.size() > 1)
            throw new RuntimeException("checksum not unique in artifactory: " + md5sum);
        ChecksumSearchResultItem result = results.get(0);
        return result.getUri();
    }

    /**
     * It's not really nice to get the version out of the repo path, but where else would I get it? Even the
     * <code>X-Result-Detail</code> header doesn't provide it.
     */
    @Override
    public Version searchByChecksum(String md5sum) {
        URI result = getUriByChecksum(md5sum);
        Path path = path(result);
        int length = path.getNameCount();
        return new Version(path.getName(length - 2).toString());
    }

    private Path path(URI result) {
        return Paths.get(result.getPath());
    }

    @Override
    public InputStream getArtifactInputStream(String md5sum, Version version) {
        // TODO Auto-generated method stub
        return null;
    }
}
