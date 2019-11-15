# Frequently Asked Questions

## Design Decisions

### Why Is There No `if`?

There's a `switch`, so you can set, e.g., stage specific values. But you can't switch anything entirely on or off, and that's on purpose: The environment of your applications should be as similar as possible on all stages, so the tests you do won't lie to you. Even the `switch` should has a big `handle with care` label to it.


### Why the system property `com.github.t1.deployer.container.CLI#DEBUG`?

CLI statements may contain credentials or other sensitive data that normally should not be logged, but it's essential to see them while developing The Deployer. The normal log level mechanism is security wise not sufficient to hide such information, as manipulating a log level may be too easy. So there's an extra flag to enable logging the CLI statements.

### Does it make sense to use The Deployer for Docker images?

At first sight, it may look like The Deployer would not have any role to play when you use [Docker](https://www.docker.com), as in a fully containerized approach, there's no need to change a running system: changes in the configuration should result in a rebuild and restart of the whole stack and it's sufficiently easy to use the normal CLI for that.

But you may find the abstraction layer that The Deployer provides to be useful nonetheless: A bundle file may be more readable than a long list of CLI statements, as it is more concise and clear (e.g., xa and non-xa data sources both use the same connection uri syntax), and it provides mechanics to reuse common configuration schemes, further DRYing your infrastructure code.

You won't want to download and apply everything at boot time, as this happens very often in a dynamic cloud setup, so it has to be as fast as possible. To apply The Deployer root bundle at Docker build time, you can start the container from your `Dockerfile` with the `shutdown-after-boot` option in your `deployer.config.yaml`. For an example, see `src/main/docker/Dockerfile`; to run it do:

    cp ../../../target/deployer.war .
    docker build --tag=wildfly .
    docker run -it --rm -p 8080:8080 --name wildfly wildfly

To use it for your own applications, you'd only need to change the `deployer.root.bundle`, and maybe configure your `repository` in the `deployer.config.yaml`.

Starting up a container when building an image is not very fast, so to speed things up, we'd like to implement an [offline mode](https://github.com/t1/deployer/issues/59).
