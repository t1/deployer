package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlValue;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static lombok.AccessLevel.PRIVATE;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonSerialize(using = ToStringSerializer.class)
public class BundleName implements Comparable<BundleName> {
    public static final BundleName ALL = new BundleName("*");

    @NonNull
    @XmlValue
    String value;

    @JsonCreator(mode = DELEGATING) public BundleName(@NotNull String value) { this.value = value; }

    @Override public String toString() { return value; }

    @Override public int compareTo(@NotNull BundleName that) { return this.value.compareTo(that.value); }
}
