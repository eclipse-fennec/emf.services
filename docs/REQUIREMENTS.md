# Eclipse Fennec Services (Arbeitsname DDSR) — Requirements

**Status**: draft · **Working name**: DDSR (Dynamic Distributed Service Registry)
**Last updated**: 2026-07-02

> This is a lightweight vision/PRD. It captures *what* we are building and *why*,
> independent of *how* (which lives in `org.eclipse.fennec.services.model/model/ddsr-model-spec.md`).
> Outstanding design questions are tracked centrally in §8. Major decisions move
> from §8's "Open questions" list to its "Resolved" subsection rather than being
> deleted, so the rationale stays auditable.

---

## 1. Vision

DDSR provides a **language-independent service model** based on EMF/Ecore.
It abstracts the OSGi Declarative Services concepts (components, service
references, lifecycle, events) so that the same model can drive implementations
in **Java** and **TypeScript** as primary, co-developed targets. **Python** is
a planned future target but deferred — the model stays language-neutral so it
can be added later without redesign.

The Java target covers **two** runtime flavors that must both work: a **plain
Java** application (no OSGi container) and a **real OSGi runtime** with
interoperability to `org.osgi.framework.BundleContext`.

DDSR is delivered as a **library / framework** in both languages — not as
a standalone application — to be embedded by other software.

The "D" in DDSR stands for **Distributed**, in the classical SOA sense: there
is a single **Remote Registry** that acts as the **Service Directory / Broker**
(UDDI-style — OSGi Remote Services intentionally leaves this piece undefined).
Every language framework (Java/TS/Python) keeps a **local registry** for
in-process services and OSGi-`ServiceRegistry`-style lifecycle, and
**transparently delegates** to the Remote Registry for cross-process lookup.

The Remote Registry is **not a data channel** — provider/consumer
communication runs directly over the **transport flavor** the provider
advertises (e.g. MQTT, REST). The Registry is purely the directory:
who-offers-what-over-which-transport.

The Remote Registry also holds the **API catalog**: the canonical set of
service descriptions (interfaces, operations, parameters, constraints,
exceptions) that are *allowed* to exist in the system. The catalog is
present even when no implementation is registered yet — it is governed
separately from the provisioning of implementations (see §3 Governance
role).

---

## 2. Problem statement

OSGi gives Java developers a mature, well-specified service model
(registration, references, declarative wiring, events). Outside the JVM —
in TypeScript/Node, Python services, browser code, or polyglot edge
deployments — there is no equivalent shared vocabulary. Each language
reinvents service discovery, dependency injection, and lifecycle, and
none of them speaks the same wire format as an OSGi runtime.

DDSR aims to give all three target languages a **shared, model-driven
service vocabulary** that maps cleanly onto OSGi DS where that runtime
exists, and stands on its own where it does not.

---

## 3. Stakeholders

| Stakeholder | Interest |
|---|---|
| **Java/OSGi developers** | Want to expose existing OSGi services to TS consumers without writing bespoke bridges. |
| **Plain-Java developers** | Want the same service vocabulary in a non-OSGi Java application (standalone JVM, Spring/Quarkus-style apps, embedded). |
| **TypeScript/Node developers** | Want a DI/service container vocabulary that matches what the Java side speaks. |
| **Python developers** | *(deferred future target — not in the first iteration.)* |
| **Governance officer** | Curates and releases the **API catalog**: which service interfaces, operations, parameters and constraints are permitted in the system. Acts as gatekeeper before new service descriptions become active in the Remote Registry. |
| **Tooling / modelers** | Want a single Ecore model from which language bindings, validators, and documentation can be generated. |
| **Operations** | Want to inspect and reason about the service topology of a polyglot deployment uniformly. |

---

## 4. Primary use cases

These are the scenarios the first release should make demonstrably easy.
UC-1 to UC-3 describe per-language behavior; UC-4 is the canonical
cross-language distribution scenario; UC-5 and UC-6 cover the
catalog/codegen lifecycle.

### UC-1 — Provider registers a service

A service provider — in Java or TypeScript (Python deferred) —
describes its component declaratively, builds a `ServiceImplementation`
that references one or more `ServiceInterface`s from the API catalog
and advertises one or more transport flavors, then registers it with
its local `ServiceRegistry`. The registry emits a
`ServiceEvent(REGISTERED)` to local listeners and asynchronously
propagates the registration to the Remote Registry.

### UC-2 — Consumer looks up a service

A consumer queries the registry by interface name and optional LDAP
filter and receives a `ServiceReference` (or a collection thereof,
respecting cardinality). The consumer reads properties via
`getProperty` / `getPropertyKeys`.

### UC-3 — Component lifecycle is observable

A `ComponentDescription` is instantiated into one or more
`ComponentConfiguration`s. The configuration's `state`
(`UNSATISFIED_*`/`SATISFIED`/`ACTIVE`/`FAILED_ACTIVATION`) is queryable
and changes are observable. `failure` carries a structured `Diagnostic`
when activation fails.

### UC-4 — Cross-language service exchange over a Broker

This is the canonical distribution scenario:

1. The Governance officer has populated the **Remote Registry** API
   catalog with a service interface `Payment` (operations, parameters,
   constraints, exceptions).
2. A **Python provider** registers an implementation of `Payment` with
   its local Python DDSR framework. It advertises an **MQTT flavor**
   (broker URL, request/response topics).
3. A **Java provider** registers an independent implementation of the
   same `Payment` interface with its local Java DDSR framework. It
   advertises a **REST flavor** (base path, HTTP methods).
4. Both providers' local frameworks propagate the registration to the
   Remote Registry (in addition to local-listener notification).
5. A **TypeScript consumer** asks its local TS DDSR framework for a
   `Payment` reference. The framework attaches the consumer's
   **supported flavors** (e.g. REST only — no MQTT client plugin
   installed) and delegates to the Remote Registry.
6. The Remote Registry filters by interface + flavor compatibility and
   returns only the **Java/REST implementation reference**.
7. The TS framework uses its REST flavor client plugin to invoke the
   service over HTTP.
8. The consumer can also subscribe to a `ServiceListener` and receives
   events when matching implementations come and go.

### UC-5 — Reverse engineering: code → service description

Service descriptions for the API catalog can be **extracted from
existing Java, TypeScript, or Python source code** (e.g. annotated
interfaces / decorated classes / typed protocols) into the
`ddsr.ecore` model. This lowers the barrier for the Governance officer:
the catalog can be seeded from existing code instead of authored
by hand.

The extraction mapping must be **lossy-safe**: anything that does not
map cleanly (e.g. language-specific exception hierarchies) is preserved
as annotation or rejected with a Diagnostic, never silently coerced.

### UC-6 — Stub-assisted provider and consumer implementation

When a new catalog entry is released by the Governance officer, the
Code Publisher (see §5 Code distribution) emits a typed artifact per
language (JAR / npm package / wheel). Providers depend on the artifact
to subclass / implement the typed stub and fill in the business logic;
consumers depend on the same artifact to call the service through a
typed interface. **The generated code contains only types and POJOs**;
runtime behavior (marshalling, lifecycle, event delivery, transport)
lives once per language in the framework runtime — see
NFR-Three-Layer-Architecture and FR-Codegen-POJO-Only.

---

## 5. Functional requirements

### Modeling
- **FR-Model-1** The model MUST cover the OSGi DS concepts that
  `ComponentDescriptionDTO`, `ComponentConfigurationDTO`, `ReferenceDTO`,
  `Satisfied/UnsatisfiedReferenceDTO`, `ServiceReferenceDTO`, and
  `ServiceEvent` express, without Java-specific constructs in the core
  classes.
- **FR-Model-2** Service properties MUST be representable as typed
  values (string, int, long, double, float, short, boolean, string list)
  so that property values can round-trip across languages without
  losing type information.
- **FR-Model-3** Service interfaces MUST be first-class, identifiable,
  versioned entities (not opaque strings) and MUST carry their
  operation signatures: `ServiceInterface` contains `ServiceOperation`s
  with `Parameter`s, return type, and declared `ServiceException`s.
