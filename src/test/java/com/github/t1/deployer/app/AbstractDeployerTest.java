package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.container.LogHandler.LogHandlerBuilder;
import com.github.t1.deployer.container.LoggerResource.LoggerResourceBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.log.LogLevel;
import com.github.t1.testtools.SystemPropertiesRule;
import lombok.*;
import org.junit.*;
import org.mockito.*;

import javax.enterprise.inject.Instance;
import javax.validation.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AbstractDeployerTest {
    @Rule public SystemPropertiesRule systemProperties = new SystemPropertiesRule();

    @InjectMocks Deployer deployer;

    @Spy Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    @Spy private Audits audits;
    @Mock Instance<Audits> auditsInstance;

    @Mock Repository repository;

    @Mock DeploymentContainer deployments;
    @Mock LoggerContainer loggers;
    @Mock LoggerResource loggerMock;
    @Mock LoggerResourceBuilder loggerBuilderMock;
    @Mock LogHandler logHandlerMock;
    @Mock LogHandlerBuilder logHandlerBuilderMock;

    private final List<Deployment> allDeployments = new ArrayList<>();

    @Before
    public void before() {
        when(auditsInstance.get()).thenReturn(audits);


        when(deployments.getAllDeployments()).then(invocation -> allDeployments);


        when(loggerMock.toBuilder()).thenReturn(loggerBuilderMock);
        when(loggerBuilderMock.level(any(LogLevel.class))).thenReturn(loggerBuilderMock);
        when(loggerBuilderMock.level(any(LogLevel.class))).thenReturn(loggerBuilderMock);
        when(loggerBuilderMock.build()).thenReturn(loggerMock);


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
        verify(loggerMock, atLeast(0)).category();
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

                when(repository.getByChecksum(fakeChecksumFor(contextRoot(), version))).then(i -> artifact());
                when(repository.buildArtifact(groupId(), artifactId(), version, type)).then(i -> artifact());
                when(repository.buildArtifact(groupId(), artifactId(), Version.ANY, type))
                        .then(i -> artifact(Version.ANY));
            }

            public VersionFixture version(String version) { return ArtifactFixture.this.version(version); }

            public VersionFixture named(String name) {
                ArtifactFixture.this.named(name);
                return this;
            }

            public DeploymentName deploymentName() { return ArtifactFixture.this.deploymentName(); }

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


            public ArtifactAuditBuilder artifactAudit() { return ArtifactAudit.of(artifact()).name(deploymentName()); }


            public ArtifactFixture and() { return ArtifactFixture.this; }


            public void verifyDeployed(Audits audits) {
                verify(deployments).deploy(deploymentName(), inputStream());
                assertThat(audits.asList()).containsExactly(artifactAudit().added());
            }

            public void verifyRedeployed(Audits audits) {
                verify(deployments).redeploy(deploymentName(), inputStream());
                assertThat(audits.asList()).containsExactly(artifactAudit().updated());
            }

            public void verifyUndeployed(Audits audits) {
                verify(deployments).undeploy(deploymentName());
                assertThat(audits.asList()).containsExactly(artifactAudit().removed());
            }
        }
    }


    public LoggerFixture givenLogger(String name) { return new LoggerFixture(name); }

    @Getter
    public class LoggerFixture {
        private final String category;
        private LogLevel level;

        public LoggerFixture(String category) {
            this.category = category;

            when(loggerMock.category()).thenReturn(category);

            when(loggers.logger(category)).thenReturn(loggerMock);
            when(loggerMock.isDeployed()).thenReturn(false);
        }

        public LoggerFixture level(LogLevel level) {
            when(loggerMock.level()).thenReturn(level);
            this.level = level;
            return this;
        }

        public LoggerFixture deployed() {
            when(loggers.logger(category)).thenReturn(loggerMock);
            when(loggerMock.isDeployed()).thenReturn(true);
            return this;
        }

        public void verifyAdded(Audits audits) {
            verify(loggerMock).toBuilder();
            verify(loggerBuilderMock).level(level);
            verify(loggerBuilderMock).build();
            verify(loggerMock).add();
            assertThat(audits.asList()).containsExactly(LoggerAudit.of(getCategory()).level(getLevel()).added());
        }

        public void verifyChanged(Audits audits) {
            verify(loggerMock).correctLevel(level);
            assertThat(audits.asList()).containsExactly(LoggerAudit.of(getCategory()).level(getLevel()).updated());
        }

        public void verifyRemoved(Audits audits) {
            verify(loggerMock).remove();
            assertThat(audits.asList()).containsExactly(LoggerAudit.of(getCategory()).level(getLevel()).removed());
        }
    }


    public LogHandlerFixture givenLogHandler(LoggingHandlerType type, String name) {
        return new LogHandlerFixture(type, name);
    }

    @Getter
    public class LogHandlerFixture {
        private final LoggingHandlerType type;
        private final String name;
        private LogLevel level;
        private String file;
        private String suffix = ".yyyy-MM-dd";
        private String formatter;

        public LogHandlerFixture(LoggingHandlerType type, String name) {
            this.type = type;
            this.name = name;

            when(loggers.handler(type, name)).thenReturn(logHandlerMock);
            when(logHandlerMock.level()).then(i -> level);
            when(logHandlerMock.file()).then(i -> file);
            when(logHandlerMock.suffix()).then(i -> suffix);
            when(logHandlerMock.formatter()).then(i -> formatter);
        }

        public LogHandlerFixture level(LogLevel level) {
            this.level = level;
            return this;
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

        public LogHandlerFixture deployed() {
            when(logHandlerMock.isDeployed()).thenReturn(true);
            return this;
        }


        public void verifyUpdatedLogLevel(Audits audits) {
            verifyLogHandler().correctLevel(level);
            assertThat(audits.asList()).isEmpty();
        }

        public void verifyUpdatedFile(Audits audits) {
            verifyLogHandler().correctFile(file);
            assertThat(audits.asList()).isEmpty();
        }

        public void verifyUpdatedSuffix(Audits audits) {
            verifyLogHandler().correctSuffix(suffix);
            assertThat(audits.asList()).isEmpty();
        }

        public void verifyUpdatedFormatter(Audits audits) {
            verifyLogHandler().correctFormatter(formatter);
            assertThat(audits.asList()).isEmpty();
        }

        public LogHandler verifyLogHandler() {
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
            verify(logHandlerBuilderMock).build();
            verify(logHandlerMock).add();
        }
    }


    public static Set<ConstraintViolation<?>> unpackViolations(Throwable thrown) {
        assertThat(thrown).isInstanceOf(WebApplicationException.class);
        Response response = ((WebApplicationException) thrown).getResponse();
        assertThat(response.getStatusInfo()).isEqualTo(BAD_REQUEST);
        //noinspection unchecked
        return (Set<ConstraintViolation<?>>) response.getEntity();
    }

    public static String pathString(ConstraintViolation<?> v) {return v.getPropertyPath().toString();}
}
