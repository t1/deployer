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
import static com.github.t1.deployer.container.LoggingHandlerType.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.tools.Tools.toMap;
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
    public static final ObjectMapper YAML = new ObjectMapper(
            new YAMLFactory()
                    .enable(MINIMIZE_QUOTES)
                    .disable(WRITE_DOC_START_MARKER))
            .setSerializationInclusion(NON_EMPTY)
            // preferable to @JsonNaming, but conflicts in container: .setPropertyNamingStrategy(KEBAB_CASE)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules();

    public static class ConfigurationPlanLoadingException extends RuntimeException {
        public ConfigurationPlanLoadingException(String message) { super(message); }

        public ConfigurationPlanLoadingException(String message, Throwable cause) { super(message, cause); }
    }

    private static final ConfigurationPlan EMPTY_PLAN = ConfigurationPlan.builder().build();

    public static ConfigurationPlan load(Reader reader) {
        try {
            ConfigurationPlan plan = YAML.readValue(reader, ConfigurationPlan.class);
            if (plan == null)
                plan = EMPTY_PLAN;
            log.debug("config plan loaded:\n{}", plan);
            return plan;
        } catch (IOException e) {
            throw new ConfigurationPlanLoadingException("exception while loading config plan", e);
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
        private static final String VARS = "vars";

        @NonNull @JsonIgnore private final DeploymentName name;
        private final DeploymentState state;
        @NonNull private final GroupId groupId;
        @NonNull private final ArtifactId artifactId;
        @NonNull private final Version version;
        @NonNull private final ArtifactType type;
        @NonNull @Singular @JsonProperty(VARS) private final Map<String, String> variables;

        public static class DeploymentConfigBuilder {}

        public static DeploymentConfig fromJson(DeploymentName name, JsonNode node) {
            if (node.isNull())
                throw new ConfigurationPlanLoadingException("no config in artifact '" + name + "'");
            DeploymentConfigBuilder builder = builder().name(name);
            apply(node, "group-id", defaultValue("group-id"), value -> builder.groupId(new GroupId(value)));
            apply(node, "artifact-id", name.getValue(), value -> builder.artifactId(new ArtifactId(value)));
            apply(node, "state", null, value -> builder.state((value == null) ? null : DeploymentState.valueOf(value)));
            apply(node, "version", null, value -> builder.version((value == null) ? null : new Version(value)));
            apply(node, "type", war.name(), value -> builder.type(ArtifactType.valueOf(value)));
            if (node.has(VARS) && !node.get(VARS).isNull())
                if (builder.type == bundle)
                    builder.variables(toMap(node.get(VARS)));
                else if (node.get(VARS).size() > 0)
                    throw new ConfigurationPlanLoadingException(
                            "variables are only allowed for bundles; maybe you forgot to add `type: bundle`?");
            return builder.build();
        }

        private static String defaultValue(String name) {
            return System.getProperty("default." + name);
        }

        @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

        @Override public String toString() {
            return "«deployment:" + getState() + ":" + name + ":" + groupId + ":" + artifactId + ":" + version
                    + ":" + type + variables + "»";
        }
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class LoggerConfig implements AbstractConfig {
        @NonNull @JsonIgnore private final LoggerCategory category;
        private final DeploymentState state;
        private final LogLevel level;
        @NonNull private final List<LogHandlerName> handlers;
        private final Boolean useParentHandlers;


        private static LoggerConfig fromJson(LoggerCategory category, JsonNode node) {
            if (node.isNull())
                throw new ConfigurationPlanLoadingException("no config in logger '" + category + "'");
            LoggerConfigBuilder builder = builder().category(category);
            apply(node, "state", null, value -> builder.state((value == null) ? null : DeploymentState.valueOf(value)));
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
                throw new ConfigurationPlanLoadingException("Can't set use-parent-handlers of [" + category + "] "
                        + "to false when there are no handlers");
            return this;
        }

        @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

        public LogLevel getLevel() { return (level == null) ? ALL : level; }

        @Override public String toString() {
            return "«logger:" + getState() + ":" + category + ":" + getLevel() + ":"
                    + handlers + (useParentHandlers == TRUE ? "+" : "") + "»";
        }
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class LogHandlerConfig implements AbstractConfig {
        @NonNull @JsonIgnore private final LogHandlerName name;
        private final DeploymentState state;
        private final LogLevel level;
        @NonNull private final LoggingHandlerType type;
        private final String format;
        private final String formatter;

        private final String file;
        private final String suffix;

        private final String module;
        @JsonProperty("class") private final String class_;
        @Singular private final Map<String, String> properties;


        private static LogHandlerConfig fromJson(LogHandlerName name, JsonNode node) {
            if (node.isNull())
                throw new ConfigurationPlanLoadingException("no config in log-handler '" + name + "'");
            LogHandlerConfigBuilder builder = builder().name(name);
            apply(node, "state", null, value -> builder.state((value == null) ? null : DeploymentState.valueOf(value)));
            apply(node, "level", null, value -> builder.level((value == null) ? null : LogLevel.valueOf(value)));
            apply(node, "type", periodicRotatingFile.getTypeName(), value ->
                    builder.type(LoggingHandlerType.valueOfTypeName(value)));
            apply(node, "format", null, builder::format);
            apply(node, "formatter", null, builder::formatter);
            applyByType(node, builder);
            return builder.build().validate();
        }

        private static void applyByType(JsonNode node, LogHandlerConfigBuilder builder) {
            switch (builder.type) {
            case console:
                // nothing more to load here
                return;
            case periodicRotatingFile:
                apply(node, "file", builder.name.getValue(), builder::file);
                apply(node, "suffix", null, builder::suffix);
                return;
            case custom:
                apply(node, "module", null, builder::module);
                apply(node, "class", null, builder::class_);
                if (node.has("properties") && !node.get("properties").isNull())
                    node.get("properties").fieldNames().forEachRemaining(fieldName
                            -> builder.property(fieldName, node.get("properties").get(fieldName).asText()));
                return;
            }
            throw new ConfigurationPlanLoadingException("unhandled log-handler type [" + builder.type + "]"
                    + " in [" + builder.name + "]");
        }

        /* make builder fields visible */ public static class LogHandlerConfigBuilder {}

        private LogHandlerConfig validate() {
            if (format == null && formatter == null || format != null && formatter != null)
                throw new ConfigurationPlanLoadingException(
                        "log-handler [" + name + "] must either have a format or a formatter");
            if (type == custom) {
                if (module == null)
                    throw new ConfigurationPlanLoadingException(
                            "log-handler [" + name + "] is of type [" + type + "], so it requires a 'module'");
                if (class_ == null)
                    throw new ConfigurationPlanLoadingException(
                            "log-handler [" + name + "] is of type [" + type + "], so it requires a 'class'");
            }
            return this;
        }

        @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

        public LogLevel getLevel() { return (level == null) ? ALL : level; }

        @Override public String toString() {
            return "«log-handler:" + getState() + ":" + type + ":" + name + ":" + getLevel()
                    + ((format == null) ? "" : ":format=" + format)
                    + ((formatter == null) ? "" : ":formatter=" + formatter)
                    + ((file == null) ? "" : ":" + file)
                    + ((suffix == null) ? "" : ":" + suffix)
                    + ((module == null) ? "" : ":" + module)
                    + ((class_ == null) ? "" : ":" + class_)
                    + ((properties == null) ? "" : ":" + properties)
                    + "»";
        }
    }

    private static void apply(JsonNode node, String fieldName, String defaultValue, Consumer<String> setter) {
        setter.accept((node.has(fieldName) && !node.get(fieldName).isNull())
                ? node.get(fieldName).asText() : defaultValue);
    }

    @Override public String toString() {
        return ""
                + "log-handlers:\n" + toStringList(logHandlers())
                + "loggers:\n" + toStringList(loggers())
                + "artifacts:\n" + toStringList(artifacts());
    }

    private String toStringList(Stream<?> stream) {
        return stream.map(Object::toString).collect(joining("\n  - ", "  - ", "\n"));
    }

    @SneakyThrows(IOException.class)
    public String toYaml() { return YAML.writeValueAsString(this); }
}
