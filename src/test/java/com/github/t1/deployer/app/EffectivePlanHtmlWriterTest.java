package com.github.t1.deployer.app;

import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.*;
import java.nio.file.Paths;

import static com.github.t1.deployer.container.LogHandlerType.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.Variables.*;
import static com.github.t1.log.LogLevel.*;
import static javax.ws.rs.core.MediaType.*;
import static org.assertj.core.api.Assertions.*;

public class EffectivePlanHtmlWriterTest {
    private static final ConfigurationPlan PLAN = ConfigurationPlan
            .builder()
            .logHandler(LogHandlerConfig
                    .builder()
                    .name(new LogHandlerName("FOO"))
                    .level(DEBUG)
                    .file("foo.log")
                    .type(periodicRotatingFile)
                    .formatter("COLOR-PATTERN")
                    .build())
            .logger(LoggerConfig
                    .builder()
                    .category(LoggerCategory.of("org.foo"))
                    .handler("FOO")
                    .level(ALL)
                    .build())
            .logger(LoggerConfig
                    .builder()
                    .category(LoggerCategory.of("org.bar"))
                    .handler("FOO")
                    .handler("BAR")
                    .level(INFO)
                    .build())
            .deployable(DeployableConfig
                    .builder()
                    .name(new DeploymentName("foo"))
                    .groupId(new GroupId("org.foo"))
                    .artifactId(new ArtifactId("foo"))
                    .version(new Version("1.0"))
                    .type(war)
                    .build())
            .deployable(DeployableConfig
                    .builder()
                    .name(new DeploymentName("bar"))
                    .groupId(new GroupId("org.bar"))
                    .artifactId(new ArtifactId("bar"))
                    .version(new Version("2.0"))
                    .type(jar)
                    .build())
            .build();

    public String expected() throws MalformedURLException {
        URI expected = Paths.get("src/test/java/com/github/t1/deployer/app/expected-plan.html").toUri();
        return contentOf(expected.toURL()).replaceAll("###hostname###", hostName());
    }

    @Test
    public void shouldWrite() throws Exception {
        EffectivePlanHtmlWriter writer = new EffectivePlanHtmlWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writer.writeTo(PLAN, ConfigurationPlan.class, ConfigurationPlan.class, null, TEXT_HTML_TYPE, null, out);

        assertThat(out.toString()).isEqualTo(expected());
    }
}
