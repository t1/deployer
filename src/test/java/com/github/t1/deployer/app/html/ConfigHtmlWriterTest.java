package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.model.Config.Authentication.*;
import static com.github.t1.deployer.model.Config.ContainerConfig.*;
import static com.github.t1.deployer.model.Config.RepositoryConfig.*;
import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.Config;

@RunWith(MockitoJUnitRunner.class)
public class ConfigHtmlWriterTest extends AbstractHtmlWriterTest<Config> {
    public ConfigHtmlWriterTest() {
        super(new ConfigHtmlWriter());
    }

    @Test
    public void shouldWriteEmptyConfigForm() throws Exception {
        Config config = Config.config().build();

        String entity = write(config);

        assertEquals(readFile(), entity);
    }

    @Test
    public void shouldWriteConfiguredConfigForm() throws Exception {
        Config config = Config.config() //
                .container(container() //
                        .uri(URI.create("http://uri.container.example.net")) //
                        .build()) //
                .repository(repository() //
                        .uri(URI.create("http://uri.repository.example.net")) //
                        .authentication(authentication() //
                                .username("joe") //
                                .password("doe") //
                                .build()) //
                        .build()) //
                .build();

        String entity = write(config);

        assertEquals(readFile(), entity);
    }
}
