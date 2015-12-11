package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.model.ConfigModel.Authentication.*;
import static com.github.t1.deployer.model.ConfigModel.ContainerConfig.*;
import static com.github.t1.deployer.model.ConfigModel.DeploymentListFileConfig.*;
import static com.github.t1.deployer.model.ConfigModel.RepositoryConfig.*;
import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.ConfigModel;

@RunWith(MockitoJUnitRunner.class)
public class ConfigHtmlWriterTest extends AbstractHtmlWriterTest<ConfigModel> {
    public ConfigHtmlWriterTest() {
        super(new ConfigHtmlWriter());
    }

    @Test
    public void shouldWriteEmptyConfigForm() throws Exception {
        ConfigModel config = ConfigModel.config().build();

        String entity = write(config);

        assertEquals(readFile(), entity);
    }

    @Test
    public void shouldWriteConfiguredConfigForm() throws Exception {
        ConfigModel config = ConfigModel.config() //
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
                .deploymentListFileConfig(deploymentListFileConfig() //
                        .autoUndeploy(true) //
                        .build()) //
                .build();

        String entity = write(config);

        assertEquals(readFile(), entity);
    }
}
