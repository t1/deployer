package com.github.t1.deployer.app;

import com.github.t1.deployer.model.ArtifactId;
import org.junit.Test;

import java.io.StringReader;

import static com.github.t1.deployer.app.Deployer.*;
import static org.assertj.core.api.Assertions.*;

public class ConfigurationPlanSeraializationTest {
    @Test
    public void shouldLoadInSequence() throws Exception {
        String A = "  A:\n"
                + "    level: ALL\n"
                + "    file: deployer-audit.log\n"
                + "    suffix: .yyyy-MM-dd\n"
                + "    formatter: \"%d{yyyy-MM-dd HH:mm:ss,SSS}|%X{host}|%X{slot}|%X{version}|%X{client}|%t|%X{reference}|%c|%p|%s%e%n\"\n";
        String B = "  B:\n"
                + "    level: ALL\n"
                + "    file: deployer.log\n"
                + "    suffix: .yyyy-MM-dd\n"
                + "    formatter: \"%d{yyyy-MM-dd HH:mm:ss,SSS}|%X{host}|%X{slot}|%X{version}|%X{client}|%t|%X{reference}|%c|%p|%s%e%n\"\n";
        ConfigurationPlan planAB = ConfigurationPlan.load(new StringReader(""
                + "log-handlers:\n"
                + A
                + B
        ));
        ConfigurationPlan planBA = ConfigurationPlan.load(new StringReader(""
                + "log-handlers:\n"
                + B
                + A
        ));

        assertThat(planAB.getGroupIds()).containsExactly(LOG_HANDLERS);
        assertThat(planAB.getArtifactIds(LOG_HANDLERS))
                .extracting(ArtifactId::getValue)
                .containsExactly("A", "B");

        assertThat(planBA.getGroupIds()).containsExactly(LOG_HANDLERS);
        assertThat(planBA.getArtifactIds(LOG_HANDLERS))
                .extracting(ArtifactId::getValue)
                .containsExactly("B", "A");
    }
}
