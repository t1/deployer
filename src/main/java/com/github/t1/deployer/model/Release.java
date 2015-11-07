package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import java.util.Comparator;

import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import lombok.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.NONE)
@ApiModel
public class Release implements Comparable<Release> {
    public static final Comparator<Release> BY_VERSION = Comparator.comparing(r -> (r == null) ? null : r.getVersion());

    @NonNull
    @JsonProperty
    @XmlValue
    Version version;

    @NonNull
    @JsonProperty
    @XmlAttribute
    CheckSum checkSum;

    @Override
    public int compareTo(Release that) {
        return BY_VERSION.compare(this, that);
    }
}
