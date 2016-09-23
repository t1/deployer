package com.github.t1.deployer.groovy;

import groovy.lang.GroovyClassLoader;
import lombok.SneakyThrows;
import org.junit.Test;

import java.io.*;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;

public class UpdatesTest {
    private static final File SCRIPT = new File("src/test/java/com/github/t1/deployer/groovy/updates.groovy");
    private static final Class<?> UPDATES_CLASS = loadClass();

    @SneakyThrows(IOException.class) private static Class loadClass() {
        return new GroovyClassLoader().parseClass(SCRIPT);
    }

    Object updates;

    public <T> T invoke(String methodName) throws ReflectiveOperationException {
        //noinspection unchecked
        return (T) UPDATES_CLASS.getMethod(methodName).invoke(updates);
    }

    public void building(String building, String... updates) throws ReflectiveOperationException {
        this.updates = UPDATES_CLASS.getConstructor(String.class).newInstance(prefix("[INFO] ", ""
                + "Scanning for projects...\n"
                + "\n"
                + "------------------------------------------------------------------------\n"
                + "" + building + "\n"
                + "------------------------------------------------------------------------\n"
                + "\n"
                + "--- versions-maven-plugin:2.3:update-properties (default-cli) @ deployer ---\n"
                + Stream.of(updates).collect(joining("\n")) + "\n"
                + "------------------------------------------------------------------------\n"
                + "BUILD SUCCESS\n"
                + "------------------------------------------------------------------------\n"
                + "Total time: 1.143 s\n"
                + "Finished at: 2016-09-22T04:41:02+02:00\n"
                + "Final Memory: 14M/245M\n"
                + "------------------------------------------------------------------------\n"));
    }

    private String prefix(String prefix, String string) {
        return Stream.of(string.split("\n")).map(line -> prefix + line).collect(joining("\n"));
    }

    @Test
    public void shouldParseVersion() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT");

        Object version = invoke("getCurrentVersion");

        assertThat(version).isEqualTo("2.3.2-SNAPSHOT");
    }

    @Test
    public void shouldParseWithNonNumericVersion() throws Exception {
        building("Building Deployer 1.0-beta3");

        Object version = invoke("getCurrentVersion");

        assertThat(version).isEqualTo("1.0-beta3");
    }

    @Test
    public void shouldParseVersionWithTwoWordName() throws Exception {
        building("Building The Deployer 1.0");

        Object version = invoke("getCurrentVersion");

        assertThat(version).isEqualTo("1.0");
    }

    @Test
    public void shouldParseVersionWithNumericWordName() throws Exception {
        building("Building The Deployer 2 1.0.0");

        Object version = invoke("getCurrentVersion");

        assertThat(version).isEqualTo("1.0.0");
    }

    @Test
    public void shouldParseUpdateEmpty() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT");

        boolean empty = invoke("isEmpty");

        assertThat(empty).isTrue();
    }

    @Test
    public void shouldParseUpdateNotEmpty() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT", "Updated ${jackson.version} from 2.7.5 to 2.8.1");

        boolean empty = invoke("isEmpty");

        assertThat(empty).isFalse();
    }

    @Test
    public void shouldParseUpdate() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT", "Updated ${jackson.version} from 2.7.5 to 2.8.1");

        Map<String, ?> updates = invoke("getUpdates");

        assertThat(updates).containsOnlyKeys("jackson.version");
        assertThat(updates.get("jackson.version")).hasToString("2.7.5 -> 2.8.1");
    }

    @Test
    public void shouldCalculateNextMicroVersion() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT", "Updated ${jackson.version} from 2.7.5 to 2.7.6");

        Object version = invoke("updateVersion");

        assertThat(version).isEqualTo("2.3.3");
    }

    @Test
    public void shouldCalculateNextMinorVersion() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT", "Updated ${jackson.version} from 2.7.5 to 2.8.1");

        Object version = invoke("updateVersion");

        assertThat(version).isEqualTo("2.4.0");
    }

    @Test
    public void shouldCalculateNextLongerMinorVersion() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT", "Updated ${jackson.version} from 2.7.5 to 2.8");

        Object version = invoke("updateVersion");

        assertThat(version).isEqualTo("2.4.0");
    }

    @Test
    public void shouldLengthenNextMinorVersion() throws Exception {
        building("Building Deployer 2-SNAPSHOT", "Updated ${jackson.version} from 2.7.5 to 2.8.1");

        Object version = invoke("updateVersion");

        assertThat(version).isEqualTo("2.1");
    }

    @Test
    public void shouldCalculateNextMajorVersion() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT", "Updated ${jackson.version} from 1.7.5 to 2.8.1");

        Object version = invoke("updateVersion");

        assertThat(version).isEqualTo("3.0.0");
    }

    @Test
    public void shouldCalculateNextVersionWithNonNumericSuffix() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT", "Updated ${postgresql.version} from 9.4.1211 to 9.4.1211.jre7");

        Object version = invoke("updateVersion");

        assertThat(version).isEqualTo("2.3.2.1");
    }

    @Test
    public void shouldCalculateNextVersionWithNonNumeric() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT", "Updated ${postgresql.version} from 9.4.1210 to 9.4.1211.jre7");

        Object version = invoke("updateVersion");

        assertThat(version).isEqualTo("2.3.3");
    }

    @Test
    public void shouldCalculateNextMinorAndMicroVersion() throws Exception {
        building("Building Deployer 2.3.2-SNAPSHOT",
                "Updated ${foo} from 2.7.5 to 2.7.6",
                "Updated ${bar} from 1.7 to 1.8");

        Object version = invoke("updateVersion");

        assertThat(version).isEqualTo("2.4.0");
    }
}
