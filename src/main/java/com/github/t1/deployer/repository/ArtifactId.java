package com.github.t1.deployer.repository;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor(onConstructor = @__({ @JsonCreator }))
public class ArtifactId {
    String value;

    @Override public String toString() { return value; }
}
