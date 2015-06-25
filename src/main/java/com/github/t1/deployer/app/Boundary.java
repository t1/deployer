package com.github.t1.deployer.app;

import static com.github.t1.log.LogLevel.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;

import javax.enterprise.inject.Stereotype;

import com.github.t1.log.Logged;

@Retention(RUNTIME)
@Stereotype
@Logged(level = INFO)
public @interface Boundary {}
