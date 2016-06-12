package com.github.t1.deployer.model;

import lombok.*;

import javax.xml.bind.annotation.XmlValue;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
public class DeploymentName {
    @NonNull
    @XmlValue
    String value;

    @Override
    public String toString() {
        return value;
    }
}
