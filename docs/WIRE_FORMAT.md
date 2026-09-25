# Wire Format Reference

Everything that travels between broker, providers and consumers — the
REST endpoints, the XMI document conventions, the SSE and MQTT event
streams, and the MQTT invocation envelope. Concrete document examples
live in [ARCHITECTURE.md §3](ARCHITECTURE.md); this page is the
reference table.

All wire names below are **frozen** (they predate the
`org.eclipse.fennec.services` namespace and deliberately stayed
stable): base path `/ddsr/rest`, header `X-DDSR-Requestor`, property
prefix `ddsr.`, topic prefix `ddsr/events`, SSE event name
`ddsr-service-event`.

## REST endpoints

Base URL: `http://<host>:8887/ddsr/rest` — everything speaks
`application/xml` (XMI) unless noted.

| Method & path | Purpose | Notes |
|---|---|---|
| `GET /registry` | full registry document | detached, consistent snapshot; multi-root: the registry, then the providers it names, so `implementations` and `providers` resolve within the document (#174) |
| `GET /catalog` | the registry incl. all catalog entries | the same document as `/registry` |
| `GET /catalog/{name}` | one catalog contract | with the `(name, sd1)` key several contracts may share a name: `?fingerprint=sd1:…` addresses one exactly; a bare ambiguous name answers **409** listing the coexisting fingerprints |
| `POST /catalog` | add a catalog entry | idempotent for identical content; a different contract under the same name **coexists**; the OK diagnostic returns the broker-computed sd1 |
| `PUT /catalog/{name}/deprecate` | soft-deprecate | optional body with `deprecationReason`/`replacedBy`; optional `?fingerprint=` |
| `DELETE /catalog/{name}` | remove an entry | strict-reject while live (or cold-cached) implementations reference it; optional `?fingerprint=` |
| `POST /implementations` | publish an implementation | body: `ServiceProvider` (with exactly one implementation) + contract siblings; response: `Diagnostic` |
| `PUT /implementations` | **modify in place** | same body as publish; the broker keeps the reference id and every lease and emits `MODIFIED` — consumers refresh, they do not rebind. The implemented contracts must be the same catalog entries: a changed contract answers **409** (code 214), that is a publish |
| `POST /implementations/withdraw` | **canonical withdraw** | a POST, not a body-carrying DELETE — Jersey's client refuses DELETE-with-entity, which silently broke the DELETE variant for the Java client |
| `DELETE /implementations` | legacy withdraw | kept for wire compatibility; do not use from Jersey clients |
| `GET /references?interface=…` | lookup | further params: `filter` (LDAP), `flavors` (CSV), `consumerId`, `fingerprint` (exact contract addressing). Envelope: `LocalServiceRegistry` with the hit references and provider copies that carry **only** the hit implementations, plus the contracts as sibling roots. A reference is paired with its implementation via the `ddsr.impl.fingerprint` decoration (im1) — the model has no direct pointer, and a provider may hold several versions |
| `PUT /references/{referenceId}/heartbeat?intervalSeconds=N` | **provider liveness** (#52) | no body. Opt-in per registration: the provider SDKs call it on a timer for every live registration; the broker retires a registration after two missed heartbeats (`2 × interval`) with `UNREGISTERING` + `RETIRED`, reason `PROVIDER_LOST`. **404** (code 212) = the broker no longer holds the registration, publish again; **400** (code 215) = non-positive interval |
| `PUT /consumers/{consumerId}` | session **full replace** | acquire = add a reference id and PUT, release = remove and PUT, heartbeat = unchanged PUT (idempotent) |
| `GET /consumers/{consumerId}` | what the broker believes | session + acquired reference-id stubs |
| `DELETE /consumers/{consumerId}` | shutdown-notify | releases all leases |
| `GET /events` | SSE lifecycle stream | optional `?flavors=` narrows by flavor kind |

Governance calls carry the caller identity in the `X-DDSR-Requestor`
header (default `anonymous`).

Diagnostics use stable numeric codes (see
`DdsrDiagnostics`): 200 catalog-has-live-impls, 201 not-found,
202 already-exists (historic, no longer produced), 203 **ambiguous**
(a name-only reference across coexisting contracts), 210
interface-not-in-catalog / contract drift, 211 ownership violation,
212 not published, 213 `replaces` names no live predecessor (warning,
publish went through), 214 a modify tried to change the contract, 230
session invalid, 300 interface deprecated (warning), 500 persistence
failed.

## Payload encoding

A body is XMI unless the contract says otherwise. Since #100 the
operation flavor decides: `consumes` for the request, `produces` for
the response, and they are two different questions — an operation may
take protobuf and answer XML. An encoding is available exactly when
the runtime has an EMF `Resource.Factory` registered for that content
type; one that nobody registered is **refused**, not quietly served as
XMI.

| Content type | Encoding | Registered by |
| --- | --- | --- |
| absent, `application/xml`, `text/xml`, `*+xml` | XMI, the hardened wire resource | always |
| `application/x-protobuf` | protobuf | `org.eclipse.fennec.protobuf` (emf.util) |

**Broker traffic is XMI and stays XMI.** The protobuf factory is in the
client and provider runs and deliberately not in the broker's: the
broker's documents are the cross-language contract and the TypeScript
track cannot read protobuf. A contract that declares protobuf is one
only a Java consumer can use — see
[Transports](TRANSPORTS.md#the-payload-encoding-is-the-contracts-choice).

The **fingerprints are unaffected**. `sd1` and `im1` are computed over
the model in memory, never over serialised bytes: a contract has one
address whatever encoding it happens to travel in, which is the whole
reason a consumer can address it at all.

## XMI document conventions

- **Multi-root documents** use an `xmi:XMI` wrapper; cross-references
  between roots are positional fragments (`/1`, `/1/@operations.0`) —
  never the value of an `iD` attribute. EMF would otherwise write
  `reference="<uuid>"` for a `ServiceReference` (its `id` is an ecore
  iD); the Java codec suppresses that (`XmiCodec.WireResource`) because
  the TypeScript loader resolves paths only, and every broker event
  names its reference this way.
- **Sibling stubs:** a publish body carries the provider root plus its
  contracts as sibling roots (full content — the broker addresses the
  catalog entry BY that content); a session PUT carries the
  `ConsumerSession` root plus `ServiceReference` id-stubs. The model's
  `acquisitions` reference is transient and never serialized.
- Alternatively a publish may reference its contract as a
  cross-document href to the canonical catalog URL
  (`<serviceInterfaces href=".../catalog/Payment"/>`); the broker
  resolves the name from the URL — under the `(name, sd1)` key this
  form requires the name to be unambiguous.
- **EMF default omission:** attributes at their model default are not
  written. Readers must apply model defaults; the fingerprint canonical
  forms render them explicitly.
- The broker rewires published implementations onto its **live catalog
  entries** and decorates every reference with the implementation's
  properties plus `ddsr.fingerprint`(`.…`) and `ddsr.impl.fingerprint`
  ([FINGERPRINTS.md](FINGERPRINTS.md)).

## SSE event stream

`GET /events` streams lifecycle events (frame name
`ddsr-service-event`, media type `application/cloudevents+json`). Each
frame is a CloudEvent in structured mode whose `data` is the
self-contained event document: the event root plus the reference (and
provider data) it refers to — an `UNREGISTERING` must be routable even
by a consumer that never saw the registration. An SSE frame carries no
headers of its own, which is why the envelope travels in the data here
and not as `ce-*` (see [The envelope](#the-envelope-cloudevents-10)). A
comment-line heartbeat every 10 s (PID
`org.eclipse.fennec.services.broker.rest.sse`, `heartbeat.seconds`)
keeps intermediaries from idling the connection out and bounds how long
a dead consumer blocks the sender. On every (re)connect the client
pulls a snapshot before processing events (FR-Sync-Reconnect).

`MODIFIED` announces an in-place change of a registration — endpoint,
properties, capabilities — under the **same** reference id (PUT
`/implementations`); the document carries the refreshed reference, so
a consumer updates what it holds and keeps its lease. `REGISTERED`
after an `UNREGISTERING/REPLACED` is the other case: a new reference
id, re-lookup required.

Every `UNREGISTERING` carries a `reasonCode` telling why the service
went away: `WITHDRAWN` (the provider withdrew it, explicitly or through
its shutdown hook), `REPLACED` (a republish under the same (name,
version) retired the old copy; a `REGISTERED` for the successor follows
immediately), `COLDIFIED` (the idle sweep parked the entry in the cold
cache; it stays discoverable and rehydrates on the next lookup with a
fresh `REGISTERED`), `CUTOVER` (a HARD_CUTOVER grace window elapsed)
and `PROVIDER_LOST` (the provider stopped answering; two missed
heartbeats). `SESSION_EXPIRED` is the one constant that exists and is
never emitted — sessions raise no service events today. `REGISTERED`
and `MODIFIED` carry no reason; the attribute is additive and simply
absent when unset.

## MQTT event transport

The broker publishes the same messages — envelope and document — to
`ddsr/events/<interface>` (QoS 0 by default, never retained — an event
is a transition, not a state; late subscribers snapshot instead).
Consumers subscribe `ddsr/events/#`. Both sides are dormant OSGi
components (`configurationPolicy = REQUIRE`) with the PIDs
`org.eclipse.fennec.services.broker.mqtt` and
`org.eclipse.fennec.services.client.mqtt` (`broker.url`,
`topic.prefix`, `client.id`, `qos`, and on the broker side
`event.source`). A message on `<prefix>/_resync` tells subscribers to
take a fresh snapshot; since #101 it carries an envelope of type
`org.eclipse.fennec.services.resync` and no payload, so a transport
without topics could say the same thing. Which transport the SDK uses is
said in the client's own configuration — `eventSource.target` against
the `ddsr.event.transport` property (`rest` or `mqtt`) — and
deliberately **not** by `service.ranking`. A deployment that configures
a client is describing the setup it expects, not entering a contest,
and a ranking handover would close the open stream and reopen another,
losing whatever is published in the gap. The podman harness scenario D
asserts that exactly one subscription exists and that it is the
configured one.

## What travels with an answer

An operation's contract says what it returns. It cannot say "and
whatever that value needs in order to be readable" — and a lookup
answer is exactly that case: the envelope's implementations point at
the contracts they serve, and a document carrying only the envelope
leaves those references pointing nowhere (#88).

So the wire says it, by a rule rather than a declaration:

> An answer travels with every object it references that belongs to
> nobody — no container and no resource — and, from those, with
> everything they reference on the same terms.

Both halves of "belongs to nobody" carry weight:

- **No container and no resource** describes a copy made for this
  answer. The lookup copies its hits precisely so that nothing live can
  be reached from the wire, and those copies are what would otherwise
  be lost.
- **Anything contained or in a resource** belongs to something that did
  not ask to travel. A live catalog entry sits in the broker's
  registry, so `GET /catalog/{name}` answers with one contract and not
  with the registry behind it.

A proxy is never followed. A cross-document href says the target lives
elsewhere and is not to be resolved here — the publish convention is
built on exactly that.

An answer that needs nothing stays a single root, byte for byte what it
was before the rule existed. One that needs something becomes a
multi-root document whose **first root is the answer**, which is what
both SDKs already read.

## The envelope: CloudEvents 1.0

Since #101 every message this registry sends travels in a CloudEvents
envelope — a lifecycle event, a call and an answer alike. The model is
`io.cloudevents.model` from fennec.common.models; the Java helper is
`org.eclipse.fennec.services.cloudevents`, the TypeScript mirror
`@ddsr/client`'s `cloud-events.ts`.

Two content modes, as the specification defines them, chosen by what
the transport can carry:

| Transport | Mode | Where the attributes are |
|---|---|---|
| SSE, MQTT (and AMQP when it comes) | **structured**, JSON event format | in the message, beside `data` |
| HTTP | **binary** | `ce-*` headers, the body is the payload |

The binary mode is why the REST wire did not change: the body is byte
for byte the payload its contract declares, and the envelope is a
handful of headers around it. `datacontenttype` is the message's own
`Content-Type` there and is never written as a `ce-` header.

In structured mode the payload is a JSON string when its encoding is
textual (XMI is) and `data_base64` when it is not (protobuf is not) —
the JSON event format's own rule, not ours.

### The attributes this registry sets

| Attribute | On an event | On a call | On an answer |
|---|---|---|---|
| `type` | `org.eclipse.fennec.services.<transition>` | `…invoke` | `…invoke.reply` |
| `source` | the broker (`event.source`, default `/fennec/services/broker`) | `/consumer/<origin>` | `/provider/<contract>` |
| `subject` | the reference id | the operation, with its contract where the caller knows it | the call's subject |
| `time` | when the transition happened, not when it was sent | when the call was made | — |
| `datacontenttype` | `application/xml` | what the contract declares (#100) | likewise |

`<transition>` is the `ServiceEventType` literal in lower case with
`_` as `.`: `registered`, `modified`, `unregistering`,
`modified.endmatch`, `upgrade.available`, `retired`, plus
`org.eclipse.fennec.services.resync` for the signal that a subscriber
has to re-read.

Two extension attributes, because CloudEvents defines neither and MQTT
3.1.1 cannot supply them:

- `replyto` — the topic an answer is expected on
- `correlationid` — the id of the request an answer belongs to

### Where it appears on the REST side, and where it does not

A service invocation carries it in both directions: the consumer's
invoker sends the `ce-*` headers, the generic distribution's dispatcher
answers with the reply pair. Since the broker's own `/catalog` and
`/implementations` are served by that same distribution (#76), their
answers carry a reply envelope too.

What carries none: the SDK's broker proxies do not put an envelope on
their requests (a catalog read is not an invocation of a contract this
registry brokered), and the two hand-written resources — the lookup
answer and the event stream — are not dispatched, so no envelope is
added around them. None of that is load-bearing: binary mode is
additive, and every one of those answers is readable exactly as it was
before.

A reader that meets a `type` it does not know has an envelope it
understands carrying something it does not. Both SDKs skip such a
message and say so at FINE: sharing a transport with somebody else's
events is normal, not an error.

## MQTT invocation (request/response)

Peer-to-peer between consumer and provider over the MQTT broker
announced in `MqttFlavor.brokers` — the DDSR broker is not involved
(discovery/acquisition only). A call is **two events**: the request and
its answer, held together by `correlationid` rather than by a
connection.

```
request topic:   MqttOperationFlavor.requestTopic,
                 else <MqttFlavor.requestTopic>/<operation.name>
reply topic:     <base>/<consumer>/<request id>, with base =
                 MqttOperationFlavor.responseTopic
                 | MqttFlavor.responseTopic
                 | <requestTopic>/reply
request:         CloudEvent, structured mode
                 type = org.eclipse.fennec.services.invoke
                 replyto = the reply topic
                 data = a ServiceInvocation document
response:        CloudEvent, structured mode
                 type = org.eclipse.fennec.services.invoke.reply
                 correlationid = the request's id
                 data = a ServiceInvocationResult document
qos:             MqttOperationFlavor.qos | MqttFlavor.defaultQos
                 | AT_LEAST_ONCE;   retained: never
```

The payload is the model, not a bag of JSON values.
`ServiceInvocation` names the operation and carries one `Argument` per
value, each in the `Property` that fits its declared type — so an `int`
stays an `int`, and a **modelled** argument is possible at all: an
`EObjectProperty` contains its value, and the model travels with the
message. The answer is a `ServiceInvocationResult`: a value, or a
`Diagnostic` that says why there is none, which is how this registry
reports failures everywhere else.

The encoding is the contract's choice here as well (#100):
`datacontenttype` comes from the operation flavor's `consumes` for the
call and `produces` for the answer. The TypeScript side can write only
XMI on this path today, and a contract declaring anything else is
**refused** rather than served XMI under a label nobody checks — the
same rule the XMI codec applies to a content type nothing is
registered for. A refusal travels back as a `Diagnostic`, in XMI: it is
not the operation's declared result, so `produces` does not describe
it, and a caller that learns why beats one that waits out its timeout.

Because `ServiceInvocation` points at its operation and each `Argument`
at its parameter — by reference, deliberately — the document carries a
second root describing the operation being called, and the references
resolve inside it. Self-contained, like every other document on this
wire.

### Who may read what

The consumer's own segment in the reply topic is what makes the
separation enforceable. A topic separated only by an unguessable id
keeps peers apart by obscurity: a broker cannot be told who may read
what, because there is no name to write the rule against. With the
segment, an ACL is two lines:

```
consumer:  publish   <prefix>/req/#
           subscribe <prefix>/res/+/+/<me>/#
provider:  subscribe <prefix>/req/<its own name>/#
           publish   <prefix>/res/#
```

Requests and answers live in **separate trees** for the same reason —
"may call this provider" and "may read what it answered" are two
permissions, and nesting the answers under the request topic would make
them one. The RSA distribution names both (`<prefix>/req/…`,
`<prefix>/res/…`); a hand-written flavor that names only a request
topic gets `<requestTopic>/reply` as before, which works and cannot be
separated by an ACL.

A name is sanitised before it becomes a topic level: `/`, `+` and `#`
are replaced, so a consumer calling itself `a/#` cannot name a subtree
it was never given.

One subtree per consumer, not one topic per request: a caller
subscribes once and `correlationid` tells its calls apart, which is
also what lets several be in flight at once.

A subscription never sees a foreign answer, and `correlationid`
double-checks. A provider-side handler
failure answers with the Diagnostic instead of letting the consumer
time out. Reference implementation:
`ddsr-ts-client/packages/ddsr-transport-mqtt/src/mqtt-rpc.ts`
(consumer plugin `MqttFlavorPlugin`, provider dispatcher
`MqttOperationServer`), with the invocation codec in
`@ddsr/client`'s `invocation.ts`.

Both languages speak this path since #98. What differs is what each can
encode: the Java codec is content-type driven (#100), so a Java end
honours a contract declaring protobuf; the TypeScript end can write only
XMI on MQTT and **refuses** a contract that declares anything else
rather than sending XMI under another name.
