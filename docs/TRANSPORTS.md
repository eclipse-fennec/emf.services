# Transports

What ships, and what a deployment has to tell it.

Two transports exist: REST with Server-Sent Events, and MQTT. REST is
the complete one — discovery, sessions, invocation and events. MQTT
today carries events, and invocation for a flavor that declares it. The
broker itself is only reachable over REST.

## What a flavor says

Before the configuration, the model. A provider does not describe its
endpoint in a configuration file; it describes it in a **flavor**, and
the flavor travels with the registration.

**`RestFlavor`**

| Field | Meaning |
| --- | --- |
| `host` | `scheme://host[:port]`. Leave it out and the consumer takes the host from the reference. |
| `basePath` | required |
| `contentTypes` | empty means `application/json` is assumed |

Per operation, a `RestOperationFlavor`: the HTTP `method`, an optional
`path` (templates allowed), the `returnCodes`, and two kinds of
binding. A `RestParameterBinding` says where an argument travels —
`BODY`, `QUERY`, `HEADER` or `PATH` — under an optional `wireName`. A
`RestExceptionBinding` says which HTTP status a declared error
travels as.

Without bindings the older convention still applies: a single model
object is the body, anything else is a query parameter.

**`MqttFlavor`**: one or more `brokers`, a `requestTopic`, an optional
`responseTopic`, and defaults for QoS and the retained flag. Per
operation, an `MqttOperationFlavor` may override those and says whether
correlation is used.

## `client.rest` — talking to the broker over REST

Registers the four broker proxies (`BrokerCatalog`,
`BrokerImplementations`, `BrokerLookup`, `BrokerSessions`), a service
invoker and a proxy factory, all carrying
**`ddsr.broker.transport=rest`**, plus the SSE event source carrying
`ddsr.event.transport=rest`.

`org.eclipse.fennec.services.client.rest`

| Property | Default |
| --- | --- |
| `broker.url` | `http://localhost:8887/ddsr/rest` |
| `connect.timeout.millis` | `3000` |
| `read.timeout.millis` | `10000` |

`org.eclipse.fennec.services.client.rest.events`

| Property | Default |
| --- | --- |
| `flavors` | `REST` — becomes the `?flavors=` filter on the stream |
| `reconnect.seconds` | `3` |

It needs a `jakarta.ws.rs.client.ClientBuilder` service in the
framework and a `ResourceSet` with `emf.name=services`.

Note that `broker.url` is only the broker. The address of an actual
service call comes from the flavor, never from this setting.

## `client.mqtt` — events over MQTT

Registers one `EventSource` with `ddsr.event.transport=mqtt`, and is
**dormant until configured**: its configuration policy requires a
configuration.

`org.eclipse.fennec.services.client.mqtt`

| Property | Default |
| --- | --- |
| `broker.url` | `tcp://localhost:1883` |
| `topic.prefix` | `ddsr/events` — must match the broker side |
| `client.id` | `ddsr-consumer` |
| `qos` | `0` |

Configuring it is not enough. The SDK must be pointed at it:

```json
"org.eclipse.fennec.services.client": {
  "eventSource.target": "(ddsr.event.transport=mqtt)"
}
```

Without that target the SDK binds whichever event source is present,
which in any launch that also has REST is the SSE one.

## `client.java` — the SDK

Transport-agnostic, and it holds four collaborators, all mandatory and
static:

| Reference | Target |
| --- | --- |
| `eventSource` | none compiled in — set `eventSource.target` per deployment |
| `implementations` | `(ddsr.broker.transport=rest)` |
| `sessions` | `(ddsr.broker.transport=rest)` |
| `lookup` | `(ddsr.broker.transport=rest)` |

The three broker filters are fixed because an embedded broker
registers the same interfaces without a transport property, and a
client must not accidentally bind the in-process one.

`org.eclipse.fennec.services.client`

| Property | Default | |
| --- | --- | --- |
| `supported.flavors` | `REST` | comma-separated flavor kinds |
| `consumer.id` | generated | `consumer-<uuid>` when empty |
| `session.interval.seconds` | `600` | 0 switches sessions off |
| `provider.heartbeat.seconds` | `30` | 0 switches liveness off |

