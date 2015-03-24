package com.github.t1.deployer;

public class InterceptorBinding {
    public static <T, I> T of(T target, Class<I> interceptorType) {
        return target;
    }
}
