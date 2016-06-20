package com.github.t1.deployer.app;

import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.as.controller.client.*;
import org.jboss.dmr.ModelNode;
import org.junit.*;
import org.mockito.*;

import java.io.*;
import java.util.*;

import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.repository.ArtifactoryMock.*;
import static org.mockito.Mockito.*;

@Slf4j
public class AbstractDeployerTest {
    @Mock Repository repository;
    @Mock ModelControllerClient jboss;

    @Mock DeploymentContainer deployments;
    @InjectMocks LoggerContainer loggers;

    @Mock LogHandler logHandlerMock;

    private final List<Deployment> allDeployments = new ArrayList<>();

    @Before
    public void before() {
        when(deployments.getAllDeployments()).then(invocation -> allDeployments);

        loggers.client = jboss;
    }

    @After
    public void afterDeployments() {
        verify(deployments, atLeast(0)).getAllDeployments();
        verify(deployments, atLeast(0)).hasDeployment(any(DeploymentName.class));
        verify(deployments, atLeast(0)).getDeployment(any(DeploymentName.class));

        verifyNoMoreInteractions(deployments);
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


    public CliFixture givenLogger(String name) {
        return givenResource().at("subsystem", "logging").at("logger", name);
    }

    public CliFixture givenLogHandler(LoggingHandlerType handlerType, String name) {
        return givenResource().at("subsystem", "logging").at(handlerType.getTypeName(), name);
    }

    public CliFixture givenResource() { return new CliFixture(); }

    public class CliFixture {
        private final ModelNode request = new ModelNode();
        private final ModelNode response = new ModelNode();

        public CliFixture at(String name, String value) {
            request.get("address").add(name, value);
            return this;
        }

        public CliFixture readResource() {
            return operation("read-resource").param("recursive", true);
        }

        public CliFixture operation(String name) {
            request.get("operation").set(name);
            return this;
        }

        public CliFixture param(String name, Object value) {
            param(request, name, value);
            return this;
        }

        public CliFixture result(String name, Object value) {
            param(response.get("result"), name, value);
            return this;
        }

        private void param(ModelNode node, String name, Object value) {
            //noinspection ChainOfInstanceofChecks
            if (value instanceof Boolean)
                node.get(name).set((Boolean) value);
            else if (value instanceof String)
                node.get(name).set((String) value);
            else if (value instanceof ModelNode)
                node.get(name).set((ModelNode) value);
            else
                throw new IllegalArgumentException("unknown object type for param: " + value.getClass());
        }

        public CliFixture notFound() {
            response.get("outcome").set("failed");
            response.get("failure-description")
                    .set("WFLYCTL0216: Management resource '" + request.get("address") + "' not found");
            response.get("rolled-back").set(true);

            return execute();
        }

        public CliFixture success() {
            response.get("outcome").set("success");

            return execute();
        }

        @SneakyThrows(IOException.class)
        private CliFixture execute() {
            // System.out.println("---------------------------------------------------------");
            // System.out.println(request);
            // System.out.println("---------------------------------------------------------");
            // System.out.println(response);
            // System.out.println("---------------------------------------------------------");
            when(jboss.execute(eq(request), any(OperationMessageHandler.class)))
                    .thenReturn(response);
            return this;
        }
    }
}
