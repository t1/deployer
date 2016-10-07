package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.*;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import static com.github.t1.problem.WebException.*;
import static javax.ws.rs.core.Response.Status.*;
import static javax.xml.bind.annotation.XmlAccessType.*;

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

    @Data
    @XmlRootElement(name = "metadata")
    static class MavenMetadata {
        private Versioning versioning;

        @Data
        @XmlAccessorType(FIELD)
        static class Versioning {
            @XmlElementWrapper
            @XmlElement(name = "snapshotVersion")
            private List<SnapshotVersion> snapshotVersions;

            @Data
            static class SnapshotVersion {
                private String extension;
                private String value;
                private String updated;
            }
        }
    }

    @Override
    public Artifact lookupArtifact(
            @NonNull GroupId groupId,
            @NonNull ArtifactId artifactId,
            @NonNull Version version,
            @NonNull ArtifactType type,
            Classifier classifier) {
        String fileName = getFileName(groupId, artifactId, version, type, classifier);
        FileInfo fileInfo = fetch(rest.nonQueryUri("repository"),
                "api/storage/{repoKey}/{*orgPath}/{module}/{baseRev}/" + fileName,
                FileInfo.class, groupId, artifactId, version, type);
        return Artifact
                .builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .type(type)
                .version(version)
                .checksum(fileInfo.getChecksum())
                .inputStreamSupplier(() -> download(fileInfo))
                .build();
    }

    private String getFileName(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type,
            Classifier classifier) {
        String classifierSuffix = (classifier == null) ? "" : "-" + classifier;
        if (version.isSnapshot()) {
            MavenMetadata metadata = fetch(rest.nonQueryUri("repository"),
                    "{repoKey}/{*orgPath}/{module}/{baseRev}/maven-metadata.xml",
                    MavenMetadata.class, groupId, artifactId, version, type);
            String snapshot = metadata
                    .getVersioning().getSnapshotVersions().stream()
                    .filter(snapshotVersion -> type.extension().equals(snapshotVersion.getExtension()))
                    .map(MavenMetadata.Versioning.SnapshotVersion::getValue)
                    .findAny()
                    .orElseThrow(() -> notFound("no metadata for extension [" + type.extension() + "]"));
            return artifactId + "-" + snapshot + classifierSuffix + "." + type.extension();
        } else {
            return "{module}-{baseRev}" + classifierSuffix + ".{ext}";
        }
    }

    private <T> T fetch(UriTemplate base, String path, Class<T> type,
            GroupId groupId, ArtifactId artifactId, Version version, ArtifactType artifactType) {
        UriTemplate uri = resolve(base.nonQuery().path(path), groupId, artifactId, version, artifactType);
        log.debug("fetch {} from {}", type.getSimpleName(), uri);
        EntityResponse<T> response = rest.createResource(uri).GET_Response(type);
        checkStatus(response, type.getSimpleName()
                + " for " + groupId + ":" + artifactId + ":" + version + ":" + artifactType);
        T result = response.getBody();
        log.debug("found {}: {}", type.getSimpleName(), result);
        return result;
    }

    private UriTemplate resolve(UriTemplate template,
            GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        return template
                .with("repoKey", version.isSnapshot() ? repositorySnapshots : repositoryReleases)
                .with("org", groupId)
                .with("orgPath", groupId.asPath())
                .with("baseRev", version)
                .with("module", artifactId)
                // (-{folderItegRev})
                // (-{fileItegRev})
                // (-{classifier})
                .with("ext", type)
                .with("type", type);
    }

    private void checkStatus(EntityResponse<?> response, String what) {
        switch ((Status) response.status()) {
        case OK:
            return;
        case NOT_FOUND:
            throw notFound("not in repository: " + what);
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
