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
i.e. you can view it as simple tables with your browser.
Note that this is very simplistic and some information may be missing.

(currently `application/xml` doesn't work... but xml seems out-of-fashion anyway ;-)

## Undeploy & Manage

Every resource has an implicit parameter `state` set to `deployed`.
You can set it explicitly to `undeployed` to have that resource removed.
This is how to get rid again of, e.g., an application.

But how long are you going to keep those undeployed resources in your plans?
When is it safe to remove that cruft?
How do you make sure that a freshly set up node looks exactly as a node that's been running for years?
Manually keeping track of this all is not very infrastructure-as-code-ish.

So you can tell the deployer to [manage](reference.md#manage) a specific type of resource.
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

See the section [loggers](reference.md#loggers) in the reference for more details.


To define a logger (a.k.a. category) for `com.mycompany.myapp` to log to this file, simply add this:

```yaml
loggers:
  com.mycompany.myapp
    level: DEBUG
    handler: MYAPP
```

See the section [log-handlers](reference.md#log-handlers) in the reference for more details.


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

To take an app centric view, you can group things into so called bundles.
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

### `myapp2.bundle`

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

If these were in the repository, we could include them in the root bundle as seen before:

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

So let's get those bundles into the repository:

## Packaging Bundles

This procedure is actually completely outside of The Deployer itself, but it's an important step,
so we'll describe it here. There are actually many ways to do it.
We _could_ deploy these files with the GUI of the repository, or with a Maven command similar to this:

```
mvn deploy:deploy-file -DgroupId=mygroup -DartifactId=myapp1 -Dversion=1.0 -Dtype=bundle
-Durl=http://localhost:8081/artifactory/libs-release-local/mygroup/myapp1/1.0/myapp1-1.0.bundle
-Dfile=myapp1.bundle
```

But we'd rather deploy artifacts automatically.
So we'll build the bundles with the `build-helper-maven-plugin`:

- Create your project directory
- Create a sub folder `src/main/resources`
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
Your apps should now get deployed and all the logging be configured.

## Variables

Nice and well, but there's no much use to it all, if you still have to copy the `deployer.root.bundle` to all of your machines by hand!
And every time you have to update a version, you'd have to repeat that process. Suboptimal.
We'd like to be able to update an application from the _outside_, e.g. from a Jenkins job.

To do that, simply set the `version` of `myapp1` to `${myapp1.version}`.
Then, using [httpie](http://httpie.org/), you can do:

    http --json POST :8080/deployer myapp1.version=1.0

... or (more wordy) using CURL:

    curl -X POST -H "Accept: application/json" -H "Content-Type: application/json" --data '{"myapp1.version":"1.0"}' http://localhost:8080/deployer

But we have _two_ artifacts. If we replace the second version `2.0` with `${myapp2.version}`,
how should the build job of `myapp1` know about the version of `myapp2`?
Why should it even care about the existence of `myapp2` at all?

Easy: Just don't. The default `version` (i.e. the effective `version` for an deployable already deployed) is `CURRENT`,
which resolves to the version currently installed.
So while you can't use this for the initial deployment, you _can_ use it to update one deployable, while all others remain the same.
Note that this doesn't work for bundles, as they are not actually deployed in the container, so there's not CURRENT version for them.

The dollar-curlies syntax is used for variables and variable expressions.
You can use variables for many things, but passing in versions is probably the most common use case.

Another interesting one is the log level. Maybe you want `DEBUG` on QA, but `INFO` in production.
But it would be cumbersome to pass this to every POST request.
Gladly, you can also define variables in the `deployer.config.yaml`; just add this section:

```yaml
vars:
  default.log-level: DEBUG
```

... for the QA stage, and with `INFO` for production.

You now could use `${default.log-level}` in all your `loggers`, but this variable happens to be the default for loggers.
Only if this variable is _not_ set, it defaults to `DEBUG`.

Another thing you'll be repeating all over the place, is the `group-id`.
You can also add this to your `deployer.config.yaml`, and then remove the `group-id` throughout your configuration files.
But there's no other fallback, i.e. if you don't have this variable set, The Deployer will fail with an undefined variable error.

Not all values can be set in this way; e.g., there's no much use in defining a `default.artifact-id`, is it?
For a complete list, see the [reference](reference.md#vars).


## Default Root Bundle

Still, something's missing. We still need to copy the root bundle to all nodes to bootstrap or when we add an artifact.
Wouldn't it be nice to have a way to deploy the root bundle itself?
Of course there is, but we'll need another level of indirection.
If there is no file `deployer.root.bundle`, a [default root bundle](reference.md#default-root-bundle) applies.
This is how it works:

Say you have a host `myhost.mydomain.org`.
Create a bundle artifact with the bundle you used as a root bundle,
and deploy it with `artifact-id` = `myhost` and `group-id` = `mydomain.org`.
Now you can pass the `version` to the POST and your done!
This works even if your host names end with digits, which is a common pattern for node names in a cluster,
i.e. `myhost01.mydomain.org` is mapped to a bundle name `myhost`.

If your domain names are generic, like `local` or `server.lan`, it's generally better to use the `default.group-id`.
If you already need a different `default.group-id`, you can define a `root-bundle-group` variable.

If your host names contain stage prefixes or suffixes like `dev`, or `qa`, you can strip them with regular expression,
by setting a variable `bundle-to-host-name` to `(.*?)(dev|qa)?\d*` for suffixes or `(?:dev|qa)?(.*?)\d*` for prefixes.
The first capturing group of the expression is used, so remember to mark any leading groups as non-capturing
by using `(?:X)`, as shown for the prefix.

If your host names are very technical and/or change very often,
you may be better off to configure an explicit `root-bundle-name` variable; this is just so much simpler.
Using regular expressions for your host names is most appropriate in situations where you have a PaaS like environment,
i.e. a generic `deployer.config.yaml` file is rolled out by some platform operations team... together with The Deployer.


## Schema Bundles

Now that you know all the parts, you should go ahead and use it for a while. 
To apply things to your own environment is not only an important step to let it sink in,
it's also critical to feel the need for the next, more advanced topic. Good bye! ;-)

Welcome back! You now probably have several application bundles that look quite similar:

```yaml
log-handlers:
  MYAPP:
    file: myapp.log
loggers:
  ${default.group-id}.myapp:
    level: DEBUG
    handler: MYAPP
deployables:
  myapp:
    version: 1.0
```

That's not too bad, you may say. There's not so much repetition. You can live with that.
And you're absolutely right... as long as you only have hand full of applications to manage.
If you have hundreds of applications, things may start to look different.
Are you really sure, all of them follow the same scheme?
What happens, if you have a central change, e.g. add a second log handler to all applications.
Things like this don't happen too often, but if they do, you wish you had started differently... without even this repetition.
But... how _can_ you?

Bundles can not only be created for one single application. By passing variables into a bundle, you can reuse them!
This type of bundles are called schema bundles, but they work technically like any other bundle does.
For example, create a bundle `apps`:

```yaml
log-handlers:
  ${toUpperCase(name)}:
    type: periodic-rotating-file
    file: logstash/${name}.log
    suffix: .yyyy-MM-dd
    format: "%d{yyyy-MM-dd HH:mm:ss,SSS}|%X{version}|%X{client}|%t|%X{reference}|%c|%p|%s%e%n"
loggers:
  ${group-id or default.group-id}.${name}:
    level: ${log-level or default.log-level or «DEBUG»}
    handler: ${toUpperCase(name)}
deployables:
  ${name}:
    group-id: ${group-id or default.group-id}
    version: ${version}
```

Note the `toUpperCase` [function](reference.md#variables) used in the variable expressions,
and the ` or ` syntax used to chain potential sub expressions... the first non-null will be picked.

You can use this bundle, as normal, but instead of passing the variables `name` and `version` to the POST request
or define them in the `deployer.config.yaml`, we'll pass them to the bundle like this:

```yaml
bundles:
  apps:
    version: 1.0.0-SNAPSHOT
    instances:
      myapp1:
        version: 1.0
```

The `instances` field is semantically a list of key value pairs, only that the key `name` is 'taken out' as an outer key,
while the other variable mappings are nested inside, i.e. we have only one instance in this example, containing:

```yaml
name: myapp1
version: 1.0
```

In this way, we can use the `apps` schema bundles for as many applications as we like;
the `name` is enforced to be unique, and the syntax is analogous to `bundles` and `deployables`.

We could also pass the variables `log-level` and `group-id`,
but they have defaults (i.e. they have ` or ` operators) that are good for now.

Note that the bundle version `1.0.0-SNAPSHOT` is _not_ the version that we pass into the apps schema bundle `1.0`.


## Controlling Versions

You can now manage the deployment of single applications.
But often there are dependencies between multiple applications... and sometimes they are very subtle.
So you'll want to have a defined and reproducible combination.
The best way for that is to have this versioned, and that's easy to do by using maven.
Modify the `pom.xml` of your bundle like this:

1. For every application in your bundle, add a maven property, e.g. `myapp.version`.
1. For every application in your bundle, add a dependency using that version property, e.g.:

 ```xml
<dependency>
    <groupId>mygroup</groupId>
    <artifactId>myapp</artifactId>
    <version>${myapp.version}</version>
    <type>war</type>
</dependency>
```
3. Enable resource filtering:

```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>
        </resource>
    </resources>
    ...
</build>
```

In this way, the variable `${myapp.version}` in your bundle will be resolved when building,
i.e. while you have the variable name in your source bundle,
it will be the fixed version string from the pom in the deployed artifact.

Now you can use the `versions` plugin to update the versions in your pom:

`mvn versions:update-properties -DgenerateBackupPom=false`

If you want to also update to snapshot versions, use `-DallowSnapshots`. 

You can also exclude or whitelist certain properties or artifacts:
see `mvn versions:help -Dgoal=update-properties -Ddetail` for details.
For details on the `rulesUri` option, see [this blog](http://blog.xebia.com/keeping-dependencies-up-to-date-in-maven/)
or the [docs](http://www.mojohaus.org/versions-maven-plugin/rule.html).
