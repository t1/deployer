package com.github.t1.deployer.app;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import lombok.RequiredArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.StringReader;

import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployerTest {
    @InjectMocks Deployer deployer;
    @Mock DeploymentContainer container;
    @Mock Repository repository;

    private ArtifactFixture givenArtifact(String name) { return new ArtifactFixture(name); }

    @RequiredArgsConstructor
    private class ArtifactFixture {
        public final String name;

        private GroupId groupId() { return new GroupId("org." + name); }

        private ArtifactId artifactId() { return new ArtifactId(name); }

        private ContextRoot contextRoot() { return new ContextRoot(name); }

        private DeploymentName deploymentName() { return new DeploymentName(name); }

        private ArtifactFixture version(String version) { return version(new Version(version)); }

        private ArtifactFixture version(Version version) {
            when(repository.fetchArtifact(groupId(), artifactId(), version)).thenReturn(Artifact
                    .builder()
                    .groupId(groupId())
                    .artifactId(artifactId())
                    .version(version)
                    .sha1(fakeChecksumFor(contextRoot(), version))
                    .inputStreamSupplier(() -> inputStreamFor(contextRoot(), version))
                    .build());
            return this;
        }
    }

    @Test
    public void shouldRunPlan() {
        ArtifactFixture foo = givenArtifact("foo").version("1.3.2");
        ConfigurationPlan plan = ConfigurationPlan.load(new StringReader(""
                + "org.foo:\n"
                + "  foo:\n"
                + "    version: 1.3.2\n"));

        deployer.run(plan);

        verify(container).deploy(foo.deploymentName(), inputStreamFor(foo.contextRoot(), new Version("1.3.2")));
    }
}