- **FR-Model-4** Activation failures MUST be representable as
  structured diagnostics (severity / message / source / code / data /
  nested children), not as language-specific stack traces.
- **FR-Model-5** Parameter validity MUST be expressible via
  composable, typed `ParameterConstraint`s (required, numeric range,
  string pattern, enumeration, collection size) so the same validation
  rules apply in every language.
- **FR-Model-6** A `ServiceImplementation` MUST be modeled separately
  from a `ComponentDescription`. An implementation can exist without a
  DS declaration; when DS-driven, it references the matching
  `ComponentDescription`.
- **FR-Model-7** Transport bindings MUST be first-class model
  citizens: `ServiceFlavor` (and per-operation `ServiceOperationFlavor`)
  with concrete subclasses for REST and MQTT in the prototype; the
  hierarchy is extensible via flavor plugins.
- **FR-Model-8** The model MUST distinguish a `LocalServiceRegistry`
  (in-process) from a `RemoteServiceRegistry` (broker, owns the API
  catalog and global implementation index) under a common abstract
  `ServiceRegistry` base.
- **FR-Model-9** Consumer-side flavor matching MUST be expressible via
  a `ConsumerCapability` (set of supported `FlavorKind`s, optional
  consumer id, optional properties), passed at lookup time, not
  persisted.
- **FR-Model-10** The model MUST expose **interception hooks** —
  `PublishHook`, `DiscoveryHook`, `DistributionHook` — as plugin
  points where external PDPs / PEPs / IAM systems can intervene.
  DDSR itself ships NO concrete policy logic and NO XACML modeling
  inside the core; production deployments plug in an OPA / XACML /
  custom adapter that implements these interfaces.

### Provider API
- **FR-Provider-1** A provider MUST be able to register a service via
  `LocalServiceRegistry.registerService(provider, implementation,
  props)`, where `implementation` is a `ServiceImplementation` that
  references the relevant `ServiceInterface`(s) and advertises zero or
  more transport flavors.
- **FR-Provider-2** A `ServiceRegistration` MUST allow property
  updates and unregistration.
- **FR-Provider-3** Registration MUST be valid only against catalog
  entries the Remote Registry knows: implementations referencing a
  `ServiceInterface` not present in the catalog MUST be rejected with
  a `Diagnostic` (subject to network-partition behavior — see §8).

### Consumer API
- **FR-Consumer-1** A consumer MUST be able to look up service
  references by interface name, optional LDAP filter, and a
  `ConsumerCapability` describing the flavors it can speak.
- **FR-Consumer-2** A consumer MUST be able to register and unregister
  service listeners with an optional filter.
- **FR-Consumer-3** Lookup results MUST contain only references whose
  underlying `ServiceImplementation` advertises at least one flavor
  in the consumer's `ConsumerCapability.supportedFlavors`.

### Events
- **FR-Events-1** Registration, modification, and unregistration MUST
  generate `ServiceEvent`s delivered to matching listeners.

### Lifecycle & semantics  **[STUB — to be filled out so Java and TS implementations stay in lockstep]**

These items will be spelled out in detail before/while the Java and
TypeScript implementations are built. The exact behavior of each must
be the same in both languages (see NFR-Behavioral-Parity).

- **FR-Lifecycle-Register** Precise step order when `registerService`
  is called: when is `service.id` assigned, when are default properties
  injected, when is `REGISTERED` delivered, are listeners invoked
  synchronously or asynchronously, and in which order relative to the
  return of `registerService`?
- **FR-Lifecycle-Modify** Precise step order for `setProperties`:
  property visibility change, `MODIFIED` event delivery, and
  `MODIFIED_ENDMATCH` semantics for filter-mismatched listeners.
- **FR-Lifecycle-Unregister** Precise step order for `unregister`:
  `UNREGISTERING` is delivered **before** the service is removed from
  the registry (OSGi semantics); consumers MUST have a chance to release
  their use before the service vanishes.
- **FR-Lookup-Ranking** Result ordering of `getServiceReferences`:
  ranking by `service.ranking` property (descending), tie-break by
  `service.id` (ascending), as OSGi does.
- **FR-Lookup-Filter** Filter evaluation semantics: LDAP filter syntax,
  property type coercion rules, behavior on missing properties.
- **FR-Lookup-Cardinality** Cardinality enforcement: how `ZERO_OR_ONE`,
  `ONE`, `ZERO_OR_MANY`, `ONE_OR_MANY` interact with lookup results and
  component satisfaction.
- **FR-Config-States** Allowed state transitions for
  `ComponentConfiguration` (e.g. `UNSATISFIED_REFERENCE` → `SATISFIED`
  → `ACTIVE`; transitions into `FAILED_ACTIVATION`; recovery paths).

### Distribution & Remote Registry

- **FR-Dist-Broker** A **Remote Registry** MUST exist as a separately
  deployable, hosted component. It is owned and operated by the DDSR
  project (not by users of DDSR).
- **FR-Dist-Transparent-Lookup** Every language framework MUST present
  a single lookup API to consumers. Whether the matched service is
  in-process (local registry) or remote (Remote Registry) MUST be
  transparent to the consumer.
- **FR-Dist-Framework-Owns-Comms** The Remote Registry communication
  is the **framework's responsibility**, not the provider's or
  consumer's. Providers call `registerService` / `unregister` on the
  local framework; the framework synchronously updates the local
  registry, fires local events, and asynchronously propagates the
  change to the Remote Registry (with retry on failure).
- **FR-Dist-Flavor-Advertisement** When a provider registers a service
  implementation, it MUST advertise one or more **transport flavors**
  (e.g. REST, MQTT) with the flavor-specific details required for
  invocation (broker + topics for MQTT; base path + method bindings
  for REST).
- **FR-Dist-Flavor-Matching** When a consumer looks up a service, the
  framework MUST attach the set of **flavors the consumer can speak**
  (based on the flavor client plugins installed) to the request. The
  Remote Registry MUST filter results so that only implementations with
  at least one consumer-supported flavor are returned.
- **FR-Dist-Eventing** Consumers MUST be able to register
  `ServiceListener`s that receive events for matching services
  *whether the underlying implementation is local or remote*.

### Partition behavior: Local ↔ Remote Registry

- **FR-Partition-ReadOnly** When the link to the Remote Registry is
  lost, the local registry MUST remain available in **read-only mode**:
  lookups, ServiceListener notifications, and local-only operations
  continue to work against the last known snapshot + replayed events.
- **FR-Partition-WritesRejected** Provider → broker writes
  (`publishImplementation`, `withdrawImplementation`, catalog
  mutations) MUST be rejected with a `Diagnostic(severity=ERROR)`
  while the connection is DEGRADED or OFFLINE. Write retry is the
  provider's responsibility, not the framework's — no write queue is
  maintained, to avoid replay-time conflicts.
- **FR-Partition-LocalUnaffected** In-process registration via
  `LocalServiceRegistry.registerService` continues to function while
  the link is down for components that do not require remote
  propagation (i.e. providers can still publish purely-local
  services); only the remote propagation step is suppressed and gets
  retried when the link returns.
- **FR-Partition-StateAttribute** `LocalServiceRegistry` MUST expose a
  `connectionState` attribute of enum type `ConnectionState`
  (`CONNECTED`, `DEGRADED`, `OFFLINE`). Consumers can read it to know
  whether their view is live or possibly stale.
- **FR-Partition-Reconnect** On reconnect, the local registry pulls a
  fresh snapshot (FR-Sync-Reconnect) and synthesises the appropriate
  `ServiceEvent`s (REGISTERED / UNREGISTERING / MODIFIED /
  MODIFIED_ENDMATCH) for the diff between the cached state and the
  fresh snapshot, so local listeners observe a consistent transition
  without a sudden state replacement.

### Sync mechanism: Local ↔ Remote Registry

