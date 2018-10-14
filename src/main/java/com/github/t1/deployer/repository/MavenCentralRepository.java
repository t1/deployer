package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.ArtifactType;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.Classifier;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.Version;
import com.github.t1.rest.EntityResponse;
import com.github.t1.rest.RestContext;
import com.github.t1.rest.RestResource;
import com.github.t1.rest.UriTemplate;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@RequiredArgsConstructor
class MavenCentralRepository extends Repository {
    @Data
    public static class MavenCentralSearchResult {
        MavenCentralSearchResponse response;
    }

    @Data
    private static class MavenCentralSearchResponse {
        List<MavenCentralSearchResponseDocs> docs;
    }

    @Data
    private static class MavenCentralSearchResponseDocs {
        String g;
        String a;
        String v;
        String p;
    }

    private final RestContext rest;

    @Override public Artifact searchByChecksum(Checksum checksum) {
        MavenCentralSearchResult result = rest
                .createResource(searchUri())
                .with("checksum", checksum)
                .GET(MavenCentralSearchResult.class);

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

        //noinspection resource
        return Artifact.builder()
                       .groupId(groupId)
                       .artifactId(artifactId)
                       .version(version)
                       .type(type)
                       .checksum(checksum)
                       .inputStreamSupplier(() -> download(groupId, artifactId, version, type))
                       .build();
    }

    public UriTemplate searchUri() {
        return rest.nonQueryUri("repository")
                   .path("solrsearch")
                   .path("select")
                   .query("q", "1:{checksum}")
                   .query("rows", "20")
                   .query("wt", "json");
    }

    @Override
    protected Artifact lookupArtifact(
            @NonNull GroupId groupId,
            @NonNull ArtifactId artifactId,
            @NonNull Version version,
            @NonNull ArtifactType type,
            Classifier classifier) {
        //noinspection resource
        return Artifact.builder()
                       .groupId(groupId)
                       .artifactId(artifactId)
                       .version(version)
                       .type(type)
                       .classifier(classifier)
                       .checksumSupplier(() -> downloadChecksum(groupId, artifactId, version, type))
                       .inputStreamSupplier(() -> download(groupId, artifactId, version, type))
                       .build();
    }

    @Override public List<Version> listVersions(GroupId groupId, ArtifactId artifactId, boolean snapshot) {
        UriTemplate.Query query = rest.nonQueryUri("repository")
                                      .path("solrsearch")
                                      .path("select")
                                      .query("q", "g:{group-id}+AND+a:{artifact-id}")
                                      .query("core", "gav")
                                      .query("rows", "10000")
                                      .query("wt", "json");
        MavenCentralSearchResult result = rest
                .createResource(query)
                .with("group-id", groupId)
                .with("artifact-id", artifactId)
                .GET(MavenCentralSearchResult.class);
        return result
                .response
                .docs
                .stream()
                .map(doc -> new Version(doc.v))
                .sorted()
                .collect(toList());
    }

    private Checksum downloadChecksum(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        EntityResponse<String> response = resource(downloadPath(groupId, artifactId, version, type) + ".sha1")
                .accept(String.class).GET_Response();
        if (response.status() == NOT_FOUND)
            throw notFound("artifact not in repository: " + groupId + ":" + artifactId + ":" + version + ":" + type);
        return Checksum.fromString(response.expecting(OK).getBody());
    }

    private InputStream download(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        RestResource resource = resource(downloadPath(groupId, artifactId, version, type));
        log.debug("download from {}", resource);
        EntityResponse<InputStream> response = resource.GET_Response(InputStream.class);
        if (!response.status().equals(OK))
            throw builderFor(BAD_GATEWAY)
                    .title("can't download " + groupId + ":" + artifactId + ":" + version + ":" + type)
                    .detail("received " + response.status().getStatusCode() + " " + response.status().getReasonPhrase()
                            + " from " + resource.uri() + "\n"
                            + "body: " + response.getBody(String.class)).build();
        return response.getBody();
    }

    private RestResource resource(Object filepath) {
        return rest.createResource(downloadUri()).with("filepath", filepath);
    }

    private Path downloadPath(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        return groupId.asPath()
                      .resolve(artifactId.getValue())
                      .resolve(version.getValue())
                      .resolve(artifactId + "-" + version + "." + type.extension());
    }

    public UriTemplate downloadUri() {
        return rest.nonQueryUri("repository").path("remotecontent").query("filepath", "{filepath}");
    }
}
