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

        return Artifact.builder()
                       .groupId(new GroupId(doc.g))
                       .artifactId(new ArtifactId(doc.a))
                       .version(new Version(doc.v))
                       .type(ArtifactType.valueOf(doc.p))
                       .checksum(checksum)
                       .inputStreamSupplier(() -> null) // TODO
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
                       .checksumSupplier(() -> null) // TODO
                       .inputStreamSupplier(() -> download(groupId, artifactId, version, type))
                       .build();
    }

    private InputStream download(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        Path filepath = groupId.asPath()
                               .resolve(artifactId.getValue())
                               .resolve(version.getValue())
                               .resolve(artifactId + "-" + version + "." + type.extension());
        return rest.createResource(downloadUri())
                   .with("filepath", filepath)
                   .GET(InputStream.class);
    }

    public UriTemplate downloadUri() {
        return rest.nonQueryUri("repository").path("remotecontent").query("filepath", "{filepath}");
    }
}
