package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.log.LogLevel;
import lombok.*;

import java.util.Map;

import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.LogHandlerType.*;
import static com.github.t1.deployer.model.Plan.*;
import static java.util.function.Function.*;
import static lombok.AccessLevel.*;

@Data
@Builder
@AllArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
public class LogHandlerPlan implements Plan.AbstractPlan {
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
        throw new Plan.PlanLoadingException("unhandled log-handler type [" + builder.type + "]"
                + " in [" + builder.name + "]");
    }

    /* make builder fields visible */ public static class LogHandlerPlanBuilder {}

    private LogHandlerPlan validate() {
        if (format != null && formatter != null)
            throw new Plan.PlanLoadingException(
                    "log-handler [" + name + "] can't have both a format and a formatter");
        if (type == custom) {
            if (module == null)
                throw new Plan.PlanLoadingException(
                        "log-handler [" + name + "] is of type [" + type + "], so it requires a 'module'");
            if (class_ == null)
                throw new Plan.PlanLoadingException(
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
