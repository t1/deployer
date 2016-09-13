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
import static com.github.t1.deployer.container.LogHandlerType.*;
import static com.github.t1.deployer.model.ArtifactType.*;
import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.tools.Tools.toMap;
import static com.github.t1.log.LogLevel.*;
import static java.lang.Boolean.*;
import static java.util.Collections.*;
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

    private static Variables variables = null;

    synchronized public static ConfigurationPlan load(Variables variables, Reader reader) {
        ConfigurationPlan.variables = variables;
        try {
            ConfigurationPlan plan = YAML.readValue(variables.resolve(reader), ConfigurationPlan.class);
            if (plan == null)
                plan = EMPTY_PLAN;
            log.debug("config plan loaded:\n{}", plan);
            return plan;
        } catch (IOException e) {
            throw new ConfigurationPlanLoadingException("exception while loading config plan", e);
        } finally {
            ConfigurationPlan.variables = null;
        }
    }

    @JsonCreator public static ConfigurationPlan fromJson(JsonNode json) {
        ConfigurationPlanBuilder builder = builder();
        readAll(json.get("log-handlers"), LogHandlerName::new, LogHandlerConfig::fromJson, builder::logHandler);
        readAll(json.get("loggers"), LoggerCategory::of, LoggerConfig::fromJson, builder::logger);
        readAll(json.get("deployables"), DeploymentName::new, DeployableConfig::fromJson, builder::deployable);
        readAll(json.get("bundles"), BundleName::new, BundleConfig::fromJson, builder::bundle);
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
    @NonNull @JsonProperty private final Map<DeploymentName, DeployableConfig> deployables;
    @NonNull @JsonProperty private final Map<BundleName, BundleConfig> bundles;

    public Stream<LogHandlerConfig> logHandlers() { return logHandlers.values().stream(); }

    public Stream<LoggerConfig> loggers() { return loggers.values().stream(); }

    public Stream<DeployableConfig> deployables() { return deployables.values().stream(); }

    public Stream<BundleConfig> bundles() { return bundles.values().stream(); }

    public static class ConfigurationPlanBuilder {
        private Map<LogHandlerName, LogHandlerConfig> logHandlers = new LinkedHashMap<>();
        private Map<LoggerCategory, LoggerConfig> loggers = new LinkedHashMap<>();
        private Map<DeploymentName, DeployableConfig> deployables = new LinkedHashMap<>();
        private Map<BundleName, BundleConfig> bundles = new LinkedHashMap<>();

        public ConfigurationPlanBuilder logHandler(LogHandlerConfig config) {
            this.logHandlers.put(config.getName(), config);
            return this;
        }

        public ConfigurationPlanBuilder logger(LoggerConfig config) {
            this.loggers.put(config.getCategory(), config);
            return this;
        }

        public ConfigurationPlanBuilder deployable(DeployableConfig config) {
            this.deployables.put(config.getName(), config);
            return this;
        }

        public ConfigurationPlanBuilder bundle(BundleConfig config) {
            this.bundles.put(config.getName(), config);
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
    public static class AbstractArtifactConfig implements AbstractConfig {
        private final DeploymentState state;
        @NonNull private final GroupId groupId;
        @NonNull private final ArtifactId artifactId;
        @NonNull private final Version version;
        private final Checksum checksum;

        @SuppressWarnings("unchecked")
        public static class AbstractArtifactConfigBuilder<T extends AbstractArtifactConfigBuilder> {
            public T state(DeploymentState state) {
                this.state = state;
                return (T) this;
            }

            public T groupId(GroupId groupId) {
                this.groupId = groupId;
                return (T) this;
            }

            public T artifactId(ArtifactId artifactId) {
                this.artifactId = artifactId;
                return (T) this;
            }

            public T version(Version version) {
                this.version = version;
                return (T) this;
            }

            public T checksum(Checksum checksum) {
                this.checksum = checksum;
                return (T) this;
            }
        }

        public static void fromJson(JsonNode node, AbstractArtifactConfigBuilder builder,
                String defaultArtifactId, String defaultVersion) {
            apply(node, "group-id", value -> builder.groupId(new GroupId(defaultValue(value, "group-id"))));
            apply(node, "artifact-id",
                    value -> builder.artifactId(new ArtifactId((value == null) ? defaultArtifactId : value)));
            apply(node, "state", value -> builder.state((value == null) ? null : DeploymentState.valueOf(value)));
            apply(node, "version", value -> builder.version(
                    (value != null)
                            ? new Version(value)
                            : (defaultVersion != null)
                                    ? new Version(defaultVersion)
                                    : null));
            apply(node, "checksum",
                    value -> builder.checksum((value == null) ? null : Checksum.fromString(value)));
        }

        @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

        @Override public String toString() {
            return getState() + ":" + groupId + ":" + artifactId + ":" + version;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Builder
    @JsonNaming(KebabCaseStrategy.class)
    public static class DeployableConfig extends AbstractArtifactConfig {
        @NonNull @JsonIgnore private final DeploymentName name;
        @NonNull private final ArtifactType type;

        public static class DeployableConfigBuilder extends AbstractArtifactConfigBuilder<DeployableConfigBuilder> {
            @Override public DeployableConfig build() {
                AbstractArtifactConfig a = super.build();
                return new DeployableConfig(name, type, a.state, a.groupId, a.artifactId, a.version, a.checksum);
            }
        }

        private DeployableConfig(DeploymentName name, ArtifactType type, DeploymentState state,
                GroupId groupId, ArtifactId artifactId, Version version, Checksum checksum) {
            super(state, groupId, artifactId, version, checksum);
            this.name = name;
            this.type = type;
        }

        public static DeployableConfig fromJson(DeploymentName name, JsonNode node) {
            if (node.isNull())
                throw new ConfigurationPlanLoadingException("no config in deployable '" + name + "'");
            DeployableConfigBuilder builder = builder().name(name);
            AbstractArtifactConfig.fromJson(node, builder, name.getValue(), "CURRENT");
            apply(node, "type",
                    value -> builder.type(ArtifactType.valueOf(defaultValue(value, "deployable-type", "«war»"))));
            return builder.build().verify();
        }

        private DeployableConfig verify() {
            if (getType() == bundle)
                throw new ConfigurationPlanLoadingException(
                        "a deployable may not be of type 'bundle'; use 'bundles' plan instead.");
            return this;
        }

        @Override public String toString() { return "«deployment:" + name + ":" + super.toString() + ":" + type + "»"; }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Builder
    @JsonNaming(KebabCaseStrategy.class)
    public static class BundleConfig extends AbstractArtifactConfig {
        @NonNull @JsonIgnore private final BundleName name;
        @NonNull @Singular private final Map<String, Map<String, String>> instances;

        public static class BundleConfigBuilder extends AbstractArtifactConfigBuilder<BundleConfigBuilder> {
            @Override public BundleConfig build() {
                AbstractArtifactConfig a = super.build();
                Map<String, Map<String, String>> instances = buildInstances(instances$key, instances$value);
                return new BundleConfig(name, instances, a.state, a.groupId, a.artifactId, a.version, a.checksum);
            }

            private Map<String, Map<String, String>> buildInstances(List<String> keys, List<Map<String, String>> list) {
                if (keys == null)
                    return emptyMap();
                Map<String, Map<String, String>> instances = new LinkedHashMap<>();
                for (int i = 0; i < keys.size(); i++)
                    instances.put(keys.get(i), list.get(i));
                return instances;
            }
        }

        private BundleConfig(BundleName name, Map<String, Map<String, String>> instances, DeploymentState state,
                GroupId groupId, ArtifactId artifactId, Version version, Checksum checksum) {
            super(state, groupId, artifactId, version, checksum);
            this.name = name;
            this.instances = instances;
        }


        public static BundleConfig fromJson(BundleName name, JsonNode node) {
            if (node.isNull())
                throw new ConfigurationPlanLoadingException("no config in bundle '" + name + "'");
            BundleConfigBuilder builder = builder().name(name);
            AbstractArtifactConfig.fromJson(node, builder, name.getValue(), null);
            if (node.has("instances") && !node.get("instances").isNull()) {
                Iterator<Map.Entry<String, JsonNode>> instances = node.get("instances").fields();
                while (instances.hasNext()) {
                    Map.Entry<String, JsonNode> next = instances.next();
                    builder.instance(next.getKey(), toMap(next.getValue()));
                }
            } else {
                builder.instance(null, ImmutableMap.of());
            }
            return builder.build();
        }

        @Override public String toString() {
            return "«bundle:" + name + ":" + super.toString() + ":" + variables + "»";
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
            apply(node, "state", value -> builder.state((value == null) ? null : DeploymentState.valueOf(value)));
            apply(node, "level", value
                    -> builder.level(LogLevel.valueOf(defaultValue(value, "log-level", "«DEBUG»"))));
            apply(node, "handler", builder::handler);
            if (node.has("handlers")) {
                Iterator<JsonNode> handlers = node.get("handlers").elements();
                while (handlers.hasNext())
                    builder.handler(handlers.next().textValue());
            }
            if (!builder.category.isRoot())
                apply(node, "use-parent-handlers", value -> {
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
        public static final String DEFAULT_LOG_FORMAT = "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n";

        @NonNull @JsonIgnore private final LogHandlerName name;
        private final DeploymentState state;
        private final LogLevel level;
        @NonNull private final LogHandlerType type;
        private final String format;
        private final String formatter;

        private final String file;
        private final String suffix;

        private final String module;
        @JsonProperty("class") private final String class_;
        @Singular private final Map<String, String> properties;


        private static LogHandlerConfig fromJson(LogHandlerName name, JsonNode node) {
            LogHandlerConfigBuilder builder = builder().name(name);
            apply(node, "state", value -> builder.state((value == null) ? null : DeploymentState.valueOf(value)));
            apply(node, "level", value -> builder.level((value == null) ? ALL : LogLevel.valueOf(value)));
            apply(node, "type", value -> builder.type(LogHandlerType.valueOfTypeName(
                    defaultValue(value, "log-handler-type", "«" + periodicRotatingFile + "»"))));
            if (node.has("format") || (!node.has("formatter") && !variables.contains("default.log-formatter")))
                apply(node, "format", value -> builder.format(
                        defaultValue(value, "log-format", "«" + DEFAULT_LOG_FORMAT + "»")));
            apply(node, "formatter", value -> builder.formatter(defaultValue(value, "log-formatter")));
            applyByType(node, builder);
            return builder.build().validate();
        }

        private static void applyByType(JsonNode node, LogHandlerConfigBuilder builder) {
            switch (builder.type) {
            case console:
                // nothing more to load here
                return;
            case periodicRotatingFile:
                apply(node, "file", value -> builder.file((value == null)
                        ? builder.name.getValue().toLowerCase() + ".log"
                        : value));
                apply(node, "suffix", builder::suffix);
                return;
            case custom:
                apply(node, "module", builder::module);
                apply(node, "class", builder::class_);
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
            if (format != null && formatter != null)
                throw new ConfigurationPlanLoadingException(
                        "log-handler [" + name + "] can't have both a format and a formatter");
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

    private static String defaultValue(String value, String name, String... alternativeExpressions) {
        if (value != null)
            return value;
        StringBuilder expression = new StringBuilder("default." + name);
        for (String alternativeExpression : alternativeExpressions)
            expression.append(" or ").append(alternativeExpression);
        return variables.resolveExpression(expression.toString());
    }

    private static void apply(JsonNode node, String fieldName, Consumer<String> setter) {
        setter.accept((node.has(fieldName) && !node.get(fieldName).isNull()) ? node.get(fieldName).asText() : null);
    }

    @Override public String toString() {
        return ""
                + "log-handlers:\n" + toStringList(logHandlers())
                + "loggers:\n" + toStringList(loggers())
                + "deployables:\n" + toStringList(deployables())
                + "bundles:\n" + toStringList(bundles());
    }

    private String toStringList(Stream<?> stream) {
        return stream.map(Object::toString).collect(joining("\n  - ", "  - ", "\n"));
    }

    @SneakyThrows(IOException.class)
    public String toYaml() { return YAML.writeValueAsString(this); }
}
