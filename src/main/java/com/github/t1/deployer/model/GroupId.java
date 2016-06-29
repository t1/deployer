package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.nio.file.Paths;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor(onConstructor = @__({ @JsonCreator }))
@JsonSerialize(using = ToStringSerializer.class)
public class GroupId {
    String value;

    @Override public String toString() { return value; }

    public java.nio.file.Path asPath() {
        return Paths.get(value.replace('.', '/'));
    }
}
