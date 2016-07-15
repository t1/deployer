package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.*;
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
import java.util.function.*;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.LoggingHandlerType.*;
import static com.github.t1.log.LogLevel.*;
import static java.lang.Boolean.*;
import static lombok.AccessLevel.*;

/**
 * The plan of how the configuration should be. This class is responsible for loading the plan from YAML, statically
 * validating the plan, and applying default values.
 */
@Builder
@EqualsAndHashCode
@AllArgsConstructor(access = PRIVATE)
@Slf4j
@JsonNaming(KebabCaseStrategy.class)
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class ConfigurationPlan {
    static final ObjectMapper YAML = new ObjectMapper(
            new YAMLFactory()
                    .enable(MINIMIZE_QUOTES)
                    .disable(WRITE_DOC_START_MARKER))
            .setSerializationInclusion(NON_EMPTY)
            // .setPropertyNamingStrategy(KEBAB_CASE) // TODO Jackson 2.7.x; remove annotation
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules();

    public static ConfigurationPlan load(Reader reader) {
        try {
            ConfigurationPlan plan = YAML.readValue(reader, ConfigurationPlan.class);
            if (plan == null)
                plan = ConfigurationPlan.builder().build();
            log.debug("config plan loaded:\n{}", plan);
            return plan;
        } catch (IOException e) {
            throw new RuntimeException("exception while loading config plan", e);
        }
    }

    @JsonCreator public static ConfigurationPlan fromJson(JsonNode json) {
        ConfigurationPlanBuilder builder = builder();
        readAll(json.get("artifacts"), DeploymentName::new, DeploymentConfig::fromJson, builder::artifact);
        readAll(json.get("loggers"), LoggerCategory::of, LoggerConfig::fromJson, builder::logger);
        readAll(json.get("log-handlers"), LogHandlerName::new, LogHandlerConfig::fromJson, builder::logHandler);
        return builder.build();
    }

    public static <K, V> void readAll(JsonNode jsonNode,
            Function<String, K> toKey,
            BiFunction<K, JsonNode, V> toConfig,
            BiConsumer<K, V> consumer) {
        if (jsonNode != null)
            jsonNode.fieldNames().forEachRemaining(name -> {
                K key = toKey.apply(name);
                consumer.accept(key, toConfig.apply(key, jsonNode.get(name)));
            });
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
        @NonNull @JsonIgnore private final DeploymentName name;
        @NonNull private final DeploymentState state;
        @NonNull private final GroupId groupId;
        @NonNull private final ArtifactId artifactId;
        @NonNull private final Version version;
        @NonNull private final ArtifactType type;


        public static DeploymentConfig fromJson(DeploymentName name, JsonNode node) {
            if (node.isNull())
                throw new RuntimeException("no config in artifact '" + name + "'");
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
        @NonNull @JsonIgnore private final LoggerCategory category;
        @NonNull private final DeploymentState state;
        private final LogLevel level;
        @NonNull private final List<LogHandlerName> handlers;
        private final Boolean useParentHandlers;


        private static LoggerConfig fromJson(LoggerCategory category, JsonNode node) {
            if (node.isNull())
                throw new RuntimeException("no config in logger '" + category + "'");
            LoggerConfigBuilder builder = builder().category(category);
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "level", null, value -> builder.level((value == null) ? null : LogLevel.valueOf(value)));
            apply(node, "handler", null, builder::handler);
            if (node.has("handlers")) {
                Iterator<JsonNode> handlers = node.get("handlers").elements();
                while (handlers.hasNext())
                    builder.handler(handlers.next().textValue());
            }
            if (!builder.category.isRoot())
                apply(node, "use-parent-handlers", null, value -> {
                    if (value == null)
                        value = Boolean.toString(builder.build().handlers.isEmpty());
                    builder.useParentHandlers(Boolean.valueOf(value));
                });
            return builder.build().validate();
        }

        public static class LoggerConfigBuilder {
            private List<LogHandlerName> handlers = new ArrayList<>();

            public LoggerConfigBuilder handler(String name) {
                if (name != null)
                    this.handlers.add(new LogHandlerName(name));
                return this;
            }
        }

        private LoggerConfig validate() {
            if (useParentHandlers == FALSE && handlers.isEmpty())
                throw new RuntimeException("Can't set use-parent-handlers to false when there are no handlers");
            return this;
        }

        @Override public String toString() {
            return "«logger:" + state + ":" + category + ":" + level + ":"
                    + handlers + (useParentHandlers == TRUE ? "+" : "") + "»";
        }
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class LogHandlerConfig {
        @NonNull @JsonIgnore private final LogHandlerName name;
        @NonNull private final DeploymentState state;
        @NonNull private final LogLevel level;
        @NonNull private final LoggingHandlerType type;
        private final String format;
        private final String formatter;

        private final String file;
        private final String suffix;


        private static LogHandlerConfig fromJson(LogHandlerName name, JsonNode node) {
            if (node.isNull())
                throw new RuntimeException("no config in log-handler '" + name + "'");
            LogHandlerConfigBuilder builder = builder().name(name);
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "level", ALL.name(), value -> builder.level(LogLevel.valueOf(value)));
            apply(node, "type", periodicRotatingFile.name(), value -> builder.type(LoggingHandlerType.valueOf(value)));
            apply(node, "format", null, builder::format);
            apply(node, "formatter", null, builder::formatter);
            applyByType(node, builder);
            return builder.build().validate();
        }

        private static void applyByType(JsonNode node, LogHandlerConfigBuilder builder) {
            switch (builder.type) {
            case periodicRotatingFile:
                apply(node, "file", builder.name.getValue(), builder::file);
                apply(node, "suffix", ".yyyy-MM-dd", builder::suffix);
                return;
            case console:
                // nothing more to load here
                return;
            }
            throw new UnsupportedOperationException("unhandled log-handler type [" + builder.type + "]"
                    + " in [" + builder.name + "]");
        }

        public static class LogHandlerConfigBuilder {}

        private LogHandlerConfig validate() {
            if (format == null && formatter == null ||
                    format != null && formatter != null)
                throw new RuntimeException("log-handler [" + name + "] must either have a format or a formatter");
            return this;
        }

        @Override public String toString() {
            return "«log-handler:" + state + ":" + name + ":" + level + ":" + type + ":" + file + ":" + suffix
                    + ":" + format + "»";
        }
    }

    private static void apply(JsonNode node, String fieldName, String defaultValue, Consumer<String> setter) {
        setter.accept((node.has(fieldName)) ? node.get(fieldName).asText() : defaultValue);
    }

    @Override public String toString() {
        return toYaml();
    }

    @SneakyThrows(IOException.class)
    public String toYaml() { return YAML.writeValueAsString(this); }
}
