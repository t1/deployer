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
public class DataSourceName implements Comparable<DataSourceName> {
    public static final DataSourceName ALL = new DataSourceName("*");

    @NonNull
    @XmlValue
    String value;

    @JsonCreator(mode = DELEGATING) public DataSourceName(@NotNull String value) { this.value = value; }

    @Override
    public String toString() {
        return value;
    }

    @Override public int compareTo(@NotNull DataSourceName that) { return this.value.compareTo(that.value); }
}
