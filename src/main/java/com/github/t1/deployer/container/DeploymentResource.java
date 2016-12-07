package com.github.t1.deployer.container;

import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.DeploymentName;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.dmr.ModelNode;
import org.wildfly.plugin.core.*;

import java.io.InputStream;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.t1.deployer.model.DeploymentName.*;
import static java.util.Comparator.*;
import static org.jboss.as.controller.client.helpers.Operations.createAddress;
import static org.wildfly.plugin.core.DeploymentOperations.*;

@Slf4j
@Builder(builderMethodName = "do_not_call", buildMethodName = "get")
@Accessors(fluent = true, chain = true)
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

    public static DeploymentResourceBuilder builder(DeploymentName name, Batch batch) {
        DeploymentResourceBuilder builder = new DeploymentResourceBuilder().name(name);
        builder.batch = batch;
        return builder;
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
        return DeploymentResource.builder(name, batch).checksum(hash).get();
    }

    public static class DeploymentResourceBuilder implements Supplier<DeploymentResource> {
        private Batch batch;

        @Override public DeploymentResource get() {
            DeploymentResource resource = new DeploymentResource(name, batch);
            resource.inputStream = inputStream;
            resource.checksum = checksum;
            return resource;
        }
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

    private static byte[] hash(ModelNode cliDeployment) {
        try {
            return cliDeployment.get("content").get(0).get("hash").asBytes();
        } catch (RuntimeException e) {
            log.error("failed to get hash for {}", cliDeployment.get("name"));
            return new byte[0];
        }
    }

    @Override public void add() {
        assert inputStream != null : "need an input stream to deploy";
        Deployment deployment = deployment(inputStream);
        writeOp(createAddDeploymentOperation(deployment));
        this.deployed = true;
    }

    public void redeploy(InputStream inputStream) {
        assert inputStream != null : "need an input stream to redeploy";
        Deployment deployment = deployment(inputStream);
        writeOp(createReplaceOperation(deployment));
    }

    private Deployment deployment(InputStream inputStream) { return Deployment.of(inputStream, name.getValue()); }

    @Override public void remove() {
        UndeployDescription deployment = UndeployDescription.of(name.getValue()).setFailOnMissing(true);
        writeOp(createUndeployOperation(deployment));
        this.deployed = false;
    }

    @Override public String getId() {
        String nameString = name().getValue();
        if (nameString.endsWith(WAR_SUFFIX))
            nameString = nameString.substring(0, nameString.length() - WAR_SUFFIX.length());
        return nameString;
    }
}
