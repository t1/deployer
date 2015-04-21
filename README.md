# Deployer

Lists the applications deployed in a JBoss 7 or 8 (for now) container with the version found in Artifactory (for now), and allows to deploy any version found in the repository.

Writing operations (deploy, redeploy, or undeploy) require basic auth with a JBoss management user; and they are written to a logger `com.github.t1.deployer.container.Audit` (as well as denied attempts to do so).

Also supports a REST API, just look at the URIs and request them with content type, e.g., `application/json`.

And there's a file `${jboss.server.config.dir}/deployments.properties` that (after the first (re/un/)deploy) contains a list of all deployed applications and their versions. Changing the version in this file will trigger a redeploy; deleting a line triggers an undeploy.

# TODO

* deploy new artifacts by searching in artifactory
* pre-deploy scanner
	* data source scanner/config
	* queue scanner/config
	* cdi-config scanner
* Proper authorization; after searching for 2 days I devised my own in 2 hours :(
* SSO
* cluster support
* Puppet support

# Configuration

## Artifactory

The Artifactory instance is by default expected to run on `http://localhost:8081/artifactory`. You can configure another instance with the system property `deployer.artifactory.uri`, or by creating a file `%{jboss.server.base.dir}/security/deployer.war/credentials.properties` with this property. If your Artifactory is configured to require authentication for read access, you'll also have to configure `deployer.artifactory.username` and `deployer.artifactory.password`.

## Audit Log

To redirect the audit log to a separate file, issue these JBoss CLI commands:

```
/subsystem=logging/periodic-rotating-file-handler=AUDIT:add(file={path=audit.log,relative-to=jboss.server.log.dir}, suffix=.yyyy-MM-dd, autoflush=true, formatter=%d{HH:mm:ss,SSS};%s%n)
/subsystem=logging/logger=com.github.t1.deployer.container.Audit:add(level=ALL,handlers=[AUDIT])
```
