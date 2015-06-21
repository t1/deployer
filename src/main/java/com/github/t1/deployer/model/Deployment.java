package com.github.t1.deployer.model;

import static javax.xml.bind.annotation.XmlAccessType.*;

import javax.xml.bind.annotation.*;

import lombok.*;
import lombok.experimental.Wither;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Value
@Builder
@AllArgsConstructor
@XmlRootElement
@XmlAccessorType(FIELD)
public class Deployment implements Comparable<Deployment> {
    public static final String NEW_DEPLOYMENT_NAME = "!";
    public static final Deployment NULL_DEPLOYMENT = new Deployment(null, null, null, null);

    @Wither
    private final DeploymentName name;
    private final ContextRoot contextRoot;
    private final CheckSum checkSum;

    @Wither
    private Version version;

    /** required by JAXB, etc. */
    @SuppressWarnings("unused")
    private Deployment() {
        this.name = null;
        this.contextRoot = null;
        this.checkSum = null;
        this.version = null;
    }

    @Override
    public int compareTo(Deployment that) {
        return this.name.getValue().compareToIgnoreCase(that.name.getValue());
    }

    @JsonIgnore
    public boolean isNew() {
        return NEW_DEPLOYMENT_NAME.equals(name);
    }

    @Override
    public String toString() {
        return name + "(" + contextRoot + ")";
    }
}
