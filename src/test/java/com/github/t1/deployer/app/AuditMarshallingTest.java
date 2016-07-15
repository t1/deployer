package com.github.t1.deployer.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.container.LoggerCategory;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.github.t1.log.LogLevel.*;
import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

public class AuditMarshallingTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory()
            .enable(MINIMIZE_QUOTES).disable(WRITE_DOC_START_MARKER))
            .setSerializationInclusion(NON_EMPTY)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    public Audit deserialize(String json) throws IOException {
        return JSON.readValue(json.replace('\'', '\"'), Audit.class);
    }

    private static final TypeReference<List<Audit>> AUDITS = new TypeReference<List<Audit>>() {};


    private static final ArtifactAuditBuilder JOLOKIA =
            ArtifactAudit.of("org.jolokia", "jolokia-war", "1.3.2").name("jolokia");

    private static final ArtifactAuditBuilder MOCKSERVER =
            ArtifactAudit.of("org.mock-server", "mockserver-war", "3.10.4").name("mockserver");

    private static LoggerAudit.LoggerAuditBuilder deployerLog() {
        return LoggerAudit.of(LoggerCategory.of("com.github.t1.deployer"));
    }


    private static final Audit[] TWO_AUDITS = {
            deployerLog().update(INFO, DEBUG).added(),
            MOCKSERVER.removed()
    };

    private static final String TWO_AUDITS_JSON =
            (""
                     + "[{"
                     + "'type':'logger',"
                     + "'change':'added',"
                     + "'updates':["
                     + "{'type':'com.github.t1.log.LogLevel','oldValue':'INFO','newValue':'DEBUG'}"
                     + "],"
                     + "'category':'com.github.t1.deployer'"
                     + "},{"
                     + "'type':'artifact',"
                     + "'change':'removed',"
                     + "'updates':[],"
                     + "'name':'mockserver',"
                     + "'groupId':'org.mock-server',"
                     + "'artifactId':'mockserver-war',"
                     + "'version':'3.10.4'"
                     + "}]"
            ).replace('\'', '\"');


    private static final String TWO_AUDITS_YAML = ""
            + "- type: logger\n"
            + "  change: added\n"
            + "  updates:\n"
            + "  - type: com.github.t1.log.LogLevel\n"
            + "    oldValue: INFO\n"
            + "    newValue: DEBUG\n"
            + "  category: com.github.t1.deployer\n"
            + "- type: artifact\n"
            + "  change: removed\n"
            + "  name: mockserver\n"
            + "  groupId: org.mock-server\n"
            + "  artifactId: mockserver-war\n"
            + "  version: 3.10.4\n";


    @Test
    public void shouldFailToDeserializeUnknownArtifactType() throws Exception {
        String json = "{'type':'xxx','change':'added'}";

        assertThatThrownBy(() -> deserialize(json))
                .hasMessageContaining("unsupported audit type: 'xxx'");
    }


    @Test
    public void shouldDeserializeDeployedArtifactAudit() throws Exception {
        String json = "{"
                + "'type':'artifact',"
                + "'change':'added',"
                + "'name':'jolokia',"
                + "'groupId':'org.jolokia',"
                + "'artifactId':'jolokia-war',"
                + "'version':'1.3.2'"
                + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(JOLOKIA.added());
    }


    @Test
    public void shouldDeserializeUndeployedArtifactAudit() throws Exception {
        String json = "{"
                + "'type':'artifact',"
                + "'change':'removed',"
                + "'name':'mockserver',"
                + "'groupId':'org.mock-server',"
                + "'artifactId':'mockserver-war',"
                + "'version':'3.10.4'"
                + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(MOCKSERVER.removed());
    }


    @Test
    public void shouldFailToDeserializeArtifactAuditWithUnknownState() throws Exception {
        String json = "{"
                + "'type':'artifact',"
                + "'change':'xxx',"
                + "'name':'mockserver',"
                + "'groupId':'org.mock-server',"
                + "'artifactId':'mockserver-war',"
                + "'version':'3.10.4'"
                + "}";

        assertThatThrownBy(() -> deserialize(json))
                .hasMessageContaining("unsupported audit change: 'xxx'");
    }


    @Test
    public void shouldDeserializeDeployedLoggerAudit() throws Exception {
        String json = "{"
                + "'type':'logger',"
                + "'change':'added',"
                + "'category':'com.github.t1.deployer',"
                + "'level':'DEBUG'"
                + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(deployerLog().added());
    }


    @Test
    public void shouldDeserializeTwoAuditsFromJson() throws Exception {
        List<Audit> audits = JSON.readValue(TWO_AUDITS_JSON, AUDITS);

        assertThat(audits).containsExactly(TWO_AUDITS);
    }


    @Test
    public void shouldSerializeTwoAuditsAsJson() throws Exception {
        String out = JSON.writeValueAsString(asList(TWO_AUDITS));

        assertThat(out).isEqualTo(TWO_AUDITS_JSON);
    }


    @Test
    public void shouldDeserializeTwoAuditsFromYaml() throws Exception {
        List<Audit> audits = YAML.readValue(TWO_AUDITS_YAML, AUDITS);

        assertThat(audits).containsExactly(TWO_AUDITS);
    }


    @Test
    public void shouldSerializeTwoAuditsToYaml() throws Exception {
        String out = YAML.writeValueAsString(asList(TWO_AUDITS));

        assertThat(out).isEqualTo(TWO_AUDITS_YAML);
    }
}
