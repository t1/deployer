package com.github.t1.deployer.tools;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Convenience {@link Filter} with empty {@link #init(FilterConfig)}/{@link #destroy()}, and downcast to
 * {@link HttpServletRequest}/{@link HttpServletResponse}.
 * <p>
 * Could be extended to store the {@link FilterConfig}.
 */
public abstract class HttpFilter implements Filter {
    @Override
    @SuppressWarnings("unused")
    public void init(FilterConfig filterConfig) throws ServletException {}

    /** use {@link #doFilter(HttpServletRequest, HttpServletResponse, FilterChain)} */
    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    public abstract void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException;

    @Override
    public void destroy() {}
}
