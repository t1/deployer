package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.Password.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.*;

import java.util.*;

import javax.json.Json;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import org.assertj.core.api.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.config.ConfigInfo;
import com.github.t1.deployer.model.Password;
import com.github.t1.ramlap.tools.ProblemDetail;

@RunWith(MockitoJUnitRunner.class)
public class ConfigResourceTest {
    private static final ConfigInfo CONFIG_0 = new DummyConfigInfo("config0", "value0");
    private static final ConfigInfo CONFIG_1 = new DummyConfigInfo("config1", "value1");
    private static final ConfigInfo BOOLEAN_CONFIG =
            new DummyConfigInfo("bool", true, boolean.class, value -> Boolean.parseBoolean(value));

    ConfigResource resource = new ConfigResource();

    List<ConfigInfo> configs = new ArrayList<>();

    @Before
    public void before() {
        resource.configs = configs;
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
        configs.add(CONFIG_0);
        configs.add(CONFIG_1);

        List<ConfigInfo> got = resource.getConfigs();

        assertThat(got).isEqualTo(configs);
    }

    @Test
    public void shouldPostConfigUpdate() {
        configs.add(CONFIG_0);

        resource.postConfig(new Form().param("config0", "value0+"));

        assertThat(configs).extracting(config -> config.getValue()).containsExactly("value0+");
    }

    @Test
    public void shouldPostBooleanConfigUpdate() {
        configs.add(BOOLEAN_CONFIG);

        resource.postConfig(new Form().param("bool", "false"));

        assertThat(configs).extracting(config -> config.getValue()).containsExactly(false);
    }

    @Test
    public void shouldPostBooleanConfigUpdateToNull() {
        configs.add(BOOLEAN_CONFIG);

        resource.postConfig(new Form());

        assertThat(configs).extracting(config -> config.getValue()).containsExactly(false);
    }

    @Test
    public void shouldPostTwoConfigUpdate() {
        configs.add(CONFIG_0);
        configs.add(CONFIG_1);

        resource.postConfig(new Form().param("config0", "value0+").param("config1", "value1+"));

        assertThat(configs).extracting(config -> config.getValue()).containsExactly("value0+", "value1+");
    }

    @Test
    public void shouldPostConfigUpdateToNull() {
        configs.add(CONFIG_0);

        resource.postConfig(new Form());

        assertThat(configs).extracting(config -> config.getValue()).containsExactly((String) null);
    }

    @Test
    public void shouldFailPostConfigUpdateWithDuplicateKeys() {
        configs.add(CONFIG_0);
        configs.add(CONFIG_1);

        WebApplicationException thrown = (WebApplicationException) catchThrowable(
                () -> resource.postConfig(new Form().param("config0", "value0+").param("config0", "value0++")));

        assertThatResponse(thrown)
                .is(status(BAD_REQUEST))
                .is(title("invalid duplication of form parameters"));
    }

    @Test
    public void shouldFailPostConfigUpdateWithInvalidKey() {
        configs.add(CONFIG_0);
        configs.add(CONFIG_1);

        Response response = resource.postConfig(new Form().param("configX", "valueX"));

        assertThat((ProblemDetail) response.getEntity())
                .is(status(BAD_REQUEST))
                .is(title("invalid form parameter"));
    }

    @Test
    public void shouldIgnoreUpdateToUndisclosedPassword() {
        configs.add(DummyConfigInfo.builder()
                .name("password")
                .value(new Password("old-secret"))
                .meta(Json.createObjectBuilder()
                        .add("confidential", true)
                        .build())
                .build());

        resource.postConfig(new Form().param("password", UNDISCLOSED_PASSWORD));

        assertThat(configs)
                .extracting(config -> ((Password) config.getValue()).getValue())
                .containsExactly("old-secret");
    }
}
