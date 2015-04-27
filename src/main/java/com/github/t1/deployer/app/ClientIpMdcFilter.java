package com.github.t1.deployer.app;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;

import org.slf4j.MDC;

/** Put the IP-Address of the client into the MDC */
@WebFilter("/*")
public class ClientIpMdcFilter implements Filter {
    private static final String MDC_NAME = "client-ip";

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null)
            MDC.put(MDC_NAME, remoteAddr);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_NAME);
        }
    }
}
