package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.JsonProperty;
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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static com.github.t1.problem.WebException.*;
import static java.lang.Boolean.*;
import static javax.ws.rs.core.Response.Status.*;
import static lombok.AccessLevel.*;

@Slf4j
@Builder
@AllArgsConstructor(access = PRIVATE)
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class ConfigurationPlan {
    private static final GroupId LOGGERS = new GroupId("loggers");
    private static final GroupId LOG_HANDLERS = new GroupId("log-handlers");

    private static final ObjectMapper MAPPER = new ObjectMapper(
            new YAMLFactory()
                    .enable(MINIMIZE_QUOTES)
                    .disable(WRITE_DOC_START_MARKER))
            .setSerializationInclusion(NON_EMPTY)
            .findAndRegisterModules()
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
                        LoggerConfig loggerConfig = LoggerConfig.fromJson(artifactId, node);
                        logger(loggerConfig.category, loggerConfig);
                    } else if (LOG_HANDLERS.equals(groupId)) {
                        LogHandlerConfig logHandlerConfig = LogHandlerConfig.fromJson(artifactId, node);
                        logHandler(logHandlerConfig.name, logHandlerConfig);
                    } else {
                        DeploymentConfig deploymentConfig = DeploymentConfig.fromJson(groupId, artifactId, node);
                        deployment(deploymentConfig.deploymentName, deploymentConfig);
                    }
                });
            });
        }
    }

    @Singular @NonNull @JsonProperty private final Map<LoggerCategory, LoggerConfig> loggers;
    @Singular @NonNull @JsonProperty private final Map<LogHandlerName, LogHandlerConfig> logHandlers;
    @Singular @NonNull @JsonProperty private final Map<DeploymentName, DeploymentConfig> deployments;

    public Stream<LoggerConfig> loggers() { return loggers.values().stream(); }

    public Stream<LogHandlerConfig> logHandlers() { return logHandlers.values().stream(); }

    public Stream<DeploymentConfig> deployments() { return deployments.values().stream(); }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    public static class DeploymentConfig {
        @NonNull private GroupId groupId;
        @NonNull private ArtifactId artifactId;
        @NonNull private DeploymentState state;
        @NonNull private DeploymentName deploymentName;
        @NonNull private Version version;
        @NonNull private ArtifactType type;


        public static DeploymentConfig fromJson(GroupId groupId, ArtifactId artifactId, JsonNode node) {
            DeploymentConfigBuilder builder = builder()
                    .groupId(groupId)
                    .artifactId(artifactId);
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "name", artifactId.getValue(), value -> builder.deploymentName(new DeploymentName(value)));
            apply(node, "version", null, value -> builder.version((value == null) ? null : new Version(value)));
            apply(node, "type", war.name(), value -> builder.type(ArtifactType.valueOf(value)));
            return builder.build();
        }

        @Override public String toString() {
            return "«deployment:" + state
                    + (deploymentName == null ? "" : ":" + deploymentName)
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
        @NonNull private LoggerCategory category;
        private LogLevel level;
        @Singular
        @NonNull private List<LogHandlerName> handlers;
        private Boolean useParentHandlers;


        private static LoggerConfig fromJson(ArtifactId artifactId, JsonNode node) {
            LoggerConfigBuilder builder = builder();
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "name", artifactId.getValue(), value -> builder.category(LoggerCategory.of(value)));
            apply(node, "level", null, value -> builder.level((value == null) ? null : LogLevel.valueOf(value)));
            apply(node, "handler", null, value -> {
                if (value != null) {
                    builder.useParentHandlers(false);
                    builder.handler(new LogHandlerName(value));
                }
            });
            if (node.has("handlers")) {
                Iterator<JsonNode> handlers = node.get("handlers").elements();
                while (handlers.hasNext()) {
                    builder.useParentHandlers(false);
                    builder.handler(new LogHandlerName(handlers.next().textValue()));
                }
            }
            apply(node, "use-parent-handlers", null, value -> {
                if (value == null)
                    value = Boolean.toString(builder.build().handlers.isEmpty());
                builder.useParentHandlers(Boolean.valueOf(value));
            });
            LoggerConfig logger = builder.build();
            logger.validate();
            return logger;
        }

        private void validate() {
            if (useParentHandlers == FALSE && handlers.isEmpty())
                throw badRequest("Can't set use-parent-handlers to false when there are no handlers");
        }

        @Override public String toString() {
            return "«logger:" + state
                    + (category == null ? "" : ":" + category)
                    + (level == null ? "" : ":" + level)
                    + "»";
        }
    }

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
            apply(node, "level", ALL.name(), value -> builder.level(LogLevel.valueOf(value)));
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

    @SneakyThrows(IOException.class)
    public String toYaml() {
        return MAPPER.writeValueAsString(this);
    }
}
