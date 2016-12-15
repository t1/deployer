# The Deployer [![Join the chat at https://gitter.im/t1/deployer](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/t1/deployer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Download](https://api.bintray.com/packages/t1/javaee-helpers/deployer/images/download.svg)](https://bintray.com/t1/javaee-helpers/deployer/_latestVersion)

Simple [Infrastructure As Code](http://martinfowler.com/bliki/InfrastructureAsCode.html) solution
for Java EE containers (currently only JBoss 7+) pulling from a maven repository
(currently full support only for Maven Central and Artifactory Pro, as we need to be able to search by checksum).


## Motivation

There are very good IaC solutions capable of deploying applications to and configuring resources in JEE containers.
There's good support for Wildfly in [Ansible](http://docs.ansible.com/ansible/jboss_module.html),
[Salt](https://docs.saltstack.com/en/latest/ref/states/all/salt.states.jboss7.html), and
[Puppet](https://forge.puppet.com/biemond/wildfly) (to name just a few).
They are very powerful and can do much more than just this; e.g., they can install the JBoss itself.
And that's exactly the problem that I had: Great power comes with great responsibility...
too much responsibility for me as a simple developer and operator of JEE applications,
working in an environment where the infrastructure and platform is managed by dedicated operations teams,
using those other IaC tools.

As a JEE developer, it also takes some time and dedication to learn those tools... and even more so to add to them.
I'd rather have a very limited tool, that's easy to understand and does only what I want it to do...
living in the environment I know best: JEE.

And this simplicity also brings some security, I suppose.
A central instance that can mess with any aspect of my system is a real honey pot to any attacker.

OTOH, The Deployer tries to keep away from container specifics, so the configuration files you'll write should run
on any JEE container The Deployer supports. I must admit that this is rather academic at the moment,
given that only JBoss 7+ is currently supported, but it may prove worth trying to keep adaptability in mind.

Configuration quickly grows unwieldy. So it's all about reducing it to the essential, driving repetition out.


## 1-Minute-Tutorial

- Create a file `$JBOSS_CONFIG_DIR/deployer.root.bundle` containing:

```yaml
deployments:
  jolokia:
    groupId: org.jolokia
    artifact-id: jolokia-war
    version: 1.3.2
```

- Deploy the `deployer.war` to your container.
On startup, it will find your file, pull jolokia from maven central, and deploy it to the container.

- Change the file to version `1.3.3` and the deployer will pick up the change and upgrade jolokia.

## Documentation

You can find an introduction to the concepts and best practices in the [tutorial](docs/tutorial.md).

For a comprehensive list of all details, see the [reference](docs/reference.md).

More questions? Take a look at the [FAQ](docs/faq.md).

## Docker

At first sight, it may look like The Deployer would not have any role to play when you use [Docker](https://www.docker.com),
as in a fully containerized approach, there's no need to change a running system:
changes in the configuration should result in a rebuilds and restarts of the whole stack
and it's sufficiently easy to use the normal CLI for that.

But you may find the abstraction layer that The Deployer provides to be useful nonetheless:
A bundle file may be more readable than a long list of CLI statements,
as it is more concise and clear (e.g., xa and non-xa data sources both use the same connection uri syntax),
and it provides mechanics to reuse common configuration schemes, further DRYing your code.

You won't want to download and apply everything at boot time,
as this happens very often in a dynamic cloud setup, so it has to be as fast as possible.
To apply The Deployer root bundle at Docker build time, you can start the container from your `Dockerfile`
with the `shutdown-after-boot` option in your `deployer.config.yaml`.
For an example, see `src/main/docker/Dockerfile`; to run it do:

    cp ../../../target/deployer.war .
    docker build --tag=wildfly .
    docker run -it --rm -p 8080:8080 --name wildfly wildfly

To use it for your own applications, you'd only need to change the `deployer.root.bundle`,
and maybe configure your `repository` in the `deployer.config.yaml`.


## Building

Just run `mvn clean install`.

If you don't have an Artifactory Pro on your local machine, but still want to test deployments of your own applications,
you can run the `ArtifactoryMockLauncher` and run `index` in its CLI.
This will build an index of every `war`, `ear`, `bundle`, and `postgresql`-`jar` in your
local maven repository (`~/.m2`) and serve files from there.


## Release Notes

See the [closed milestones](https://github.com/t1/deployer/milestones?state=closed)

We use [Semantic Versioning](http://semver.org).


## Plans

See the open [milestones](https://github.com/t1/deployer/milestones) and [issues](https://github.com/t1/deployer/issues).


## Contribute

You can [join the chat](https://gitter.im/t1/deployer) or feel free to simply open issues.
Pull requests are also welcome.

License: [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
