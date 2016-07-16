package com.github.t1.deployer.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.container.LoggerCategory;
import org.junit.Test;

import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.github.t1.log.LogLevel.*;
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


    private static final ArtifactAuditBuilder JOLOKIA =
            ArtifactAudit.of("org.jolokia", "jolokia-war", "1.3.2").name("jolokia");

    private static final ArtifactAuditBuilder MOCKSERVER =
            ArtifactAudit.of("org.mock-server", "mockserver-war", "3.10.4").name("mockserver");

    private static LoggerAuditBuilder deployerLog() {
        return LoggerAudit.of(LoggerCategory.of("com.github.t1.deployer"));
    }


    private static final Audits TWO_AUDITS = new Audits()
            .audit(deployerLog().change("level", INFO, DEBUG).added())
            .audit(MOCKSERVER.removed());

    private static final String TWO_AUDITS_JSON =
            ("{'audits':"
                     + "[{"
                     + "'type':'logger',"
                     + "'operation':'add',"
                     + "'changes':["
                     + "{'name':'level','oldValue':'INFO','newValue':'DEBUG'}"
                     + "],"
                     + "'category':'com.github.t1.deployer'"
                     + "},{"
                     + "'type':'artifact',"
                     + "'operation':'remove',"
                     + "'name':'mockserver',"
                     + "'groupId':'org.mock-server',"
                     + "'artifactId':'mockserver-war',"
                     + "'version':'3.10.4'"
                     + "}]}"
            ).replace('\'', '\"');


    private static final String TWO_AUDITS_YAML = ""
            + "audits:\n"
            + "- !<logger>\n"
            + "  operation: add\n"
            + "  changes:\n"
            + "  - name: level\n"
            + "    oldValue: INFO\n"
            + "    newValue: DEBUG\n"
            + "  category: com.github.t1.deployer\n"
            + "- !<artifact>\n"
            + "  operation: remove\n"
            + "  name: mockserver\n"
            + "  groupId: org.mock-server\n"
            + "  artifactId: mockserver-war\n"
            + "  version: 3.10.4\n";


    @Test
    public void shouldFailToDeserializeUnknownArtifactType() throws Exception {
        String json = "{'type':'xxx','operation':'add'}";

        assertThatThrownBy(() -> deserialize(json))
                .hasMessageContaining("Could not resolve type id 'xxx' into a subtype");
    }


    @Test
    public void shouldDeserializeDeployedArtifactAudit() throws Exception {
        String json = "{"
                + "'type':'artifact',"
                + "'operation':'add',"
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
                + "'operation':'remove',"
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
                + "'operation':'xxx',"
                + "'name':'mockserver',"
                + "'groupId':'org.mock-server',"
                + "'artifactId':'mockserver-war',"
                + "'version':'3.10.4'"
                + "}";

        assertThatThrownBy(() -> deserialize(json))
                .hasMessageContaining("value not one of declared Enum instance names")
                .hasMessageContaining("xxx");
    }


    @Test
    public void shouldDeserializeDeployedLoggerAudit() throws Exception {
        String json = "{"
                + "'type':'logger',"
                + "'operation':'add',"
                + "'category':'com.github.t1.deployer',"
                + "'changes':[{"
                + "'name':'level',"
                + "'oldValue':null,"
                + "'newValue':'INFO'"
                + "}]}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(deployerLog().change("level", null, INFO).added());
    }


    @Test
    public void shouldDeserializeTwoAuditsFromJson() throws Exception {
        Audits audits = JSON.readValue(TWO_AUDITS_JSON, Audits.class);

        assertThat(audits).isEqualTo(TWO_AUDITS);
    }


    @Test
    public void shouldSerializeTwoAuditsAsJson() throws Exception {
        String out = JSON.writeValueAsString(TWO_AUDITS);

        assertThat(out).isEqualTo(TWO_AUDITS_JSON);
    }


    @Test
    public void shouldDeserializeTwoAuditsFromYaml() throws Exception {
        Audits audits = YAML.readValue(TWO_AUDITS_YAML, Audits.class);

        assertThat(audits).isEqualTo(TWO_AUDITS);
    }


    @Test
    public void shouldSerializeTwoAuditsToYaml() throws Exception {
        String out = YAML.writeValueAsString(TWO_AUDITS);

        assertThat(out).isEqualTo(TWO_AUDITS_YAML);
    }
}
