package com.github.t1.deployer.app;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.deployer.container.*;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.model.Expressions.VariableName;
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
import static java.lang.Boolean.*;
import static java.util.Collections.*;
import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;
import static lombok.AccessLevel.*;

/**
 * The plan of how the container should be or is configured. This class is responsible for loading the plan from/to YAML,
 * statically validating it, and applying default values.
 */
@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = PRIVATE)
@Slf4j
@JsonNaming(KebabCaseStrategy.class)
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class Plan {
    public static final ObjectMapper YAML = new ObjectMapper(
            new YAMLFactory()
                    .enable(MINIMIZE_QUOTES)
                    .disable(WRITE_DOC_START_MARKER))
            .setSerializationInclusion(NON_EMPTY)
            // preferable to @JsonNaming, but conflicts in container: .setPropertyNamingStrategy(KEBAB_CASE)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules();

    public static class PlanLoadingException extends RuntimeException {
        public PlanLoadingException(String message) { super(message); }

        public PlanLoadingException(String message, Throwable cause) { super(message, cause); }
    }

    private static final Plan EMPTY_PLAN = Plan.builder().build();

    private static Expressions expressions = null;

    synchronized public static Plan load(Expressions expressions, Reader reader, String sourceMessage) {
        Plan.expressions = expressions;
        try {
            Plan plan = YAML.readValue(reader, Plan.class);
            if (plan == null)
                plan = EMPTY_PLAN;
            log.debug("plan loaded from {}:\n{}", sourceMessage, plan);
            return plan;
        } catch (IOException e) {
            throw new PlanLoadingException("exception while loading plan from " + sourceMessage, e);
        } finally {
            Plan.expressions = null;
        }
    }

    @JsonCreator public static Plan fromJson(JsonNode json) {
        PlanBuilder builder = builder();
        readAll(json.get("log-handlers"), LogHandlerName::new, LogHandlerPlan::fromJson, builder::logHandler);
        readAll(json.get("loggers"), LoggerCategory::of, LoggerPlan::fromJson, builder::logger);
        readAll(json.get("deployables"), DeploymentName::new, DeployablePlan::fromJson, builder::deployable);
        readAll(json.get("bundles"), BundleName::new, BundlePlan::fromJson, builder::bundle);
        return builder.build();
    }

    public static <K, V> void readAll(JsonNode jsonNode, Function<String, K> toKey, BiFunction<K, JsonNode, V> toPlan,
            Consumer<V> consumer) {
        if (jsonNode != null)
            jsonNode.fieldNames().forEachRemaining(
                    name -> consumer.accept(
                            toPlan.apply(toKey.apply(expressions.resolve(name, null)), jsonNode.get(name))));
    }

    @NonNull @JsonProperty private final Map<LogHandlerName, LogHandlerPlan> logHandlers;
    @NonNull @JsonProperty private final Map<LoggerCategory, LoggerPlan> loggers;
    @NonNull @JsonProperty private final Map<DeploymentName, DeployablePlan> deployables;
    @NonNull @JsonProperty private final Map<BundleName, BundlePlan> bundles;

    public Stream<LogHandlerPlan> logHandlers() { return logHandlers.values().stream(); }

    public Stream<LoggerPlan> loggers() { return loggers.values().stream(); }

    public Stream<DeployablePlan> deployables() { return deployables.values().stream(); }

    public Stream<BundlePlan> bundles() { return bundles.values().stream(); }

    public static class PlanBuilder {
        private Map<LogHandlerName, LogHandlerPlan> logHandlers = new LinkedHashMap<>();
        private Map<LoggerCategory, LoggerPlan> loggers = new LinkedHashMap<>();
        private Map<DeploymentName, DeployablePlan> deployables = new LinkedHashMap<>();
        private Map<BundleName, BundlePlan> bundles = new LinkedHashMap<>();

        public PlanBuilder logHandler(LogHandlerPlan plan) {
            this.logHandlers.put(plan.getName(), plan);
            return this;
        }

        public PlanBuilder logger(LoggerPlan plan) {
            this.loggers.put(plan.getCategory(), plan);
            return this;
        }

        public PlanBuilder deployable(DeployablePlan plan) {
            this.deployables.put(plan.getName(), plan);
            return this;
        }

        public PlanBuilder bundle(BundlePlan plan) {
            this.bundles.put(plan.getName(), plan);
            return this;
        }
    }

    public interface AbstractPlan {
        DeploymentState getState();

        @JsonIgnore String getId();
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class AbstractArtifactPlan implements AbstractPlan {
        private final DeploymentState state;
        private final GroupId groupId;
        @NonNull private final ArtifactId artifactId;
        @NonNull private final Version version;
        private final Classifier classifier;
        private final Checksum checksum;

        /** can't be abstract, as the class can't be abstract, as the lombok builder would complain */
        @Override public String getId() { throw new UnsupportedOperationException("need to overload"); }

        @SuppressWarnings("unchecked")
        public static class AbstractArtifactPlanBuilder<T extends AbstractArtifactPlanBuilder> {
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

            public T classifier(Classifier classifier) {
                this.classifier = classifier;
                return (T) this;
            }

            public T checksum(Checksum checksum) {
                this.checksum = checksum;
                return (T) this;
            }
        }

        public static void fromJson(JsonNode node, AbstractArtifactPlanBuilder builder,
                String defaultArtifactId, String defaultVersion) {
            apply(node, "state", builder::state, DeploymentState::valueOf);
            apply(node, "group-id", builder::groupId, GroupId::of, "default.group-id");
            apply(node, "artifact-id", builder::artifactId, ArtifactId::new, "«" + defaultArtifactId + "»");
            apply(node, "version", builder::version, Version::new, defaultVersion, defaultVersion);
            apply(node, "classifier", builder::classifier, Classifier::new);
            apply(node, "checksum", builder::checksum, Checksum::fromString);
            verify(builder);
        }

        private static void verify(AbstractArtifactPlanBuilder builder) {
            if (builder.groupId == null && builder.state != undeployed)
                throw new PlanLoadingException("the `group-id` can only be null when undeploying");
        }

        @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

        @Override public String toString() {
            return getState() + ":" + groupId + ":" + artifactId + ":" + version
                    + ((classifier == null) ? "" : ":" + classifier);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Builder
    @JsonNaming(KebabCaseStrategy.class)
    public static class DeployablePlan extends AbstractArtifactPlan {
        @NonNull @JsonIgnore private final DeploymentName name;
        @NonNull private final ArtifactType type;

        @Override public String getId() { return name.getValue(); }

        public static class DeployablePlanBuilder extends AbstractArtifactPlanBuilder<DeployablePlanBuilder> {
            @Override public DeployablePlan build() {
                AbstractArtifactPlan a = super.build();
                return new DeployablePlan(name, type,
                        a.state, a.groupId, a.artifactId, a.version, a.classifier, a.checksum);
            }
        }

        private DeployablePlan(DeploymentName name, ArtifactType type, DeploymentState state,
                GroupId groupId, ArtifactId artifactId, Version version, Classifier classifier, Checksum checksum) {
            super(state, groupId, artifactId, version, classifier, checksum);
            this.name = name;
            this.type = type;
        }

        public static DeployablePlan fromJson(DeploymentName name, JsonNode node) {
            if (node.isNull())
                throw new PlanLoadingException("incomplete plan for deployable '" + name + "'");
            DeployablePlanBuilder builder = builder().name(name);
            AbstractArtifactPlan.fromJson(node, builder, name.getValue(), "«CURRENT»");
            apply(node, "type", builder::type, ArtifactType::valueOf, "default.deployable-type or «war»");
            return builder.build().verify();
        }

        private DeployablePlan verify() {
            if (getType() == bundle)
                throw new PlanLoadingException(
                        "a deployable may not be of type 'bundle'; use 'bundles' plan instead.");
            return this;
        }

        @Override public String toString() {
            return "deployment:" + name + ":" + super.toString()
                    + ":" + type + ((getChecksum() == null) ? "" : ":" + getChecksum());
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Builder
    @JsonNaming(KebabCaseStrategy.class)
    public static class BundlePlan extends AbstractArtifactPlan {
        @NonNull @JsonIgnore private final BundleName name;
        @NonNull @Singular private final Map<String, Map<VariableName, String>> instances;

        @Override public String getId() { return name.getValue(); }

        public static class BundlePlanBuilder extends AbstractArtifactPlanBuilder<BundlePlanBuilder> {
            @Override public BundlePlan build() {
                AbstractArtifactPlan a = super.build();
                Map<String, Map<VariableName, String>> instances = buildInstances(instances$key, instances$value);
                return new BundlePlan(name, instances,
                        a.state, a.groupId, a.artifactId, a.version, a.classifier, a.checksum);
            }

            private Map<String, Map<VariableName, String>> buildInstances(
                    List<String> keys, List<Map<VariableName, String>> list) {
                if (keys == null)
                    return emptyMap();
                Map<String, Map<VariableName, String>> instances = new LinkedHashMap<>();
                for (int i = 0; i < keys.size(); i++)
                    instances.put(keys.get(i), list.get(i));
                return instances;
            }
        }

        private BundlePlan(BundleName name, Map<String, Map<VariableName, String>> instances, DeploymentState state,
                GroupId groupId, ArtifactId artifactId, Version version, Classifier classifier, Checksum checksum) {
            super(state, groupId, artifactId, version, classifier, checksum);
            this.name = name;
            this.instances = instances;
        }


        public static BundlePlan fromJson(BundleName name, JsonNode node) {
            if (node.isNull())
                throw new PlanLoadingException("incomplete plan for bundle '" + name + "'");
            BundlePlanBuilder builder = builder().name(name);
            AbstractArtifactPlan.fromJson(node, builder, name.getValue(), null);
            if (node.has("instances") && !node.get("instances").isNull()) {
                Iterator<Map.Entry<String, JsonNode>> instances = node.get("instances").fields();
                while (instances.hasNext()) {
                    Map.Entry<String, JsonNode> next = instances.next();
                    builder.instance(next.getKey(), toVariableMap(next.getValue()));
                }
            } else {
                builder.instance(null, ImmutableMap.of());
            }
            return builder.build();
        }

        private static Map<VariableName, String> toVariableMap(JsonNode node) {
            ImmutableMap.Builder<VariableName, String> builder = ImmutableMap.builder();
            node.fields().forEachRemaining(field
                    -> builder.put(new VariableName(field.getKey()), field.getValue().asText()));
            return builder.build();
        }

        @Override public String toString() {
            return "bundle:" + name + ":" + super.toString() + (noInstances() ? "" : ":" + instances);
        }

        private boolean noInstances() {
            return instances.size() == 1 && instances.containsKey(null) && instances.get(null).isEmpty();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class LoggerPlan implements AbstractPlan {
        @NonNull @JsonIgnore private final LoggerCategory category;
        private final DeploymentState state;
        private final LogLevel level;
        @NonNull private final List<LogHandlerName> handlers;
        @JsonProperty private final Boolean useParentHandlers;

        @Override public String getId() { return category.getValue(); }

        private static LoggerPlan fromJson(LoggerCategory category, JsonNode node) {
            if (node.isNull())
                throw new PlanLoadingException("incomplete plan for logger '" + category + "'");
            LoggerPlanBuilder builder = builder().category(category);
            apply(node, "state", builder::state, DeploymentState::valueOf);
            apply(node, "level", builder::level, LoggerResource::mapLogLevel, "default.log-level or «DEBUG»");
            apply(node, "handler", builder::handler, identity());
            if (node.has("handlers"))
                applyHandlers(node, builder);
            if (!builder.category.isRoot())
                apply(node, "use-parent-handlers", builder::useParentHandlers, Boolean::valueOf);
            return builder.build().validate();
        }

        private static void applyHandlers(JsonNode node, LoggerPlanBuilder builder) {
            Iterator<JsonNode> handlers = node.get("handlers").elements();
            while (handlers.hasNext())
                builder.handler(expressions.resolve(handlers.next().textValue(), null));
        }

        public static class LoggerPlanBuilder {
            private List<LogHandlerName> handlers = new ArrayList<>();

            public LoggerPlanBuilder handler(String name) {
                if (name != null)
                    this.handlers.add(new LogHandlerName(name));
                return this;
            }
        }

        private LoggerPlan validate() {
            if (useParentHandlers == FALSE && handlers.isEmpty())
                throw new PlanLoadingException("Can't set use-parent-handlers of [" + category + "] "
                        + "to false when there are no handlers");
            return this;
        }

        @SuppressWarnings("unused") @JsonIgnore public Boolean getUseParentHandlers() {
            return (useParentHandlers == null) ? handlers.isEmpty() : useParentHandlers;
        }

        @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

        @Override public String toString() {
            return "logger:" + getState() + ":" + category + ":" + getLevel() + ":"
                    + handlers + (useParentHandlers == TRUE ? "+" : "");
        }
    }

    @Data
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class LogHandlerPlan implements AbstractPlan {
        private static final VariableName DEFAULT_LOG_FORMATTER = new VariableName("default.log-formatter");
        public static final String DEFAULT_LOG_FORMAT = "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n";
        public static final String DEFAULT_SUFFIX = ".yyyy-MM-dd";

        @NonNull @JsonIgnore private final LogHandlerName name;
        private final DeploymentState state;
        @NonNull private final LogHandlerType type;
        private final LogLevel level;
        private final String format;
        private final String formatter;

        private final String file;
        private final String suffix;
        private final String encoding;

        private final String module;
        @JsonProperty("class") private final String class_;
        @Singular private final Map<String, String> properties;


        @Override public String getId() { return name.getValue(); }

        private static LogHandlerPlan fromJson(LogHandlerName name, JsonNode node) {
            LogHandlerPlanBuilder builder = builder().name(name);
            apply(node, "state", builder::state, DeploymentState::valueOf);
            apply(node, "level", builder::level, LoggerResource::mapLogLevel, "«ALL»");
            apply(node, "type", builder::type, LogHandlerType::valueOfTypeName,
                    "default.log-handler-type or «" + periodicRotatingFile + "»");
            if (node.has("format") || (!node.has("formatter") && !expressions.contains(DEFAULT_LOG_FORMATTER)))
                apply(node, "format", builder::format, identity(),
                        "default.log-format or «" + DEFAULT_LOG_FORMAT + "»");
            apply(node, "formatter", builder::formatter, identity(), "default.log-formatter");
            apply(node, "encoding", builder::encoding, identity(), "default.log-encoding");
            applyByType(node, builder);
            return builder.build().validate();
        }

        private static void applyByType(JsonNode node, LogHandlerPlanBuilder builder) {
            switch (builder.type) {
            case console:
                // nothing more to load here
                return;
            case periodicRotatingFile:
                apply(node, "file", builder::file, identity(),
                        "«" + (builder.name.getValue().toLowerCase() + ".log") + "»");
                apply(node, "suffix", builder::suffix, identity(),
                        "default.log-file-suffix or «" + DEFAULT_SUFFIX + "»");
                return;
            case custom:
                apply(node, "module", builder::module, identity());
                apply(node, "class", builder::class_, identity());
                if (node.has("properties") && !node.get("properties").isNull())
                    node.get("properties").fieldNames().forEachRemaining(fieldName
                            -> builder.property(fieldName, node.get("properties").get(fieldName).asText()));
                return;
            }
            throw new PlanLoadingException("unhandled log-handler type [" + builder.type + "]"
                    + " in [" + builder.name + "]");
        }

        /* make builder fields visible */ public static class LogHandlerPlanBuilder {}

        private LogHandlerPlan validate() {
            if (format != null && formatter != null)
                throw new PlanLoadingException(
                        "log-handler [" + name + "] can't have both a format and a formatter");
            if (type == custom) {
                if (module == null)
                    throw new PlanLoadingException(
                            "log-handler [" + name + "] is of type [" + type + "], so it requires a 'module'");
                if (class_ == null)
                    throw new PlanLoadingException(
                            "log-handler [" + name + "] is of type [" + type + "], so it requires a 'class'");
            }
            return this;
        }

        @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

        @Override public String toString() {
            return "log-handler:" + getState() + ":" + type + ":" + name + ":" + getLevel()
                    + ((format == null) ? "" : ":format=" + format)
                    + ((formatter == null) ? "" : ":formatter=" + formatter)
                    + ((file == null) ? "" : ":" + file)
                    + ((suffix == null) ? "" : ":" + suffix)
                    + ((encoding == null) ? "" : ":" + encoding)
                    + ((module == null) ? "" : ":" + module)
                    + ((class_ == null) ? "" : ":" + class_)
                    + ((properties == null) ? "" : ":" + properties);
        }
    }

    private static <T> void apply(JsonNode node, String fieldName, Consumer<T> setter, Function<String, T> convert) {
        apply(node, fieldName, setter, convert, null);
    }

    private static <T> void apply(JsonNode node, String fieldName, Consumer<T> setter, Function<String, T> convert,
            CharSequence alternativeExpression) {
        apply(node, fieldName, setter, convert, alternativeExpression, null);
    }

    private static <T> void apply(JsonNode node, String fieldName, Consumer<T> setter, Function<String, T> convert,
            CharSequence alternativeExpression, String expressionAlternative) {
        String value = (node.has(fieldName) && !node.get(fieldName).isNull())
                ? expressions.resolve(node.get(fieldName).asText(), expressionAlternative)
                : null;
        if (value == null && alternativeExpression != null)
            value = expressions.resolver(alternativeExpression).getValueOr(null);
        setter.accept((value == null) ? null : convert.apply(value));
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
