package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.DeployableAudit.DeployableAuditBuilder;
import com.github.t1.deployer.app.Audit.LogHandlerAudit.LogHandlerAuditBuilder;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.DataSourcePlan.*;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.deployer.model.LogHandlerPlan;
import com.github.t1.deployer.model.LogHandlerType;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.deployer.tools.KeyStoreConfig;
import com.github.t1.log.LogLevel;
import com.github.t1.testtools.*;
import lombok.*;
import org.jboss.as.controller.client.*;
import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import javax.enterprise.inject.Instance;
import java.io.*;
import java.lang.Boolean;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.*;
import java.util.function.*;

import static com.github.t1.deployer.app.DeployerBoundary.*;
import static com.github.t1.deployer.app.Trigger.*;
import static com.github.t1.deployer.container.Container.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.LogHandlerPlan.*;
import static com.github.t1.deployer.model.LogHandlerType.*;
import static com.github.t1.deployer.model.Password.*;
import static com.github.t1.deployer.model.Plan.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.*;
import static com.github.t1.deployer.tools.Tools.*;
import static com.github.t1.log.LogLevel.*;
import static java.lang.Boolean.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.*;
import static org.jboss.as.controller.client.helpers.Operations.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

@SuppressWarnings("SameParameterValue")
@RunWith(MockitoJUnitRunner.Silent.class)
public abstract class AbstractDeployerTests {

    @SneakyThrows(IOException.class)
    private static Path tempDir() { return Files.createTempDirectory("deployer.test"); }

    private final Path tempDir = tempDir();

    @Rule public SystemPropertiesRule systemProperties = new SystemPropertiesRule()
            .given("jboss.server.config.dir", tempDir)
            .given(CLI_DEBUG, "true");
    @Rule @SuppressWarnings("resource")
    public FileMemento rootBundle = new FileMemento(() -> tempDir.resolve(ROOT_BUNDLE));

    Audits deploy(String plan) {
        rootBundle.write(plan);
        boundary.apply(post, emptyMap());
        return boundary.audits;
    }


    @InjectMocks DeployerBoundary boundary;

    @Spy private LogHandlerDeployer logHandlerDeployer;
    @Spy private LoggerDeployer loggerDeployer;
    @Spy private DataSourceDeployer dataSourceDeployer;
    @Spy private ArtifactDeployer artifactDeployer;
    @Mock Instance<Deployer> deployers;

    @Mock Repository repository;

    @SuppressWarnings("resource") ModelControllerClient cli = mock(ModelControllerClient.class);
    @Spy Container container = JBossCliTestClient.buildContainer(cli);

    private final Map<VariableName, String> configuredVariables = new HashMap<>();
    private final List<String> managedResourceNames = new ArrayList<>();
    private final Map<String, List<String>> pinnedResourceNames = new LinkedHashMap<>();
    private final List<String> allDeployments = new ArrayList<>();
    private final List<String> allLoggers = new ArrayList<>();
    private final List<String> allNonXaDataSources = new ArrayList<>();
    private final List<String> allXaDataSources = new ArrayList<>();
    private final Map<LogHandlerType, List<String>> allLogHandlers = new LinkedHashMap<>();

    private final Map<String, List<Version>> versions = new LinkedHashMap<>();

    private String processState;

    @Before
    public void before() {
        logHandlerDeployer.managedResourceNames
                = loggerDeployer.managedResourceNames
                = dataSourceDeployer.managedResourceNames
                = artifactDeployer.managedResourceNames
                = managedResourceNames;
        logHandlerDeployer.pinnedResourceNames
                = loggerDeployer.pinnedResourceNames
                = dataSourceDeployer.pinnedResourceNames
                = artifactDeployer.pinnedResourceNames
                = pinnedResourceNames;
        artifactDeployer.repository
                = repository;
        logHandlerDeployer.container
                = loggerDeployer.container
                = dataSourceDeployer.container
                = artifactDeployer.container
                = container;
        boundary.audits
                = logHandlerDeployer.audits
                = loggerDeployer.audits
                = dataSourceDeployer.audits
                = artifactDeployer.audits
                = new Audits();
        boundary.configuredVariables = this.configuredVariables;
        boundary.deployers = this.deployers;
        boundary.triggers = EnumSet.allOf(Trigger.class);

        //noinspection unchecked
        doAnswer(i -> {
            asList(logHandlerDeployer, loggerDeployer, dataSourceDeployer, artifactDeployer)
                    .forEach(i.<Consumer<AbstractDeployer>>getArgument(0));
            return null;
        }).when(deployers).forEach(any(Consumer.class));

        when(anyModelNode()).then(i -> success(processState)); // write-attribute calls
        when(anyOperation()).then(i -> success(processState)); // composite calls
        whenCli(readResourceRequest(rootLogger())).thenRaw(this::rootLoggerResponse);
        whenCli(readLoggerRequest("*")).then(this::allLoggersResponse);
        whenCli(readDatasourceRequest("*", false)).then(this::allNonXaDataSourcesResponse);
        whenCli(readDatasourceRequest("*", true)).then(this::allXaDataSourcesResponse);
        Arrays.stream(LogHandlerType.values()).forEach(this::stubAllLogHandlers);
        whenCli(readDeploymentRequest("*")).then(this::allDeploymentsResponse);

        //noinspection deprecation
        when(repository.listVersions(isA(GroupId.class), isA(ArtifactId.class), isA(Boolean.class)))
                .then(i -> versions.get(versionsKey(i.getArgument(0), i.getArgument(1)))
                                   .stream()
                                   .filter(i.getArgument(2) ? Version::isSnapshot : Version::isStable)
                                   .collect(toList()));
    }

