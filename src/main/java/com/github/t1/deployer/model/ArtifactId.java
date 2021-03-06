package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static lombok.AccessLevel.PRIVATE;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonSerialize(using = ToStringSerializer.class)
public class ArtifactId {
    public static ArtifactId of(String value) { return (value == null) ? null : new ArtifactId(value);}

    @NonNull String value;

    @JsonCreator(mode = DELEGATING) public ArtifactId(@NonNull String value) { this.value = value; }

    @Override public String toString() { return value; }
}