- **FR-Sync-Hybrid** The sync model is **hybrid: initial snapshot + persistent
  event stream**. On (re)connect, the local registry pulls a
  *snapshot* of the broker state filtered by the consumer-side
  `ConsumerCapability` (so each local only receives what its flavors
  can consume). After the snapshot, the broker pushes incremental
  events on a persistent stream until disconnect.
- **FR-Sync-Write-Path** Provider → Broker writes (`publishImplementation`,
  `withdrawImplementation`, `addCatalogEntry`, `deprecateCatalogEntry`,
  `removeCatalogEntry`, `setProperties`) are **synchronous request /
  response operations**, NOT events. Each returns a `Diagnostic`. The
  broker emits the resulting state change on the event stream so that
  other locals see the change.
- **FR-Sync-Transport** The event stream uses **HTTP-SSE** (Server-Sent
  Events) as the standard transport — unidirectional broker → local,
  text/event-stream content type, native browser and Node support,
  no separate broker infrastructure required. WebSocket is a
  permissible alternative if bidirectional traffic over the same
  connection becomes necessary; the provider → broker write path stays
  on plain HTTP request / response either way.
- **FR-Sync-EventTypes** Two distinct event kinds flow on the stream:
  `ServiceEvent` (REGISTERED / MODIFIED / UNREGISTERING /
  MODIFIED_ENDMATCH — see model) and `CatalogEvent` (added /
  deprecated / removed — to be added to the model).
- **FR-Sync-Reconnect** On reconnect after a transient drop, the
  local registry MUST request a fresh snapshot rather than try to
  replay missed events. This avoids per-client sequence-number tracking
  on the broker.
- **FR-Sync-Filtering** The snapshot AND the event stream MUST honor
  the local's `ConsumerCapability` filter, so a local that only speaks
  REST does not receive snapshots / events for MQTT-only
  implementations.

### API catalog & governance

- **FR-Catalog-Authority** The Remote Registry MUST hold a persistent
  **API catalog**: the set of `ServiceInterface`s (each with its
  `ServiceOperation`s, `Parameter`s + `ParameterConstraint`s, and
  `ServiceException`s) that are allowed to exist in the system. The
  catalog is present even when no implementation is registered.
- **FR-Catalog-Curation** A **Governance officer** role MUST be able
  to add, modify, deprecate, and remove entries in the API catalog.
  Catalog entries become active for providers/consumers only after the
  Governance officer releases them.
- **FR-Catalog-Versioning** Service interfaces MUST be versioned in the
  catalog (already supported by `ServiceInterface extends
  VersionedElement` in the model).
- **FR-Catalog-Bootstrapping** The catalog SHOULD be bootstrappable
  from existing code via the Reverse-Engineering FRs below.
- **FR-Catalog-Immutable** A `ServiceInterface` is conceptually
  **immutable** once `addCatalogEntry` has accepted it. Changes to
  operations, parameters, constraints, or exceptions MUST be expressed
  as a **new `ServiceInterface` entry** (typically a new version), not
  as in-place mutation. The old entry MAY be deprecated. This matches
  semver and the artifact-publishing model (FR-CodeDist-Versioning):
  every catalog mutation that would break wire compatibility surfaces
  as a distinct artifact.
- **FR-Catalog-Deprecation-Soft** `deprecateCatalogEntry` performs a
  **soft** deprecation: existing `ServiceImplementation`s keep
  running, lookups continue to return references, and existing
  `ServiceListener`s keep receiving events. The interface's `status`
  attribute transitions to `DEPRECATED`. Subsequent
  `publishImplementation` calls against the deprecated interface
  succeed but the returned `Diagnostic` carries `severity = WARNING`
  with the recorded `deprecationReason`. Deprecation is one-way; a
  revival is a new catalog entry.
- **FR-Catalog-Removal-StrictReject** `removeCatalogEntry` MUST refuse
  with `Diagnostic(severity=ERROR, code=CATALOG_HAS_LIVE_IMPLS)` while
  any `ServiceImplementation` in the registry still references the
  target `ServiceInterface`. The governance officer's workflow is:
  deprecate → wait for providers to withdraw → remove. No automatic
  cascade-withdraw at the framework level.
- **FR-Catalog-MigrationHint** `ServiceInterface.replacedBy` MAY point
  to a successor interface that supersedes a deprecated one. Catalog
  browsers and IDE tooling SHOULD surface this to consumers as a
  migration target.

### Reverse engineering: code → service description *(deferred — post-prototype)*

Reverse engineering is a **nice-to-have feature for a later iteration**,
not for the first prototype. The catalog is seeded by hand-authored
XMI in the prototype. The requirements below are recorded as guidance
for when the feature is picked up; concrete source-surface choices are
captured in §8 Deferred (Q1).

- **FR-Rev-Java** *(future)* The Java tooling SHOULD be able to extract
  a `ServiceInterface` (with `ServiceOperation`s, parameters,
  constraints if expressible, and exception types) from a Java
  interface — likely driven by annotations or marker types.
- **FR-Rev-TypeScript** *(future)* Equivalent extraction SHOULD exist
  for TypeScript, likely driven by decorators, JSDoc tags, or typed
  protocol declarations.
- **FR-Rev-Python** *(future)* Equivalent extraction SHOULD exist for
  Python, likely driven by `typing.Protocol`, dataclasses, or
  decorator-based metadata.
- **FR-Rev-Lossless-or-Diagnostic** *(future)* Where source code
  expresses something that does not map cleanly to the DDSR model
  (e.g. inheritance of Java exception types), the extractor MUST
  either capture it as a structured annotation or reject it with a
  `Diagnostic` — never silently coerce.

The same "extract a `ServiceInterface` from an existing description"
mechanism applies to **contract description formats**, not only source
code. This lets brownfield services that already ship an OpenAPI /
AsyncAPI contract be onboarded into the catalog without hand-authoring
XMI. OpenAPI and AsyncAPI are *import formats*, **not** new transport
flavors (see §8 Deferred): the derived binding is a standard
`RestFlavor` / `MqttFlavor`.

- **FR-Rev-OpenAPI** *(future)* The tooling SHOULD be able to import an
  **OpenAPI** document and derive one or more `ServiceInterface`s
  (`ServiceOperation`s from path + method, `Parameter`s from request
  params / body schema, return type from the response schema,
  `ServiceException`s from documented error responses) together with a
  **`RestFlavor`** binding (base path; per-operation HTTP method + path
  taken from the OpenAPI operation). OpenAPI describes a REST contract,
  so the resulting transport is REST.
- **FR-Rev-AsyncAPI** *(future)* Equivalent import SHOULD exist for
  **AsyncAPI**: channels / messages map to `ServiceOperation`s and an
  **`MqttFlavor`** (or other async flavor) binding (broker +
  request/response topics, QoS). AsyncAPI is a description format for
  message-driven transports, mapped onto the existing async flavor(s).
- **FR-Rev-Contract-Governance** *(future)* A contract import MUST flow
  through the same catalog path as a hand-authored entry: it produces
  reviewable XMI that is released via `addCatalogEntry` → `PublishHook`
  (FR-Catalog-Curation). An imported spec MUST NOT self-activate.
- **FR-Rev-Contract-SafeFetch** *(future)* Fetching and parsing an
  externally supplied OpenAPI / AsyncAPI document is **untrusted input**
  and MUST obey NFR-Security-WireParsing (external entities / DTDs
  disabled, size / depth limits) and MUST NOT open an SSRF vector —
  remote `$ref` / spec-URL resolution restricted to a broker-owned
  allowlist (see the `S*` SSRF findings). The lossy-safe contract of
  FR-Rev-Lossless-or-Diagnostic applies: anything that does not map
  cleanly is captured as annotation or rejected with a `Diagnostic`.

### Dynamic (stub-free) consumption *(future)*

