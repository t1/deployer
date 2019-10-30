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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static com.github.t1.problem.WebException.builderFor;
import static com.github.t1.problem.WebException.notFound;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;

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
            .path("remotecontent");
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
        InputStream response = download(groupId, artifactId, version, type, ".sha1");
        return Checksum.fromString(read(response));
    }

    private InputStream downloadArtifact(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type) {
        return download(groupId, artifactId, version, type, "");
    }

    private InputStream download(GroupId groupId, ArtifactId artifactId, Version version, ArtifactType type, String suffix) {
        Path downloadPath = groupId.asPath()
            .resolve(artifactId.getValue())
            .resolve(version.getValue())
            .resolve(artifactId + "-" + version + "." + type.extension() + suffix);
        // Don't use JAX-RS here, because Maven Central requires (theoretically invalid) unencoded slashes in the query param
        URI uri = URI.create(downloadUri.getUri() + "?filepath=" + downloadPath);
        log.debug("download from {}", uri);
        try {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            switch (connection.getResponseCode()) {
                case 404:
                    throw notFound("artifact not in repository: " + groupId + ":" + artifactId + ":" + version + ":" + type);
                case 200:
                    return connection.getInputStream();
                default:
                    throw builderFor(BAD_GATEWAY)
                        .title("can't download " + groupId + ":" + artifactId + ":" + version + ":" + type)
                        .detail("received " + connection.getResponseCode() + " " + connection.getResponseMessage()
                            + " from " + uri + "\n"
                            + "body: " + read(connection.getErrorStream())).build();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("can't construct URL from " + uri);
        } catch (IOException e) {
            throw new RuntimeException("can't connect to / read from " + uri);
        }
    }

    private String read(InputStream stream) {
        try (Scanner scanner = new Scanner(stream).useDelimiter("\\Z")) {
            return scanner.next();
        }
    }
}
