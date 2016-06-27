package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.model.*;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static java.util.Collections.*;
import static lombok.AccessLevel.*;

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

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") @NonNull
    private final Map<GroupId, Map<ArtifactId, Item>> groupMap;

    public Set<GroupId> getGroupIds() { return unmodifiableSet(groupMap.keySet()); }

    public Set<ArtifactId> getArtifactIds(GroupId groupId) { return unmodifiableSet(groupMap.get(groupId).keySet()); }

    public Item getItem(GroupId groupId, ArtifactId artifactId) { return groupMap.get(groupId).get(artifactId); }

    @Data
    public static class Item {
        // general
        @NonNull private DeploymentState state = deployed;

        // deployment
        @NotNull(groups = deployment.class)
        private Version version;

        private String name;

        @NotNull(groups = deployment.class)
        private ArtifactType type = war;


        // logger/handler
        private LogLevel level;
        @JsonProperty("handler-type") private LoggingHandlerType handlerType = periodicRotatingFile;
        private String file;
        private String suffix = "";
        private String formatter;

        @Override public String toString() {
            return "«" + state
                    + (version == null ? "" : ":" + version)
                    + (name == null ? "" : ":" + name)
                    + (type == war ? "" : ":" + type)
                    + (level == null ? "" : ":" + level)
                    + (handlerType == periodicRotatingFile ? "" : ":" + handlerType)
                    + (file == null ? "" : ":" + file)
                    + (suffix.isEmpty() ? "" : ":" + suffix)
                    + (formatter == null ? "" : ":" + formatter)
                    + "»";
        }
    }
}
