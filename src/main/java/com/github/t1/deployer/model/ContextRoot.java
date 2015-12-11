package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import javax.xml.bind.annotation.XmlValue;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@JsonSerialize(using = ToStringSerializer.class)
public class ContextRoot {
    @NonNull
    @XmlValue
    String value;

    @Override
    public String toString() {
        return value;
    }
}
