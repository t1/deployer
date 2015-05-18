package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import java.net.URI;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * @see javax.annotation.sql.DataSourceDefinition
 */
@Data
@Builder
@AllArgsConstructor(access = PRIVATE)
@Accessors(chain = true)
public class DataSourceConfig implements Comparable<DataSourceConfig> {
    public static final String NEW_DATA_SOURCE = "!";

    @NonNull
    String name;

    String driver;

    String jndiName;

    /**
     * DataSource implementation class name which implements: <code>javax.sql.DataSource</code> or
     * <code>javax.sql.XADataSource</code> or <code>javax.sql.ConnectionPoolDataSource</code>.
     */
    // String className();

    URI uri;

    String user;

    String password;

    /**
     * Isolation level for connections. The Isolation level must be one of the following:
     * <p>
     * <ul>
     * <li>Connection.TRANSACTION_NONE,
     * <li>Connection.TRANSACTION_READ_ UNCOMMITTED,
     * <li>Connection.TRANSACTION_READ_COMMITTED,
     * <li>Connection.TRANSACTION_REPEATABLE_READ,
     * <li>Connection.TRANSACTION_SERIALIZABLE
     * </ul>
     * <p>
     * Default is vendor-specific.
     */
    // int isolationLevel() default -1;

    /**
     * Set to <code>false</code> if connections should not participate in transactions.
     * <p>
     * Default is to enlist in a transaction when one is active or becomes active.
     */
    // boolean transactional() default true;

    /**
     * Number of connections that should be created when a connection pool is initialized.
     * <p>
     * Default is vendor-specific
     */
    // int initialPoolSize() default -1;

    /**
     * Maximum number of connections that should be concurrently allocated for a connection pool.
     * <p>
     * Default is vendor-specific.
     */
    // int maxPoolSize() default -1;

    /**
     * Minimum number of connections that should be allocated for a connection pool.
     * <p>
     * Default is vendor-specific.
     */
    // int minPoolSize() default -1;

    /**
     * The number of seconds that a physical connection should remain unused in the pool before the connection is closed
     * for a connection pool.
     * <p>
     * Default is vendor-specific
     */
    // int maxIdleTime() default -1;

    /**
     * The total number of statements that a connection pool should keep open. A value of 0 indicates that the caching
     * of statements is disabled for a connection pool.
     * <p>
     * Default is vendor-specific
     */
    // int maxStatements() default -1;

    /**
     * Used to specify Vendor specific properties and less commonly used <code>DataSource</code> properties such as:
     * <p>
     * <ul>
     * <li>dataSourceName
     * <li>networkProtocol
     * <li>propertyCycle
     * <li>roleName
     * </ul>
     * <p>
     * Properties are specified using the format: <i>propertyName=propertyValue</i> with one property per array element.
     * <p>
     * If a DataSource property is specified in the properties element and the annotation element for the property is
     * also specified, the annotation element value takes precedence.
     */
    // String[] properties() default {};

    /**
     * Sets the maximum time in seconds that this data source will wait while attempting to connect to a database. A
     * value of zero specifies that the timeout is the default system timeout if there is one; otherwise, it specifies
     * that there is no timeout.
     * <p>
     * Default is vendor-specific.
     */
    // int loginTimeout() default 0;

    @Override
    public int compareTo(DataSourceConfig that) {
        return this.name.compareToIgnoreCase(that.name);
    }

    public boolean isNew() {
        return NEW_DATA_SOURCE.equals(name);
    }
}
