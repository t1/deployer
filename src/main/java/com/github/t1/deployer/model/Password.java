package com.github.t1.deployer.model;

import lombok.Value;

@Value
public class Password {
    public static final String CONCEALED = "concealed";

    private final String value;

    /** strange, but Jackson seems to not find the lombok generated String constructor :( */
    public Password(String value) { this.value = value; }

    @Override public String toString() { return CONCEALED; }
}
