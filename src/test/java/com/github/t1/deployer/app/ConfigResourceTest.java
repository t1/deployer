package com.github.t1.deployer.app;

import static com.github.t1.config.ConfigInfo.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.*;

import java.util.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response.Status;

import org.assertj.core.api.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.config.ConfigInfo;
import com.github.t1.ramlap.tools.ProblemDetail;

@RunWith(MockitoJUnitRunner.class)
public class ConfigResourceTest {
    private static final ConfigInfo CONFIG_0 = config("config0").value("value0").build();
    private static final ConfigInfo CONFIG_1 = config("config1").value("value1").build();

    ConfigResource resource = new ConfigResource();

    List<ConfigInfo> given = new ArrayList<>();

    @Before
    public void before() {
        resource.configs = given;
    }

    private AbstractObjectAssert<?, ProblemDetail> assertThatResponse(WebApplicationException thrown) {
        assertThat(thrown).as("web application exception").isNotNull();
        assertThat(thrown.getResponse()).as("response").isNotNull();
        return assertThat((ProblemDetail) thrown.getResponse().getEntity());
    }

    private Condition<ProblemDetail> status(Status status) {
        return new Condition<>(d -> status.equals(d.status()), "status: " + status);
    }

    private Condition<ProblemDetail> title(String title) {
        return new Condition<>(d -> title.equals(d.title()), "title: " + title);
    }

    @Test
    public void shouldGetConfig() {
        given.add(CONFIG_0);
        given.add(CONFIG_1);

        List<ConfigInfo> got = resource.getConfig();

        assertThat(got).isEqualTo(given);
    }

    @Test
    public void shouldPostConfigUpdate() {
        given.add(CONFIG_0);
        given.add(CONFIG_1);

        resource.postConfig(new Form().param("config0", "value0+"));

        assertThat(given).extracting(config -> config.getValue()).containsExactly("value0+", "value1");
    }

    @Test
    public void shouldFailPostConfigUpdateWithDuplicateKeys() {
        given.add(CONFIG_0);
        given.add(CONFIG_1);

        WebApplicationException thrown = (WebApplicationException) catchThrowable(
                () -> resource.postConfig(new Form().param("config0", "value0+").param("config0", "value0++")));

        assertThatResponse(thrown)
                .is(status(BAD_REQUEST))
                .is(title("invalid duplication of form parameters"));
    }

    @Test
    public void shouldFailPostConfigUpdateWithInvalidKey() {
        given.add(CONFIG_0);
        given.add(CONFIG_1);

        WebApplicationException thrown = (WebApplicationException) catchThrowable(
                () -> resource.postConfig(new Form().param("configX", "valueX")));

        assertThatResponse(thrown)
                .is(status(BAD_REQUEST))
                .is(title("invalid form parameter"));
    }
}
