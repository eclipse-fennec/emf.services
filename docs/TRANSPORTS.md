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

## `client.mqtt` — events and calls over MQTT

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

The same bundle also **calls** services announced over MQTT (#98). That
half needs no address of its own — the brokers a flavor announces are
where it dials — and is configured only with how long to wait:

`org.eclipse.fennec.services.client.mqtt.invoker`

| Property | Default |
| --- | --- |
| `reply.timeout.seconds` | `10` |

Which transport a proxy uses is not configured at all: the SDK picks by
the flavors the service announces, in the order it announces them, from
the invokers this runtime has. A service reachable only over a
transport that is not installed fails with a message naming both sides
instead of a call going out the wrong way.

## `provider.mqtt` — serving a contract over topics

The MQTT twin of `provider.rest`: one subscription per operation of a
flavor, a dispatcher that reads the call, invokes the service and
answers on the topic the call named. Nothing in it is written per
contract.

It has no configuration of its own. Where it listens is the flavor's
own statement — the brokers it announces — and connections are pooled
per broker URL, opened with the first export and closed with the last.

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

## The payload encoding is the contract's choice

How an argument or a result becomes bytes used to be decided inside
each transport, and nobody could say otherwise: XMI over REST, because
the wire resource was created as `ddsr-wire.xmi` and the extension
picked the factory; hand-rolled JSON over MQTT. The same operation
called two ways was two encodings.

Since #100 the codec asks the ResourceSet which factory serves a
content type. That is EMF's own mechanism, so **an encoding exists
exactly when someone registered a `Resource.Factory` for it** — there
is no serializer abstraction of ours on top, and adding a format is a
deployment decision rather than a code change.

Two rules make that safe to rely on:

- **An unregistered content type is refused**, never quietly served as
  XMI. A provider that declared one encoding and got another would be
  a wire bug that only surfaces at the far end.
- **The hardening moves with the format.** The size cap and the
  refusal to fetch what a document points at apply to every read —
  those are questions about what a body may cost and reach, not about
  XML. The XML parser features are set only for an XML resource. A new
  format brings its own exposure and has to bring its own answer.

The contract is where the choice is written down, and it already had
the words for it:

| Says | Means |
| --- | --- |
| `RestOperationFlavor.consumes` | how the provider reads the request body |
| `RestOperationFlavor.produces` | how it writes the response |
| `RestFlavor.contentTypes` | what the flavor speaks as a whole |

`consumes` and `produces` are two different questions: an operation may
take protobuf and answer XML. Empty means XMI, which is what every
caller had before.

### Protobuf, and where it is deliberately absent

`org.eclipse.fennec.protobuf` (Eclipse Fennec emf.util) registers an
EMF `Resource.Factory` for `application/x-protobuf`. It is in the
**client and provider runs and deliberately not in the broker's**.

That asymmetry is the point rather than an oversight. Broker traffic —
catalog, publish, lookup, events — is the cross-language contract, and
the TypeScript track has no protobuf-to-EMF binding. With the factory
absent from the broker, XMI there is a property of the deployment; with
it present, cross-language parity would depend on content negotiation
going right every single time, and a mistake would be a silent break.

An invocation is the opposite case: two parties the flavor names, and
what they agree on is nobody else's business.

**The limit that follows:** a contract that declares protobuf is one a
TypeScript consumer cannot read, and nothing in the registry stops it
being offered one. The `PersonStore` example is therefore Java on both
ends, and says so in the contract document itself.

### The example

`model/person-store.xmi` in `examples.payment` is the smallest thing
that shows it: one operation, a modelled argument and a modelled
result, `consumes`/`produces` = `application/x-protobuf`, and a BODY
binding. Nothing else. The provider side is a five-line service; the
generic distribution serves it from the document, and the consumer in
`examples.persons` calls it through an ordinary Java interface.

Neither side mentions an encoding. Change one word in the contract and
the same call travels as XMI — which is the only honest test of
whether the choice is really the contract's.

Scenario P of the host harness runs it and reads the result back:

```
[DDSR-Protobuf] stored Ada Lovelace and got id=… back
                — argument and result travelled as protobuf
```

One thing the example could not use: the m2t interface template maps
the model's primitive type names to Java, and an `eType` that is an
`EClass` has no such mapping yet, so it generates an empty type. The
`PersonStore` interface is therefore hand-written and says why — the
template gap belongs to [#25](https://github.com/eclipse-fennec/emf.services/issues/25).

## And the envelope around it

Since [#101](https://github.com/eclipse-fennec/emf.services/issues/101)
every message — a lifecycle event, a call, an answer — travels in a
CloudEvents 1.0 envelope. The two content modes exist because the
transports differ, and this registry uses both for exactly that reason:

- **Binary mode over HTTP.** The attributes are `ce-*` headers, the
  body is the payload in whatever encoding the contract declared. The
  encoding is still the contract's choice, still announced as
  `Content-Type`, still selected the way this page describes — the
  envelope says *which* encoding was used, and changes nothing about
  it. Nothing had to move on the REST wire, and nothing did.
- **Structured mode where a transport carries only messages** — MQTT,
  an SSE frame, AMQP when it arrives. Envelope and payload in one JSON
  document. A textual encoding (XMI) rides as a JSON string; a binary
  one (protobuf) as `data_base64`, because that is what the JSON event
  format says to do.

The MQTT call path gained more than a wrapper: its payload is now
`ServiceInvocation` rather than a JSON bag of arguments, so a value
keeps the type its contract gave it and a **modelled** argument is
possible there at all — which is the same thing this page's protobuf
example proved for REST, arriving on the transport that had no way to
say it.

## Read next

- [Eventing](EVENTING.md) — what the two event transports guarantee
- [Code generation](CODE_GENERATION.md) — generating a resource from a flavor
- [Wire format reference](WIRE_FORMAT.md) — the documents on the wire
