package com.github.t1.deployer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static lombok.AccessLevel.PRIVATE;

@Getter @Setter @Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class Artifact {
    private GroupId groupId;
    private ArtifactId artifactId;
    private Version version;
    private ArtifactType type;
    private String error;

    private Classifier classifier;
    private Checksum checksum;
    private Supplier<Checksum> checksumSupplier;
    private Supplier<InputStream> inputStreamSupplier;

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
