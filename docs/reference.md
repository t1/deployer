# Reference

## Bundle File Format

Bundles – including the `deployer.root.bundle` – are yaml files with the following sections:


### `deployables`

The key is the `name` of the deployment, i.e. a `war` named `foo` will be deployed as `foo.war`,
so the base uri for a REST service is `https://<hostname>:<port>/<name>`. 

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`. See [`manage`](#manage).
- `group-id`: Defaults to the variable [`default.group-id`](#vars).
- `artifact-id`: Defaults to the name of the deployment.
- `version`: Defaults to `CURRENT`, which is the currently deployed version of this artifact.
- `type`: `war` or `jar`. Defaults to [`default.deployable-type`](#vars) or `war`.
- `checksum`: The SHA-1 checksum of the artifact file. Optional to check for integrity.


### `bundles`

The key is the `name` of the bundle. 

- `group-id`: Defaults to the variable [`default.group-id`](#vars).
- `artifact-id`: Defaults to the name of the deployment.
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


### Variables

Bundles can refer to variables in the `${name}` notation. To escape a `$`, duplicate it to `$$`.

Variable values are:

- system properties,
- variables defined in the configuration [`vars`](#vars), and
- variables passed into bundles (see below).

The deployer fails, if a variable passed-in overwrites an existing variable (esp. system property),
or if the resulting plan has unresolved variables.

A variable name can contain the name of a function to be applied to the value:

- `«x»`: The literal string `x` – useful for, e.g., ` or ` expressions. This quote type has the advantage of being nestable.
- `toUpperCase(x)`: Turns the value of the variable `x` into all upper case.
- `toLowerCase(x)`: Turns the value of the variable `x` into all lower case.
- `hostName()`: Returns the DNS name of the local host (without the domain name).
- `domainName()`: Returns the DNS domain of the local host.
- `regex(a, b)`: Apply the regular expression `b` to `a`, returning the first matching group.

You can chain variable expressions, by separating them with ` or `.
E.g. `toLowerCase(foo) or bar` will resolve to `baz`, if the variable `foo` is set to `BAZ`,
or fall back to `bar` if `foo` is not set.


### Default Root Bundle

If there is no file `deployer.root.bundle`, the following default applies:

```yaml
bundles:
  ${regex(root-bundle-name or hostName(), bundle-to-host-name or «(.*?)\d*»)}:
    group-id: ${root-bundle-group or default.group-id or domainName()}
    version: ${version}
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


### `vars`

This is a map of variables to set.

Special values:

| name | usage |
| --- | --- |
| default.group-id | `group-id` to be used, if none is specified. |
| default.deployable-type | `type` to be used for `deployables`, if none is specified. Defaults to `war`. |
| default.log-level | `level` to be used for `loggers` (not `log-handlers`!), if none is specified. |
| default.log-handler-type | `type` to be used for `log-handlers`, if none is specified. Defaults to `periodicRotatingFile`. |
| default.log-format | `format` to be used for `log-handlers`, if none is specified. Defaults to `%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n`. |
| default.log-formatter | `formatter` to be used for `log-handlers`, if none is specified. Defaults to using the `default.log-format`. |
| default.log-file-suffix | `suffix` to be used for file `log-handlers`, if none is specified. Defaults to using the `yyyy-MM-dd` (i.e. daily rotation). |
| root-bundle-name | The name of the bundle loaded, if no `deployer.root.bundle` file exists. Defaults to the DNS host name (without the domain). |
| root-bundle-group | The `group-id` of the bundle loaded, if no `deployer.root.bundle` file exists and `default.group-id` is not set. Defaults to the DNS domain name. |


### `manage`

This is a list of resource type names (currently only `deployables`) to be managed,
i.e. resources of this kind existing in the container, but are not in the plan, are removed.
Defaults to an empty list, i.e. things are left alone.


## Miscellaneous


### Log Levels

The deployer uses the log levels of [slf4j](http://www.slf4j.org):
`ALL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`, and `OFF`.

The JBoss CLI additionally allows log levels from Java Util Logging,
so when reading these levels, The Deployer maps them as follows:

| jul | slf4j |
| --- | --- |
| ALL | ALL |
| SEVERE | ERROR |
| WARNING | WARN |
| INFO | INFO |
| CONFIG | INFO |
| FINE | DEBUG |
| FINER | DEBUG |
| FINEST | TRACE |
| OFF | OFF |
