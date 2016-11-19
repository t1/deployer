# Frequently Asked Questions

## Rationale

### Why Is There No `if`?

There's a `switch`, so you can set stage specific values.
But you can't switch anything entirely off, and that's on purpose:
The environment of your applications should be as similar as possible on all stages,
so the tests you do won't lie at you.
Even the `switch` should has a big `handle with care` label to it.
