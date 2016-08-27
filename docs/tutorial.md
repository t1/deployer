# Tutorial

After you've read the 1-Minute-Tutorial in the README, you can already nicely install and update deployables.
But the deployer can do more, and this tutorial will show you how to configure resources,
how to combine them into bundles, and how to integrate it all into a CI or even CD pipeline.

'Resources' are all the things that the deployer can change, e.g. deployables (`war` files, etc.) as well as loggers, etc.

## GET config

Before we start, you can also _read_ the currently effective configuration by simply GETting `http://localhost:8080/deployer`.
This resource provides a concise overview of the relevant configuration without providing confidential data like passwords
(it may still not be wise to make this information freely available in the internet).

You can GET this data as `application/yaml`, `application/json`, or `text/html`,
i.e. you can view it as a simple table with your browser.
Note that this is very simplistic and some information is missing.

(currently `application/xml` doesn't work... but it seems out-of-fashion anyway ;-)

## Undeploy & Manage

Every resource has an implicit parameter `state` set to `deployed`.
You can set it explicitly to `undeployed` to have that resource removed.
This is how to get rid again of, e.g., an application.

But how long are you going to keep those undeployed resources in your plans?
When is it safe to remove that cruft?
How do you make sure that a freshly set up node looks exactly as one running for years?
Manually keeping track of this all is not very infrastructure-as-code-ish.

You can instead tell the deployer to [manage](reference.md#manage) a specific type of resource.
This makes the deployer _remove_ all resources that are _not_ listed.

To do so, add the following snippet to a file `deployer.config.yaml`:

```yaml
managed:
- deployables
```

See the section [config](reference.md#config) in the reference for details.

## Configuring Resources

The deployer can be used to configure more than just deployables.
Here, we'll only scratch on the subject and just configure simple loggers and log-handlers, to build on that and combine things.
For the full details, please refer to the [reference](reference.md).

If you have several applications running in one container,
you'll want to have the logs for every app in a separate file and not everything clumped into the `server.log`.
To define a log-handler for `myapp`, simply add this to your `deployer.root.bundle`:

```yaml
log-handlers:
  MYAPP:
    file: myapp.log
```

To define a logger (a.k.a. category) for `com.mycompany.myapp` to log to this file, simply add this:

```yaml
loggers:
  com.mycompany.myapp
    level: DEBUG
    handler: MYAPP
```

## Bundles

Now that you know about the building blocks, you should start to actually use them.
Do it for maybe two or three deployables and their loggers, then come back.
Yeah: Take a break from reading this tutorial. Do it for real. Now!

Welcome back! You now have a `deployer.root.bundle` that looks somewhat similar to this:

```yaml
log-handlers:
  MYAPP1:
    file: myapp1.log
  MYAPP2:
    file: myapp2.log
loggers:
  mygroup.myapp1:
    level: DEBUG
    handler: MYAPP1
  mygroup.myapp2:
    level: DEBUG
    handler: MYAPP2
deployables:
  myapp1:
    groupId: mygroup
    version: 1.0
  myapp2:
    groupId: mygroup
    version: 2.0
```

Note that you don't have to specify an `artifact-id` if it's the same as the `name` of the `deployable`.

Also note that the `version` can be a `-SNAPSHOT` version;
The Deployer will resolve it to the latest of those snapshots in the repository.

Even with only two apps, you'll notice that it's not easy to find all the things belonging to one app.
They are spread over all the resource types.
And this will be getting worse as you add more and more apps and more and more resource types.
The Deployer was designed to keep the resource types together, not the applications they belong to.

To allow for an app centric view, the deployer allows you to group things into so called bundles.
You already know how bundles look: The `deployer.root.bundle` is one.
So let's define one bundle for each app above:

### `myapp1.bundle`

```yaml
log-handlers:
  MYAPP1:
    file: myapp1.log
loggers:
  mygroup.myapp1:
    level: DEBUG
    handler: MYAPP1
deployables:
  myapp1:
    groupId: mygroup
    version: 1.0
```

### `myapp.bundle`

```yaml
log-handlers:
  MYAPP2:
    file: myapp2.log
loggers:
  mygroup.myapp2:
    level: DEBUG
    handler: MYAPP2
deployables:
  myapp2:
    groupId: mygroup
    version: 2.0
```

We will include them in the root bundle, after we've published them to our Maven repository:

### `deployer.root.bundle`

```yaml
bundles:
  myapp1:
    groupId: mygroup
    version: 1.0
  myapp2:
    groupId: mygroup
    version: 2.0
```

Note that:

- The `version` of the bundles is "coincidentally" the `version` of the applications within.
- The `group-id` of the bundles is "coincidentally" the `group-id` of the applications within.

Now let's get these bundles into a repository:

## Packaging Bundles

This step is actually completely outside of The Deployer itself, but it's an important step.
There are many ways to do it; here we build the bundles with the `build-helper-maven-plugin`:

- Create your project directory
- Create a sub folder `src/main/resrouces`
- Put your `myapp1.bundle` file in there.
- And finally add a `pom.xml` like this:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>mygroup</groupId>
    <artifactId>myapp1.bundle</artifactId>
    <version>1.0</version>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>

    <build>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <version>1.12</version>
                <executions>
                    <execution>
                        <id>attach-artifacts</id>
                        <phase>package</phase>
                        <goals>
                            <goal>attach-artifact</goal>
                        </goals>
                        <configuration>
                            <artifacts>
                                <artifact>
                                    <file>target/classes/${project.artifactId}</file>
                                    <type>bundle</type>
                                </artifact>
                            </artifacts>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

You may also need a `distribution-management` section. Then you can `mvn clean deploy` your bundle.

After you did this for `myapp1` and `myapp2`, you can make the change to the `deployer.root.bundle` described above.
Your apps should now get deployed and the logging configured.

## Variables

Nice and well, but there's no much use to it all, if you still have to copy the `deployer.root.bundle` to all of your machines by hand!
And every time you have to update a version, you'd have to repeat that process. Suboptimal.
We'd like to be able to update an application from the _outside_, e.g. from a Jenkins job.

To do that, simply replace the `version` of `myapp1` by `${myapp1.version}`.
Then, using [httpie](http://httpie.org/), you can do:

http --json POST :8080/deployer myapp1.version=1.0

... or (more wordy) using CURL:

curl -X POST -H "Accept: application/json" -H "Content-Type: application/json" --data '{"myapp1.version":"1.0"}' http://localhost:8080/deployer

You can use variables for many things, but this is the most common use case.

But we have _two_ artifacts. How should the build job of `myapp1` know about the version of `myapp2`?
Why should it care about the existence of `myapp2` at all?

Easy: Just don't. The default `version` (i.e. the effective `version` for an deployable already deployed) is `CURRENT`,
which resolves to the version currently installed.
So while you can't use this for the initial deployment, you _can_ use it to update one deployable, while all others remain the same.
Note that this doesn't work for bundles, as they are not actually deployed in the container, so there's not CURRENT version for them.


## Default Group-Id

One thing you'll be repeating all over the place, is the `group-id`.
Put this section into the `deployer.config.yaml`, instead:

```yaml
vars:
  default.group-id: mygroup
```

Then can simply remove this `group-id` throughout your configuration files.

If you find that you often repeat other values, you can also add them to the `vars` section
and use them as `${myvar}`. Note that this is especially useful if a value may change some time.


## Default Root Bundle

If there is no file `deployer.root.bundle`, a [default root bundle](reference.md#default-root-bundle) applies.
This is how it works:

Say you have a host `myhost.mydomain.org`.
Create a bundle artifact with `artifact-id` = `myhost` and `group-id` = `mydomain.org`
and pass the `version` to the POST. Done!
This works even if your host names end with digits, which is a common pattern for node names in a cluster,
i.e. `myhost01.mydomain.org` is mapped to a bundle name `myhost`, as trailing digits are stripped.

As domain names are often very generic, like `local` or `server.lan`,
it's generally better to use the [`default.group-id`](#default-group-id).
If you already need a different `default.group-id`, you can define a `root-bundle-name` variable.

If your host names contain stage prefixes or suffixes like `dev`, or `qa`, you can strip them, too,
by setting a variable `bundle-to-host-name` to `(.*?)(dev|qa)?\d*` for suffixes or `(?:dev|qa)?(.*?)\d*` for prefixes.
The first capturing group of the expression is used, so remember to mark ay leading groups as non-capturing
by using `(?:X)`, as shown for the prefix.

If your host names are very technical and/or change very often,
you may be better off to configure an explicit `root-bundle-name` variable.


## Application Schema Bundles

Now that you know all the parts, you
