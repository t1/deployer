package com.github.t1.deployer.groovy;

import groovy.lang.GroovyClassLoader;
import lombok.SneakyThrows;
import org.junit.Test;

import java.io.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class UpdatesTest {
    private static final File SCRIPT = new File("src/test/java/com/github/t1/deployer/groovy/updates.groovy");

    private static final String MVN_OUT = ""
            + "[INFO] Scanning for projects...\n"
            + "[INFO]                                                                         \n"
            + "[INFO] ------------------------------------------------------------------------\n"
            + "[INFO] Building Deployer 2.3.2-SNAPSHOT\n"
            + "[INFO] ------------------------------------------------------------------------\n"
            + "[INFO] \n"
            + "[INFO] --- versions-maven-plugin:2.3:update-properties (default-cli) @ deployer ---\n"
            + "[INFO] Updated ${jackson.version} from 2.7.5 to 2.8.1\n"
            + "[INFO] ------------------------------------------------------------------------\n"
            + "[INFO] BUILD SUCCESS\n"
            + "[INFO] ------------------------------------------------------------------------\n"
            + "[INFO] Total time: 1.143 s\n"
            + "[INFO] Finished at: 2016-09-22T04:41:02+02:00\n"
            + "[INFO] Final Memory: 14M/245M\n"
            + "[INFO] ------------------------------------------------------------------------\n";

    private static final Class<?> UPDATES_CLASS = load();

    @SneakyThrows(IOException.class) private static Class load() { return new GroovyClassLoader().parseClass(SCRIPT); }

    public <T> T invoke(String methodName) throws ReflectiveOperationException {
        Object updates = UPDATES_CLASS.getConstructor(String.class).newInstance(MVN_OUT);
        //noinspection unchecked
        return (T) UPDATES_CLASS.getMethod(methodName).invoke(updates);
    }

    @Test
    public void shouldParseVersion() throws Exception {
        Object version = invoke("getVersion");

        assertThat(version).isEqualTo("2.3.2-SNAPSHOT");
    }

    @Test
    public void shouldParseUpdateNotEmpty() throws Exception {
        boolean empty = invoke("isEmpty");

        assertThat(empty).isFalse();
    }

    @Test
    public void shouldParseUpdate() throws Exception {
        Map<String, ?> updates = invoke("getUpdates");

        assertThat(updates).containsOnlyKeys("jackson.version");
        assertThat(updates.get("jackson.version")).hasToString("2.7.5 -> 2.8.1");
    }
}
