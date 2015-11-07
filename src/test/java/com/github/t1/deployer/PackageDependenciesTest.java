package com.github.t1.deployer;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.github.t1.deployer.app.Deployments;
import com.github.t1.deployer.app.html.DeployerPage;
import com.github.t1.deployer.tools.*;

import jdepend.framework.*;

public class PackageDependenciesTest {
    private final JDepend jdepend = new JDepend();
    private final DependencyConstraint constraint = new DependencyConstraint();

    @Before
    public void setup() throws Exception {
        jdepend.addDirectory("target/classes");
        setupFilter();
        setupDependencies(Deployments.class, DeployerPage.class, ConfigProducer.class);
        jdepend.analyze();
    }

    private void setupFilter() {
        PackageFilter filter = new PackageFilter();
        filter.addPackage("java.*");
        filter.addPackage("javax.*");
        filter.addPackage("lombok");
        filter.addPackage("org.slf4j");
        filter.addPackage("com.github.t1.log");
        jdepend.setFilter(filter);
    }

    private void setupDependencies(Class<?>... types) {
        for (Class<?> type : types)
            loadDependenciesOf(type.getPackage());
    }

    private JavaPackage loadDependenciesOf(Package pkg) {
        JavaPackage result = new JavaPackage(pkg.getName());
        for (Package target : dependenciesOf(pkg))
            result.dependsUpon(loadDependenciesOf(target));
        constraint.addPackage(result);
        return result;
    }

    private List<Package> dependenciesOf(Package source) {
        List<Package> result = new ArrayList<>();
        if (source.isAnnotationPresent(DependsUpon.class))
            for (Class<?> target : source.getAnnotation(DependsUpon.class).value())
                result.add(target.getPackage());
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
