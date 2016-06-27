package com.github.t1.deployer.app;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.container.LogHandler.LogHandlerBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.log.LogLevel;
import com.github.t1.testtools.SystemPropertiesRule;
import lombok.*;
import org.junit.*;
import org.mockito.*;

import javax.validation.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AbstractDeployerTest {
    @Rule public SystemPropertiesRule systemProperties = new SystemPropertiesRule();

    @InjectMocks Deployer deployer;

    @Spy Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

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
        private final String groupId;
        private final String artifactId;
        private String name;

        public ArtifactFixture named(String name) {
            this.name = name;
            return this;
        }

        public GroupId groupId() { return new GroupId(groupId); }

        public ArtifactId artifactId() { return new ArtifactId(artifactId); }

        public ContextRoot contextRoot() { return new ContextRoot(artifactId); }

        public DeploymentName deploymentName() { return new DeploymentName((name == null) ? artifactId : name); }

        public VersionFixture version(String version) { return version(new Version(version)); }

        public VersionFixture version(Version version) { return new VersionFixture(version, war); }

        public VersionFixture version(String version, ArtifactType type) { return version(new Version(version), type); }

        public VersionFixture version(Version version, ArtifactType type) { return new VersionFixture(version, type); }

        public class VersionFixture {
            private final Version version;
            private final ArtifactType type;
            private Checksum checksum;
            private String contents;

            public VersionFixture(Version version, ArtifactType type) {
                this.version = version;
                this.type = type;

                when(repository.buildArtifact(groupId(), artifactId(), version, type)).then(i -> artifact());
                when(repository.buildArtifact(groupId(), artifactId(), Version.ANY, type))
                        .then(i -> artifact(Version.ANY));
            }

            public VersionFixture checksum(Checksum checksum) {
                this.checksum = checksum;
                return this;
            }

            public Checksum checksum() {
                return (checksum == null)
                        ? fakeChecksumFor(contextRoot(), version)
                        : checksum;
            }

            public void containing(String contents) { this.contents = contents; }

            public VersionFixture deployed() {
                Deployment deployment = new Deployment(deploymentName(), contextRoot(), checksum(), version);
                allDeployments.add(deployment);
                when(deployments.hasDeployment(deploymentName())).thenReturn(true);
                when(deployments.getDeployment(deploymentName())).thenReturn(deployment);
                return this;
            }

            public InputStream inputStream() {
                return (contents == null)
                        ? inputStreamFor(contextRoot(), version)
                        : new StringInputStream(contents);
            }

            public Artifact artifact() {
                return artifact(this.version);
            }

            private Artifact artifact(Version version) {
                return Artifact
                        .builder()
                        .groupId(groupId())
                        .artifactId(artifactId())
                        .version(version)
                        .type(type)
                        .checksum(checksum())
                        .inputStreamSupplier(this::inputStream)
                        .build();
            }

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


    public static Set<ConstraintViolation<?>> unpackViolations(Throwable thrown) {
        assertThat(thrown).isInstanceOf(WebApplicationException.class)
                          .hasMessage("HTTP 400 Bad Request");
        Response response = ((WebApplicationException) thrown).getResponse();
        //noinspection unchecked
        return (Set<ConstraintViolation<?>>) response.getEntity();
    }

    public static String pathString(ConstraintViolation<?> v) {return v.getPropertyPath().toString();}
}
