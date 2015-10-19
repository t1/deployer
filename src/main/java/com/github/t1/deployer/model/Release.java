package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

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
public class Release {
    @JsonProperty
    @XmlValue
    Version version;

    @JsonProperty
    @XmlAttribute
    CheckSum checkSum;
}
