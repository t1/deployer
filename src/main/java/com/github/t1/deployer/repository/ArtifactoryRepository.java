package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import static com.github.t1.problem.WebException.*;
import static javax.ws.rs.core.Response.Status.*;

@Slf4j
@RequiredArgsConstructor
public class ArtifactoryRepository extends Repository {
    public static Artifact artifactFromArtifactoryUri(Checksum checksum, URI uri) {
        Path path = Paths.get(uri.getPath());
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

    public static GroupId groupIdFrom(Path path) {
        String string = path.subpath(4, path.getNameCount() - 3).toString().replace("/", ".");
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

    @NonNull private final RestContext rest;
    @NonNull private final String repositorySnapshots;
    @NonNull private final String repositoryReleases;

    /**
     * It's not really nice to get the version out of the repo path, but where else would I get it? Even with the
     * <code>X-Result-Detail</code> header, it's not provided.
     */
    @Override
    public Artifact searchByChecksum(Checksum checksum) {
        log.debug("searchByChecksum({})", checksum);
        if (checksum == null || checksum.isEmpty())
            throw new IllegalArgumentException("empty or null checksum");
        URI uri = findUriFor(checksum);
        log.debug("got uri {}", uri);
        Artifact artifact = artifactFromArtifactoryUri(checksum, uri);
        log.debug("found {}", artifact);
        return artifact;
    }

    private URI findUriFor(Checksum checksum) {
        List<ChecksumSearchResultItem> results = searchByChecksumResults(checksum);
        switch (results.size()) {
        case 0:
            log.debug("not found: {}", checksum);
            throw new UnknownChecksumException(checksum);
        case 1:
            ChecksumSearchResultItem item = results.get(0);
            log.debug("got {}", item);
            return item.getUri();
        default:
            log.error("checksum not unique in repository: '{}'", checksum);
            throw new RuntimeException("checksum not unique in repository: '" + checksum + "'");
        }
    }

    private List<ChecksumSearchResultItem> searchByChecksumResults(Checksum checksum) {
        RestRequest<ChecksumSearchResult> request = searchByChecksumRequest();
        try {
            return request.with("checkSum", checksum.hexString()).GET().getResults();
        } catch (RuntimeException e) {
            log.error("can't search by checksum [" + checksum + "] in " + request, e);
            throw new ErrorWhileFetchingChecksumException(checksum);
        }
    }

    private RestRequest<ChecksumSearchResult> searchByChecksumRequest() {
        UriTemplate uri = rest
                .nonQueryUri("repository")
                .path("api/search/checksum")
                .query("sha1", "{checkSum}");
        return rest
                .createResource(uri)
                .header("X-Result-Detail", "info")
                .accept(ChecksumSearchResult.class);
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
    public Artifact lookupArtifact(
            @NonNull GroupId groupId,
            @NonNull ArtifactId artifactId,
            @NonNull Version version,
            @NonNull ArtifactType type) {
        UriTemplate template = rest
                .nonQueryUri("repository")
                .path("api/storage/{repoKey}/{*orgPath}/{module}/{baseRev}/{module}-{baseRev}.{ext}");
        UriTemplate uri = template
                .with("repoKey", version.isSnapshot() ? repositorySnapshots : repositoryReleases)
                .with("org", groupId)
                .with("orgPath", groupId.asPath())
                .with("baseRev", version)
                .with("module", artifactId)
                // (-{folderItegRev})
                // (-{fileItegRev})
                // (-{classifier})
                .with("ext", type)
                .with("type", type)
                // customTokenName<customTokenRegex>
                ;
        log.debug("fetch artifact info from {}", uri);
        EntityResponse<FileInfo> response = rest.createResource(uri).GET_Response(FileInfo.class);
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
            throw notFound("artifact not in repository: " + groupId + ":" + artifactId + ":" + version + ":" + type);
        default:
            throw new UnexpectedStatusException(response.status(), response.headers(), OK);
        }
    }

    private InputStream download(FileInfo fileInfo) {
        URI uri = fileInfo.getDownloadUri();
        if (uri == null)
            throw new RuntimeException("no download uri from repository for " + fileInfo.getUri());
        return rest.createResource(uri).GET(InputStream.class);
    }
}
