package com.github.t1.deployer.container;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import javax.interceptor.InterceptorBinding;

@InterceptorBinding
@Target({ TYPE, ANNOTATION_TYPE, METHOD, CONSTRUCTOR })
@Retention(RUNTIME)
public @interface ContainerDeployment {}
