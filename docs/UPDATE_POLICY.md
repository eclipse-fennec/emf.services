# DDSR — Service API Update Policy

**Status:** broker side implemented (§11); consumer reaction to `UPGRADE_AVAILABLE` and version negotiation (§5) are outstanding.
**Last updated:** 2026-09-15.

Governs how the broker deals with new versions of a service API, whether and when old versions disappear, and how consumers find out. Complements the architecture description in [ARCHITECTURE.md](ARCHITECTURE.md), and incidentally settles OPEN_ISSUES A1 (SSE/event stream), C2 (provider-aware lookup) and C3 (stale providers).

---

## 1. Motivation

Today (see [ARCHITECTURE.md §2.6](ARCHITECTURE.md)) the broker deduplicates on `(name, version)` at `publishImplementation` and synchronously retires the old implementation when a new one of the same identity is published. That is good, but:

- **There is only one variant.** For the broker API itself you want versions running side by side ("evergreen"); for ordinary APIs you want a migration window with deprecation; sometimes you want a hard cut.
- **Consumers learn about changes passively.** As long as one does not actively look up again, it stays on the old reference.
- **There is no clean drain mechanism.** The broker does not know who still holds a reference, so it cannot know when it is safe to clear an old version away.

This document specifies three policies, the trigger model, and the heartbeat protocol they need.

## 2. The three update policies

The policy is annotated **on the `ServiceInterface`** (the default for every implementation of that interface); overridable per `ServiceImplementation` when one concrete implementation should behave differently. Default for catalog entries with no explicit statement: `DEPRECATE_AND_DRAIN` (safe, friendly).

### 2.1 `EVERGREEN`

Several major versions run side by side indefinitely. None is retired automatically. Soft migrations stay the consumer's business (they pick a version explicitly via `versionRange`).

**When:** the broker API itself, infrastructural services with unknown consumers, anything where the provider has no control over consumer lifecycles.

**Broker behaviour:**

