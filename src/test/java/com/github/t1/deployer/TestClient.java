package com.github.t1.deployer;

import com.github.t1.deployer.app.Audit;
import com.github.t1.rest.UriTemplate;
import com.github.t1.testtools.FileMemento;
import org.junit.*;

import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static com.github.t1.deployer.app.DeployerBoundary.*;
import static com.github.t1.rest.RestContext.*;
import static org.assertj.core.api.Assertions.*;

@Ignore
public class TestClient {
    private static final Path JBOSS_CONFIG = Paths.get(System.getProperty("user.home"),
            "Tools/JBoss/current/standalone/configuration");

    public List<Audit> run(String plan) throws IOException {
        try (FileMemento memento = new FileMemento(JBOSS_CONFIG.resolve(ROOT_DEPLOYER_CONFIG)).setup()) {
            memento.write(plan);

            return REST
                    .createResource(UriTemplate.fromString("http://localhost:8080/deployer/api"))
                    .accept(new GenericType<List<Audit>>() {})
                    .POST();
        }
    }

    @Test
    public void shouldDeployJolokia() throws Exception {
        String plan = ""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n";

        List<Audit> audits = run(plan);

        assertThat(audits).containsExactly(Audit.ArtifactAudit.of("org.jolokia", "jolokia-war", "1.3.2").deployed());
    }

    @Test
    public void shouldUndeployJolokia() throws Exception {
        String plan = ""
                + "org.jolokia:\n"
                + "  jolokia-war:\n"
                + "    version: 1.3.2\n"
                + "    state: undeployed";

        List<Audit> audits = run(plan);

        assertThat(audits).containsExactly(Audit.ArtifactAudit.of("org.jolokia", "jolokia-war", "1.3.2").undeployed());
    }
}
