package com.github.t1.deployer.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.*;

import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static javax.ws.rs.core.Response.Status.*;
import static lombok.AccessLevel.*;

@Slf4j
@Builder
@AllArgsConstructor(access = PRIVATE)
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class ConfigurationPlan {
    private static final GroupId LOGGERS = new GroupId("loggers");
    private static final GroupId LOG_HANDLERS = new GroupId("log-handlers");

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final TypeReference<Map<GroupId, Map<ArtifactId, JsonNode>>>
            ARTIFACT_MAP_TYPE = new TypeReference<Map<GroupId, Map<ArtifactId, JsonNode>>>() {};

    public static ConfigurationPlan load(Reader reader) {
        ConfigurationPlan plan = ConfigurationPlan.from(loadJson(reader));
        log.debug("config plan loaded:\n{}", plan);
        return plan;
    }

    public static JsonNode loadJson(Reader reader) {
        try {
            return MAPPER.readValue(reader, JsonNode.class);
        } catch (IOException e) {
            log.debug("exception while loading config plan", e);
            throw new WebApplicationException(Response.status(BAD_REQUEST).entity(e.getMessage()).build());
        }
    }

    public static ConfigurationPlan from(JsonNode json) {
        ConfigurationPlanBuilder builder = builder();
        builder.load(json);
        return builder.build();
    }

    public static class ConfigurationPlanBuilder {
        public void load(JsonNode json) {
            fieldNames(json).map(GroupId::new).forEach(groupId -> {
                JsonNode artifactNode = json.get(groupId.getValue());
                fieldNames(artifactNode).map(ArtifactId::new).forEach(artifactId -> {
                    JsonNode node = artifactNode.get(artifactId.getValue());
                    if (node.isNull())
                        throw new NullPointerException("no config in " + groupId + ":" + artifactId);
                    if (LOGGERS.equals(groupId)) {
                        logger(LoggerConfig.fromJson(artifactId, node));
                    } else if (LOG_HANDLERS.equals(groupId)) {
                        logHandler(LogHandlerConfig.fromJson(artifactId, node));
                    } else {
                        deployment(DeploymentConfig.fromJson(groupId, artifactId, node));
                    }
                });
            });
        }
    }

    @Singular @NonNull private final List<LoggerConfig> loggers;
    @Singular @NonNull private final List<LogHandlerConfig> logHandlers;
    @Singular @NonNull private final List<DeploymentConfig> deployments;

    public Stream<LoggerConfig> loggers() { return loggers.stream(); }

    public Stream<LogHandlerConfig> logHandlers() { return logHandlers.stream(); }

    public Stream<DeploymentConfig> deployments() { return deployments.stream(); }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    public static class DeploymentConfig {
        @NonNull private GroupId groupId;
        @NonNull private ArtifactId artifactId;
        @NonNull private DeploymentState state;
        @NonNull private String name;
        @NonNull private Version version;
        @NonNull private ArtifactType type;


        public DeploymentName getDeploymentName() { return new DeploymentName(name); }


        public static DeploymentConfig fromJson(GroupId groupId, ArtifactId artifactId, JsonNode node) {
            DeploymentConfigBuilder builder = builder()
                    .groupId(groupId)
                    .artifactId(artifactId);
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "name", artifactId.getValue(), builder::name);
            apply(node, "version", null, value -> builder.version((value == null) ? null : new Version(value)));
            apply(node, "type", war.name(), value -> builder.type(ArtifactType.valueOf(value)));
            return builder.build();
        }

        @Override public String toString() {
            return "«deployment:" + state
                    + (name == null ? "" : ":" + name)
                    + (version == null ? "" : ":" + version)
                    + (type == war ? "" : ":" + type)
                    + "»";
        }
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    public static class LoggerConfig {
        @NonNull private DeploymentState state;
        @NonNull private String category;
        private LogLevel level;
        @Singular
        @NonNull private List<LogHandlerName> handlers = new ArrayList<>();
        // TODO use-parent-handlers


        private static LoggerConfig fromJson(ArtifactId artifactId, JsonNode node) {
            LoggerConfigBuilder builder = builder();
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "name", artifactId.getValue(), builder::category);
            apply(node, "level", null, value -> builder.level((value == null) ? null : LogLevel.valueOf(value)));
            apply(node, "handler", null, value -> {
                if (value != null)
                    builder.handler(new LogHandlerName(value));
            });
            if (node.has("handlers")) {
                Iterator<JsonNode> handlers = node.get("handlers").elements();
                while (handlers.hasNext())
                    builder.handler(new LogHandlerName(handlers.next().textValue()));
            }
            return builder.build();
        }

        @Override public String toString() {
            return "«logger:" + state
                    + (category == null ? "" : ":" + category)
                    + (level == null ? "" : ":" + level)
                    + "»";
        }
    }

    // TODO more smart defaults for LogHandlers
    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    public static class LogHandlerConfig {
        @NonNull private DeploymentState state;
        @NonNull private LogHandlerName name;
        @NonNull private LogLevel level;
        @NonNull private LoggingHandlerType type;
        @NonNull private String file;
        @NonNull private String suffix;
        @NonNull private String format;
        // TODO formatter / named-formatter


        private static LogHandlerConfig fromJson(ArtifactId artifactId, JsonNode node) {
            LogHandlerConfigBuilder builder = builder();
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "name", artifactId.getValue(), value -> builder.name(new LogHandlerName(value)));
            apply(node, "level", null, value -> builder.level((value == null) ? null : LogLevel.valueOf(value)));
            apply(node, "type", periodicRotatingFile.name(), value -> builder.type(LoggingHandlerType.valueOf(value)));
            apply(node, "file", artifactId.getValue(), builder::file);
            apply(node, "suffix", ".yyyy-MM-dd", builder::suffix);
            apply(node, "format", null, builder::format);
            return builder.build();
        }

        @Override public String toString() {
            return "«log-handler:" + state
                    + (name == null ? "" : ":" + name)
                    + (level == null ? "" : ":" + level)
                    + (type == periodicRotatingFile ? "" : ":" + type)
                    + (file == null ? "" : ":" + file)
                    + (suffix.isEmpty() ? "" : ":" + suffix)
                    + (format == null ? "" : ":" + format)
                    + "»";
        }
    }

    private static Stream<String> fieldNames(JsonNode json) {
        return StreamSupport.stream(((Iterable<String>) json::fieldNames).spliterator(), false);
    }

    private static void apply(JsonNode node, String fieldName, String defaultValue, Consumer<String> setter) {
        setter.accept((node.has(fieldName)) ? node.get(fieldName).asText() : defaultValue);
    }

    @Override public String toString() {
        StringBuilder out = new StringBuilder();
        loggers().forEach(logger -> out.append(logger).append("\n"));
        logHandlers().forEach(handler -> out.append(handler).append("\n"));
        deployments().forEach(deployment -> out.append(deployment).append("\n"));
        return out.toString();
    }
}
