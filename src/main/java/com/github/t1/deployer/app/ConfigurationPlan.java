package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.log.LogLevel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

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
import static lombok.AccessLevel.*;

@Value
@Slf4j
@Builder
@AllArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
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
            throw badRequest(e.getMessage());
        }
    }

    public static ConfigurationPlan from(JsonNode json) { return builder().load(json).build(); }

    public static class ConfigurationPlanBuilder {
        public ConfigurationPlanBuilder load(JsonNode json) {
            JsonNode artifacts = json.get("artifacts");
            if (artifacts != null)
                fieldNames(artifacts).map(DeploymentName::new).forEach(deploymentName -> artifact(deploymentName,
                        DeploymentConfig.from(deploymentName, artifacts.get(deploymentName.getValue()))));

            JsonNode loggers = json.get("loggers");
            if (loggers != null)
                fieldNames(loggers).map(LoggerCategory::of).forEach(loggerCategory -> logger(loggerCategory,
                        LoggerConfig.from(loggerCategory, loggers.get(loggerCategory.getValue()))));

            JsonNode logHandlers = json.get("log-handlers");
            if (logHandlers != null)
                fieldNames(logHandlers).map(LogHandlerName::new).forEach(logHandlerName -> logHandler(logHandlerName,
                        LogHandlerConfig.fromJson(logHandlerName, logHandlers.get(logHandlerName.getValue()))));

            return this;
        }
    }

    @Singular @NonNull @JsonProperty private final Map<LoggerCategory, LoggerConfig> loggers;
    @Singular @NonNull @JsonProperty private final Map<LogHandlerName, LogHandlerConfig> logHandlers;
    @Singular @NonNull @JsonProperty private final Map<DeploymentName, DeploymentConfig> artifacts;

    public Stream<LoggerConfig> loggers() { return loggers.values().stream(); }

    public Stream<LogHandlerConfig> logHandlers() { return logHandlers.values().stream(); }

    public Stream<DeploymentConfig> artifacts() { return artifacts.values().stream(); }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class DeploymentConfig {
        @NonNull @JsonIgnore private DeploymentName name;
        @NonNull private DeploymentState state;
        @NonNull private GroupId groupId;
        @NonNull private ArtifactId artifactId;
        @NonNull private Version version;
        @NonNull private ArtifactType type;


        public static DeploymentConfig from(DeploymentName name, JsonNode node) {
            if (node.isNull())
                throw badRequest("no config in artifact '" + name + "'");
            DeploymentConfigBuilder builder = builder().name(name);
            apply(node, "group-id", null, value -> builder.groupId(new GroupId(value)));
            apply(node, "artifact-id", name.getValue(), value -> builder.artifactId(new ArtifactId(value)));
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "version", null, value -> builder.version((value == null) ? null : new Version(value)));
            apply(node, "type", war.name(), value -> builder.type(ArtifactType.valueOf(value)));
            return builder.build();
        }

        @Override public String toString() {
            return "«deployment:" + state + ":" + groupId + ":" + artifactId + ":" + version + ":" + type + "»";
        }
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class LoggerConfig {
        @NonNull @JsonIgnore private LoggerCategory category;
        @NonNull private DeploymentState state;
        private LogLevel level;
        @Singular
        @NonNull private List<LogHandlerName> handlers;
        private Boolean useParentHandlers;


        private static LoggerConfig from(LoggerCategory category, JsonNode node) {
            if (node.isNull())
                throw badRequest("no config in logger '" + category + "'");
            LoggerConfigBuilder builder = builder().category(category);
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
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
    @JsonNaming(KebabCaseStrategy.class)
    public static class LogHandlerConfig {
        @NonNull @JsonIgnore private LogHandlerName name;
        @NonNull private DeploymentState state;
        @NonNull private LogLevel level;
        @NonNull private LoggingHandlerType type;
        @NonNull private String file;
        @NonNull private String suffix;
        @NonNull private String format;
        // TODO formatter / named-formatter


        private static LogHandlerConfig fromJson(LogHandlerName name, JsonNode node) {
            if (node.isNull())
                throw badRequest("no config in log-handler '" + name + "'");
            LogHandlerConfigBuilder builder = builder().name(name);
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "level", ALL.name(), value -> builder.level(LogLevel.valueOf(value)));
            apply(node, "type", periodicRotatingFile.name(), value -> builder.type(LoggingHandlerType.valueOf(value)));
            apply(node, "file", name.getValue(), builder::file);
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
        artifacts().forEach(deployment -> out.append(deployment).append("\n"));
        return out.toString();
    }

    @SneakyThrows(IOException.class)
    public String toYaml() {
        return MAPPER.writeValueAsString(this);
    }
}
