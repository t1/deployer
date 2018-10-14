package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.log.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

import java.util.Map;

import static com.github.t1.deployer.model.DeploymentState.deployed;
import static com.github.t1.deployer.model.LogHandlerType.custom;
import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.deployer.model.Plan.AbstractPlan;
import static com.github.t1.deployer.model.Plan.PlanLoadingException;
import static com.github.t1.deployer.model.Plan.apply;
import static com.github.t1.deployer.model.Plan.expressions;
import static java.util.function.Function.identity;
import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@AllArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
public final class LogHandlerPlan implements AbstractPlan {
    private static final Expressions.VariableName DEFAULT_LOG_FORMATTER
            = new Expressions.VariableName("default.log-formatter");
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
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Singular private final Map<String, String> properties;


    @Override public String getId() { return name.getValue(); }

    static LogHandlerPlan fromJson(LogHandlerName name, JsonNode node) {
        LogHandlerPlanBuilder builder = builder().name(name);
        apply(node, "state", builder::state, DeploymentState::valueOf);
        apply(node, "level", builder::level, LogLevel::valueOf, "«ALL»");
        apply(node, "type", builder::type, LogHandlerType::valueOfTypeName,
                "default.log-handler-type or «" + periodicRotatingFile + "»");
        if (node.has("format") || (!node.has("formatter") && !Plan.expressions.contains(DEFAULT_LOG_FORMATTER)))
            apply(node, "format", builder::format, identity(), "default.log-format or null");
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
            applySuffix(node, builder, true);
            return;
        case custom:
            apply(node, "file", builder::file, identity(), null);
            applySuffix(node, builder, false);
            apply(node, "module", builder::module, identity());
            apply(node, "class", builder::class_, identity());
            if (node.has("properties") && !node.get("properties").isNull())
                node.get("properties").fieldNames().forEachRemaining(fieldName ->
                        builder.property(
                                expressions.resolve(fieldName),
                                expressions.resolve(node.get("properties").get(fieldName).asText())));
            return;
        }
        throw new PlanLoadingException("unhandled log-handler type [" + builder.type + "]"
                + " in [" + builder.name + "]");
    }

    private static void applySuffix(JsonNode node, LogHandlerPlanBuilder builder, boolean defaultSuffix) {
        apply(node, "suffix", builder::suffix, identity(),
                "default.log-file-suffix" + (defaultSuffix ? " or «" + DEFAULT_SUFFIX + "»" : ""));
    }

    /* make builder fields visible */ public static class LogHandlerPlanBuilder {}

    private LogHandlerPlan validate() {
        if (format != null && formatter != null)
            throw new PlanLoadingException("log-handler [" + name + "] can't have both a format and a formatter");
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
