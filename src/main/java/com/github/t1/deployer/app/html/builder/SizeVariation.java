package com.github.t1.deployer.app.html.builder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SizeVariation {
    L("-lg"),
    M(""),
    S("-sm"),
    XS("-xs");

    public final String suffix;
}
