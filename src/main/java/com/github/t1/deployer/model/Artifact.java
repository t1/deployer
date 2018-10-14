package com.github.t1.deployer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static lombok.AccessLevel.PRIVATE;

@Builder
@Getter
@AllArgsConstructor(access = PRIVATE)
public class Artifact {
    @NonNull private final GroupId groupId;
    @NonNull private final ArtifactId artifactId;
    @NonNull private final Version version;
    @NonNull private final ArtifactType type;
    private final String error;

    private final Classifier classifier;
    private Checksum checksum;
    private final Supplier<Checksum> checksumSupplier;
    @NonNull private final Supplier<InputStream> inputStreamSupplier;

    public Checksum getChecksumRaw() { return checksum; }

    public Checksum getChecksum() {
        if (checksum == null)
            checksum = checksumSupplier.get();
        return checksum;
    }

    public InputStream getInputStream() {
        return inputStreamSupplier.get();
    }

    public Reader getReader() { return new InputStreamReader(getInputStream(), UTF_8); }

    @Override public String toString() {
        return groupId + ":" + artifactId + ":" + version + ":" + type
                + ((classifier == null) ? "" : ":" + classifier)
                + ((checksum == null) ? "" : "=" + checksum)
                + ((error == null) ? "" : " ### " + error + " ###");
    }
}
