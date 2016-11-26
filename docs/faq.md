# Frequently Asked Questions

## Design Decisions

### Why Is There No `if`?

There's a `switch`, so you can set, e.g., stage specific values.
But you can't switch anything entirely on or off, and that's on purpose:
The environment of your applications should be as similar as possible on all stages,
so the tests you do won't lie at you.
Even the `switch` should has a big `handle with care` label to it.


### Why the system property `com.github.t1.deployer.container.CLI#DEBUG`?

CLI statements may contain credentials or other sensitive data that normally should not be logged,
but it's essential to see them while developing The Deployer.
The normal log level mechanism is security wise not sufficient to hide such information,
as manipulating a log level may be too easy.
So there's an extra flag to enable logging the CLI statements.
