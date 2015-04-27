package com.github.t1.deployer.repository;

import static java.util.Collections.*;
import static javax.ws.rs.core.Response.Status.*;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriBuilder;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.auth.Credentials;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import com.github.t1.rest.UriTemplate.UriScheme;

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

    @Inject
    @Artifactory
    URI baseUri;

    @Inject
    @Artifactory
    Credentials credentials;

    RestResource artifactory;

    @PostConstruct
    void init() {
        UriTemplate template = UriScheme.of(baseUri).authority(baseUri.getAuthority()).path(baseUri.getPath());
        RestResource resource = new RestResource(template);
        this.artifactory = resource;
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
    @VendorType("org.jfrog.artifactory.search.ChecksumSearchResult")
    private static class ChecksumSearchResult {
        List<ChecksumSearchResultItem> results;
    }

    @Data
    @NoArgsConstructor
    private static class ChecksumSearchResultItem {
        URI uri;
        URI downloadUri;
    }

    private ChecksumSearchResultItem searchByChecksum(CheckSum checkSum) {
        List<ChecksumSearchResultItem> results = authenticated( //
                artifactory //
                        .path("api/search/checksum") //
                        .query("sha1", checkSum.hexString()) //
                        .header("X-Result-Detail", "info") //
                ) //
                .get(ChecksumSearchResult.class) //
                        .getResults();
        if (results.size() == 0)
            return null;
        if (results.size() > 1)
            throw new RuntimeException("checksum not unique in repository: " + checkSum);
        ChecksumSearchResultItem result = results.get(0);
        log.debug("got {}", result);
        return result;
    }

    private RestRequest authenticated(RestRequest request) {
        if (credentials != null && request.authority().equals(artifactory.authority()))
            request = request.basicAuth(credentials.getUserPrincipal().getName(), credentials.getPassword());
        return request;
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

    @Data
    @NoArgsConstructor
    @VendorType("org.jfrog.artifactory.storage.FolderInfo")
    private static class FolderInfo {
        List<FileInfo> children;
        URI uri;

        public List<FileInfo> getChildren() {
            return (children == null) ? Collections.<FileInfo> emptyList() : children;
        }
    }

    @Data
    @NoArgsConstructor
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
    public List<Deployment> availableVersionsFor(CheckSum checkSum) {
        ChecksumSearchResultItem deployment = searchByChecksum(checkSum);
        if (deployment == null)
            return emptyList();
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
        // TODO we assume the path of files anyways... so we could reduce the number of requests and not recurse fully
        log.trace("get deployments in {} (fileName: {})", uri, fileName);
        // TODO eventually it would be more efficient to use the Artifactory Pro feature 'List File':
        // /api/storage/{repoKey}/{folder-path}?list[&deep=0/1][&depth=n][&listFolders=0/1][&mdTimestamps=0/1][&includeRootPath=0/1]
        FolderInfo folderInfo = authenticated(new RestResource(uri).request()).get(FolderInfo.class);
        log.trace("got {}", folderInfo);
        return deploymentsIn(fileName, folderInfo);
    }

    private List<Deployment> deploymentsIn(String fileName, FolderInfo folderInfo) {
        URI root = folderInfo.getUri();
        List<Deployment> result = new ArrayList<>();
        for (FileInfo child : folderInfo.getChildren()) {
            URI uri = UriBuilder.fromUri(root).path(child.getUri().toString()).build();
            if (child.isFolder()) {
                result.addAll(deploymentsIn(fileName, uri));
            } else {
                log.trace("get deployment in {} (fileName: {})", uri, fileName);
                Deployment deployment = deploymentIn(uri);
                if (deployment.getName().getValue().equals(fileName)) {
                    result.add(deployment);
                }
            }
        }
        return result;
    }

    private Deployment deploymentIn(URI uri) {
        FileInfo file = authenticated(new RestResource(uri).request()).get(FileInfo.class);
        return file.deployment();
    }

    @Override
    public InputStream getArtifactInputStream(CheckSum checkSum) {
        ChecksumSearchResultItem found = searchByChecksum(checkSum);
        if (found == null)
            throw new WebApplicationException(NOT_FOUND);
        URI uri = found.getDownloadUri();
        log.info("found {} for checksum {}", uri, checkSum);
        return authenticated(new RestResource(uri).request()).accept(InputStream.class).get();
    }
}
