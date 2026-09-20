# DDSR — Wire Channels, Interaction Models, Capability/Requirement Matching

**Status:** a design discussion. The model sketch stands, several open design questions are marked. Implementation as a parallel v2 in the workspace (see §9).
**Last updated:** 2026-07-02.

Specifies the wire channel model, the interaction styles (request/response, streaming, bidirectional), the `OperationChannel` notion including correlation strategies, and the general capability/requirement matching between provider and consumer. Complements [UPDATE_POLICY.md](UPDATE_POLICY.md), which uses the stream termination mechanics from this document.

---

## 1. Motivation

Where things stand today (see [ARCHITECTURE.md §2.5](ARCHITECTURE.md)):

- An operation has a `RestOperationFlavor` that bundles HTTP method, path and content type.
- An implicit assumption everywhere: **request → response, synchronous, one round trip, the same channel.**
- Operations with `Cardinality 0..*` mean "a list as the return value", not "a stream over time". The model cannot tell a finite list from an endless stream.

Three design goals for the next iteration:

1. **Interaction styles as first class:** request/response, fire-and-forget, event stream, bidirectional stream as a declarative choice on the operation.
2. **Channels as equal wire constructs:** the request channel and the response channel need not be the same flavor. Async RPC (request over REST, response over a topic) becomes conceptually clean.
3. **Capability/requirement matching** as a general provider↔consumer resolution system, instead of `supportedFlavors` as a special case.

A substantial model extension follows from that. It is developed alongside the existing v1 model (§9), not as one giant refactor.

## 2. The channel model — new classes and structures

A sketch (provisional naming, no ecore code):

```
ServiceImplementation
├── publishedVia       : Flavor           (1; how the implementation registered with the broker)
├── discoverableVia    : Flavor[*]        (over which channels the broker makes it findable — usually = the broker default)
├── capabilities       : Capability[*]    (see §6)
└── operationFlavors   : OperationFlavor[*]

OperationFlavor
├── operation          : ServiceOperation (cross-reference into the catalog)
├── interactionStyle   : InteractionStyle (a declarative hint, see §3)
├── requestChannel     : OperationChannel?   (nil for subscribe-only streams)
├── responseChannel    : OperationChannel?   (nil for FIRE_AND_FORGET)
└── streamOptions      : StreamOptions?      (set when one of the channels is of stream type)

OperationChannel
├── flavor             : Flavor              (RestFlavor, MqttFlavor, GrpcFlavor, …)
├── correlationStrategy: CorrelationStrategy (see §5)
└── timeout            : Duration?           (optional, default is flavor specific)

StreamOptions
├── backpressure       : BackPressureStrategy
├── bufferSize         : int
├── delivery           : DeliveryGuarantee
└── keepAliveInterval  : Duration

InteractionStyle : enum { REQUEST_RESPONSE, FIRE_AND_FORGET, EVENT_STREAM, COMMAND_STREAM, BIDIRECTIONAL_STREAM }
CorrelationStrategy : enum { SYNCHRONOUS, HEADER_BASED, TOPIC_BASED, NONE }
BackPressureStrategy : enum { NONE, DROP_OLDEST, DROP_NEWEST, BLOCK }
DeliveryGuarantee : enum { AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE }
```

`Flavor` itself becomes the abstract base class that `RestFlavor`, `MqttFlavor`, `GrpcFlavor` and friends inherit from (today `RestFlavor` is already a concrete subclass, which fits). Every flavor subclass carries its transport-specific properties (host, base path for REST; broker, topic for MQTT; and so on) and declares the interaction styles its transport can carry as a capability (see §6, §7).

`publishedVia`/`discoverableVia` and the `operationFlavors`/`OperationChannel` structure can be written by hand, but also **derived from an imported OpenAPI/AsyncAPI contract** — the mapping is in §12.

## 3. InteractionStyle — a declarative hint, not the primary axis

