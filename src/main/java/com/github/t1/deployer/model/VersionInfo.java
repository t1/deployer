package com.github.t1.deployer.model;

import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import lombok.*;

@Value
@RequiredArgsConstructor
@XmlRootElement(name = "version")
@XmlAccessorType(XmlAccessType.NONE)
@ApiModel
public class VersionInfo {
    @JsonProperty
    @XmlValue
    Version version;

    @JsonProperty
    @XmlAttribute
    CheckSum checkSum;

    @SuppressWarnings("unused")
    private VersionInfo() {
        this.version = null;
        this.checkSum = null;
    }
}
