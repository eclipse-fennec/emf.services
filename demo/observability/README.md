# The demo: three processes, one trace, one dashboard

What this starts:

```
  fennec-consumer     (Java)   ──lookup──▶ fennec-broker
  fennec-ts-consumer  (Node)   ──lookup──▶     ▲
        │                                      │
        └──── call ──▶ fennec-payment-provider ─┘ publish

  all four ──OTLP──▶ collector ──▶ Tempo · Prometheus · Loki ──▶ Grafana
```

One command:

```bash
demo/observability/run-demo.sh
```

Then open **http://localhost:3000** (no login) and pick the dashboard
**Fennec Services**. Ctrl-C stops the three frameworks; the stack keeps
running so the last minutes stay readable. `stop.sh` takes it down.

Java 21 — newer JVMs break the SPI Fly weaving these launches need.
Node 24 with corepack for the TypeScript consumer — pnpm 11 does not
start on Node 20. `run-demo.sh` checks both before it builds anything,
and picks up an nvm that is installed but not on the PATH.

## What to show

**One call is one trace.** In the traces panel, a `Demo/tick` trace has
nine spans across three services: the consumer's unit of work, the
lookup it sent to the broker, the broker answering it, the Payment call
and the provider answering it, a second lookup and the
`BindingProbe/echo` that the provider answers.
Nothing was passed between the processes but a `traceparent` header.

**And one of those consumers is not Java.** Filter the traces by
service `fennec-ts-consumer` and the same tree appears with a Node
process at its root: a TypeScript consumer, a Java broker answering its
lookup and a Java provider answering its call, in one trace. Nothing
was shared between them but a `traceparent` header — which is the claim
this project makes, in one picture.

**Every span says who called.** `fennec.origin` carries the identity of
#125, so the trace answers *which system told which system what* and not
only what happened.

**The gauges are what nobody else can count.** `fennec_services_broker_leases`
is the number of claims consumers hold on registrations — a
`DEPRECATE_AND_DRAIN` handover waits for exactly that, and no reader of
the wire can work it out. Same for `fennec_services_client_stream_connected`:
the broker sees a subscription, only the consumer knows whether it is
reading one.

**The logs are in the same picture.** A line written inside a call
carries that call's trace id, so the Loki panel and the trace are two
views of one thing.

## What is traced, and what is not

| | traced |
| --- | --- |
| a call the SDK makes to the broker — publish, lookup, heartbeat, session | both halves |
| a contract served by the generic REST distribution (`BindingProbe`, `PersonStore`, the broker's own contracts) | both halves |
| a hand-written `@JakartarsResource`, like the example's `PaymentResource` | both halves, through `…telemetry.rest` |
| a contract served over MQTT | both halves |
| an SSE subscription | not at all, on purpose |

A hand-written resource is covered by a whiteboard extension
(`…telemetry.rest`) that reaches the default application, which is where
a plain `@JakartarsResource` lands. It deliberately does not reach the
generic distribution's named applications — those bring their own
providers and their dispatcher is instrumented already, so a call is
traced once wherever it is served. That is why a tick shows
`PaymentResource/getBalance` and `BindingProbe/echo` and never both
namings of one call.

A subscription is one request that lasts as long as the consumer does,
so the filter leaves `text/event-stream` alone: a span around it would
be open for hours and would say nothing that
`fennec_services_client_stream_connected` does not say better.

## The pieces

| | |
| --- | --- |
| `observability-stack.yaml` | collector, Tempo, Prometheus, Loki, Grafana as one podman pod — taken from the OSGi Technology project and kept close to it |
| `dashboards/*.json` | provisioned into Grafana by `deploy.sh` |
| `with-dashboards.py` | puts those files into a ConfigMap on the way to `podman kube play` |
| `deploy.sh` / `stop.sh` | the stack alone, without the frameworks |
| `run-demo.sh` | the stack, then broker, provider and consumer |
| `work/` | the three logs, and the gradle output (gitignored) |

The frameworks are the ordinary launches plus telemetry:
`broker-otel.bndrun`, `payment-provider-otel.bndrun`, `client-otel.bndrun`
— each one `-include`s the launch it extends, so the demo cannot drift
away from what is shipped. The configuration that points the SDK at the
collector is a bundle (`…demo.telemetry`), and the consumer that keeps
calling is another one (`…demo.traffic`, quiet unless a launch
configures it).

## Framework integrations

Besides this project's own telemetry, the launches carry the OSGi
Technology integrations for Configuration Admin, Declarative Services,
the Jakarta REST whiteboard and the HTTP whiteboard. Their dashboards
live in that project (`container/grafana-dashboards/`) and can be
imported into this Grafana as they are.

On a cold start those integrations log a burst of NPEs and then run with
part of their instruments missing — a startup-order bug reported as
eclipse-osgi-technology/opentelemetry#24. Nothing in this demo depends
on them.
