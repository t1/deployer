package com.github.t1.deployer.repository;

import com.github.t1.deployer.model.*;
import lombok.*;

import java.io.InputStream;
import java.util.function.Supplier;

@Builder
@Getter
@RequiredArgsConstructor
public class Artifact {
    @NonNull private final GroupId groupId;
    @NonNull private final ArtifactId artifactId;
    @NonNull private final Version version;

    @NonNull private final CheckSum sha1;

    @NonNull private final Supplier<InputStream> inputStreamSupplier;

    public InputStream getInputStream() {
        return inputStreamSupplier.get();
    }

    @Override public String toString() { return groupId + ":" + artifactId + ":" + version + "=" + sha1;}
}
