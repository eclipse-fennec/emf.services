# Discovery, acquisition, invocation — the three stages and the broker's boundary

A design draft (stage 3, like [UPDATE_POLICY.md](UPDATE_POLICY.md) and
[WIRE_CHANNELS.md](WIRE_CHANNELS.md) — specified, not implemented).
It came out of the question: *we know who offers an implementation —
but a consumer that fetches a reference is a long way from consuming
it. Where does knowledge about actual usage belong?*

## 1. The three stages and the broker's scope

OSGi knows three stages of service usage. DDSR maps them like this:

| Stage | OSGi | DDSR | The broker's role |
|---|---|---|---|
| **Discovery** | `getServiceReference` | `GET /references` | complete — implemented |
| **Acquisition** | `getService` / `ungetService`, use count | ConsumerSession + leases (this document) | complete — **designed** |
| **Invocation** | a method call | the flavor path (REST/MQTT/…) | **none** — peer to peer |

**The decision: the broker supports exactly the first two stages.**
For the invocation it is purely the mediator of peer-to-peer connection
information (the flavors in the service description); the call itself
never touches the broker. That is the existing architecture and it
stays — a broker-as-gateway would be a different system.

The consequence, named honestly: **acquisition is a cooperative
protocol, not enforcement.** A consumer that has the connection
information once can call past the broker. The acquisition stage buys
drain semantics (`DEPRECATE_AND_DRAIN`), stale cleanup (OPEN_ISSUES C3)
and usage telemetry — not access control. Enforcement would be the
provider side's business, or the hook architecture's (A3), and is
deliberately out of scope here.

## 2. Who knows what — the epistemics

- **"Who offers"** the broker knows with transactional certainty (a
  publish is acknowledged and persisted).
- **"Who searched"** it knows anecdotally (`consumerId` at lookup).
- **"Who wants to consume"** is the gap this document fills — as an
  *assertion with an expiry date* (a lease), because in a distributed
  system there is no enforced use count: a crashed consumer does not
  count itself down.
- **"Who actually consumes"** is, in the last instance, known only to
  the provider at its endpoint — and only if invocations carry a
  consumer identity (they do not today; deliberately not part of this
  draft).

## 3. Model: the session owns the lease, the registration gets a view

### 3.1 Why not on the `ServiceReference`

`ServiceReference` ↔ `ServiceRegistration` is a 1:1 pair per published
service. What is "many and fleeting" is not the references but their
**wire copies**: the reference is the consumer-visible artefact and is
copied into every lookup result and every event document. Usage state
on the reference would therefore (a) travel along in every
serialisation and be stale the moment it touched the wire, (b) put
write load on a read-mostly object, (c) suggest a live view to the
consumer that is not one.

`ServiceReference.usingProviders` is doubly unfortunate on top of that:
the wrong place (see above) **and the wrong type** — a consumer is not
necessarily a `ServiceProvider`. The feature is deprecated and replaced
by what follows (OPEN_ISSUES M5).

### 3.2 Why not as a counter on the `ServiceRegistration`

A stored `usageCount:int` drifts at the first consumer crash — nobody
counts down. **The truth is the lease; the count is a query.**

### 3.3 The model

```
ConsumerSession                       (containment: LocalServiceRegistry.sessions)
  consumerId    : String              (identity, see §7)
  capabilities  : ConsumerCapability  (containment; greedy, supportedFlavors, …)
  lastRenewal   : Instant
  acquisitions  : ServiceRegistration[*]   (non-containment)

ServiceRegistration
  usingSessions : ConsumerSession[*]  (eOpposite of acquisitions — the derived view)
```

- **The session is the owner**, because the lease lifecycle follows the
  consumer, not the service: one heartbeat renews *all* of a consumer's
  acquisitions in one go, one shutdown releases them all, one crash
  lets them all expire together. The dominant event, "the consumer is
  gone", hits exactly one session instead of N registrations. (OSGi
  too keeps the use counts in the user's *BundleContext*;
  `getUsingBundles()` is only the aggregated view.)
