package com.github.t1.deployer.model;

import lombok.Value;

@Value
public class Password {
    public static final String UNDISCLOSED_PASSWORD = "undisclosed password";

    String value;

    public Password(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return UNDISCLOSED_PASSWORD;
    }
}
