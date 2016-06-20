package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.repository.*;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.github.t1.deployer.app.ConfigurationPlan.State.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static java.nio.charset.StandardCharsets.*;
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

    public static ConfigurationPlan load(String plan) { return load(new StringReader(plan)); }

    @SneakyThrows(IOException.class)
    public static ConfigurationPlan load(Path path) {
        return load(Files.newBufferedReader(path, UTF_8));
    }

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
        // general
        private State state = deployed;

        // deployment
        private Version version;
        private String name;
        private ArtifactType type = war;

        // logger/handler
        private LogLevel level;
        @JsonProperty("handler-type") private LoggingHandlerType handlerType = periodicRotatingFile;
        private String file;
        private String suffix = "";
        private String formatter;
    }

    public enum State {
        deployed, undeployed
    }
}
