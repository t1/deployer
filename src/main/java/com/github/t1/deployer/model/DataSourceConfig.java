package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import java.net.URI;

import lombok.*;
import lombok.experimental.*;

/**
 * The model we use is aligned but not equal to the javax.annotation.sql.DataSourceDefinition; the JBoss CLI data-source
 * structure is quite different.
 */
@Data
@Builder
@AllArgsConstructor(access = PRIVATE)
@Accessors(chain = true)
public class DataSourceConfig implements Comparable<DataSourceConfig> {
    public static final String NEW_DATA_SOURCE = "!";

    @NonNull
    String name;
    URI uri;

    @Override
    public int compareTo(DataSourceConfig that) {
        return this.name.compareToIgnoreCase(that.name);
    }
}
