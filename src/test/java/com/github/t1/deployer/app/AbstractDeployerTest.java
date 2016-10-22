package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.DeployableAudit.DeployableAuditBuilder;
import com.github.t1.deployer.app.Audit.LogHandlerAudit.LogHandlerAuditBuilder;
import com.github.t1.deployer.app.Plan.*;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.log.LogLevel;
import com.github.t1.testtools.*;
import lombok.*;
import org.jboss.as.controller.client.helpers.standalone.*;
import org.jboss.dmr.ModelNode;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import javax.enterprise.inject.Instance;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.*;

import static com.github.t1.deployer.app.DeployerBoundary.*;
import static com.github.t1.deployer.app.Plan.LogHandlerPlan.*;
import static com.github.t1.deployer.app.Plan.*;
import static com.github.t1.deployer.app.Trigger.mock;
import static com.github.t1.deployer.container.LogHandlerType.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static com.github.t1.deployer.tools.Tools.*;
import static com.github.t1.log.LogLevel.*;
import static java.lang.Boolean.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AbstractDeployerTest {
    @SneakyThrows(IOException.class)
    private static Path tempDir() { return Files.createTempDirectory("deployer.test"); }

    private final Path tempDir = tempDir();

    @Rule public SystemPropertiesRule systemProperties = new SystemPropertiesRule()
            .given("jboss.server.config.dir", tempDir);
    @Rule public FileMemento rootBundle = new FileMemento(() -> tempDir.resolve(ROOT_BUNDLE));

    @SneakyThrows(IOException.class)
    Audits deploy(String plan) {
        rootBundle.write(plan);
        return deployer.apply(mock, emptyMap());
    }


    @InjectMocks DeployerBoundary deployer;

    @Spy private LogHandlerDeployer logHandlerDeployer;
    @Spy private LoggerDeployer loggerDeployer;
    @Spy private DeployableDeployer deployableDeployer;
    @Mock Instance<AbstractDeployer> deployers;

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

    private final Map<VariableName, String> configuredVariables = new HashMap<>();
    private final List<String> managedResourceNames = new ArrayList<>();
    private final List<String> allDeployments = new ArrayList<>();
    private final List<String> allLoggers = new ArrayList<>();
    private final Map<LogHandlerType, List<String>> allLogHandlers = new LinkedHashMap<>();

    private final Map<String, List<Version>> versions = new LinkedHashMap<>();

    @Before
    public void before() {
        container.cli = cli;

        logHandlerDeployer.managedResourceNames
                = loggerDeployer.managedResourceNames
                = deployableDeployer.managedResourceNames
                = managedResourceNames;
        deployableDeployer.repository
                = repository;
        logHandlerDeployer.container
                = loggerDeployer.container
                = deployableDeployer.container
                = container;
        deployer.audits
                = logHandlerDeployer.audits
                = loggerDeployer.audits
                = deployableDeployer.audits
                = new Audits();
        deployer.configuredVariables = this.configuredVariables;

        //noinspection unchecked
        // doAnswer(i -> {
        //     asList(logHandlerDeployer, loggerDeployer, deployableDeployer)
        //             .forEach(i.<Consumer<AbstractDeployer>>getArgument(0));
        //     return null;
        // }).when(deployers).forEach(any(Consumer.class));

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

        when(cli.executeRaw(readResource(rootLogger()))).then(i -> rootLoggerResponse());
        when(cli.execute(readResource("logging", "logger", "*"))).then(
                i -> toModelNode(allLoggers.stream().collect(joining(",", "[", "]"))));
        Arrays.stream(LogHandlerType.values()).forEach(this::stubAllLogHandlers);
        when(cli.execute(readResource(null, "deployment", "*"))).then(i -> allDeploymentsResponse());
        when(cli.openServerDeploymentManager()).then(i -> deploymentManager);

        //noinspection deprecation
        when(repository.listVersions(isA(GroupId.class), isA(ArtifactId.class), isA(Boolean.class)))
                .then(i -> versions.get(versionsKey(i.getArgument(0), i.getArgument(1)))
                                   .stream()
                                   .filter(i.getArgument(2) ? Version::isSnapshot : Version::isStable)
                                   .collect(toList()));
    }

    private static String versionsKey(GroupId groupId, ArtifactId artifactId) { return groupId + ":" + artifactId; }

    public static String rootLogger() {return address("logging", "root-logger", "ROOT");}

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

    private void stubAllLogHandlers(LogHandlerType type) {
        when(cli.execute(readResource("logging", type.getHandlerTypeName(), "*"))).then(i ->
                toModelNode(allLogHandlers.getOrDefault(type, emptyList()).stream().collect(joining(",", "[", "]"))));
    }

    private ModelNode allDeploymentsResponse() {
        return toModelNode(allDeployments.stream().collect(joining(",", "[", "]")));
    }

    @Test public void dummyTestToStopJUnitFromComplainingAboutMissingTestsInAbstractDeployerTest() {}

    @After
    public void after() {
        verify(cli, atLeast(0)).executeRaw(any(ModelNode.class));
        verify(cli, atLeast(0)).openServerDeploymentManager();
        verify(cli, atLeast(0)).execute(readResource(null, "deployment", "*"));
        verify(cli, atLeast(0)).execute(readResource("logging", "logger", "*"));
        Arrays.stream(LogHandlerType.values()).forEach(type ->
                verify(cli, atLeast(0)).execute(readResource("logging", type.getHandlerTypeName(), "*")));

        verifyNoMoreInteractions(cli);
        // TODO verifyNoMoreInteractions(deploymentManager);
    }


    @SneakyThrows(IOException.class)
    public void givenConfiguredRootBundle(String key, String value) {
        deployer.rootBundle = YAML.readValue(key + ": " + value, RootBundleConfig.class);
    }

    public void givenConfiguredVariable(String name, String value) {
        givenConfiguredVariable(new VariableName(name), value);
    }

    public void givenConfiguredVariable(VariableName name, String value) { this.configuredVariables.put(name, value); }


    protected void givenManaged(String... resourceName) { this.managedResourceNames.addAll(asList(resourceName)); }


    public ArtifactFixtureBuilder givenArtifact(String name) {
        return givenArtifact(name, "org." + name, name);
    }

    public ArtifactFixtureBuilder givenArtifact(ArtifactType type, String name) {
        return givenArtifact(type, name, "org." + name, name);
    }

    public ArtifactFixtureBuilder givenArtifact(ArtifactType type, String groupId, String artifactId) {
        return givenArtifact(type, artifactId, groupId, artifactId);
    }

    public ArtifactFixtureBuilder givenArtifact(String groupId, String artifactId) {
        return givenArtifact(artifactId, groupId, artifactId);
    }

    public ArtifactFixtureBuilder givenArtifact(String name, GroupId groupId, ArtifactId artifactId) {
        return givenArtifact(name, groupId.getValue(), artifactId.getValue());
    }

    public ArtifactFixtureBuilder givenArtifact(String name, String groupId, String artifactId) {
        return givenArtifact(war, name, groupId, artifactId);
    }

    public ArtifactFixtureBuilder givenArtifact(ArtifactType type, String name, String groupId, String artifactId) {
        return new ArtifactFixtureBuilder(type, name).groupId(groupId).artifactId(artifactId);
    }

    public class ArtifactFixtureBuilder {
        private final ArtifactType type;
        private final String name;
        private String groupId;
        private String artifactId;
        private String classifier;
        private ArtifactFixture deployed;

        public ArtifactFixtureBuilder(ArtifactType type, String name) {
            this.type = type;
            this.name = name;

            when(cli.executeRaw(readResource(null, "deployment", name + typeSuffix()))).then(i -> (deployed == null)
                    ? notDeployedNode(null, "deployment", name)
                    : toModelNode("{" + deployed.deployedNode() + "}"));
        }

        @NotNull protected String typeSuffix() { return (this.type == war) ? ".war" : ""; }

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

        public ArtifactFixtureBuilder classifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        public Classifier classifier() { return (classifier == null) ? null : new Classifier(classifier); }

        public DeploymentName deploymentName() { return new DeploymentName((name == null) ? artifactId : name); }

        public ArtifactFixture version(String version) { return version(new Version(version)); }

        public ArtifactFixture version(Version version) { return new ArtifactFixture(version); }

        public class ArtifactFixture {
            @NonNull @Getter private final Version version;
            @Getter private Checksum checksum;
            private String contents;

            public ArtifactFixture(Version version) {
                this.version = version;

                when(repository.resolveArtifact(groupId(), artifactId(), version, type, classifier()))
                        .then(i -> artifact());
                versions.computeIfAbsent(versionsKey(groupId(), artifactId()), k -> new ArrayList<>()).add(version);
                checksum(fakeChecksumFor(deploymentName(), version));
            }

            public String deployedNode() {
                return ""
                        + "'outcome' => 'success',\n"
                        + "'result' => {\n"
                        + "    'content' => [{'hash' => bytes {" + checksum.hexByteArray() + "}}],\n"
                        + "    'enabled' => true,\n"
                        + "    'name' => '" + deploymentName() + typeSuffix() + "',\n"
                        + "    'persistent' => true,\n"
                        + "    'runtime-name' => '" + deploymentName() + "',\n"
                        + "    'subdeployment' => undefined,\n"
                        + "    'subsystem' => {"
                        + "        'jaxrs' => {},\n"
                        + "        'ejb3' => {\n"
                        + "            'entity-bean' => undefined,\n"
                        + "            'message-driven-bean' => undefined,\n"
                        + "            'singleton-bean' => undefined,\n"
                        + "            'stateful-session-bean' => undefined,\n"
                        + "            'stateless-session-bean' => undefined\n"
                        + "        },\n"
                        + "        'undertow' => {\n"
                        + "            'context-root' => '" + deploymentName() + "',\n"
                        + "            'virtual-host' => 'default-host',\n"
                        + "            'servlet' => {'javax.ws.rs.core.Application' => {\n"
                        + "                'servlet-class' => 'org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher',\n"
                        + "                'servlet-name' => 'javax.ws.rs.core.Application'\n"
                        + "            }},\n"
                        + "            'websocket' => undefined,\n"
                        + "            'logging' => {'configuration' => undefined}\n"
                        + "        }\n"
                        + "    }\n"
                        + "}";
            }

            public ArtifactFixture version(String version) { return ArtifactFixtureBuilder.this.version(version); }

            public DeploymentName deploymentName() { return ArtifactFixtureBuilder.this.deploymentName(); }

            public ArtifactFixture checksum(Checksum checksum) {
                this.checksum = checksum;
                when(repository.searchByChecksum(checksum)).then(i -> artifact());
                when(repository.lookupByChecksum(checksum)).then(i -> artifact());
                return this;
            }

            public void containing(String contents) { this.contents = contents; }

            public ArtifactFixture deployed() {
                if (deployed != null)
                    throw new RuntimeException("already have deployed " + name + ":" + version);
                deployed = this;
                allDeployments.add("{" + deploymentAddress() + deployedNode() + "}");
                return this;
            }

            private String deploymentAddress() { return address(null, "deployment", name); }

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

            public Classifier classifier() { return ArtifactFixtureBuilder.this.classifier(); }

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


            public DeployableAuditBuilder artifactAudit() { return DeployableAudit.builder().name(deploymentName()); }


            public ArtifactFixtureBuilder and() { return ArtifactFixtureBuilder.this; }

            public void verifyDeployed(Audits audits) {
                verifyAddExecuted();
                assertThat(audits.getAudits()).containsExactly(addedAudit());
            }

            public void verifyAddExecuted() {
                // TODO verify(cli).execute(toModelNode(""
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
                verify(planBuilder).undeploy(name + ".war");
                verify(undeployPlanBuilder).remove(name + ".war");
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

            public DeployablePlan asPlan() {
                return DeployablePlan
                        .builder()
                        .name(deploymentName())
                        .groupId(groupId())
                        .artifactId(artifactId())
                        .version(version)
                        .type(type)
                        .state(deployed == null ? DeploymentState.undeployed : DeploymentState.deployed)
                        .checksum(checksum)
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

            when(cli.executeRaw(readResource("logging", "logger", category))).then(i -> deployed
                    ? toModelNode("{" + deployedNode() + "}")
                    : notDeployedNode("logging", "logger", category));
        }

        public String deployedNode() {
            return ""
                    + "'outcome' => 'success',\n"
                    + "'result' => {\n"
                    + "    'category' => " + ((category == null) ? "undefined" : "'" + category + "'") + ",\n"
                    + "    'filter' => undefined,\n"
                    + "    'filter-spec' => undefined,\n"
                    + (handlers.isEmpty() ? "" : "        'handlers' => " + handlersArrayNode() + ",\n")
                    + "    'level' => " + ((level == null) ? "undefined" : "'" + level + "'") + ",\n"
                    + "    'use-parent-handlers' => " + useParentHandlers + "\n"
                    + "}\n";
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
            allLoggers.add("{" + loggerAddress() + deployedNode() + "}");
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
                    .change("use-parent-handlers", null, useParentHandlers);
            for (LogHandlerName handler : handlerNames())
                audit.change("handler", null, handler);
            assertThat(audits.getAudits()).containsExactly(audit.added());
        }

        public List<LogHandlerName> handlerNames() {
            return handlers.stream().map(LogHandlerName::new).collect(toList());
        }

        public void verifyUpdatedFrom(LogLevel oldLevel, Audits audits) {
            verify(cli).writeAttribute(buildRequest(), "level", level.toString());
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("level", oldLevel, level).changed());
        }

        public ModelNode buildRequest() { return toModelNode("{" + loggerAddress() + "}"); }

        public void verifyRemoved(Audits audits) {
            verify(cli).execute(toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'remove'\n"
                    + "}"));
            AuditBuilder audit = LoggerAudit
                    .of(getCategory())
                    .change("level", level, null)
                    .change("use-parent-handlers", useParentHandlers, null);
            for (LogHandlerName handler : handlerNames())
                audit.change("handler", handler, null);
            assertThat(audits.getAudits()).containsExactly(audit.removed());
        }

        public void verifyUpdatedUseParentHandlers(Boolean oldUseParentHandlers, Audits audits) {
            verify(cli).writeAttribute(buildRequest(), "use-parent-handlers", useParentHandlers);
            assertThat(audits.getAudits()).containsExactly(
                    LoggerAudit.of(getCategory()).change("use-parent-handlers", oldUseParentHandlers, useParentHandlers)
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

        public LoggerPlan asPlan() {
            return LoggerPlan
                    .builder()
                    .category(category)
                    .state(deployed ? DeploymentState.deployed : DeploymentState.undeployed)
                    .level(level)
                    .handlers(handlerNames())
                    .useParentHandlers((useParentHandlers == FALSE) ? false : null) // true -> null (default)
                    .build();
        }
    }

    public static ModelNode readResource(String subsystem, String type, Object name) {
        return readResource(address(subsystem, type, name));
    }

    public static ModelNode readResource(String address) {
        return toModelNode(""
                + "{\n"
                + address
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


    public LogHandlerFixture givenLogHandler(LogHandlerType type, String name) {
        return new LogHandlerFixture(type, name);
    }

    @Getter
    public class LogHandlerFixture {
        private final LogHandlerType type;
        private final LogHandlerName name;
        private final LogHandlerAuditBuilder expectedAudit;
        private LogLevel level;
        private String format;
        private String formatter;
        private String file;
        private String suffix;
        private String encoding;
        private String module;
        private String class_;
        private Map<String, String> properties;
        private boolean deployed;

        public LogHandlerFixture(LogHandlerType type, String name) {
            this.type = type;
            this.name = new LogHandlerName(name);
            this.expectedAudit = LogHandlerAudit.builder().type(this.type).name(this.name);
            this.suffix = (type == periodicRotatingFile) ? DEFAULT_SUFFIX : null;

            when(cli.executeRaw(readResource("logging", type.getHandlerTypeName(), name))).then(i -> deployed
                    ? toModelNode("{" + deployedNode() + "}")
                    : notDeployedNode("logging", type.getHandlerTypeName(), name));
        }

        public String deployedNode() {
            return ""
                    + "'outcome' => 'success',\n"
                    + "'result' => {\n"
                    + "    'name' => '" + name + "',\n"
                    + ((level == null) ? "" : "    'level' => '" + level + "',\n")
                    + "    'append' => true,\n"
                    + "    'autoflush' => true,\n"
                    + "    'enabled' => true,\n"
                    + "    'encoding' => undefined,\n"
                    + "    'formatter' => '"
                    + ((format == null) ? "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n" : format) + "',\n"
                    + "    'named-formatter' => " + ((formatter == null) ? "undefined" : "'" + formatter + "'") + ",\n"
                    + ((file == null) ? "" :
                    "    'file' => {\n"
                            + "        'relative-to' => 'jboss.server.log.dir',\n"
                            + "        'path' => '" + file + "'\n"
                            + "    },\n")
                    + ((suffix == null) ? "" : "    'suffix' => '" + suffix + "',\n")
                    + ((encoding == null) ? "" : "    'encoding' => '" + encoding + "',\n")
                    + ((module == null) ? "" : "    'module' => '" + module + "',\n")
                    + ((class_ == null) ? "" : "    'class' => '" + class_ + "',\n")
                    + ((properties == null) ? "" :
                    "    'properties' => [\n        "
                            + properties.entrySet().stream()
                                        .map(entry -> "('" + entry.getKey() + "' => '" + entry.getValue() + "')")
                                        .collect(joining(",\n        "))
                            + "\n    ],\n")
                    + "    'filter' => undefined,\n"
                    + "    'filter-spec' => undefined\n"
                    + "}\n";
        }

        public LogHandlerFixture level(LogLevel level) {
            this.level = level;
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

        public LogHandlerFixture file(String file) {
            this.file = file;
            return this;
        }

        public LogHandlerFixture suffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        public LogHandlerFixture encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public LogHandlerFixture module(String module) {
            this.module = module;
            return this;
        }

        public LogHandlerFixture class_(String class_) {
            this.class_ = class_;
            return this;
        }

        public LogHandlerFixture property(String key, String value) {
            if (this.properties == null)
                this.properties = new LinkedHashMap<>();
            this.properties.put(key, value);
            return this;
        }

        public LogHandlerFixture deployed() {
            this.deployed = true;
            allLogHandlers.computeIfAbsent(type, t -> new ArrayList<>())
                          .add("{" + logHandlerAddress() + deployedNode() + "}");
            return this;
        }


        private ModelNode buildRequest() {
            return toModelNode("{" + logHandlerAddress() + "}");
        }

        private String logHandlerAddress() { return address("logging", type.getHandlerTypeName(), name); }


        public <T> LogHandlerFixture verifyChange(String name, T oldValue, T newValue) {
            verifyWriteAttribute(name, newValue);
            expectChange(name, oldValue, newValue);
            return this;
        }

        public <T> void verifyWriteAttribute(String name, T value) {
            verify(cli).writeAttribute(buildRequest(), name, toStringOrNull(value));
        }

        public <T> LogHandlerFixture expectChange(String name, T oldValue, T newValue) {
            expectedAudit.change(name, oldValue, newValue);
            return this;
        }

        public void verifyChanged(Audits audits) {
            assertThat(audits.getAudits()).containsExactly(this.expectedAudit.changed());
        }

        public void verifyMapPut(String name, String key, String value) {
            verify(cli).mapPut(buildRequest(), name, key, value);
        }

        public void verifyMapRemove(String name, String key) {
            verify(cli).mapRemove(buildRequest(), name, key);
        }

        public void verifyAdded(Audits audits) {
            ModelNode expectedAdd = toModelNode("{\n"
                    + logHandlerAddress()
                    + "    'operation' => 'add'"
                    + ",\n    'level' => '" + ((level == null) ? "ALL" : level) + "'"
                    + ",\n    " + ((formatter == null)
                                           ? "'formatter' => '" + ((format == null) ? DEFAULT_LOG_FORMAT : format) + "'"
                                           : "'named-formatter' => '" + formatter + "'")
                    + ((type != periodicRotatingFile) ? "" : ",\n    'file' => {\n"
                    + "        'path' => '" + ((file == null) ? name.getValue().toLowerCase() + ".log" : file) + "',\n"
                    + "        'relative-to' => 'jboss.server.log.dir'\n"
                    + "    }")
                    + ((suffix == null) ? "" : ",\n    'suffix' => '" + suffix + "'")
                    + ((encoding == null) ? "" : ",\n    'encoding' => '" + encoding + "'")
                    + ((module == null) ? "" : ",\n    'module' => '" + module + "'")
                    + ((class_ == null) ? "" : ",\n    'class' => '" + class_ + "'")
                    + ((properties == null) ? "" :
                    ",\n    'properties' => [\n        "
                            + properties.entrySet().stream()
                                        .map(entry -> "('" + entry.getKey() + "' => '" + entry.getValue() + "')")
                                        .collect(joining(",\n        "))
                            + "\n    ]\n")
                    + "\n}");
            verify(cli).execute(expectedAdd);
            expectedAudit.change("level", null, (level == null) ? ALL : level);
            if (format == null && formatter == null)
                expectedAudit.change("format", null, DEFAULT_LOG_FORMAT);
            if (format != null)
                expectedAudit.change("format", null, format);
            if (formatter != null)
                expectedAudit.change("formatter", null, formatter);
            if (encoding != null)
                expectedAudit.change("encoding", null, encoding);
            if (type == periodicRotatingFile)
                expectedAudit.change("file", null, (file == null) ? name.getValue().toLowerCase() + ".log" : file);
            if (suffix != null)
                expectedAudit.change("suffix", null, suffix);
            if (module != null)
                expectedAudit.change("module", null, module);
            if (class_ != null)
                expectedAudit.change("class", null, class_);
            if (properties != null)
                properties.forEach((key, value) -> expectedAudit.change("property/" + key, null, value));
            assertThat(audits.getAudits()).containsExactly(expectedAudit.added());
        }

        public void verifyRemoved(Audits audits) {
            verify(cli).execute(toModelNode("{\n"
                    + logHandlerAddress()
                    + "    'operation' => 'remove'\n"
                    + "}"));
            expectedAudit.change("level", this.level, null);
            if (this.format != null)
                expectedAudit.change("format", this.format, null);
            if (this.formatter != null)
                expectedAudit.change("formatter", this.formatter, null);
            if (this.file != null)
                expectedAudit.change("file", this.file, null);
            if (this.suffix != null)
                expectedAudit.change("suffix", this.suffix, null);
            if (this.encoding != null)
                expectedAudit.change("encoding", this.encoding, null);
            if (this.module != null)
                expectedAudit.change("module", this.module, null);
            if (this.class_ != null)
                expectedAudit.change("class", this.class_, null);
            if (properties != null)
                properties.forEach((key, value) -> expectedAudit.change("property/" + key, value, null));
            assertThat(audits.getAudits()).containsExactly(expectedAudit.removed());
        }

        public LogHandlerPlan asPlan() {
            return LogHandlerPlan
                    .builder()
                    .type(type)
                    .name(name)
                    .level(level)
                    .format(format)
                    .formatter(formatter)
                    .file(file)
                    .suffix(suffix)
                    .encoding(encoding)
                    .module(module)
                    .class_(class_)
                    .properties((properties == null) ? emptyMap() : properties)
                    .build();
        }
    }
}
