package com.github.t1.deployer;

import static javax.xml.bind.annotation.XmlAccessType.*;

import javax.xml.bind.annotation.*;

import lombok.*;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@XmlRootElement
@XmlAccessorType(FIELD)
public class Deployment {
    private final DeploymentName name;
    private final ContextRoot contextRoot;
    private final CheckSum checkSum;

    private Version version;

    /** required by JAXB, etc. */
    @SuppressWarnings("unused")
    private Deployment() {
        this.name = null;
        this.contextRoot = null;
        this.checkSum = null;
    }
}
