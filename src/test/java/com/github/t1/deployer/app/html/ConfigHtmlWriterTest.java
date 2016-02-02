package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.ConfigResource.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.config.*;
import com.github.t1.deployer.app.file.DeploymentListFile;
import com.github.t1.deployer.model.Password;
import com.github.t1.deployer.repository.ArtifactoryRepository;

@RunWith(MockitoJUnitRunner.class)
public class ConfigHtmlWriterTest extends AbstractHtmlWriterTest<List<ConfigInfo>> {
    public ConfigHtmlWriterTest() {
        super(new ConfigHtmlWriter());
    }

    @Test
    public void shouldWriteEmptyConfigForm() throws Exception {
        List<ConfigInfo> config = emptyList();

        String entity = write(config);

        assertEquals(readFile(), entity);
    }

    @Test
    public void shouldWriteConfiguredConfigForm() throws Exception {
        List<ConfigInfo> config = asList(
                configInfo(DeploymentListFile.class, "autoUndeploy", true),
                configInfo(ArtifactoryRepository.class, "artifactory", URI.create("http://uri.repository.example.net")),
                configInfo(ArtifactoryRepository.class, "artifactoryUserName", "joe"),
                configInfo(ArtifactoryRepository.class, "artifactoryPassword", new Password("doe")));
        Collections.sort(config, BY_ORDER);

        String entity = write(config);

        assertEquals(readFile(), entity);
    }

    private ConfigInfo configInfo(Class<?> container, String name, Object value) throws ReflectiveOperationException {
        Field field = container.getDeclaredField(name);
        field.setAccessible(true);
        Config config = field.getAnnotation(Config.class);
        return new ConfigInfo() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return config.description();
            }

            @Override
            public String getDefaultValue() {
                return config.defaultValue();
            }

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public Class<?> getType() {
                return value.getClass();
            }

            @Override
            public Class<?> getContainer() {
                return container;
            }

            @Override
            public JsonObject getMeta() {
                return CdiProducers.toJson(config.meta());
            }

            @Override
            public boolean isUpdatable() {
                return false;
            }

            @Override
            public void updateTo(String value) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
