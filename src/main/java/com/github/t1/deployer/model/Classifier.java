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
public class Classifier {
    public static Classifier of(String classifier) { return (classifier == null) ? null : new Classifier(classifier); }

    @NonNull String value;

    @JsonCreator(mode = DELEGATING) public Classifier(@NonNull String value) { this.value = value; }

    @Override public String toString() { return value; }
}