With the channel generalisation, the "style" of an operation strictly follows from the channel configuration:

- Both channels present, a single round trip, synchronous correlation → `REQUEST_RESPONSE`
- Only a request channel → `FIRE_AND_FORGET`
- Only a response channel, long lived → `EVENT_STREAM`
- Both channels, both long lived → `BIDIRECTIONAL_STREAM`

`InteractionStyle` as a model property is therefore **redundant with the channel configuration**, and is **kept anyway** for:

- **Readability** — the author says what they mean; a generator or a reader does not have to decode the channel configuration to see the intent.
- **Validatability** — a model validator can find inconsistencies ("the style says `FIRE_AND_FORGET` but `responseChannel != null` → a model error").
- **Migration** — today's models (all implicitly `REQUEST_RESPONSE`) get a clean anchor without a migration having to change the channel structure.

## 4. Symmetric vs. asymmetric channels

**Symmetric (the default shorthand):** `requestChannel` and `responseChannel` reference **the same `OperationChannel` object**. In XMI that is a cross-document href to the same place. This covers about 80% of today's operations (synchronous REST).

**Asymmetric (split channel):** two different `OperationChannel` objects, possibly with different flavors. Necessary for:

- Async RPC: the request over REST POST `/start-job`, the response over an MQTT topic or an SSE stream (the webhook pattern, `CompletableFuture` style).
- Streaming patterns: the request is a one-off subscribe over REST, the response is an open event stream over a separate channel.
- Cross-transport patterns: the consumer sits behind a firewall, the request goes out over HTTP, the response comes back over a persistent MQTT connection.

**The shorthand rule in the model:** when `responseChannel` is not set and `interactionStyle == REQUEST_RESPONSE`, it defaults to the same object as `requestChannel`. When the two differ, a correlation strategy becomes mandatory (see §5).

## 5. Correlation strategies

With a split channel the producer has to know **which response belongs to which request**. Four strategies:

| Strategy | Mechanism | Example flavor |
|---|---|---|
| `SYNCHRONOUS` | The answer comes back on the same connection / in the same RPC frame. No id needed. | REST GET/POST, gRPC unary |
| `HEADER_BASED` | A UUID/correlation id in the request header or a message property, which the producer copies into the response. | MQTT message property `correlation-data`, JMS `JMSCorrelationID`, AMQP RPC |
| `TOPIC_BASED` | The consumer opens a response topic and attaches its identity to the request (`reply-to`). The producer publishes to that topic. | The MQTT request/response pattern with `response-topic` |
| `NONE` | No correlation; applies only to `FIRE_AND_FORGET` and to some streams, where the identity is implicit in the subscription. | A pure event publish |

The strategy is a property of `OperationChannel` (more precisely of the response channel, because that is where the "where does it arrive" logic sits). The consumer facade implements the correlation routing transparently — the application code sees only its `Future<T>` or `Flux<T>`, whether the wire path is synchronous, async-header or topic based.

**Implementation consequence:** today's `RestServiceInvoker` is a `SYNCHRONOUS` implementation. `HEADER_BASED` and `TOPIC_BASED` need an `AsyncInvoker` with a pending-call map (`correlation-id → CompletableFuture<Response>`), timeout cleanup, and a subscription on the response channel.

