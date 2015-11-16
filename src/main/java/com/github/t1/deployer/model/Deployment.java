package com.github.t1.deployer.model;

import static javax.xml.bind.annotation.XmlAccessType.*;

import java.util.List;

import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.*;

import io.swagger.annotations.*;
import lombok.*;
import lombok.experimental.Wither;

/** This is the artifact returned by Artifactory as well as the deployment installed in the container. */
@Value
@Wither
@AllArgsConstructor
@NoArgsConstructor(force = true)
@XmlRootElement
@XmlAccessorType(FIELD)
@ApiModel
public class Deployment implements Comparable<Deployment> {
    public static final String NEW_DEPLOYMENT_PATH = "!";
    private static final DeploymentName NEW_DEPLOYMENT_NAME = new DeploymentName(NEW_DEPLOYMENT_PATH);
    public static final Deployment NEW_DEPLOYMENT = new Deployment(NEW_DEPLOYMENT_NAME, null, null, null, null);

    @JsonProperty
    DeploymentName name;

    @JsonProperty
    ContextRoot contextRoot;

    @JsonProperty
    CheckSum checkSum;

    @ApiModelProperty("The version currently deployed")
    @JsonProperty
    Version version;

    @ApiModelProperty("The list of available releases for this artifact")
    @JsonProperty
    @XmlElement(name = "release")
    @XmlElementWrapper
    List<Release> releases;

    public Deployment(ContextRoot contextRoot) {
        this(null, contextRoot, null, null, null);
    }

    public Deployment(DeploymentName name, ContextRoot contextRoot, CheckSum checkSum, Version version) {
        this(name, contextRoot, checkSum, version, null);
    }

    @Override
    public int compareTo(Deployment that) {
        return this.name.getValue().compareToIgnoreCase(that.name.getValue());
    }

    @JsonIgnore
    public boolean isNew() {
        return NEW_DEPLOYMENT_NAME.equals(name);
    }

    /** Is the name of the deployment equal to the context-root plus '.war'? */
    @JsonIgnore
    public boolean isDefaultName() {
        return (name == null) ? false : name.getValue().equals(contextRoot + ".war");
    }

    @Override
    public String toString() {
        return (isDefaultName() ? "" : name + ":") + contextRoot + ((version == null) ? "" : "@" + version);
    }
}
