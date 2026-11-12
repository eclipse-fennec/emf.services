# Transports

What ships, and what a deployment has to tell it.

Three transports exist: REST with Server-Sent Events, MQTT and AMQP.
REST is the complete one — discovery, sessions, invocation and events.
MQTT carries events, and invocation for a flavor that declares it.
AMQP carries invocation only. The broker itself is only reachable over
REST.

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

**`MessagingFlavor`** — one flavor for everything that travels over a
messaging broker, whichever protocol the broker speaks. Its `kind`
(`MQTT`, `AMQP`) and the scheme of its `brokers` say which; the
protocol decides how a channel is spelled and nothing about the shape
of the flavor.

| Field | Meaning |
| --- | --- |
| `brokers` | one or more broker URLs — `tcp://`, `ssl://` for MQTT, `amqp://`, `amqps://` (the virtual host as the path) for AMQP. Never with user or password: the flavor is announced to everyone |
| `channel` | the base channel for calls: a topic over MQTT, a routing key over AMQP. An operation is reached on `<channel>/<operation>` or `<channel>.<operation>` |
| `replyChannel` | where answers go. **Set:** a consumer listens on a subtree of its own name below it and names the exact channel in each call — a place a broker's ACL can be written against. **Empty:** the flavor names no place, and the consumer's connection provides a channel only it reads |
| `exchange` | the exchange calls are published to, where the protocol has exchanges (AMQP); empty is `amq.topic` |
| `defaultQos` | `AT_LEAST_ONCE` unless said otherwise; AMQP has no exactly-once and caps at 1 |

Per operation, a `MessagingOperationFlavor` may override `channel`,
`replyChannel` and `qos`.

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
| `broker.tokens.target` | `(fennec.tokens=none)` — which token supplier calls to the broker take their token from |
| `provider.tokens.target` | `(fennec.tokens=none)` — the same for calls to providers |

The two targets are how a node says whether it shows a token: both
references are mandatory, so a transport configured for
`(fennec.tokens=oauth)` exists only once that supplier does
([Client framework guide](CLIENT_FRAMEWORK_GUIDE.md) §5.4a).

**The transport needs this configuration to exist at all** — an empty
one is enough for a node that takes every default. Without it there is
no transport and so no broker proxies and no client. That is on
purpose: a transport that started on defaults and was then given its
real target would be taken down and rebuilt, with everything above it.

`org.eclipse.fennec.services.client.rest.events`

| Property | Default |
| --- | --- |
| `flavors` | `REST` — becomes the `?flavors=` filter on the stream |
| `reconnect.seconds` | `3` |

It needs a `jakarta.ws.rs.client.ClientBuilder` service in the
framework and a `ResourceSet` with `emf.name=services`.

Note that `broker.url` is only the broker. The address of an actual
service call comes from the flavor, never from this setting.

## The messaging connection

