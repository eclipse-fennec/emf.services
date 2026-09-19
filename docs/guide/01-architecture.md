# 1 · Architecture

## Three parties and one thing the broker does not do

A **provider** has something to offer. It publishes a contract it
serves, together with an address for it.

A **broker** holds two lists: the catalogue of contracts it knows, and
the registrations of who currently serves them. It answers "who serves
this contract", it tells subscribers when that answer changes, and it
notices when a provider stops answering.

A **consumer** asks the broker who serves a contract, gets an address,
and calls the provider directly.

That last part is the load-bearing one: **the broker is never in the
call path.** It hands out an address and steps aside. A broker that is
down costs you discovery, not the calls already in flight.

```
provider ──publish──▶ broker ◀──look up── consumer
                        │                     │
                        └──── events ─────────┘
                                              │
provider ◀════════ the actual call ═══════════┘
```

## The model is the contract

The thing that travels between the three is an
[EMF](https://eclipse.dev/emf/) model, not a document format that
happens to describe one. One `services.ecore` is generated into Java
and into TypeScript, so both sides mean the same thing by a `ServiceInterface`,
a `ServiceOperation` and a `Parameter`.

Two consequences run through everything else:

- **A contract can be hashed.** Both sides compute the same fingerprint
  over the same model, which is how a consumer can refuse a provider
  whose contract drifted. See [3 · Fingerprints](03-fingerprints.md).
- **A call can be made without generated code.** An invoker reads the
  parameter names from the model rather than from Java reflection, so
  it does not depend on the `-parameters` compile flag, and a generic
  REST distribution can serve a contract nobody wrote a class for.

## What a contract looks like

Three model types carry the weight:

- **`ServiceInterface`** — the contract. A name, a version, and its
  operations with their parameters. This is what goes into the
  catalogue and what gets fingerprinted.
- **`ServiceImplementation`** — somebody's claim to serve one or more
  contracts. It carries the **flavors**.
- **A flavor** — how to reach it. `RestFlavor` has a host, a base path
  and one operation flavor per operation, with its own path and HTTP
  method. `MqttFlavor` names brokers and topics instead. The contract
  says *what*, the flavor says *where and how*.

A provider publishes an implementation; the broker mints a
`ServiceReference` for it and hands that to consumers.

## Discovery, acquisition, invocation

Three stages, and only the first is mandatory.

1. **Discovery** — `GET /references?interface=…`, optionally with a
   fingerprint and an LDAP filter. Plus events, so the answer stays
   current without polling.
2. **Acquisition** — a consumer may take a *lease* on the references it
   is using, by PUTting a session. This is what lets a graceful
   handover wait for the last consumer to let go. It is an idempotent
   full replace: the list you send is the complete truth.
3. **Invocation** — peer to peer, over the flavor.

Details in [docs/ACQUISITION.md](../ACQUISITION.md).

## The one lifecycle guarantee worth memorising

**A consumer is told before the endpoint disappears.** A provider
shutting down blocks on the broker's acknowledgement before it stops
serving:

```
provider: SIGTERM
  provider ──▶ broker   withdraw
               broker   retire the registration, release leases
               broker ──▶ consumers   UNREGISTERING
               broker   save the snapshot
               broker ──▶ provider    OK
provider: only now, stop the endpoint
```

This is called FR-P3 in the issues and the harness. The cost is that a
shutdown is not instant. The benefit is that a consumer never discovers
the withdrawal by getting a connection refused.

A provider that dies without saying so is a different case, handled by
heartbeats — see [2 · Eventing](02-eventing.md).

## Bundles

Java, grouped by what they are for. `src-gen` folders are generated;
never edit them by hand.

**The model**
- `…services.model` — generated from `model/services.ecore`

**The broker**
- `…broker.core` — the registry itself. `DdsrBroker` is the façade over
  nine components, one concern each (#110).
- `…broker.rest` — JAX-RS endpoints and the SSE stream
- `…broker.mqtt` — publishes the same events to an MQTT broker

**Talking to it**
- `…client.java` — the SDK, transport-agnostic
- `…client.rest`, `…client.mqtt` — the two transports
- `…xmi.codec` — the wire codec and the fingerprints

**Serving**
- `…flavor.rest` — the REST placement rules, shared by consumer,
  dispatcher and template. No JAX-RS in it.
- `…provider.rest` — serves a contract straight from its model, with no
  generated code (#84)
- `…derive` — builds a contract from a Java interface by reflection,
  deterministically

**Remote Service Admin** — see [6 · RSA](06-rsa.md)
- `…rsa`, `…rsa.distribution.rest`, `…rsa.discovery.rest`,
  `…rsa.discovery.local`, `…rsa.topology`, `…rsa.config`

**Examples and tests**
- `…examples.payment`, `…examples.model`, `…examples.rsa*`
- `…rsa.tck` — the OSGi RSA TCK as a launch

TypeScript lives in `ddsr-ts-client/`, a pnpm workspace. It keeps its
original `@ddsr/*` naming until the namespace rename (#4); the *content*
of its ecore is synced from the Java one.

## Where the truth is

The cross-language harness is the anchor. `itest/run-harness.sh` and
`itest/run-harness-podman.sh` run a real broker, a real provider and a
real consumer in both languages and assert the lifecycle guarantees end
to end. When a document and the harness disagree, the harness is right.

## Read next

- [docs/GETTING_STARTED.md](../GETTING_STARTED.md) — a broker and a
  provider running, from a fresh checkout
- [2 · Eventing](02-eventing.md) — what a consumer is told, and when
- [docs/ARCHITECTURE.md](../ARCHITECTURE.md) — the internal view, in
  German, with the decision record
