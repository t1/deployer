package com.github.t1.deployer.app;

import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Expressions.*;
import com.github.t1.deployer.model.Plan.PlanBuilder;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.deployer.tools.KeyStoreConfig;
import com.github.t1.log.Logged;
import com.github.t1.problem.*;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.*;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.github.t1.deployer.app.Trigger.*;
import static com.github.t1.deployer.model.ProcessState.*;
import static com.github.t1.log.LogLevel.*;
import static com.github.t1.problem.WebException.*;
import static java.lang.Boolean.*;
import static java.nio.file.Files.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static javax.ws.rs.core.Response.Status.*;

@javax.ws.rs.Path("/")
@Stateless
@Logged(level = INFO)
@Slf4j
public class DeployerBoundary {
    public static final String IGNORE_SERVER_RELOAD = DeployerBoundary.class + "#IGNORE_SERVER_RELOAD";
    public static final String ROOT_BUNDLE_CONFIG_FILE = "deployer.root.bundle";
    private static final String DEFAULT_ROOT_BUNDLE = ""
            + "bundles:\n"
            + "  ${regex(root-bundle:artifact-id or hostName(), «(.*?)\\d*»)}:\n"
            + "    group-id: ${root-bundle:group-id or default.group-id or domainName()}\n"
            + "    classifier: ${root-bundle:classifier or null}\n"
            + "    version: ${root-bundle:version or version}\n";
    private static final VariableName NAME = new VariableName("name");
    private static final Object CONTAINER_LOCK = new Object();

    public Path getRootBundlePath() { return Container.getConfigDir().resolve(ROOT_BUNDLE_CONFIG_FILE); }


    @GET
    public Plan getEffectivePlan() {
        PlanBuilder builder = Plan.builder();
        deployers.forEach(deployer -> deployer.read(builder));
        return builder.build();
    }


    @POST
    public AuditsResponse post(Map<String, String> form) {

        apply(post, mapVariableNames(form));

        if (reloadRequired()) {
            container.suspend();
            container.reload();
        }

        return new AuditsResponse(audits.getAudits(), audits.getWarnings(), audits.getProcessState());
    }

    private boolean reloadRequired() {
        return audits.getProcessState() != running && !Boolean.getBoolean(IGNORE_SERVER_RELOAD);
    }

    private Map<VariableName, String> mapVariableNames(Map<String, String> form) {
        return (form == null)
                ? emptyMap()
                : form.entrySet().stream()
                      .collect(toMap(entry -> new VariableName(entry.getKey()), Map.Entry::getValue));
    }


    @GET
    @javax.ws.rs.Path("/repository/versions")
    public List<Version> getVersions(
            @QueryParam("groupId") @NotNull GroupId groupId,
            @QueryParam("artifactId") @NotNull ArtifactId artifactId) {
        return repository.listVersions(groupId, artifactId, false);
    }

    @GET
    @javax.ws.rs.Path("/variables")
    public Set<VariableName> getVariables() {
        String plan = readPlan();
        return scanVariableNames(plan);
    }

    private String readPlan() {
        Reader reader = hasRootBundleConfigFile() ? reader(getRootBundlePath()) : rootBundleReader();
        return new Scanner(reader).useDelimiter("\\A").next();
    }

    private Reader rootBundleReader() {
        AtomicBoolean hasUnresolvedVariables = new AtomicBoolean(false);
        Expressions expressions = expressions().withFinalResolver(expression -> {
            log.debug("unresolved variable in default root bundle: {}", expression);
            hasUnresolvedVariables.set(true);
            return Match.of(expression);
        });
        Plan defaultRootBundle = Plan.load(expressions, new StringReader(DEFAULT_ROOT_BUNDLE), "root bundle");
        if (hasUnresolvedVariables.get())
            return new StringReader(DEFAULT_ROOT_BUNDLE);
        Map<BundleName, BundlePlan> bundles = defaultRootBundle.getBundles();
        assert bundles.size() == 1 : "expected default root bundle to have exactly one bundle";
        BundlePlan bundle = bundles.values().iterator().next();
        Artifact artifact = repository.resolveArtifact(bundle.getGroupId(), bundle.getArtifactId(),
                bundle.getVersion(), ArtifactType.bundle, bundle.getClassifier());
        if (artifact == null)
            throw badRequest("root bundle not found: " + bundle);
        return artifact.getReader();
    }

    private Set<VariableName> scanVariableNames(String plan) {
        Set<VariableName> result = new TreeSet<>();
        Expressions expressions = new Expressions().withFinalResolver(expression -> {
            result.add(new VariableName(expression));
            return Match.of(expression);
        });
        Stream.of(plan.split("\n")).forEach(expressions::resolve);
        return result;
    }