## A launch that only talks to a broker

`org.eclipse.fennec.services.broker.api` carries the four role
interfaces and nothing else. A framework that only *talks* to a broker
takes that bundle and gets no broker: the implementation lives in
`…broker.core`, and a launch without it has none.

Every launch also wants `org.eclipse.fennec.services.shutdown`. It is
one component with no configuration, and it makes an externally
terminated JVM stop the framework instead of dying, so that every
`@Deactivate` runs. Without it a provider killed with SIGTERM never
withdraws, and its consumers are never told.

## `broker.rest` — the broker's REST face

Serves `/registry`, `/references` (with the heartbeat endpoint),
`/consumers` and `/events`. `/catalog` and `/implementations` are **not**
hand-written resources: they are two factory configurations of the
generic distribution below, serving the broker's own contract from a
model document. The broker describes its own wire the same way a
provider does.

The bundle ships a configurator document. What a deployment usually
touches, all through environment variables:

| Variable | Default |
| --- | --- |
| `DDSR_HTTP_PORT` | `8887` |
| `DDSR_HTTP_HOST` | `0.0.0.0` |
| `DDSR_PUBLIC_URL` | `http://localhost:8887/ddsr/rest` |

The default base URL is the port plus the context path `ddsr` plus the
Jersey context `rest`. `DDSR_PUBLIC_URL` must be the address others can
actually reach, because it is written into the flavor the broker
publishes for itself. Behind a container port mapping or a proxy, that
is not the address it binds.

Also configurable: `heartbeat.seconds` on
`org.eclipse.fennec.services.broker.rest.sse`, default `10`, the SSE
keepalive.

## `broker.mqtt` — the same events over MQTT

One `EventSink`, dormant until configured.

`org.eclipse.fennec.services.broker.mqtt`

| Property | Default |
| --- | --- |
| `broker.url` | `tcp://localhost:1883` |
| `topic.prefix` | `ddsr/events` |
| `client.id` | `ddsr-broker` |
| `qos` | `0` |

Production launches ship the MQTT bundles unconfigured. See
[Eventing](EVENTING.md) for the topic layout and what is guaranteed.

## `provider.rest` — serving a contract with no generated code

One factory configuration serves one contract, read from a model
document in a bundle, and dispatches to whatever OSGi service a filter
selects.

Factory PID `org.eclipse.fennec.services.provider.rest`

| Property | Default | |
| --- | --- | --- |
| `service.filter` | — | **required**, usually `(ddsr.contract=<name>)` |
| `model.bundle` | — | **required**, the bundle carrying the document |
| `model.entry` | — | **required**, the path inside it |
| `ddsr.contract` | `""` | the contract's name |
| `publish` | `false` | announce to a broker as well as serve |
| `public.url` | `""` | the address to announce |
| `broker.url` | `""` | only used to name the contract in the announcement |
| `osgi.jakartars.application.base` | `""` | empty takes the flavor's `basePath` |

It waits for the whiteboard to confirm the application was really
deployed, up to 10 seconds, and fails loudly with the runtime's own
reason rather than reporting success for something that never mounted.

Two ordering caveats when `publish` is on, both consequences of how
Declarative Services registers and unregisters a component. The
announcement goes out before the whiteboard has mounted, and on
shutdown the endpoint stops before the withdrawal is announced, which
is the opposite of the order [Eventing](EVENTING.md) describes. A
deployment that needs that order announces from a separate component
and leaves `publish` off here.

## `flavor.rest` — the placement rules

No services, no configuration, no JAX-RS. A library of the rules three
parties have to agree on: which operation a method and path select
(more specific paths win), where each argument goes on the way out,
how to read them back on the way in, and which HTTP status a failure
travels as. Consumer, provider dispatcher and the code-generation
template all use this one copy, which is the point of it existing.

## Read next

- [Eventing](EVENTING.md) — what the two event transports guarantee
- [Code generation](CODE_GENERATION.md) — generating a resource from a flavor
- [Wire format reference](WIRE_FORMAT.md) — the documents on the wire
