package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import static com.github.t1.deployer.repository.ArtifactoryRepository.SearchResult.*;
import static com.github.t1.deployer.repository.ArtifactoryRepository.SearchResultStatus.*;
import static com.github.t1.rest.RestContext.*;
import static javax.ws.rs.core.Response.Status.*;

@Slf4j
public class ArtifactoryRepository extends Repository {
    public static GroupId groupIdFrom(Path path) {
        String string = path.subpath(5, path.getNameCount() - 3).toString().replace("/", ".");
        return new GroupId(string);
    }

    public static ArtifactId artifactIdFrom(Path path) {
        String string = element(-3, path);
        return new ArtifactId(string);
    }

    public static Version versionFrom(Path path) {
        String string = element(-2, path);
        return new Version(string);
    }

    public static ArtifactType typeFrom(Path path) {
        String pathString = path.toString();
        String typeString = pathString.substring(pathString.lastIndexOf('.') + 1);
        return ArtifactType.valueOf(typeString);
    }

    private static String element(int n, Path path) {
        if (n < 0)
            n += path.getNameCount();
        return path.getName(n).toString();
    }

    private URI artifactory;

    private String artifactoryUserName;

    private Password artifactoryPassword;

    private RestContext restContext;

    private RestContext rest() {
        if (restContext == null) {
            restContext = REST.register("repository", artifactory);
            if (artifactoryUserName != null && artifactoryPassword != null) {
                log.debug("put {} credentials for {}", artifactoryUserName, artifactory);
                Credentials credentials = new Credentials(artifactoryUserName, artifactoryPassword.getValue());
                restContext = restContext.register(artifactory, credentials);
            }
        }
        return restContext;
    }

    ArtifactoryRepository() {
        // TODO make configurable:
        this.artifactory = URI.create("http://localhost:8081/artifactory");
        // this.artifactory = URI.create("https://artifactory.1and1.org/artifactory");
        // this.artifactoryUserName = "xxx";
        // this.artifactoryPassword = new Password("xxx");
    }

    ArtifactoryRepository(RestContext restContext) {
        this.restContext = restContext;
    }

    /**
     * It's not really nice to get the version out of the repo path, but where else would I get it? Even with the
     * <code>X-Result-Detail</code> header, it's not provided.
     */
    @Override
    public Artifact getByChecksum(Checksum checksum) {
        check(checksum);
        SearchResult result = searchByChecksum(checksum);
        switch (result.getStatus()) {
        case error:
            throw new RuntimeException("error while searching for checksum: '" + checksum + "'");
        case unknown:
            throw new RuntimeException("unknown checksum: '" + checksum + "'");
        case ok:
            break;
        }
        URI uri = result.getUri();
        log.debug("got uri {}", uri);
        Path path = path(uri);
        return artifact(checksum, path);
    }

    private void check(Checksum checksum) {
        if (checksum == null || checksum.isEmpty())
            throw new IllegalArgumentException("empty or null checksum");
    }

    private SearchResult searchByChecksum(Checksum checksum) {
        RestRequest<ChecksumSearchResult> request = searchByChecksumRequest();
        try {
            log.debug("searchByChecksum({})", checksum);
            List<ChecksumSearchResultItem> results = request.with("checkSum", checksum.hexString()).GET().getResults();
            if (results.size() == 0)
                return searchResult().status(SearchResultStatus.unknown).build();
            if (results.size() > 1)
                throw new RuntimeException("checksum not unique in repository: " + checksum);
            ChecksumSearchResultItem item = results.get(0);
            log.debug("got {}", item);
            return searchResult().status(ok).uri(item.getUri()).downloadUri(item.getDownloadUri()).build();
        } catch (RuntimeException e) {
            log.error("can't search by checksum [" + checksum + "] in " + request, e);
            return searchResult().status(error).build();
        }
    }

    private RestRequest<ChecksumSearchResult> searchByChecksumRequest() {
        UriTemplate uri = rest()
                .nonQueryUri("repository")
                .path("api/search/checksum")
                .query("sha1", "{checkSum}");
        RestRequest<ChecksumSearchResult> searchByChecksum = rest()
                .createResource(uri)
                .header("X-Result-Detail", "info")
                .accept(ChecksumSearchResult.class);
        log.debug("configured searchByChecksum request: {}", searchByChecksum);
        return searchByChecksum;
    }

    enum SearchResultStatus {
        ok,
        unknown,
        error
    }

    @lombok.Value
    @lombok.Builder(builderMethodName = "searchResult")
    static class SearchResult {
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

    private Artifact artifact(Checksum checksum, Path path) {
        return Artifact
                .builder()
                .groupId(groupIdFrom(path))
                .artifactId(artifactIdFrom(path))
                .version(versionFrom(path))
                .type(typeFrom(path))
                .checksum(checksum)
                .inputStreamSupplier(() -> {
                    throw new RuntimeException("already downloaded?");
                })
                .build();
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @VendorType("org.jfrog.artifactory.storage.FileInfo")
    private static class FileInfo {
        boolean folder;
        URI uri, downloadUri;
        Map<String, Checksum> checksums;

        public Checksum getChecksum() {
            return checksums.get("sha1");
        }
    }

    @Override
    public Artifact lookupArtifact(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        UriTemplate template = rest()
                .nonQueryUri("repository")
                .path("api/storage/{repoKey}/{*orgPath}/{module}/{baseRev}/{module}-{baseRev}.{ext}");
        UriTemplate uri = template
                .with("repoKey", "remote-repos") // TODO make configurable
                .with("org", groupId)
                .with("orgPath", groupId.asPath())
                .with("baseRev", (version == null) ? "" : version)
                .with("module", artifactId)
                // (-{folderItegRev})
                // (-{fileItegRev})
                // (-{classifier})
                .with("ext", type)
                .with("type", type)
                // customTokenName<customTokenRegex>
                ;
        log.debug("fetch artifact info from {}", uri);
        EntityResponse<FileInfo> response = rest().createResource(uri).GET_Response(FileInfo.class);
        switch ((Status) response.status()) {
        case OK:
            FileInfo fileInfo = response.getBody();
            log.debug("found artifact info: {}", fileInfo);
            return Artifact
                    .builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .type(type)
                    .version(version)
                    .checksum(fileInfo.getChecksum())
                    .inputStreamSupplier(() -> download(fileInfo))
                    .build();
        case NOT_FOUND:
            throw new WebApplicationException(Response
                    .status(NOT_FOUND)
                    .entity("artifact not in repository: " + groupId + ":" + artifactId + ":" + version + ":" + type)
                    .build());
        default:
            throw new UnexpectedStatusException(response.status(), response.headers(), OK);
        }
    }

    private InputStream download(FileInfo fileInfo) {
        URI uri = fileInfo.getDownloadUri();
        if (uri == null)
            throw new RuntimeException("no download uri from repository for " + fileInfo.getUri());
        return rest().createResource(uri).GET(InputStream.class);
    }
}
