package com.github.t1.deployer.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.github.t1.deployer.app.Audit.DeployableAudit;
import com.github.t1.deployer.app.Audit.LoggerAudit;
import com.github.t1.deployer.model.LoggerCategory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.KEBAB_CASE;
import static com.github.t1.deployer.model.Plan.YAML;
import static com.github.t1.deployer.model.ProcessState.reloadRequired;
import static com.github.t1.log.LogLevel.DEBUG;
import static com.github.t1.log.LogLevel.INFO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditsSerializationTest {
    private static final ObjectMapper JSON = new ObjectMapper()
        .setSerializationInclusion(NON_EMPTY) //
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false) //
        .setPropertyNamingStrategy(KEBAB_CASE);

    private Audit deserialize(String json) throws IOException {
        return JSON.readValue(json.replace('\'', '\"'), Audit.class);
    }


    private static final DeployableAudit JOLOKIA = new DeployableAudit().setName("jolokia");

    private static final DeployableAudit MOCKSERVER = new DeployableAudit().setName("mockserver");

    private static LoggerAudit deployerLog() {
        return new LoggerAudit().setCategory(LoggerCategory.of("com.github.t1.deployer"));
    }


    private static final Audits TWO_AUDITS = new Audits()
        .add(deployerLog().change("level", INFO, DEBUG).added())
        .add(MOCKSERVER.removed())
        .setProcessState(reloadRequired);

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
            + "}],"
            + "'process-state':'reloadRequired'}"
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
        + "  name: mockserver\n"
        + "processState: reloadRequired\n";


    @Test void shouldFailToDeserializeUnknownDeployableType() {
        String json = "{'type':'xxx','operation':'add'}";

        assertThatThrownBy(() -> deserialize(json))
            .isInstanceOf(InvalidTypeIdException.class)
            .hasMessageContaining("Could not resolve type id 'xxx'");
    }


    @Test void shouldDeserializeDeployedDeployableAudit() throws Exception {
        String json = "{"
            + "'type':'deployable',"
            + "'operation':'add',"
            + "'name':'jolokia'"
            + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(JOLOKIA.added());
    }


    @Test void shouldDeserializeChangeAudit() throws Exception {
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


    @Test void shouldDeserializeUndeployedDeployableAudit() throws Exception {
        String json = "{"
            + "'type':'deployable',"
            + "'operation':'remove',"
            + "'name':'mockserver'"
            + "}";

        Audit audit = deserialize(json);

        assertThat(audit).isEqualTo(MOCKSERVER.removed());
    }


    @Test void shouldFailToDeserializeDeployableAuditWithUnknownState() {
        String json = "{"
            + "'type':'deployable',"
            + "'operation':'xxx',"
            + "'name':'mockserver',"
            + "'group-id':'org.mock-server',"
            + "'artifact-id':'mockserver-war',"
            + "'version':'3.10.4'"
            + "}";

        assertThatThrownBy(() -> deserialize(json))
            .hasMessageContaining("from String \"xxx\": not one of the values accepted for Enum class: [change, add, remove]");
    }


    @Test void shouldDeserializeDeployedLoggerAudit() throws Exception {
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


    @Test void shouldDeserializeTwoAuditsFromJson() throws Exception {
        Audits audits = JSON.readValue(TWO_AUDITS_JSON, Audits.class);

        assertThat(audits).isEqualTo(TWO_AUDITS);
    }


    @Test void shouldSerializeTwoAuditsAsJson() throws Exception {
        String out = JSON.writeValueAsString(TWO_AUDITS);

        assertThat(out).isEqualTo(TWO_AUDITS_JSON);
    }


    @Test void shouldDeserializeTwoAuditsFromYaml() throws Exception {
        Audits audits = YAML.readValue(TWO_AUDITS_YAML, Audits.class);

        assertThat(audits).isEqualTo(TWO_AUDITS);
    }


    @Test void shouldSerializeTwoAuditsToYaml() throws Exception {
        String out = YAML.writeValueAsString(TWO_AUDITS);

        assertThat(out).isEqualTo(TWO_AUDITS_YAML);
    }
}
