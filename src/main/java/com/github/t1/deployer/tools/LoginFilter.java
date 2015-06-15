package com.github.t1.deployer.tools;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebFilter("/*")
public class LoginFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String authType = getAuthType(request);
        if (request.getUserPrincipal() == null && authType != null) {
            log.info("attempt {} login", authType);
            boolean authenticated = request.authenticate(response);
            if (!authenticated) {
                log.info("authentication not finished");
                return;
            }
            log.info("authenticated {}/{} by {}/{}. continue.", //
                    request.getUserPrincipal(), request.getRemoteUser(), authType, request.getAuthType());
        } else {
            log.debug("don't attempt login: {} : {}", request.getUserPrincipal(), authType);
        }
        chain.doFilter(request, response);
    }

    private String getAuthType(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null)
            return null;
        String[] split = authorization.split(" ", 2);
        if (split.length != 2)
            return null;
        return split[0];
    }
}
