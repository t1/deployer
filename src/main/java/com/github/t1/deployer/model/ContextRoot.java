package com.github.t1.deployer.model;

import javax.xml.bind.annotation.XmlValue;

import lombok.*;

@Value
@AllArgsConstructor
public class ContextRoot {
    @NonNull
    @XmlValue
    String value;

    @SuppressWarnings("unused")
    private ContextRoot() {
        this.value = null;
    }

    @Override
    public String toString() {
        return value;
    }
}
