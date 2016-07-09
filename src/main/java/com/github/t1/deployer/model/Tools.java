package com.github.t1.deployer.model;

public class Tools {
    public static <T> T nvl(T value, T defaultValue) {
        return (value == null) ? defaultValue : value;
    }
}
