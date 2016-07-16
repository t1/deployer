package com.github.t1.deployer.container;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import javax.xml.bind.annotation.XmlValue;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonSerialize(using = ToStringSerializer.class)
public class DeploymentName {
    @NonNull
    @XmlValue
    String value;

    @JsonCreator public DeploymentName(String value) { this.value = value; }

    @Override
    public String toString() {
        return value;
    }

    public boolean matches(@NonNull Deployment deployment) { return this.equals(deployment.getName()); }
}
