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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static java.util.Collections.*;
import static javax.ws.rs.core.Response.Status.*;
import static lombok.AccessLevel.*;

@Slf4j
@AllArgsConstructor(access = PRIVATE)
public class ConfigurationPlan {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()
            .enable(MINIMIZE_QUOTES).disable(WRITE_DOC_START_MARKER))
            .setSerializationInclusion(NON_EMPTY)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final TypeReference<Map<GroupId, Map<ArtifactId, Item>>>
            ARTIFACT_MAP_TYPE = new TypeReference<Map<GroupId, Map<ArtifactId, Item>>>() {};

    public static ConfigurationPlan load(Reader reader) {
        Map<GroupId, Map<ArtifactId, Item>> map;
        try {
            map = MAPPER.readValue(reader, ARTIFACT_MAP_TYPE);
        } catch (IOException e) {
            log.debug("exception while loading config plan", e);
            throw new WebApplicationException(Response.status(BAD_REQUEST).entity(e.getMessage()).build());
        }
        if (map == null)
            map = emptyMap();
        ConfigurationPlan plan = new ConfigurationPlan(map);
        log.debug("config plan loaded:\n{}", plan);
        return plan;
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") @NonNull
    private final Map<GroupId, Map<ArtifactId, Item>> groupMap;

    public Set<GroupId> getGroupIds() { return unmodifiableSet(groupMap.keySet()); }

    public Set<ArtifactId> getArtifactIds(GroupId groupId) { return unmodifiableSet(groupMap.get(groupId).keySet()); }

    public Item getItem(GroupId groupId, ArtifactId artifactId) { return groupMap.get(groupId).get(artifactId); }

    // TODO split into DeploymentItem, LoggerItem, and LogHandlerItem sharing state and name from AbstractItem
    // TODO more smart defaults for LogHandlers
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
        @NotNull(groups = { logger.class, loghandler.class })
        private LogLevel level;
        @NotNull(groups = { loghandler.class })
        @JsonProperty("handler-type") private LoggingHandlerType handlerType = periodicRotatingFile;
        @NotNull(groups = { loghandler.class })
        private String file;
        @NotNull(groups = { loghandler.class })
        private String suffix = "";
        @NotNull(groups = { loghandler.class })
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

    @Override public String toString() {
        StringBuilder out = new StringBuilder();
        for (GroupId groupId : getGroupIds()) {
            out.append(groupId).append(":\n");
            for (ArtifactId artifactId : getArtifactIds(groupId)) {
                out.append("  ").append(artifactId).append(": ").append(getItem(groupId, artifactId)).append("\n");
            }
        }
        return out.toString();
    }
}
