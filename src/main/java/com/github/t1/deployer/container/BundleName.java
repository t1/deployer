package com.github.t1.deployer.container;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlValue;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonSerialize(using = ToStringSerializer.class)
public class BundleName implements Comparable<BundleName> {
    public static final BundleName ALL = new BundleName("*");

    @NonNull
    @XmlValue
    String value;

    @JsonCreator public BundleName(String value) { this.value = value; }

    @Override public String toString() { return value; }

    public boolean matches(@NonNull DeploymentResource deployment) { return this.equals(deployment.name()); }

    @Override public int compareTo(@NotNull BundleName that) { return this.value.compareTo(that.value); }
}
