# Telemetry — traces across the three parties

A call that starts at a consumer, asks the broker who serves a
contract, and ends at a provider is three processes doing one thing.
This page is about making it look like one thing.

Two seams carry it, and both are optional:

| Seam | Where | What it answers |
| --- | --- | --- |
| `CallTracer` | api bundle, used by the invokers and dispatchers | what happened during one call, and whose call it was part of |
| `BrokerRuntime` / `ClientRuntime` | api bundle, see [Runtime services](RUNTIME.md) | what a node holds right now |

`…telemetry` implements both on OpenTelemetry. A deployment that does
not install it loses its telemetry and nothing else: no call path
requires the bundle, and the bundle requires no part of the call path.

## The one thing that had to be built

The OSGi Technology project's
[OpenTelemetry integration](https://github.com/osgi-technology/opentelemetry)
publishes the SDK as OSGi services — `TracerProvider`, `MeterProvider`,
`LoggerProvider` — with an OTLP/HTTP exporter and integrations for the
framework, DS, ConfigAdmin and the REST whiteboard. What it does not
do is **context propagation**, and that is the whole of what makes a
call one trace:

- it builds its SDK without `setPropagators(…)`, so the
  `ContextPropagators` service it registers is a no-op
- nothing reads or writes `traceparent`, in either direction

So this bundle brings its own W3C propagators and uses them by
default. `useRegisteredPropagators=true` switches to the published
service for the day the integration supplies real ones —
`ContextPropagators` that silently inject nothing is worse than none,
because everything looks configured.

## Where the context travels

| Wire | Carrier | Why there |
| --- | --- | --- |
| REST | the `traceparent` / `tracestate` HTTP headers | what every instrumented server already extracts |
| MQTT | the CloudEvents envelope's extension attributes | MQTT 3 has no user properties, and CloudEvents puts distributed tracing there |

Both are additive. A reader that does not know the fields ignores
them, which is why turning telemetry on changes no wire format and
breaks no cross-language test.

The context is written **after** the span exists and **before** the
message is serialised, in both invokers, because what travels has to
name the call it belongs to.

## What gets a span today

Four places, which between them cover the four moments worth seeing:

- the **REST invoker** and the **MQTT invoker** — the client half of a
  service call
- the **REST dispatcher** and the **MQTT dispatcher** — the provider
  half, continuing the caller's trace

Because all three broker contracts are served generically since #88,
the broker's own lookups, publishes and withdrawals pass through the
REST dispatcher like any other contract — so they are traced without
anything being written for them.

Span names are `Contract/operation`, never a path or a topic: a name
with an id in it is a name nobody can group by.

Attributes follow the OpenTelemetry semantic conventions where they
fit — `rpc.system`, `rpc.service`, `rpc.method`, `server.address`,
`http.request.method`, `http.response.status_code` — plus
`fennec.flavor`, which says REST or MQTT.

## What a watcher sees of a node

Six gauges per bound runtime, read at collection time from the
[runtime services](RUNTIME.md):

```
fennec.services.broker.registrations
fennec.services.broker.catalog.entries
fennec.services.broker.sessions
fennec.services.broker.leases              who holds a claim on what
fennec.services.broker.events.dropped
fennec.services.broker.cold.entries
fennec.services.broker.changes

fennec.services.client.published           and .live
fennec.services.client.bindings            and .rebinding
fennec.services.client.stream.connected
fennec.services.client.changes
```

Observed rather than pushed: an instrument asks its runtime for a
snapshot when the exporter collects. A number recorded when it changed
stops being true between changes, and a runtime that goes quiet would
export a stale reading forever.

## Installing it

The bundles come from the OSGi Technology project's snapshots, which
resolve from the Central snapshot repository this workspace already
uses:

```
org.eclipse.osgi-technology.opentelemetry.repack              API + SDK
org.eclipse.osgi-technology.opentelemetry.core.commons         publishes the provider services
org.eclipse.osgi-technology.opentelemetry.core.sender.http     OTLP/HTTP exporter
org.eclipse.osgi-technology.opentelemetry.core.sender.logging  logs instead of exporting
org.eclipse.fennec.services.telemetry                          this project's half
```

The repack bundle needs SPI Fly, in its bundle form — every launch here
already pins `org.apache.aries.spifly.dynamic.bundle` with ASM for
unrelated reasons.

A sender registers nothing until it is configured
(`ConfigurationPolicy.REQUIRE`), so a deployment adds, to the
configuration its own bundle carries:

```json
"org.eclipse.osgi.technology.opentelemetry.core.sender.logging": {
    "serviceName": "the-name-of-this-node"
}
```

and this bundle's own configuration, if the defaults do not fit:

```json
"org.eclipse.fennec.services.telemetry": {
    "scope": "org.eclipse.fennec.services",
    "useRegisteredPropagators": false
}
```

`org.eclipse.fennec.services.telemetry/telemetry-demo.bndrun` is a
framework with a broker, this bundle and the SDK, for trying the wiring
out.

## What is not built yet

- **The SDK's own calls to the broker.** A Java provider publishing, or
  a consumer looking up, goes through the proxies in `…client.rest`
  rather than through the service invoker, and those are not
  instrumented. The broker end of such a call is traced; it simply
  starts a trace instead of continuing one.
- **`setPropagators` upstream.** Two lines in each sender bundle of the
  OSGi integration, and then `useRegisteredPropagators` can become the
  default and this bundle can stop bringing its own.
- **Logs.** The integration bridges the OSGi LogService already;
  nothing here routes JUL into it.

Issue #126 stays open for these.
