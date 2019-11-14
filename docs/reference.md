# Reference

## Bundle File Format

Bundles – including the `deployer.root.bundle` – are yaml files with the following sections:


### `deployables`

The key is the `name` of the deployable, i.e. a `war` named `foo` will be deployed as `foo.war`, so the base uri for a REST service is `https://<hostname>:<port>/<name>`. 

- `state`: Either `deployed` or `undeployed`. Defaults to 
the variable [`${name}.state`](#vars) or `deployed`.
- `group-id`: Defaults to the variable [`default.group-id`](#vars).
- `artifact-id`: Defaults to the name of the deployable.
- `classifier`: Defaults to none.
- `version`: Defaults to the variable [`${name}.version}`](#vars) or `CURRENT` (see below).
- `type`: `war` or `jar`. Defaults to [`default.deployable-type`](#vars) or `war`.
- `checksum`: The SHA-1 checksum of the artifact file. Optional to check for integrity.

Special `version` values:

| name | usage |
| --- | --- |
| `CURRENT` | The currently deployed version. Only for `deployables`, not for `bundles`. |
| `LATEST` | The numerically largest version in the repository that is not a `-SNAPSHOT`. |
| `UNSTABLE` | The numerically largest version in the repository, `-SNAPSHOT` or not. |


### `bundles`

The key is the `name` of the bundle. 

- `group-id`: Defaults to the variable [`default.group-id`](#vars).
- `artifact-id`: Defaults to the name of the deployable.
- `classifier`: Defaults to none.
- `version`: Mandatory.
- `instances`: The map of instance names to a map of variables passed into a `bundle`.
Defaults to a single empty-named entry with an empty map.


### `log-handlers`

The key is the `name` of the log handler. This is conventionally an upper case string.

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`.
- `level`: The [log level](#log-levels). Defaults to `ALL`.
- `type`: One of `console`, `periodic-rotating-file`, or `custom`.
Defaults to [`default.log-handler-type`](#vars) or `periodic-rotating-file`.
- `format`: The format used for log lines.
Defaults to using a [`default.log-formatter`](#vars) or [`default.log-format`](#vars) or `%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n`.
- `formatter`: Alternative to `format`: The name of a named formatter. Wildfly 10.0.0 defines: `PATTERN` and `COLOR-PATTERN`.

Only for `periodic-rotating-file`:
- `file`: The base name of the file to log to. Defaults to the lower case `name` of the log handler plus `.log`. 
- `suffix`: The date/time suffix used when rotating, defining the rotation frequency.
Defaults to [`default.log-file-suffix`](#vars) or `.yyyy-MM-dd`, i.e. daily rotation.

Only for `custom`:
- `module`: The JBoss module name. Mandatory.
- `class`: The fully qualified class name of the custom handler. Mandatory.
- `properties`: A map of parameters passed to the custom handler. Defaults to an empty map.


### `loggers`

The key is the `category` of the logger, usually the fully qualified name of the class producing the logs. 

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`.
- `level`: The [log level](#log-levels). Defaults to [`default.log-level`](#vars) or `«DEBUG»`.
- `handlers`: A list of log handler names. Defaults to an empty list.
- `handler`: Alternative syntax for a single log handler name. Defaults to an empty list.
- `use-parent-handlers`: Should the log handlers of the parent logger be used?
Defaults to `true` if the `handlers` are empty, or `false`, if there _are_ log handlers.


### `data-sources`

The key is the `pool-name` of the data source. 

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`.
- `uri`: The JNDI URI used to connect to the database. Mandatory.
- `jndi-name`: The name that can be used to look up the data source.
Defaults to `java:/datasources/<name>DS` where `<name>` is the name of the data source.
- `driver`: The `driver-name` to be used to connect.
Defaults to the part after `jdbc` in the `uri` or `default.data-source-driver`, if the URN isn't a `jdbc`.
- `user-name`: The user name to authenticate with. Default is vendor specific.
- `password`: The password to authenticate with. Default is vendor specific.
- `pool:min`: The minimum number of connections to keep in the connection pool. Default is vendor specific.
- `pool:initial`: The number of connections that should be opened at startup. Default is vendor specific.
- `pool:max`: The maximum number of connections to keep in the connection pool. Default is vendor specific.
- `pool:age`: The maximum age in some time unit that an unused connection is kept in the pool. Default is vendor specific.
Note that on JBoss this value is configured in minutes, so values below 60 seconds will be rounded to one minute.

Note that the `pool` settings are nested, i.e. they share one `pool` parent tag.

Supported time units:

- `milliseconds`, `millisecond`, `millis`, `milli`, `ms`
- `seconds`, `second`, `s`
- `minutes`, `minute`, `min`


### Variables

Bundles can refer to variables in the `${name}` notation. To escape a `$`, duplicate it to `$$`.

Variable values are:

- system properties,
- variables defined in the configuration [`vars`](#vars), and
- variables passed into bundles (see below).

The deployer fails, if a variable passed-in overwrites an existing variable (esp. system property),
or if the resulting plan has unresolved variables.

A variable name can contain the name of a function to be applied to the value:

- `«x»`: The literal string `x` – useful for, e.g., ` or ` expressions. This quote type (guillemet) has the advantage of being nestable.
- `toUpperCase(x)`: Turns the value of the variable `x` into all upper case.
- `toLowerCase(x)`: Turns the value of the variable `x` into all lower case.
- `toInitCap(x)`: Uppercase the first character and append the rest as is.
- `hostName()`: Returns the DNS name of the local host (without the domain name).
- `domainName()`: Returns the DNS domain of the local host.
- `regex(a, b)`: Apply the regular expression `b` to `a`, returning the first matching group.
- `decrypt(secret)`: Use a key from a keystore to decrypt a secret. See [key-store config](#key-store).
- `decrypt(secret, alias)`: Use a key with a specific alias from a keystore to decrypt a secret. See [key-store config](#key-store).
- `switch(x) body`: Use one of a map of string literals in the body, depending on the value of expression `x`.
For an example, see below.

You can chain variable expressions, by separating them with ` or `.
E.g. `toLowerCase(foo) or bar` will resolve to `baz`, if the variable `foo` is set to `BAZ`,
or fall back to `bar` if `foo` is not set.

The body of the `switch` function maps some labels to string literals, e.g.:

```yaml
password: "${switch(stage)
  dev: «A»
  qa: «B»
  prod: «C»
  }"
```


## Config

The deployer itself can be configured with a file `deployer.config.yaml`.

Restart The Deployer for changes to this file to be picked up.


### `repository`

Where and how to access the repository containing deployables (`war`, etc.) and bundles.

- `type`: `maven-central` or `artifactory`. Defaults to `artifactory`, if it's running on `localhost:8081`, or `maven-central` otherwise.
- `uri`: The base URI of the repository. For the defaults, see `type`.
- `username`: The credentials required by `artifactory`. Defaults to not using auth.
- `password`: The credentials required by `artifactory`. Defaults to not using auth.
- `repository-snapshots`: The name of the snapshot repository. Defaults to `snapshots-virtual`.
- `repository-releases`: The name of the release repository. Defaults to `releases-virtual`.


### `root-bundle`

What root bundle should be used when there is no `deployer.root.bundle`:

| key | value |
| --- | --- |
| name | The name of the root bundle loaded. Defaults to the DNS host name (without the domain or trailing digits). |
| group-id | The `group-id` of the bundle loaded. Defaults to the `default.group-id` or the DNS domain name. |
| classifier | The `classifier` of the bundle loaded. Defaults to null. |
| version | The `version` of the bundle loaded. Defaults to the `version` variable (or fails). |
| shutdown-after-boot | Shut the container down right after the root bundle has been applied. Useful when building, e.g., a Docker file. Defaults to `false`. |

These values can be used in expressions as `root-bundle:name`, etc.


### `manage`

This is a list of resource type names to be managed or `[all]`, i.e. resources of this kind that are deployed in the container, but are not in the plan, are removed (only exception: the Deployer itself).
Defaults to an empty list, i.e. things are left alone.


### `pin`

The list of resources to be skipped, i.e. they won't get removed when managed,
your plan will fail if it contains this resource, and they won't show up when reading the effective plan.

I.e., to pin a deployable `myapp`, use this:

```yaml
pin:
  deployables: [myapp]
```

### `triggers`

These triggers can cause The Deployer to run:

| name | description |
| --- | --- |
| startup | The initial run after the container was booted |
| post | A http POST coming in |
| fileChange | The `deployer.root.bundle` file was changed | 

By default all triggers are allowed, but you can limit the allowed triggers in the `deployer.config.yaml`.
E.g., to only allow the `startup` trigger, add:

```yaml
triggers: [startup]
```

If you specify an empty list, i.e. `triggers: []`, The Deployer runs in read-only mode.


### `vars`

This is a map of variables to set.

Special values:

| name | usage |
| --- | --- |
| default.group-id | `group-id` to be used, if none is specified. |
| default.deployable-type | `type` to be used for `deployables`, if none is specified. Defaults to `war`. |
| default.log-level | `level` to be used for `loggers`, if none is specified. This is _not_ used for `log-handlers`! The level of log handlers defaults to `ALL`. |
| default.log-handler-type | `type` to be used for `log-handlers`, if none is specified. Defaults to `periodicRotatingFile`. |
| default.log-format | `format` to be used for `log-handlers`, if none is specified. Defaults to `%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n`. |
| default.log-formatter | `formatter` to be used for `log-handlers`, if none is specified. Defaults to using the `default.log-format`. |
| default.log-encoding | `encoding` to be used for `log-handlers`, if none is specified. Defaults to system default. |
| default.log-file-suffix | `suffix` to be used for file `log-handlers`, if none is specified. Defaults to using the `yyyy-MM-dd` (i.e. daily rotation). |

### `key-store`

The key to be used for the `decrypt` expression.

| name | usage |
| --- | --- |
| path | The file path to the keystore. Mandatory to use `decrypt`. |
| type | The format of the keystore, e.g. `jks` or `jceks`. Defaults to `jks`. Note that you can't store secret keys (i.e. symmetric encryption keys) in `jks`. |
| pass | The password required for the keystore. Defaults to `changeit`, the JDK default. |
| alias | The default name of the key in the keystore. Can be overridden in the call to `decrypt`. Defaults to `secretkey`. |

To encrypt some key, you can use the `main` method in the `CipherFacade` class, e.g. to get the CLI help via Maven:

    mvn exec:java -Dexec.mainClass="com.github.t1.deployer.app.CipherFacade" -Dexec.args="--help"


## Miscellaneous


### Log Levels

The deployer uses the log levels of [slf4j](http://www.slf4j.org):
`ALL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`, and `OFF`.

The JBoss CLI additionally allows log levels from Java Util Logging and more,
so when reading these levels, The Deployer maps them as follows:

| jul | slf4j |
| --- | --- |
| ALL | ALL |
| FATAL | ERROR |
| SEVERE | ERROR |
| WARNING | WARN |
| INFO | INFO |
| CONFIG | INFO |
| FINE | DEBUG |
| FINER | DEBUG |
| FINEST | TRACE |
| OFF | OFF |
