package com.github.t1.deployer.tools;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.util.*;

import javax.servlet.http.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpServletRequestWith extends HttpServletRequestWrapper {
    public static HttpServletRequest header(HttpServletRequest request, String headerName, String headerValue) {
        String oldHeaderValue = request.getHeader(headerName);
        if (oldHeaderValue == null)
            log.debug("add header {}: {}", headerName, headerValue);
        else
            log.debug("overwrite {} header {} with {}", headerName, oldHeaderValue, headerValue);
        return new HttpServletRequestWith(request, headerName, headerValue);
    }

    private final String headerName;
    private final String headerValue;

    private HttpServletRequestWith(HttpServletRequest request, String headerName, String headerValue) {
        super(request);
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    @Override
    public String getHeader(String name) {
        if (headerName.equals(name))
            return headerValue;
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        if (!names.contains(headerName))
            names.add(headerName);
        return Collections.enumeration(names);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (headerName.equals(name))
            return enumeration(asList(headerValue));
        return super.getHeaders(name);
    }
}