package com.github.t1.deployer.app;

import static com.github.t1.deployer.TestData.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import io.dropwizard.testing.junit.DropwizardClientRule;

import java.net.URI;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.*;

import org.glassfish.jersey.filter.LoggingFilter;
import org.junit.*;

import com.github.t1.deployer.app.html.Index;

public class IndexIT {
    @ClassRule
    public static DropwizardClientRule index = new DropwizardClientRule(new Index());

    @Test
    public void shouldRedirectFromIndexToGetAll() {
        URI uri = UriBuilder.fromUri(index.baseUri()).path(Index.class).build();
        Response response = ClientBuilder.newClient().target(uri).register(LoggingFilter.class).request().get();

        // I don't know how I could prevent WebTarget from following the redirect
        assertStatus(NOT_FOUND, response);
        assertThat(response.readEntity(String.class), containsString("Problem accessing /application/deployments/*"));
    }
}
