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

import org.apache.http.HttpHost;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.github.t1.log.Logged;

@Slf4j
@Logged
public class ArtifactoryRepository implements Repository {
    private final CloseableHttpClient http = HttpClients.createDefault();
    private final URI baseUri;
    private final Credentials credentials;

    @Inject
    public ArtifactoryRepository(@Artifactory URI baseUri, @Artifactory Credentials credentials) {
        this.baseUri = baseUri;
        this.credentials = credentials;
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
        uri = UriBuilder.fromUri(uri).replacePath(versionsFolder(uri)).build();
        try (CloseableHttpResponse response = httpGet(uri)) {
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

    private CloseableHttpResponse httpGet(URI uri) throws IOException, ClientProtocolException {
        log.debug("GET {}", uri);
        HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        CloseableHttpResponse response = http.execute(targetHost, new HttpGet(uri), authContext(targetHost));
        log.debug("response {}", response);
        return response;
    }

    private HttpClientContext authContext(HttpHost host) {
        HttpClientContext context = HttpClientContext.create();

        if (credentials != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(host.getHostName(), host.getPort()), credentials);
            context.setCredentialsProvider(credsProvider);

            AuthCache authCache = new BasicAuthCache();
            authCache.put(host, new BasicScheme());
            context.setAuthCache(authCache);
        }

        return context;
    }

    private <T> T readEntity(CloseableHttpResponse response, Class<T> type) throws IOException {
        if (OK.getStatusCode() != response.getStatusLine().getStatusCode())
            throw new RuntimeException("error from repository: " + response.getStatusLine());
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
                .queryParam("sha1", checkSum.hexString()) //
                .build();
        try (CloseableHttpResponse response = httpGet(uri)) {
            if (OK.getStatusCode() != response.getStatusLine().getStatusCode())
                throw new RuntimeException("error from repository: " + response.getStatusLine() + ": " + uri);
            List<ChecksumSearchResultItem> results = readEntity(response, ChecksumSearchResult.class).getResults();
            if (results.size() == 0)
                return null;
            if (results.size() > 1)
                throw new RuntimeException("checksum not unique in repository: " + checkSum);
            ChecksumSearchResultItem result = results.get(0);
            log.debug("got {}", result);
            return result;
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
        ChecksumSearchResultItem item = searchByChecksum(checkSum);
        if (item == null)
            return null;
        URI result = item.getUri();
        log.debug("got uri {}", result);
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
