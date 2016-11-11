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
public class DataSourceName implements Comparable<DataSourceName> {
    public static final DataSourceName ALL = new DataSourceName("*");

    @NonNull
    @XmlValue
    String value;

    @JsonCreator public DataSourceName(String value) { this.value = value; }

    @Override
    public String toString() {
        return value;
    }

    public boolean matches(@NonNull DataSourceResource dataSource) { return this.equals(dataSource.name()); }

    @Override public int compareTo(@NotNull DataSourceName that) { return this.value.compareTo(that.value); }
}
