package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import com.github.t1.rest.*;
import lombok.Data;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import java.util.List;

import static com.github.t1.rest.UriTemplate.CommonScheme.*;

@Vetoed
public class MavenCentralRepository extends Repository {
    public static final UriTemplate QUERY =
            http.host("search.maven.org")
                .path("solrsearch")
                .path("select")
                .query("q", "1:{checksum}")
                .query("rows", "20")
                .query("wt", "json");

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

    @Inject RestContext rest;

    @Override public Artifact getByChecksum(Checksum checksum) {
        MavenCentralSearchResult result = rest
                .createResource(QUERY)
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

    @Override
    public Artifact lookupArtifact(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        return null;
    }
}
