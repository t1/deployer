package com.github.t1.deployer.tools;

import static javax.ws.rs.core.MediaType.*;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;

/**
 * JBoss tries to return application/octet-stream for WILDCARD requests, which often doesn't work, and even if it does,
 * it's generally not what clients can actually use. So we replace it with APPLICATION_JSON
 */
@WebFilter("/*")
public class AcceptHeaderFilter extends HttpFilter {
    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String accept = request.getHeader("Accept");
        if (accept == null || WILDCARD.equals(accept))
            request = HttpServletRequestWith.header(request, "Accept", APPLICATION_JSON);

        chain.doFilter(request, response);
    }
}
