package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.deployer.model.DataSourcePlan.PoolPlan.PoolPlanBuilder;
import com.github.t1.deployer.tools.Tools;
import lombok.*;

import java.net.URI;
import java.util.regex.*;

import static com.github.t1.deployer.model.DeploymentState.*;
import static com.github.t1.deployer.model.Plan.*;
import static java.util.function.Function.*;
import static lombok.AccessLevel.*;

@Data
@Builder
@AllArgsConstructor(access = PRIVATE)
@JsonNaming(KebabCaseStrategy.class)
public class DataSourcePlan implements Plan.AbstractPlan {
    private static final Pattern JDBC_URI = Pattern.compile("jdbc:(\\p{Alnum}{1,256}):.*");

    @NonNull @JsonIgnore private final DataSourceName name;
    private final DeploymentState state;
    private final Boolean xa;
    private final URI uri;
    private final String jndiName;
    private final String driver;

    private final String userName;
    private final String password;

    private final PoolPlan pool;

    @lombok.Value
    @lombok.Builder
    @lombok.AllArgsConstructor(access = PRIVATE)
    @JsonNaming(KebabCaseStrategy.class)
    public static class PoolPlan {
        private final Integer min;
        private final Integer initial;
        private final Integer max;
        private final Age maxAge;

        @Override public String toString() { return min + "/" + initial + "/" + max + "/" + maxAge; }
    }

    @Override public String getId() { return name.getValue(); }

    static DataSourcePlan fromJson(DataSourceName name, JsonNode node) {
        if (node.isNull())
            throw new Plan.PlanLoadingException("incomplete data-sources plan '" + name + "'");
        DataSourcePlanBuilder builder = builder().name(name);
        apply(node, "xa", builder::xa, Tools::trueOrNull, "false");
        apply(node, "state", builder::state, DeploymentState::valueOf);
        apply(node, "uri", builder::uri, URI::create);
        apply(node, "jndi-name", builder::jndiName, identity(), "«java:/datasources/" + name + "DS»");
        apply(node, "driver", builder::driver, identity(), defaultDriver(builder.uri));

        apply(node, "user-name", builder::userName, identity());
        apply(node, "password", builder::password, identity());

        if (node.hasNonNull("pool")) {
            PoolPlanBuilder pool = PoolPlan.builder();
            JsonNode poolNode = node.get("pool");
            apply(poolNode, "min", pool::min, Integer::valueOf);
            apply(poolNode, "initial", pool::initial, Integer::valueOf);
            apply(poolNode, "max", pool::max, Integer::valueOf);
            apply(poolNode, "max-age", pool::maxAge, Age::new);
            builder.pool(pool.build());
        }

        return builder.build().validate();
    }

    private static String defaultDriver(URI uri) {
        if (uri != null) {
            Matcher matcher = JDBC_URI.matcher(uri.toString());
            if (matcher.matches())
                return "«" + matcher.group(1) + "»";
        }
        return "default.data-source-driver";
    }

    private DataSourcePlan validate() {
        if (uri == null && state != undeployed)
            throw new Plan.PlanLoadingException(
                    "field 'uri' for data-source '" + name + "' can only be null when undeploying");
        return this;
    }

    /* make builder fields visible */ public static class DataSourcePlanBuilder {}

    @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

    @Override public String toString() {
        return "data-source:" + getState() + ":" + name + ":" + jndiName + ":" + driver + ":" + uri
                + ((xa == null) ? "" : ":xa=" + xa)
                + ((pool == null) ? "" : "{" + pool + "}");
    }
}