**Status 2026-08-25 (A2 stage 2, issue #3):** exactly this
`TOPIC_BASED` variant is implemented in the v1 model — on the TypeScript
side as `MqttFlavorPlugin` (the consumer, a pending map over the
correlationId plus a per-request reply topic) and `MqttOperationServer`
(the provider dispatch). Since Paho v3 speaks only MQTT 3.1.1 (no
`response-topic`/`correlation-data` properties), both travel in the
request envelope; the frozen convention lives in
`ddsr-ts-client/packages/ddsr-transport-mqtt/src/mqtt-rpc.ts` and is
documented in ARCHITECTURE.md §3. The FR-P4 harness (scenario E)
proves the invocation over a real Mosquitto.

## 6. Capability/requirement matching

Instead of carrying `supportedFlavors` (today) as a special filter, **DDSR models a general capability/requirement system** along the lines of OSGi `Provide-Capability` / `Require-Capability`. Provider and consumer both declare lists, and the broker resolves.

### 6.1 The basic form

```
Capability
├── namespace : String          (e.g. "ddsr.transport", "ddsr.contentType", "ddsr.version")
└── attributes : Map<String, String>

Requirement
├── namespace : String          (matched against Capability.namespace)
└── filter    : LdapFilter      (an OSGi-style LDAP filter over the attributes)
```

**Examples:**

A provider declares on `ServiceImplementation.capabilities`:

```
namespace = "ddsr.transport",      attributes = { version="mqtt-v5", qos="2" }
namespace = "ddsr.contentType",    attributes = { type="application/json+cloudevents" }
namespace = "ddsr.interaction",    attributes = { style="EVENT_STREAM,BIDIRECTIONAL_STREAM" }
```

A consumer declares on `ConsumerCapability.requirements`:

```
namespace = "ddsr.transport",      filter = "(version=mqtt-*)"
namespace = "ddsr.contentType",    filter = "(type=application/*)"
namespace = "ddsr.interaction",    filter = "(style=EVENT_STREAM)"
```

The broker resolves at lookup: return only implementations whose `capabilities` satisfy every consumer `requirement`.

### 6.2 What capabilities express

- **Transport versions:** `mqtt-v3.1`, `mqtt-v5`, `http/1.1`, `http/2`, `grpc-v1.x`
- **Content types:** `application/json`, `application/cloudevents+json`, `application/xml` and so on. With wildcards in the requirement.
- **Codecs:** XMI, JSON, protobuf, Avro
- **Quality-of-service guarantees:** at-most-once vs at-least-once and so on (overlaps with `StreamOptions.delivery`, but there it is a *request* and here a *guarantee*)
- **Security modes:** `tls-v1.3`, `mtls` and so on
- **Compression:** `gzip`, `zstd`
- **Provider identity / region / tenant** — everything that is hand-wired through service properties today

### 6.3 Relation to today's `supportedFlavors`

Today's `ConsumerCapability.supportedFlavors = [REST]` becomes a requirement:

```
namespace = "ddsr.transport", filter = "(family=rest)"
```

`supportedFlavors` is therefore the **primitive predecessor** of the capability system. After the migration to v2 (see §9) it is gone.

### 6.4 Relation to the OSGi model

Deliberately modelled on OSGi:

- The namespace concept is 1:1.
- The LDAP filter syntax is 1:1 (`(&(attr=val)(other<=5))`).
- The resolution semantics are 1:1: every requirement must be satisfied, wildcards in the filter are matched against attribute values.

What is different:

- There is no version range hopping as with OSGi imports — service versions are governed separately in [UPDATE_POLICY.md](UPDATE_POLICY.md).
- Resolution happens at lookup time on the broker, not at bundle wiring time in the resolver.

## 7. Cross-axis constraints: which flavor can carry which style?

Not every flavor can carry every interaction style. That is declared through flavor capabilities:

```
RestFlavor capabilities:
  ddsr.interaction.styles : { REQUEST_RESPONSE, FIRE_AND_FORGET, EVENT_STREAM (via SSE) }

MqttFlavor capabilities:
  ddsr.interaction.styles : { ALL }
  ddsr.correlation.strategies : { TOPIC_BASED, HEADER_BASED, NONE }

GrpcFlavor capabilities:
  ddsr.interaction.styles : { REQUEST_RESPONSE, EVENT_STREAM, COMMAND_STREAM, BIDIRECTIONAL_STREAM }
  ddsr.correlation.strategies : { SYNCHRONOUS, HEADER_BASED (via metadata) }
```

A model validator checks at publish: if `OperationFlavor.interactionStyle == BIDIRECTIONAL_STREAM` and `requestChannel.flavor` is a subclass of `RestFlavor` → an error (REST cannot do that). The publish hook (`PublishHook` from REQUIREMENTS.md) is the natural attachment point.

## 8. Stream termination — a wire protocol for a graceful close

Both [UPDATE_POLICY.md](UPDATE_POLICY.md) (HARD_CUTOVER on streams) and ordinary service shutdown need a clean mechanism for closing an open stream.

**Requirements:**

- Both sides (consumer and provider) must be able to end the stream independently.
- A reason code must be transmittable (for the UI, retry logic, logging).
- Whoever sends first, the other has to answer and clean up.

**Protocol:**

```
StreamCloseFrame
├── reason : CloseReason
└── message : String?    (free-text detail, optional)

CloseReason : enum {
  CONSUMER_LEFT,        // initiated by the consumer (a regular unsubscribe)
  PROVIDER_RETIRED,     // initiated by the provider (a service shutdown)
  BROKER_CUTOVER,       // the broker sent UNREGISTERING (update policy)
  IDLE_TIMEOUT,         // no activity for longer than keepAliveInterval
  PROTOCOL_VIOLATION,   // the other side violated the wire format
  INTERNAL_ERROR        // catch-all
}
```

**Flow:**

1. One side (say the provider) decides to end the stream. It sends `StreamCloseFrame(reason=PROVIDER_RETIRED)` on the stream.
2. The other side (the consumer) receives the frame, calls its application code (`onStreamClose(reason)`), and sends its own `StreamCloseFrame(reason=CONSUMER_LEFT, ackOf=…)`.
3. Both sides release their resources (buffers, subscriptions, heartbeat timers).

Per flavor-specific mapping:

- **MQTT:** `StreamCloseFrame` as its own topic suffix (`/control/close`) or as a special message property.
- **gRPC:** the native half-close / status code, mapped onto `CloseReason`.
- **WebSocket:** the native close frame with a status code; the wire format wraps `CloseReason` inside it.
- **REST+SSE:** the server sends `event: close\ndata: {...}` and closes the SSE connection. The consumer can unsubscribe on its side through a separate POST.

## 9. Implementation strategy — a parallel workspace, not one giant refactor

The model sketched above is substantially bigger than today's. Rather than refactoring the existing v1 model and the broker, the new model is built as a **parallel workspace**:

```
org.eclipse.fennec.services.model            (v1, stays)
org.eclipse.fennec.services.broker.core      (v1, stays)
…
org.eclipse.fennec.services.model.v2         (new — the complete ecore including the channel model)
org.eclipse.fennec.services.broker.core.v2   (new — a broker on the v2 model, with a capability resolver and stream support)
```

Both versions run side by side in the same bnd workspace. v2 bundles can read the v1 wire format (for migration and mixed setups) but publish in v2 form. Cross-checks against v1 are possible through the demo: the same `payment.charge(10.0, "EUR")` has to give the same result against a v1 broker and a v2 broker.

Once v2 has stabilised: the v1 bundles are retired and the workspace default build depends only on v2. Model code generation switches to the v2 `.genmodel`.

**What goes straight into the v2 model (no second jump):**

- The channel model from §2
- The `InteractionStyle`, `CorrelationStrategy`, `BackPressureStrategy`, `DeliveryGuarantee` enums
- `Capability` / `Requirement` with an LDAP filter
- `updatePolicy`, `replaces`, `deprecated`, `cutoverGraceMillis` from [UPDATE_POLICY.md](UPDATE_POLICY.md)
- `Consumer` with `activeRefs` plus heartbeat fields
- `StreamCloseFrame` as a wire construct (its own ecore? or a class in the model?)

## 10. Use case catalogue — `TODO`

Before the stream paths are implemented concretely, the team use case workshop collects real examples here. This list is **deliberately empty** and will be filled at the next meeting — the architecture decisions below depend on it.

```
// TODO: use case 1 — a real stream service (live telemetry? order status
//        updates? a catalog subscription for auto rebind?)
// TODO: use case 2 — async long-running with a webhook response
// TODO: use case 3 — bidirectional: the consumer steers producer state and
//        receives an event stream in parallel
// TODO: use case 4 — pure event publish (producer only, no control channel)
```

**Architecture decisions that can only be made with use cases:**

- **Resource lifecycle on streams:** who closes a stream subscription when the provider behind it performs a `HARD_CUTOVER` mid-stream? The broker triggers the `UNREGISTERING`; but who is responsible for handing state over to the new provider (a last-event-id for resumable streams, say)? Without a concrete use case this stays open.
- **State recovery after a reconnect:** after a brief network outage on a stream — does the consumer want the whole stream again from the beginning, from the last event, or from the moment of the reconnect? That depends on the use case.
- **Multi-subscriber semantics:** an event stream service has several subscribers. Do they all get the same events (fanout), or are events partitioned (the worker pattern)? The model can certainly express both, but the default deserves thought.

## 11. Open design questions

- **The default `CorrelationStrategy`:** `SYNCHRONOUS` for REST, `HEADER_BASED` for MQTT? Defaulted per flavor subclass, or explicitly required?
- **Who chooses between several `discoverableVia` options?** If a provider is findable both through the REST catalog and through MQTT discovery, and the consumer can do both — who decides? Gut feeling: the consumer has a `preferredDiscovery` ordering and the broker respects it. But that moves resolution logic into the consumer, which is not where it has been.
- **Capability namespace conventions:** a registry-style document for the official namespaces (`ddsr.transport`, `ddsr.contentType`, …) — its own document, or inline here?
- **Resolution performance:** evaluating LDAP filters per lookup is fine for a manageable catalog, but at 1000+ implementations we need index structures over the capability namespaces. A stage-4 optimisation.
- **Flavor subclasses vs. one generic flavor with capabilities:** instead of `RestFlavor extends Flavor` and `MqttFlavor extends Flavor` as hard-coded subclasses, one could declare *one* generic `Flavor` with capabilities (`ddsr.transport.family=rest`). The trade-off: more extensible vs. less type safety. Today we have subclasses — should we keep them?
- **`OperationChannel` identity:** with the symmetric default, `requestChannel` and `responseChannel` are the same object in the XMI (through a cross-reference). Is that stable across a reload (EMF identity), or do we have to make sure the identity survives an `EcoreUtil.copy`?

## 12. Contract import (OpenAPI / AsyncAPI) and stub-free consumption in the v2 model

Two connected ideas that the v2 channel model picks up directly. On the requirements side: `REQUIREMENTS.md` §5 (FR-Rev-OpenAPI / FR-Rev-AsyncAPI / FR-Rev-Contract-*, "dynamic (stub-free) consumption" / FR-Dynamic-*) and §8 deferred; backlog: [OPEN_ISSUES.md](OPEN_ISSUES.md) A7 (ingestion) and A8 (the dynamic consumer).

**Important:** OpenAPI and AsyncAPI are *import and description formats*, **not** a new flavor. The derived transport is an ordinary `RestFlavor` or `MqttFlavor` — no new flavor subclass appears.

### 12.1 Contract → v2 flavor/channel mapping

- **OpenAPI → REST.** The document supplies catalog `ServiceInterface`s; the publishing `ServiceImplementation` gets `publishedVia`/`discoverableVia` = the derived `RestFlavor`. Every OpenAPI operation (`path` + method) → one `OperationFlavor` with `interactionStyle = REQUEST_RESPONSE` and a **symmetric** `OperationChannel` (§4): `flavor = RestFlavor`, `correlationStrategy = SYNCHRONOUS`. That matches today's v1 behaviour.
- **AsyncAPI → MQTT/async.** `channels`/`messages` → `OperationFlavor`s. **This is where the v2 model starts to carry weight:** AsyncAPI operations are typically `EVENT_STREAM` or `FIRE_AND_FORGET`, and the request and response channels sit on *different* topics → an **asymmetric split channel** (§4) with `MqttFlavor`. AsyncAPI `replyTo`/`correlationId` map straight onto `CorrelationStrategy = TOPIC_BASED` or `HEADER_BASED` (§5); QoS/delivery → `StreamOptions.delivery` (§2).
- **Why into the v2 model and not (only) v1:** v1 knows only the synchronous REST round trip. An AsyncAPI stream cannot be expressed in v1 without loss — it needs the interaction style and channel axis from §2/§3. The `RestFlavor` branch of an OpenAPI import, by contrast, already works against v1.
- **Validation:** the cross-axis validator from §7 applies automatically — an AsyncAPI stream operation accidentally mapped onto a `RestFlavor` fails at publish through the `PublishHook`.

### 12.2 A stub-free (dynamic) consumer through the v2 model

The v2 model makes the stub-free consumer (FR-Dynamic-*) **stronger** than v1: `OperationFlavor` plus channels plus `interactionStyle` are a complete wire description. `invoke(reference, operationName, args)` reads them at runtime and picks the matching invoker:

- `SYNCHRONOUS` → today's `RestServiceInvoker`.
- `HEADER_BASED` / `TOPIC_BASED` → the `AsyncInvoker` with a pending-call map (§5).

"No generated types" means, concretely, in the v2 context: the invoker takes `OperationChannel.flavor` plus `correlationStrategy` plus `interactionStyle` from the model instead of from generated code. The application code sees — depending on the style — a `Future<T>` or a `Flux<T>` without a stub ever having been generated. The correlation abstraction from §5 is therefore the lever that carries the dynamic consumer beyond synchronous REST.

### 12.3 Contract metadata → capabilities

OpenAPI and AsyncAPI are richer than the DDSR core model. Rather than discarding that richness silently (which would violate FR-Rev-Lossless-or-Diagnostic), it is stored as a `Capability` (§6):

- Content types (OpenAPI `content`) → `ddsr.contentType`.
- Security schemes (OAuth2, apiKey, mTLS) → `ddsr.security`.
- Transport version (http/1.1 vs. http/2; mqtt-v3.1 vs. mqtt-v5) → `ddsr.transport`.
- AsyncAPI QoS/delivery → `ddsr.transport` or `StreamOptions`.

An imported contract is thereby immediately usable in the capability/requirement resolver (§6): a consumer with `requirement (type=application/json)` matches against the capability derived from the OpenAPI `content` — with no special path for imported services.

> An open question (related to §11, "who chooses between several `discoverableVia` options?"): whether an imported contract that declares several servers or transports produces several `discoverableVia` flavors, and who then makes the choice on the consumer side.

## 13. References

- [ARCHITECTURE.md](ARCHITECTURE.md) — where v1 stands today, especially §2.5 (the reflective proxy and the ServiceInvoker)
- [UPDATE_POLICY.md](UPDATE_POLICY.md) — the update policy uses the stream termination from §8, and the heartbeat protocol
- [OPEN_ISSUES.md](OPEN_ISSUES.md) — A1 (SSE/event stream), W3 (mixed body arguments), A2 (MQTT flavor), A7 (OpenAPI/AsyncAPI ingestion, → §12), A8 (the dynamic stub-free consumer, → §12)
- [REQUIREMENTS.md](REQUIREMENTS.md) — the cross-language demo flow, the hook architecture, FR-Rev-OpenAPI/AsyncAPI and FR-Dynamic-* (→ §12)
- OSGi Core Spec, chapter "Provide-Capability / Require-Capability" — the model for §6
