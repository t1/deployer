package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.net.URI;
import java.util.regex.*;

import static com.github.t1.deployer.model.DeploymentState.*;
import static java.util.function.Function.*;
import static lombok.AccessLevel.*;

@Data
@Builder
@AllArgsConstructor(access = PRIVATE)
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class DataSourcePlan implements Plan.AbstractPlan {
    private static final Pattern JDBC_URI = Pattern.compile("jdbc:(\\p{Alnum}{1,256}):.*");

    @NonNull @JsonIgnore private final DataSourceName name;
    private final DeploymentState state;
    private final String driver;
    private final String jndiName;
    private final URI uri;

    private final String userName;
    private final String password;

    // private final int initialPoolSize
    // private final int maxPoolSize
    // private final int minPoolSize
    // private final int maxIdleTime

    @Override public String getId() { return name.getValue(); }

    static DataSourcePlan fromJson(DataSourceName name, JsonNode node) {
        if (node.isNull())
            throw new Plan.PlanLoadingException("incomplete data-sources plan '" + name + "'");
        DataSourcePlanBuilder builder = builder().name(name);
        Plan.apply(node, "state", builder::state, DeploymentState::valueOf);
        Plan.apply(node, "uri", builder::uri, URI::create);
        Plan.apply(node, "jndi-name", builder::jndiName, identity(), "«java:/datasources/" + name + "DS»");
        Plan.apply(node, "driver", builder::driver, identity(), defaultDriver(builder.uri));

        Plan.apply(node, "user-name", builder::userName, identity());
        Plan.apply(node, "password", builder::password, identity());

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
        return "data-source:" + getState() + ":" + name + ":" + jndiName + ":" + driver + ":" + uri;
    }
}
