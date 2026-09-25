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

- the **REST invoker** and the **MQTT invoker** — the client half of a
  service call
- the **REST dispatcher** and the **MQTT dispatcher** — the provider
  half, continuing the caller's trace
- the **client transport** — every call the SDK itself makes to the
  broker: publish, modify, withdraw, heartbeat, lookup, the session
  PUT and DELETE, the catalogue operations
- **`…telemetry.rest`** — a Jakarta REST whiteboard extension, for
  resources somebody wrote by hand: it continues the caller's trace in
  the default application, which is where a plain
  `@JakartarsResource` lands

The extension and the dispatcher do not overlap. A named application
brings its own providers, so the whiteboard's extension is never asked
there — which is the same rule that once cost us an afternoon (#125)
and here keeps a call from being traced twice.

Because all three broker contracts are served generically since #88,
the broker's own lookups, publishes and withdrawals pass through the
REST dispatcher like any other contract — so the two halves of, say, a
publish carry the same name from both ends without either end being
told about the other.

The transport is one place rather than twelve for the same reason the
origin header of #125 lives there: what has to be true of every call
belongs where every call passes. It is also not a client filter, which
was tried: a JAX-RS filter pair cannot close a span for a request that
never reaches a response, and a broker that refuses the connection is
exactly the call worth seeing.

Span names are `Contract/operation`, never a path or a topic: a name
with an id in it is a name nobody can group by.

Attributes follow the OpenTelemetry semantic conventions where they
fit — `rpc.system`, `rpc.service`, `rpc.method`, `server.address`,
`http.request.method`, `http.response.status_code` — plus two of ours:

- `fennec.flavor` — REST or MQTT
- `fennec.origin` — **who made this call**, the identity of
  [#125](https://github.com/eclipse-fennec/emf.services/issues/125). On
  a client span it is this runtime, on a server span the caller, read
  off the `X-DDSR-Origin` header over REST and off the CloudEvents
  `source` over MQTT. It is the difference between a trace that says
  what happened and one that says which system told which system what.
  A caller that named nobody is `anonymous`, which is a statement, not
  a gap.

## The TypeScript side

The TS client has the same seam, for the same reason: `@ddsr/telemetry`
is `CallTracer`, `CallSpan` and `TraceCarrier` with no dependencies,
and `@ddsr/telemetry-otel` is the implementation a deployment can leave
out. `@ddsr/client` and `@ddsr/flavor-rest` take a tracer and do
nothing without one.

```ts
const telemetry = startTelemetry({ serviceName: 'my-consumer' });
const client = DdsrClientImpl.create({
  brokerUrl,
  flavorPlugins: [new RestFlavorPlugin({ tracer: telemetry.tracer, origin: origin.token })],
  tracer: telemetry.tracer,
});
```

Traced on that side: every call the SDK makes to the broker (one
wrapper around its `fetch`, the twin of `RestTransport.send`), and every
service invocation through the REST flavor plugin.

One difference is the language's, not a decision: Java makes a span
current for a scope with try-with-resources, and JavaScript can only
enter a context inside a callback. So the unit-of-work span is
`telemetry.during('Demo/tick', run)` rather than a `doing` that is
closed later.

Because both sides write and read the same W3C header, a trace crosses
them: a `Demo/tick` from the TypeScript consumer has the Java broker's
lookup and the Java provider's answer under it, and every span carries
the same `fennec.origin`.

A TypeScript client reports what it holds as well (#167).
`client.runtime.snapshot()` is the twin of the Java `ClientRuntime`,
and `telemetry.watch(client.runtime)` turns it into the client gauges
under the same names and attributes as Java's, including the binding
series:

```ts
const watch = telemetry.watch(client.runtime);
// …
watch.close();
```

`startTelemetry` exports metrics to the same OTLP endpoint as traces,
every 15 s unless `metricIntervalMillis` says otherwise, and takes
`relationships: false` like the Java configuration. The exporter uses
delta temporality. Under cumulative temporality the JavaScript SDK keeps
reporting a series nobody observes any more, at its last value, so an
ended binding would linger. The Java SDK drops it either way. A gauge
carries no temporality on the wire, so a backend sees no difference.

## Logs

This project logs with JUL by convention, and the OSGi integration
bridges the OSGi LogService — so without a bridge of our own, nothing
this code writes would reach a backend. `JulBridge` puts a handler on
the root logger and forwards every record with its severity, its logger
name, its thread and, for a failure, the exception as the three
attributes a backend groups errors by.

What it buys is correlation: a line written inside a call carries that
call's trace id, so a failed invocation and the line the provider wrote
about it are one thing rather than two that happened around the same
time. `logs=false` turns it off.

## What a watcher sees of a node

Counts per bound runtime, read at collection time from the
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

### Who uses what

The counts say how many, not which. Beside them, three gauges report
one series per relationship (#166), so a view can draw the deployment
as a graph:

```
fennec.services.broker.registration   one per registration and contract
    fennec.node, fennec.reference, fennec.provider, fennec.implementation,
    rpc.service, fennec.flavor (comma-joined)

fennec.services.broker.lease          one per registration and holder
    fennec.node, fennec.reference, fennec.consumer, fennec.origin

fennec.services.client.binding        one per binding
    fennec.node, rpc.service, fennec.reference, fennec.state
```

The provider side is on the registration series only. A lease or a
binding names the registration by `fennec.reference`, and a view joins
on that. The broker's series are the complete picture: the broker knows
every lease, including those of consumers that report no telemetry of
their own. The client's series add what only the consumer knows, which
is the state its locator is in.

`fennec.origin` on a lease is the token the holder's session reached
the broker from, the same one its spans carry. That is what joins a
lease to the calls in a trace. `fennec.consumer` is what the client
calls itself, which it chooses freely.

A value is the number of relationships with those attributes, which is
almost always 1. Two locators with different filters may be bound to
the same registration, and then the binding series says 2.

Two things make series come and go:

- A client without a configured `consumer.id` gets a new random one on
  every start, so its leases and bindings become new series each time.
  A deployment that wants a stable graph sets `consumer.id`.
- A reference id is not stable across a broker restart.

The series grow with the deployment. `relationships=false` turns them
off and keeps the counts.

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
    "useRegisteredPropagators": false,
    "relationships": true
}
```

## Seeing it

```bash
demo/observability/run-demo.sh
```

starts a collector with Tempo, Prometheus, Loki and Grafana as one
podman pod, then a broker, a provider and a consumer that keeps calling
it — all three reporting. Grafana is at http://localhost:3000 with a
provisioned dashboard, and a `Demo/tick` trace has eight spans across
the three processes. [demo/observability/README.md](../demo/observability/README.md)
says what to show and what is not traced.

`org.eclipse.fennec.services.telemetry/telemetry-demo.bndrun` is the
smaller thing: one framework with a broker and the SDK, for trying the
wiring out without the stack.

## What is not built yet

- **`setPropagators` upstream.** Proposed to the OSGi Technology
  project: `buildPropagators` on the shared base class and a
  `propagators` attribute on both sender configurations, defaulting to
  W3C Trace Context plus Baggage. When that is released,
  `useRegisteredPropagators` can become the default here and this
  bundle can stop bringing propagators of its own.
- **The event stream.** SSE and the MQTT event subscription are
  long-lived streams rather than calls, and a span per stream would
  either last for days or say nothing. The REST extension skips
  `text/event-stream` for that reason. What a watcher wants of them is
  in the runtime services already: whether the stream is connected, and
  how many events the broker had to drop.
- **A collector in the harness.** The harness proves the wire, not the
  telemetry; nothing asserts end to end that two processes report one
  trace. The unit tests assert exactly that across a carrier.
