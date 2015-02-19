package com.github.t1.deployer;

import static javax.xml.bind.annotation.XmlAccessType.*;

import java.io.*;

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

    public void deploy(Container container, Repository repository) {
        try (InputStream inputStream = repository.getArtifactInputStream(checkSum)) {
            container.deploy(name, inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
