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
public class LogHandlerName implements Comparable<LogHandlerName> {
    public static final LogHandlerName ALL = new LogHandlerName("*");

    @NonNull
    @XmlValue
    String value;

    @JsonCreator public LogHandlerName(String value) { this.value = value; }

    @Override
    public String toString() {
        return value;
    }

    public boolean matches(@NonNull LogHandlerResource logHandler) { return this.equals(logHandler.name()); }

    @Override public int compareTo(@NotNull LogHandlerName that) { return this.value.compareTo(that.value); }
}
