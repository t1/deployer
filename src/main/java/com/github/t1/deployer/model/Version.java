package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@XmlAccessorType(XmlAccessType.NONE)
@JsonSerialize(using = ToStringSerializer.class)
public class Version {
    @NonNull
    @XmlValue
    private String version;

    @Override
    public String toString() {
        return version;
    }
}
