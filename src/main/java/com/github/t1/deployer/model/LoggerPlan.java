package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.log.LogLevel;
import lombok.*;

import java.util.*;

import static com.github.t1.deployer.model.DeploymentState.*;
import static java.lang.Boolean.*;
import static java.util.function.Function.*;
import static lombok.AccessLevel.*;

@Data
@Builder
@AllArgsConstructor(access = PRIVATE)
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class LoggerPlan implements Plan.AbstractPlan {
    @NonNull @JsonIgnore private final LoggerCategory category;
    private final DeploymentState state;
    private final LogLevel level;
    @NonNull private final List<LogHandlerName> handlers;
    @JsonProperty private final Boolean useParentHandlers;

    @Override public String getId() { return category.getValue(); }

    static LoggerPlan fromJson(LoggerCategory category, JsonNode node) {
        if (node.isNull())
            throw new Plan.PlanLoadingException("incomplete loggers plan '" + category + "'");
        LoggerPlanBuilder builder = builder().category(category);
        Plan.apply(node, "state", builder::state, DeploymentState::valueOf);
        Plan.apply(node, "level", builder::level, LogLevel::valueOf, "default.log-level or «DEBUG»");
        Plan.apply(node, "handler", builder::handler, identity());
        if (node.has("handlers"))
            applyHandlers(node, builder);
        if (!builder.category.isRoot())
            Plan.apply(node, "use-parent-handlers", builder::useParentHandlers, Boolean::valueOf);
        return builder.build().validate();
    }

    private static void applyHandlers(JsonNode node, LoggerPlanBuilder builder) {
        Iterator<JsonNode> handlers = node.get("handlers").elements();
        while (handlers.hasNext())
            builder.handler(Plan.expressions.resolve(handlers.next().textValue(), null));
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
            throw new Plan.PlanLoadingException("Can't set use-parent-handlers of [" + category + "] "
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
