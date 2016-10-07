package com.github.t1.deployer.model;

import lombok.*;

import java.io.*;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.*;
import static lombok.AccessLevel.*;

@Builder
@Getter
@AllArgsConstructor(access = PRIVATE)
public class Artifact {
    @NonNull private final GroupId groupId;
    @NonNull private final ArtifactId artifactId;
    @NonNull private final Version version;
    @NonNull private final ArtifactType type;

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
                + ((checksum == null) ? "" : "=" + checksum);
    }
}
