package com.github.t1.deployer;

import static org.junit.Assert.*;

import java.util.Collection;

import jdepend.framework.*;

import org.junit.*;

import com.github.t1.deployer.app.Deployments;
import com.github.t1.deployer.app.html.Index;
import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.Deployment;
import com.github.t1.deployer.repository.Repository;
import com.github.t1.deployer.tools.RestClient;

public class PackageDependenciesTest {
    private final JDepend jdepend = new JDepend();
    private final DependencyConstraint constraint = new DependencyConstraint();

    public static class Package extends JavaPackage {
        public Package(String name) {
            super(name);
        }

        public void dependsUpon(Package... packages) {
            for (Package p : packages) {
                super.dependsUpon(p);
            }
        }
    }

    @Before
    public void setup() throws Exception {
        jdepend.addDirectory("target/classes");
        setupFilter();
        setupDependencies();
        jdepend.analyze();
    }

    private void setupFilter() {
        PackageFilter filter = new PackageFilter();
        filter.addPackage("java.*");
        filter.addPackage("javax.*");
        filter.addPackage("lombok");
        filter.addPackage("org.joda.*");
        filter.addPackage("org.slf4j");
        filter.addPackage("com.github.t1.log");
        jdepend.setFilter(filter);
    }

    private void setupDependencies() {
        Package app = packageOf(Deployments.class);
        Package html = packageOf(Index.class);
        Package container = packageOf(Container.class);
        Package model = packageOf(Deployment.class);
        Package repository = packageOf(Repository.class);
        Package tools = packageOf(RestClient.class);

        Package credentials = packageOf(org.apache.http.auth.Credentials.class);

        app.dependsUpon(model, container, repository, tools);
        html.dependsUpon(model, repository, app); // app for resource paths

        container.dependsUpon(
                model,
                tools, //
                packageOf(org.jboss.as.controller.client.ModelControllerClient.class),
                packageOf(org.jboss.as.controller.client.helpers.standalone.DeploymentPlan.class),
                packageOf(org.jboss.dmr.ModelNode.class));
        repository.dependsUpon(model, tools, credentials);

        model.dependsUpon(packageOf(com.fasterxml.jackson.annotation.JsonCreator.class),
                packageOf(org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.class));
        tools.dependsUpon( //
                packageOf("org.jboss.as.controller.client"), // config -> ModelControllerClient
                packageOf("org.codehaus.jackson.map"), // RestClient -> ObjectMapper
                packageOf("org.apache.http"), //
                packageOf("org.apache.http.auth"), //
                packageOf("org.apache.http.client"), //
                packageOf("org.apache.http.client.methods"), //
                packageOf("org.apache.http.client.protocol"), //
                packageOf("org.apache.http.impl.auth"), //
                packageOf("org.apache.http.impl.client"), //
                packageOf("org.apache.http.util") //
        );
    }

    private Package packageOf(Class<?> type) {
        return packageOf(type.getPackage().getName());
    }

    private Package packageOf(String packageName) {
        Package result = new Package(packageName);
        constraint.addPackage(result);
        return result;
    }

    private abstract static class DependencyPredicate {
        public abstract boolean apply(JavaPackage javaPackage, JavaPackage efferent);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected boolean containsDependency(Collection packages, JavaPackage javaPackage, JavaPackage efferent) {
            for (JavaPackage candidate : (Collection<JavaPackage>) packages) {
                if (equals(javaPackage, candidate)) {
                    Collection<JavaPackage> candidateEfferents = candidate.getEfferents();
                    for (JavaPackage candidateEfferent : candidateEfferents) {
                        if (equals(efferent, candidateEfferent)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        protected boolean equals(JavaPackage left, JavaPackage right) {
            return right.getName().equals(left.getName());
        }
    }

    @Test
    public void shouldHaveNoCycles() {
        checkDependencies("cyclic dependencies", jdepend.getPackages(), new DependencyPredicate() {
            @Override
            public boolean apply(JavaPackage javaPackage, JavaPackage efferent) {
                return efferent.containsCycle();
            }
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void checkDependencies(String message, Collection packages, DependencyPredicate predicate) {
        StringBuilder out = new StringBuilder();
        for (JavaPackage javaPackage : (Collection<JavaPackage>) packages) {
            for (JavaPackage efferent : (Collection<JavaPackage>) javaPackage.getEfferents()) {
                if (predicate.apply(javaPackage, efferent)) {
                    out.append(javaPackage.getName()).append(" -> ").append(efferent.getName()).append("\n");
                }
            }
        }
        if (out.length() > 0) {
            fail(message + ":\n" + out);
        }
    }

    @Test
    public void shouldHaveOnlyDefinedDependencies() {
        checkDependencies("unexpected dependencies", jdepend.getPackages(), new DependencyPredicate() {
            @Override
            public boolean apply(JavaPackage javaPackage, JavaPackage efferent) {
                return !isExpected(javaPackage, efferent);
            }

            private boolean isExpected(JavaPackage javaPackage, JavaPackage efferent) {
                return containsDependency(constraint.getPackages(), javaPackage, efferent);
            }
        });
    }

    @Test
    public void shouldHaveNoSpecifiedButUnrealizedDependencies() {
        checkDependencies("specified but unrealized dependencies", constraint.getPackages(), new DependencyPredicate() {
            @Override
            public boolean apply(JavaPackage javaPackage, JavaPackage efferent) {
                return !containsDependency(jdepend.getPackages(), javaPackage, efferent);
            }
        });
    }
}
