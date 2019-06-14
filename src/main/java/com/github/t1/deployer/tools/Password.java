package com.github.t1.deployer.tools;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Value;

@Value
@JsonSerialize(using = ToStringSerializer.class)
public class Password {
    public static final String CONCEALED = "concealed";

    private final String value;

    /** strange, but Jackson seems to not find the lombok generated String constructor :( */
    public Password(String value) { this.value = value; }

    @Override public String toString() { return CONCEALED; }
}
