package com.github.t1.deployer.app;

import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.DataSourceName;
import com.github.t1.deployer.model.DataSourcePlan;
import com.github.t1.deployer.model.DeployablePlan;
import com.github.t1.deployer.model.DeploymentName;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.LogHandlerName;
import com.github.t1.deployer.model.LogHandlerPlan;
import com.github.t1.deployer.model.LoggerCategory;
import com.github.t1.deployer.model.LoggerPlan;
import com.github.t1.deployer.model.Plan;
import com.github.t1.deployer.model.Version;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Paths;

import static com.github.t1.deployer.model.ArtifactType.jar;
import static com.github.t1.deployer.model.ArtifactType.war;
import static com.github.t1.deployer.model.Expressions.hostName;
import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.log.LogLevel.ALL;
import static com.github.t1.log.LogLevel.DEBUG;
import static com.github.t1.log.LogLevel.INFO;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

class EffectivePlanHtmlWriterTest {
    private static final Plan PLAN = new Plan()
        .addLogHandler(new LogHandlerPlan(new LogHandlerName("FOO"))
            .setLevel(DEBUG)
            .setFile("foo.log")
            .setType(periodicRotatingFile)
            .setFormatter("COLOR-PATTERN"))
        .addLogger(new LoggerPlan(LoggerCategory.of("org.foo"))
            .setLevel(ALL)
            .addHandler("FOO"))
        .addLogger(new LoggerPlan(LoggerCategory.of("org.bar"))
            .setLevel(INFO)
            .addHandler("FOO")
            .addHandler("BAR"))
        .addDataSource(new DataSourcePlan(new DataSourceName("my-ds"))
            .setUri(URI.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")))
        .addDeployable(new DeployablePlan(new DeploymentName("foo"))
            .setGroupId(new GroupId("org.foo"))
            .setArtifactId(new ArtifactId("foo"))
            .setVersion(new Version("1.0"))
            .setType(war))
        .addDeployable(new DeployablePlan(new DeploymentName("bar"))
            .setGroupId(new GroupId("org.bar"))
            .setArtifactId(new ArtifactId("bar"))
            .setVersion(new Version("2.0"))
            .setType(jar));

    private String expected() throws MalformedURLException {
        URI expected = Paths.get("src/test/java/com/github/t1/deployer/app/expected-plan.html").toUri();
        return contentOf(expected.toURL()).replaceAll("###hostname###", hostName());
    }

    @Test void shouldWrite() throws Exception {
        EffectivePlanHtmlWriter writer = new EffectivePlanHtmlWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writer.writeTo(PLAN, Plan.class, Plan.class, null, TEXT_HTML_TYPE, null, out);

        assertThat(out.toString()).isEqualTo(expected());
    }
}
