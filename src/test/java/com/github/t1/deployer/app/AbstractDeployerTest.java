package com.github.t1.deployer.app;

import com.github.t1.deployer.container.DeploymentContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.mockito.Mock;

import java.io.InputStream;

import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.mockito.Mockito.*;

@Slf4j
public class AbstractDeployerTest {
    @Mock DeploymentContainer container;
    @Mock Repository repository;

    protected ArtifactFixture givenArtifact(String groupId, String artifactId) {
        return new ArtifactFixture(groupId, artifactId);
    }

    protected ArtifactFixture givenArtifact(String name) { return new ArtifactFixture("org." + name, name + "-war"); }

    @RequiredArgsConstructor
    public class ArtifactFixture {
        public final String groupId;
        public final String artifactId;

        public GroupId groupId() { return new GroupId(groupId); }

        public ArtifactId artifactId() { return new ArtifactId(artifactId); }

        public ContextRoot contextRoot() { return new ContextRoot(artifactId); }

        public DeploymentName deploymentName() { return new DeploymentName(artifactId); }

        public VersionFixture version(String version) { return version(new Version(version)); }

        public VersionFixture version(Version version) { return new VersionFixture(version); }

        public class VersionFixture {
            private final Version version;

            public VersionFixture(Version version) {
                this.version = version;

                Artifact artifact = Artifact
                        .builder()
                        .groupId(groupId())
                        .artifactId(artifactId())
                        .version(version)
                        .sha1(checkSum())
                        .inputStreamSupplier(() -> inputStreamFor(contextRoot(), version))
                        .build();
                log.debug("given artifact: {}", artifact);
                when(repository.buildArtifact(groupId(), artifactId(), version)).thenReturn(artifact);
            }

            public VersionFixture deployed() {
                when(container.hasDeployment(deploymentName())).thenReturn(true);
                when(container.getDeployment(deploymentName()))
                        .thenReturn(new Deployment(deploymentName(), contextRoot(), checkSum(), version));
                return this;
            }

            public CheckSum checkSum() { return fakeChecksumFor(contextRoot(), version); }

            public InputStream inputStream() { return inputStreamFor(contextRoot(), version); }

            public DeploymentName deploymentName() { return ArtifactFixture.this.deploymentName(); }

            public ArtifactFixture and() { return ArtifactFixture.this; }


            public void verifyDeployed() { verify(container).deploy(deploymentName(), inputStream()); }

            public void verifyRedeployed() { verify(container).redeploy(deploymentName(), inputStream()); }

            public void verifyUndeployed() { verify(container).undeploy(deploymentName()); }
        }
    }

    @After
    public void after() {
        verify(container, atLeast(0)).hasDeployment(any(DeploymentName.class));
        verify(container, atLeast(0)).getDeployment(any(DeploymentName.class));

        verifyNoMoreInteractions(container);
    }
}
