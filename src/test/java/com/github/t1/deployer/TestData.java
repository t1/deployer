package com.github.t1.deployer;

import static java.util.Arrays.*;

import java.util.List;

public class TestData {
    public static final String CURRENT_FOO_VERSION = "1.3.1";
    public static final String CURRENT_BAR_VERSION = "0.3";

    public static final String FOO_CHECKSUM = "32D59F10CCEA21A7844D66C9DBED030FD67964D1";
    public static final String BAR_CHECKSUM = "FBD368E959DF458C562D0A4D1F70049D0FA3D620";

    public static String byteArray(String checksum) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < checksum.length(); i++) {
            if (i != 0)
                out.append(", ");
            out.append("0x").append(checksum.charAt(i++)).append(checksum.charAt(i));
        }
        return out.toString();
    }

    public static final List<Version> FOO_VERSIONS = asList(//
            new Version("1.3.10"), //
            new Version("1.3.2"), //
            new Version(CURRENT_FOO_VERSION), //
            new Version("1.3.0"), //
            new Version("1.2.1"), //
            new Version("1.2.1-SNAPSHOT"), //
            new Version("1.2.0") //
            );
}