- `publishImplementation(v2)` takes v2 in addition. v1 stays untouched.
- `find()` without `versionRange` returns the highest non-deprecated version (matches today's default).
- `find(versionRange="[1.0,2.0)")` returns v1, `find(versionRange="[2.0,3.0)")` returns v2.
- A retire happens only explicitly, through `withdrawImplementation(v1)` by the provider.

### 2.2 `DEPRECATE_AND_DRAIN`

The new version coexists with the old one; the old one is marked `deprecated=true`. New `find()` calls without an explicit range get the new version. Existing consumers on the old version get an `UPGRADE_AVAILABLE` event and may switch voluntarily. As soon as the broker sees no active references to v1 any more (see §4), it retires v1 automatically.

**When:** planned API evolution with a migration window; the gentler variant of HARD_CUTOVER.

**Broker behaviour:**

- `publishImplementation(v2)` takes v2 in, sets `v1.deprecated = true`, writes `v1.replacedBy = v2`.
- `find("Payment")` returns **v2** (deprecated is filtered out by default).
- `find("Payment", versionRange="[1.0,2.0)")` still returns v1 (an explicit reference to the old range).
- The broker sends `ServiceEvent.UPGRADE_AVAILABLE` to every consumer that, according to the heartbeat map, still holds references to v1.
- Greedy consumers (capability `ddsr.consumer.greedy=true`) rebind automatically when the event arrives.
- Once the heartbeat map shows no consumer holding a v1 reference, the broker sends `RETIRED` to nobody (there is nobody left) and removes v1 from the registry.
- **No hard timeout.** v1 may live for as long as it likes, as long as consumers are on it.

### 2.3 `HARD_CUTOVER`

The new version is published; after a short failover window the old one is retired by force. Consumers get `UNREGISTERING` and must rebind (or fail).

**When:** a security fix, a bug killer, a breaking change with no migration budget.

**Broker behaviour (the order is load-bearing):**

1. **Phase 1 — take both:** `publishImplementation(v2, policy=HARD_CUTOVER, replaces=v1)` takes v2 in. v1 stays available for the moment.
2. **Phase 2 — failover window:** the broker waits a configurable interval (default `30s`; configurable per implementation via `cutoverGraceMillis`). During this window consumers can see both v1 and v2 — they may deliberately rebind to v2 if they want to.
3. **Phase 3 — unregister event:** the broker sends `ServiceEvent.UNREGISTERING(impl=v1, reason=BROKER_CUTOVER)` to every consumer in the heartbeat map holding references to v1. For stream channels (see `WIRE_CHANNELS.md`) the broker initiates the graceful close on both the provider and the consumer side.
4. **Phase 4 — retire:** the broker removes v1 from the catalog and the registry. From then on `find()` returns only v2.

**Why v2 first and only then retire v1, not the other way round:** in the short overlap of phase 2 the consumer has a real failover option. Otherwise (v1 out first, then v2 in) there would be a window with *no* implementation at all — calls in flight would fail hard.

## 3. Trigger model — everything over events, no polling

Consumers learn about policy changes through the event stream (a stage-3 feature, OPEN_ISSUES A1). Polling is explicitly not foreseen — we want reactive consumers.

**Event types the broker sends:**

| Event | When | Reaction expected |
|---|---|---|
| `UPGRADE_AVAILABLE` | on a `DEPRECATE_AND_DRAIN` publish | a hint; the consumer decides for itself |
| `UNREGISTERING` | on `HARD_CUTOVER` phase 3, or on `withdrawImplementation()` | the consumer **must** rebind, otherwise it fails |
| `RETIRED` | after the old implementation was actually removed | a cleanup hint; no action required (it should have rebound long since) |
| `REGISTERED` | on a new implementation publish (whatever the policy) | information for interested subscribers |

**Greediness as a consumer property** (`ddsr.consumer.greedy`, default `false`):

- `greedy=true`: the consumer facade rebinds on `UPGRADE_AVAILABLE` automatically. Suitable for long-running services that always want "the newest API".
- `greedy=false`: the consumer facade passes the event through to the application code, which decides for itself (the default — more defensive).

`UNREGISTERING` does not depend on greediness; the consumer **must** react in any case.

## 4. Heartbeat protocol (prerequisite for drain and C3)

The broker has to know reliably which consumer holds which references, otherwise neither `DEPRECATE_AND_DRAIN` auto-retire nor general stale cleanup (OPEN_ISSUES C3) works.

> **Made precise in [ACQUISITION.md](ACQUISITION.md):** the session owns the leases (a registration only gets the derived view), acquire/release/heartbeat collapse into one idempotent full replace at `PUT /consumers/{id}`, leases are deliberately not persisted (mind the restart grace rule for auto-retire), and `ServiceReference.usingProviders` is deprecated (M5). The sketch below stays as the origin; where the two disagree, ACQUISITION.md wins.

**Data model in the broker:**

```
Map<ConsumerId, ConsumerState>
  ConsumerState
    lastHeartbeat : Instant
    activeRefs    : Set<ServiceReferenceId>
    capabilities  : ConsumerCapability   (greedy, supportedFlavors, …)
```

**Heartbeat protocol:**

- The consumer sends a `ConsumerHeartbeat` to the broker every **10 minutes**, carrying its current `activeRefs` list.
- The broker updates `lastHeartbeat` and the reference map.
- If a heartbeat is missing for **2× the interval** (20 minutes), the broker marks the consumer as dead and releases all of its references.

**Shutdown notify:**

- On a regular shutdown the consumer sends a `ConsumerShutdown` event to the broker (all active references are released). That makes a drain prompt without the 20-minute wait.
- Best effort — if the consumer crashes, the heartbeat timeout takes over.

**Solving OPEN_ISSUES C3 as a side effect:** the same protocol applied to the provider side gives us provider reachability probing. A provider sends a `ProviderHeartbeat` every 10 minutes; after 20 minutes without one the broker retires the provider entry including all of its implementations.

**Trade-off:** a dead consumer is counted as a live reference holder for up to 20 minutes. Not ideal for drain semantics, but pragmatic — the alternative model (a round trip to the broker per `getService`/`ungetService`) would be an order of magnitude more traffic for marginally more accuracy.

## 5. Version negotiation at lookup

So that consumers in `EVERGREEN` setups can ask for a specific version, the lookup API is extended:

- `find("Payment")` — the default: the highest non-deprecated version, filtered by consumer capabilities.
- `find("Payment", versionRange="[2.0,3.0)")` — an explicit range in OSGi version range syntax.
- `ConsumerCapability.supportedVersions : Map<InterfaceName, VersionRange>` — a consumer can declare a range per interface that the broker applies to all its lookups automatically (as it does for `supportedFlavors`).

**Deprecation filter:** by default `deprecated=true` implementations are removed from the lookup result. With `find("Payment", includeDeprecated=true)` a consumer can see deprecated versions as well (a management use case, migration tooling).

## 6. Relation to the hook architecture

The update policy is the first concrete use of the three hooks from the REQUIREMENTS document:

- **`PublishHook`** — enforces the policy on the publish side. If a provider calls `publishImplementation(v2)` with no `replaces` hint, the hook could turn that into an `EVERGREEN` add, or refuse the publish when the policy demands `HARD_CUTOVER` but `cutoverGraceMillis` is missing.
- **`DiscoveryHook`** — filters `deprecated=true` out for non-greedy consumers; applies the `versionRange` filter.
- **`DistributionHook`** — on `UNREGISTERING` of stream channels: triggers the graceful close on both sides (see `WIRE_CHANNELS.md`, stream termination).

The full hook specification gets a document of its own; only the attachment points are named here.

## 7. Cross-constraints with streams

On a `HARD_CUTOVER` of a stream service: the `UNREGISTERING` event goes to the consumer and the provider. Both initiate a graceful close of their end of the stream (reason code `BROKER_CUTOVER`). Whoever sends first, the other receives it as a wire close frame and answers symmetrically. The stream ends cleanly; new calls against v1 fail with `SERVICE_RETIRED`.

On a `DEPRECATE_AND_DRAIN` of a stream: the stream counts as an active reference. v1 is retired only once all stream channels have ended gracefully. Long streams can therefore keep v1 alive indefinitely — that is by design.

The stream termination mechanism itself is specified in `WIRE_CHANNELS.md`.

## 8. Implementation order

The features build on each other. Suggested order:

1. **The heartbeat protocol** (solves C3, is the prerequisite for everything else). Valuable standalone.
2. **The event stream** (OPEN_ISSUES A1). The prerequisite for any active notification. Once the stream channel extension from `WIRE_CHANNELS.md` is there, it becomes a concrete event channel.
3. **The policy annotation on the model** (`ServiceInterface.updatePolicy`, `ServiceImplementation.updatePolicy`, the `replaces` reference, the `deprecated` flag, `cutoverGraceMillis`). A model extension; build it straight into the v2 workspace.
4. **Broker logic per policy**, in the order EVERGREEN → DEPRECATE_AND_DRAIN → HARD_CUTOVER. EVERGREEN is trivial (today's dedup logic without the retire). DEPRECATE_AND_DRAIN needs the deprecation filter plus a drain watcher. HARD_CUTOVER needs a failover window timer plus `UNREGISTERING` distribution.
5. **The version range at lookup** — can come earlier, it is orthogonal.

## 9. Open points

- **The default policy:** `DEPRECATE_AND_DRAIN` is proposed above. The alternatives are `EVERGREEN` (safer for unknown consumers) or **no default at all** (a model validator forces an explicit statement). To be decided.
- **`cutoverGraceMillis` default:** 30s is a guess. Probably depends on the kind of service (longer for interactive ones, shorter for batch backends).
- **Heartbeat interval:** 10 minutes is pragmatic. If drain turns out to be too slow, shorten it (a traffic trade-off).
- **`replaces` semantics for multi-step migrations:** if v1 → v2 → v3 are published one after another, v3 is the `replaces` of v2 (which is the `replaces` of v1). Is that enough, or should the broker resolve a chain ("retire v1 as soon as v3 is published, not only once v2 is retired")?
- **Multi-catalog:** if an interface exists in several catalogs at once (stage 4+), does the policy apply per catalog or globally? For now: per catalog. But that has to be settled together with the federation discussion.

## 10. References

- [ARCHITECTURE.md](ARCHITECTURE.md) — the broker as it stands today, especially §2.6 (dedup, reindex)
- [WIRE_CHANNELS.md](WIRE_CHANNELS.md) — the channel model, stream termination, the capability/requirement system
- OPEN_ISSUES.md — A1 (SSE/event stream), C2 (provider-aware lookup), C3 (stale providers)
- REQUIREMENTS.md — the stage-2 DoD and the cross-language demo flow

## 11. Implementation status (2026-09-15, issue #45)

The broker implements §2 as soon as a publish carries `ServiceImplementation.replaces`. Without `replaces` everything stays as it was: the same `(name, version)` is replaced synchronously (`UNREGISTERING` with reason `REPLACED`, then `REGISTERED`), a different identity coexists.

**Resolution.** `replaces` arrives as a stub from the wire and is rewired onto the live registered implementation with the same `(name, version)`. No hit, or its own identity: WARNING `CODE_IMPL_REPLACES_NOT_FOUND` (213), `replaces` is cleared and the publish goes through as an ordinary publish — a successor starting up whose predecessor is long gone must not be locked out.

**Effective policy.** `ServiceImplementation.updatePolicy`, else the strictest `updatePolicy` among the interfaces it serves (`HARD_CUTOVER` > `DEPRECATE_AND_DRAIN` > `EVERGREEN`), else `DEPRECATE_AND_DRAIN`. `UNSPECIFIED` means "inherit" on both levels.

| Policy | on publishing the successor | retiring the predecessor | events for the predecessor |
|---|---|---|---|
| `EVERGREEN` | nothing | only through `withdrawImplementation` | none |
| `DEPRECATE_AND_DRAIN` | the predecessor drops out of `getServiceReferences` (not out of `getAllServiceReferences`), leases stay valid | a sweep, as soon as no `ConsumerSession` holds a lease any more | `UPGRADE_AVAILABLE` after the successor's `REGISTERED`; on retire `UNREGISTERING` + `RETIRED` (reason `REPLACED`) |
| `HARD_CUTOVER` | both visible (the failover window) | a sweep after `cutoverGraceMillis` (0 = the broker default `cutover.grace.seconds`, 30 s), leases are ignored | `UNREGISTERING` + `RETIRED` (reason `CUTOVER`) |

**The sweep.** `BrokerImplementations.advanceUpdatePolicies(now)`, run in the broker every `policy.sweep.seconds` (default 5 s). Withdrawing the predecessor cancels the supersession; withdrawing the successor aborts the drain or cutover and the predecessor becomes ordinarily visible again. Both parties of a supersession are skipped by the cold cache sweep.

**Deliberate deviations and limits.**
- The supersession is runtime state like the leases: a broker restart forgets drains and cutovers in progress (fail-safe, nothing is retired by accident). `replaces` itself is in the snapshot; on retire it is cleared on the successor so that nothing dangles.
- `deprecated=true`/`replacedBy` on the *interface* are not touched by the policy path — that stays `deprecateCatalogEntry`. The visibility rule works on the registration, not on the catalog.
- A predecessor in the cold cache is not drained; the publish is then an ordinary publish.
- No `UPGRADE_AVAILABLE` on `HARD_CUTOVER` (§3 foresees the hint only for a drain); `RETIRED` is emitted only by the policy path, an explicit withdraw stays at the plain `UNREGISTERING`.
- Greedy rebind in the consumer (§3) is implemented in both SDKs: `greedy_rebind` (Java, PID `org.eclipse.fennec.services.client`) or `greedyRebind` (TS) makes a locator move to the successor on its next call after `UPGRADE_AVAILABLE`; without the option the binding stays until the broker retires it (CLIENT_FRAMEWORK_GUIDE §5.3/§6).
- The provider heartbeat (§4, #52) is implemented — as its own light path rather than a provider session: `PUT /references/{id}/heartbeat?intervalSeconds=N` per registration, opt-in (whoever never heartbeats is never retired for silence). The broker retires after `2 × interval` of silence (`liveness.sweep.seconds`, default 5 s) with `UNREGISTERING` + `RETIRED`, reason `PROVIDER_LOST`. A lost predecessor of a supersession = retired (the drain ends), a lost successor = the drain is aborted and the predecessor becomes visible again. SDKs: `provider_heartbeat_seconds` (Java, default 30) / `providerHeartbeatSeconds` (TS); a 404 on the heartbeat means "the broker does not know us any more" and triggers a republish with a rebind of the `Registration` handle. A deviation from the sketch above: 30 s instead of a 10 minute default, because otherwise a dead endpoint stays resolvable for consumers far too long. Harness scenario H (`kill -9`) proves the path end to end.
- Still open: `versionRange`/`includeDeprecated` at lookup (§5), `PublishHook` enforcement (§6).
