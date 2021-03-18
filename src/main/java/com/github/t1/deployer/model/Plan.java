package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static java.util.stream.Collectors.joining;

/**
 * The plan of how the container should be or is configured. This class is responsible for loading the plan from/to YAML,
 * statically validating it, and applying default values.
 */
@Data @Accessors(chain = true)
@RequiredArgsConstructor
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

    static class PlanLoadingException extends RuntimeException {
        private static final long serialVersionUID = -1L;

        PlanLoadingException(String message) { super(message); }

        private PlanLoadingException(String message, Throwable cause) { super(message, cause); }
    }

    static Expressions expressions = null;

    @JsonProperty private Map<LogHandlerName, LogHandlerPlan> logHandlers = new LinkedHashMap<>();
    @JsonProperty private Map<LoggerCategory, LoggerPlan> loggers = new LinkedHashMap<>();
    @JsonProperty private Map<DataSourceName, DataSourcePlan> dataSources = new LinkedHashMap<>();
    @JsonProperty private Map<DeploymentName, DeployablePlan> deployables = new LinkedHashMap<>();
    @JsonProperty private Map<BundleName, BundlePlan> bundles = new LinkedHashMap<>();


    public Stream<LogHandlerPlan> logHandlers() { return logHandlers.values().stream(); }

    public Stream<LoggerPlan> loggers() { return loggers.values().stream(); }

    public Stream<DataSourcePlan> dataSources() { return dataSources.values().stream(); }

    public Stream<DeployablePlan> deployables() { return deployables.values().stream(); }

    public Stream<BundlePlan> bundles() { return bundles.values().stream(); }


    public Plan addLogHandler(LogHandlerPlan logHandlerPlan) { logHandlers.put(logHandlerPlan.getName(), logHandlerPlan); return this; }

    public Plan addLogger(LoggerPlan loggerPlan) { loggers.put(loggerPlan.getCategory(), loggerPlan); return this; }

    public Plan addDataSource(DataSourcePlan dataSourcePlan) { dataSources.put(dataSourcePlan.getName(), dataSourcePlan); return this; }

    public Plan addDeployable(DeployablePlan deployablePlan) { deployables.put(deployablePlan.getName(), deployablePlan); return this; }

    public Plan addBundle(BundlePlan bundlePlan) { bundles.put(bundlePlan.getName(), bundlePlan); return this; }


    public static Plan load(@NotNull Expressions expressions, Reader reader, String sourceMessage) {
        return Plan.with(expressions, sourceMessage, () -> {
            Plan plan = YAML.readValue(reader, Plan.class);
            if (plan == null)
                plan = new Plan();
            log.debug("plan loaded from {}:\n{}", sourceMessage, plan);
            return plan;
        });
    }

    public static synchronized Plan with(Expressions expressions, String sourceMessage, Callable<Plan> callable) {
        assert Plan.expressions == null : "can not reenter Plan#load";
        Plan.expressions = expressions;
        try {
            return callable.call();
        } catch (Exception e) {
            throw new PlanLoadingException("exception while loading plan from " + sourceMessage, e);
        } finally {
            Plan.expressions = null;
        }
    }

    @JsonCreator(mode = DELEGATING) public static Plan fromJson(JsonNode json) {
        Plan plan = new Plan();
        readAll(json.get("log-handlers"), LogHandlerName::new, LogHandlerPlan::fromJson, plan::addLogHandler);
        readAll(json.get("loggers"), LoggerCategory::of, LoggerPlan::fromJson, plan::addLogger);
        readAll(json.get("data-sources"), DataSourceName::new, DataSourcePlan::fromJson, plan::addDataSource);
        readAll(json.get("deployables"), DeploymentName::new, DeployablePlan::fromJson, plan::addDeployable);
        readAll(json.get("bundles"), BundleName::new, BundlePlan::fromJson, plan::addBundle);
        return plan;
    }

    private static <K, V> void readAll(JsonNode jsonNode, Function<String, K> toKey, BiFunction<K, JsonNode, V> toPlan,
                                       Consumer<V> consumer) {
        if (jsonNode != null)
            jsonNode.fieldNames().forEachRemaining(
                name -> consumer.accept(
                    toPlan.apply(toKey.apply(expressions.resolve(name, null)), jsonNode.get(name))));
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
            value = expressions.resolver().match(alternativeExpression).getValueOrNull();
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
