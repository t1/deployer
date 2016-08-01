package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.log.LogLevel;
import com.google.common.collect.ImmutableMap;
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
import static java.util.stream.Collectors.*;
import static lombok.AccessLevel.*;

/**
 * The plan of how the configuration should be. This class is responsible for loading the plan from YAML, statically
 * validating the plan, and applying default values. It also provides {@link #toYaml}.
 */
@Builder
@Getter
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
            // preferable to @JsonNaming, but won't be picked up by JAX-RS: .setPropertyNamingStrategy(KEBAB_CASE)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules();

    private static final ConfigurationPlan EMPTY_PLAN = ConfigurationPlan.builder().build();

    public static ConfigurationPlan load(Reader reader) {
        try {
            ConfigurationPlan plan = YAML.readValue(reader, ConfigurationPlan.class);
            if (plan == null)
                plan = EMPTY_PLAN;
            log.debug("config plan loaded:\n{}", plan);
            return plan;
        } catch (IOException e) {
            throw new RuntimeException("exception while loading config plan", e);
        }
    }

    @JsonCreator public static ConfigurationPlan fromJson(JsonNode json) {
        ConfigurationPlanBuilder builder = builder();
        readAll(json.get("log-handlers"), LogHandlerName::new, LogHandlerConfig::fromJson, builder::logHandler);
        readAll(json.get("loggers"), LoggerCategory::of, LoggerConfig::fromJson, builder::logger);
        readAll(json.get("artifacts"), DeploymentName::new, DeploymentConfig::fromJson, builder::artifact);
        return builder.build();
    }

    public static <K, V> void readAll(JsonNode jsonNode, Function<String, K> toKey, BiFunction<K, JsonNode, V> toConfig,
            Consumer<V> consumer) {
        if (jsonNode != null)
            jsonNode.fieldNames().forEachRemaining(name ->
                    consumer.accept(toConfig.apply(toKey.apply(name), jsonNode.get(name))));
    }

    @NonNull @JsonProperty private final Map<LogHandlerName, LogHandlerConfig> logHandlers;
    @NonNull @JsonProperty private final Map<LoggerCategory, LoggerConfig> loggers;
    @NonNull @JsonProperty private final Map<DeploymentName, DeploymentConfig> artifacts;

    public Stream<LogHandlerConfig> logHandlers() { return logHandlers.values().stream(); }

    public Stream<LoggerConfig> loggers() { return loggers.values().stream(); }

    public Stream<DeploymentConfig> artifacts() { return artifacts.values().stream(); }

    public static class ConfigurationPlanBuilder {
        private Map<LogHandlerName, LogHandlerConfig> logHandlers = new LinkedHashMap<>();
        private Map<LoggerCategory, LoggerConfig> loggers = new LinkedHashMap<>();
        private Map<DeploymentName, DeploymentConfig> artifacts = new LinkedHashMap<>();

        public ConfigurationPlanBuilder logHandler(LogHandlerConfig config) {
            this.logHandlers.put(config.getName(), config);
            return this;
        }

        public ConfigurationPlanBuilder logger(LoggerConfig config) {
            this.loggers.put(config.getCategory(), config);
            return this;
        }

        public ConfigurationPlanBuilder artifact(DeploymentConfig config) {
            this.artifacts.put(config.getName(), config);
            return this;
        }
    }

    public interface AbstractConfig {
        DeploymentState getState();
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class DeploymentConfig implements AbstractConfig {
        @NonNull @JsonIgnore private final DeploymentName name;
        @NonNull private final DeploymentState state;
        @NonNull private final GroupId groupId;
        @NonNull private final ArtifactId artifactId;
        @NonNull private final Version version;
        @NonNull private final ArtifactType type;
        @NonNull private final Map<String, String> variables;

        public static class DeploymentConfigBuilder {
            @SuppressWarnings("unused")
            private Map<String, String> variables = ImmutableMap.of();
        }

        public static DeploymentConfig fromJson(DeploymentName name, JsonNode node) {
            if (node.isNull())
                throw new RuntimeException("no config in artifact '" + name + "'");
            DeploymentConfigBuilder builder = builder().name(name);
            apply(node, "group-id", null, value -> builder.groupId(new GroupId(value)));
            apply(node, "artifact-id", name.getValue(), value -> builder.artifactId(new ArtifactId(value)));
            apply(node, "state", deployed.name(), value -> builder.state(DeploymentState.valueOf(value)));
            apply(node, "version", null, value -> builder.version((value == null) ? null : new Version(value)));
            apply(node, "type", war.name(), value -> builder.type(ArtifactType.valueOf(value)));
            if (node.has("var") && !node.get("var").isNull())
                builder.variables(toMap(node.get("var")));
            return builder.build();
        }

        private static Map<String, String> toMap(JsonNode node) {
            ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                builder.put(field.getKey(), field.getValue().asText());
            }
            return builder.build();
        }

        @Override public String toString() {
            return "«deployment:" + state + ":" + name + ":" + groupId + ":" + artifactId + ":" + version
                    + ":" + type + "»";
        }
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class LoggerConfig implements AbstractConfig {
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
                throw new RuntimeException("Can't set use-parent-handlers of [" + category + "] "
                        + "to false when there are no handlers");
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
    public static class LogHandlerConfig implements AbstractConfig {
        private static final LogLevel DEFAULT_LEVEL = ALL;

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
            apply(node, "level", DEFAULT_LEVEL.name(), value -> builder.level(LogLevel.valueOf(value)));
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

        public static class LogHandlerConfigBuilder {
            public LogHandlerConfigBuilder level(LogLevel level) {
                this.level = (level == null) ? DEFAULT_LEVEL : level;
                return this;
            }
        }

        private LogHandlerConfig validate() {
            if (format == null && formatter == null || format != null && formatter != null)
                throw new RuntimeException("log-handler [" + name + "] must either have a format or a formatter");
            return this;
        }

        @Override public String toString() {
            return "«log-handler:" + state + ":" + type + ":" + name + ":" + level
                    + ":" + file + ":" + suffix
                    + ((format == null) ? "" : ":format=" + format)
                    + ((formatter == null) ? "" : ":formatter=" + formatter)
                    + "»";
        }
    }

    private static void apply(JsonNode node, String fieldName, String defaultValue, Consumer<String> setter) {
        setter.accept((node.has(fieldName) && !node.get(fieldName).isNull())
                ? node.get(fieldName).asText() : defaultValue);
    }

    @Override public String toString() {
        return ""
                + "log-handlers:\n  - " + logHandlers().map(Object::toString).collect(joining("\n  - "))
                + "\nloggers:\n  - " + loggers().map(Object::toString).collect(joining("\n  - "))
                + "\nartifacts:\n  - " + artifacts().map(Object::toString).collect(joining("\n  - "));
    }

    @SneakyThrows(IOException.class)
    public String toYaml() { return YAML.writeValueAsString(this); }
}
