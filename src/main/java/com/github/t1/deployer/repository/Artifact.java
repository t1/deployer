package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import lombok.*;

import java.io.*;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.*;

@Builder
@Getter
@RequiredArgsConstructor
public class Artifact {
    @NonNull private final GroupId groupId;
    @NonNull private final ArtifactId artifactId;
    @NonNull private final Version version;
    @NonNull private final ArtifactType type;

    @NonNull private final CheckSum sha1;

    @NonNull private final Supplier<InputStream> inputStreamSupplier;

    public InputStream getInputStream() {
        return inputStreamSupplier.get();
    }

    public Reader getReader() { return new InputStreamReader(getInputStream(), UTF_8); }

    @Override public String toString() { return groupId + ":" + artifactId + ":" + version + "=" + sha1;}
}
