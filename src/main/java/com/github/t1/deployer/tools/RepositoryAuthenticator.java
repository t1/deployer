package com.github.t1.deployer.tools;

import com.github.t1.deployer.model.Config;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.net.URI;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class RepositoryAuthenticator implements ClientRequestFilter {
    @Setter @Inject @Config("repository.uri") URI uri;
    @Setter @Inject @Config("repository.username") String username;
    @Setter @Inject @Config("repository.password") Password password;

    @Override public void filter(ClientRequestContext requestContext) {
        if (uri != null && requestContext.getUri().getHost().equals(uri.getHost())) {
            log.debug("use {} credentials for {}", username, uri);
            requestContext.getHeaders().add("Authorization", "BASIC " + base64(username + ":" + password.getValue()));
        }
    }

    private String base64(String string) {
        return Base64.getEncoder().encodeToString(string.getBytes(UTF_8));
    }
}
