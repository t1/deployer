package com.github.t1.deployer.app;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.container.LogHandler.LogHandlerBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.log.LogLevel;
import lombok.*;
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

    @Mock DeploymentContainer deployments;
    @Mock LoggerContainer loggers;

    @Mock LogHandler logHandlerMock;
    @Mock LogHandlerBuilder logHandlerBuilderMock;

    private final List<Deployment> allDeployments = new ArrayList<>();

    @Before
    public void before() {
        when(deployments.getAllDeployments()).then(invocation -> allDeployments);

        when(loggers.buildHandler(any(LoggingHandlerType.class), anyString())).thenReturn(logHandlerBuilderMock);
        when(logHandlerBuilderMock.file(anyString())).thenReturn(logHandlerBuilderMock);
        when(logHandlerBuilderMock.suffix(anyString())).thenReturn(logHandlerBuilderMock);
        when(logHandlerBuilderMock.formatter(anyString())).thenReturn(logHandlerBuilderMock);
        when(logHandlerBuilderMock.build()).thenReturn(logHandlerMock);
    }

    @After
    public void afterDeployments() {
        verify(deployments, atLeast(0)).getAllDeployments();
        verify(deployments, atLeast(0)).hasDeployment(any(DeploymentName.class));
        verify(deployments, atLeast(0)).getDeployment(any(DeploymentName.class));

        verifyNoMoreInteractions(deployments);
    }

    @After
    public void afterLoggers() {
        verify(loggers, atLeast(0)).hasLogger(anyString());
        verify(loggers, atLeast(0)).getLogger(anyString());
        verify(loggers, atLeast(0)).buildHandler(any(LoggingHandlerType.class), anyString());

        verifyNoMoreInteractions(loggers);
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
                when(deployments.hasDeployment(deploymentName())).thenReturn(true);
                when(deployments.getDeployment(deploymentName())).thenReturn(deployment);
                return this;
            }

            public CheckSum checkSum() { return fakeChecksumFor(contextRoot(), version); }

            public InputStream inputStream() { return inputStreamFor(contextRoot(), version); }

            public DeploymentName deploymentName() { return ArtifactFixture.this.deploymentName(); }

            public ArtifactFixture and() { return ArtifactFixture.this; }


            public void verifyDeployed() { verify(deployments).deploy(deploymentName(), inputStream()); }

            public void verifyRedeployed() { verify(deployments).redeploy(deploymentName(), inputStream()); }

            public void verifyUndeployed() { verify(deployments).undeploy(deploymentName()); }
        }
    }


    public LoggerFixture givenLogger(String name) { return new LoggerFixture(name); }

    @Getter
    public class LoggerFixture {
        private final String category;
        private LogLevel level;

        public LoggerFixture(String category) {
            this.category = category;

            when(loggers.hasLogger(category)).thenReturn(false);
        }

        public LoggerFixture level(LogLevel level) {
            this.level = level;
            return this;
        }

        public LoggerConfig getConfig() {
            return new LoggerConfig(category, level);
        }

        public LoggerFixture exists() {
            when(loggers.hasLogger(category)).thenReturn(true);
            when(loggers.getLogger(category)).then(invocation -> getConfig());
            return this;
        }
    }


    public LogHandlerFixture givenLogHandler(LoggingHandlerType type, String name) {
        return new LogHandlerFixture(type, name);
    }

    public class LogHandlerFixture {
        private final LoggingHandlerType type;
        private final String name;
        private String file;
        private String suffix;
        private String formatter;

        public LogHandlerFixture(LoggingHandlerType type, String name) {
            this.type = type;
            this.name = name;
        }

        public LogHandlerFixture file(String file) {
            this.file = file;
            return this;
        }

        public LogHandlerFixture suffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        public LogHandlerFixture formatter(String formatter) {
            this.formatter = formatter;
            return this;
        }

        public void verifyAdded() {
            verify(loggers).buildHandler(type, name);
            verify(logHandlerBuilderMock).file(file);
            verify(logHandlerBuilderMock).suffix(suffix);
            verify(logHandlerBuilderMock).formatter(formatter);
            verify(logHandlerBuilderMock).build();
            verify(logHandlerMock).add();
        }
    }
}