    @SneakyThrows(IOException.class)
    private ModelNode anyModelNode() { return cli.execute(any(ModelNode.class), any(OperationMessageHandler.class)); }

    @SneakyThrows(IOException.class)
    private ModelNode anyOperation() { return cli.execute(any(Operation.class), any(OperationMessageHandler.class)); }

    @RequiredArgsConstructor
    public class OngoingCli {
        private final ModelNode request;

        public void then(Supplier<ModelNode> supplier) {
            thenRaw(() -> success(supplier.get()));
        }

        @SneakyThrows(IOException.class)
        public void thenRaw(Supplier<ModelNode> supplier) {
            when(cli.execute(eq(request), any(OperationMessageHandler.class))).then(i -> supplier.get());
        }
    }

    public OngoingCli whenCli(ModelNode request) {
        return new OngoingCli(request);
    }


    private static String versionsKey(GroupId groupId, ArtifactId artifactId) { return groupId + ":" + artifactId; }

    public static String rootLogger() { return address("logging", "root-logger", "ROOT"); }

    public static ModelNode rootLoggerNode() { return createAddress("subsystem", "logging", "root-logger", "ROOT"); }

    private ModelNode rootLoggerResponse() {
        return toModelNode(""
                + "{\n"
                + "    'outcome' => 'success',\n"
                + "    'result' => {\n"
                + "        'filter' => undefined,\n"
                + "        'filter-spec' => undefined,\n"
                + "        'handlers' => [\n"
                + "            'CONSOLE',\n"
                + "            'FILE'\n"
                + "        ],\n"
                + "        'level' => 'INFO'\n"
                + "    }\n"
                + "}");
    }

    private void stubAllLogHandlers(LogHandlerType type) {
        whenCli(readLogHandlerRequest(type, "*"))
                .then(() -> joinModelNode(allLogHandlers.getOrDefault(type, emptyList())));
    }

    private ModelNode allLoggersResponse() { return joinModelNode(allLoggers); }

    private ModelNode allDeploymentsResponse() { return joinModelNode(allDeployments); }

    private ModelNode allNonXaDataSourcesResponse() { return joinModelNode(allNonXaDataSources); }

    private ModelNode allXaDataSourcesResponse() { return joinModelNode(allXaDataSources); }

    private ModelNode joinModelNode(List<String> list) {
        return toModelNode(list.stream().collect(joining(",", "[", "]")));
    }

    @After
    @SneakyThrows(IOException.class) @SuppressWarnings("resource")
    public void after() {
        verify(cli, atLeast(0)).execute(any(ModelNode.class), any(OperationMessageHandler.class));
        verifyCli(readDeploymentRequest("*"), atLeast(0));
        verifyCli(readLoggerRequest("*"), atLeast(0));
        verifyCli(readDatasourceRequest("*", true), atLeast(0));
        verifyCli(readDatasourceRequest("*", false), atLeast(0));
        Arrays.stream(LogHandlerType.values()).forEach(type ->
                verifyCli(readLogHandlerRequest(type, "*"), atLeast(0)));

        verifyNoMoreInteractions(cli);
    }

    @SneakyThrows(IOException.class) @SuppressWarnings("resource")
    private void verifyCli(ModelNode request, VerificationMode mode) {
        verify(cli, mode).execute(eq(request), any(OperationMessageHandler.class));
    }

