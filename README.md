# Deployer

[![Join the chat at https://gitter.im/t1/deployer](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/t1/deployer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Lists the applications deployed in a JBoss 7+ (for now) container with the version found in Artifactory (for now), and allows to deploy any version found in the repository.

Writing operations (deploy, redeploy, or undeploy) require basic authenticated user with the `deployer` role and are written to a logger `com.github.t1.deployer.container.Audit` (as well as denied attempts to do so).

Also supports a REST API, just look at the URIs and request them with content type, e.g., `application/json`.

And there's a file `${jboss.server.config.dir}/deployments.properties` that contains a list of all deployed applications and their versions. Changing the version in this file will trigger a redeploy; deleting a line can trigger an undeploy (if it's so configured).

# Configuration

## Artifactory

The Artifactory instance is by default expected to run on `http://localhost:8081/artifactory` and require no authentication for read access.

TODO

You can configure another instance with the system property `deployer.artifactory.uri`, or by creating a file `%{jboss.server.base.dir}/security/deployer.war/credentials.properties` with this property.

If your Artifactory is configured to require authentication for read access, you'll also have to configure `deployer.artifactory.username` and `deployer.artifactory.password`.

## Container

TODO

## Auto-Undeploy

TODO

## Audit Log

To redirect the audit log to a separate file, issue these JBoss CLI commands:

```
/subsystem=logging/periodic-rotating-file-handler=AUDIT:add(file={path=audit.log,relative-to=jboss.server.log.dir}, suffix=.yyyy-MM-dd, autoflush=true, formatter=%d{HH:mm:ss,SSS};%s%n)
/subsystem=logging/logger=com.github.t1.deployer.container.Audit:add(level=ALL,handlers=[AUDIT])
```


## Security

### default

By default, you can simply configure an application user with the `bin/add-user.sh` command in your JBoss. The realm is `ApplicationRealm` and the user must be added to the `deployer` group.

### management

If you want to use management users instead, you'll have to:

1. configure a security-domain:

	```
	/subsystem=security/security-domain=deployer:add(cache-type=default)
	/subsystem=security/security-domain=deployer/authentication=classic:add()
	/subsystem=security/security-domain=deployer/authentication=classic/login-module=local:add(code="RealmUsersRoles", 	flag=required, module-options={"usersProperties" => "${jboss.server.config.dir}/mgmt-users.properties", "rolesProperties" => "${jboss.server.config.dir}/mgmt-roles.properties", "realm" => "ManagementRealm"}
	```

1. create an overlay for a `jboss-web.xml` that includes:

	```
	<?xml version="1.0" encoding="UTF-8"?>
	<jboss-web xmlns="http://www.jboss.org/j2ee/schema/jboss-web_7_2.xsd">
	    <security-domain>deployer</security-domain>
	</jboss-web>
	```

### jboss sso

If you need a security-domain `spp` instead of the default `other`, create a file `jboss-web.xml`:

	<?xml version="1.0" encoding="UTF-8"?>
	<jboss-web xmlns="http://www.jboss.org/j2ee/schema/jboss-web_7_2.xsd">
	   <security-domain>spp</security-domain>
	</jboss-web>

Create a custom `web.xml`:

	<?xml version="1.0" encoding="UTF-8"?>
	<web-app version="3.0" xmlns="http://java.sun.com/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	    xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd">
	    <servlet>
	        <servlet-name>javax.ws.rs.core.Application</servlet-name>
	    </servlet>
	    <servlet-mapping>
	        <servlet-name>javax.ws.rs.core.Application</servlet-name>
	        <url-pattern>/*</url-pattern>
	    </servlet-mapping>

	    <context-param>
	        <param-name>resteasy.scan</param-name>
	        <param-value>true</param-value>
	    </context-param>

	    <context-param>
	        <!-- prevent XXE attacks: -->
	        <param-name>resteasy.document.expand.entity.references</param-name>
	        <param-value>false</param-value>
	    </context-param>

	    <security-constraint>
	        <web-resource-collection>
	            <web-resource-name>protected-resource</web-resource-name>
	            <url-pattern>/*</url-pattern>
	        </web-resource-collection>
	        <auth-constraint>
	            <role-name>*</role-name>
	        </auth-constraint>
	    </security-constraint>

	    <login-config>
	        <auth-method>SSO</auth-method>
	    </login-config>

	    <security-role>
	        <role-name>*</role-name>
	    </security-role>
	 </web-app>

Finally execute this cli:

	deployment-overlay add --name=deployer-sso --content=/WEB-INF/web.xml=web.xml,/WEB-INF/jboss-web.xml=jboss-web.xml --deployments=deployer.war --redeploy-affected
