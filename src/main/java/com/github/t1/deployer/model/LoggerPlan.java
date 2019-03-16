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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.github.t1.deployer.model.DeploymentState.deployed;
import static com.github.t1.deployer.model.Plan.apply;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.function.Function.identity;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
@Data @Accessors(chain = true)
@RequiredArgsConstructor
@JsonNaming(KebabCaseStrategy.class)
public final class LoggerPlan implements Plan.AbstractPlan {
    @NonNull @JsonIgnore private final LoggerCategory category;
    private DeploymentState state;
    private LogLevel level;
    private List<LogHandlerName> handlers = new ArrayList<>();
    @JsonProperty private Boolean useParentHandlers;

    @Override public String getId() { return category.getValue(); }

    public LoggerPlan addHandler(String name) {
        if (name != null)
            this.handlers.add(new LogHandlerName(name));
        return this;
    }

    static LoggerPlan fromJson(LoggerCategory category, JsonNode node) {
        if (node.isNull())
            throw new Plan.PlanLoadingException("incomplete loggers plan '" + category + "'");
        LoggerPlan plan = new LoggerPlan(category);
        apply(node, "state", plan::setState, DeploymentState::valueOf);
        apply(node, "level", plan::setLevel, LogLevel::valueOf, "default.log-level or «DEBUG»");
        applyHandlers(node, plan);
        apply(node, "use-parent-handlers", plan::setUseParentHandlers, Boolean::valueOf);
        return plan.validate();
    }

    private static void applyHandlers(JsonNode node, LoggerPlan plan) {
        if (node.has("handler")) {
            if (node.has("handlers"))
                throw new Plan.PlanLoadingException("Can't have 'handler' _and_ 'handlers'");
            apply(node, "handler", plan::addHandler, identity());
        } else if (node.has("handlers")) {
            Iterator<JsonNode> handlers = node.get("handlers").elements();
            while (handlers.hasNext())
                plan.addHandler(Plan.expressions.resolve(handlers.next().textValue()));
        }
    }

    private LoggerPlan validate() {
        if (useParentHandlers == FALSE && handlers.isEmpty())
            throw new Plan.PlanLoadingException("Can't set use-parent-handlers of [" + category + "] "
                + "to false when there are no handlers");
        if (category.isRoot() && useParentHandlers != null)
            throw new Plan.PlanLoadingException("Can't set use-parent-handlers of ROOT");
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