The catalog's `ServiceInterface` is already a complete, language-neutral,
runtime-available description (operations, `Parameter`s + constraints,
return type, `ServiceException`s). That makes a **stub-free consumer**
possible: one that invokes a service purely from the `ServiceReference` +
`ServiceInterface` it receives at lookup time, without any
Code-Publisher-generated artifact. This is an **additional** consumption
mode alongside the typed-stub path (UC-6 / FR-Codegen-POJO-Only), not a
replacement — it trades compile-time type safety for zero build-time
coupling. Use cases: gateways, low-code / scripting consumers, test
harnesses, browser consoles, admin tooling.

- **FR-Dynamic-Invoke** *(future)* Each language framework SHOULD offer
  a **late-bound invocation API** — e.g. `invoke(reference,
  operationName, args)` — that resolves the operation against the
  `ServiceInterface`, marshals via the matched flavor's client plugin,
  and returns a generic (untyped / map-shaped) result. No generated
  proxy is required.
- **FR-Dynamic-Validation** *(future)* A dynamic invocation MUST enforce
  the same `ParameterConstraint`s as the typed path before dispatch and
  MUST surface violations as a `Diagnostic` — the safety guarantees do
  not depend on generated code.
- **FR-Dynamic-Transport-Unchanged** *(future)* The dynamic path changes
  only how a call is *expressed* (no generated types), not the
  transport: it uses the same REST / MQTT flavor client plugins
  (FR-Plugin-API) and the same wire format as a stub-based call, so a
  dynamic consumer and a stubbed consumer are indistinguishable to the
  provider.
- **FR-Dynamic-Parity** *(future)* Dynamic-invocation semantics
  (operation resolution, validation, error shape) MUST be identical
  across Java and TypeScript (NFR-Behavioral-Parity).
- **FR-Dynamic-FromImport** *(future)* When the catalog entry originated
  from an imported OpenAPI / AsyncAPI contract (FR-Rev-OpenAPI /
  FR-Rev-AsyncAPI), the dynamic consumer needs nothing further: the
  imported `ServiceInterface` + flavor binding is exactly what
  `invoke(...)` drives. The raw spec MAY optionally be retained as an
  annotation for richer tooling (e.g. rendering a request UI).

### Flavor-plugin API (provider and consumer side)

- **FR-Plugin-API** Each language framework MUST expose a
  **flavor-plugin API** with which third parties can register new
  transport flavors. A flavor plugin contributes:
  - on the provider side, a "publish" implementation that listens for
    incoming requests over the transport and dispatches to the local
    service object;
  - on the consumer side, a "client" implementation that takes a
    flavor-specific service reference and produces an invocable proxy.
- **FR-Plugin-Parity** The plugin API surface MUST be conceptually
  identical across Java, TS, and (later) Python — only the syntactic
  shape differs. A `RestFlavor` plugin written for Java MUST be
  describable by a plugin written for TS with the same configuration
  schema.
- **FR-Plugin-Bundled-Flavors** REST and MQTT MUST ship as reference
  flavor plugins in all primary-target languages.

### Code generation

- **FR-Codegen-POJO-Only** The code generator MUST emit only **types
  and POJOs** (EMF model classes, service-interface stub types) — no
  behavior, no transport code, no lifecycle code. Behavior lives once
  per language, hand-written, in the framework runtime.
- **FR-Codegen-Stacks** The EMF stack per language is fixed:
  - **Java**: standard EMF (Eclipse Modeling Framework).
  - **TypeScript**: `ecore.ts` (developed by the DDSR project itself).
  - **Python**: `PyEcore` (upstream community project — no direct
    maintainer contact, so edge cases are handled via GitHub issues or
    our own contributions).
- **FR-Codegen-Inputs** The generator's input is the API catalog held
  by the Remote Registry: a set of `ServiceInterface`s with their
  `ServiceOperation`s, `Parameter`s + `ParameterConstraint`s, and
  `ServiceException`s. Output is one artifact per target language
  (see Code distribution below).
- **FR-Codegen-Symmetry** The shape of the generated provider stub and
  the generated consumer interface MUST be conceptually identical
  across the three languages — only the syntactic surface differs.

### Code distribution

- **FR-CodeDist-Publisher** A **Code Publisher** service runs adjacent
  to the Remote Registry. When the Governance officer releases a new
  or modified catalog entry, the Publisher generates code for each
  target language and uploads the resulting artifacts to the
  configured artifact repositories.
- **FR-CodeDist-Artifacts** One artifact per language is produced and
  published:
  - **Java** → JAR uploaded to a Maven repository (Sonatype Nexus by
    default).
  - **TypeScript** → npm package uploaded to an npm registry.
  - **Python** → wheel uploaded to a PyPI-compatible index.
- **FR-CodeDist-Versioning** Artifact semver MUST be derived from
  `ServiceInterface.version`. The classification per change kind
  is fixed below (FR-CodeDist-SemverRules-*). The broker validates
  the bump at `addCatalogEntry` time (FR-CodeDist-SemverEnforce).

- **FR-CodeDist-SemverRules-Major** The following changes MUST yield
  a **MAJOR** version bump (X.Y.Z → X+1.0.0):
  - Operation removed or renamed
  - Parameter removed or renamed
  - `Parameter.type` or `ServiceOperation.returnType` changed
  - Optional parameter becomes required (`optional: true → false`)
  - Exception **added or removed** on an Operation
  - Invariant **added or removed** on an Interface or Operation
  - Constraint added or **tightened** on a Parameter
  - Constraint **loosened or removed** on a return value (Liskov:
    consumers had narrower guarantees previously)
  - `ServiceException.properties` shape changed

