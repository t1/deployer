package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.nio.file.*;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonSerialize(using = ToStringSerializer.class)
public class GroupId {
    String value;

    @JsonCreator public GroupId(String value) { this.value = value; }

    @Override public String toString() { return value; }

    public Path asPath() { return Paths.get(value.replace('.', '/')); }
}