    public void verifyWriteAttribute(ModelNode address, String name, String value) {
        verifyWriteAttribute(address, name, ModelNode::set, value);
    }

    public void verifyWriteAttribute(ModelNode address, String name, Integer value) {
        verifyWriteAttribute(address, name, ModelNode::set, value);
    }

    public void verifyWriteAttribute(ModelNode address, String name, Long value) {
        verifyWriteAttribute(address, name, ModelNode::set, value);
    }

    public void verifyWriteAttribute(ModelNode address, String name, Boolean value) {
        verifyWriteAttribute(address, name, ModelNode::set, value);
    }

    @SuppressWarnings("resource")
    private <T> void verifyWriteAttribute(ModelNode addr, String name, BiFunction<ModelNode, T, ModelNode> set, T v) {
        ModelNode op = createOperation(WRITE_ATTRIBUTE_OPERATION, addr);
        op.get(NAME).set(name);
        if (v != null)
            set.apply(op.get(VALUE), v);
        assertThat(captureOperations()).haveExactly(1, step(op));
    }

    private static ModelNode notDeployedNode(String subsystem, Object type, Object name) {
        //noinspection SpellCheckingInspection
        return ModelNode.fromString("{\n"
                + "    \"outcome\" => \"failed\",\n"
                + "    \"failure-description\" => \"WFLYCTL0216: Management resource '[\n"
                + ((subsystem == null) ? "" : "    (\\\"subsystem\\\" => \\\"" + subsystem + "\\\"),\n")
                + "    (\\\"" + type + "\\\" => \\\"" + name + "\\\")\n"
                + "]' not found\",\n"
                + "    \"rolled-back\" => true\n"
                + "}");
    }


    @SneakyThrows(IOException.class)
    public void givenConfiguredRootBundle(String key, String value) {
        boundary.rootBundle = YAML.readValue(key + ": " + value, RootBundleConfig.class);
    }

    public void givenConfiguredKeyStore(KeyStoreConfig keyStoreConfig) {
        boundary.keyStore = keyStoreConfig;
    }

    public void givenConfiguredVariable(String name, String value) {
        this.configuredVariables.put(new VariableName(name), value);
    }


    protected void givenManaged(String... resourceName) { this.managedResourceNames.addAll(asList(resourceName)); }


    private void givenPinned(String type, String name) {
        pinnedResourceNames.computeIfAbsent(type, k -> new ArrayList<>()).add(name);
    }


    public ArtifactFixtureBuilder givenArtifact(String name) { return givenArtifact(name, "org." + name, name); }

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

