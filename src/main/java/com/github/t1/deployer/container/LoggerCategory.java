package com.github.t1.deployer.container;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlValue;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor(access = PRIVATE)
@JsonSerialize(using = ToStringSerializer.class)
public class LoggerCategory implements Comparable<LoggerCategory> {
    public static final LoggerCategory ROOT = new LoggerCategory("ROOT");
    public static final LoggerCategory ALL = new LoggerCategory("*");

    public static LoggerCategory of(@NonNull String value) {
        if (value.isEmpty() || value.equals(ROOT.getValue()))
            return ROOT;
        if (value.equals(ALL.getValue()))
            return ALL;
        return new LoggerCategory(value);
    }

    @NonNull
    @XmlValue
    String value;

    @Override
    public String toString() {
        return value;
    }

    public boolean isRoot() { return ROOT.getValue().equals(value); }

    @Override public int compareTo(@NotNull LoggerCategory that) {
        return this.value.compareTo(that.value);
    }
}
