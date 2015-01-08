package com.github.t1.deployer;

import static javax.ws.rs.core.Response.Status.*;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.github.t1.log.Logged;

@Slf4j
@Logged
public class ArtifactoryRepository implements Repository {
    private final CloseableHttpClient http = HttpClients.createDefault();
    private final URI baseUri;

    @Inject
    public ArtifactoryRepository(@Artifactory URI baseUri) {
        this.baseUri = baseUri;
    }

    @PreDestroy
    void closeWebClient() throws IOException {
        http.close();
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
    public List<Version> availableVersionsFor(CheckSum checkSum) {
        URI uri = searchByChecksum(checkSum).getUri();
        uri = UriBuilder.fromUri(uri).replacePath(versionsFolder(uri)).build(); // TODO double check

        log.debug("GET {}", uri);
        try (CloseableHttpResponse response = http.execute(new HttpGet(uri))) {
            log.debug("response {}", response);
            if (OK.getStatusCode() != response.getStatusLine().getStatusCode())
                throw new RuntimeException("error from repository: " + response.getStatusLine() + ": " + uri);
            return versionsIn(readEntity(response, FolderInfo.class));
        } catch (IOException e) {
            throw new RuntimeException("can't read available versions for " + checkSum, e);
        }
    }

    private String versionsFolder(URI uri) {
        Path path = path(uri);
        int length = path.getNameCount();
        return path.subpath(0, length - 2).toString();
    }

    private <T> T readEntity(CloseableHttpResponse response, Class<T> type) throws IOException {
        String string = EntityUtils.toString(response.getEntity());
        return new ObjectMapper().readValue(string, type);
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
    @org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChecksumSearchResult {
        List<ChecksumSearchResultItem> results;
    }

    @Data
    @NoArgsConstructor
    @org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChecksumSearchResultItem {
        URI uri;
        URI downloadUri;
    }

    private ChecksumSearchResultItem searchByChecksum(CheckSum checkSum) {
        URI uri = UriBuilder.fromUri(baseUri) //
                .path("/api/search/checksum") //
                .queryParam("md5", checkSum.hexString()) //
                .build();
        log.debug("GET {}", uri);
        try (CloseableHttpResponse response = http.execute(new HttpGet(uri))) {
            log.debug("response {}", response);
            if (OK.getStatusCode() != response.getStatusLine().getStatusCode())
                throw new RuntimeException("error from repository: " + response.getStatusLine() + ": " + uri);
            List<ChecksumSearchResultItem> results = readEntity(response, ChecksumSearchResult.class).getResults();
            if (results.size() == 0)
                throw new RuntimeException("checksum not found in repository: " + checkSum);
            if (results.size() > 1)
                throw new RuntimeException("checksum not unique in repository: " + checkSum);
            return results.get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * It's not really nice to get the version out of the repo path, but where else would I get it? Even the
     * <code>X-Result-Detail</code> header doesn't provide it.
     */
    @Override
    public Version getVersionByChecksum(CheckSum checkSum) {
        URI result = searchByChecksum(checkSum).getUri();
        Path path = path(result);
        int length = path.getNameCount();
        return new Version(path.getName(length - 2).toString());
    }

    private Path path(URI result) {
        return Paths.get(result.getPath());
    }

    @Override
    @SneakyThrows(IOException.class)
    public InputStream getArtifactInputStream(CheckSum checkSum) {
        URI uri = searchByChecksum(checkSum).getDownloadUri();
        return uri.toURL().openStream();
    }
}
