# Deployer

[![Join the chat at https://gitter.im/t1/deployer](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/t1/deployer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Download](https://api.bintray.com/packages/t1/javaee-helpers/deployer/images/download.svg)](https://bintray.com/t1/javaee-helpers/deployer/_latestVersion)

Simple [Infrastructure As Code](http://martinfowler.com/bliki/InfrastructureAsCode.html) solution for Java EE containers (currently only JBoss 7+) pulling from a maven repository (currently full support only for Maven Central and Artifactory, as we need to be able to search by checksum).

## 1-Minute-Tutorial

- Create a file `$JBOSS_CONFIG_DIR/deployer.root.bundle` containing:

```yaml
artifacts:
  jolokia:
    groupId: org.jolokia
    artifact-id: jolokia-war
    version: 1.3.2
```

- Deploy the `deployer.war` to your container.
On startup, it will find your file, pull jolokia from maven central, and deploy it to the container.
If there is already a different version of jolokia deployed, it will replace it.

- Change the file to version `1.3.3` and the deployer will pick up the change and upgrade jolokia.

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
- `file`: The base name of the file to log to. Defaults to the `name` of the log handler. 
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

### `vars`

This is a map of system properties to set.

### `repository`

- `type`: `maven-central` or `artifactory`. Defaults to `artifactory`, if it's running on `localhost:8081`, or `maven-central` otherwise.
- `uri`: The base URI of the repository. For the defaults, see `type`.
- `username`: The credentials required by `artifactory`. Defaults to not using auth.
- `password`: The credentials required by `artifactory`. Defaults to not using auth.
- `repository-snapshots`: The name of the snapshot repository. Defaults to `snapshots-virtual`.
- `repository-releases`: The name of the release repository. Defaults to `releases-virtual`.

### `manage`

This is a list of resource type names (currently only `artifacts`) to be managed,
i.e. resources of this kind existing in the container, but are not in the plan, are removed.
Defaults to an empty list, i.e. things are left alone.


## History

Version 1.0.0 (which was never released) provided a rest api and html gui to manage deployments by remote requests or manually on each node.
As this wasn't secure / didn't scale for many instances and stages, 2.0 was initiated.
