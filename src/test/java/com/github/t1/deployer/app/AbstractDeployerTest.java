package com.github.t1.deployer.app;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.container.LogHandler.LogHandlerBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.log.LogLevel;
import lombok.*;
import org.junit.*;
import org.mockito.Mock;

import java.io.InputStream;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.mockito.Mockito.*;

public class AbstractDeployerTest {
    @Mock Repository repository;

    @Mock DeploymentContainer deployments;
    @Mock LoggerContainer loggers;

    @Mock LoggerResource loggerMock;

    @Mock LogHandler logHandlerMock;
    @Mock LogHandlerBuilder logHandlerBuilderMock;

    private final List<Deployment> allDeployments = new ArrayList<>();

    @Before
    public void before() {
        when(deployments.getAllDeployments()).then(invocation -> allDeployments);

        when(logHandlerMock.toBuilder()).thenReturn(logHandlerBuilderMock);

        when(logHandlerMock.correctLevel(any(LogLevel.class))).thenReturn(logHandlerMock);
        when(logHandlerMock.correctFile(anyString())).thenReturn(logHandlerMock);
        when(logHandlerMock.correctSuffix(anyString())).thenReturn(logHandlerMock);
        when(logHandlerMock.correctFormatter(anyString())).thenReturn(logHandlerMock);

        when(logHandlerBuilderMock.level(any(LogLevel.class))).thenReturn(logHandlerBuilderMock);
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
        verify(loggers, atLeast(0)).logger(anyString());

        verify(loggerMock, atLeast(0)).isDeployed();
        verify(loggerMock, atLeast(0)).level();

        verifyNoMoreInteractions(loggerMock);
        verifyNoMoreInteractions(loggers);
    }

    @After
    public void afterLogHandlers() {
        verify(loggers, atLeast(0)).handler(any(LoggingHandlerType.class), anyString());

        verify(logHandlerMock, atLeast(0)).isDeployed();

        verify(logHandlerMock, atLeast(0)).level();
        verify(logHandlerMock, atLeast(0)).file();
        verify(logHandlerMock, atLeast(0)).suffix();
        verify(logHandlerMock, atLeast(0)).formatter();

        verify(logHandlerMock, atLeast(0)).correctLevel(any(LogLevel.class));
        verify(logHandlerMock, atLeast(0)).correctFile(anyString());
        verify(logHandlerMock, atLeast(0)).correctSuffix(anyString());
        verify(logHandlerMock, atLeast(0)).correctFormatter(anyString());

        verifyNoMoreInteractions(logHandlerMock);
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

            when(loggers.logger(category)).thenReturn(loggerMock);
            when(loggerMock.isDeployed()).thenReturn(false);
        }

        public LoggerFixture level(LogLevel level) {
            when(loggerMock.level()).thenReturn(level);
            this.level = level;
            return this;
        }

        public LoggerFixture exists() {
            when(loggers.logger(category)).thenReturn(loggerMock);
            when(loggerMock.isDeployed()).thenReturn(true);
            return this;
        }

        public LoggerResource verifyLogger() { return verify(loggerMock); }
    }


    public LogHandlerFixture givenLogHandler(LoggingHandlerType type, String name) {
        return new LogHandlerFixture(type, name);
    }

    public class LogHandlerFixture {
        private final LoggingHandlerType type;
        private final String name;
        private LogLevel level;
        private String file;
        private String suffix;
        private String formatter;

        public LogHandlerFixture(LoggingHandlerType type, String name) {
            this.type = type;
            this.name = name;

            when(loggers.handler(type, name)).thenReturn(logHandlerMock);
        }

        public LogHandlerFixture level(LogLevel level) {
            this.level = level;
            when(logHandlerMock.level()).thenReturn(level);
            return this;
        }

        public LogHandlerFixture file(String file) {
            this.file = file;
            when(logHandlerMock.file()).thenReturn(file);
            return this;
        }

        public LogHandlerFixture suffix(String suffix) {
            this.suffix = suffix;
            when(logHandlerMock.suffix()).thenReturn(suffix);
            return this;
        }

        public LogHandlerFixture formatter(String formatter) {
            this.formatter = formatter;
            when(logHandlerMock.formatter()).thenReturn(formatter);
            return this;
        }

        public LogHandlerFixture deployed() {
            when(logHandlerMock.isDeployed()).thenReturn(true);
            return this;
        }

        public LogHandler verifyUpdated() {
            verify(loggers).handler(type, name);
            return verify(logHandlerMock);
        }

        public void verifyAdded() {
            verify(loggers).handler(type, name);
            verify(logHandlerMock).toBuilder();
            verify(logHandlerBuilderMock).level(level);
            verify(logHandlerBuilderMock).file(file);
            verify(logHandlerBuilderMock).suffix(suffix);
            verify(logHandlerBuilderMock).formatter(formatter);
            verify(logHandlerMock).add();
        }
    }
}
