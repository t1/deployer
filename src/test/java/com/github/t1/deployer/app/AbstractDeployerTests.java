package com.github.t1.deployer.app;

import com.github.t1.deployer.app.Audit.DataSourceAudit;
import com.github.t1.deployer.app.Audit.DeployableAudit;
import com.github.t1.deployer.app.Audit.LogHandlerAudit;
import com.github.t1.deployer.app.Audit.LoggerAudit;
import com.github.t1.deployer.app.Audits.Warning;
import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.container.JBossCliTestClient;
import com.github.t1.deployer.model.Age;
import com.github.t1.deployer.model.Artifact;
import com.github.t1.deployer.model.ArtifactId;
import com.github.t1.deployer.model.ArtifactType;
import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.Classifier;
import com.github.t1.deployer.model.DataSourceName;
import com.github.t1.deployer.model.DataSourcePlan;
import com.github.t1.deployer.model.DataSourcePlan.PoolPlan;
import com.github.t1.deployer.model.DeployablePlan;
import com.github.t1.deployer.model.DeploymentName;
import com.github.t1.deployer.model.DeploymentState;
import com.github.t1.deployer.model.Expressions.VariableName;
import com.github.t1.deployer.model.GroupId;
import com.github.t1.deployer.model.LogHandlerName;
import com.github.t1.deployer.model.LogHandlerPlan;
import com.github.t1.deployer.model.LogHandlerType;
import com.github.t1.deployer.model.LoggerCategory;
import com.github.t1.deployer.model.LoggerPlan;
import com.github.t1.deployer.model.ProcessState;
import com.github.t1.deployer.model.RootBundleConfig;
import com.github.t1.deployer.model.Version;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.deployer.tools.KeyStoreConfig;
import com.github.t1.log.LogLevel;
import com.github.t1.testtools.FileMemento;
import com.github.t1.testtools.SystemPropertiesMemento;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Condition;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import javax.enterprise.inject.Instance;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.github.t1.deployer.app.DeployerBoundary.ROOT_BUNDLE_CONFIG_FILE;
import static com.github.t1.deployer.app.Trigger.post;
import static com.github.t1.deployer.container.Container.CLI_DEBUG;
import static com.github.t1.deployer.model.ArtifactType.war;
import static com.github.t1.deployer.model.LogHandlerPlan.DEFAULT_SUFFIX;
import static com.github.t1.deployer.model.LogHandlerType.custom;
import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.deployer.model.Plan.YAML;
import static com.github.t1.deployer.model.ProcessState.reloadRequired;
import static com.github.t1.deployer.model.ProcessState.restartRequired;
import static com.github.t1.deployer.model.ProcessState.running;
import static com.github.t1.deployer.repository.ArtifactoryMock.StringInputStream;
import static com.github.t1.deployer.repository.ArtifactoryMock.fakeChecksumFor;
import static com.github.t1.deployer.repository.ArtifactoryMock.inputStreamFor;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.address;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.dataSource;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.readDatasourceRequest;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.readDeploymentRequest;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.readLogHandlerRequest;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.readLoggerRequest;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.readResourceRequest;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.step;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.success;
import static com.github.t1.deployer.testtools.ModelNodeTestTools.toModelNode;
import static com.github.t1.deployer.tools.Password.CONCEALED;
import static com.github.t1.deployer.tools.Tools.toStringOrNull;
import static com.github.t1.log.LogLevel.ALL;
import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.COMPOSITE;
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.STEPS;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.Operations.createAddress;
import static org.jboss.as.controller.client.helpers.Operations.createOperation;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("SameParameterValue")
@ExtendWith(MockitoExtension.class)
public abstract class AbstractDeployerTests {

    private static final Version UNKNOWN = new Version("unknown");

    @SneakyThrows(IOException.class)
    private static Path tempDir() {return Files.createTempDirectory("deployer.test");}

    private final Path tempDir = tempDir();

    @RegisterExtension SystemPropertiesMemento systemProperties = new SystemPropertiesMemento()
        .given("jboss.server.config.dir", tempDir)
        .given(CLI_DEBUG, "true");
    @RegisterExtension FileMemento rootBundle = new FileMemento(() -> tempDir.resolve(ROOT_BUNDLE_CONFIG_FILE));

    void deployWithRootBundle(String plan) {deployWithRootBundle(plan, emptyMap());}

    void deployWithRootBundle(String plan, Map<VariableName, String> variables) {
        rootBundle.write(plan);
        postVariables(variables);
    }

    void post() {postVariables(emptyMap());}

    void postVariables(Map<VariableName, String> variables) {
        boundary.apply(post, variables);
    }


    @InjectMocks DeployerBoundary boundary;

