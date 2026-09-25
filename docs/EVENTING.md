# Eventing

A consumer that only ever looked things up would be working from a
stale answer. The broker therefore tells subscribers when the answer
changes: a service appeared, changed, is going away, or was taken away.

Two transports carry the same events, and the payload is byte-identical
between them. Which one a deployment uses is a configuration decision,
not a contest.

## What an event is

One model object, `ServiceEvent`, with four fields: the **type**, the
**reference** it is about, a **timestamp**, and an optional
**reasonCode**.

What travels is not just that object. It is a self-contained XMI
document: the event plus copies of the reference, its implementation,
its provider and its contracts. Self-contained matters most in the one
case where it is hardest — an `UNREGISTERING` is emitted when the
implementation has already been detached, so a document that referred
to live broker state would refer to nothing.

That document travels inside a **CloudEvents 1.0 envelope** (#101), in
structured mode on both transports. The envelope says which transition
this is (`type`), which reference it is about (`subject`), when it
happened (`time`) and what the payload is encoded in
(`datacontenttype`) — so a consumer can route a message without
knowing our format, and only has to know ours once it opens `data`.
The attributes are listed in [Wire format](WIRE_FORMAT.md).

### The types

| Type | Meaning |
| --- | --- |
| `REGISTERED` | a service appeared |
| `MODIFIED` | its properties changed, it still matches your filter |
| `MODIFIED_ENDMATCH` | its properties changed and it no longer matches |
| `UNREGISTERING` | it is being removed |
| `RETIRED` | it is gone from the registry; leases on it are released |
| `UPGRADE_AVAILABLE` | a successor was published under a draining policy; the old one still serves |

The first four follow OSGi. `UPGRADE_AVAILABLE` and `RETIRED` are this
registry's additions — see [Update policies](UPDATE_POLICY.md). There
is also an `UNSPECIFIED` literal with value 0, which exists only so
that none of the meaningful ones is EMF's default and therefore omitted
on the wire.

`MODIFIED_ENDMATCH` is in the model but nothing emits it today.

### The reason codes

`REGISTERED` and `MODIFIED` carry no reason. Every `UNREGISTERING`
does:

| Reason | What happened |
| --- | --- |
| `WITHDRAWN` | the provider withdrew, explicitly or on shutdown |
| `REPLACED` | superseded by a republish of the same identity, or a drain finished |
| `CUTOVER` | a hard cutover's grace window elapsed |
| `COLDIFIED` | the idle sweep parked it; it comes back on the next lookup |
| `PROVIDER_LOST` | the provider stopped answering |

`SESSION_EXPIRED` exists as a constant and is not emitted: sessions do
not raise service events today.

The field is a free-form string on purpose, so a transport or a future
policy can add a token without a model change.

## SSE

Served by `…broker.rest`:

```
GET http://localhost:8887/ddsr/rest/events
Accept: text/event-stream
```

- SSE event name: **`ddsr-service-event`** — the name of the stream,
  unchanged, and one of the frozen wire names
- data media type: `application/cloudevents+json` — the frame carries a
  CloudEvent in structured mode; an SSE frame has no headers of its
  own, so the envelope travels in the data. The event document is its
  `data`, still XMI (`datacontenttype: application/xml`)
- optional `?flavors=REST,MQTT` to be sent only events for those flavor
  kinds. Unknown tokens are ignored with a warning rather than
  rejecting the connection.
- optional `?consumerId=…` to say who is listening. The SDK sends it
  automatically. It buys one thing: losing the connection then shortens
  that consumer's session deadline instead of waiting out the full
  expiry. Leaving it out changes nothing else.

The stream sends a `: keepalive` comment every 10 seconds by default
(`org.eclipse.fennec.services.broker.rest.sse`, `heartbeat.seconds`).
That is not a liveness protocol. It keeps intermediaries from idling
the connection out, it lets a blocked reader wake up so a client-side
close can finish, and it is how the broker notices subscribers that
have gone away.

**Nothing is sent on connect.** No snapshot, no replay, no sequence
numbers, and the broker keeps no per-client history. The client pulls
instead: when the stream is established, the SDK re-reads the
references for every interface it is listening to. That happens on the
first connect as well as on every reconnect.

The Java client reconnects on a fixed delay, 3 seconds by default
(`org.eclipse.fennec.services.client.rest.events`,
`reconnect.seconds`). There is no backoff and no `Last-Event-ID`,
because there is nothing to resume.

Every answer is reconnected to except one. **204 No Content stops the
stream**, which is how a server tells an event stream client to stop
(WHATWG EventSource). The client then neither reports the stream as
established nor reconnects on its own. It opens a new stream only when
one is asked for again, for example by a new listener. Both SDKs behave
this way (#171). Jersey's own `SseEventSource` lacked it until
eclipse-ee4j/jersey#6119, and neither SDK uses that class. Our broker
never answers 204 on this endpoint, so in practice the 204 comes from a
proxy or a foreign server.

## MQTT

Publisher `…broker.mqtt`, subscriber `…client.mqtt`. Both are dormant
until configured — their configuration policy requires a configuration.

- topic: `ddsr/events/<interfaceName>`, one publish per interface the
  service serves
- when the interface cannot be determined, which is the normal case for
  `UNREGISTERING`: `ddsr/events/_unknown`
- QoS **0** by default, **not retained**
- payload: the same message as SSE — a CloudEvent in structured mode
  carrying the XMI document, UTF-8
- `ddsr/events/_resync` says an event did not reach the wire and
  everything should be re-read. It carries an envelope of type
  `org.eclipse.fennec.services.resync` and no payload

Not retained is deliberate. An event describes a transition, not a
state. A late subscriber must not be told about a registration that was
withdrawn long ago; it pulls a snapshot instead.

The subscriber takes `ddsr/events/#` — the whole subtree — because the
`EventSource` contract carries no interest set. It works, and it moves
bytes it could avoid.

## Choosing the transport

In the client's own configuration, by target filter:

```json
{
  "org.eclipse.fennec.services.client.mqtt": { "broker.url": "tcp://localhost:1883" },
  "org.eclipse.fennec.services.client": {
    "eventSource.target": "(ddsr.event.transport=mqtt)"
  }
}
```

**Not by service ranking.** A deployment that configures a client is
describing the setup it expects, not entering a competition. Ranking
two transports against each other also has a concrete failure: the
switch closes one stream and opens another, and whatever is published
in the gap reaches nobody.

## What you are guaranteed

- **An event is emitted only after the change behind it was saved.** A
  mutation that could not be persisted is rolled back and never
  announced. The one exception is the cold-cache sweep, which is
  degraded-but-recoverable by design and announces anyway.
- **Events for one service arrive in the order the changes happened.**
  The broker emits while it still holds its write lock, which is also
  why a subscriber must not block: foreign code is running under that
  lock.
- **You are told before the endpoint goes.** A provider shutting down
  blocks on the broker's acknowledgement before it stops serving. This
  is FR-P3, and it is what makes a withdrawal something you hear about
  rather than something you collide with.

## What you are not guaranteed

- **No replay and no history.** Miss an event and you recover by
  re-reading, not by resuming. That is why reconnect pulls a snapshot.
- **No cross-service ordering.** The ordering guarantee is per service.
- **No delivery acknowledgement.** A subscriber that cannot be written
  to is dropped. MQTT at QoS 0 is at-most-once.
- **Over-delivery happens, on purpose.** When the broker cannot tell
  which flavors or interfaces an event concerns, it delivers to
  everyone rather than guessing. An extra event is recoverable; a
  dropped `UNREGISTERING` leaves a consumer bound to something that is
  gone.
- **A lease is not a contract.** The broker may forget leases, and a
  restart does. Handle `RETIRED` even while holding one.
- **One known FR-P3 violation.** The generic REST distribution
  (`…provider.rest`) is unregistered by Declarative Services before its
  deactivate runs, so its endpoint stops before the withdrawal is
  announced. A deployment that needs the order announces from a
  separate component and leaves `publish` off there.

## Provider liveness

A provider that dies without withdrawing is the case events alone
cannot cover. The answer is a heartbeat, and it is **opt-in**: a
registration that never heartbeats is never retired for silence.

```
PUT /references/{referenceId}/heartbeat?intervalSeconds=30
```

- The Java and TypeScript SDKs send it automatically at
  `provider.heartbeat.seconds`, **30 seconds** by default, on the
  `org.eclipse.fennec.services.client` configuration. `0` switches it
  off. The first one fires early, within 5 seconds of activation.
- **Two missed intervals** and the registration is lost, so 60 seconds
  of silence at the default.
- The broker sweeps every 5 seconds
  (`liveness.sweep.seconds` on `org.eclipse.fennec.services.broker.core`).
- A consumer then sees `UNREGISTERING` and `RETIRED`, both with reason
  `PROVIDER_LOST`.
- A heartbeat for a registration the broker no longer has answers 404.
  That is the cue to publish again, and the SDKs do.

Leases are runtime state. A broker restart forgets them, and the next
heartbeat rebuilds them through that same 404.

## Consumer presence

A consumer session normally expires 1200 seconds after its last
renewal. An open event stream is a presence signal the broker gets for
free, so losing one is worth acting on sooner: a consumer whose stream
has been gone for `session.disconnect.grace.seconds`, **60 by default**,
loses its session without waiting out the full expiry.

Three things keep that safe. A reconnect or a fresh session clears the
deadline, and the client reconnects within seconds. A consumer that
still holds another stream is not counted as gone. And a consumer whose
transport the broker sees no connection for, MQTT among them, is never
affected: for those the renewal interval remains the only truth.

The reverse never holds. An open connection does not replace the
session: it says "alive", not "still holding reference X". Set the
property to 0 to switch the shortcut off.

## Proven by

The harness runs all of this against real processes:

| Scenario | What it proves |
| --- | --- |
| A | SSE end to end, `WITHDRAWN`, and that the consumer was told *before* the provider process was gone |
| B | FR-P3 order from the TypeScript side: withdrawal confirmed before the endpoint stops |
| C, D | the event document over real MQTT/TCP, and that the transport was chosen by target and not by ranking |
| F | `MODIFIED` in place, same reference id, leases kept |
| G | `UPGRADE_AVAILABLE`, drain, rebind |
| H | SIGKILL with no withdraw, `PROVIDER_LOST` through the heartbeat |

`./itest/run-harness.sh` runs A, B, F, G, H on the host;
`./itest/run-harness-podman.sh` adds C, D, E in containers with a real
Mosquitto.

## Read next

- [Overview](OVERVIEW.md) — the three parties
- [Transports](TRANSPORTS.md) — what to configure
- [Update policies](UPDATE_POLICY.md) — where `UPGRADE_AVAILABLE` comes from
- [Discovery, Acquisition, Invocation](ACQUISITION.md) — leases and sessions
