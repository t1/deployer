# Tutorial

After you've read the 1-Minute-Tutorial in the README, you can already nicely install and update artifacts.
But the deployer can do much more, and this tutorial will show you how to basically configure resources,
how to combine them into bundles, and how to integrate it all into a CI or even CD pipeline.

'Resources' are all the things that the deployer can change, e.g. applications (artifacts like `war` files)
as well as loggers, etc.

## GET config

Before we start, you can also _read_ the currently effective configuration by simply GETting `http://localhost:8080/deployer`.
This resource provides a concise overview of the relevant configuration without providing confidential data like passwords.
Still it may not be wise to make this information freely available in the internet.

## Undeploy & Manage

Every resource has an implicit parameter `state` set to `deployed`.
You can set it explicitly to `undeployed` to have that resource removed.
This is how to get rid again of, e.g., an application.

But how long are you going to keep those undeployed resources in your plans?
When is it safe to remove that cruft?
How do you make sure that a freshly set up node looks exactly as one running for years?
Manually keeping track of this all is not very infrastructure-as-code-ish.

You can instead tell the deployer to [manage](REFERENCE.md#manage) a specific type of resource.
This makes the deployer _remove_ all resources that are _not_ listed.

## Configuring Resources

The deployer can be used to configure more than just artifacts.
Here, we'll only scratch on configuring just loggers and log-handlers, so we can build on that to combine things.
For the full details, please refer to the [reference](REFERENCE.md).

If you have several applications running in one container,
you'll want to have the logs for every app in a separate file and not everything clumped into the `server.log`.
To define a log-handler for `myapp`, simply add this to your `deployer.root.bundle`:

```yaml
log-handlers:
  MYAPP:
    file: myapp.log
```

To define a logger for `com.mycompany.myapp`, simply add this:

```yaml
loggers:
  com.mycompany.myapp
    level: DEBUG
    handler: MYAPP
```

## Bundles

Now that you know about the building blocks, you should start to actually use them.
Do it for maybe two or three artifacts and their loggers, then come back.
Yeah: Take a break from reading this tutorial. Do it for real. Now!

Welcome back! You now have a `deployer.root.bundle` that looks somewhat similar to this:

```yaml
log-handlers:
  JOLOKIA:
    file: jolokia.log
  MYAPP:
    file: myapp.log
loggers:
  org.jolokia:
    level: DEBUG
    handler: JOLOKIA
  mygroup.myapp:
    level: DEBUG
    handler: MYAPP
artifacts:
  jolokia:
    groupId: org.jolokia
    artifact-id: jolokia-war
    version: 1.3.4
  myapp:
    groupId: mygroup
    version: 1.0.0
```

Even with only two apps, you'll notice that it's not easy to find all the things belonging to one app.
They are spread over all the resource types.
And this will be getting worse as you add more and more apps and more and more resource types.

You can group things into so called bundles. You already know how they look: The `deployer.root.bundle` is one.
So let's define one bundle for each app above:

### `jolokia.bundle`

```yaml
log-handlers:
  JOLOKIA:
    file: jolokia.log
loggers:
  org.jolokia:
    level: DEBUG
    handler: JOLOKIA
artifacts:
  jolokia:
    groupId: org.jolokia
    artifact-id: jolokia-war
    version: 1.3.4
```

### `myapp.bundle`

```yaml
log-handlers:
  MYAPP:
    file: myapp.log
loggers:
  mygroup.myapp:
    level: DEBUG
    handler: MYAPP
artifacts:
  myapp:
    groupId: mygroup
    version: 1.0.0
```

And include them in the root bundle:

### `deployer.root.bundle`

```yaml
artifacts:
  jolokia:
    type: bundle
    groupId: mygroup
    version: 1.3.4
  myapp:
    type: bundle
    groupId: mygroup
    version: 1.0.0
```

Note that:

1. 

You'll also have to get these files into your repository:

## Packaging Bundles

