package com.github.t1.deployer;

import static javax.xml.bind.annotation.XmlAccessType.*;

import javax.xml.bind.annotation.*;

import lombok.*;

@Data
@RequiredArgsConstructor
@XmlRootElement
@XmlAccessorType(FIELD)
public class Deployment {
    private final String name;
    private final String contextRoot;
    private final String hash;

    private String version;

    /** required by JAXB, etc. */
    @SuppressWarnings("unused")
    private Deployment() {
        this.name = null;
        this.contextRoot = null;
        this.hash = null;
    }
}
