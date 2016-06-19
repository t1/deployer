package com.github.t1.deployer.app;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.log.LogLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.mockito.Mock;

import java.io.InputStream;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.mockito.Mockito.*;

@Slf4j
public class AbstractDeployerTest {
    @Mock Repository repository;
    @Mock DeploymentContainer deploymentContainer;
    @Mock LoggerContainer loggerContainer;

    private final List<Deployment> allDeployments = new ArrayList<>();


    @Before
    public void before() {
        when(deploymentContainer.getAllDeployments()).then(invocation -> allDeployments);
    }

    @After
    public void after() {
        verify(deploymentContainer, atLeast(0)).getAllDeployments();
        verify(deploymentContainer, atLeast(0)).hasDeployment(any(DeploymentName.class));
        verify(deploymentContainer, atLeast(0)).getDeployment(any(DeploymentName.class));

        verifyNoMoreInteractions(deploymentContainer);
    }


    public ArtifactFixture givenArtifact(String groupId, String artifactId) {
        return new ArtifactFixture(groupId, artifactId);
    }

    public ArtifactFixture givenArtifact(String name) { return new ArtifactFixture("org." + name, name + "-war"); }

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
                when(repository.buildArtifact(groupId(), artifactId(), version, war)).thenReturn(artifact);
            }

            public VersionFixture deployed() {
                Deployment deployment = new Deployment(deploymentName(), contextRoot(), checkSum(), version);
                allDeployments.add(deployment);
                when(deploymentContainer.hasDeployment(deploymentName())).thenReturn(true);
                when(deploymentContainer.getDeployment(deploymentName())).thenReturn(deployment);
                return this;
            }

            public CheckSum checkSum() { return fakeChecksumFor(contextRoot(), version); }

            public InputStream inputStream() { return inputStreamFor(contextRoot(), version); }

            public DeploymentName deploymentName() { return ArtifactFixture.this.deploymentName(); }

            public ArtifactFixture and() { return ArtifactFixture.this; }


            public void verifyDeployed() { verify(deploymentContainer).deploy(deploymentName(), inputStream()); }

            public void verifyRedeployed() { verify(deploymentContainer).redeploy(deploymentName(), inputStream()); }

            public void verifyUndeployed() { verify(deploymentContainer).undeploy(deploymentName()); }
        }
    }

    public LoggerFixture givenLogger(String name) { return new LoggerFixture(name); }

    public class LoggerFixture {
        private final String category;
        private LogLevel level;

        public LoggerFixture(String category) {
            this.category = category;

            when(loggerContainer.hasLogger(category)).thenReturn(true);
            when(loggerContainer.getLogger(category)).then(invocation -> getConfig());
        }

        public LoggerFixture level(LogLevel level) {
            this.level = level;
            return this;
        }

        public LoggerConfig getConfig() {
            return new LoggerConfig(category, level);
        }
    }
}