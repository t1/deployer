package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.container.LogHandler.LogHandlerBuilder;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.log.LogLevel;
import com.github.t1.testtools.SystemPropertiesRule;
import lombok.*;
import org.jboss.dmr.ModelNode;
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
    @Mock LogHandler logHandlerMock;
    @Mock LogHandlerBuilder logHandlerBuilderMock;

    private final List<Deployment> allDeployments = new ArrayList<>();

    @Before
    public void before() {
        when(auditsInstance.get()).thenReturn(audits);


        when(artifacts.getAllArtifacts()).then(invocation -> allDeployments);


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
        verify(loggers, atLeast(0)).executeRaw(any(ModelNode.class));

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


    public ArtifactFixtureBuilder givenArtifact(String name) {
        return givenArtifact(name, "org." + name, name);
    }

    public ArtifactFixtureBuilder givenArtifact(String groupId, String artifactId) {
        return givenArtifact(artifactId, groupId, artifactId);
    }

    public ArtifactFixtureBuilder givenArtifact(String name, GroupId groupId, ArtifactId artifactId) {
        return givenArtifact(name, groupId.getValue(), artifactId.getValue());
    }

    public ArtifactFixtureBuilder givenArtifact(String name, String groupId, String artifactId) {
        return new ArtifactFixtureBuilder(name).groupId(groupId).artifactId(artifactId);
    }

    @RequiredArgsConstructor
    public class ArtifactFixtureBuilder {
        private final String name;
        private String groupId;
        private String artifactId;

        public ArtifactFixtureBuilder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public GroupId groupId() { return new GroupId(groupId); }

        public ArtifactFixtureBuilder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

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

                when(repository.lookupArtifact(groupId(), artifactId(), version, type)).then(i -> artifact());
                when(repository.lookupArtifact(groupId(), artifactId(), Version.ANY, type))
                        .then(i -> artifact(Version.ANY));
                checksum(fakeChecksumFor(contextRoot(), version));
            }

            public ArtifactFixture version(String version) { return ArtifactFixtureBuilder.this.version(version); }

            public DeploymentName deploymentName() { return ArtifactFixtureBuilder.this.deploymentName(); }

            public ArtifactFixture checksum(Checksum checksum) {
                this.checksum = checksum;
                when(repository.searchByChecksum(checksum)).then(i -> artifact());
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

            public GroupId groupId() { return ArtifactFixtureBuilder.this.groupId(); }

            public ArtifactId artifactId() { return ArtifactFixtureBuilder.this.artifactId(); }

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
                assertThat(audits.getAudits()).containsExactly(artifactAudit().added());
            }

            public void verifyRedeployed(Audits audits) {
                verify(artifacts).redeploy(deploymentName(), inputStream());
                assertThat(audits.getAudits()).containsExactly(artifactAudit().changed());
            }

            public void verifyUndeployed(Audits audits) {
                verify(artifacts).undeploy(deploymentName());
                assertThat(audits.getAudits()).containsExactly(artifactAudit().removed());
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
        private boolean deployed;

        public LoggerFixture(String category) {
            this.category = LoggerCategory.of(category);

            when(loggers.logger(this.category)).then(i -> LoggerResource.builder(this.category, loggers));

            when(loggers.executeRaw(readResource("logging", "logger", category))).then(
                    i -> deployed ? deployedNode() : notDeployedNode());
        }

        public ModelNode deployedNode() {
            String string = ""
                    + "{\n"
                    + "    'outcome' => 'success',\n"
                    + "    'result' => {\n"
                    + "        'category' => " + ((category == null) ? "undefined" : "'" + category + "'") + ",\n"
                    + "        'filter' => undefined,\n"
                    + "        'filter-spec' => undefined,\n"
                    + (handlers.isEmpty() ? "" : "        'handlers' => " + handlersArrayNode() + ",\n")
                    + "        'level' => '" + level + "',\n"
                    + "        'use-parent-handlers' => " + useParentHandlers + "\n"
                    + "    }\n"
                    + "}";
            return toModelNode(string);
        }

        private ModelNode notDeployedNode() {
            return ModelNode.fromString("{\n"
                    + "    \"outcome\" => \"failed\",\n"
                    + "    \"failure-description\" => \"WFLYCTL0216: Management resource '[\n"
                    + "    (\\\"subsystem\\\" => \\\"logging\\\"),\n"
                    + "    (\\\"logger\\\" => \\\"" + category + "\\\")\n"
                    + "]' not found\",\n"
                    + "    \"rolled-back\" => true\n"
                    + "}");
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
            this.deployed = true;
            return this;
        }

        public String loggerAddress() { return address("logging", "logger", category); }

        private String handlersArrayNode() {
            if (handlers.size() == 1)
                return "['" + handlers.get(0) + "']";
            else
                return handlers.stream().collect(joining("',\n        '", "[\n        '", "'\n    ]"));
        }

        public void verifyAdded(Audits audits) {
            ArgumentCaptor<ModelNode> captor = ArgumentCaptor.forClass(ModelNode.class);
            verify(loggers).execute(captor.capture());
            ModelNode node = captor.getValue();
            assertThat(node.toString().replace('\"', '\'')).isEqualTo("{\n"
                    + loggerAddress()
                    + "    'operation' => 'add',\n"
                    + ((level == null) ? "" : "    'level' => '" + level + "',\n")
                    + (handlers.isEmpty() ? "" : "    'handlers' => " + handlersArrayNode() + ",\n")
                    + "    'use-parent-handlers' => " + useParentHandlers + "\n"
                    + "}");
            AuditBuilder audit = LoggerAudit
                    .of(getCategory())
                    .change("level", null, level)
                    .change("useParentHandlers", null, useParentHandlers);
            for (LogHandlerName handler : handlerNames())
                audit.change("handlers", null, handler);
            assertThat(audits.getAudits()).containsExactly(audit.added());
        }

        public List<LogHandlerName> handlerNames() {
            return handlers.stream().map(LogHandlerName::new).collect(toList());
        }

        public ModelNode buildRequest() {
            return toModelNode("{'address' => [\n"
                    + "    ('subsystem' => 'logging'),\n"
                    + "    ('logger' => '" + category + "')\n"
                    + "]}");
        }

        public void verifyUpdated(LogLevel oldLevel, Audits audits) {
            verify(loggers).writeAttribute(buildRequest(), "level", level.toString());
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("level", oldLevel, level).changed());
        }

        public void verifyRemoved(Audits audits) {
            verify(loggers).execute(toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'remove'\n"
                    + "}"));
            AuditBuilder audit = LoggerAudit
                    .of(getCategory())
                    .change("level", level, null)
                    .change("useParentHandlers", useParentHandlers, null);
            for (LogHandlerName handler : handlerNames())
                audit.change("handlers", handler, null);
            assertThat(audits.getAudits()).containsExactly(audit.removed());
        }

        public void verifyUpdatedUseParentHandlers(Boolean oldUseParentHandlers, Audits audits) {
            verify(loggers).writeAttribute(buildRequest(), "use-parent-handlers", useParentHandlers);
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("useParentHandlers", oldUseParentHandlers, useParentHandlers)
                               .changed());
        }

        public void verifyAddedHandler(Audits audits, String name) {
            LogHandlerName handlerName = new LogHandlerName(name);
            verify(loggers).execute(toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'add-handler',\n"
                    + "    'name' => '" + name + "'\n"
                    + "}"));
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("handlers", null, handlerName).changed());
        }

        public void verifyRemovedHandler(Audits audits, String name) {
            LogHandlerName handlerName = new LogHandlerName(name);
            verify(loggers).execute(toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'remove-handler',\n"
                    + "    'name' => '" + name + "'\n"
                    + "}"));
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("handlers", handlerName, null).changed());
        }
    }

    public static ModelNode readResource(String subsystem, String type, Object name) {
        return toModelNode(""
                + "{\n"
                + address(subsystem, type, name)
                + "    'operation' => 'read-resource',\n"
                + "    'recursive' => true\n"
                + "}");
    }

    private static String address(String subsystem, String type, Object name) {
        return ""
                + "    'address' => [\n"
                + "        ('subsystem' => '" + subsystem + "'),\n"
                + "        ('" + type + "' => '" + name + "')\n"
                + "    ],\n";
    }

    public static ModelNode toModelNode(String replace) { return ModelNode.fromString(replace.replace('\'', '\"')); }


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
            assertThat(audits.getAudits()).isEmpty(); // TODO implement audit
        }

        public void verifyUpdatedFile(Audits audits) {
            verifyLogHandler().correctFile(file);
            assertThat(audits.getAudits()).isEmpty(); // TODO implement audit
        }

        public void verifyUpdatedSuffix(Audits audits) {
            verifyLogHandler().correctSuffix(suffix);
            assertThat(audits.getAudits()).isEmpty(); // TODO implement audit
        }

        public void verifyUpdatedFormatter(Audits audits) {
            verifyLogHandler().correctFormatter(format, formatter);
            assertThat(audits.getAudits()).isEmpty(); // TODO implement audit
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

            assertThat(audits.getAudits()).isEmpty(); // TODO implement audit
        }

        public void verifyRemoved(Audits audits) {
            verify(logHandlerMock).remove();
            assertThat(audits.getAudits()).isEmpty(); // TODO implement audit
            // assertThat(audits.getAudits()).containsExactly(LogHandlerAudit.of(getCategory()).level(getLevel()).removed());
        }
    }
}
