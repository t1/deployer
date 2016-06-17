package com.github.t1.deployer.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.model.Version;
import com.github.t1.deployer.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.github.t1.deployer.app.ConfigurationPlan.State.*;
import static java.util.Collections.*;
import static lombok.AccessLevel.*;

@Data
@AllArgsConstructor(access = PRIVATE)
@Slf4j
public class ConfigurationPlan {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()
            .enable(MINIMIZE_QUOTES).disable(WRITE_DOC_START_MARKER))
            .setSerializationInclusion(NON_EMPTY)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final TypeReference<Map<GroupId, Map<ArtifactId, Item>>>
            ARTIFACT_MAP_TYPE = new TypeReference<Map<GroupId, Map<ArtifactId, Item>>>() {};

    @SneakyThrows(IOException.class)
    public static ConfigurationPlan load(Reader reader) {
        Map<GroupId, Map<ArtifactId, Item>> map = MAPPER.readValue(reader, ARTIFACT_MAP_TYPE);
        if (map == null)
            map = emptyMap();
        log.debug("config plan loaded:\n{}", map);
        return new ConfigurationPlan(map);
    }

    @NonNull private final Map<GroupId, Map<ArtifactId, Item>> groupMap;

    @Data
    public static class Item {
        Version version;
        State state = deployed;
    }

    public enum State {
        deployed, undeployed
    }
}