Every MQTT and AMQP part of a node — the client's event source and
invokers, the generic distributions, the broker's event sink — connects
through
[Gecko Messaging](https://github.com/geckoprojects-org/org.gecko.messaging)
(`…services.messaging`). None of them is configured differently for it:
each derives a messaging instance from the broker URL, client id and
`tls.*` attributes it already has, and takes it away again when it
stops. The scheme of the URL says which binding speaks to the broker —
`tcp`, `ssl`, `ws`, `wss` the `mqtt3` binding (factory PID
`org.gecko.messaging.mqtt3`), `amqp` and `amqps` the `amqp` one
(`org.gecko.messaging.amqp`). What a launch has to bring is the binding
for the protocols it speaks and the Configuration Admin service, which
is how an instance comes to be.

What the instance gives each of them:

- **The connection is its own.** It reconnects with a backoff of 1 s
  doubling to 30 s, and restores every subscription before it reports
  being connected again. A broker restart costs what was published
  meanwhile, and nothing after.
- **Nothing waits at start.** A part that is configured before its
  broker is up is not a failed component; it becomes useful when the
  connection is there. The broker's sink is registered with the first
  connection, a contract served from configuration is announced once
  its topics are subscribed, and the RSA distribution offers itself
  only then — an export made after that is listening when it returns.
- **The envelope is its own too.** Every message is a CloudEvent, and
  the instance writes and reads it (`cloudEvents=structured`): this
  project hands it what the event says and the document it carries, and
  how that looks in bytes is the same on every protocol the messaging
  project binds. That is also what gives MQTT 3.1.1, which has no
  message properties, a reply address and a correlation. One visible
  difference to what this project wrote itself: an XMI document travels
  as `data_base64` rather than as a string — both are the same
  document, and the TypeScript track, which still writes the string,
  reads either ([Wire format](WIRE_FORMAT.md#the-envelope-cloudevents-10)).

The instances are named `fennec.services.broker.events`,
`fennec.services.client.events`, `fennec.services.invoker.<id>` and
`fennec.services.provider.<id>`, and show up under those names in
`MessageServiceRuntime`. A message is limited to 1 MiB, sent and
received, unless the connection's owner names a larger document
([How large a body may be](#how-large-a-body-may-be)).

One thing follows for a caller: an invoker opens its connection with
the first call to a broker and waits for it, up to the reply timeout.
A first call made while a component is being activated by a
configuration can run into that timeout — the instance is activated on
the thread that delivers configurations, and that thread is the one
waiting. Calls from a thread of the application's own are not affected.
The RSA example's consumers (`GreeterCaller`, `LedgerCaller` in
`…examples.rsa.consumer`) show the shape: `activate` starts a thread
that makes the call, `deactivate` interrupts it.

## `client.messaging` — events over MQTT, calls over MQTT and AMQP

Registers one `EventSource` with `ddsr.event.transport=mqtt`, and is
**dormant until configured**: its configuration policy requires a
configuration.

`org.eclipse.fennec.services.client.mqtt` (the event transport; its PID
kept the protocol's name, because events travel over MQTT only)

| Property | Default |
| --- | --- |
| `broker.url` | `tcp://localhost:1883` |
| `topic.prefix` | `ddsr/events` — must match the broker side |
| `client.id` | empty — the node is then `ddsr-consumer-` and the first eight characters of its framework's UUID |
| `qos` | `0` |

Leave `client.id` empty unless your MQTT broker writes its access
rules against client ids. A broker disconnects the older of two
connections that show the same id, both reconnect, and two nodes
given one name throw each other out for as long as both run — which
looks like an event connection that keeps dropping. The derived id is
new with every start; nothing depends on it, because the transport
takes a snapshot on every connect.

Configuring it is not enough. The SDK must be pointed at it:

```json
"org.eclipse.fennec.services.client": {
  "eventSource.target": "(ddsr.event.transport=mqtt)"
}
```

Without that target the SDK binds whichever event source is present,
which in any launch that also has REST is the SSE one.

The same bundle also **calls** services announced over a messaging
broker (#98, #138) — with one invoker for MQTT and AMQP. Over messaging
the API is the same for both: publish the call to a channel, receive
the one answer that correlates. What differs is what the flavor says —
what a channel is called, where the broker is, and how the answer finds
its way back (in the envelope over MQTT 3.1.1, in the protocol's own
reply-to over AMQP). The invoker needs no address of its own — the
broker a flavor announces is where it dials, and the scheme of that URL
says which protocol — and is configured with how long to wait and who
the node is to a broker:

`org.eclipse.fennec.services.client.messaging.invoker`

| Property | Default |
| --- | --- |
| `reply.timeout.seconds` | `10` |
| `max.body.bytes` | `0` — an answer's document up to 1 MiB |
| `username`, `password` | empty — the broker's default user |
| `tls.keystore`, `tls.truststore` and their passwords | empty; used on a TLS URL (`ssl://`, `amqps://`) |

A user and a password are stated here and never in a broker URL: the
URL is what a flavor announces to every consumer.

Which protocols the invoker can actually speak is a matter of what the
launch carries: the `mqtt3` binding with Paho, the `amqp` binding with
the RabbitMQ client, or both. That is the whole difference between an
MQTT and an AMQP deployment — configuration and dependencies.

Which transport a proxy uses is not configured at all: the SDK picks by
the flavors the service announces, in the order it announces them, from
the invokers this runtime has. A service reachable only over a
transport that is not installed fails with a message naming both sides
instead of a call going out the wrong way.

## `provider.messaging` — serving a contract over a messaging broker

The messaging twin of `provider.rest`, for MQTT and AMQP alike: one
subscription per operation of a flavor, a dispatcher that reads the
call, invokes the service and answers the way the call asks to be
answered — on the topic it named (MQTT), or on the reply address the
protocol carried (AMQP). Nothing in it is written per contract, and
nothing per protocol beyond that one distinction.

Connections are pooled per broker URL (and exchange), opened with the
first export and closed with the last. Who the node is to a broker is
the connection's to say:

`org.eclipse.fennec.services.provider.messaging.connection`

| Property | Default |
| --- | --- |
| `username`, `password` | empty — the broker's default user |
| `tls.keystore`, `tls.truststore` and their passwords | empty; used on a TLS URL |
| `max.body.bytes` | `0` — a call's document up to 1 MiB |

Over AMQP the reply queue is exclusive to the connection that declared
it, so no other connection can consume from it. The answer is still
routed by a key on the topic exchange; a broker that should also keep
others from binding that key says so in its permissions.

Since #25 it also has the configuration-driven half, the twin of
`provider.rest`: one factory configuration serves one contract from a
model document and, if asked, announces it.

Factory PID `org.eclipse.fennec.services.provider.messaging`

| Property | Default | |
| --- | --- | --- |
| `service.filter` | — | **required**, usually `(ddsr.contract=<name>)` |
| `model.bundle` | — | **required**, the bundle carrying the document |
| `model.entry` | — | **required**, the path inside it |
| `ddsr.contract` | `""` | the contract's name |
| `publish` | `false` | announce to a broker as well as serve |
| `messaging.broker` | `""` | where this instance listens — a URL of the protocol the document's flavor speaks; empty keeps what the document says |
| `broker.url` | `""` | only used to name the contract in the announcement |

Topics, routing keys and QoS are **not** in that table, and that is the
point: they are the contract's, read from the same document that is
announced, whichever protocol its `MessagingFlavor` speaks. What
a deployment states is where it runs.

Unlike the REST twin it goes down in the right order by construction:
the withdrawal is awaited first and the subscription closed afterwards,
so a consumer is told while the topics still answer. A subscription can
do that; a whiteboard application cannot, because Declarative Services
unregisters it before the component's own deactivation runs.

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
| `invokers.target` | `(fennec.invokers=REST)` | the invokers the client waits for — see below |
| `consumer.id` | generated | `consumer-<uuid>` when empty |
| `session.interval.seconds` | `600` | 0 switches sessions off |
| `provider.heartbeat.seconds` | `30` | 0 switches liveness off |

The client has no defaults to start on: it takes its configuration
with `ConfigurationPolicy.REQUIRE`, so a node without a
`org.eclipse.fennec.services.client` configuration has no client.

### A client waits for its invokers

A client tells the broker which flavors it speaks, and a lookup is
filtered by that. Saying it speaks a flavor it has no invoker for gets
it services it cannot call, so the two are tied together:

- One component, the `InvokerProvider`, collects every `ServiceInvoker`
  in the framework and publishes the flavors it has one for as a
  service property, `fennec.invokers` (a `String[]` of flavor kinds).
  An invoker is put in before its flavor is published and taken out
  after the flavor is withdrawn.
- The client has a reference to that service that is never injected —
  it is only waited for. `invokers.target` is its target, and it names
  the same flavors as `supported.flavors`:

  ```json
  "org.eclipse.fennec.services.client": {
      "supported.flavors": "REST,MQTT",
      "invokers.target": "(&(fennec.invokers=REST)(fennec.invokers=MQTT))"
  }
  ```

- Until there is an invoker for each of them the client is not
  activated — `scr:info` shows the reference `invokers` unsatisfied —
  and nothing has been said to the broker. When one of them goes, the
  target stops matching and the client goes with it, session and all.
- The proxy factory binds the same `InvokerProvider`, without a
  target, and asks it for the invoker of a service. A proxy needs a
  locator, and a locator comes from a client, so the client's
  condition covers the factory.

The messaging invoker is one service that speaks `MQTT` and `AMQP`,
and is listed under both. The RSA role writes both properties from
the flavors of the node; a hand-written configuration writes them
itself, as the examples do.

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
| `client.id` | empty — `ddsr-broker-` and the first eight characters of the framework's UUID, so two brokers on one MQTT broker never share one |
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
| `max.body.bytes` | `0` | how much of a request body is read; `0` is 1 MiB |

**Serving and announcing are two components.** The distribution
serves. With `publish` it also says *when* what it serves may be
announced: it registers a condition (`ServedContract`, an OSGi
`Condition` with `osgi.condition.id=fennec.distribution.served`) whose
`publish` property is `false` until a whiteboard lists the application,
and `true` from then on. The announcing is done by a second component,
`ContractAnnouncement` in the client SDK bundle — the same one for
every transport — which binds three things as mandatory references —
that condition with a target on `publish=true`, the client, and the
broker's catalog over REST — announces when it comes up and withdraws
when it goes down.

So the order [Eventing](EVENTING.md) describes is the order
Declarative Services activates and deactivates in: served first, then
told; withdrawn first, then gone. And what an announcement is waiting
for is readable in the component runtime (`scr:info` on
`…client.announce.ContractAnnouncement`) — a missing broker client shows as
an unsatisfied reference instead of as an announcement that silently
never happens.

Nobody configures the announcement: the distribution derives its
configuration (factory PID `…client.announcement`) from its
own and deletes it again, which is why it needs Configuration Admin.
The messaging distribution does the same; there the condition turns
`true` once the messaging broker has the subscriptions.
A whiteboard that refuses the application is reported once, with the
runtime's own reason, and nothing is announced.

## How large a body may be

A body is attacker input, and how much of it a node holds in memory is
not the sender's decision. Every inbound document is therefore read
with a bound, and the bound is **1 MiB** wherever nobody says
otherwise — which is what broker traffic (catalog, publish, lookup)
needs and stays at.

A deployment that serves an operation taking a large model raises it
where it configures the serving part, with `max.body.bytes`:

| Where | What it bounds |
| --- | --- |
| `…provider.rest` (factory) | the request body of the contract it serves |
| `…rsa.distribution.rest` | the request body of every export of this node |
| `…provider.messaging.connection` | the document of a call, and the messages of the connection |
| `…client.messaging.invoker` | the document of an answer, and the messages of the connections |
| the RSA role (`…rsa.provider`, `…rsa.consumer`) | written into the three above ([RSA](RSA.md#two-transports-and-splitting-them)) |

Over REST a body above the limit is refused with **413** before the
implementation is called; anything else the codec cannot read is 400.

Over a messaging broker two limits are in play. The messaging instance
bounds the *message*, the configuration names the *document*: a
document that is not text travels in the envelope as base64, four
bytes for every three, so the instance is configured with that much
more plus room for the envelope. And the messaging broker has a limit
of its own (`message_size_limit` in Mosquitto, `max_message_size` in
RabbitMQ) that this configuration cannot move.

What this does not cover: an **answer read by the REST client** is
bounded at 1 MiB and not configurable yet, and the limit is a
deployment's statement, not the contract's — a consumer cannot read it
off a flavor before calling.

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