- **FR-CodeDist-SemverRules-Minor** The following changes MUST yield
  a **MINOR** version bump (X.Y.Z → X.Y+1.0):
  - New Operation added
  - New **optional** Parameter (with `defaultValue`) added to an
    existing Operation
  - Constraint **loosened** on a Parameter (provider becomes more
    tolerant; old consumers still satisfy)
  - Constraint **tightened** on a return value (provider promises a
    narrower output; old consumers' expectations still met)

- **FR-CodeDist-SemverRules-Patch** The following changes MUST yield
  a **PATCH** version bump (X.Y.Z → X.Y.Z+1):
  - `description` text changes anywhere in the ServiceInterface tree
  - Non-semantic metadata changes (catalog browser hints, etc.)

- **FR-CodeDist-SemverRules-NoBump** `status: ACTIVE → DEPRECATED`
  along with `deprecationReason` / `replacedBy` updates is an
  in-place mutation on the existing entry (see
  FR-Catalog-Deprecation-Soft) and MUST NOT trigger a version bump.

- **FR-CodeDist-SemverEnforce** When a new `ServiceInterface` is
  submitted via `addCatalogEntry` with the same `name` as a previous
  catalog entry, the broker MUST:
  1. compute the minimum required version bump from the diff between
     the old and new ServiceInterface using the rules above,
  2. compare it with the version the author specified,
  3. reject with `Diagnostic(severity=ERROR,
     code=SEMVER_BUMP_INSUFFICIENT)` if the author's bump is smaller
     than the required minimum.
  An optional `force` parameter on `addCatalogEntry` MAY bypass this
  check for edge cases (e.g. doc-only patches the validator
  classifies as no-bump); use of the override MUST flow through
  `PublishHook` like the rest of the catalog mutation API.
- **FR-CodeDist-Authority** Publishing MUST flow through the
  `PublishHook` interception chain on the broker (see
  FR-Hook-CodeDist-Authority). Only an authorized
  Governance-officer release triggers publication.
- **FR-CodeDist-Reproducible** Given a fixed catalog snapshot, the
  Publisher MUST produce byte-identical artifacts (or, where
  packaging-format-imposed timestamps are unavoidable, semantically
  identical artifacts) so that consumers can audit what they receive.
- **FR-CodeDist-BuildTimeOnly** DDSR-published artifacts (JAR / npm
  package / wheel) are consumed by **build-time dependency resolution
  only** — Maven for Java, `npm install` for TypeScript, `pip install`
  for Python. Runtime / hot-loading mechanisms (e.g. Daanse TSM's
  `PluginRegistry`, OSGi dynamic bundle install) are not part of the
  artifact-consumption path. This is a deliberate symmetry across
  all three target languages and a deliberate exclusion of runtime
  artifact-fetching from DDSR's scope.

### Interception hooks (Publish / Discovery / Distribution)

DDSR does NOT model authorization, policy decision, or enforcement
semantics inside its core. Instead it exposes three strategic
**hook interfaces** at which integrators plug in their own PDP / PEP /
IAM / audit / mutation logic. The prototype ships with no hook
implementations, so it runs hook-free (every action permitted).

Hook contract: each hook method returns a `Diagnostic`. Severity
`OK` / `INFO` / `WARNING` means *proceed*; `ERROR` or `CANCEL` means
*abort, return this diagnostic to the caller*. Multiple hooks of the
same kind form a pipeline — every hook must permit the action.

- **FR-Hook-Publish** Before `publishImplementation` or
  `withdrawImplementation` succeeds, every `PublishHook` registered
  on the receiving `ServiceRegistry` MUST be consulted. Any hook
  returning ERROR/CANCEL aborts the action. Use cases: governance
  enforcement on the broker side, audit logging, multi-tenancy
  filtering of allowed catalog entries.
- **FR-Hook-Discovery** Before a lookup or subscription runs, every
  `DiscoveryHook` MUST be consulted via `onLookup` / `onSubscribe`.
  After the registry has assembled a candidate result list,
  `filterReferences` MUST be invoked so the hook can prune the list
  per consumer. Use cases: per-tenant visibility, ABAC on consumer
  identity, masking of references the consumer is not entitled to see.
- **FR-Hook-Distribution** Whenever an event crosses the local↔remote
  boundary, every `DistributionHook` MUST be consulted (`onOutbound`
  before local → broker propagation, `onInbound` before delivering an
  event from the broker to local listeners). Use cases: scrubbing
  sensitive properties at the federation boundary, dropping events
  destined for unauthorized tenants, redaction.
- **FR-Hook-CodeDist-Authority** Code Publisher invocation (triggered
  by a catalog mutation) flows through a `PublishHook` whose action
  context identifies it as a publishing/governance action. Authority
  of catalog releases is enforced via this hook, not via a separate
  PDP class.
- **FR-Hook-PrototypeNoOp** The prototype ships with **no** hook
  implementations, so every action proceeds. Production deployments
  attach hook implementations as part of their integration with their
  chosen PDP / IAM platform.

---

## 6. Non-functional requirements

- **NFR-LangNeutral** No type/feature in the core model may require a
  Java-specific runtime concept (e.g. `Class<?>`, JVM bundle id as
  `long`, `Throwable`). Identifiers MUST be string/UUID; bundle-like
  concepts MUST be modeled as `ServiceProvider`.
- **NFR-Portable-Code-Gen** It MUST be feasible to generate idiomatic
  code from `ddsr.ecore` for at least Java (via EMF genmodel),
  TypeScript, and Python. The Ecore model SHOULD avoid constructs that
  are awkward to translate (e.g. heavy use of `EMap` or Java-only data
  types).
- **NFR-OSGi-Interop** The Java implementation MUST interoperate with a
  real OSGi runtime, i.e. it MUST be possible to bridge between a
  DDSR `ServiceRegistry` and `org.osgi.framework.BundleContext` in both
  directions (DDSR service ↔ OSGi service).
- **NFR-PlainJava-Interop** The Java implementation MUST also be usable
  in a **plain Java application without any OSGi container**. The core
  Java runtime of DDSR MUST NOT have a hard dependency on
  `org.osgi.framework`. OSGi integration MUST live in a separate,
  optional module so that the core can run in standalone JVMs,
  Spring/Quarkus apps, embedded contexts, and tests.
- **NFR-Persistability** For the prototype, registry state and
  component descriptions MUST be (de)serializable as **XMI** via EMF
  resources, so that descriptions can be authored offline and exchanged
  between runtimes. **JSON** (via Gecko EMF JSON or a TypeScript-side
  equivalent) is a planned follow-up but explicitly out of scope for the
  prototype.
- **NFR-RemoteRegistry-Storage** The Remote Registry MUST keep its
  state (API catalog + ServiceImplementation index + ServiceProvider
  list) **in memory** as an EMF `ResourceSet`. After every mutation
  (publish / withdraw / addCatalogEntry / deprecateCatalogEntry /
  removeCatalogEntry / setProperties) the registry MUST persist the
  full state to disk as an **XMI snapshot** *before* returning OK to
  the caller; that guarantees a mutation acknowledged to the provider
  survives a crash. On startup, the snapshot is loaded into a fresh
  ResourceSet and the SSE event stream is brought up. Recovery
  granularity = last acknowledged mutation.
- **NFR-RemoteRegistry-Concurrency** The Remote Registry MUST guard
  its `ResourceSet` with a **read-write lock**: lookups acquire the
  read lock and may run in parallel; mutations acquire the write lock
  exclusively. EMF resources are not thread-safe by default; this
  lock is the bridge.
- **NFR-RemoteRegistry-Singleton** The prototype is **single-node**.
  No HA, no clustering, no failover. Hot-standby or DB-backed
  scale-out are explicit later-iteration concerns (see §7 Out of
  scope).
- **NFR-RemoteRegistry-Language** The Remote Registry is implemented
  in **Java**. The reason is technical, not preferential: EMF, M2X
  OCL, Fennec-Persistence (if/when used), and the BND/Gradle build
  toolchain are all Java-native and operate without friction. The
  choice does NOT affect client framework portability — clients in
  Java, TypeScript, or Python talk to the broker over HTTP + SSE
  regardless of the broker's implementation language.
- **NFR-RemoteRegistry-ModuleLayout** Broker code and client-framework
  code live in the **same workspace** under separate module prefixes:
  `org.eclipse.fennec.services.broker.*` for broker modules and
  `org.eclipse.fennec.services.client.*` for per-language client frameworks.
  The model bundle (`org.eclipse.fennec.services.model`) is shared. Single repo,
  single BND workspace, single Gradle build — so a model change can
  be propagated to broker and client(s) in one commit.
- **NFR-Behavioral-Parity** The Java and TypeScript implementations
  MUST exhibit **identical observable behavior** for the operations
  defined in this document — in particular, service registration
  lifecycle (incl. event ordering), service modification, service
  unregistration, and service lookup (ranking, filter evaluation,
  cardinality enforcement). This is the central reason §5 contains a
  "Lifecycle & semantics" subsection: parity requires that semantics be
  written down, not left to each implementation's intuition.
- **NFR-Three-Layer-Architecture** Each language implementation
  follows the same three-layer stack. The EMF layer and the native
  service framework vary per language; the **top layer** (DDSR's own
  registry / lookup / events / remote bridge) is hand-written against
  the spec in every language and is the load-bearing piece for
  Behavioral Parity.

  | Layer | Java | TypeScript | Python |
  |---|---|---|---|
  | EMF / POJOs | EMF | `ecore.ts` | PyEcore |
  | Component lifecycle / module loading / DI | OSGi DS *(in the OSGi flavor)* or a hand-written plain-Java lifecycle core | **Daanse TSM** (Eclipse) | **iPOPO** (+ Pelix) |
  | **DDSR-own**: registry, lookup, events, listeners, remote bridge | hand-written (spec-implementing) | hand-written | hand-written |

  Each native middle-layer framework has its own lifecycle state
  machine; the DDSR top layer MUST provide an explicit mapping between
  its `ComponentState` enum and the native states, so transitions are
  traceable cross-language.
- **NFR-License** The model and reference implementations are licensed
  under **EPL-2.0**.
- **NFR-Java-Version** Java reference implementation targets **Java 21**
  (per `cnf/build.bnd`). TypeScript target version **[OPEN]**.

### Security (Security-by-Design)

Security is a first-class, cross-cutting concern, **not** something
fully delegated to the interception hooks. The hooks cover external
authorization/policy (PDP/PEP/IAM); transport hardening, broker
authentication, safe wire-parsing, input validation, resource limits,
and secure defaults are DDSR's own responsibility. Full methodology,
STRIDE threat tables per functionality, OWASP-ASVS-5.0 and BSI-
Grundschutz mapping, and the secure-defaults catalog live in
[SECURITY.md](SECURITY.md); concrete findings are tracked as the `S*`
series in [OPEN_ISSUES.md](OPEN_ISSUES.md).

- **NFR-Security-ThreatModel** Every functionality (existing and new)
  MUST carry a STRIDE-based threat analysis with a documented risk
  rating and a chosen handling option (Mitigate / Accept / Transfer /
  Avoid) before it counts as done. Prototype-scope acceptances are
  allowed but MUST be marked `Accept` with a rationale — never left
  silently open.
- **NFR-Security-Standards** DDSR components MUST be assessable against
  **OWASP ASVS 5.0** (applicable chapters: V1, V2, V4, V5, V6, V7, V8,
  V9, V11, V12, V13, V14, V15, V16) and the **BSI Grundschutz++**
  control families **DEV** (secure development / security-by-default),
  **KONF** (secure configuration of distributed apps — esp. KONF.14
  "Verteilte Anwendungen"), **BER** (authentication / key management),
  **ARCH**, **DET** (logging), and **RISK** (risk management). The
  **Mindeststandard-TLS** catalog governs TLS parameters. The
  self-operated broker targets ASVS **L2**. The canonical standard
  sources live locally at `/opt/git/OWASP-ASVS/` and
  `/opt/git/BSI-Stand-der-Technik-Bibliothek/` (OSCAL) and are
  consulted directly — see [SECURITY.md](SECURITY.md) §2.
- **NFR-Security-Defaults** Defaults MUST be **secure (fail-closed),
  opt-out not opt-in**: TLS on by default, broker mutations require
  authentication, XMI parsing forbids external entities and limits
  expansion, an empty hook pipeline is permitted only in a dev profile.
  An insecure default is a vulnerability even when a secure mode exists.
- **NFR-Security-WireParsing** The XMI wire codec MUST parse untrusted
  input safely: external entities / DTDs disabled, entity-expansion and
  size/depth limits enforced, and cross-document `href` resolution
  restricted to a broker-owned allowlist (no SSRF via client-supplied
  URIs).
- **NFR-Security-Transport** Communication on every trust boundary
  (client↔broker, provider↔consumer, broker→local event stream) MUST
  support TLS, with mTLS negotiable via Capability. Plaintext is a
  dev-only opt-out.

---

## 7. Out of scope (for now)

- **Bundle lifecycle** beyond what `ServiceProvider` already captures.
  DDSR models services, not deployment units.
- **External authorization / policy engines** (PDP/PEP, identity
  providers, XACML / OPA semantics). These plug in via the interception
  hooks (`PublishHook`, `DiscoveryHook`, `DistributionHook` — see
  FR-Hook-*); DDSR itself ships no hook implementations.
  **Note:** this exclusion covers *policy decision/enforcement only*.
  Transport hardening, broker authentication, safe wire-parsing, input
  validation, resource limits, and secure defaults are explicitly
  **in scope** as DDSR's own responsibility — see the Security NFRs in
  §6 and [SECURITY.md](SECURITY.md). They cannot be delegated to the
  hooks.
- **Prototype-scope factory API** (`PrototypeServiceFactory`,
  `ServiceObjects`). Scope is captured as an enum value; no factory
  machinery in the first iteration.
- **`ServiceComponentRuntime`** as a separate EClass — postponed until
  DS lifecycle operations (enable/disable component, runtime
  introspection) become a concrete need.
- **Filter parsing/evaluation** — `target` and listener filters are
  modeled as plain LDAP-syntax strings; the evaluator is an
  implementation concern.
- **Remote Registry HA / clustering** — the prototype runs as a
  single-node broker (see NFR-RemoteRegistry-Singleton). Failover,
  hot-standby, active-active clusters, and migration to a real DB are
  deliberately later-iteration concerns. Storage stays in-memory with
  XMI snapshots until that point.
- **Runtime artifact loading / hot-update of DDSR artifacts** — DDSR
  artifacts are consumed via build-time dependency resolution only
  (see FR-CodeDist-BuildTimeOnly). Specifically: Daanse TSM's
  `PluginRegistry` is NOT used to load DDSR-published npm packages
  at runtime. TSM remains the TypeScript framework's
  module-loading / DI / lifecycle layer per
  NFR-Three-Layer-Architecture, but the DDSR artifact-consumption
  path stays build-time.

---

## 8. Open questions

*(none currently open — see §9 for the concrete prototype Definition of
Done; deferred topics below)*

### Deferred

- **Reverse-engineering source surface** *(formerly Q1)*: deferred.
  Reverse engineering is a **nice-to-have for a later iteration**;
  the prototype uses hand-authored XMI for catalog seeding (see §5
  Reverse engineering, marked *future*). When the feature is picked
  up, the source-surface analysis from the deferral discussion is the
  starting point:

  | Language | Likely source surface | Tooling sketch |
  |---|---|---|
  | **Java** | Annotations (`@DdsrService`, `@DdsrOperation`, `@DdsrParam(constraints=…)`, `@DdsrThrows`) + `javax.lang.model` | Annotation Processor at compile time |
  | **TypeScript** | Typed `interface` + minimal marker decorator (`@DdsrService()`) | TS Compiler API (e.g. `ts-morph`) reads the interface; the decorator only signals "extract this" |
  | **Python** | `typing.Protocol` + decorator (`@ddsr_service`) + pydantic-style `Field(...)` for constraints | `inspect` / `typing.get_type_hints` driven extractor |

  Recommended pattern: **marker + structural** (the marker signals
  "extract me", the type system carries the content). Avoids
  unintended catalog entries (pure-structural) and avoids
  doubly-typed boilerplate (annotation-only).

  Tooling flow: build-time extraction → XMI file lives in the
  repo and is reviewable → separate `ddsr publish-catalog` tool
  calls `RemoteServiceRegistry.addCatalogEntry(...)`, which goes
  through the `PublishHook` chain (Governance enforcement).

  Open detail decisions when picked up: exact decorator/annotation
  vocabulary, how lossy-safe edge cases (Java exception inheritance,
  generic types, Python `TypedDict`) are surfaced (FR-Rev-Lossless-or-Diagnostic
  gives the contract: capture as annotation OR reject with
  Diagnostic).

- **OpenAPI / AsyncAPI ingestion** *(new — deferred)*: import an
  OpenAPI (→ REST) or AsyncAPI (→ MQTT / async) contract into the
  catalog, so brownfield services with an existing spec can be onboarded
  without hand-authoring XMI. Recorded as FR-Rev-OpenAPI /
  FR-Rev-AsyncAPI / FR-Rev-Contract-* (all *future*). Two design
  questions to settle when picked up:

  1. **Importer vs. runtime flavor.** Recommended framing: OpenAPI and
     AsyncAPI are *description-format importers* that produce a
     `ServiceInterface` plus a standard `RestFlavor` / `MqttFlavor` —
     NOT a new `FlavorKind`. A consumer "speaking OpenAPI" is just a
     REST consumer whose bindings were derived from the spec. An
     `OPENAPI` FlavorKind would collide with FR-Model-7 (flavors are
     transports) and with the typed-stub model.
  2. **Dynamic / generic (stub-free) consumer** — *decision: pursue.*
     A generic consumer that drives a service purely from its runtime
     description, needing no generated stub. Recorded as the
     FR-Dynamic-* set (§5, *future*). Resolved that it is an
     **additional** consumption mode alongside typed stubs, not a
     replacement — so FR-Codegen-POJO-Only and the UC-6 typed DX stay
     intact. The insight that removed the earlier tension: the DDSR
     catalog `ServiceInterface` is *already* a complete language-neutral
     runtime description, so dynamic invocation is a general framework
     capability; OpenAPI / AsyncAPI import is merely one way to populate
     the catalog that feeds it. Open details when picked up: exact
     late-bound API shape, generic result representation, and how much
     of the raw spec (if any) to retain for tooling.

- **Concrete PDP / PEP contract** *(formerly Q2)*: deliberately
  deferred. DDSR will not model XACML semantics inside its core.
  Production deployments integrate with their existing PDP/PEP
  (Open Policy Agent, XACML server, IAM platform, custom) via the
  three interception hooks added in §5: `PublishHook` for
  provider-side actions, `DiscoveryHook` for consumer-side lookup
  and subscription, `DistributionHook` for federation traffic.
  The hook contract uses `Diagnostic` as the decision return type
  (severity OK ⇒ proceed, ERROR/CANCEL ⇒ abort). The exact
  taxonomy of policy actions, contextual attributes, and decision
  metadata stays out of the DDSR model.

### Resolved

- **OSGi-Java interop**: hard requirement (see NFR-OSGi-Interop).
  Additionally, plain-Java interop is also a hard requirement
  (see NFR-PlainJava-Interop) — the OSGi parts live in a separate,
  optional module.
- **Language priority**: Java and TypeScript are co-developed primary
  targets. Python is deferred to a later iteration.
- **TypeScript repo layout**: handled by a colleague in parallel.
  Out of this document's scope, but with one binding constraint that
  flows back into the requirements: the resulting TS implementation
  MUST exhibit **identical observable behavior** to the Java
  implementation for the operations defined here — see
  `NFR-Behavioral-Parity` and the lifecycle/semantics requirements
  in §5.
- **Persistence format for the prototype**: **XMI only**. JSON is a
  planned follow-up but not required for the prototype (see
  NFR-Persistability).
- **End-user product**: DDSR is a **library / framework** for
  developers, in Java and TypeScript. It is not a standalone shipped
  product; it is embedded by other software.
- **Distribution semantics**: UDDI-style central Broker (Remote
  Registry), provider-advertised transport flavors, framework owns
  Remote-Registry communication, consumer-side flavor matching at
  lookup. See §1 Vision and §5 Distribution.
- **Hosting of the Remote Registry**: built and operated by the DDSR
  project itself, not by users.
- **Catalog authority**: a Governance officer role curates the
  catalog; a no-op default policy ships with the prototype but a PDP
  integration point exists.
- **Stub generation vs. reflective proxies**: codegen emits **only
  POJOs / typed interfaces, no behavior** (see FR-Codegen-POJO-Only).
  Artifacts are published per language to Nexus / npm / PyPI (see §5
  Code distribution). Behavior lives once per language in the
  framework runtime. This resolves the symmetric reflection-only
  approach (would have killed TS DX) as well as the full-codegen
  approach (would have inflated the generator surface).
- **EMF stack per language**: EMF (Java) / `ecore.ts` (TypeScript,
  developed by the DDSR project) / PyEcore (Python, upstream community
  project). Locked in NFR-Three-Layer-Architecture and
  FR-Codegen-Stacks.
- **Native service / lifecycle framework per language**: OSGi DS for
  the Java OSGi flavor (with a hand-written plain-Java lifecycle core
  for NFR-PlainJava-Interop), **Daanse TSM** for TypeScript, **iPOPO**
  for Python. Locked in NFR-Three-Layer-Architecture.
- **Definition of done for the prototype**: cross-language Stufe-2
  scenario — Java and TypeScript co-developed, two ServiceInterfaces
  in the catalog, both REST and MQTT flavors, Java+TS providers and
  consumers, ServiceListener-driven cross-language event delivery,
  hooks-free, catalog-mutation-free. Full details: §9 Prototype
  scope / Definition of Done. Success criterion: identical observable
  behavior with actors swapped between Java and TS, plus a green
  cross-language behavioral-parity test suite.
- **TSM PluginRegistry for DDSR artifact loading**: **endgültig
  out-of-scope**. DDSR-Artefakte werden per Build-Time-Dependency
  konsumiert (Maven, npm install, pip install). TSM bleibt als
  Framework-Schicht für die TS-Implementierung im Spiel (Modul-Loading,
  DI, Lifecycle), aber NICHT für die Distribution der vom Code
  Publisher erzeugten Pakete. Bewusste Symmetrie über alle drei
  Sprachen. Siehe FR-CodeDist-BuildTimeOnly und §7 Out of scope.
- **Code-artifact semver compatibility rules**: Liskov-aware
  classification (Parameter-Constraint gelockert = MINOR; verschärft
  = MAJOR; Return-Constraint umgekehrt). Exception- und
  Invariant-Änderungen beidseitig MAJOR. Deprecation = no-bump (in-place
  mutation). Broker validiert beim `addCatalogEntry` und lehnt
  unter-versionierte Submissions mit `Diagnostic(ERROR,
  code=SEMVER_BUMP_INSUFFICIENT)` ab; ein `force`-Override existiert
  für edge cases und geht durch den PublishHook. Siehe
  FR-CodeDist-SemverRules-* und FR-CodeDist-SemverEnforce.
- **Remote Registry implementation language**: **Java**. Driven by
  EMF / M2X-OCL / Fennec tooling all being Java-native, not by
  language preference. Clients in Java, TS, Python remain
  language-independent and talk to the broker over HTTP+SSE.
  Module layout: broker under `org.eclipse.fennec.services.broker.*`, clients
  under `org.eclipse.fennec.services.client.*`, shared model bundle, single
  workspace. See NFR-RemoteRegistry-Language and
  NFR-RemoteRegistry-ModuleLayout.
- **Remote Registry storage backend**: in-memory EMF `ResourceSet`
  with a synchronous XMI snapshot to disk after every acknowledged
  mutation; concurrency via read-write lock around the ResourceSet;
  single-node deployment. HA / clustering / DB-backed scale-out are
  explicit later-iteration concerns. See NFR-RemoteRegistry-Storage,
  NFR-RemoteRegistry-Concurrency, NFR-RemoteRegistry-Singleton.
- **Catalog mutation while implementations are live**: `ServiceInterface`s
  are **immutable** after `addCatalogEntry`; changes become new
  entries. `deprecateCatalogEntry` is a **soft** marker (existing
  implementations stay, lookups still resolve, new publishes WARN).
  `removeCatalogEntry` is **strict-reject** while implementations
  exist — no automatic cascade-withdraw. New status enum
  `CatalogStatus { ACTIVE, DEPRECATED }`, plus optional
  `deprecationReason` and `replacedBy` on `ServiceInterface`. See
  FR-Catalog-Immutable, FR-Catalog-Deprecation-Soft,
  FR-Catalog-Removal-StrictReject in §5.
- **Network-partition behavior**: the local registry stays read-only
  available while disconnected. Writes are rejected with a
  `Diagnostic`, no write queue. The `LocalServiceRegistry` exposes a
  `connectionState` attribute (`CONNECTED` / `DEGRADED` / `OFFLINE`).
  On reconnect a fresh snapshot is pulled and synthetic `ServiceEvent`s
  bring local listeners up to the new state. See FR-Partition-* in §5.
- **Local ↔ Remote sync mechanism**: **hybrid snapshot + event
  stream** over **HTTP-SSE**. On (re)connect the local registry pulls
  a capability-filtered snapshot; afterwards the broker pushes
  `ServiceEvent`s and `CatalogEvent`s over a persistent
  text/event-stream. Provider → broker writes (publish / withdraw /
  catalog mutations) stay as plain HTTP request / response, NOT events.
  See FR-Sync-* in §5.

---

## 9. Prototype scope / Definition of Done

The prototype is **"done"** when the following end-to-end scenario
runs reliably, with the same observable behavior in both Java and
TypeScript framework implementations:

### Scenario (concretized UC-4)

```
Setup:
  - 1 Remote Registry (Broker, Java, single-node) running locally
  - Catalog seeded from a hand-authored XMI containing two
    ServiceInterfaces:
      • "Payment"     (operations: charge, refund, getBalance)
      • "OrderQuery"  (operations: findById, listByCustomer)
  - 1 Java client framework (one flavor — OSGi or Plain-Java, the
    other can come later)
  - 1 TypeScript client framework
  - REST + MQTT flavor plugins available in both framework runtimes

Live actors:
  - 1 Java Provider implementing Payment, advertising REST flavor
  - 1 TypeScript Provider implementing Payment, advertising MQTT
    flavor  (intentional: same interface, different transports)
  - 1 TypeScript Consumer with both REST and MQTT client plugins
  - 1 Java Consumer with REST client plugin only

Demo flow:
  1. Broker starts, loads catalog XMI, opens HTTP-SSE event stream.
  2. Java framework starts, snapshot pulled from broker, SSE
     subscribed; Java Provider calls registerService → broker
     receives publishImplementation → CatalogEvent +
     ServiceEvent(REGISTERED) flow back to all locals.
  3. TS framework starts, snapshot pulled, SSE subscribed; TS
     Provider publishes its Payment-via-MQTT implementation.
  4. TS Consumer subscribes a ServiceListener for Payment.
  5. TS Consumer issues getServiceReferences("Payment", null,
     {supportedFlavors: [REST, MQTT]}) → receives BOTH refs.
     Invokes charge() on the REST one, getBalance() on the MQTT one
     — both succeed.
  6. Java Consumer issues the same lookup with
     {supportedFlavors: [REST]} → receives ONLY the Java/REST ref
     (flavor matching works).
  7. Java Provider calls registration.unregister() → UNREGISTERING
     fires synchronously to local listeners; broker is notified;
     TS Consumer's ServiceListener receives the UNREGISTERING event
     via the SSE stream.
  8. Repeat scenario with TS Provider as the registrant and Java
     Consumer as the listener, demonstrating symmetric behavior.
```

### In scope for the prototype

- Modell + EMF-Codegen (Java EMF, TypeScript via `ecore.ts`)
- Broker mit den in §5 Distribution / Sync / Partition / Catalog
  beschriebenen Operations (publish, withdraw, snapshot, SSE event
  stream, in-memory ResourceSet + XMI snapshot)
- Java client framework (eine der zwei Flavors)
- TypeScript client framework on top of Daanse TSM
- REST + MQTT flavor plugins in both frameworks (reference impls)
- Code Publisher generates Java + TS stubs **locally** (no Nexus /
  npm registry push required)
- `ServiceListener` lifecycle (REGISTERED + UNREGISTERING delivery)
- `ConsumerCapability`-driven flavor filtering at lookup
- Cross-language symmetry: the same provider/consumer flows work
  with the languages swapped (Java↔TS), proving NFR-Behavioral-Parity
- A test suite that runs identical scenarios against both framework
  implementations and asserts identical observable outcomes

### Out of scope for the prototype (deferred to later iterations)

- The second Java flavor (if OSGi is in, Plain-Java is deferred or
  vice versa)
- Python framework (deferred per language priority decision)
- Runtime catalog mutations (`deprecateCatalogEntry`,
  `removeCatalogEntry`) — catalog stays static
- Hook implementations (`PublishHook`, `DiscoveryHook`,
  `DistributionHook`) — prototype runs hook-free
- Code Publisher → real Nexus / npm / PyPI publishing (artifacts
  stay local file:// for now)
- Reverse-engineering of catalog entries from source code
  (catalog is hand-authored XMI)
- Partition-recovery scenarios (broker restart, network drop) —
  the design supports it (NFR-RemoteRegistry-Storage,
  FR-Partition-*) but explicit verification is post-prototype
- Multi-tenancy, security, audit, identity — entirely external
  via Hooks, none of which the prototype demonstrates
- HA / clustering of the broker
- Semver-bump validation in the broker (the rules are written
  down in FR-CodeDist-SemverRules-*, but enforcement is
  post-prototype)
- Catalog-event delivery on SSE (only `ServiceEvent` is required;
  `CatalogEvent` shapes are described but their delivery is
  post-prototype because catalog is static in the prototype)

### Success criteria

The prototype is **done** when:

1. The Demo flow above completes end-to-end without manual workarounds.
2. The same flow runs with Java and TypeScript actors swapped, with
   identical observable outcomes (timing differences excluded).
3. The test suite asserting cross-language behavioral parity passes
   green on a clean checkout and `./gradlew test` / `npm test`.

---

## 10. Glossary

- **Component** — a declarative service definition (cf. OSGi DS
  `@Component`). Modeled as `ComponentDescription`.
- **Configuration** — a runtime instance of a component with resolved
  properties and references. Modeled as `ComponentConfiguration`.
- **Service** — a published instance of a service interface, addressable
  via a `ServiceReference`.
- **Service Interface** — the contract a service implements; identified
  by a symbolic name (FQN-style) and a version.
- **Provider** — a deployment unit that owns components and registers
  services. Language-neutral counterpart to an OSGi `Bundle`. Modeled
  as `ServiceProvider`.
- **Registry** — the central object that holds providers, registrations,
  references, configurations, and listeners. There are two flavors:
  the **local registry** (in-process, per language framework) and the
  **Remote Registry** (the shared Broker).
- **Local Registry** — per-process registry that mirrors OSGi's
  in-VM service registry. Holds local registrations and listeners, and
  delegates outward lookups to the Remote Registry.
- **Remote Registry** / **Broker** — the centrally hosted DDSR
  component that holds the API catalog plus the global implementation
  index. Provider/consumer wire traffic does NOT pass through it; it
  is purely the directory.
- **API Catalog** — the curated set of service descriptions allowed in
  the system. Lives in the Remote Registry; managed by the Governance
  officer.
- **Governance officer** — the role that releases new or changed
  service descriptions into the API catalog.
- **Flavor** — a transport binding (REST, MQTT, …) over which a
  service implementation is reachable. A single implementation may
  advertise multiple flavors; the same interface may be implemented by
  different providers over different flavors.
- **Flavor Plugin** — a third-party-contributable module that adds
  support for a transport flavor on either the provider side
  ("publish") or the consumer side ("client").
- **Interception Hooks** — DDSR's plug-points where external
  policy/audit/mutation logic intervenes. Three kinds: `PublishHook`
  (provider operations), `DiscoveryHook` (lookup, subscription,
  result filtering), `DistributionHook` (events crossing local↔remote
  boundary). Hooks return a `Diagnostic`; severity OK ⇒ proceed,
  ERROR/CANCEL ⇒ abort.
- **PDP / PEP** — Policy Decision Point / Policy Enforcement Point;
  XACML terminology. DDSR does NOT implement these — production
  deployments integrate an external PDP/PEP via the Interception
  Hooks.
- **Diagnostic** — a structured failure or warning record;
  structurally analogous to `org.eclipse.emf.common.util.Diagnostic`
  but defined in DDSR's own EClass.
- **Service Operation** — a single method on a `ServiceInterface`:
  named, parameters with optional constraints, optional return type
  with constraints, declared exceptions. Modeled as `ServiceOperation`.
- **Parameter** — a positional, typed input to a `ServiceOperation`;
  may carry `ParameterConstraint`s (required, numeric range, string
  pattern, enumeration, collection size).
- **Service Exception** — a declared error a `ServiceOperation` can
  throw; carries typed properties for the exception payload. Modeled as
  `ServiceException`.
- **Service Implementation** — a concrete realization of one or more
  service interfaces by a `ServiceProvider`, optionally reachable
  through one or more transport flavors. Distinct from
  `ComponentDescription`. Modeled as `ServiceImplementation`.
- **Service Operation Flavor** — the transport-specific binding for a
  single operation within a `ServiceFlavor` (e.g. HTTP method + path
  for REST, request/response topic + QoS for MQTT).
- **Consumer Capability** — a non-persistent bag of capabilities a
  consumer attaches to a lookup, primarily its supported flavors.
- **Code Publisher** — the service adjacent to the Remote Registry
  that emits typed POJO artifacts (JAR / npm package / wheel) when the
  Governance officer releases a catalog entry.
