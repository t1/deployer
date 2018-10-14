package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlValue;

import static lombok.AccessLevel.PRIVATE;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@JsonSerialize(using = ToStringSerializer.class)
public class LogHandlerName implements Comparable<LogHandlerName> {
    public static final LogHandlerName ALL = new LogHandlerName("*");

    @NonNull
    @XmlValue
    String value;

    @JsonCreator public LogHandlerName(String value) { this.value = value; }

    @Override
    public String toString() { return value; }

    @Override public int compareTo(@NotNull LogHandlerName that) { return this.value.compareTo(that.value); }
}
