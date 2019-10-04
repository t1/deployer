package com.github.t1.deployer.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.ArtifactType;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.Classifier;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.Version;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static com.github.t1.problem.WebException.builderFor;
import static com.github.t1.problem.WebException.notFound;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Slf4j
public class MavenCentralRepository extends Repository {
    private final WebTarget searchUri;
    private final WebTarget listVersionsTemplate;
    private final WebTarget downloadUri;

    MavenCentralRepository(WebTarget baseTarget) {
        this.searchUri = baseTarget
            .path("solrsearch")
            .path("select")
            .queryParam("q", "1:{checksum}")
            .queryParam("rows", "20")
            .queryParam("wt", "json");
        this.listVersionsTemplate = baseTarget
            .path("solrsearch")
            .path("select")
            .queryParam("q", "g:{group-id} AND a:{artifact-id}")
            .queryParam("core", "gav")
            .queryParam("rows", "10000")
            .queryParam("wt", "json");
        this.downloadUri = baseTarget
            .path("remotecontent")
            .queryParam("filepath", "{filepath}");
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MavenCentralSearchResult {
        MavenCentralSearchResponse response;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MavenCentralSearchResponse {
        List<MavenCentralSearchResponseDocs> docs;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MavenCentralSearchResponseDocs {
        String g;
        String a;
        String v;
        String p;
    }

    @Override public Artifact searchByChecksum(Checksum checksum) {
        MavenCentralSearchResult result = searchUri
            .resolveTemplate("checksum", checksum)
            .request()
            .get(MavenCentralSearchResult.class);

        switch (result.response.docs.size()) {
            case 0:
                log.debug("not found: {}", checksum);
                throw new UnknownChecksumException(checksum);
            case 1:
                MavenCentralSearchResponseDocs doc = result.response.docs.get(0);
                log.debug("got {}", doc);
                return toArtifact(checksum, doc);
            default:
                throw new RuntimeException("checksum not unique in repository: '" + checksum + "'");
        }
    }

    private Artifact toArtifact(Checksum checksum, MavenCentralSearchResponseDocs doc) {
        GroupId groupId = new GroupId(doc.g);
        ArtifactId artifactId = new ArtifactId(doc.a);
        Version version = new Version(doc.v);
        ArtifactType type = ArtifactType.valueOf(doc.p);

        return new Artifact()
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setVersion(version)
            .setType(type)
            .setChecksum(checksum)
            .setInputStreamSupplier(() -> downloadArtifact(groupId, artifactId, version, type));
    }

    @Override
    protected Artifact lookupArtifact(
        @NonNull GroupId groupId,
        @NonNull ArtifactId artifactId,
        @NonNull Version version,
        @NonNull ArtifactType type,
        Classifier classifier) {
        return new Artifact()
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setVersion(version)
            .setType(type)
            .setClassifier(classifier)
            .setChecksumSupplier(() -> downloadChecksum(groupId, artifactId, version, type))
            .setInputStreamSupplier(() -> downloadArtifact(groupId, artifactId, version, type));
    }

    @Override public List<Version> listVersions(GroupId groupId, ArtifactId artifactId, boolean snapshot) {
        MavenCentralSearchResult result = listVersionsTemplate
            .resolveTemplate("group-id", groupId)
            .resolveTemplate("artifact-id", artifactId)
            .request()
            .get(MavenCentralSearchResult.class);
        return result
            .response
            .docs
            .stream()
            .map(doc -> new Version(doc.v))
            .sorted()
            .collect(toList());
    }

    private Checksum downloadChecksum(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        Response response = download(groupId, artifactId, version, type, ".sha1");
        return Checksum.fromString(response.readEntity(String.class));
    }

    private InputStream downloadArtifact(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        Response response = download(groupId, artifactId, version, type, "");
        return response.readEntity(InputStream.class);
    }

    private Response download(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type, String suffix) {
        Path downloadPath = groupId.asPath()
            .resolve(artifactId.getValue())
            .resolve(version.getValue())
            .resolve(artifactId + "-" + version + "." + type.extension() + suffix);
        WebTarget resource = downloadUri.resolveTemplate("filepath", downloadPath);
        log.debug("download from {}", resource);
        Response response = resource.request().get();
        if (response.getStatusInfo().equals(NOT_FOUND))
            throw notFound("artifact not in repository: " + groupId + ":" + artifactId + ":" + version + ":" + type);
        if (!response.getStatusInfo().equals(OK))
            throw builderFor(BAD_GATEWAY)
                .title("can't download " + groupId + ":" + artifactId + ":" + version + ":" + type)
                .detail("received " + response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase()
                    + " from " + resource.getUri() + "\n"
                    + "body: " + response.readEntity(String.class)).build();
        return response;
    }
}