            whenCli(readDeploymentRequest(fullName())).thenRaw(() -> (deployed == null)
                    ? notDeployedNode(null, "deployment", name)
                    : toModelNode("{" + deployed.deployedNode() + "}"));
        }

        private String fullName() { return deploymentName() + ((this.type == war) ? ".war" : ""); }

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

        @SuppressWarnings("resource")
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
                        + "    'name' => '" + fullName() + "',\n"
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

            public ArtifactFixture pinned() {
                givenPinned("deployables", name);
                return this;
            }

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
                return Artifact
                        .builder()
                        .groupId(groupId())
                        .artifactId(artifactId())
                        .version(this.version)
                        .type(type)
                        .checksum(checksum)
                        .inputStreamSupplier(this::inputStream)
                        .build();
            }

            public GroupId groupId() { return ArtifactFixtureBuilder.this.groupId(); }

            public ArtifactId artifactId() { return ArtifactFixtureBuilder.this.artifactId(); }

            public Classifier classifier() { return ArtifactFixtureBuilder.this.classifier(); }

            public DeployableAuditBuilder artifactAudit() { return DeployableAudit.builder().name(deploymentName()); }


            public ArtifactFixtureBuilder and() { return ArtifactFixtureBuilder.this; }

            public void verifyDeployed(Audits audits) {
                ModelNode request = toModelNode("{\n"
                        + "    'operation' => 'add',\n"
                        + "    'address' => [('deployment' => '" + fullName() + "')],\n"
                        + "    'enabled' => true,\n"
                        + "    'content' => [('input-stream-index' => 0)]\n"
                        + "}");
                assertThat(captureOperations()).haveExactly(1, step(request));

                assertThat(audits.getAudits()).contains(addedAudit());
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
                verifyRedeployExecuted();

                Checksum oldChecksum = (deployed == null) ? null : deployed.checksum;
                Version oldVersion = (deployed == null) ? null : deployed.version;
                assertThat(audits.getAudits()).contains(artifactAudit()
                        .change("checksum", oldChecksum, checksum)
                        .change("version", oldVersion, version)
                        .changed());
            }

            private void verifyRedeployExecuted() {
                ModelNode request = toModelNode("{\n"
                        + "    'operation' => 'full-replace-deployment',\n"
                        + "    'address' => [],\n"
                        + "    'name' => '" + fullName() + "',\n"
                        + "    'content' => [('input-stream-index' => 0)],\n"
                        + "    'enabled' => true\n"
                        + "}");
                assertThat(captureOperations()).haveExactly(1, step(request));
            }

            public void verifyRemoved(Audits audits) {
                verifyUndeployExecuted();
                assertThat(audits.getAudits()).contains(removedAudit());
            }

            public void verifyUndeployExecuted() {
                ModelNode undeploy = toModelNode(""
                        + "{\n"
                        + "    'operation' => 'undeploy',\n"
                        + "    'address' => [('deployment' => '" + fullName() + "')]\n"
                        + "}\n");
                ModelNode remove = toModelNode(""
                        + "{\n"
                        + "    'operation' => 'remove',\n"
                        + "    'address' => [('deployment' => '" + fullName() + "')]\n"
                        + "}\n");
                assertThat(captureOperations())
                        .haveExactly(1, step(undeploy))
                        .haveExactly(1, step(remove));
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

    private List<Operation> operations;

    @SneakyThrows(IOException.class) @SuppressWarnings("resource")
    public List<Operation> captureOperations() {
        if (operations == null) {
            ArgumentCaptor<Operation> captor = ArgumentCaptor.forClass(Operation.class);
            verify(cli, atLeastOnce()).execute(captor.capture(), any(OperationMessageHandler.class));
            operations = captor.getAllValues();
        }
        return operations;
    }

    public List<ModelNode> steps() { return captureOperations().get(0).getOperation().get(STEPS).asList(); }


    public LoggerFixture givenLogger(String name) { return new LoggerFixture(name); }

    @Getter
    public class LoggerFixture {
        private final LoggerCategory category;
        private final List<String> handlers = new ArrayList<>();
        private String level;
        private Boolean useParentHandlers = true;
        private boolean deployed;

        public LoggerFixture(@NonNull String category) {
            this.category = LoggerCategory.of(category);

            whenCli(readLoggerRequest(category)).thenRaw(() -> deployed
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

        public LoggerFixture level(LogLevel level) { return level(level.name()); }

        public LoggerFixture level(String level) {
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

        public LoggerFixture pinned() {
            givenPinned("loggers", category.getValue());
            return this;
        }

        public LoggerFixture deployed() {
            this.deployed = true;
            allLoggers.add("{" + loggerAddress() + deployedNode() + "}");
            return this;
        }

        public String loggerAddress() { return address("logging", "logger", category); }

        protected ModelNode loggerAddressNode() {
            return createAddress("subsystem", "logging", "logger", category.getValue());
        }

        private String handlersArrayNode() {
            if (handlers.size() == 1)
                return "['" + handlers.get(0) + "']";
            else
                return handlers.stream().collect(joining("',\n        '", "[\n        '", "'\n    ]"));
        }

        public void verifyAdded(Audits audits) {
            ModelNode request = toModelNode("{\n"
                    + loggerAddress()
                    + "    'operation' => 'add',\n"
                    + loggerAddress()
                    + ((level == null) ? "" : "    'level' => '" + level + "',\n")
                    + (handlers.isEmpty() ? "" : "    'handlers' => " + handlersArrayNode() + ",\n")
                    + "    'use-parent-handlers' => " + useParentHandlers + "\n"
                    + "}");
            assertThat(captureOperations()).haveExactly(1, step(request));

            AuditBuilder audit = LoggerAudit
                    .of(getCategory())
                    .change("level", null, level)
                    .change("use-parent-handlers", null, useParentHandlers);
            if (!handlerNames().isEmpty())
                audit.change("handlers", null, handlerNames());
            assertThat(audits.getAudits()).contains(audit.added());
        }

        public List<LogHandlerName> handlerNames() {
            return handlers.stream().map(LogHandlerName::new).collect(toList());
        }

        public void verifyUpdatedLogLevelFrom(LogLevel oldLevel, Audits audits) {
            verifyWriteAttribute(loggerAddressNode(), "level", level);
            assertThat(audits.getAudits()).contains(
                    LoggerAudit.of(getCategory()).change("level", oldLevel, level).changed());
        }

        public void verifyRemoved(Audits audits) {
            ModelNode request = toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'remove'\n"
                    + "}");
            assertThat(captureOperations()).haveExactly(1, step(request));

            AuditBuilder audit = LoggerAudit.of(getCategory());
            if (level != null)
                audit.change("level", level, null);
            if (useParentHandlers != null)
                audit.change("use-parent-handlers", useParentHandlers, null);
            if (!handlerNames().isEmpty())
                audit.change("handlers", handlerNames(), null);
            assertThat(audits.getAudits()).contains(audit.removed());
        }

        public void verifyUpdatedUseParentHandlersFrom(Boolean oldUseParentHandlers, Audits audits) {
            verifyWriteAttribute(loggerAddressNode(), "use-parent-handlers", useParentHandlers);
            assertThat(audits.getAudits()).contains(
                    LoggerAudit.of(getCategory()).change("use-parent-handlers", oldUseParentHandlers, useParentHandlers)
                               .changed());
        }

        public void verifyAddedHandler(Audits audits, String name) {
            ModelNode request = toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'add-handler',\n"
                    + "    'name' => '" + name + "'\n"
                    + "}");
            assertThat(captureOperations()).haveExactly(1, step(request));
            assertThat(audits.getAudits()).contains(
                    LoggerAudit.of(getCategory()).change("handlers", null, "[" + name + "]").changed());
        }

        public void verifyRemovedHandler(Audits audits, String name) {
            ModelNode request = toModelNode(""
                    + "{\n"
                    + loggerAddress()
                    + "    'operation' => 'remove-handler',\n"
                    + "    'name' => '" + name + "'\n"
                    + "}");
            assertThat(captureOperations()).haveExactly(1, step(request));
            assertThat(audits.getAudits()).contains(
                    LoggerAudit.of(getCategory()).change("handlers", "[" + name + "]", null).changed());
        }

        public LoggerPlan asPlan() {
            return LoggerPlan
                    .builder()
                    .category(category)
                    .state(deployed ? DeploymentState.deployed : DeploymentState.undeployed)
                    .level((level == null) ? null : LogLevel.valueOf(level))
                    .handlers(handlerNames())
                    .useParentHandlers((useParentHandlers == FALSE) ? false : null) // true -> null (default)
                    .build();
        }
    }

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

            whenCli(readLogHandlerRequest(type, name)).thenRaw(() -> deployed
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

        public LogHandlerFixture pinned() {
            givenPinned("log-handlers", name.getValue());
            return this;
        }

        public LogHandlerFixture deployed() {
            this.deployed = true;
            allLogHandlers.computeIfAbsent(type, t -> new ArrayList<>())
                          .add("{" + logHandlerAddress() + deployedNode() + "}");
            return this;
        }


        private String logHandlerAddress() {
            return address("logging", type.getHandlerTypeName(), name);
        }

        public ModelNode logHandlerAddressNode() {
            return createAddress("subsystem", "logging", type.getHandlerTypeName(), name.getValue());
        }


        public <T> LogHandlerFixture verifyChange(String name, T oldValue, T newValue) {
            verifyWriteAttribute(logHandlerAddressNode(), name, toStringOrNull(newValue));
            expectChange(name, oldValue, newValue);
            return this;
        }

        public <T> LogHandlerFixture expectChange(String name, T oldValue, T newValue) {
            expectedAudit.change(name, oldValue, newValue);
            return this;
        }

        public void verifyChanged(Audits audits) {
            assertThat(audits.getAudits()).contains(this.expectedAudit.changed());
        }

        public void verifyPutProperty(String key, String value) {
            ModelNode request = createOperation("map-put", logHandlerAddressNode());
            request.get("name").set("property");
            request.get("key").set(key);
            request.get("value").set(value);

            assertThat(captureOperations()).haveExactly(1, step(request));
        }

        public void verifyRemoveProperty(String key) {
            ModelNode request = createOperation("map-remove", logHandlerAddressNode());
            request.get("name").set("property");
            request.get("key").set(key);

            assertThat(captureOperations()).haveExactly(1, step(request));
        }

        public void verifyAdded(Audits audits) {
            ModelNode request = toModelNode("{\n"
                    + logHandlerAddress()
                    + "    'operation' => 'add'"
                    + ",\n    'level' => '" + ((level == null) ? "ALL" : level) + "'"
                    + ((formatter == null) ? "" : ",\n    'named-formatter' => '" + formatter + "'")
                    + ((format == null) ? "" : ",\n    'formatter' => '" + format + "'")
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
            assertThat(captureOperations()).haveExactly(1, step(request));

            expectedAudit.change("level", null, (level == null) ? ALL : level);
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
                properties.forEach((key, value) -> expectedAudit.change("property:" + key, null, value));
            assertThat(audits.getAudits()).contains(expectedAudit.added());
        }

        public void verifyRemoved(Audits audits) {
            ModelNode request = toModelNode("{\n"
                    + logHandlerAddress()
                    + "    'operation' => 'remove'\n"
                    + "}");
            assertThat(captureOperations()).haveExactly(1, step(request));

            if (this.level != null)
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
                properties.forEach((key, value) -> expectedAudit.change("property:" + key, value, null));
            assertThat(audits.getAudits()).contains(expectedAudit.removed());
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


    public DataSourceFixture givenDataSource(String name) { return new DataSourceFixture(name); }

    @Getter
    public class DataSourceFixture {
        private final DataSourceName name;
        private boolean xa;
        private boolean deployed;
        private String driver;
        private String jndiName;

        private String uri;
        private String userName;

        private String password;
        private Integer minPoolSize;
        private Integer initialPoolSize;
        private Integer maxPoolSize;
        private Age maxAge;

        public DataSourceFixture(@NonNull String name) {
            this.name = new DataSourceName(name);
            this.uri = "jdbc:h2:mem:" + name;
            this.jndiName = "java:/datasources/" + name;
            this.driver = "h2";

            whenCli(readDatasourceRequest(name, true)).thenRaw(() -> xa && deployed
                    ? toModelNode("{" + deployedNode() + "}")
                    : notDeployedNode("datasources", dataSource(xa), name));
            whenCli(readDatasourceRequest(name, false)).thenRaw(() -> !xa && deployed
                    ? toModelNode("{" + deployedNode() + "}")
                    : notDeployedNode("datasources", dataSource(xa), name));
        }

        public String deployedNode() {
            return ""
                    + "'outcome' => 'success',\n"
                    + "'result' => {\n"
                    + "    'name' => " + ((name == null) ? "undefined" : "'" + name + "'") + ",\n"
                    + (xa ? "" : "    'connection-url' => '" + uri + "',\n")
                    + "    'jndi-name' => '" + jndiName + "',\n"
                    + "    'driver-name' => '" + driver + "',\n"

                    + "    'user-name' => " + ((userName == null) ? "undefined" : "'" + userName + "'") + ",\n"
                    + "    'password' => " + ((password == null) ? "undefined" : "'" + password + "'") + ",\n"

                    + "    'min-pool-size' => " + ((minPoolSize == null)
                                                           ? "undefined" : "'" + minPoolSize + "'") + ",\n"
                    + "    'initial-pool-size' => " + ((initialPoolSize == null)
                                                               ? "undefined" : "'" + initialPoolSize + "'") + ",\n"
                    + "    'max-pool-size' => " + ((maxPoolSize == null)
                                                           ? "undefined" : "'" + maxPoolSize + "'") + ",\n"
                    + "    'idle-timeout-minutes' => "
                    + ((maxAge == null) ? "undefined" : "'" + maxAge.asMinutes() + "'") + "\n"
                    + (xa
                               ? ", 'xa-datasource-properties' => {"
                    + "'ServerName' => {'value' => '" + jdbcHost() + "'}, "
                    + "'DatabaseName' => {'value' => '" + jdbcDbName() + "'}}"
                               : "")
                    + "}\n";
        }

        public DataSourceFixture uri(String uri) {
            this.uri = uri;
            return this;
        }


        public DataSourceFixture xa(boolean xa) {
            this.xa = xa;
            return this;
        }

        public DataSourceFixture jndiName(String jndiName) {
            this.jndiName = jndiName;
            return this;
        }

        public DataSourceFixture driver(String driver) {
            this.driver = driver;
            return this;
        }

        public DataSourceFixture credentials(String userName, String password) {
            return userName(userName).password(password);
        }

        public DataSourceFixture userName(String userName) {
            this.userName = userName;
            return this;
        }

        public DataSourceFixture password(String password) {
            this.password = password;
            return this;
        }

        public DataSourceFixture minPoolSize(Integer minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }

        public DataSourceFixture initialPoolSize(Integer initialPoolSize) {
            this.initialPoolSize = initialPoolSize;
            return this;
        }

        public DataSourceFixture maxPoolSize(Integer maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public DataSourceFixture maxAge(Age maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public DataSourceFixture processState(String processState) {
            AbstractDeployerTests.this.processState = processState;
            return this;
        }

        public DataSourceFixture pinned() {
            givenPinned("data-sources", name.getValue());
            return this;
        }

        public DataSourceFixture deployed() {
            this.deployed = true;
            (xa ? allXaDataSources : allNonXaDataSources).add("{" + dataSourceAddress() + deployedNode() + "}");
            return this;
        }

        public String dataSourceAddress() { return address("datasources", dataSource(xa), name); }

        public ModelNode dataSourceAddressNode() {
            return createAddress("subsystem", "datasources", dataSource(xa), name.getValue());
        }

        public void verifyAdded(Audits audits) {
            verifyAddCli();
            assertAddAudit(audits);
        }

        @SuppressWarnings("resource") public void verifyAddCli() {
            ModelNode addRequest = toModelNode("{\n"
                    + "    'operation' => 'add',\n"
                    + dataSourceAddress()
                    + (xa ? "" : "    'connection-url' => '" + uri + "',\n")
                    + "    'jndi-name' => '" + jndiName + "',\n"
                    + "    'driver-name' => '" + driver + "'\n"

                    + ((userName == null) ? "" : ",    'user-name' => '" + userName + "'\n")
                    + ((password == null) ? "" : ",    'password' => '" + password + "'\n")

                    + ((minPoolSize == null) ? "" : ",    'min-pool-size' => " + minPoolSize + "\n")
                    + ((initialPoolSize == null) ? "" : ",    'initial-pool-size' => " + initialPoolSize + "\n")
                    + ((maxPoolSize == null) ? "" : ",    'max-pool-size' => " + maxPoolSize + "\n")
                    + ((maxAge == null) ? "" : ",    'idle-timeout-minutes' => " + maxAge.asMinutes() + "L\n")
                    + "}");

            if (xa) {
                assertThat(captureOperations())
                        .haveExactly(1, step(addRequest))
                        .haveExactly(1, step(addXaDataSourceProperty("ServerName", jdbcHost())))
                        .haveExactly(1, step(addXaDataSourceProperty("PortNumber", jdbcPort())))
                        .haveExactly(1, step(addXaDataSourceProperty("DatabaseName", jdbcDbName())));
            } else {
                assertThat(captureOperations()).haveExactly(1, step(addRequest));
            }
        }

        private String jdbcHost() { return jdbcUri().getHost(); }

        private String jdbcPort() { return Integer.toString(jdbcUri().getPort()); }

        private String jdbcDbName() { return jdbcUri().getPath().substring(1); }

        private URI jdbcUri() {
            assert this.uri.startsWith("jdbc:");
            return URI.create(this.uri.substring(5));
        }

        private ModelNode addXaDataSourceProperty(String propertyName, String value) {
            return toModelNode("{\n"
                    + "'operation' => 'add',\n"
                    + "'address' => [\n"
                    + "    ('subsystem' => 'datasources'),\n"
                    + "    ('xa-data-source' => '" + name + "'),\n"
                    + "    ('xa-datasource-properties' => '" + propertyName + "')\n"
                    + "],\n"
                    + "'value' => '" + value + "'\n"
                    + "}");
        }

        private void assertAddAudit(Audits audits) {
            AuditBuilder audit = DataSourceAudit
                    .of(getName())
                    .change("uri", null, uri)
                    .change("jndi-name", null, jndiName)
                    .change("driver", null, driver);
            if (xa)
                audit.change("xa", null, true);
            if (userName != null)
                audit.change("user-name", null, CONCEALED);
            if (password != null)
                audit.change("password", null, CONCEALED);
            if (minPoolSize != null)
                audit.change("pool:min", null, minPoolSize);
            if (initialPoolSize != null)
                audit.change("pool:initial", null, initialPoolSize);
            if (maxPoolSize != null)
                audit.change("pool:max", null, maxPoolSize);
            if (maxAge != null)
                audit.change("pool:max-age", null, maxAge);
            assertThat(audits.getAudits()).containsExactly(audit.added());
        }

        public void verifyUpdatedUriFrom(String oldUri, Audits audits) {
            verifyWriteAttribute(dataSourceAddressNode(), "uri", uri);
            assertThat(audits.getAudits()).contains(
                    DataSourceAudit.of(name).change("uri", oldUri, uri).changed());
        }

        public void verifyUpdatedJndiNameFrom(String oldJndiName, Audits audits) {
            verifyWriteAttribute(dataSourceAddressNode(), "jndi-name", jndiName);
            assertThat(audits.getAudits()).contains(
                    DataSourceAudit.of(name).change("jndi-name", oldJndiName, jndiName).changed());
        }

        public void verifyUpdatedDriverNameFrom(String oldDriverName, Audits audits) {
            verifyWriteAttribute(dataSourceAddressNode(), "driver-name", driver);
            assertThat(audits.getAudits()).contains(
                    DataSourceAudit.of(name).change("driver", oldDriverName, driver).changed());
        }

        public void verifyUpdatedUserNameFrom(String oldUserName, Audits audits) {
            verifyWriteAttribute(dataSourceAddressNode(), "user-name", userName);
            assertThat(audits.getAudits()).contains(
                    DataSourceAudit.of(name)
                                   .changeRaw("user-name",
                                           oldUserName == null ? null : CONCEALED,
                                           userName == null ? null : CONCEALED)
                                   .changed());
        }

        public void verifyUpdatedPasswordFrom(String oldPassword, Audits audits) {
            verifyWriteAttribute(dataSourceAddressNode(), "password", password);
            assertThat(audits.getAudits()).contains(
                    DataSourceAudit.of(name)
                                   .changeRaw("password",
                                           oldPassword == null ? null : CONCEALED,
                                           password == null ? null : CONCEALED)
                                   .changed());
        }

        public void verifyUpdatedMinPoolSizeFrom(Integer oldMinPoolSize, Audits audits) {
            verifyWriteAttribute(dataSourceAddressNode(), "min-pool-size", minPoolSize);
            assertThat(audits.getAudits()).contains(
                    DataSourceAudit.of(name).change("pool:min", oldMinPoolSize, minPoolSize).changed());
        }

        public void verifyUpdatedInitialPoolSizeFrom(Integer oldInitialPoolSize, Audits audits) {
            verifyWriteAttribute(dataSourceAddressNode(), "initial-pool-size", initialPoolSize);
            assertThat(audits.getAudits()).contains(
                    DataSourceAudit.of(name).change("pool:initial", oldInitialPoolSize, initialPoolSize).changed());
        }

        public void verifyUpdatedMaxPoolSizeFrom(Integer oldMaxPoolSize, Audits audits) {
            verifyWriteAttribute(dataSourceAddressNode(), "max-pool-size", maxPoolSize);
            assertThat(audits.getAudits()).contains(
                    DataSourceAudit.of(name).change("pool:max", oldMaxPoolSize, maxPoolSize).changed());
        }

        public void verifyUpdatedMaxAgeFrom(Age oldMaxAge, Audits audits) {
            verifyWriteAttribute(dataSourceAddressNode(), "idle-timeout-minutes", maxAge.asMinutes());
            assertThat(audits.getAudits()).contains(
                    DataSourceAudit.of(name).change("pool:max-age", oldMaxAge, maxAge).changed());
        }

        public void verifyRemoved(Audits audits) {
            verifyRemoveCli();

            AuditBuilder audit = DataSourceAudit
                    .of(getName())
                    .change("uri", uri, null)
                    .change("jndi-name", jndiName, null)
                    .change("driver", driver, null);
            if (xa)
                audit.change("xa", true, null);
            if (userName != null)
                audit.change("user-name", CONCEALED, null);
            if (password != null)
                audit.change("password", CONCEALED, null);
            if (minPoolSize != null)
                audit.change("pool:min", minPoolSize, null);
            if (initialPoolSize != null)
                audit.change("pool:initial", initialPoolSize, null);
            if (maxPoolSize != null)
                audit.change("pool:max", maxPoolSize, null);
            if (maxAge != null)
                audit.change("pool:max-age", maxAge, null);
            assertThat(audits.getAudits()).contains(audit.removed());
        }

        public void verifyRemoveCli() {
            ModelNode request = toModelNode(""
                    + "{\n"
                    + "    'operation' => 'remove',\n"
                    + dataSourceAddress().substring(0, dataSourceAddress().length() - 1)
                    + "}");
            assertThat(captureOperations()).haveExactly(1, step(request));
        }

        public DataSourcePlan asPlan() {
            DataSourcePlanBuilder builder = DataSourcePlan
                    .builder()
                    .name(name)
                    .state(deployed ? DeploymentState.deployed : DeploymentState.undeployed)
                    .uri(URI.create(uri))
                    .jndiName(jndiName)
                    .driver(driver)
                    .userName(userName)
                    .password(password)
                    .pool(PoolPlan.builder()
                                  .min(minPoolSize)
                                  .initial(initialPoolSize)
                                  .max(maxPoolSize)
                                  .maxAge(maxAge)
                                  .build());
            return builder.build();
        }
    }
}
