package com.github.t1.deployer.repository;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;

import javax.inject.Qualifier;

@Retention(RUNTIME)
@Qualifier
public @interface Artifactory {}