    @Spy private LogHandlerDeployer logHandlerDeployer;
    @Spy private LoggerDeployer loggerDeployer;
    @Spy private DataSourceDeployer dataSourceDeployer;
    @Spy private ArtifactDeployer artifactDeployer;
    @Mock private Instance<Deployer> deployers;

    @Mock private Repository repository;

    private final ModelControllerClient cli = mock(ModelControllerClient.class);
    @Spy private Container container = JBossCliTestClient.buildContainer(cli);

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

    @BeforeEach
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
            //noinspection rawtypes
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

        when(repository.listVersions(isA(GroupId.class), isA(ArtifactId.class), isA(Boolean.class)))
            .then(i -> versions.get(versionsKey(i.getArgument(0), i.getArgument(1)))
                .stream()
                .filter(i.getArgument(2) ? Version::isSnapshot : Version::isStable)
                .collect(toList()));
    }

    @SneakyThrows(IOException.class)
    private ModelNode anyModelNode() {return cli.execute(any(ModelNode.class), any(OperationMessageHandler.class));}

    @SneakyThrows(IOException.class)
    private ModelNode anyOperation() {return cli.execute(any(Operation.class), any(OperationMessageHandler.class));}

    @RequiredArgsConstructor
    public class OngoingCli {
        private final ModelNode request;

        void then(Supplier<ModelNode> supplier) {
            thenRaw(() -> success(supplier.get()));
        }

        @SneakyThrows(IOException.class) void thenRaw(Supplier<ModelNode> supplier) {
            when(cli.execute(eq(request), any(OperationMessageHandler.class))).then(i -> supplier.get());
        }
    }

    private OngoingCli whenCli(ModelNode request) {
        return new OngoingCli(request);
    }


    private static String versionsKey(GroupId groupId, ArtifactId artifactId) {return groupId + ":" + artifactId;}

    private static String rootLogger() {return address("logging", "root-logger", "ROOT");}

    static ModelNode rootLoggerNode() {return createAddress("subsystem", "logging", "root-logger", "ROOT");}

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

    private ModelNode allLoggersResponse() {return joinModelNode(allLoggers);}

    private ModelNode allDeploymentsResponse() {return joinModelNode(allDeployments);}

    private ModelNode allNonXaDataSourcesResponse() {return joinModelNode(allNonXaDataSources);}

    private ModelNode allXaDataSourcesResponse() {return joinModelNode(allXaDataSources);}

    private ModelNode joinModelNode(List<String> list) {
        return toModelNode(list.stream().collect(joining(",", "[", "]")));
    }

    @AfterEach
    @SneakyThrows(IOException.class)
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

    @SneakyThrows(IOException.class)
    private void verifyCli(ModelNode request, VerificationMode mode) {
        verify(cli, mode).execute(eq(request), any(OperationMessageHandler.class));
    }

    void verifyWriteAttribute(ModelNode address, String name, String value) {
        verifyWriteAttribute(address, name, ModelNode::set, value);
    }

    private void verifyWriteAttribute(ModelNode address, String name, Integer value) {
        verifyWriteAttribute(address, name, ModelNode::set, value);
    }

    private void verifyWriteAttribute(ModelNode address, String name, Long value) {
        verifyWriteAttribute(address, name, ModelNode::set, value);
    }

    private void verifyWriteAttribute(ModelNode address, String name, Boolean value) {
        verifyWriteAttribute(address, name, ModelNode::set, value);
    }

    private <T> void verifyWriteAttribute(ModelNode addr, String name, BiFunction<ModelNode, T, ModelNode> set, T v) {
        ModelNode op = createOperation(WRITE_ATTRIBUTE_OPERATION, addr);
        op.get(NAME).set(name);
        if (v != null)
            set.apply(op.get(VALUE), v);
        assertThat(capturedOperations()).describedAs(capturedOperationsDescription()).haveExactly(1, step(op));
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


    @SneakyThrows(IOException.class) void givenConfiguredRootBundle(String key, String value) {
        boundary.rootBundleConfig = YAML.readValue(key + ": " + value, RootBundleConfig.class);
    }

    void givenConfiguredKeyStore(KeyStoreConfig keyStoreConfig) {boundary.keyStore = keyStoreConfig;}

    void givenConfiguredVariable(String name, String value) {
        this.configuredVariables.put(new VariableName(name), value);
    }


    void givenManaged(String... resourceName) {this.managedResourceNames.addAll(asList(resourceName));}


    private void givenPinned(String type, String name) {
        pinnedResourceNames.computeIfAbsent(type, k -> new ArrayList<>()).add(name);
    }


    ArtifactFixtureBuilder.ArtifactFixture givenUnknownArtifact(String name) {
        return givenArtifact(name, "org." + name, name).version(UNKNOWN);
    }

    ArtifactFixtureBuilder givenArtifact(String name) {return givenArtifact(name, "org." + name, name);}

    ArtifactFixtureBuilder givenArtifact(ArtifactType type, String name) {
        return givenArtifact(type, name, "org." + name, name);
    }

    ArtifactFixtureBuilder givenArtifact(ArtifactType type, String groupId, String artifactId) {
        return givenArtifact(type, artifactId, groupId, artifactId);
    }

    ArtifactFixtureBuilder givenArtifact(String groupId, String artifactId) {
        return givenArtifact(artifactId, groupId, artifactId);
    }

    ArtifactFixtureBuilder givenArtifact(String name, GroupId groupId, ArtifactId artifactId) {
        return givenArtifact(name, groupId.getValue(), artifactId.getValue());
    }

    ArtifactFixtureBuilder givenArtifact(String name, String groupId, String artifactId) {
        return givenArtifact(war, name, groupId, artifactId);
    }

    ArtifactFixtureBuilder givenArtifact(ArtifactType type, String name, String groupId, String artifactId) {
        return new ArtifactFixtureBuilder(type, name).groupId(groupId).artifactId(artifactId);
    }

    public class ArtifactFixtureBuilder {
        private final ArtifactType type;
        private final String name;
        private String groupId;
        private String artifactId;
        private String classifier;
        private ArtifactFixture deployed;

        ArtifactFixtureBuilder(ArtifactType type, String name) {
            this.type = type;
            this.name = name;

            whenCli(readDeploymentRequest(fullName())).thenRaw(() -> (deployed == null)
                ? notDeployedNode(null, "deployment", name)
                : toModelNode("{" + deployed.deployedNode() + "}"));
        }

        private String fullName() {return deploymentName() + ((this.type == war) ? ".war" : "");}

        ArtifactFixtureBuilder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        GroupId groupId() {return new GroupId(groupId);}

        ArtifactFixtureBuilder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        ArtifactId artifactId() {return new ArtifactId(artifactId);}

        ArtifactFixtureBuilder classifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        Classifier classifier() {return (classifier == null) ? null : new Classifier(classifier);}

        DeploymentName deploymentName() {return new DeploymentName((name == null) ? artifactId : name);}

        public ArtifactFixture version(String version) {return version(new Version(version));}

        public ArtifactFixture version(Version version) {return new ArtifactFixture(version);}

        public class ArtifactFixture extends AbstractFixture {
            @NonNull @Getter private final Version version;
            @Getter private Checksum checksum;
            private String contents;

            ArtifactFixture(Version version) {
                this.version = version;

                if (!version.equals(UNKNOWN)) {
                    when(repository.resolveArtifact(groupId(), artifactId(), version, type, classifier()))
                        .then(i -> artifact());
                    versions.computeIfAbsent(versionsKey(groupId(), artifactId()), k -> new ArrayList<>()).add(version);
                }
                checksum(fakeChecksumFor(deploymentName(), version));
            }

            String deployedNode() {
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

            public ArtifactFixture version(String version) {return ArtifactFixtureBuilder.this.version(version);}

            DeploymentName deploymentName() {return ArtifactFixtureBuilder.this.deploymentName();}

            public ArtifactFixture checksum(Checksum checksum) {
                this.checksum = checksum;
                when(repository.searchByChecksum(checksum)).then(i -> artifact());
                when(repository.lookupByChecksum(checksum)).then(i -> artifact());
                return this;
            }

            ArtifactFixture containing(String contents) {this.contents = contents; return this;}

            ArtifactFixture pinned() {
                givenPinned("deployables", name);
                return this;
            }

            ArtifactFixture deployed() {
                if (deployed != null)
                    throw new RuntimeException("already have deployed " + name + ":" + version);
                deployed = this;
                allDeployments.add("{" + deploymentAddress() + deployedNode() + "}");
                return this;
            }

            private String deploymentAddress() {return address(null, "deployment", name);}

            InputStream inputStream() {
                return (contents == null)
                    ? inputStreamFor(deploymentName(), version)
                    : new StringInputStream(contents);
            }

            public Artifact artifact() {
                return new Artifact()
                    .setGroupId(groupId())
                    .setArtifactId(artifactId())
                    .setVersion(this.version)
                    .setType(type)
                    .setChecksum(checksum)
                    .setInputStreamSupplier(this::inputStream);
            }

            GroupId groupId() {return ArtifactFixtureBuilder.this.groupId();}

            ArtifactId artifactId() {return ArtifactFixtureBuilder.this.artifactId();}

            Classifier classifier() {return ArtifactFixtureBuilder.this.classifier();}

            DeployableAudit artifactAudit() {return new DeployableAudit().setName(deploymentName());}


            ArtifactFixtureBuilder and() {return ArtifactFixtureBuilder.this;}

            void verifySkipped() {
                verifyUnchanged();
                assertThat(boundary.audits.getWarnings()).describedAs("warnings").containsExactly(new Warning("skip deploying " + name + " in version CURRENT"));
            }

            @Override Condition<Audit> forThisArtifact() {
                return new Condition<>(audit -> audit instanceof DeployableAudit && ((DeployableAudit) audit).name().equals(deploymentName()), "for artifact " + name);
            }

            @Override ModelNode addressNode() {return createAddress("deployment", name + "." + type);}

            void verifyDeployed() {
                ModelNode request = toModelNode("{\n"
                                                + "    'operation' => 'add',\n"
                                                + "    'address' => [('deployment' => '" + fullName() + "')],\n"
                                                + "    'enabled' => true,\n"
                                                + "    'content' => [('input-stream-index' => 0)]\n"
                                                + "}");
                assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                    .haveExactly(1, step(request));

                assertThat(boundary.audits.getAudits()).contains(addedAudit());
            }

            Audit addedAudit() {
                return artifactAudit()
                    .change("group-id", null, groupId)
                    .change("artifact-id", null, artifactId)
                    .change("version", null, version)
                    .change("type", null, type)
                    .change("checksum", null, checksum)
                    .added();
            }

            void verifyRedeployed() {
                verifyRedeployExecuted();

                Checksum oldChecksum = (deployed == null) ? null : deployed.checksum;
                Version oldVersion = (deployed == null) ? null : deployed.version;
                assertThat(boundary.audits.getAudits()).contains(artifactAudit()
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
                assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                    .haveExactly(1, step(request));
            }

            void verifyRemoved() {
                verifyUndeployExecuted();
                assertThat(boundary.audits.getAudits()).contains(removedAudit());
            }

            private void verifyUndeployExecuted() {
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
                assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                    .haveExactly(1, step(undeploy))
                    .haveExactly(1, step(remove));
            }

            Audit removedAudit() {
                return artifactAudit()
                    .change("group-id", groupId, null)
                    .change("artifact-id", artifactId, null)
                    .change("version", version, null)
                    .change("type", type, null)
                    .change("checksum", checksum, null)
                    .removed();
            }

            DeployablePlan asPlan() {
                DeployablePlan plan = new DeployablePlan(deploymentName());
                plan
                    .setGroupId(groupId())
                    .setArtifactId(artifactId())
                    .setVersion(version);
                plan.setType(type)
                    .setState(deployed == null ? DeploymentState.undeployed : DeploymentState.deployed)
                    .setChecksum(checksum);
                return plan;
            }
        }
    }

    private List<Operation> operations;

    private String capturedOperationsDescription() {
        return "operations: " + capturedOperations().stream().map(Operation::getOperation)
            .map(ModelNode::toString).collect(joining("\n"));
    }

    @SneakyThrows(IOException.class) List<Operation> capturedOperations() {
        if (operations == null) {
            ArgumentCaptor<Operation> captor = ArgumentCaptor.forClass(Operation.class);
            verify(cli, atLeastOnce()).execute(captor.capture(), any(OperationMessageHandler.class));
            operations = captor.getAllValues();
        }
        return operations;
    }

    List<ModelNode> steps() {return capturedOperations().get(0).getOperation().get(STEPS).asList();}


    LoggerFixture givenLogger(String name) {return new LoggerFixture(name);}

    @Getter
    public class LoggerFixture extends AbstractFixture {
        private final LoggerCategory category;
        private final List<String> handlers = new ArrayList<>();
        private String level;
        private Boolean useParentHandlers = true;
        private boolean deployed;

        LoggerFixture(@NonNull String category) {
            this.category = LoggerCategory.of(category);

            whenCli(readLoggerRequest(category)).thenRaw(() -> deployed
                ? toModelNode("{" + deployedNode() + "}")
                : notDeployedNode("logging", "logger", category));
        }

        @Override public String toString() {return "Logger:" + category;}

        String deployedNode() {
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

        public LoggerFixture level(LogLevel level) {return level(level.name());}

        public LoggerFixture level(String level) {
            this.level = level;
            return this;
        }

        LoggerFixture handler(String handlerName) {
            this.handlers.add(handlerName);
            return this;
        }

        LoggerFixture useParentHandlers(Boolean useParentHandlers) {
            this.useParentHandlers = useParentHandlers;
            return this;
        }

        LoggerFixture pinned() {
            givenPinned("loggers", category.getValue());
            return this;
        }

        LoggerFixture deployed() {
            this.deployed = true;
            allLoggers.add("{" + loggerAddress() + deployedNode() + "}");
            return this;
        }

        String loggerAddress() {return address("logging", "logger", category);}

        @Override ModelNode addressNode() {
            return createAddress("subsystem", "logging", "logger", category.getValue());
        }

        private String handlersArrayNode() {
            if (handlers.size() == 1)
                return "['" + handlers.get(0) + "']";
            else
                return handlers.stream().collect(joining("',\n        '", "[\n        '", "'\n    ]"));
        }

        void verifyAdded() {
            ModelNode request = toModelNode("{\n"
                                            + loggerAddress()
                                            + "    'operation' => 'add',\n"
                                            + loggerAddress()
                                            + ((level == null) ? "" : "    'level' => '" + level + "',\n")
                                            + (handlers.isEmpty() ? "" : "    'handlers' => " + handlersArrayNode() + ",\n")
                                            + "    'use-parent-handlers' => " + useParentHandlers + "\n"
                                            + "}");
            assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                .haveExactly(1, step(request));

            Audit audit = new LoggerAudit()
                .category(getCategory())
                .change("level", null, level)
                .change("use-parent-handlers", null, useParentHandlers);
            if (!handlerNames().isEmpty())
                audit.change("handlers", null, handlerNames());
            assertThat(boundary.audits.getAudits()).contains(audit.added());
        }

        List<LogHandlerName> handlerNames() {
            return handlers.stream().map(LogHandlerName::new).collect(toList());
        }

        void verifyUpdatedLogLevelFrom(LogLevel oldLevel) {
            verifyWriteAttribute(addressNode(), "level", level);
            assertThat(boundary.audits.getAudits()).contains(
                new LoggerAudit().category(getCategory()).change("level", oldLevel, level).changed());
        }

        void verifyRemoved() {
            ModelNode request = toModelNode(""
                                            + "{\n"
                                            + loggerAddress()
                                            + "    'operation' => 'remove'\n"
                                            + "}");
            assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                .haveExactly(1, step(request));

            Audit audit = new LoggerAudit().category(getCategory());
            if (level != null)
                audit.change("level", level, null);
            if (useParentHandlers != null)
                audit.change("use-parent-handlers", useParentHandlers, null);
            if (!handlerNames().isEmpty())
                audit.change("handlers", handlerNames(), null);
            assertThat(boundary.audits.getAudits()).contains(audit.removed());
        }

        void verifyUpdatedUseParentHandlersFrom(Boolean oldUseParentHandlers) {
            verifyWriteAttribute(addressNode(), "use-parent-handlers", useParentHandlers);
            assertThat(boundary.audits.getAudits()).contains(
                new LoggerAudit().category(getCategory()).change("use-parent-handlers", oldUseParentHandlers, useParentHandlers)
                    .changed());
        }

        public void verifyAddedHandler(String name) {
            ModelNode request = toModelNode(""
                                            + "{\n"
                                            + loggerAddress()
                                            + "    'operation' => 'add-handler',\n"
                                            + "    'name' => '" + name + "'\n"
                                            + "}");
            assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                .haveExactly(1, step(request));
            assertThat(boundary.audits.getAudits()).contains(
                new LoggerAudit().category(getCategory()).change("handlers", null, "[" + name + "]").changed());
        }

        public void verifyRemovedHandler(String name) {
            ModelNode request = toModelNode(""
                                            + "{\n"
                                            + loggerAddress()
                                            + "    'operation' => 'remove-handler',\n"
                                            + "    'name' => '" + name + "'\n"
                                            + "}");
            assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                .haveExactly(1, step(request));
            assertThat(boundary.audits.getAudits()).contains(
                new LoggerAudit().category(getCategory()).change("handlers", "[" + name + "]", null).changed());
        }

        LoggerPlan asPlan() {
            return new LoggerPlan(category)
                .setState(deployed ? DeploymentState.deployed : DeploymentState.undeployed)
                .setLevel((level == null) ? null : LogLevel.valueOf(level))
                .setHandlers(handlerNames())
                .setUseParentHandlers((useParentHandlers == FALSE) ? false : null); // true -> null (default)
        }

        @Override Condition<Audit> forThisArtifact() {
            return new Condition<>(audit -> audit instanceof LoggerAudit && ((LoggerAudit) audit).category().equals(category), "for logger " + category);
        }
    }

    LogHandlerFixture givenLogHandler(LogHandlerType type, String name) {
        return new LogHandlerFixture(type, name);
    }

    @Getter
    public class LogHandlerFixture extends AbstractFixture {
        private final LogHandlerType type;
        private final LogHandlerName name;
        private final LogHandlerAudit expectedAudit;
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

        LogHandlerFixture(LogHandlerType type, String name) {
            this.type = type;
            this.name = new LogHandlerName(name);
            this.expectedAudit = new LogHandlerAudit().type(this.type).name(this.name);
            this.suffix = (type == periodicRotatingFile) ? DEFAULT_SUFFIX : null;

            whenCli(readLogHandlerRequest(type, name)).thenRaw(() -> deployed
                ? toModelNode("{" + deployedNode() + "}")
                : notDeployedNode("logging", type.getHandlerTypeName(), name));
        }

        String deployedNode() {
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

        LogHandlerFixture format(String format) {
            this.format = format;
            this.formatter = null;
            return this;
        }

        LogHandlerFixture formatter(String formatter) {
            this.format = null;
            this.formatter = formatter;
            return this;
        }

        public LogHandlerFixture file(String file) {
            this.file = file;
            return this;
        }

        LogHandlerFixture suffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        LogHandlerFixture encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        LogHandlerFixture module(String module) {
            this.module = module;
            return this;
        }

        LogHandlerFixture class_(String class_) {
            this.class_ = class_;
            return this;
        }

        LogHandlerFixture property(String key, String value) {
            if (this.properties == null)
                this.properties = new LinkedHashMap<>();
            this.properties.put(key, value);
            return this;
        }

        LogHandlerFixture pinned() {
            givenPinned("log-handlers", name.getValue());
            return this;
        }

        LogHandlerFixture deployed() {
            this.deployed = true;
            allLogHandlers.computeIfAbsent(type, t -> new ArrayList<>())
                .add("{" + logHandlerAddress() + deployedNode() + "}");
            return this;
        }


        private String logHandlerAddress() {
            return address("logging", type.getHandlerTypeName(), name);
        }

        @Override ModelNode addressNode() {
            return createAddress("subsystem", "logging", type.getHandlerTypeName(), name.getValue());
        }


        <T> LogHandlerFixture verifyChange(String name, T oldValue, T newValue) {
            verifyWriteAttribute(addressNode(), name, toStringOrNull(newValue));
            expectChange(name, oldValue, newValue);
            return this;
        }

        <T> LogHandlerFixture expectChange(String name, T oldValue, T newValue) {
            expectedAudit.change(name, oldValue, newValue);
            return this;
        }

        void verifyChanged() {
            assertThat(boundary.audits.getAudits()).contains(this.expectedAudit.changed());
        }

        void verifyPutProperty(String key, String value) {
            ModelNode request = createOperation("map-put", addressNode());
            request.get("name").set("property");
            request.get("key").set(key);
            request.get("value").set(value);

            assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                .haveExactly(1, step(request));
        }

        void verifyRemoveProperty(String key) {
            ModelNode request = createOperation("map-remove", addressNode());
            request.get("name").set("property");
            request.get("key").set(key);

            assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                .haveExactly(1, step(request));
        }

        void verifyAdded() {
            ModelNode request = toModelNode("{\n"
                                            + logHandlerAddress()
                                            + "    'operation' => 'add'"
                                            + ",\n    'level' => '" + ((level == null) ? "ALL" : level) + "'"
                                            + ((formatter == null) ? "" : ",\n    'named-formatter' => '" + formatter + "'")
                                            + ((format == null) ? "" : ",\n    'formatter' => '" + format + "'")
                                            + (hasFile()
                ? ",\n    'file' => {\n"
                  + "        'path' => '" + ((file == null) ? name.getValue().toLowerCase() + ".log" : file) + "',\n"
                  + "        'relative-to' => 'jboss.server.log.dir'\n"
                  + "    }"
                : "")
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
            assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                .haveExactly(1, step(request));

            expectedAudit.change("level", null, (level == null) ? ALL : level);
            if (format != null)
                expectedAudit.change("format", null, format);
            if (formatter != null)
                expectedAudit.change("formatter", null, formatter);
            if (encoding != null)
                expectedAudit.change("encoding", null, encoding);
            if (hasFile())
                expectedAudit.change("file", null, (file == null) ? name.getValue().toLowerCase() + ".log" : file);
            if (suffix != null)
                expectedAudit.change("suffix", null, suffix);
            if (module != null)
                expectedAudit.change("module", null, module);
            if (class_ != null)
                expectedAudit.change("class", null, class_);
            if (properties != null)
                properties.forEach((key, value) -> expectedAudit.change("property:" + key, null, value));
            assertThat(boundary.audits.getAudits()).contains(expectedAudit.added());
        }

        private boolean hasFile() {
            return type == periodicRotatingFile || (type == custom && file != null);
        }

        void verifyRemoved() {
            ModelNode request = toModelNode("{\n"
                                            + logHandlerAddress()
                                            + "    'operation' => 'remove'\n"
                                            + "}");
            assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                .haveExactly(1, step(request));

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
            assertThat(boundary.audits.getAudits()).contains(expectedAudit.removed());
        }

        LogHandlerPlan asPlan() {
            return new LogHandlerPlan(name)
                .setType(type)
                .setLevel(level)
                .setFormat(format)
                .setFormatter(formatter)
                .setFile(file)
                .setSuffix(suffix)
                .setEncoding(encoding)
                .setModule(module)
                .setClass_(class_)
                .setProperties((properties == null) ? emptyMap() : properties);
        }

        @Override Condition<Audit> forThisArtifact() {
            return new Condition<>(audit -> audit instanceof LogHandlerAudit && ((LogHandlerAudit) audit).name().equals(name), "for log handler " + name);
        }
    }


    DataSourceFixture givenDataSource(String name) {return new DataSourceFixture(name);}

    @Getter
    public class DataSourceFixture extends AbstractFixture {
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

        DataSourceFixture(@NonNull String name) {
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

        String deployedNode() {
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


        DataSourceFixture xa(boolean xa) {
            this.xa = xa;
            return this;
        }

        DataSourceFixture jndiName(String jndiName) {
            this.jndiName = jndiName;
            return this;
        }

        DataSourceFixture driver(String driver) {
            this.driver = driver;
            return this;
        }

        DataSourceFixture credentials(String userName, String password) {
            return userName(userName).password(password);
        }

        DataSourceFixture userName(String userName) {
            this.userName = userName;
            return this;
        }

        DataSourceFixture password(String password) {
            this.password = password;
            return this;
        }

        DataSourceFixture minPoolSize(Integer minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }

        DataSourceFixture initialPoolSize(Integer initialPoolSize) {
            this.initialPoolSize = initialPoolSize;
            return this;
        }

        DataSourceFixture maxPoolSize(Integer maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        DataSourceFixture maxAge(Age maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        DataSourceFixture processState(String processState) {
            AbstractDeployerTests.this.processState = processState;
            return this;
        }

        DataSourceFixture pinned() {
            givenPinned("data-sources", name.getValue());
            return this;
        }

        DataSourceFixture deployed() {
            this.deployed = true;
            (xa ? allXaDataSources : allNonXaDataSources).add("{" + dataSourceAddress() + deployedNode() + "}");
            return this;
        }

        String dataSourceAddress() {return address("datasources", dataSource(xa), name);}

        @Override ModelNode addressNode() {
            return createAddress("subsystem", "datasources", dataSource(xa), name.getValue());
        }

        void verifyAdded() {
            verifyAddCli();
            assertAddAudit();
        }

        void verifyAddCli() {
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
                assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                    .haveExactly(1, step(addRequest))
                    .haveExactly(1, step(addXaDataSourceProperty("ServerName", jdbcHost())))
                    .haveExactly(1, step(addXaDataSourceProperty("PortNumber", jdbcPort())))
                    .haveExactly(1,
                        step(addXaDataSourceProperty("DatabaseName", jdbcDbName())));
            } else {
                assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                    .haveExactly(1, step(addRequest));
            }
        }

        private String jdbcHost() {return jdbcUri().getHost();}

        private String jdbcPort() {return Integer.toString(jdbcUri().getPort());}

        private String jdbcDbName() {return jdbcUri().getPath().substring(1);}

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

        private void assertAddAudit() {
            Audit audit = new DataSourceAudit()
                .name(getName())
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
            assertThat(boundary.audits.getAudits()).containsExactly(audit.added());
        }

        void verifyUpdatedUriFrom(String oldUri) {
            verifyWriteAttribute(addressNode(), "uri", uri);
            assertThat(boundary.audits.getAudits()).contains(
                new DataSourceAudit().name(name).change("uri", oldUri, uri).changed());
        }

        void verifyUpdatedJndiNameFrom(String oldJndiName) {
            verifyWriteAttribute(addressNode(), "jndi-name", jndiName);
            assertThat(boundary.audits.getAudits()).contains(
                new DataSourceAudit().name(name).change("jndi-name", oldJndiName, jndiName).changed());
        }

        void verifyUpdatedDriverNameFrom(String oldDriverName) {
            verifyWriteAttribute(addressNode(), "driver-name", driver);
            assertThat(boundary.audits.getAudits()).contains(
                new DataSourceAudit().name(name).change("driver", oldDriverName, driver).changed());
        }

        void verifyUpdatedUserNameFrom(String oldUserName) {
            verifyWriteAttribute(addressNode(), "user-name", userName);
            assertThat(boundary.audits.getAudits()).contains(
                new DataSourceAudit().name(name)
                    .changeRaw("user-name",
                        oldUserName == null ? null : CONCEALED,
                        userName == null ? null : CONCEALED)
                    .changed());
        }

        void verifyUpdatedPasswordFrom(String oldPassword) {
            verifyWriteAttribute(addressNode(), "password", password);
            assertThat(boundary.audits.getAudits()).contains(
                new DataSourceAudit().name(name)
                    .changeRaw("password",
                        oldPassword == null ? null : CONCEALED,
                        password == null ? null : CONCEALED)
                    .changed());
        }

        void verifyUpdatedMinPoolSizeFrom(Integer oldMinPoolSize) {
            verifyWriteAttribute(addressNode(), "min-pool-size", minPoolSize);
            assertThat(boundary.audits.getAudits()).contains(
                new DataSourceAudit().name(name).change("pool:min", oldMinPoolSize, minPoolSize).changed());
        }

        void verifyUpdatedInitialPoolSizeFrom(Integer oldInitialPoolSize) {
            verifyWriteAttribute(addressNode(), "initial-pool-size", initialPoolSize);
            assertThat(boundary.audits.getAudits()).contains(
                new DataSourceAudit().name(name).change("pool:initial", oldInitialPoolSize, initialPoolSize).changed());
        }

        void verifyUpdatedMaxPoolSizeFrom(Integer oldMaxPoolSize) {
            verifyWriteAttribute(addressNode(), "max-pool-size", maxPoolSize);
            assertThat(boundary.audits.getAudits()).contains(
                new DataSourceAudit().name(name).change("pool:max", oldMaxPoolSize, maxPoolSize).changed());
        }

        void verifyUpdatedMaxAgeFrom(Age oldMaxAge) {
            verifyWriteAttribute(addressNode(), "idle-timeout-minutes", maxAge.asMinutes());
            assertThat(boundary.audits.getAudits()).contains(
                new DataSourceAudit().name(name).change("pool:max-age", oldMaxAge, maxAge).changed());
        }

        void verifyRemoved() {
            verifyRemoveCli();

            Audit audit = new DataSourceAudit()
                .name(getName())
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
            assertThat(boundary.audits.getAudits()).contains(audit.removed());
        }

        void verifyRemoveCli() {
            ModelNode request = toModelNode(""
                                            + "{\n"
                                            + "    'operation' => 'remove',\n"
                                            + dataSourceAddress().substring(0, dataSourceAddress().length() - 1)
                                            + "}");
            assertThat(capturedOperations()).describedAs(capturedOperationsDescription())
                .haveExactly(1, step(request));
        }

        DataSourcePlan asPlan() {
            return new DataSourcePlan(name)
                .setState(deployed ? DeploymentState.deployed : DeploymentState.undeployed)
                .setUri(URI.create(uri))
                .setJndiName(jndiName)
                .setDriver(driver)
                .setUserName(userName)
                .setPassword(password)
                .setPool(new PoolPlan()
                    .setMin(minPoolSize)
                    .setInitial(initialPoolSize)
                    .setMax(maxPoolSize)
                    .setMaxAge(maxAge));
        }

        void verifyReloadRequired() {verifyProcessState(reloadRequired);}

        void verifyRestartRequired() {verifyProcessState(restartRequired);}

        private void verifyProcessState(ProcessState processState) {assertThat(boundary.audits.getProcessState()).isEqualTo(processState);}

        @Override Condition<Audit> forThisArtifact() {
            return new Condition<>(audit -> audit instanceof DataSourceAudit && ((DataSourceAudit) audit).name().equals(name), "for artifact " + name);
        }
    }

    abstract class AbstractFixture {
        void verifyUnchanged() {
            verifyNoOperation();
            assertThat(boundary.audits.getAudits()).describedAs("audits").areNot(forThisArtifact());
            assertThat(boundary.audits.getProcessState()).describedAs("process state").isEqualTo(running);
        }

        abstract Condition<Audit> forThisArtifact();

        @SneakyThrows(IOException.class) void verifyNoOperation() {verify(cli, never()).execute(argThat(operationOnThis()), any(OperationMessageHandler.class));}

        @NotNull private ArgumentMatcher<Operation> operationOnThis() {
            return new ArgumentMatcher<>() {
                @Override public boolean matches(Operation op) {
                    ModelNode operation = op.getOperation();
                    assert operation.get(OP).asString().equals(COMPOSITE);
                    assert operation.get(ADDRESS).asList().isEmpty();
                    List<ModelNode> steps = operation.get(STEPS).asList();
                    return steps.stream().anyMatch(step -> matchAddress(step.get(ADDRESS)));
                }

                @Override public String toString() {return "operation on " + AbstractFixture.this;}
            };
        }

        private boolean matchAddress(ModelNode actualAddress) {
            return actualAddress.equals(addressNode());
        }

        abstract ModelNode addressNode();
    }
}
