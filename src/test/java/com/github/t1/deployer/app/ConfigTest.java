package com.github.t1.deployer.app;

import static com.github.t1.deployer.model.Config.*;
import static com.github.t1.deployer.model.Config.Authentication.*;
import static com.github.t1.deployer.model.Config.ContainerConfig.*;
import static com.github.t1.deployer.model.Config.RepositoryConfig.*;
import static com.github.t1.rest.fallback.JsonMessageBodyReader.*;
import static org.assertj.core.api.Assertions.*;

import java.io.*;
import java.net.URI;

import org.junit.Test;

import com.github.t1.deployer.model.Config;

public class ConfigTest {
    private static final String JSON = "{" //
            + "\"repository\":{" //
            + /**/ "\"uri\":\"http://example.org/repository\"," //
            + /**/ "\"authentication\":{" //
            + /**/ /**/ "\"username\":\"joe\"," //
            + /**/ /**/ "\"password\":\"doe\"" //
            + /**/ "}" //
            + "}," //
            + "\"container\":{" //
            + /**/ "\"uri\":\"http://example.org/container\"" //
            + "}" //
            + "}";
    private static final Config CONFIG = config() //
            .container(container() //
                    .uri(URI.create("http://example.org/container")) //
                    .build()) //
            .repository(repository() //
                    .uri(URI.create("http://example.org/repository")) //
                    .authentication(authentication() //
                            .username("joe") //
                            .password("doe") //
                            .build()) //
                    .build()) //
            .build();

    @Test
    public void shouldReadFromJson() throws Exception {
        Config config = MAPPER.readValue(new StringReader(JSON), Config.class);

        assertThat(config).isEqualTo(CONFIG);
    }

    @Test
    public void shouldWriteToJson() throws Exception {
        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, CONFIG);

        assertThat(writer.toString()).isEqualTo(JSON);
    }
}
