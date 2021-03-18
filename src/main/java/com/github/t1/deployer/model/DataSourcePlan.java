package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.KebabCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.t1.deployer.tools.Tools;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.t1.deployer.model.DeploymentState.deployed;
import static com.github.t1.deployer.model.DeploymentState.undeployed;
import static com.github.t1.deployer.model.Plan.apply;
import static java.util.function.Function.identity;

@Data @Accessors(chain = true)
@RequiredArgsConstructor
@JsonNaming(KebabCaseStrategy.class)
public final class DataSourcePlan implements Plan.AbstractPlan {
    private static final Pattern JDBC_URI = Pattern.compile("jdbc:(\\p{Alnum}{1,256}):.*");

    @NonNull @JsonIgnore private final DataSourceName name;
    private DeploymentState state;
    private Boolean xa;
    private URI uri;
    private String jndiName;
    private String driver;

    private String userName;
    private String password;

    private PoolPlan pool;

    @Data @Accessors(chain = true)
    @RequiredArgsConstructor
    @JsonNaming(KebabCaseStrategy.class)
    public static class PoolPlan {
        private Integer min;
        private Integer initial;
        private Integer max;
        private Age maxAge;

        @Override public String toString() { return min + "/" + initial + "/" + max + "/" + maxAge; }
    }

    @Override public String getId() { return name.getValue(); }

    static DataSourcePlan fromJson(DataSourceName name, JsonNode node) {
        if (node.isNull())
            throw new Plan.PlanLoadingException("incomplete data-sources plan '" + name + "'");
        DataSourcePlan builder = new DataSourcePlan(name);
        apply(node, "xa", builder::setXa, Tools::trueOrNull, "false");
        apply(node, "state", builder::setState, DeploymentState::valueOf);
        apply(node, "uri", builder::setUri, URI::create);
        apply(node, "jndi-name", builder::setJndiName, identity(), "«java:/datasources/" + name + "»");
        apply(node, "driver", builder::setDriver, identity(), defaultDriver(builder.uri));

        apply(node, "user-name", builder::setUserName, identity());
        apply(node, "password", builder::setPassword, identity());

        if (node.hasNonNull("pool")) {
            PoolPlan pool = new PoolPlan();
            JsonNode poolNode = node.get("pool");
            apply(poolNode, "min", pool::setMin, Integer::valueOf);
            apply(poolNode, "initial", pool::setInitial, Integer::valueOf);
            apply(poolNode, "max", pool::setMax, Integer::valueOf);
            apply(poolNode, "max-age", pool::setMaxAge, Age::new);
            builder.setPool(pool);
        }

        return builder.validate();
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

    @JsonIgnore @Override public DeploymentState getState() { return (state == null) ? deployed : state; }

    @Override public String toString() {
        return "data-source:" + getState() + ":" + name + ":" + jndiName + ":" + driver + ":" + uri + ":xa=" + xa
            + ((pool == null) ? "" : "{" + pool + "}");
    }
}
