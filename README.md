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

## Reference

### Deployments

The key is the `name` of the deployment, i.e. a `war` named `foo` will be deployed as `foo.war`,
so the base uri for a REST service is `https://<hostname>:<port>/<name>`. 

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`. See `managed`.
- `group-id`: Defaults to the system property `default.group-id` (see below).
- `artifact-id`: Defaults to the name of the deployment.
- `version`: Mandatory.
- `type`: `war`, `jar`, or `bundle`. Defautls to `war`.
- `var`: The map of variables passed into a `bundle`. Forbidden for other types. Defaults to empty map.

### LogHandlers

The key is the `name` of the log handler. This is conventionally an upper case string.

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`. See `managed`.
- `level`: The log level; one of `ALL`, `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF`. Defaults to `ALL`.
- `type`: One of `console` or `periodic-rotating-file`. Defaults to `periodic-rotating-file`.
- `format`: The format used for log lines. Defaults to `%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n`.
- `formatter`: The name of a named formatter. Wildfly 10.0.0 defines: `PATTERN` and `COLOR-PATTERN`.

Only for `periodic-rotating-file`:
- `file`: The base name of the file to log to. Defaults to the `name` of the log handler. 
- `suffix`: The date/time suffix used when rotating, defining the rotation frequency. Defaults to `.yyyy-MM-dd`, i.e. daily rotation.


### Loggers

The key is the `category` of the logger, usually the fully qualified name of the class producing the logs. 

- `state`: Either `deployed` or `undeployed`. Defaults to `deployed`. See `managed`.
- `level`: The log level; one of `ALL`, `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF`. Defaults to `ALL`.
- `handlers`: A list of log handler names. Defaults to the empty list.
- `handler`: Alternative syntax for a single log handler name. Defaults to the empty list.
- `use-parent-handlers`: Should the log handlers of the parent logger be used?
Defaults to `true` if the `handlers` are empty, or `false`, if there _are_ log handlers.

## Config

The deployer itself can be configured with a file `deployer.config.yaml`.

### `vars`

This is a map of system properties to set.

### `repository`

* `type`
* `uri`
* `username`
* `password`
* `repositorySnapshots`
* `repositoryReleases`


## History

Version 1.0.0 (which was never released) provided a rest api and html gui to manage deployments manually on each node.
As this didn't scale for many instances and stages, 2.0 was initiated.
