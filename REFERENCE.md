# Reference

## Bundle File Format

Bundles – including the `deployer.root.bundle` – are yaml files with the following sections:

### `artifacts`

The key is the `name` of the deployment, i.e. a `war` named `foo` will be deployed as `foo.war`,
so the base uri for a REST service is `https://<hostname>:<port>/<name>`. 

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`. See [`manage`](#manage).
- `group-id`: Defaults to the system property `default.group-id` (see below).
- `artifact-id`: Defaults to the name of the deployment.
- `version`: Mandatory.
- `type`: `war`, `jar`, or `bundle`. Defaults to `war`.
- `vars`: The map of variables passed into a `bundle`; forbidden for other types. Defaults to an empty map.

### `log-handlers`

The key is the `name` of the log handler. This is conventionally an upper case string.

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`.
- `level`: The log level; one of `ALL`, `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF`. Defaults to `ALL`.
- `type`: One of `console`, `periodic-rotating-file`, or `custom`. Defaults to `periodic-rotating-file`.
- `format`: The format used for log lines. Defaults to `%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n`.
- `formatter`: Alternative to `format`: The name of a named formatter. Wildfly 10.0.0 defines: `PATTERN` and `COLOR-PATTERN`.

Only for `periodic-rotating-file`:
- `file`: The base name of the file to log to. Defaults to the lower case `name` of the log handler plus `.log`. 
- `suffix`: The date/time suffix used when rotating, defining the rotation frequency. Defaults to `.yyyy-MM-dd`, i.e. daily rotation.

Only for `custom`:
- `module`: The JBoss module name. Mandatory.
- `class`: The fully qualified class name of the custom handler. Mandatory.
- `properties`: A map of parameters passed to the custom handler. Defaults to an empty map.

### `loggers`

The key is the `category` of the logger, usually the fully qualified name of the class producing the logs. 

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`.
- `level`: The log level; one of `ALL`, `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF`. Defaults to `ALL`.
- `handlers`: A list of log handler names. Defaults to an empty list.
- `handler`: Alternative syntax for a single log handler name. Defaults to an empty list.
- `use-parent-handlers`: Should the log handlers of the parent logger be used?
Defaults to `true` if the `handlers` are empty, or `false`, if there _are_ log handlers.

### Variables

Bundles can refer to variables in the `${name}` notation. To escape a `$`, simply duplicate `$$`.

Variable values are:

- system properties,
- variables defined in the configuration [`vars`](#vars), and
- variables passed into bundles (see below).

Config variables are written into system properties. The deployer fails, if a variable passed in overwrites an existing
variable (esp. system property), or if the resulting plan has unresolved variables.

A variable name can contain the name of a function to be applied to the value:

- `toUpperCase`: Turns the value into all upper case.
- `toLowerCase`: Turns the value into all lower case.


## Config

The deployer itself can be configured with a file `deployer.config.yaml`.

### `repository`

Where and how to access the repository containing artifacts (`war`, `bundle`, etc.).

- `type`: `maven-central` or `artifactory`. Defaults to `artifactory`, if it's running on `localhost:8081`, or `maven-central` otherwise.
- `uri`: The base URI of the repository. For the defaults, see `type`.
- `username`: The credentials required by `artifactory`. Defaults to not using auth.
- `password`: The credentials required by `artifactory`. Defaults to not using auth.
- `repository-snapshots`: The name of the snapshot repository. Defaults to `snapshots-virtual`.
- `repository-releases`: The name of the release repository. Defaults to `releases-virtual`.

### `vars`

This is a map of system properties to set.

### `manage`

This is a list of resource type names (currently only `artifacts`) to be managed,
i.e. resources of this kind existing in the container, but are not in the plan, are removed.
Defaults to an empty list, i.e. things are left alone.

## Miscellaneous

### Log Levels

The deployer uses the log levels of [slf4j](http://www.slf4j.org).
The JBoss CLI additionally allows log levels from Java Util Logging,
so when reading these levels, the Deployer maps them as follows:

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
