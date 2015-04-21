package com.github.t1.deployer.model;

import javax.xml.bind.annotation.XmlValue;

import lombok.*;

@Value
@AllArgsConstructor
public class DeploymentName {
    @NonNull
    @XmlValue
    String value;

    @SuppressWarnings("unused")
    private DeploymentName() {
        this.value = null;
    }

    @Override
    public String toString() {
        return value;
    }
}
