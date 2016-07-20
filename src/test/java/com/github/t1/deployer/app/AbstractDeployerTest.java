package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.app.Audit.LogHandlerAudit.LogHandlerAuditBuilder;
import com.github.t1.deployer.app.ConfigurationPlan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.log.LogLevel;
import com.github.t1.testtools.SystemPropertiesRule;
import lombok.*;
import org.jboss.as.controller.client.helpers.standalone.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.mockito.*;

import javax.enterprise.inject.Instance;
import java.io.InputStream;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.Tools.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AbstractDeployerTest {
    @Rule public SystemPropertiesRule systemProperties = new SystemPropertiesRule();

    @InjectMocks Deployer deployer;

    @Spy private Audits audits;
    @Mock Instance<Audits> auditsInstance;

    @Mock Repository repository;

    @Spy Container container;
    @Mock CLI cli;
    @Mock ServerDeploymentManager deploymentManager;
    @Mock InitialDeploymentPlanBuilder planBuilder;
    @Mock AddDeploymentPlanBuilder addPlanBuilder;
    @Mock ReplaceDeploymentPlanBuilder replacePlanBuilder;
    @Mock UndeployDeploymentPlanBuilder undeployPlanBuilder;
    @Mock DeploymentPlan deploymentPlan;
    @Mock ServerDeploymentPlanResult planResult;

    private List<String> allDeployments = new ArrayList<>();
    private List<String> allLoggers = new ArrayList<>();

    @Before
    public void before() {
        container.cli = cli;
        when(auditsInstance.get()).thenReturn(audits);
        when(cli.openServerDeploymentManager()).then(i -> deploymentManager);

        when(deploymentManager.newDeploymentPlan()).then(i -> planBuilder);

        when(planBuilder.add(any(String.class), any(InputStream.class))).then(i -> addPlanBuilder);
        when(addPlanBuilder.deploy(any(String.class))).then(i -> addPlanBuilder);
        when(addPlanBuilder.build()).then(i -> deploymentPlan);

        when(planBuilder.replace(any(String.class), any(InputStream.class))).then(i -> replacePlanBuilder);
        when(replacePlanBuilder.build()).then(i -> deploymentPlan);

        when(planBuilder.undeploy(any(String.class))).then(i -> undeployPlanBuilder);
        when(undeployPlanBuilder.remove(any(String.class))).then(i -> undeployPlanBuilder);
        when(undeployPlanBuilder.build()).then(i -> deploymentPlan);

        when(deploymentManager.execute(any(DeploymentPlan.class))).then(i -> constantFuture(planResult));

        when(cli.executeRaw(readResource("logging", "root-logger", "ROOT"))).then(i -> rootLoggerResponse());
        when(cli.execute(readResource("logging", "logger", "*"))).then(
                i -> toModelNode(allLoggers.stream().collect(joining(",", "[", "]"))));
        when(cli.execute(readResource(null, "deployment", "*"))).then(i -> allDeploymentsResponse());
    }

    private ModelNode rootLoggerResponse() {
        return toModelNode(""
                + "{\n"
                + "    \"outcome\" => \"success\",\n"
                + "    \"result\" => {\n"
                + "        \"filter\" => undefined,\n"
                + "        \"filter-spec\" => undefined,\n"
                + "        \"handlers\" => [\n"
                + "            \"CONSOLE\",\n"
                + "            \"FILE\"\n"
                + "        ],\n"
                + "        \"level\" => \"INFO\"\n"
                + "    }\n"
                + "}");
    }

    private ModelNode allDeploymentsResponse() {
        return toModelNode(allDeployments.stream().collect(joining(",", "[", "]")));
    }

    @After
    public void after() {
        verify(cli, atLeast(0)).executeRaw(any(ModelNode.class));
        verify(cli, atLeast(0)).openServerDeploymentManager();
        verify(cli, atLeast(0)).execute(readResource(null, "deployment", "*"));
        verify(cli, atLeast(0)).execute(readResource("logging", "logger", "*"));

        verifyNoMoreInteractions(cli);
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

    public class ArtifactFixtureBuilder {
        private final String name;
        private String groupId;
        private String artifactId;
        private ArtifactFixture deployed;

        public ArtifactFixtureBuilder(String name) {
            this.name = name;

            when(cli.executeRaw(readResource(null, "deployment", name))).then(
                    i -> (deployed == null)
                            ? notDeployedNode(null, "deployment", name)
                            : toModelNode(deployed.deployedNode()));
        }

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
            @NonNull @Getter private final Version version;
            @NonNull @Getter private final ArtifactType type;
            @Getter private Checksum checksum;
            private String contents;

            public ArtifactFixture(Version version, ArtifactType type) {
                this.version = version;
                this.type = type;

                when(repository.lookupArtifact(groupId(), artifactId(), version, type)).then(i -> artifact());
                when(repository.lookupArtifact(groupId(), artifactId(), Version.ANY, type))
                        .then(i -> artifact(Version.ANY));
                checksum(fakeChecksumFor(deploymentName(), version));
            }

            public String deployedNode() {
                return ""
                        + "{\n"
                        + "    'outcome' => 'success',\n"
                        + "    'result' => {\n"
                        + "        'content' => [{'hash' => bytes {" + checksum.hexByteArray() + "}}],\n"
                        + "        'enabled' => true,\n"
                        + "        'name' => '" + deploymentName() + "',\n"
                        + "        'persistent' => true,\n"
                        + "        'runtime-name' => '" + deploymentName() + "',\n"
                        + "        'subdeployment' => undefined,\n"
                        + "        'subsystem' => {'web' => {\n"
                        + "            'context-root' => '" + deploymentName() + "',\n"
                        + "            'virtual-host' => 'default-host',\n"
                        + "            'servlet' => {'javax.ws.rs.core.Application' => {\n"
                        + "                'servlet-class' => 'org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher',\n"
                        + "                'servlet-name' => 'javax.ws.rs.core.Application'\n"
                        + "            }}\n"
                        + "        }}\n"
                        + "    }\n"
                        + "}";
            }

            public ArtifactFixture version(String version) { return ArtifactFixtureBuilder.this.version(version); }

            public DeploymentName deploymentName() { return ArtifactFixtureBuilder.this.deploymentName(); }

            public ArtifactFixture checksum(Checksum checksum) {
                this.checksum = checksum;
                when(repository.searchByChecksum(checksum)).then(i -> artifact());
                return this;
            }

            public void containing(String contents) { this.contents = contents; }

            public ArtifactFixture deployed() {
                if (deployed != null)
                    throw new RuntimeException("already have deployed " + name + ":" + version);
                deployed = this;
                allDeployments.add(deployedNode());
                return this;
            }

            public InputStream inputStream() {
                return (contents == null)
                        ? inputStreamFor(deploymentName(), version)
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
                        .checksum(checksum)
                        .inputStreamSupplier(this::inputStream)
                        .build();
            }


            public ArtifactAuditBuilder artifactAudit() { return ArtifactAudit.builder().name(deploymentName()); }


            public ArtifactFixtureBuilder and() { return ArtifactFixtureBuilder.this; }

            public void verifyDeployed(Audits audits) {
                verifyAddExecuted();
                assertThat(audits.getAudits()).containsExactly(addedAudit());
            }

            public void verifyAddExecuted() {
                // verify(cli).execute(toModelNode(""
                //         + "{\n"
                //         + "    'address' => [('deployment' => '" + name + "')],"
                //         + "    'operation' => 'add'\n"
                //         + "}"));
            }

            public Audit addedAudit() {
                return artifactAudit()
                        .change("group-id", null, groupId)
                        .change("artifact-id", null, artifactId)
                        .change("version", null, version)
                        .change("type", null, type)
                        .change("checksum", null, checksum)
                        .added();
            }

            public void verifyRedeployed(Audits audits) {
                // FIXME verify(artifacts).redeploy(deploymentName(), inputStream());
                Checksum oldChecksum = (deployed == null) ? null : deployed.checksum;
                Version oldVersion = (deployed == null) ? null : deployed.version;
                assertThat(audits.getAudits()).containsExactly(artifactAudit()
                        .change("checksum", oldChecksum, checksum)
                        .change("version", oldVersion, version)
                        .changed());
            }

            public void verifyRemoved(Audits audits) {
                verifyUndeployExecuted();
                assertThat(audits.getAudits()).containsExactly(removedAudit());
            }

            public void verifyUndeployExecuted() {
                verify(planBuilder).undeploy(name);
                verify(undeployPlanBuilder).remove(name);
            }

            public Audit removedAudit() {
                return artifactAudit()
                        .change("group-id", groupId, null)
                        .change("artifact-id", artifactId, null)
                        .change("version", version, null)
                        .change("type", type, null)
                        .change("checksum", checksum, null)
                        .removed();
            }

            public DeploymentConfig asConfig() {
                return DeploymentConfig
                        .builder()
                        .name(deploymentName())
                        .groupId(groupId())
                        .artifactId(artifactId())
                        .version(version)
                        .type(type)
                        .state(deployed == null ? DeploymentState.undeployed : DeploymentState.deployed)
                        .build();
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

        public LoggerFixture(@NonNull String category) {
            this.category = LoggerCategory.of(category);

            when(cli.executeRaw(readResource("logging", "logger", category))).then(
                    i -> deployed ? deployedNode() : notDeployedNode("logging", "logger", category));
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
            allLoggers.add(""
                    + "{\n"
                    + "    \"address\" => [\n"
                    + "        (\"subsystem\" => \"logging\"),\n"
                    + "        (\"logger\" => \"" + category + "\")\n"
                    + "    ],\n"
                    + "    \"outcome\" => \"success\",\n"
                    + "    \"result\" => {\n"
                    + "        \"category\" => \"" + category + "\",\n"
                    + "        \"filter\" => undefined,\n"
                    + "        \"filter-spec\" => undefined,\n"
                    + "        \"handlers\" => undefined,\n"
                    + ((level == null) ? "" : "        \"level\" => \"" + level + "\",\n")
                    + "        \"use-parent-handlers\" => " + useParentHandlers + "\n"
                    + "    }\n"
                    + "}\n");
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
            verify(cli).execute(toModelNode("{\n"
                    + loggerAddress()
                    + "    'operation' => 'add',\n"
                    + ((level == null) ? "" : "    'level' => '" + level + "',\n")
                    + (handlers.isEmpty() ? "" : "    'handlers' => " + handlersArrayNode() + ",\n")
                    + "    'use-parent-handlers' => " + useParentHandlers + "\n"
                    + "}"));
            AuditBuilder audit = LoggerAudit
                    .of(getCategory())
                    .change("level", null, level)
                    .change("useParentHandlers", null, useParentHandlers);
            for (LogHandlerName handler : handlerNames())
                audit.change("handler", null, handler);
            assertThat(audits.getAudits()).containsExactly(audit.added());
        }

        public List<LogHandlerName> handlerNames() {
            return handlers.stream().map(LogHandlerName::new).collect(toList());
        }

        public ModelNode buildRequest() {
            return toModelNode("{" + loggerAddress() + "}");
        }

        public void verifyUpdated(LogLevel oldLevel, Audits audits) {
            verify(cli).writeAttribute(buildRequest(), "level", level.toString());
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("level", oldLevel, level).changed());
        }

        public void verifyRemoved(Audits audits) {
            verify(cli).execute(toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'remove'\n"
                    + "}"));
            AuditBuilder audit = LoggerAudit
                    .of(getCategory())
                    .change("level", level, null)
                    .change("useParentHandlers", useParentHandlers, null);
            for (LogHandlerName handler : handlerNames())
                audit.change("handler", handler, null);
            assertThat(audits.getAudits()).containsExactly(audit.removed());
        }

        public void verifyUpdatedUseParentHandlers(Boolean oldUseParentHandlers, Audits audits) {
            verify(cli).writeAttribute(buildRequest(), "use-parent-handlers", useParentHandlers);
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("useParentHandlers", oldUseParentHandlers, useParentHandlers)
                               .changed());
        }

        public void verifyAddedHandler(Audits audits, String name) {
            LogHandlerName handlerName = new LogHandlerName(name);
            verify(cli).execute(toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'add-handler',\n"
                    + "    'name' => '" + name + "'\n"
                    + "}"));
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("handler", null, handlerName).changed());
        }

        public void verifyRemovedHandler(Audits audits, String name) {
            LogHandlerName handlerName = new LogHandlerName(name);
            verify(cli).execute(toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'remove-handler',\n"
                    + "    'name' => '" + name + "'\n"
                    + "}"));
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("handler", handlerName, null).changed());
        }

        public LoggerConfig asConfig() {
            return LoggerConfig
                    .builder()
                    .category(category)
                    .state(deployed ? DeploymentState.deployed : DeploymentState.undeployed)
                    .level(level)
                    .handlers(handlerNames())
                    .useParentHandlers(useParentHandlers)
                    .build();
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
                + ((subsystem == null) ? "" : "        ('subsystem' => '" + subsystem + "'),\n")
                + "        ('" + type + "' => '" + name + "')\n"
                + "    ],\n";
    }

    private static ModelNode notDeployedNode(String subsystem, Object type, Object name) {
        return ModelNode.fromString("{\n"
                + "    \"outcome\" => \"failed\",\n"
                + "    \"failure-description\" => \"WFLYCTL0216: Management resource '[\n"
                + ((subsystem == null) ? "" : "    (\\\"subsystem\\\" => \\\"" + subsystem + "\\\"),\n")
                + "    (\\\"" + type + "\\\" => \\\"" + name + "\\\")\n"
                + "]' not found\",\n"
                + "    \"rolled-back\" => true\n"
                + "}");
    }

    public static ModelNode toModelNode(String string) { return ModelNode.fromString(string.replace('\'', '\"')); }


    public LogHandlerFixture givenLogHandler(LoggingHandlerType type, String name) {
        return new LogHandlerFixture(type, name);
    }

    @Getter
    public class LogHandlerFixture {
        private final LoggingHandlerType type;
        private final LogHandlerName name;
        private final LogHandlerAuditBuilder audit;
        private LogLevel level;
        private String file;
        private String suffix = ".yyyy-MM-dd";
        private String format;
        private String formatter;
        private boolean deployed;

        public LogHandlerFixture(LoggingHandlerType type, String name) {
            this.type = type;
            this.name = new LogHandlerName(name);
            this.audit = LogHandlerAudit.builder().type(this.type).name(this.name);

            when(cli.executeRaw(readResource("logging", type.getTypeName(), name))).then(
                    i -> deployed ? deployedNode() : notDeployedNode("logging", type, name));
        }

        public ModelNode deployedNode() {
            String string = ""
                    + "{\n"
                    + "    'outcome' => 'success',\n"
                    + "    'result' => {\n"
                    + ((level == null) ? "" : "        'level' => '" + level + "',\n")
                    + ((file == null) ? "" : "        'file' => '" + file + "',\n")
                    + ((suffix == null) ? "" : "        'suffix' => '" + suffix + "',\n")
                    + ((format == null) ? "" : "        'format' => '" + format + "',\n")
                    + ((formatter == null) ? "" : "        'named-formatter' => '" + formatter + "',\n")
                    + "    }\n"
                    + "}";
            return toModelNode(string);
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
            this.deployed = true;
            return this;
        }


        private ModelNode buildRequest() {
            return toModelNode("{" + logHandlerAddress() + "}");
        }

        private String logHandlerAddress() { return address("logging", type.getTypeName(), name); }


        public <T> void verifyChange(String name, T oldValue, T newValue) {
            verifyWriteAttribute(name, newValue);
            expectChange(name, oldValue, newValue);
        }

        public <T> void verifyWriteAttribute(String name, T value) {
            verify(cli).writeAttribute(buildRequest(), name, toStringOrNull(value));
        }

        public <T> void expectChange(String name, T oldValue, T newValue) { audit.change(name, oldValue, newValue); }

        public void verifyChanged(Audits audits) {
            assertThat(audits.getAudits()).containsExactly(this.audit.changed());
        }

        public void verifyAdded(Audits audits) {
            verify(cli).execute(toModelNode("{\n"
                    + logHandlerAddress()
                    + "    'operation' => 'add'"
                    + ((level == null) ? "" : ",\n    'level' => '" + level + "'")
                    + ((file == null) ? "" : ",\n    'file' => {\n"
                    + "        'path' => '" + file + "',\n"
                    + "        'relative-to' => 'jboss.server.log.dir'\n"
                    + "    }")
                    + ((suffix == null) ? "" : ",\n    'suffix' => '" + suffix + "'")
                    + ((format == null) ? "" : ",\n    'format' => '" + format + "'")
                    + ((formatter == null) ? "" : ",\n    'named-formatter' => '" + formatter + "'")
                    + "\n}"));
            if (level != null)
                audit.change("level", null, level);
            if (file != null)
                audit.change("file", null, file);
            if (suffix != null)
                audit.change("suffix", null, suffix);
            if (format != null)
                audit.change("format", null, format);
            if (formatter != null)
                audit.change("formatter", null, formatter);
            assertThat(audits.getAudits()).containsExactly(audit.added());
        }

        public void verifyRemoved(Audits audits) {
            verify(cli).execute(toModelNode("{\n"
                    + logHandlerAddress()
                    + "    'operation' => 'remove'\n"
                    + "}"));
            audit.change("level", this.level, null)
                 .change("file", this.file, null)
                 .change("suffix", this.suffix, null);
            if (this.format != null)
                audit.change("format", this.format, null);
            if (this.formatter != null)
                audit.change("formatter", this.formatter, null);
            assertThat(audits.getAudits()).containsExactly(audit.removed());
        }
    }
}
