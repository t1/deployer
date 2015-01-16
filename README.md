# Deployer

Lists the applications deployed in a JBoss 7 (for now) container with the version found in Artifactory, and allows to deploy any version found in Artifactory.

Also supports a REST API.

# TODO

* SSO
* cluster support
* Puppet support

# Configuration

## Security

```
/subsystem=security/security-domain=deployer:add()
/subsystem=security/security-domain=deployer:add(cache-type=default)

```
