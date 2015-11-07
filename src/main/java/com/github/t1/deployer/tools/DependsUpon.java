package com.github.t1.deployer.tools;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

@Retention(RUNTIME)
@Target(PACKAGE)
public @interface DependsUpon {
    Class<?>[] value() default {};
}
