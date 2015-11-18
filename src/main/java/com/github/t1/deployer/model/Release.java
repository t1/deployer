package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import java.util.Comparator;

import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.*;
import lombok.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.NONE)
@ApiModel
public class Release implements Comparable<Release> {
    public static final Comparator<Release> BY_VERSION = Comparator.comparing(r -> (r == null) ? null : r.getVersion());

    @ApiModelProperty(example = "2.12.1")
    @NonNull
    @JsonProperty
    @XmlValue
    Version version;

    @ApiModelProperty(example = "E4D3BC23D706CFF1599359EC14F61EB7000082E0")
    @NonNull
    @JsonProperty
    @XmlAttribute
    CheckSum checkSum;

    @Override
    public int compareTo(Release that) {
        return BY_VERSION.compare(this, that);
    }
}
