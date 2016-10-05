package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonSerialize(using = ToStringSerializer.class)
public class ArtifactId {
    @NonNull String value;

    @JsonCreator public ArtifactId(@NonNull String value) { this.value = value; }

    @Override public String toString() { return value; }
}