- The drain question of `DEPRECATE_AND_DRAIN` becomes a query in the
  other direction: `registration.usingSessions.isEmpty()`.
- Acquisitions point at the **registration** (stable, 1:1 with the
  published implementation, and it does *not* survive the reference-id
  regeneration of a broker restart — see §6, which is fine).

The ecore change (a new EClass `ConsumerSession`,
`LocalServiceRegistry.sessions`, the eOpposite on `ServiceRegistration`,
deprecating `usingProviders`) happens separately; code generation is
the model owner's job as usual.

## 4. Protocol: one idempotent endpoint instead of three verbs

Acquire, release and heartbeat collapse into **one idempotent full
replace** — the same pattern as FR-Sync-Reconnect (a snapshot instead
of a delta, state instead of history):

```
PUT    /consumers/{consumerId}    body: ConsumerSession XMI
                                  (capabilities + acquisitions as reference ids)
                                  → creates or fully replaces; renews the lease
DELETE /consumers/{consumerId}    → shutdown: releases every acquisition at once
GET    /consumers/{consumerId}    → diagnostics (what does the broker believe about me?)
```

- **Acquire** = add the reference to the local list, `PUT`.
  **Release** = remove the reference, `PUT`. **Heartbeat** = an
  unchanged `PUT` on the interval. One round trip per interval, crash
  safe, insensitive to ordering (the last `PUT` wins).
- **TTL:** a lease expires after 2× the interval without a `PUT`
  (values as in UPDATE_POLICY §4: 10 min / 20 min; configurable on the
  broker). Expiry releases every acquisition of the session.
- **A liveness shortcut:** the loss of the event connection (the
  SSE/MQTT session) *may* bring the expiry forward — an open connection
  is a free presence signal and beats the timeout in the normal case.
  The heartbeat stays the truth for transports with no connection
  semantics. The converse does not hold: an open connection does not
  replace the `PUT` (it says "alive", not "holds reference X").
- The client has it all already: the SDK's `noteReference` map and
  `subscribedInterfaces()` are exactly the `acquisitions` list;
  `close()`/shutdown hooks call the `DELETE` (symmetric in Java and TS,
  in FR-P3 order: withdraw/release first, then the endpoint/streams).
- **Wire realisation (implemented):** the PUT document is multi-root
  XMI following the publish convention — the `ConsumerSession`
  (consumerId plus capabilities by containment) plus **sibling
  `ServiceReference` stubs that carry nothing but their `id`**; the
  stub list *is* the acquisition list. The model feature `acquisitions`
  is transient and never travels in the XMI (nor does `usingSessions`,
  nor the `reference`⟷`registration` pair: the provider handles are
  runtime objects on the broker side with no containment home — a
  serialised link would tear every snapshot apart). `GET` answers in
  the same shape. The path owns the identity; a body id that
  contradicts it is a 400. **A stale acquire** (a reference id that no
  longer exists — after a broker restart with regenerated ids, say) is
  skipped and named in the diagnostic, never refused (§5).
- A side benefit: the same session structure is the natural carrier for
  the **interests of the event subscription** — the documented A2 hole
  that `EventSource.open()` carries no interests.

## 5. Semantics and guarantees

- **Over-claiming is harmless** (a consumer holds leases on references
  it never calls — it costs only a delayed drain), **under-claiming
  hurts only itself** (whoever does not acquire loses the drain
  protection: their service can be retired out from under them). Both
  deliberately symmetric with the "rather over-deliver than drop" rule
  of the events.
- The broker may forget leases at any time (a restart, §6). Consumers
  have to cope with `RETIRED`/`UNREGISTERING` despite holding a lease —
  the lease is protection *within the policy*, not a contract.

## 6. Persistence: deliberately not

