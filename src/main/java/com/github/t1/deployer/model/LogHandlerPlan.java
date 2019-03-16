package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.log.LogLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.t1.deployer.model.DeploymentState.deployed;
import static com.github.t1.deployer.model.LogHandlerType.custom;
import static com.github.t1.deployer.model.LogHandlerType.periodicRotatingFile;
import static com.github.t1.deployer.model.Plan.AbstractPlan;
import static com.github.t1.deployer.model.Plan.PlanLoadingException;
import static com.github.t1.deployer.model.Plan.apply;
import static com.github.t1.deployer.model.Plan.expressions;
import static java.util.function.Function.identity;

@Data @Accessors(chain = true)
@RequiredArgsConstructor
@JsonNaming(KebabCaseStrategy.class)
public final class LogHandlerPlan implements AbstractPlan {
    private static final Expressions.VariableName DEFAULT_LOG_FORMATTER
        = new Expressions.VariableName("default.log-formatter");
    public static final String DEFAULT_SUFFIX = ".yyyy-MM-dd";

    @NonNull @JsonIgnore private final LogHandlerName name;
    private DeploymentState state;
    private LogHandlerType type;
    private LogLevel level;
    private String format;
    private String formatter;

    private String file;
    private String suffix;
    private String encoding;

    private String module;
    @JsonProperty("class") private String class_;
    private Map<String, String> properties = new LinkedHashMap<>();


    @Override public String getId() { return name.getValue(); }

    public void addProperty(String key, String value) { properties.put(key, value); }

    static LogHandlerPlan fromJson(LogHandlerName name, JsonNode node) {
        LogHandlerPlan plan = new LogHandlerPlan(name);
        apply(node, "state", plan::setState, DeploymentState::valueOf);
        apply(node, "level", plan::setLevel, LogLevel::valueOf, "«ALL»");
        apply(node, "type", plan::setType, LogHandlerType::valueOfTypeName,
            "default.log-handler-type or «" + periodicRotatingFile + "»");
        if (node.has("format") || (!node.has("formatter") && !Plan.expressions.contains(DEFAULT_LOG_FORMATTER)))
            apply(node, "format", plan::setFormat, identity(), "default.log-format or null");
        apply(node, "formatter", plan::setFormatter, identity(), "default.log-formatter");
        apply(node, "encoding", plan::setEncoding, identity(), "default.log-encoding");
        applyByType(node, plan);
        return plan.validate();
    }

    private static void applyByType(JsonNode node, LogHandlerPlan plan) {
        switch (plan.type) {
            case console:
                // nothing more to load here
                return;
            case periodicRotatingFile:
                apply(node, "file", plan::setFile, identity(),
                    "«" + (plan.name.getValue().toLowerCase() + ".log") + "»");
                applySuffix(node, plan, true);
                return;
            case custom:
                apply(node, "file", plan::setFile, identity(), null);
                applySuffix(node, plan, false);
                apply(node, "module", plan::setModule, identity());
                apply(node, "class", plan::setClass_, identity());
                if (node.has("properties") && !node.get("properties").isNull())
                    node.get("properties").fieldNames().forEachRemaining(fieldName ->
                        plan.addProperty(
                            expressions.resolve(fieldName),
                            expressions.resolve(node.get("properties").get(fieldName).asText())));
                return;
        }
        throw new PlanLoadingException("unhandled log-handler type [" + plan.type + "]"
            + " in [" + plan.name + "]");
    }

    private static void applySuffix(JsonNode node, LogHandlerPlan plan, boolean defaultSuffix) {
        apply(node, "suffix", plan::setSuffix, identity(),
            "default.log-file-suffix" + (defaultSuffix ? " or «" + DEFAULT_SUFFIX + "»" : ""));
    }

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
        return "log-handler:" + getState() + ":" + type + ":" + name + ":" + level
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
