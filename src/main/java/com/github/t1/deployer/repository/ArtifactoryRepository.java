package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.ArtifactType;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.Classifier;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.Version;
import com.github.t1.deployer.tools.VendorType;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.github.t1.problem.WebException.notFound;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.xml.bind.annotation.XmlAccessType.FIELD;

@Slf4j
@RequiredArgsConstructor
public class ArtifactoryRepository extends Repository {
    @NonNull private final Client client;
    @NonNull private final URI artifactoryUri;
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
        Invocation.Builder request = client.target(artifactoryUri)
            .path("api/search/checksum")
            .queryParam("sha1", checksum.hexString())
            .request()
            .header("X-Result-Detail", "info");
        try {
            return request.get(ChecksumSearchResult.class).results;
        } catch (RuntimeException e) {
            log.error("can't search by checksum [" + checksum + "] in " + request, e);
            throw new ErrorWhileFetchingChecksumException(checksum);
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @VendorType("org.jfrog.artifactory.search.ChecksumSearchResult")
    public static class ChecksumSearchResult {
        List<ChecksumSearchResultItem> results;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class ChecksumSearchResultItem {
        URI uri;
        URI downloadUri;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @VendorType("org.jfrog.artifactory.storage.FileInfo")
    public static class FileInfo {
        boolean folder;
        URI uri, downloadUri;
        Map<String, String> checksums;

        public Checksum getSha1() { return Checksum.ofHexString(checksums.get("sha1")); }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @VendorType("org.jfrog.artifactory.storage.FolderInfo")
    public static class FolderInfo {
        List<FileInfo> children;
        URI uri;

        public List<FileInfo> getChildren() {
            return (children == null) ? emptyList() : children;
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
    protected Artifact lookupArtifact(
        @NonNull GroupId groupId,
        @NonNull ArtifactId artifactId,
        @NonNull Version version,
        @NonNull ArtifactType type,
        Classifier classifier) {
        String fileName = getFileName(groupId, artifactId, version, type, classifier);
        FileInfo fileInfo = fetch(API_STORAGE + "/{repoKey}/{orgPath}/{module}/{baseRev}/" + fileName,
            FileInfo.class, groupId, artifactId, version, type);
        //noinspection resource
        return new Artifact()
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setType(type)
            .setVersion(version)
            .setChecksum(fileInfo.getSha1())
            .setInputStreamSupplier(() -> download(fileInfo));
    }

    @Override public List<Version> listVersions(GroupId groupId, ArtifactId artifactId, boolean snapshot) {
        WebTarget resolvedTarget = client.target(artifactoryUri).path("api/storage/" + "{repoKey}/{orgPath}/{module}")
            .resolveTemplate("repoKey", snapshot ? repositorySnapshots : repositoryReleases)
            .resolveTemplate("orgPath", groupId.asPath())
            .resolveTemplate("module", artifactId);
        log.debug("fetch folder from {}", resolvedTarget.getUri());
        Response response = resolvedTarget.request(APPLICATION_JSON_TYPE).get();
        checkStatus(response, "folder for " + groupId + ":" + artifactId);
        FolderInfo fileInfo = response.readEntity(FolderInfo.class);
        log.debug("found folder: {}", fileInfo);
        List<Version> versions = fileInfo
            .getChildren()
            .stream()
            .filter(FileInfo::isFolder)
            .map(this::toVersion)
            .sorted()
            .collect(toList());
        log.debug("found versions: {}", versions);
        return versions;
    }

    private Version toVersion(FileInfo file) {
        String string = file.getUri().toString();
        if (string.length() > 0 && string.charAt(0) == '/')
            string = string.substring(1);
        return new Version(string);
    }

    private String getFileName(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type,
                               Classifier classifier) {
        String classifierSuffix = (classifier == null) ? "" : "-" + classifier;
        if (version.isSnapshot()) {
            MavenMetadata metadata = fetch("{repoKey}/{orgPath}/{module}/{baseRev}/maven-metadata.xml",
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

    private <T> T fetch(String path,
                        Class<T> type,
                        GroupId groupId,
                        ArtifactId artifactId,
                        Version version,
                        ArtifactType artifactType) {
        WebTarget target = client.target(artifactoryUri).path(path)
            .resolveTemplate("repoKey", version.isSnapshot() ? repositorySnapshots : repositoryReleases)
            .resolveTemplate("org", groupId)
            .resolveTemplate("orgPath", groupId.asPath())
            .resolveTemplate("baseRev", version)
            .resolveTemplate("module", artifactId)
            // (-{folderItegRev})
            // (-{fileItegRev})
            // (-{classifier})
            .resolveTemplate("ext", artifactType)
            .resolveTemplate("type", artifactType);
        MediaType mediaType = vendorTypeOf(type);
        log.debug("fetch {} {} from {}", mediaType, type.getSimpleName(), target.getUri());
        Response response = target.request(mediaType).get();
        checkStatus(response, type.getSimpleName()
            + " for " + groupId + ":" + artifactId + ":" + version + ":" + artifactType);
        T result = response.readEntity(type);
        log.debug("found {}: {}", type.getSimpleName(), result);
        return result;
    }

    private static <T> MediaType vendorTypeOf(Class<T> type) {
        VendorType vendorType = type.getAnnotation(VendorType.class);
        if (vendorType != null)
            return new MediaType("application", "vnd." + vendorType.value() + "+json");
        return type.isAnnotationPresent(XmlRootElement.class) ? APPLICATION_XML_TYPE : APPLICATION_JSON_TYPE;
    }

    private void checkStatus(Response response, String what) {
        switch (response.getStatus()) {
            case 200:
                return;
            case 404:
                throw notFound("not in repository: " + what);
            default:
                throw new RuntimeException("unexpected http status " + response.getStatusInfo());
        }
    }

    private InputStream download(FileInfo fileInfo) {
        URI uri = fileInfo.getDownloadUri();
        if (uri == null)
            throw new RuntimeException("no download uri from repository for " + fileInfo.getUri());
        log.debug("download {}", uri);
        return client.target(uri)
            .property("jersey.config.client.followRedirects", Boolean.TRUE)
            .request("application/java-archive")
            .get(InputStream.class);
    }

    static Artifact artifactFromArtifactoryUri(Checksum checksum, URI uri) {
        Path path = Paths.get(uri.getPath());
        return new Artifact()
            .setGroupId(groupIdFrom(path))
            .setArtifactId(artifactIdFrom(path))
            .setVersion(versionFrom(path))
            .setType(typeFrom(path))
            .setChecksum(checksum)
            .setInputStreamSupplier(() -> {
                throw new RuntimeException("already downloaded?");
            });
    }

    private static GroupId groupIdFrom(Path path) {
        int index = find(API_STORAGE, path);
        String string = path.subpath(index + API_STORAGE.getNameCount() + 1 // +1 for the repo name
            , path.getNameCount() - 3).toString().replace("/", ".");
        return new GroupId(string);
    }

    private static ArtifactId artifactIdFrom(Path path) {
        String string = element(-3, path);
        return new ArtifactId(string);
    }

    static Version versionFrom(Path path) {
        String string = element(-2, path);
        return new Version(string);
    }

    private static ArtifactType typeFrom(Path path) {
        String pathString = path.toString();
        String typeString = pathString.substring(pathString.lastIndexOf('.') + 1);
        return ArtifactType.valueOf(typeString);
    }

    private static int find(@SuppressWarnings("SameParameterValue") Path pattern, Path path) {
        int n = pattern.getNameCount();
        for (int i = 0; i < path.getNameCount() - n; i++)
            if (path.subpath(i, i + n).equals(pattern))
                return i;
        return -1;
    }

    private static String element(int n, Path path) {
        if (n < 0)
            n += path.getNameCount();
        return path.getName(n).toString();
    }

    private static final Path API_STORAGE = Paths.get("api/storage");
}