Sessions and leases are **runtime state** and do not belong in
`broker-state.xmi`. After a broker restart the consumers rebuild the
map themselves through their regular `PUT`s — the same philosophy as
FR-Sync-Reconnect, and it spares us the expiry-date problem when
loading old snapshots. (In practice: reference ids are regenerated on
reindex anyway; persisted leases would point at nothing.)

**A consequent rule for DEPRECATE_AND_DRAIN:** right after a broker
restart the session map is empty for up to one heartbeat interval.
Auto-retire may therefore believe "no users any more" only once the
broker has been running for at least one full interval — otherwise a
restart drains everything by accident.

## 7. Security (a caveat)

`consumerId` is unauthenticated today (S2). An idempotent
`PUT /consumers/{id}` with somebody else's id **replaces their
session** — which could be used to disturb the drain semantics (delete
another consumer's leases → a premature retire) or to inflate it.
Accepted for the prototype and recorded here; once S2 (authentication)
is solved, the session is bound to the authenticated identity and
`{id}` is checked against it. To be noted in SECURITY.md as an addition
to S2 once the implementation is due.

## 8. The registration as a materialised fact — the missing model references

A provider registering an implementation **materialises** a
`ServiceRegistration` — it is the durable fact "provider P published
implementation I". Today the EClass does not carry that fact at all: it
has only the `reference` eOpposite and `unregistered`; the mapping to
provider and implementation lives in the broker as an
`implByRegistration` **side map** (an IdentityHashMap, with documented
unspecified iteration order). That belongs in the model:

```
ServiceRegistration
  provider       : ServiceProvider        [1]  (non-containment)
  implementation : ServiceImplementation  [1]  (non-containment)
  reference      : ServiceReference       [1]  (eOpposite, as before)
  usingSessions  : ConsumerSession[*]          (eOpposite of acquisitions, §3)
```

The picture is then symmetric: **the registration is the provider side
of the usage relation, the session is the consumer side** — both
reference their identity non-containment, and the `ServiceReference`
stays the neutral wire artefact in between. The side map in the broker
disappears without replacement (its scans become model navigation).

## 9. Derived features through OCL (fennec m2x)

The consumer count is modelled as a **derived/volatile/transient**
feature with a fennec m2x OCL annotation (namespace
`http://www.eclipse.org/fennec/m2x/ocl/1.0` — the ecore already uses it
for the `unregisteredNotInRegistry` invariant on exactly this class):

```
ServiceRegistration.consumerCount : EInt  (derived, volatile, transient)
  ocl: self.usingSessions->size()
```

That fits twice over: derived+transient means **never on the wire**
(exactly the §3.1 requirement), and "the count is a query" becomes
literally true — the query stands declaratively in the model instead of
imperatively in the broker. The cost, honestly: the broker gains the
OCL engine as a runtime dependency, and it would be the first *active*
use of OCL in the project (M2, "constraints not active", would be
addressed in the same pass). The engine brings its own
`FingerprintExpressionCache` for repeated evaluation (the m2x module
`ocl.fingerprint`, keyed on the same model-fingerprint principle as
emf.osgi/sd1) — the evaluation cost per access is therefore
manageable. The fallback stays a plain Java derivation; the OCL variant
is the modelled one.

**Status 2026-08-25: implemented** (branch feat/ocl-activation, issue
#7). `consumerCount` is in the model (derived/volatile/transient, OCL
`self.usingSessions->size()`), the EPackage declares setting *and*
validation delegates on the fennec OCL namespace, and the curated
invariants are live through `constraints` annotations (validSemver,
range/length/size bounds, replacedByIsDeprecated, atLeastOneInterface,
operationFlavorsCoverInterfaces, unregisteredNotInRegistry,
failureOnlyWhenFailed, the three registry invariants;
`immutableAfterPublish` stays prose/documentation). The broker launches
carry the OCL engine as a runrequire — the delegate factories arrive as
DS services and the emf.osgi registry wires them globally. Two
portability adjustments to the expressions: enum comparisons go through
`toString()` (the engine yields the EEnumLiteral for an EnumLiteralExp
while generated models yield the type-safe enumerator — direct equality
would always be false; reported upstream to m2x), and
`eContainer().oclAsType(...)` was replaced by plain model navigation.
Mind the delegate caching: EMF caches the setting delegate per feature
and instance — the factory has to be registered before the first
`getConsumerCount()` access.

## 10. Hot/cold cache: parking registrations with no consumers

With dependable usage information a storage policy becomes possible:
**if a registration holds no session for a duration T** (and there were
no lookups on its interface for T), it moves from "hot" (the in-memory
registry) to "cold" (disk).

The one rule that carries the design: **cold ≠ undiscoverable.** A
service with no consumers has to stay discoverable — otherwise nobody
ever finds it again and cold would be cold forever. So a small
**in-memory stub** stays in the lookup index per cold entry:

```
ColdEntry: interfaceName, implementationId, provider.name,
           the sd1 fingerprint (plus the impl fingerprint where applicable, §11),
           the path of the cold file
```

When a lookup or an acquire hits the stub, the entry is rehydrated
lazily (XMI from disk, back into the registry and the index) and is hot
again. The OSGi parallel is DS's **delayed activation**: a service with
no users does not materialise its instance — the cold cache is the same
principle one level up, at the registry.

Perspective: for the prototype the memory saving is irrelevant (the
catalog is tiny); the value is architectural — the broker then scales
with the catalog size, not with the hot-set size. Designed as an
**optional policy** (off by default), to be implemented no earlier than
after §3–§5.

**Status 2026-08-25: implemented** (branch feat/catalog-contract-key,
issue #6). Configuration `cold.after.seconds` on the broker component
(default 0 = off; the sweep runs at a quarter of that value). The idle
rule: no lease at sweep time (a lease resets the idle clock), and since
the cutoff neither published/rehydrated nor a lookup on one of the
interface names. Cold storage is a self-contained XMI per entry (a
provider stub plus the implementation plus the full contract siblings —
the publish wire form) under `<snapshot>.cold/`; the in-memory stub
carries the interface names, the addressing sd1s and the
implementationId, and is reconstructed from the cold directory on a
broker restart. Rehydration runs lazily on the lookup path THROUGH the
regular publish (catalog validation, decoration including im1). The
lifecycle is kept honest: coldifying announces UNREGISTERING (the
reference id becomes invalid — references are not restart-stable
anyway), rehydration announces REGISTERED with a fresh reference; the
sd1 on the reference stays identical. Cold entries count as live for
the catalog's strict reject (otherwise they could never rehydrate), and
a republish of the same identity replaces the cold twin (a provider
restarting while cold).

## 11. Fingerprint-backed reconnect and contract addressing

### 11.1 Composition: the implementation fingerprint folds in the contract fingerprints

sd1 identifies the **contract** (`ServiceInterface`). The
implementation gets its own, separately frozen scheme (working title
`im1`) that **composes** rather than traversing anew — the Merkle
principle, exactly the `derivationInputs` pattern from emf.osgi:

```
im1(Impl) = H( sd1(SI₁), …, sd1(SIₙ),
               implementationId, flavors/endpoints, properties )
```

A provider's reconnect check — *"does the broker still have my
registration, and unchanged?"* — thereby says three different things
(the identity key is `(provider.name, implementationId)`, both of which
exist today):

| Comparison | Meaning | Action |
|---|---|---|
| `im1` equal | nothing changed | only renew the session/lease (`PUT`, §4) |
| `im1` different, every `sd1` equal | endpoint/property drift | republish the implementation |
| one `sd1` different | **contract** drift | a catalog/policy question (UPDATE_POLICY), not merely a republish |

Consumers have the sd1 check already: a locally computed value against
the `ddsr.fingerprint` reference property (the FR-P4 harness checks
exactly that).

**Status 2026-08-25: implemented** (branch feat/im1-fingerprint, issue
#6). The scheme is frozen as `im1` — the canonical grammar is in the
javadoc of `ServiceImplementationFingerprint` (xmi.codec, shared with
the broker), the TypeScript mirror is
`service-implementation-fingerprint.ts`; both languages are pinned
byte-identically through the goldens
`itest/fixtures/fingerprint/payment-impl.{xmi,canonical.txt,im1}`.
Excluded are `description` and `componentDescription` (documentation
and deployment detail, not endpoint identity). The broker additionally
decorates every reference with `ddsr.impl.fingerprint` — computed AFTER
the catalog rewire, so the `c|sd1:…` lines are catalog truth. The
identity comparison at reconnect is purely content based: an im1 match
under the same `provider.name` IS one's own registration (im1 contains
the implementationId, the endpoints and the sd1 tokens). `publish()` is
therefore idempotent in both SDKs: an im1 match → the publish is
skipped, the registration is reused, consumers see no
UNREGISTERING/REGISTERED churn; drift → a republish (the broker retires
the old entry), and the direction is logged (sd1 equal → endpoint drift
at INFO, sd1 different → contract drift at WARNING — a publish stays
the safe default, the broker validates against the live catalog). The
"only renew the lease" row of the table means, in practice: the reused
publish path needs no broker mutation, and the session renewal (§4)
runs anyway.

### 11.2 Contract addressing: lookup and catalog keyed on `(name, sd1)`

Interface fingerprints carry more than the drift check on the consumer
side: they make the contract **addressable**.

- **Lookup:** `GET /references?interface=Payment&fingerprint=sd1:…` —
  or better: the `ConsumerCapability` declares the contracts the
  consumer *speaks* (its stubs were generated from one concrete state
  of the ServiceInterface, which sd1 names exactly), and the broker
  filters every lookup on that automatically — as it does for
  `supportedFlavors`.
- **Catalog:** the key becomes `(name, sd1)` instead of `name`.
  Interfaces of the same name with a different signature or properties
  are then simply different catalog entries that coexist (today:
  `CATALOG_ENTRY_ALREADY_EXISTS`); by construction a consumer only ever
  gets implementations of the contract it knows exactly. That
  incidentally defuses M1 (names not globally unique).

**Status 2026-08-25: the `(name, sd1)` key is implemented** (branch
feat/catalog-contract-key, issue #6). The rules:

- `addCatalogEntry`: identical content → an idempotent OK; the same
  name with a different contract → a coexisting entry (code 202 is no
  longer produced but stays documented for wire compatibility).
- **Resolution** (`resolveCatalogEntry`): an incoming ServiceInterface
  **with content** (operations/exceptions) addresses exactly by
  content — no hit is contract drift and is refused rather than
  silently rewired onto the same-named catalog contract (the old rewire
  semantics were precisely the false-equal that fingerprints are meant
  to rule out; the publisher instead puts its contract into the catalog
  itself — it coexists after all). A **stub** (a name only, a catalog
  URL proxy, or a bodiless REST call) resolves by name and demands
  uniqueness — otherwise `CODE_CATALOG_ENTRY_AMBIGUOUS` (203).
- **The addressing fingerprint** (`ContractAddressing`): by its frozen
  grammar sd1 contains `status=` — but a deprecation must not move the
  address. Addressing therefore goes through the sd1 of a
  lifecycle-normalised copy (status→ACTIVE, deprecationReason and
  replacedBy cleared); the raw sd1 remains what is decorated onto
  references and compared by consumers.
- **REST:** `GET/DELETE /catalog/{name}` and `PUT …/deprecate` take an
  optional `?fingerprint=sd1:…`; a bare name answers with 409 (GET) or
  the ambiguity diagnostic when entries coexist. The strict reject on
  remove checks by identity, not by name — a same-named sibling
  contract does not block it.

### 11.3 Boundaries: identity comes free, compatibility does not

What fingerprints give away for free is **identity versioning**
(content-addressed contracts): every differing detail separates
cleanly. What they **cannot** give is compatibility *semantics*: hashes
have no ordering — a backward-compatible addition (a new optional
operation) changes sd1 exactly as much as a hard break. "A consumer of
1.4 may bind a 1.5 provider" is not something a hash can express (the
emf.osgi documentation says as much, word for word: *not a version
number, no compatibility semantics*; conservatively wrongly-different,
never wrongly-equal). For that, declared versions and ranges remain
(UPDATE_POLICY §5), or explicit compatibility assertions in the catalog
("replaces sd1:X compatibly" — an assertion by the publisher).

The two complement each other: **the version communicates the
intention, the fingerprint verifies the reality.** A range match whose
fingerprint comparison fails is a lying version label — and becomes
visible as one.

**Synergy with §10:** sd1 and im1 are in the cold stub — the broker can
answer "still there and unchanged?" without rehydrating the cold entry.

## 12. Implementation order

**Status 2026-08-25: steps 1–3 are implemented** (branch
feat/acquisition): the broker holds sessions as a runtime map with TTL
expiry (`org.eclipse.fennec.services.broker.core`,
`session.expiry.seconds` default 1200, swept at a quarter of that),
`PUT/GET/DELETE /consumers/{id}`, and the side map `implByRegistration`
is replaced by the model references
`registration.provider`/`.implementation` (deterministic insertion
order instead of an IdentityHashMap scan); withdraw/republish **release
the leases of the affected registration** (a rollback on a persist
failure restores them); both SDKs (Java: `SessionsHttpProxy` plus a
renewal scheduler in the client component, `session.interval.seconds`
default 600, DELETE on shutdown before the stream close; TS:
`putConsumerSession`/`deleteConsumerSession`/`getConsumerSession` plus a
timer in `DdsrClientImpl`, the same `close()` ordering). From §11.2 the
**lookup contract addressing** is implemented: `GET
/references?...&fingerprint=sd1:…` filters exactly against the
broker-computed catalog fingerprints (Java through the
`ddsr.fingerprint` property on the ConsumerCapability, TS through
`find(interface, filter, fingerprint)`). Step 4 (auto-retire/drain) is
deliberately separated and comes with the policy machinery.

**Status 2026-08-25, addendum:** from step 5 the **fingerprint
reconnect for providers** is implemented (im1, §11.1 — branch
feat/im1-fingerprint); with feat/catalog-contract-key the **catalog key
`(name, sd1)`** (§11.2) and the **cold cache** (§10) are implemented as
well — which completes step 5 and closes issue #6. Step 4
(auto-retire/drain) remains open, deliberately deferred.

1. Ecore: `ConsumerSession`, `LocalServiceRegistry.sessions`, the
   eOpposite `ServiceRegistration.usingSessions`, plus
   `ServiceRegistration.provider`/`.implementation` (§8, replacing the
   `implByRegistration` side map) and deprecating
   `ServiceReference.usingProviders` (M5) — code generation by the
   model owner. Optionally in the same pass: `consumerCount` as an
   OCL-derived feature (§9).
2. Broker: the three endpoints plus TTL expiry (one scheduler, like the
   SSE heartbeat) plus the connection-loss hook.
3. SDK Java and TS symmetrically: the `PUT` out of the existing
   reference bookkeeping, the `DELETE` into the FR-P3 shutdown path.
4. Only then consume it: switch `DEPRECATE_AND_DRAIN` auto-retire and
   the C3 cleanup from UPDATE_POLICY over to the session map.
5. Afterwards, optionally: the fingerprint reconnect for providers
   (§11, which needs only the existing sd1 properties plus an identity
   comparison) and finally the cold cache (§10, its own policy).
