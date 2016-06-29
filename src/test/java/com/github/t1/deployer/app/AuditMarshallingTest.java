package com.github.t1.deployer.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.ArtifactAudit.ArtifactAuditBuilder;
import com.github.t1.deployer.container.DeploymentName;
import org.junit.Test;

import java.io.*;
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
        return JSON.readValue(new StringReader(json.replace('\'', '\"')), Audit.class);
    }

    private static final TypeReference<List<Audit>> AUDITS = new TypeReference<List<Audit>>() {};


    private static final ArtifactAuditBuilder JOLOKIA =
            ArtifactAudit.of("org.jolokia", "jolokia-war", "1.3.2").name(new DeploymentName("jolokia"));

    private static final ArtifactAuditBuilder MOCKSERVER =
            ArtifactAudit.of("org.mock-server", "mockserver-war", "3.10.4").name(new DeploymentName("mockserver"));

    private static final LoggerAudit.LoggerAuditBuilder DEPLOYER_LOG =
            LoggerAudit.of("com.github.t1.deployer").level(DEBUG);


    private static final String TWO_AUDITS_JSON =
            (""
                     + "[{"
                     + "'type':'logger',"
                     + "'state':'deployed',"
                     + "'category':'foo',"
                     + "'level':'DEBUG'"
                     + "},{"
                     + "'type':'logger',"
                     + "'state':'undeployed',"
                     + "'category':'bar',"
                     + "'level':'INFO'"
                     + "}]"
            ).replace('\'', '\"');


    private static final String TWO_AUDITS_YAML = ""
            + "- type: logger\n"
            + "  state: deployed\n"
            + "  category: com.github.t1.deployer\n"
            + "  level: DEBUG\n"
            + "- type: artifact\n"
            + "  state: undeployed\n"
            + "  name: mockserver\n"
            + "  groupId: org.mock-server\n"
            + "  artifactId: mockserver-war\n"
            + "  version: 3.10.4\n";


    @Test
    public void shouldFailToDeserializeUnknownArtifactType() throws Exception {
        String json = "{'type':'xxx','state':'deployed'}";

        assertThatThrownBy(() -> deserialize(json))
                .hasMessageContaining("unsupported audit type: 'xxx'");
    }


    @Test
    public void shouldDeserializeDeployedArtifactAudit() throws Exception {
        String json = "{"
                + "'type':'artifact',"
                + "'state':'deployed',"
                + "'name':'jolokia',"
                + "'groupId':'org.jolokia',"
                + "'artifactId':'jolokia-war',"
                + "'version':'1.3.2'"
                + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(JOLOKIA.deployed());
    }


    @Test
    public void shouldDeserializeUndeployedArtifactAudit() throws Exception {
        String json = "{"
                + "'type':'artifact',"
                + "'state':'undeployed',"
                + "'name':'mockserver',"
                + "'groupId':'org.mock-server',"
                + "'artifactId':'mockserver-war',"
                + "'version':'3.10.4'"
                + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(MOCKSERVER.undeployed());
    }


    @Test
    public void shouldFailToDeserializeArtifactAuditWithUnknownState() throws Exception {
        String json = "{"
                + "'type':'artifact',"
                + "'state':'xxx',"
                + "'name':'mockserver',"
                + "'groupId':'org.mock-server',"
                + "'artifactId':'mockserver-war',"
                + "'version':'3.10.4'"
                + "}";

        assertThatThrownBy(() -> deserialize(json))
                .hasMessageContaining("unsupported audit state: 'xxx'");
    }


    @Test
    public void shouldDeserializeDeployedLoggerAudit() throws Exception {
        String json = "{"
                + "'type':'logger',"
                + "'state':'deployed',"
                + "'category':'com.github.t1.deployer',"
                + "'level':'DEBUG'"
                + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(DEPLOYER_LOG.deployed());
    }


    @Test
    public void shouldDeserializeTwoLoggerAudits() throws Exception {
        List<Audit> audits = JSON.readValue(new StringReader(TWO_AUDITS_JSON), AUDITS);

        assertThat(audits).containsExactly(
                LoggerAudit.of("foo").level(DEBUG).deployed(),
                LoggerAudit.of("bar").level(INFO).undeployed());
    }


    @Test
    public void shouldSerializeTwoLoggerAudits() throws Exception {
        List<Audit> audits = asList(
                LoggerAudit.of("foo").level(DEBUG).deployed(),
                LoggerAudit.of("bar").level(INFO).undeployed());
        StringWriter out = new StringWriter();

        JSON.writeValue(out, audits);

        assertThat(out.toString()).isEqualTo(TWO_AUDITS_JSON);
    }


    @Test
    public void shouldDeserializeTwoAuditsFromYaml() throws Exception {
        List<Audit> audits = YAML.readValue(TWO_AUDITS_YAML, AUDITS);

        assertThat(audits).containsExactly(DEPLOYER_LOG.deployed(), MOCKSERVER.undeployed());
    }


    @Test
    public void shouldSerializeTwoAuditsToYaml() throws Exception {
        List<Audit> audits = asList(DEPLOYER_LOG.deployed(), MOCKSERVER.undeployed());
        StringWriter out = new StringWriter();

        YAML.writeValue(out, audits);

        assertThat(out.toString()).isEqualTo(TWO_AUDITS_YAML);
    }
}
