# Watching a node — the runtime services

A broker and a client SDK both hold state that nothing outside them can
see: which registrations exist, who holds a lease on one, whether the
event stream is still up. Two services hand that out as OSGi DTOs, and
say when it changed.

They exist because telemetry should be a separate bundle. An
OpenTelemetry integration (#126), a console, a health check and a test
all want the same answer, and none of them should have to reach into
the broker to get it — nor should the broker have to know that any of
them exist.

| Service | Registered by | Answers |
| --- | --- | --- |
| `BrokerRuntime` | `…broker.core` | what this broker holds |
| `ClientRuntime` | `…client.java` | what this runtime published and bound |

Both live in `org.eclipse.fennec.services.runtime`, in the api bundle.

## Getting the answer

```java
@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
volatile BrokerRuntime runtime;

void report() {
    BrokerRuntimeDTO now = runtime.snapshot();
    for (RegistrationDTO registration : now.registrations) {
        System.out.println(registration.implementationId + " held by " + registration.heldBy);
    }
}
```

`snapshot()` builds a fresh DTO per call. On the broker side it is
built under the read lock in one pass, so what comes back is a broker
that actually existed at one moment — a snapshot assembled from several
short reads would show registrations from before a withdraw and
sessions from after it.

Everything in a DTO is a copy of state that went on living. Nothing in
it is EMF, which is the point: a watcher reporting to a metrics backend
should not need the model to read a number.

## Being told when it changed

Both services carry the standard OSGi `service.changecount` property,
and its value moves whenever the next snapshot would differ. A dynamic
reference with method injection is the whole subscription:

```java
@Reference(name = "runtime", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
void setRuntime(BrokerRuntime runtime, Map<String, Object> properties) {
    this.runtime = runtime;
    report();
}

void updatedRuntime(BrokerRuntime runtime, Map<String, Object> properties) {
    report();                       // the count moved; ask again
}

void unsetRuntime(BrokerRuntime runtime) {
    this.runtime = null;
}
```

No listener to register, no lifecycle to know. The property is the
standard one rather than one of ours, because OSGi already has a name
for "this service's answer has changed" and every whiteboard runtime
uses it.

The number itself is deliberately dumb. It counts changes rather than
describing them: the answer to *has anything happened* is a comparison,
and the answer to *what* is another snapshot.

**Two details DS will not warn you about.** The callback is
`updated<Name>`, not `modified<Name>`: DS calls the first when a *bound
service's* properties change and the second when the *component's own
configuration* does, so a watcher that writes `modified` compiles,
binds, and is never told anything. And the properties have to be a
method parameter — a field-injected reference never receives them, and
a reference bound by field gets no callback at all.

### What it costs

Updates are coalesced on a daemon thread, at most one every 200 ms.
Setting a service property makes the component runtime call every bound
watcher synchronously; doing that on the broker's mutating thread would
put a watcher's work inside the broker's write lock, and a burst of
twenty registrations would do it twenty times. So the mutating path
only moves a number, and the publisher thread turns that into at most
one notification per pass.

A watcher whose callback throws is logged and skipped. It does not stop
the next update, and it does not reach the broker.

## What the broker reports

`BrokerRuntimeDTO`:

- `name`, `changeCount`, `takenAt`
- `registrations` — every live registration: its reference id, the
  provider, the contracts and flavors, both fingerprints as the broker
  computed them, the update policy, **who holds a lease on it**, and
  the last heartbeat with the interval that was promised
- `catalog` — every known contract with its status, its `sd1`
  fingerprint, and how many implementations currently serve it
- `sessions` — each consumer session: what it has acquired, when it
  last renewed, whether its event stream is connected, and its
  `origin`: the token `label/runtimeId` the session reached the broker
  from, the same one a span carries as `fennec.origin`
- `delivery` — how many events were dropped, and whether the broker
  owes a resync
- `coldEntries` — how many registrations the cold cache holds

The leases and the delivery debt are the reason this service exists at
all. Everything else can be reconstructed from the wire by a consumer
that watches long enough; who is holding what cannot, because the
broker never announces it.

## What a client reports

`ClientRuntimeDTO`:

- `consumerId`, `changeCount`, `takenAt`, `supportedFlavors`
- `published` — what this runtime published, whether the broker is
  holding it (`live`), and `failure` when the broker objected
- `bindings` — what it is bound to: the contract, the filter it was
  tracked with, the reference id, the endpoint the flavor announced,
  and the locator's state: `LIVE`, `MODIFIED` (changed in place,
  refreshed on next use), `STALE` (parked in the cold cache) or
  `REBIND`
- `eventStreamConnected` and `eventTransport` — the one thing nobody
  else can answer. The broker sees a subscription; only the consumer
  knows whether it is currently reading one.

`failure` is the small version of what a whiteboard runtime does with
its failed DTOs: something that was offered and not taken is worth
reporting *with the reason*, because the alternative is a watcher that
sees an absence and has nowhere to look.

### Why the client counts differently

The broker funnels every mutation through one façade, so a counter
there is exact and free. A client's state changes in many places — a
locator rebinds inside event handling, a stream drops on its own thread
— and a counter incremented at the call sites would have to be
remembered at every one of them. A missed call site is a watcher that
is never told, and that failure is silent.

So on the client side the change is *detected* rather than reported: a
fingerprint over what a snapshot would say, compared against the last
one seen. The count moves when the fingerprint does, so a watcher still
gets the monotonic number the OSGi idiom promises.

The consequence is that a change and its undo within one 200 ms pass
are invisible to a watcher. That is true of any coalescing publisher,
and it is the reason the number says *that* something happened rather
than *what*.

## What this is not

It is not the event stream. [Eventing](EVENTING.md) tells a *consumer*
what it needs to keep working: a provider arrived, one is going away.
This tells an *operator* what a node holds. A consumer that used the
runtime service to notice a new provider would be watching the wrong
thing — it would see the broker it happens to share a framework with,
and nothing else.

A bundle that turns these into metrics already exists —
see [Telemetry](TELEMETRY.md).

It is also not remote. Both services are OSGi services in their own
framework. Exposing a broker's runtime over REST is a different
decision, with a different threat model, and has not been taken.
