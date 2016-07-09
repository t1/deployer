package com.github.t1.deployer.model;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.*;

@Retention(RUNTIME)
@Qualifier
public @interface Config {
    String value();
}
