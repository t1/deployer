package com.github.t1.deployer.container;

import com.github.t1.deployer.model.Checksum;
import com.github.t1.deployer.model.DeploymentName;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;

import java.io.InputStream;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentName.ALL;
import static java.lang.Boolean.TRUE;
import static java.util.Comparator.comparing;
import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTENT;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_UNDEPLOY_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.Operations.createAddress;
import static org.jboss.as.controller.client.helpers.Operations.createOperation;
import static org.jboss.as.controller.client.helpers.Operations.createRemoveOperation;

@Slf4j
@Getter @Setter @Accessors(fluent = true, chain = true)
public class DeploymentResource extends AbstractResource<DeploymentResource> {
    public static final String WAR_SUFFIX = ".war";
    private static final int TIMEOUT = 30;

    @NonNull @Getter private final DeploymentName name;
    private Checksum checksum;
    private InputStream inputStream;

    public DeploymentResource(DeploymentName name, Batch batch) {
        super(batch);
        this.name = name;
    }

    public static Stream<DeploymentResource> allDeployments(Batch batch) {
        return batch.readResource(address(ALL))
            .map(match -> toDeployment(match.get("result"), batch))
            .sorted(comparing(DeploymentResource::name));
    }

    private static DeploymentResource toDeployment(ModelNode node, Batch batch) {
        DeploymentName name = readName(node);
        Checksum hash = readHash(node);
        log.debug("read deployment '{}' [{}]", name, hash);
        return new DeploymentResource(name, batch).checksum(hash);
    }

    @Override public String toString() {
        return name
            + ((checksum == null) ? "" : ":" + checksum)
            + ((deployed == null) ? ":?" : deployed ? ":deployed" : ":undeployed");
    }

    public Checksum checksum() {
        checkDeployed();
        return checksum;
    }

    @Override protected ModelNode address() { return address(name); }

    private static ModelNode address(DeploymentName name) { return createAddress("deployment", name.getValue()); }

    @Override protected void readFrom(ModelNode node) {
        DeploymentName name = readName(node);
        Checksum checksum = readHash(node);
        log.debug("read deployment {}: {}", name, checksum);
        assert this.name.equals(name);
        this.checksum = checksum;
    }

    private static DeploymentName readName(ModelNode node) { return new DeploymentName(node.get("name").asString()); }

    private static Checksum readHash(ModelNode node) { return Checksum.of(hash(node)); }

    public static byte[] hash(ModelNode cliDeployment) {
        try {
            return cliDeployment.get("content").get(0).get("hash").asBytes();
        } catch (RuntimeException e) {
            log.error("failed to get hash for {}", cliDeployment.get("name"));
            return new byte[0];
        }
    }

    @Override public void add() {
        assert inputStream != null : "need an input stream to deploy";
        addDeployOperation(ADD, address());
        this.deployed = true;
    }

    public void redeploy() {
        checkDeployed();
        assert deployed == TRUE;
        addDeployOperation("full-replace-deployment", new ModelNode().setEmptyList());
    }

    private void addDeployOperation(String operationName, ModelNode address) {
        assert inputStream != null : "need an input stream to redeploy";
        int index = addInputStreamAndReturnIndex(inputStream);
        ModelNode operation = createOperation(operationName, address);
        operation.get("enabled").set(true);
        if (address.asList().isEmpty())
            operation.get(NAME).set(name.getValue());
        operation.get(CONTENT).set(new ModelNode().add(new ModelNode().set(INPUT_STREAM_INDEX, index)));
        addStep(operation);
    }

    @Override public void addRemoveStep() {
        addStep(createOperation(DEPLOYMENT_UNDEPLOY_OPERATION, address()));
        addStep(createRemoveOperation(address()));
        this.deployed = false;
    }

    @Override public String getId() {
        String nameString = name().getValue();
        if (nameString.endsWith(WAR_SUFFIX))
            nameString = nameString.substring(0, nameString.length() - WAR_SUFFIX.length());
        return nameString;
    }
}
