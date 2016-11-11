package com.github.t1.deployer.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.t1.deployer.app.Audit.*;
import com.github.t1.deployer.app.Audit.DeployableAudit.DeployableAuditBuilder;
import com.github.t1.deployer.app.Audit.LoggerAudit.LoggerAuditBuilder;
import com.github.t1.deployer.model.LoggerCategory;
import org.junit.Test;

import java.io.IOException;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.*;
import static com.github.t1.deployer.model.Plan.*;
import static com.github.t1.log.LogLevel.*;
import static org.assertj.core.api.Assertions.*;

public class AuditsSerializationTest {
    private static final ObjectMapper JSON = new ObjectMapper().setPropertyNamingStrategy(KEBAB_CASE);

    public Audit deserialize(String json) throws IOException {
        return JSON.readValue(json.replace('\'', '\"'), Audit.class);
    }


    private static final DeployableAuditBuilder JOLOKIA = DeployableAudit.builder().name("jolokia");

    private static final DeployableAuditBuilder MOCKSERVER = DeployableAudit.builder().name("mockserver");

    private static LoggerAuditBuilder deployerLog() {
        return LoggerAudit.of(LoggerCategory.of("com.github.t1.deployer"));
    }


    private static final Audits TWO_AUDITS = new Audits()
            .add(deployerLog().change("level", INFO, DEBUG).added())
            .add(MOCKSERVER.removed());

    private static final String TWO_AUDITS_JSON =
            ("{'audits':"
                     + "[{"
                     + "'type':'logger',"
                     + "'operation':'add',"
                     + "'changes':["
                     + "{'name':'level','old-value':'INFO','new-value':'DEBUG'}"
                     + "],"
                     + "'category':'com.github.t1.deployer'"
                     + "},{"
                     + "'type':'deployable',"
                     + "'operation':'remove',"
                     + "'name':'mockserver'"
                     + "}]}"
            ).replace('\'', '\"');


    private static final String TWO_AUDITS_YAML = ""
            + "audits:\n"
            + "- !<logger>\n"
            + "  operation: add\n"
            + "  changes:\n"
            + "  - name: level\n"
            + "    old-value: INFO\n"
            + "    new-value: DEBUG\n"
            + "  category: com.github.t1.deployer\n"
            + "- !<deployable>\n"
            + "  operation: remove\n"
            + "  name: mockserver\n";


    @Test
    public void shouldFailToDeserializeUnknownDeployableType() throws Exception {
        String json = "{'type':'xxx','operation':'add'}";

        assertThatThrownBy(() -> deserialize(json))
                .hasMessageContaining("Could not resolve type id 'xxx' into a subtype");
    }


    @Test
    public void shouldDeserializeDeployedDeployableAudit() throws Exception {
        String json = "{"
                + "'type':'deployable',"
                + "'operation':'add',"
                + "'name':'jolokia'"
                + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(JOLOKIA.added());
    }


    @Test
    public void shouldDeserializeChangeAudit() throws Exception {
        String json = "{'audits':["
                + "{"
                + "'type':'deployable',"
                + "'operation':'add',"
                + "'changes':["
                + "{'name':'group-id','old-value':null,'new-value':'org.jolokia'},"
                + "{'name':'artifact-id','old-value':null,'new-value':'jolokia-war'},"
                + "{'name':'version','old-value':null,'new-value':'1.3.2'},"
                + "{'name':'type','old-value':null,'new-value':'war'}"
                + "],'name':'jolokia'}"
                + "]}";

        Audits audits = JSON.readValue(json.replace('\'', '\"'), Audits.class);

        assertThat(audits.toYaml()).isEqualTo("audits:\n"
                + "- !<deployable>\n"
                + "  operation: add\n"
                + "  changes:\n"
                + "  - name: group-id\n"
                + "    new-value: org.jolokia\n"
                + "  - name: artifact-id\n"
                + "    new-value: jolokia-war\n"
                + "  - name: version\n"
                + "    new-value: 1.3.2\n"
                + "  - name: type\n"
                + "    new-value: war\n"
                + "  name: jolokia\n");
    }


    @Test
    public void shouldDeserializeUndeployedDeployableAudit() throws Exception {
        String json = "{"
                + "'type':'deployable',"
                + "'operation':'remove',"
                + "'name':'mockserver'"
                + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(MOCKSERVER.removed());
    }


    @Test
    public void shouldFailToDeserializeDeployableAuditWithUnknownState() throws Exception {
        String json = "{"
                + "'type':'deployable',"
                + "'operation':'xxx',"
                + "'name':'mockserver',"
                + "'group-id':'org.mock-server',"
                + "'artifact-id':'mockserver-war',"
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
                + "'old-value':null,"
                + "'new-value':'INFO'"
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
