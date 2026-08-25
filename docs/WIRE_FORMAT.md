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
| `GET /registry` | full registry document | detached, consistent snapshot |
| `GET /catalog` | the registry incl. all catalog entries | |
| `GET /catalog/{name}` | one catalog contract | with the `(name, sd1)` key several contracts may share a name: `?fingerprint=sd1:…` addresses one exactly; a bare ambiguous name answers **409** listing the coexisting fingerprints |
| `POST /catalog` | add a catalog entry | idempotent for identical content; a different contract under the same name **coexists**; the OK diagnostic returns the broker-computed sd1 |
| `PUT /catalog/{name}/deprecate` | soft-deprecate | optional body with `deprecationReason`/`replacedBy`; optional `?fingerprint=` |
| `DELETE /catalog/{name}` | remove an entry | strict-reject while live (or cold-cached) implementations reference it; optional `?fingerprint=` |
| `POST /implementations` | publish an implementation | body: `ServiceProvider` (with exactly one implementation) + contract siblings; response: `Diagnostic` |
| `POST /implementations/withdraw` | **canonical withdraw** | a POST, not a body-carrying DELETE — Jersey's client refuses DELETE-with-entity, which silently broke the DELETE variant for the Java client |
| `DELETE /implementations` | legacy withdraw | kept for wire compatibility; do not use from Jersey clients |
| `GET /references?interface=…` | lookup | further params: `filter` (LDAP), `flavors` (CSV), `consumerId`, `fingerprint` (exact contract addressing) |
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
212 not published, 230 session invalid, 300 interface deprecated
(warning), 500 persistence failed.

## XMI document conventions

- **Multi-root documents** use an `xmi:XMI` wrapper; cross-references
  between roots are positional fragments (`/1`, `/1/@operations.0`).
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

`GET /events` streams `ServiceEvent` documents (event name
`ddsr-service-event`), each self-contained: the event root plus the
reference (and provider data) it refers to — an `UNREGISTERING` must be
routable even by a consumer that never saw the registration. A
comment-line heartbeat every 10 s (PID
`org.eclipse.fennec.services.broker.rest.sse`, `heartbeat.seconds`)
keeps intermediaries from idling the connection out and bounds how long
a dead consumer blocks the sender. On every (re)connect the client
pulls a snapshot before processing events (FR-Sync-Reconnect).

## MQTT event transport

The broker publishes the same self-contained event documents to
`ddsr/events/<interface>` (QoS 0 by default, never retained — an event
is a transition, not a state; late subscribers snapshot instead).
Consumers subscribe `ddsr/events/#`. Both sides are dormant OSGi
components (`configurationPolicy = REQUIRE`) with the PIDs
`org.eclipse.fennec.services.broker.mqtt` and
`org.eclipse.fennec.services.client.mqtt` (`broker.url`,
`topic.prefix`, `client.id`, `qos`). Ranking the client transport above
the SSE source (`service.ranking`) makes the SDK switch — the stream is
closed and reopened, and the reopen re-snapshots, so a transport
handover loses no events.

## MQTT invocation (request/response)

Peer-to-peer between consumer and provider over the MQTT broker
announced in `MqttFlavor.brokers` — the DDSR broker is not involved
(discovery/acquisition only). MQTT 3.1.1-compatible: correlation and
reply address travel in the JSON envelope, not in MQTT 5 properties.

```
request topic:   MqttOperationFlavor.requestTopic,
                 else <MqttFlavor.requestTopic>/<operation.name>
reply topic:     consumer-chosen <base>/<correlationId>, with base =
                 MqttOperationFlavor.responseTopic
                 | MqttFlavor.responseTopic
                 | <requestTopic>/reply
request (JSON):  {"correlationId":"<uuid>","replyTo":"<topic>","args":{…}}
response (JSON): {"correlationId":"<uuid>","result":<value>}
                 | {"correlationId":"<uuid>","error":"<message>"}
qos:             MqttOperationFlavor.qos | MqttFlavor.defaultQos
                 | AT_LEAST_ONCE;   retained: never
```

One reply topic per request — a subscription never sees a foreign
answer, and the correlationId double-checks. Provider-side handler
failures answer with the error envelope instead of letting the consumer
time out. Reference implementation:
`ddsr-ts-client/packages/ddsr-transport-mqtt/src/mqtt-rpc.ts`
(consumer plugin `MqttFlavorPlugin`, provider dispatcher
`MqttOperationServer`).
