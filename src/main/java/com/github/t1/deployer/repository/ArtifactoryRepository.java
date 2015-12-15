package com.github.t1.deployer.repository;

import static com.github.t1.deployer.repository.ArtifactoryRepository.SearchResult.*;
import static com.github.t1.deployer.repository.ArtifactoryRepository.SearchResultStatus.*;
import static com.github.t1.ramlap.tools.ProblemDetail.*;
import static java.util.Collections.*;
import static javax.ws.rs.core.Response.Status.*;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import javax.ws.rs.core.UriBuilder;

import com.github.t1.config.Config;
import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArtifactoryRepository extends Repository {
    public static ContextRoot contextRoot(Path path) {
        // this is not perfect... we should read it from the container and pass it in
        return new ContextRoot(element(-3, path));
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

    @Config(defaultValue = "http://localhost:8081/artifactory", meta = "{'label':'URI','order':100}",
            description = "The base URI where the Artifactory instance can be reached")
    URI artifactory;

    @Config(defaultValue = "", meta = "{'label':'User Name','order':110}",
            description = "The user name used to authenticate on the Artifactory server. Leave empty to use Artifactory without authenticating.")
    String artifactoryUserName;

    @Config(defaultValue = "", meta = "{'label':'Password','order':120}",
            description = "The password used to authenticate on the Artifactory server.")
    Password artifactoryPassword;

    private RestContext restContext;

    private RestContext rest() {
        if (restContext == null) {
            restContext = RestContext.REST.register("repository", artifactory);
            if (artifactoryUserName != null && artifactoryPassword != null) {
                log.debug("put {} credentials for {}", artifactoryUserName, artifactory);
                Credentials credentials = new Credentials(artifactoryUserName, artifactoryPassword.getValue());
                restContext = restContext.register(artifactory, credentials);
            }
        }
        return restContext;
    }

    public ArtifactoryRepository() {}

    public ArtifactoryRepository(RestContext restContext) {
        this.restContext = restContext;
    }

    /**
     * It's not really nice to get the version out of the repo path, but where else would I get it? Even with the
     * <code>X-Result-Detail</code> header, it's not provided.
     */
    @Override
    public Deployment getByChecksum(CheckSum checkSum) {
        if (isEmpty(checkSum))
            return new Deployment().withVersion(NO_CHECKSUM);
        SearchResult result = searchByChecksum(checkSum);
        switch (result.getStatus()) {
        case error:
            return new Deployment().withCheckSum(checkSum).withVersion(ERROR);
        case unknown:
            return new Deployment().withCheckSum(checkSum).withVersion(UNKNOWN);
        case ok:
            break;
        }
        URI uri = result.getUri();
        log.debug("got uri {}", uri);
        Path path = path(uri);
        return deployment(checkSum, path);
    }

    private boolean isEmpty(CheckSum checkSum) {
        return checkSum == null || checkSum.isEmpty();
    }

    private SearchResult searchByChecksum(CheckSum checkSum) {
        RestRequest<ChecksumSearchResult> request = searchByChecksumRequest();
        try {
            log.debug("searchByChecksum({})", checkSum);
            List<ChecksumSearchResultItem> results = request.with("checkSum", checkSum.hexString()) //
                    .GET() //
                    .getResults();
            if (results.size() == 0)
                return searchResult().status(SearchResultStatus.unknown).build();
            if (results.size() > 1)
                throw new RuntimeException("checksum not unique in repository: " + checkSum);
            ChecksumSearchResultItem item = results.get(0);
            log.debug("got {}", item);
            return searchResult().status(ok).uri(item.getUri()).downloadUri(item.getDownloadUri()).build();
        } catch (RuntimeException e) {
            log.error("can't search by checksum [" + checkSum + "] in " + request, e);
            return searchResult().status(error).build();
        }
    }

    private RestRequest<ChecksumSearchResult> searchByChecksumRequest() {
        UriTemplate uri = rest() //
                .nonQueryUri("repository") //
                .path("api/search/checksum") //
                .query("sha1", "{checkSum}");
        RestRequest<ChecksumSearchResult> searchByChecksum = rest() //
                .createResource(uri) //
                .header("X-Result-Detail", "info") //
                .accept(ChecksumSearchResult.class);
        log.debug("configured searchByChecksum request: {}", searchByChecksum);
        return searchByChecksum;
    }

    public enum SearchResultStatus {
        ok,
        unknown,
        error;
    }

    @lombok.Value
    @lombok.Builder(builderMethodName = "searchResult")
    protected static class SearchResult {
        SearchResultStatus status;
        URI uri;
        URI downloadUri;

        public boolean is(SearchResultStatus status) {
            return this.status == status;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @VendorType("org.jfrog.artifactory.search.ChecksumSearchResult")
    private static class ChecksumSearchResult {
        List<ChecksumSearchResultItem> results;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    private static class ChecksumSearchResultItem {
        URI uri;
        URI downloadUri;
    }

    private Path path(URI result) {
        return Paths.get(result.getPath());
    }

    private static Deployment deployment(CheckSum checkSum, Path path) {
        DeploymentName name = new DeploymentName(fileNameWithoutVersion(path));
        ContextRoot contextRoot = contextRoot(path);
        Version version = version(path);
        return new Deployment(name, contextRoot, checkSum, version);
    }

    private static String fileNameWithoutVersion(URI uri) {
        return fileNameWithoutVersion(Paths.get(uri.getPath()));
    }

    private static String fileNameWithoutVersion(Path path) {
        String version = element(-2, path);
        String fileName = element(-1, path);
        int versionIndex = fileName.indexOf("-" + version);
        int suffixIndex = fileName.lastIndexOf('.');
        String prefix = (versionIndex >= 0) ? fileName.substring(0, versionIndex) : fileName;
        String suffix = (suffixIndex >= 0) ? fileName.substring(suffixIndex, fileName.length()) : "";
        return prefix + suffix;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @VendorType("org.jfrog.artifactory.storage.FolderInfo")
    private static class FolderInfo {
        List<FileInfo> children;
        URI uri;

        public List<FileInfo> getChildren() {
            return (children == null) ? Collections.<FileInfo> emptyList() : children;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @VendorType("org.jfrog.artifactory.storage.FileInfo")
    private static class FileInfo {
        boolean folder;
        URI uri;
        Map<String, CheckSum> checksums;

        public Deployment deployment() {
            return ArtifactoryRepository.deployment(getChecksum(), Paths.get(getUri().getPath()));
        }

        public CheckSum getChecksum() {
            return checksums.get("sha1");
        }
    }

    @Override
    public List<Release> releasesFor(CheckSum checkSum) {
        SearchResult result = searchByChecksum(checkSum);
        if (!result.is(ok))
            return emptyList();
        URI uri = result.getUri();
        uri = UriBuilder.fromUri(uri).replacePath(versionsFolder(uri)).build();
        return releasesIn(fileNameWithoutVersion(result.getUri()), uri);
    }

    private String versionsFolder(URI uri) {
        Path path = path(uri);
        int length = path.getNameCount();
        return path.subpath(0, length - 2).toString();
    }

    private List<Release> releasesIn(String fileName, URI uri) {
        // TODO we assume the path of files anyways... so we could reduce the number of requests and not recurse fully
        log.trace("get deployments in {} (fileName: {})", uri, fileName);
        // TODO eventually it would be more efficient to use the Artifactory Pro feature 'List File':
        // /api/storage/{repoKey}/{folder-path}?list[&deep=0/1][&depth=n][&listFolders=0/1][&mdTimestamps=0/1][&includeRootPath=0/1]
        FolderInfo folderInfo = rest().createResource(uri).GET(FolderInfo.class);
        log.trace("got {}", folderInfo);
        return releasesIn(fileName, folderInfo);
    }

    private List<Release> releasesIn(String fileName, FolderInfo folderInfo) {
        URI root = folderInfo.getUri();
        List<Release> result = new ArrayList<>();
        for (FileInfo child : folderInfo.getChildren()) {
            URI uri = UriBuilder.fromUri(root).path(child.getUri().toString()).build();
            if (child.isFolder()) {
                result.addAll(releasesIn(fileName, uri));
            } else {
                log.trace("get deployment in {} (fileName: {})", uri, fileName);
                Deployment deployment = deploymentIn(uri);
                if (deployment.getName().getValue().equals(fileName)) {
                    result.add(new Release(deployment.getVersion(), deployment.getCheckSum()));
                }
            }
        }
        return result;
    }

    private Deployment deploymentIn(URI uri) {
        FileInfo file = rest().createResource(uri).GET(FileInfo.class);
        return file.deployment();
    }

    @Override
    public InputStream getArtifactInputStream(CheckSum checkSum) {
        SearchResult found = searchByChecksum(checkSum);
        switch (found.getStatus()) {
        case error:
            throw webException(BAD_GATEWAY, "error while finding checksum " + checkSum);
        case unknown:
            throw notFound("checksum " + checkSum + " not found to fetch input stream");
        case ok:
            break;
        }
        URI uri = found.getDownloadUri();
        log.info("found {} for checksum {}", uri, checkSum);
        return rest().createResource(uri).GET(InputStream.class);
    }
}
