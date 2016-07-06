package com.github.t1.deployer.container;

import com.github.t1.deployer.model.Checksum;
import lombok.*;
import lombok.experimental.Wither;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.*;

import static javax.xml.bind.annotation.XmlAccessType.*;

/** This is the artifact returned by Artifactory as well as the deployment installed in the container. */
@Value
@Wither
@AllArgsConstructor
@NoArgsConstructor(force = true)
@XmlRootElement
@XmlAccessorType(FIELD)
public class Deployment implements Comparable<Deployment> {
    public static final String NEW_DEPLOYMENT_PATH = "!";
    private static final DeploymentName NEW_DEPLOYMENT_NAME = new DeploymentName(NEW_DEPLOYMENT_PATH);
    public static final Deployment NEW_DEPLOYMENT = new Deployment(NEW_DEPLOYMENT_NAME, null, null);

    DeploymentName name;

    ContextRoot contextRoot;

    Checksum checksum;

    @Override
    public int compareTo(@NotNull Deployment that) {
        return this.name.getValue().compareToIgnoreCase(that.name.getValue());
    }

    public boolean isNew() {
        return NEW_DEPLOYMENT_NAME.equals(name);
    }

    /** Is the name of the deployment equal to the context-root plus '.war'? */
    private boolean isDefaultName() {
        return name != null && name.getValue().equals(contextRoot + ".war");
    }

    @Override
    public String toString() {
        return (isDefaultName() ? "" : name + ":") + contextRoot;
    }
}
