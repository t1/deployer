package com.github.t1.deployer.app;

import static java.nio.file.Files.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;
import static javax.xml.bind.DatatypeConverter.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.security.*;
import java.util.Properties;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import com.github.t1.deployer.tools.User;

@WebFilter("/*")
public class AuthorizationFilter implements Filter {
    public static final String REALM = "ManagementRealm";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public static Exception unauthorized(String message) {
        Response response = Response //
                .status(UNAUTHORIZED) //
                .header("WWW-Authenticate", "Basic realm=\"" + REALM + "\"") //
                .entity(message) //
                .type(TEXT_PLAIN) //
                .build();
        return new WebApplicationException(response);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        authorize((HttpServletRequest) request);
        try {
            chain.doFilter(request, response);
        } finally {
            User.setCurrent(null);
        }
    }

    private void authorize(HttpServletRequest request) {
        String authorization = request.getHeader("authorization");
        if (authorization != null) {
            String[] split = authorization.split(" ", 2);
            if (split.length == 2 && "basic".equalsIgnoreCase(split[0])) {
                basicAuth(split[1]);
            }
        }
    }

    private void basicAuth(String base64) {
        String[] split = parseBase64(base64.trim()).split(":", 2);
        if (split.length != 2)
            throw webException(BAD_REQUEST, "no colon in basic auth");
        String userName = split[0];
        String password = split[1];
        String expected = readManagers().getProperty(userName);
        if (expected == null)
            throw webException(UNAUTHORIZED, "unknown user");
        String actual = crypted(userName, REALM, password);
        if (!expected.equalsIgnoreCase(actual))
            throw webException(UNAUTHORIZED, "hash mismatch");
        User.setCurrent(new User(userName).withPrivilege("deploy", "undeploy", "redeploy"));
    }

    private WebApplicationException webException(Status status, String message) {
        return new WebApplicationException(Response.status(status).entity(message).type(TEXT_PLAIN).build());
    }

    private String parseBase64(String base64) {
        try {
            return new String(parseBase64Binary(base64));
        } catch (IllegalArgumentException e) {
            throw webException(BAD_REQUEST, "not valid base64");
        }
    }

    private Properties readManagers() {
        try {
            Properties properties = new Properties();
            Path dir = Paths.get(System.getProperty("jboss.server.config.dir", "."));
            Path file = dir.resolve("mgmt-users.properties");
            if (Files.isRegularFile(file))
                properties.load(newBufferedReader(file, UTF_8));
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String crypted(String userName, String realm, String password) {
        // username=HEX( MD5( username ':' realm ':' password))
        return printHexBinary(md5(userName + ":" + realm + ":" + password)).toLowerCase();
    }

    private byte[] md5(String string) {
        try {
            return MessageDigest.getInstance("MD5").digest(string.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
