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
    public static String contextRoot(Path path) {
        return element(-3, path); // this is not perfect... we should read it from the container and pass it in
    }

    public static Version version(Path path) {
        String string = element(-2, path);
        return new Version(string);
    }

    public static String element(int n, Path path) {
        if (n < 0)
            n += path.getNameCount();
        return path.getName(n).toString();
    }

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

    /**
     * It's not really nice to get the version out of the repo path, but where else would I get it? Even the
     * <code>X-Result-Detail</code> header doesn't provide it.
     */
    @Override
    public Deployment getByChecksum(CheckSum checkSum) {
        ChecksumSearchResultItem item = searchByChecksum(checkSum);
        if (item == null)
            return null;
        URI result = item.getUri();
        log.debug("got uri {}", result);
        Path path = path(result);
        return deployment(checkSum, path);
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

    private Path path(URI result) {
        return Paths.get(result.getPath());
    }

    private Deployment deployment(CheckSum checkSum, Path path) {
        String name = fileNameWithoutVersion(path);
        String contextRoot = contextRoot(path);
        Version version = version(path);
        return new Deployment(name, contextRoot, checkSum, version);
    }

    private String fileNameWithoutVersion(URI uri) {
        return fileNameWithoutVersion(Paths.get(uri.getPath()));
    }

    private String fileNameWithoutVersion(Path path) {
        String version = element(-2, path);
        String fileName = element(-1, path);
        int versionIndex = fileName.indexOf("-" + version);
        int suffixIndex = fileName.lastIndexOf('.');
        String prefix = (versionIndex >= 0) ? fileName.substring(0, versionIndex) : fileName;
        String suffix = (suffixIndex >= 0) ? fileName.substring(suffixIndex, fileName.length()) : "";
        return prefix + suffix;
    }

    @Data
    @NoArgsConstructor
    @org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class FolderInfo {
        List<ChildInfo> children;
        URI uri;
    }

    @Data
    @NoArgsConstructor
    @org.codehaus.jackson.annotate.JsonIgnoreProperties(ignoreUnknown = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChildInfo {
        boolean folder;
        URI uri;
        Map<String, CheckSum> checksums;

        public CheckSum getChecksum(String type) {
            return checksums.get(type);
        }
    }

    @Override
    public List<Deployment> availableVersionsFor(CheckSum checkSum) {
        ChecksumSearchResultItem deployment = searchByChecksum(checkSum);
        URI uri = deployment.getUri();
        uri = UriBuilder.fromUri(uri).replacePath(versionsFolder(uri)).build();
        return deploymentsIn(fileNameWithoutVersion(deployment.getUri()), uri);
    }

    private String versionsFolder(URI uri) {
        Path path = path(uri);
        int length = path.getNameCount();
        return path.subpath(0, length - 2).toString();
    }

    private List<Deployment> deploymentsIn(String fileName, URI uri) {
        // eventually it would be more efficient to use the Artifactory Pro feature 'List File':
        // /api/storage/{repoKey}/{folder-path}?list[&deep=0/1][&depth=n][&listFolders=0/1][&mdTimestamps=0/1][&includeRootPath=0/1]
        try (CloseableHttpResponse response = httpGet(uri)) {
            return deploymentsIn(fileName, readEntity(response, FolderInfo.class));
        } catch (IOException e) {
            throw new RuntimeException("can't read files in " + uri, e);
        }
    }

    private List<Deployment> deploymentsIn(String fileName, FolderInfo folderInfo) {
        URI root = folderInfo.getUri();
        List<Deployment> result = new ArrayList<>();
        for (ChildInfo child : folderInfo.getChildren()) {
            URI uri = UriBuilder.fromUri(root).path(child.getUri().toString()).build();
            if (child.isFolder()) {
                result.addAll(deploymentsIn(fileName, uri));
            } else {
                Deployment deployment = deploymentIn(uri);
                if (deployment.getName().equals(fileName)) {
                    result.add(deployment);
                }
            }
        }
        return result;
    }

    private Deployment deploymentIn(URI uri) {
        try (CloseableHttpResponse response = httpGet(uri)) {
            ChildInfo file = readEntity(response, ChildInfo.class);
            return deployment(file);
        } catch (IOException e) {
            throw new RuntimeException("can't read files in " + uri, e);
        }
    }

    private Deployment deployment(ChildInfo file) {
        return deployment(file.getChecksum("sha1"), Paths.get(file.getUri().getPath()));
    }

    @Override
    @SneakyThrows(IOException.class)
    public InputStream getArtifactInputStream(CheckSum checkSum) {
        URI uri = searchByChecksum(checkSum).getDownloadUri();
        return uri.toURL().openStream();
    }
}
