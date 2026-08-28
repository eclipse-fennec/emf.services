# DDSR Client Framework — Developer Guide

**Audience:** Java + TypeScript implementers (you, the colleague, and Claude assisting). This document is *operational*, not narrative — it tells you exactly which modules to create, which endpoints to hit, which class names to use, and how to verify each step against the running broker.

**Status:** drafted on 2026-05-27 against broker version `1.0.0.202605271720-SNAPSHOT`. Update the version stamp whenever the broker's wire format changes.

**Implementation progress (end of 2026-05-27):**
- Java SDK skeleton complete: `org.eclipse.fennec.services.client.java` compiles green, all interfaces and impls from §5 are in place. See module layout below.
- Code-level deviations from §5 sketch (intentional):
  - Java HTTP client built on `java.net.http.HttpClient` (no third-party dependency).
  - Publish payload is *multi-root XMI*: `ServiceProvider` (with one containment `ServiceImplementation`) as the first root, plus stub copies of the referenced `ServiceInterface`s (name + version only) as sibling roots. This lets the broker resolve `serviceInterfaces` cross-refs by name inside the wire document.
  - `ProviderImpl.publish` does a *follow-up lookup* to discover the broker-assigned reference id (the broker's publish response is a Diagnostic without an id today — see §10 TODO).
  - `ServiceLocator.urlFor(...)` falls back to the broker origin when `RestFlavor.host` is null/empty — works for the self-published broker API and for co-hosted demo providers.
- **Open before smoke-test runs:** see §10 → "Broker must accept multi-root XMI on POST /implementations".

**Cross-refs:** `docs/REQUIREMENTS.md` for the *what*, `org.eclipse.fennec.services.broker.core/...` for the in-tree reference impl, `org.eclipse.fennec.services.model/model/ddsr-model-spec.md` for the model.

---

## 0. TL;DR — What we build tomorrow

Two **client frameworks** that talk to the existing in-tree broker (`org.eclipse.fennec.services.broker.rest`, currently running on Eclipse `localhost:8887/ddsr/rest`):

- `org.eclipse.fennec.services.client.java` — Java client SDK with provider-side and consumer-side facades.
- `ts/ddsr-client` (separate repo / npm package, owned by the colleague) — TypeScript equivalent with **identical observable behavior**.

Both frameworks consume the broker's *self-published* API (`BrokerCatalog` + `BrokerImplementations` + `BrokerLookup`). The broker is the only HTTP component; the client frameworks are libraries embedded in user code.

End-of-day goal: a Java provider publishes `Payment`, a Java consumer looks it up, both via the new SDK — no curl, no hand-XMI. TypeScript track follows identical patterns.

---

## 1. Architecture — Three layers per language

Locked by `REQUIREMENTS.md NFR-Three-Layer-Architecture`:

| Layer | Java | TypeScript |
|---|---|---|
| **EMF / POJOs** (generated from `ddsr.ecore`) | EMF (already generated under `org.eclipse.fennec.services.model/src/`) | `ecore.ts` (own project) generates POJOs from same `ddsr.ecore` |
| **Native lifecycle / DI** | OSGi DS (this prototype) — `@Component`, `@Reference`, `@Activate` | Daanse TSM — module loading + DI |
| **DDSR-own** (registry, lookup, events, broker bridge) | hand-written; this guide's primary subject | hand-written; **identical observable behavior** to the Java code |

The DDSR-own layer is what we build tomorrow. The two lower layers already exist.

---

## 2. Repository layout

In `org.eclipse.fennec.services.client.java` (Java) — to be created tomorrow:

```
org.eclipse.fennec.services.client.java/
├── bnd.bnd
├── src/
│   └── org/eclipse/fennec/services/client/
│       ├── DdsrClient.java          — facade / entry point
│       ├── DdsrProvider.java        — provider-side API
│       ├── DdsrConsumer.java        — consumer-side API
│       ├── ServiceListener.java     — re-export of model interface, for ergonomics
│       └── internal/
│           ├── BrokerHttpClient.java   — thin HTTP+XMI wrapper around broker REST
│           ├── ProviderImpl.java       — DdsrProvider implementation
│           ├── ConsumerImpl.java       — DdsrConsumer implementation
│           └── DdsrClientComponent.java — DS adapter
```

In `org.eclipse.fennec.services.client.java.example` (or merged into example.model):

- A `PaymentProvider` DS-component that uses `DdsrProvider` to publish a Payment implementation.
- A `PaymentConsumer` DS-component that uses `DdsrConsumer` to look one up and invoke it.

TypeScript track lives in its own repo; mirror the package shape:
- `packages/ddsr-client/src/{DdsrClient,DdsrProvider,DdsrConsumer,...}`
- `packages/ddsr-client-example/src/{paymentProvider,paymentConsumer}`

---

## 3. Wire format & transport

**Transport:** HTTP/1.1 against the broker base path `http://<host>:8887/ddsr/rest`. SSE for events arrives in Increment 3 — for the first iteration we focus on the synchronous request/response path.

**Body format:** XMI (UTF-8, `application/xml` Content-Type). All payloads are model elements (`ServiceInterface`, `ServiceProvider` with contained `ServiceImplementation`, `ServiceReference`, `Diagnostic`). XMI is the **prototype-only** format (NFR-Persistability); JSON is deferred.

**Round-trip rule:** any EObject the client receives MUST be safe to mutate — the broker returns deep copies via `EcoreUtil.copy`. Any EObject the client sends MUST be fresh — never re-send an object that's already in the client's local registry, because EMF containment is exclusive and serializing it again would steal it. (We hit this bug in the broker once already, see commit history.)

**Headers:**
- `Content-Type: application/xml` on POST/PUT/DELETE-with-body
- `Accept: application/xml`
- `X-DDSR-Requestor: <symbolic name>` (catalog mutations only, for audit)

---

## 4. Broker API — cheat sheet

The broker is *self-published* via the `BrokerCatalog`, `BrokerImplementations`, `BrokerLookup` interfaces. A consumer can fetch the live definition any time with:

```
GET /ddsr/rest/references?interface=BrokerCatalog&flavors=REST
```

For the manual build the table below is the source of truth. **If the broker's self-published definition disagrees with this table, the broker wins** — update the table.

### 4.1 Catalog (governance officer scope — typically operator, not provider/consumer code)

| Method | Path | Request body | Response body | Status codes |
|---|---|---|---|---|
| GET | `/catalog` | — | `RemoteServiceRegistry` snapshot | 200 |
| POST | `/catalog` | `<services:ServiceInterface>` | `Diagnostic` | 200 ok / 409 already exists |
| PUT | `/catalog/{name}/deprecate` | optional `<services:ServiceInterface deprecationReason="..."/>` | `Diagnostic` | 200 / 404 not found |
| DELETE | `/catalog/{name}` | — | `Diagnostic` | 200 / 404 / 409 (live impls) |

### 4.2 Implementations (provider scope)

| Method | Path | Request body | Response body | Status codes |
|---|---|---|---|---|
| POST | `/implementations` | `<services:ServiceProvider>` containing **exactly one** `<implementations>` child | `Diagnostic` | 200 / 200+WARNING (deprecated iface) / 403 (ownership) / 422 (iface not in catalog) |
| DELETE | `/implementations` | same shape: provider with the impl to withdraw | `Diagnostic` | 200 / 403 / 404 |

The single-impl-per-request shape is deliberate: it matches the model's containment ownership ("the provider owns the impl") and avoids ambiguity. To publish N impls, send N requests.

### 4.3 Lookup (consumer scope)

| Method | Path | Query params | Response body | Status |
|---|---|---|---|---|
| GET | `/references` | `interface=NAME` (required), `filter=LDAP` (opt, no-op for now), `flavors=REST,MQTT` (opt), `consumerId=ID` (opt) | Multi-root XMI: `LocalServiceRegistry` envelope (references + providers with impls + flavors as containment) **plus** referenced `ServiceInterface` objects as siblings | 200 / 400 (no interface) |

The multi-root response is intentional — gives the client everything for invocation in one call. Parse the document with EMF's `XMLResource.load(...)`: the resource's `getContents()` will have one envelope plus N interface roots.

### 4.4 Registry snapshot (admin)

| Method | Path | Response |
|---|---|---|
| GET | `/registry` | Complete `RemoteServiceRegistry` XMI |

### 4.5 Diagnostic interpretation

Every mutation returns a `Diagnostic`. Read `severity` first:

- `OK` / `INFO` → success
- `WARNING` → success but watch the message and `code` (e.g. `code=300 INTERFACE_DEPRECATED`)
- `ERROR` / `CANCEL` → action did not happen

`code` constants (mirrors `org.eclipse.fennec.services.broker.core.DdsrDiagnostics`):

| Code | Meaning | Typical HTTP |
|---|---|---|
| 0 | OK | 200 |
| 100 | NETWORK_PARTITION | 503 |
| 200 | CATALOG_HAS_LIVE_IMPLS | 409 |
| 201 | CATALOG_ENTRY_NOT_FOUND | 404 |
| 202 | CATALOG_ENTRY_ALREADY_EXISTS | 409 |
| 210 | IMPL_INTERFACE_NOT_IN_CATALOG | 422 |
| 211 | IMPL_OWNERSHIP_VIOLATION | 403 |
| 212 | IMPL_NOT_PUBLISHED | 404 |
| 300 | INTERFACE_DEPRECATED | 200+WARNING |
| 500 | PERSISTENCE_FAILED | 503 |

The client SDK should expose these as a `DdsrDiagnostics`/`DdsrDiagnosticCode` enum so consumers can branch on the code without parsing the integer.

---

## 5. SDK shape (Java track)

### 5.1 `DdsrClient` — facade

```java
public interface DdsrClient {
    /** Provider-side API (publish / withdraw). */
    DdsrProvider provider();

    /** Consumer-side API (lookup / listen). */
    DdsrConsumer consumer();

    /** The broker base URL this client talks to. */
    URI brokerUrl();

    void close();
}
```

Construction in OSGi: a DS component `DdsrClientComponent` reads `broker.url` from config (default `http://localhost:8887/ddsr/rest`) and registers `DdsrClient` as a service. Outside OSGi: `DdsrClients.connect(URI brokerUrl)` factory.

### 5.2 `DdsrProvider`

```java
public interface DdsrProvider {

    /**
     * Publish an implementation of a known catalog ServiceInterface.
     * Returns a Registration handle the caller MUST keep — calling
     * withdraw() on it removes the implementation.
     */
    Registration publish(ServiceProvider self, ServiceImplementation implementation);

    /** Look up a previously returned Registration by its reference id. */
    Registration registrationOf(String referenceId);
}

public interface Registration {
    ServiceReference reference();
    ServiceImplementation implementation();
    /** Removes the impl from the broker. Idempotent. */
    Diagnostic withdraw();
}
```

Internally `publish(...)` does:
1. Wrap provider+impl into an XMI POST to `/implementations`.
2. Parse `Diagnostic` response. On WARNING surface the message but proceed.
3. On OK: do a follow-up `GET /references?interface=...&flavors=...` to retrieve the assigned reference id (or, better, the broker returns the reference id in the diagnostic message — TODO for Increment 3).

### 5.3 `DdsrConsumer`

```java
public interface DdsrConsumer {

    /**
     * Look up one or more references for an interface. The capability
     * is constructed from the client's installed flavor plugins (REST,
     * MQTT once available).
     */
    List<ServiceLocator> find(String interfaceName, String filter);

    /** Subscribe to lifecycle events (REGISTERED / MODIFIED / UNREGISTERING). */
    Subscription subscribe(String interfaceName, ServiceListener listener);
}

/**
 * Pair of reference + the implementation/flavor details needed to invoke.
 * Built from the broker's multi-root lookup response.
 */
public interface ServiceLocator {
    ServiceReference reference();
    ServiceImplementation implementation();
    /** First RestFlavor in implementation.flavors, or null. */
    RestFlavor restFlavor();
    /** Convenience: full URL for a named operation. */
    URI urlFor(String operationName);
}
```

`subscribe(...)` is a no-op stub until Increment 3 lands SSE — implementation can return a `Subscription` whose `cancel()` is a no-op, and document the gap.

### 5.4 `BrokerHttpClient` — the only place that talks HTTP

Keep ALL HTTP and XMI handling here. Implementations of `DdsrProvider` / `DdsrConsumer` go through `BrokerHttpClient`. This makes it possible to:

- Test the SDK without a real broker (mock the HttpClient).
- Add caching / retries in one place.
- Plug in a different transport later (e.g. a Lucene-fed gRPC, if we go there).

Recommended Java HTTP client: `java.net.http.HttpClient` (JDK built-in, no extra dependency). For XMI use `XMIResource` from EMF, exactly as the broker's `XmiSupport` does.

### 5.5 Module layout & buildpath

`bnd.bnd`:

```
Bundle-Name: DDSR Client (Java)
-library: enableEMF

Export-Package: org.eclipse.fennec.services.client
Private-Package: org.eclipse.fennec.services.client.internal

-buildpath: \
    org.osgi.service.component.annotations;version=latest,\
    org.osgi.service.condition;version=latest,\
    org.eclipse.fennec.services.model;version=snapshot
```

(No direct `broker.core` dependency — the SDK only knows the broker via HTTP.)

---

## 6. SDK shape (TypeScript track)

Mirror Java. Concept-equivalence is what the parity test asserts; the API surface should *read* analogous.

```ts
export interface DdsrClient {
  readonly provider: DdsrProvider;
  readonly consumer: DdsrConsumer;
  readonly brokerUrl: URL;
  close(): Promise<void>;
}

export interface DdsrProvider {
  publish(self: ServiceProvider, impl: ServiceImplementation): Promise<Registration>;
  registrationOf(referenceId: string): Promise<Registration | undefined>;
}

export interface DdsrConsumer {
  find(interfaceName: string, filter?: string): Promise<ServiceLocator[]>;
  subscribe(interfaceName: string, listener: ServiceListener): Subscription;
}
```

Use `ecore.ts` for the EMF POJOs (they should be generated from the same `ddsr.ecore` we already use in Java). HTTP: `fetch` (Node 18+ / browser). XMI parsing: piggy-back on `ecore.ts` if it exposes a resource loader, otherwise a small DOM parser + factory dispatch.

For the TSM integration: register the `DdsrClient` as a TSM service so consumer modules can `@inject` it.

---

## 7. Behavioral-Parity contract

These are the **observable** behaviors that MUST match between Java and TS implementations (per `NFR-Behavioral-Parity`):

| Aspect | Required behavior |
|---|---|
| Publish semantics | A single `provider.publish(self, impl)` is one HTTP POST. Successful return populates `Registration.reference()` with a non-null id. WARNING on deprecated interface is exposed, action still succeeds. |
| Lookup result ordering | (Currently registration order — backend-specific; will be ranking once Lucene lands. For now, order is irrelevant to parity *as long as both langs preserve the broker's response order verbatim*.) |
| Flavor matching | Lookup with `flavors=[REST]` returns only locators whose impl has at least one matching `RestFlavor`. Lookup with `flavors=[MQTT]` returns empty for REST-only impls. Lookup with no flavors → null `ConsumerCapability` → all matches returned (consistent with Java `lookup.LookupResource` behavior). |
| Error mapping | Diagnostic code → exposed as the same SDK-level enum/code on both sides. Examples: `CATALOG_ENTRY_ALREADY_EXISTS=202`, `IMPL_OWNERSHIP_VIOLATION=211`. |
| Idempotence | Calling `publish` twice with the same `provider.name + impl.name` is *not* deduplicated by the SDK — both calls go to the broker and the second gets the broker's response (which may be OK or an error depending on broker idempotency rules). The SDK does NOT silently swallow duplicates. |
| Round-trip of EObjects | Whatever you receive (e.g. a `ServiceImplementation` from a lookup) you can re-send on a subsequent `publish` on a different broker without `EcoreUtil.copy()` because what you received is *already* a copy. The SDK guarantees this. |

The parity test suite lives in *both* repos and tests each SDK against the running broker; both suites must pass for an iteration to be "done".

---

## 8. Smoke-test sequence — Day 1 happy path

Run order, both languages independently against the same broker. Each step verifiable via MCP / curl from the side, see §9.

| Step | Action | Verify |
|---|---|---|
| 1 | `DdsrClient.connect(http://localhost:8887/ddsr/rest)` | Health: `GET /catalog` returns 200 |
| 2 | `consumer.find("BrokerCatalog", null)` returns 1 locator | locator has `restFlavor().basePath == "/ddsr/rest"`, 4 operationFlavors |
| 3 | Seed a fresh `Payment` interface via the SDK (calls `POST /catalog` under the hood — exposed via `consumer().governance().add(...)` or similar admin facade) | broker shows 4 catalog entries via MCP gogo or curl |
| 4 | Provider builds a `ServiceProvider("payments-java", impl=ServiceImplementation("payments-java-rest", interfaces=[Payment], flavors=[RestFlavor(basePath="/payments")]))` and calls `provider.publish(self, impl)` | `Registration.reference().id` is non-null UUID |
| 5 | Consumer `consumer.find("Payment", null)` | one locator, basePath `/payments`, operationFlavors for charge / getBalance |
| 6 | Provider `registration.withdraw()` | next `find` returns empty list |

When all 6 steps pass for Java, repeat with TS. When all 6 pass for *both*, swap roles: Java provider + TS consumer, then TS provider + Java consumer. Cross-language parity demonstrated.

---

## 9. MCP tools — verifying live state

When implementing, do not rely solely on the SDK's return values. Always verify against the broker's actual state.

### Via the `osgi-gogo` MCP server (this Claude session has this tool):

```
mcp__osgi-gogo__execute_gogo  command="lb -s | grep ddsr"
mcp__osgi-gogo__execute_gogo  command="felix:inspect cap service 70"   # broker.core bundle
mcp__osgi-gogo__execute_gogo  command="scr:info org.eclipse.fennec.services.broker.core.internal.DdsrBrokerComponent"
```

### Via direct curl from the dev box:

```bash
# Catalog state
curl -s http://localhost:8887/ddsr/rest/catalog | xmllint --format -

# Registry snapshot (all containment)
curl -s http://localhost:8887/ddsr/rest/registry | xmllint --format -

# What the broker self-publishes about itself
curl -s 'http://localhost:8887/ddsr/rest/references?interface=BrokerCatalog&flavors=REST'
```

### XMI on disk (persistence verification):

```
/opt/git/kloster-prototype/org.eclipse.fennec.services.broker.rest/broker-state.xmi
```

After every SDK mutation, this file MUST reflect the new state. If it doesn't: the broker's persistence path has a bug (we hit this once — see `XmiSupport.toXmi` → `EcoreUtil.copy` fix).

### Useful Eclipse Console search terms

`[DDSR]` — the broker logs key activation/publish events with this prefix.

---

## 10. Known gaps as of writing

These are not in scope for the Day-1 SDK but the SDK must not lock us out of them:

| Gap | Impact on SDK | Mitigation |
|---|---|---|
| **SSE event stream** | `consumer.subscribe()` is a stub. | Define the listener interface today, swallow no-op until Increment 3. |
| **LDAP filter evaluation** | `find(...)` ignores the `filter` arg. | Accept it, pass to broker; broker ignores; document. |
| **Service ranking** | Lookup order is registration order. | Document; do not bake order-sensitive logic into tests. |
| **`getServiceReference` (singular)** | Broker exposes only the list form via REST. | SDK can offer a `findOne` that calls `find` and returns first or null. |
| **Reference-id discovery on publish** | Broker's publish response is a Diagnostic without the reference id. | SDK does a follow-up lookup. **TODO**: ask broker to include the reference id in the OK diagnostic. |
| **Authentication / PDP** | No auth on the broker yet. | SDK has no auth surface. Add a request-interceptor hook on `BrokerHttpClient` so we can plug in headers later. |
| **Broker reads single-root XMI on POST /implementations** | Java SDK sends multi-root XMI (provider + interface stubs). `ImplementationsResource.readProviderWithImpl` currently throws on multi-root. | **First task tomorrow**: change `XmiSupport.fromXmi` / `fromXmiResource` so the implementations resource picks the first `ServiceProvider` root from the parsed resource, regardless of how many sibling roots are present. |
| **Example provider/consumer modules** | `org.eclipse.fennec.services.client.java.example` not yet created. | Stub with `PaymentProvider` + `PaymentConsumer` DS components — needed for the §8 smoke-test sequence. |
| **`launch.bndrun` does not include `org.eclipse.fennec.services.client.java` yet** | Bundle compiles but isn't part of any run. | Add `bnd.identity;id='org.eclipse.fennec.services.client.java'` to the rest module's `-runrequires` once the example consumer/provider lives somewhere that requires it (or, until then, install it manually via gogo `install file:...`). |

---

## 11. Quick reference — `ddsr.ecore` core types (the SDK will touch these)

From `org.eclipse.fennec.services.model.ddsr`:

- `ServiceInterface(name, version, description, operations[], exceptions[], invariants[], status, deprecationReason, replacedBy)`
- `ServiceOperation(name, description, parameters[], returnType, returnConstraints[], exceptions[], preconditions[], postconditions[])`
- `Parameter(name, index, type, optional, defaultValue, description, constraints[])`
- `ParameterConstraint` abstract; concrete: `RequiredConstraint`, `NumericRangeConstraint(min,max,inclusiveMin,inclusiveMax)`, `StringPatternConstraint(pattern,minLength,maxLength)`, `EnumerationConstraint(allowedValues[])`, `CollectionSizeConstraint(minSize,maxSize)`, `ExpressionConstraint(language, expression, message)`
- `ServiceException(name, version, type, description, properties[])`
- `ServiceProvider(name, version, symbolicName, descriptions[], implementations[])`  *(implementations are CONTAINMENT — exclusive ownership)*
- `ServiceImplementation(name, version, implementationId, description, serviceInterfaces[], flavors[], properties[], componentDescription?)`
- `ServiceFlavor` abstract; concrete: `RestFlavor(host, basePath, contentTypes[], operationFlavors[])`, `MqttFlavor(brokers[], requestTopic, responseTopic, defaultQos, defaultRetained, operationFlavors[])`
- `RestOperationFlavor(operation→ServiceOperation, method, path, returnCodes[], consumes[], produces[])`
- `ServiceReference(id, properties[], provider, usingProviders[], registration)`
- `ServiceRegistration(reference, unregistered)`
- `ConsumerCapability(consumerId, supportedFlavors[], properties[])`
- `Diagnostic(severity, message, source, code, data[], children[])`
- `LocalServiceRegistry(...)` / `RemoteServiceRegistry(catalog[], implementations[], providers[], endpoint)` *(used as response envelopes)*

`name iD=true` on `NamedElement` mixin means **names are XMI-document-unique**. Cross-references in the wire XMI resolve by name — relevant for the multi-root lookup response.

---

## 12. Notes for Claude (operational instructions)

These are pointers for me (the AI) when the user asks me to implement parts of this guide:

- **Module skeleton**: each new Java module needs `bnd.bnd`, `.project`, `.classpath`, `.settings/{org.eclipse.jdt.core.prefs, org.eclipse.core.resources.prefs}`. Without the Eclipse files Bndtools won't recognize it (we hit this with `broker.core`). Copy structure from `org.eclipse.fennec.services.broker.core/`.
- **EMF + bndtools**: `Write` of `.project` files can strip nested XML — verify with `Read` after writing.
- **`bndtools` library names**: use `enableEMF` in module bnd.bnd; the `cnf/build.bnd` references the workspace-level libraries (`fennec`, `fennecTest`, `fennecJacoco`, `fennecEMF`, `fennecCodec`, `fennecMCP`).
- **EMF containment trap**: never do `someResource.getContents().add(eo)` on a live EObject — it moves it out of its existing container. Always `EcoreUtil.copy(eo)` first. See `broker.rest/.../XmiSupport.toXmi`.
- **`@Component(immediate=true)`** when a DS component must come up at bundle start and have nothing depending on its service yet (e.g. `BrokerSelfPublisher`).
- **`scr:info <implementation-class-fqn>`** is the most useful gogo command for diagnosing why a DS component isn't activating.
- **Iterating on Eclipse-launched bundles**: changes hot-reload usually, but if a new bundle is added to `-runrequires`, the run config needs an explicit *Resolve* + *Restart*. Watch for stale bundle IDs from cached resolves.
- **Persistence smoke**: after any mutation, peek at `broker-state.xmi` on disk; the file is the single source of truth that survives restarts.
- **MCP-gogo first, curl second**: when verifying live state, the gogo server gives more (DS state, bundle wiring) than HTTP alone.
- **Behavioral parity**: when implementing the Java SDK, deliberately keep method names / param order / return semantics one-to-one with the TS API spec in §6. If a divergence sneaks in, it shows up as a parity-test failure on the colleague's side. Cheap to keep in sync, expensive to retrofit.
- **Memory of decisions**: when the user makes a design call (e.g. "use TSM for module loading"), record it in `~/.claude/projects/-opt-git-kloster-prototype/memory/`. The next session reads it without re-asking.
