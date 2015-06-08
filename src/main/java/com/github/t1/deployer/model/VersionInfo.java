package com.github.t1.deployer.model;

import javax.xml.bind.annotation.*;

import lombok.*;

@Value
@RequiredArgsConstructor
@XmlRootElement(name = "version")
@XmlAccessorType(XmlAccessType.NONE)
public class VersionInfo {
    @XmlValue
    Version version;
    @XmlAttribute
    CheckSum checkSum;

    @SuppressWarnings("unused")
    private VersionInfo() {
        this.version = null;
        this.checkSum = null;
    }
}
