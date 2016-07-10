package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import lombok.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
public class MavenCentralRepository extends Repository {
    @Data
    private static class MavenCentralSearchResult {
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

    @Override public Artifact getByChecksum(Checksum checksum) {
        MavenCentralSearchResult result = rest
                .createResource(searchUri())
                .with("checksum", checksum)
                .GET(MavenCentralSearchResult.class);

        assert result.response.docs.size() == 1;
        MavenCentralSearchResponseDocs doc = result.response.docs.get(0);

        GroupId groupId = new GroupId(doc.g);
        ArtifactId artifactId = new ArtifactId(doc.a);
        Version version = new Version(doc.v);
        ArtifactType type = ArtifactType.valueOf(doc.p);

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
    public Artifact lookupArtifact(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        return Artifact.builder()
                       .groupId(groupId)
                       .artifactId(artifactId)
                       .version(version)
                       .type(type)
                       .checksumSupplier(() -> downloadChecksum(groupId, artifactId, version, type))
                       .inputStreamSupplier(() -> download(groupId, artifactId, version, type))
                       .build();
    }

    private Checksum downloadChecksum(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        String checksum = resource(downloadPath(groupId, artifactId, version, type) + ".sha1").GET(String.class);
        return Checksum.fromString(checksum);
    }

    private InputStream download(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        return resource(downloadPath(groupId, artifactId, version, type)).GET(InputStream.class);
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