    @Asynchronous
    public void applyAsync(Trigger trigger) {
        Container.waitForMBean();
        container.waitForBoot();

        try {
            apply(trigger, emptyMap());
        } catch (RuntimeException e) {
            // not really nice, but seems - over all - better than splitting and repeating the overall control flow
            for (Throwable cause = e; cause != null; cause = cause.getCause())
                if (cause instanceof UnresolvedVariableException)
                    log.info("skip async run for unresolved variable: {}",
                            ((UnresolvedVariableException) cause).getExpression());
            throw e;
        }

        if (rootBundleConfig != null && rootBundleConfig.getShutdownAfterBoot() == TRUE)
            container.shutdown();
    }


    @Inject Principal principal;
    @Inject Container container;
    @Inject Repository repository;

    @Inject @Config("variables") Map<VariableName, String> configuredVariables;
    @Inject @Config("root-bundle") RootBundleConfig rootBundleConfig;
    @Inject @Config("key-store") KeyStoreConfig keyStore;
    @Inject @Config("triggers") Set<Trigger> triggers;
    @Inject @Config("use.default.config") boolean useDefaultConfig;

    @Inject Audits audits;
    @Inject Instance<Deployer> deployers;

    private Expressions expressions() {
        return new Expressions()
                .withAllNew(configuredVariables)
                .withRootBundleConfig(rootBundleConfig)
                .withKeyStore(keyStore);
    }

    public void apply(Trigger trigger, Map<VariableName, String> variables) {
        synchronized (CONTAINER_LOCK) {
            if (triggers.contains(trigger)) {
                Execution execution = new Execution().withVariables(variables);

                try {
                    container.startBatch();
                    if (hasRootBundleConfigFile()) {
                        Path plan = getRootBundlePath();
                        log.info("load plan from: {}", plan);
                        execution.apply(reader(plan), plan.toString());
                    } else if (useDefaultConfig) {
                        throw new RuntimeException("For security reasons, applying the default root bundle "
                                + "is only allowed when there is a configuration file. "
                                + "See https://github.com/t1/deployer/issues/61");
                    } else {
                        log.info("load default root plan");
                        execution.apply(new StringReader(DEFAULT_ROOT_BUNDLE), "default root bundle");
                    }
                } catch (RuntimeException e) {
                    container.rollbackBatch();
                    throw e;
                }
                ProcessState processState = container.commitBatch();

                audits.setProcessState(processState);
                audits.applied(trigger, principal, variables, audits);
            } else {
                log.info("ignoring disabled trigger {}", trigger);
            }
        }
    }

    private boolean hasRootBundleConfigFile() { return isRegularFile(getRootBundlePath()); }

    private BufferedReader reader(Path plan) {
        try {
            return Files.newBufferedReader(plan);
        } catch (IOException e) {
            throw new RuntimeException("can't read plan [" + plan + "]", e);
        }
    }


    private class Execution {
        private Expressions expressions = expressions();

        private Execution withVariables(Map<VariableName, String> variables) {
            this.expressions = this.expressions.withAllNew(variables);
            return this;
        }

        private void apply(Reader reader, String sourceMessage) {
            String failureMessage = "can't apply plan [" + sourceMessage + "]";
            try {
                this.apply(Plan.load(expressions, reader, sourceMessage));
            } catch (WebApplicationApplicationException e) {
                log.info(failureMessage);
                throw e;
            } catch (RuntimeException e) {
                log.debug(failureMessage, e);
                for (Throwable cause = e; cause != null; cause = cause.getCause())
                    if (cause.getMessage() != null && !cause.getMessage().isEmpty())
                        failureMessage += ": " + cause.getMessage();
                throw WebException
                        .builderFor(BAD_REQUEST)
                        .causedBy(e)
                        .detail(failureMessage)
                        .build();
            }
        }

        private void apply(Plan plan) {
            deployers.forEach(deployer -> deployer.apply(plan));

            plan.bundles().forEach(this::applyBundle);
        }

        private void applyBundle(BundlePlan bundle) {
            bundle.actualInstances().forEach(instance -> {
                Expressions pop = this.expressions;
                try {
                    if (instance.getKey() != null)
                        this.expressions = this.expressions.with(NAME, instance.getKey());
                    this.expressions = this.expressions.withAllReplacing(instance.getValue());
                    Artifact artifact = repository.resolveArtifact(bundle.getGroupId(), bundle.getArtifactId(),
                            bundle.getVersion(), ArtifactType.bundle, bundle.getClassifier());
                    if (artifact == null)
                        throw badRequest("bundle not found: " + bundle);
                    apply(artifact.getReader(), artifact.toString());
                } finally {
                    this.expressions = pop;
                }
            });
        }
    }
}
