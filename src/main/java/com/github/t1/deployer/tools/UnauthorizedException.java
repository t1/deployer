package com.github.t1.deployer.tools;

import javax.ejb.ApplicationException;

import lombok.RequiredArgsConstructor;

@ApplicationException
@RequiredArgsConstructor
public class UnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final User user;
    private final String operation;
    private final Object target;

    @Override
    public String getMessage() {
        return user + " is not allowed to perform " + operation + " on " + target;
    }
}
