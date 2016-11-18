package com.github.t1.deployer.tools;

public class Tools {
    public static <T> T nvl(T value, T defaultValue) { return (value == null) ? defaultValue : value; }

    public static <T> String toStringOrNull(T value) { return (value == null) ? null : value.toString(); }

    public static Boolean trueOrNull(String string) { return ("true".equals(string)) ? true : null; }
}
