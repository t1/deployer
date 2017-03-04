package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.*;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
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
public final class Plan {
    public static final ObjectMapper YAML = new ObjectMapper(
            new YAMLFactory()
                    .enable(MINIMIZE_QUOTES)
                    .disable(WRITE_DOC_START_MARKER))
            .setSerializationInclusion(NON_EMPTY)
            // preferable to @JsonNaming, but conflicts in container: .setPropertyNamingStrategy(KEBAB_CASE)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules();

    public static class PlanLoadingException extends RuntimeException {
        private static final long serialVersionUID = -1L;

        public PlanLoadingException(String message) { super(message); }

        public PlanLoadingException(String message, Throwable cause) { super(message, cause); }
    }

    private static final Plan EMPTY_PLAN = Plan.builder().build();

    static Expressions expressions = null;

    public static Plan load(Expressions expressions, Reader reader, String sourceMessage) {
        return Plan.with(expressions, sourceMessage, () -> {
            Plan plan = YAML.readValue(reader, Plan.class);
            if (plan == null)
                plan = EMPTY_PLAN;
            log.debug("plan loaded from {}:\n{}", sourceMessage, plan);
            return plan;
        });
    }

    public static synchronized Plan with(Expressions expressions, String sourceMessage, Callable<Plan> callable) {
        Plan.expressions = expressions;
        try {
            return callable.call();
        } catch (Exception e) {
            throw new PlanLoadingException("exception while loading plan from " + sourceMessage, e);
        } finally {
            Plan.expressions = null;
        }
    }

    @JsonCreator public static Plan fromJson(JsonNode json) {
        PlanBuilder builder = builder();
        readAll(json.get("log-handlers"), LogHandlerName::new, LogHandlerPlan::fromJson, builder::logHandler);
        readAll(json.get("loggers"), LoggerCategory::of, LoggerPlan::fromJson, builder::logger);
        readAll(json.get("data-sources"), DataSourceName::new, DataSourcePlan::fromJson, builder::dataSource);
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
    @NonNull @JsonProperty private final Map<DataSourceName, DataSourcePlan> dataSources;
    @NonNull @JsonProperty private final Map<DeploymentName, DeployablePlan> deployables;
    @NonNull @JsonProperty private final Map<BundleName, BundlePlan> bundles;

    public Stream<LogHandlerPlan> logHandlers() { return logHandlers.values().stream(); }

    public Stream<LoggerPlan> loggers() { return loggers.values().stream(); }

    public Stream<DataSourcePlan> dataSources() { return dataSources.values().stream(); }

    public Stream<DeployablePlan> deployables() { return deployables.values().stream(); }

    public Stream<BundlePlan> bundles() { return bundles.values().stream(); }

    public static class PlanBuilder {
        private Map<LogHandlerName, LogHandlerPlan> logHandlers = new LinkedHashMap<>();
        private Map<LoggerCategory, LoggerPlan> loggers = new LinkedHashMap<>();
        private Map<DataSourceName, DataSourcePlan> dataSources = new LinkedHashMap<>();
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

        public PlanBuilder dataSource(DataSourcePlan plan) {
            this.dataSources.put(plan.getName(), plan);
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


    static <T> void apply(JsonNode node, String fieldName, Consumer<T> setter, Function<String, T> convert) {
        apply(node, fieldName, setter, convert, null);
    }

    static <T> void apply(JsonNode node, String fieldName, Consumer<T> setter, Function<String, T> convert,
            String alternativeExpression) {
        String value = (node.has(fieldName) && !node.get(fieldName).isNull())
                ? expressions.resolve(node.get(fieldName).asText(), alternativeExpression)
                : null;
        if (value == null && alternativeExpression != null)
            value = expressions.resolver(alternativeExpression).getValueOrNull();
        setter.accept((value == null) ? null : convert.apply(value));
    }

    @Override public String toString() {
        return ""
                + "log-handlers:\n" + toStringList(logHandlers())
                + "loggers:\n" + toStringList(loggers())
                + "data-sources:\n" + toStringList(dataSources())
                + "deployables:\n" + toStringList(deployables())
                + "bundles:\n" + toStringList(bundles());
    }

    private String toStringList(Stream<?> stream) {
        return stream.map(Object::toString).collect(joining("\n  - ", "  - ", "\n"));
    }

    @SneakyThrows(IOException.class)
    public String toYaml() { return YAML.writeValueAsString(this); }
}
