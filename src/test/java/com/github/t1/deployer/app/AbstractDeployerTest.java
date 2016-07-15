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
import java.io.InputStream;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AbstractDeployerTest {
    @Rule public SystemPropertiesRule systemProperties = new SystemPropertiesRule();

    @InjectMocks Deployer deployer;

    @Spy private Audits audits;
    @Mock Instance<Audits> auditsInstance;

    @Mock Repository repository;

    @Mock ArtifactContainer artifacts;
    @Mock LoggerContainer loggers;
    @Mock LoggerResource loggerMock;
    @Mock LoggerResourceBuilder loggerBuilderMock;
    @Mock LogHandler logHandlerMock;
    @Mock LogHandlerBuilder logHandlerBuilderMock;

    private final List<Deployment> allDeployments = new ArrayList<>();

    @Before
    public void before() {
        when(auditsInstance.get()).thenReturn(audits);


        when(artifacts.getAllArtifacts()).then(invocation -> allDeployments);


        when(loggerMock.toBuilder()).thenReturn(loggerBuilderMock);
        when(loggerMock.addLoggerHandler(any(LogHandlerName.class))).thenReturn(loggerMock);
        when(loggerMock.removeLoggerHandler(any(LogHandlerName.class))).thenReturn(loggerMock);
        when(loggerBuilderMock.level(any(LogLevel.class))).thenReturn(loggerBuilderMock);
        when(loggerBuilderMock.useParentHandlers(any(Boolean.class))).thenReturn(loggerBuilderMock);
        when(loggerBuilderMock.handler(any(LogHandlerName.class))).thenReturn(loggerBuilderMock);
        when(loggerBuilderMock.handlers(anyListOf(LogHandlerName.class))).thenReturn(loggerBuilderMock);
        when(loggerBuilderMock.build()).thenReturn(loggerMock);


        when(logHandlerMock.toBuilder()).thenReturn(logHandlerBuilderMock);

        when(logHandlerMock.correctLevel(any(LogLevel.class))).thenReturn(logHandlerMock);
        when(logHandlerMock.correctFile(any(String.class))).thenReturn(logHandlerMock);
        when(logHandlerMock.correctSuffix(any(String.class))).thenReturn(logHandlerMock);
        when(logHandlerMock.correctFormatter(any(String.class), any(String.class))).thenReturn(logHandlerMock);

        when(logHandlerBuilderMock.level(any(LogLevel.class))).thenReturn(logHandlerBuilderMock);
        when(logHandlerBuilderMock.file(any(String.class))).thenReturn(logHandlerBuilderMock);
        when(logHandlerBuilderMock.suffix(any(String.class))).thenReturn(logHandlerBuilderMock);
        when(logHandlerBuilderMock.format(any(String.class))).thenReturn(logHandlerBuilderMock);
        when(logHandlerBuilderMock.formatter(any(String.class))).thenReturn(logHandlerBuilderMock);

        when(logHandlerBuilderMock.build()).thenReturn(logHandlerMock);
    }

    @After
    public void afterArtifacts() {
        verify(artifacts, atLeast(0)).getAllArtifacts();
        verify(artifacts, atLeast(0)).hasDeployment(any(DeploymentName.class));
        verify(artifacts, atLeast(0)).getDeployment(any(DeploymentName.class));

        verifyNoMoreInteractions(artifacts);
    }

    @After
    public void afterLoggers() {
        verify(loggers, atLeast(0)).logger(any(LoggerCategory.class));

        verify(loggerMock, atLeast(0)).isDeployed();
        verify(loggerMock, atLeast(0)).category();
        verify(loggerMock, atLeast(0)).isRoot();
        verify(loggerMock, atLeast(0)).handlers();
        verify(loggerMock, atLeast(0)).useParentHandlers();
        verify(loggerMock, atLeast(0)).level();

        verifyNoMoreInteractions(loggerMock);
        verifyNoMoreInteractions(loggers);
    }

    @After
    public void afterLogHandlers() {
        verify(loggers, atLeast(0)).handler(any(LoggingHandlerType.class), any(LogHandlerName.class));

        verify(logHandlerMock, atLeast(0)).isDeployed();

        verify(logHandlerMock, atLeast(0)).level();
        verify(logHandlerMock, atLeast(0)).file();
        verify(logHandlerMock, atLeast(0)).suffix();
        verify(logHandlerMock, atLeast(0)).format();
        verify(logHandlerMock, atLeast(0)).formatter();

        verify(logHandlerMock, atLeast(0)).correctLevel(any(LogLevel.class));
        verify(logHandlerMock, atLeast(0)).correctFile(any(String.class));
        verify(logHandlerMock, atLeast(0)).correctSuffix(any(String.class));
        verify(logHandlerMock, atLeast(0)).correctFormatter(any(String.class), any(String.class));

        verifyNoMoreInteractions(logHandlerMock);
    }


    public ArtifactFixtureBuilder givenArtifact(String groupId, String artifactId) {
        return new ArtifactFixtureBuilder(groupId, artifactId);
    }

    public ArtifactFixtureBuilder givenArtifact(String name) {
        return new ArtifactFixtureBuilder("org." + name, name + "-war");
    }

    @RequiredArgsConstructor
    public class ArtifactFixtureBuilder {
        private final String groupId;
        private final String artifactId;
        private String name;

        public ArtifactFixtureBuilder named(String name) {
            this.name = name;
            return this;
        }

        public GroupId groupId() { return new GroupId(groupId); }

        public ArtifactId artifactId() { return new ArtifactId(artifactId); }

        public ContextRoot contextRoot() { return new ContextRoot(artifactId); }

        public DeploymentName deploymentName() { return new DeploymentName((name == null) ? artifactId : name); }

        public ArtifactFixture version(String version) { return version(new Version(version)); }

        public ArtifactFixture version(Version version) { return new ArtifactFixture(version, war); }

        public ArtifactFixture version(String version, ArtifactType type) {
            return version(new Version(version), type);
        }

        public ArtifactFixture version(Version version, ArtifactType type) {
            return new ArtifactFixture(version, type);
        }

        public class ArtifactFixture {
            private final Version version;
            private final ArtifactType type;
            private Checksum checksum;
            private String contents;

            public ArtifactFixture(Version version, ArtifactType type) {
                this.version = version;
                this.type = type;

                when(repository.searchByChecksum(fakeChecksumFor(contextRoot(), version))).then(i -> artifact());
                when(repository.lookupArtifact(groupId(), artifactId(), version, type)).then(i -> artifact());
                when(repository.lookupArtifact(groupId(), artifactId(), Version.ANY, type))
                        .then(i -> artifact(Version.ANY));
            }

            public ArtifactFixture version(String version) { return ArtifactFixtureBuilder.this.version(version); }

            public ArtifactFixture named(String name) {
                ArtifactFixtureBuilder.this.named(name);
                return this;
            }

            public DeploymentName deploymentName() { return ArtifactFixtureBuilder.this.deploymentName(); }

            public ArtifactFixture checksum(Checksum checksum) {
                this.checksum = checksum;
                return this;
            }

            public Checksum checksum() {
                return (checksum == null)
                        ? fakeChecksumFor(contextRoot(), version)
                        : checksum;
            }

            public void containing(String contents) { this.contents = contents; }

            public ArtifactFixture deployed() {
                Deployment deployment = new Deployment(deploymentName(), contextRoot(), checksum());
                allDeployments.add(deployment);
                when(artifacts.hasDeployment(deploymentName())).thenReturn(true);
                when(artifacts.getDeployment(deploymentName())).thenReturn(deployment);
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


            public ArtifactFixtureBuilder and() { return ArtifactFixtureBuilder.this; }


            public void verifyDeployed(Audits audits) {
                verify(artifacts).deploy(deploymentName(), inputStream());
                assertThat(audits.asList()).containsExactly(artifactAudit().added());
            }

            public void verifyRedeployed(Audits audits) {
                verify(artifacts).redeploy(deploymentName(), inputStream());
                assertThat(audits.asList()).containsExactly(artifactAudit().updated());
            }

            public void verifyUndeployed(Audits audits) {
                verify(artifacts).undeploy(deploymentName());
                assertThat(audits.asList()).containsExactly(artifactAudit().removed());
            }
        }
    }


    public LoggerFixture givenLogger(String name) { return new LoggerFixture(name); }

    @Getter
    public class LoggerFixture {
        private final LoggerCategory category;
        private final List<String> handlers = new ArrayList<>();
        private LogLevel level;
        private Boolean useParentHandlers = true;

        public LoggerFixture(String category) {
            this.category = LoggerCategory.of(category);

            when(loggerMock.category()).then(i -> this.category);
            when(loggerMock.isRoot()).then(i -> this.category.isRoot());
            when(loggerMock.handlers()).then(i -> handlerNames());
            when(loggerMock.useParentHandlers()).then(i -> useParentHandlers);
            when(loggerMock.level()).then(i -> level);

            when(loggers.logger(this.category)).thenReturn(loggerMock);
            when(loggerMock.isDeployed()).thenReturn(false);
        }

        public LoggerFixture level(LogLevel level) {
            this.level = level;
            return this;
        }

        public LoggerFixture handler(String handlerName) {
            this.handlers.add(handlerName);
            return this;
        }

        public LoggerFixture useParentHandlers(Boolean useParentHandlers) {
            this.useParentHandlers = useParentHandlers;
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
            verify(loggerBuilderMock).handlers(handlerNames());
            verify(loggerBuilderMock).useParentHandlers(useParentHandlers);
            verify(loggerBuilderMock).build();
            verify(loggerMock).add();
            assertThat(audits.asList()).containsExactly(LoggerAudit.of(getCategory()).update(null, level).added());
        }

        public List<LogHandlerName> handlerNames() {
            return handlers.stream().map(LogHandlerName::new).collect(toList());
        }

        public void verifyUpdated(LogLevel oldLevel, Audits audits) {
            verify(loggerMock).writeLevel(level);
            assertThat(audits.asList()).containsExactly(
                    LoggerAudit.of(getCategory()).update(oldLevel, level).updated());
        }

        public void verifyRemoved(Audits audits) {
            verify(loggerMock).remove();
            assertThat(audits.asList()).containsExactly(LoggerAudit.of(getCategory()).removed());
        }

        public void verifyUpdatedUseParentHandlers(Audits audits) {
            verify(loggerMock).writeUseParentHandlers(useParentHandlers);
            assertThat(audits.asList()).containsExactly(LoggerAudit.of(getCategory()).updated());
        }

        public void verifyAddedHandlers(Audits audits, String name) {
            verify(loggerMock).addLoggerHandler(new LogHandlerName(name));
            assertThat(audits.asList()).containsExactly(LoggerAudit.of(getCategory()).updated());
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
        private String format;
        private String formatter;

        public LogHandlerFixture(LoggingHandlerType type, String name) {
            this.type = type;
            this.name = name;

            when(loggers.handler(type, new LogHandlerName(name))).thenReturn(logHandlerMock);
            when(logHandlerMock.level()).then(i -> level);
            when(logHandlerMock.file()).then(i -> file);
            when(logHandlerMock.suffix()).then(i -> suffix);
            when(logHandlerMock.format()).then(i -> format);
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

        public LogHandlerFixture format(String format) {
            this.format = format;
            this.formatter = null;
            return this;
        }

        public LogHandlerFixture formatter(String formatter) {
            this.format = null;
            this.formatter = formatter;
            return this;
        }

        public LogHandlerFixture deployed() {
            when(logHandlerMock.isDeployed()).thenReturn(true);
            return this;
        }


        public void verifyUpdatedLogLevel(Audits audits) {
            verifyLogHandler().correctLevel(level);
            assertThat(audits.asList()).isEmpty(); // TODO implement audit
        }

        public void verifyUpdatedFile(Audits audits) {
            verifyLogHandler().correctFile(file);
            assertThat(audits.asList()).isEmpty(); // TODO implement audit
        }

        public void verifyUpdatedSuffix(Audits audits) {
            verifyLogHandler().correctSuffix(suffix);
            assertThat(audits.asList()).isEmpty(); // TODO implement audit
        }

        public void verifyUpdatedFormatter(Audits audits) {
            verifyLogHandler().correctFormatter(format, formatter);
            assertThat(audits.asList()).isEmpty(); // TODO implement audit
        }

        public LogHandler verifyLogHandler() {
            verify(loggers).handler(type, new LogHandlerName(name));
            return verify(logHandlerMock);
        }

        public void verifyAdded(Audits audits) {
            verifyLogHandler().toBuilder();
            verify(logHandlerBuilderMock).level(level);
            verify(logHandlerBuilderMock).file(file);
            verify(logHandlerBuilderMock).suffix(suffix);
            verify(logHandlerBuilderMock).format(format);
            verify(logHandlerBuilderMock).formatter(formatter);
            verify(logHandlerBuilderMock).build();
            verify(logHandlerMock).add();

            assertThat(audits.asList()).isEmpty(); // TODO implement audit
        }

        public void verifyRemoved(Audits audits) {
            verify(logHandlerMock).remove();
            assertThat(audits.asList()).isEmpty(); // TODO implement audit
            // assertThat(audits.asList()).containsExactly(LogHandlerAudit.of(getCategory()).level(getLevel()).removed());
        }
    }
}
