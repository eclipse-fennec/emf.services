# DDSR Model Spec

The design specification for `ddsr.ecore` — a language-neutral OSGi service model on top of EMF/Ecore.

- **Package**: `ddsr`
- **nsURI**: `http://geckoprojects.org/ddsr/1.0`
- **nsPrefix**: `ddsr`
- **basePackage**: `org.gecko.ddsr.model`

The template: the OSGi DS DTOs (`ComponentDescriptionDTO`, `ComponentConfigurationDTO`, `ReferenceDTO`, `SatisfiedReferenceDTO`, `UnsatisfiedReferenceDTO`, `ServiceReferenceDTO`) plus `ServiceEvent`/`ServiceRegistration`/`ServiceReference` from `org.osgi.framework`. Java-specific things are translated language-neutrally (see the section "Language-neutral translations").

## Implementation architecture (context)

The model is language-neutral, but the implementations follow the same **three-layer structure** in every language (see REQUIREMENTS §6 NFR-Three-Layer-Architecture):

| Layer | Java | TypeScript | Python |
|---|---|---|---|
| **EMF / POJOs** *(generated from the model)* | EMF | `ecore.ts` | PyEcore |
| **Component lifecycle / DI** *(engines that are natively there)* | OSGi DS *(the OSGi flavor)* or DDSR's own plain-Java lifecycle core | Daanse TSM | iPOPO |
| **DDSR's own layer** *(registry, lookup, events, listeners, remote bridge)* | hand-written, implementing the spec | hand-written, implementing the spec | hand-written, implementing the spec |

The bottom row is the lever for **behavioral parity** — everywhere it is the direct realisation of this spec. Mappings from the DDSR `ComponentState` enum onto the respective native lifecycle states (DS states / TSM `registered…stopped` / iPOPO states) have to be explicit and documented per language.

---

## Language-neutral translations

| OSGi DTO / API | The DDSR equivalent | Rationale |
|---|---|---|
| `BundleDTO bundle` | EClass `ServiceProvider` | "a deployment unit that registers services" — language-neutral |
| `String implementationClass` | `implementationId: EString` | symbolic instead of an FQN Java class |
| `bind/unbind/updated/field` | EClass `ReferenceBinding` (a container) | away from Java method names, towards the notion of a "hook" |
| `activate/deactivate/modified/activationFields/init` | EClass `LifecycleHook` (a container) | as above |
| `service.id : long` | `EString` (UUID) | more flexible across languages and the network |
| `Map<String,Object> properties` | `Property[0..*]` containment, typed subclasses | DTO-capable and language-neutral |
| `Throwable failure` | EClass `Diagnostic` (structured like the EMF `Diagnostic`) | structured instead of a stack-trace string |

---

## EEnums

| Enum | Literals (value) | Source |
|---|---|---|
| `ServiceScope` | `SINGLETON`, `BUNDLE`, `PROTOTYPE` | `ComponentDescriptionDTO.scope` |
| `ReferenceCardinality` | `ZERO_OR_ONE`, `ONE`, `ZERO_OR_MANY`, `ONE_OR_MANY` | `ReferenceDTO.cardinality` |
| `ReferencePolicy` | `STATIC`, `DYNAMIC` | `ReferenceDTO.policy` |
| `ReferencePolicyOption` | `RELUCTANT`, `GREEDY` | `ReferenceDTO.policyOption` |
| `ConfigurationPolicy` | `OPTIONAL`, `REQUIRE`, `IGNORE` | `ComponentDescriptionDTO.configurationPolicy` |
| `ComponentState` | `UNSATISFIED_CONFIGURATION = 1`, `UNSATISFIED_REFERENCE = 2`, `SATISFIED = 4`, `ACTIVE = 8`, `FAILED_ACTIVATION = 16` | the `ComponentConfigurationDTO` static ints (the values are bitwise!) |
| `ServiceEventType` | `REGISTERED = 1`, `MODIFIED = 2`, `UNREGISTERING = 4`, `MODIFIED_ENDMATCH = 8` | the `ServiceEvent` static ints (the values are bitwise!) |
| `FieldOption` | `REPLACE`, `UPDATE` | `ReferenceDTO.fieldOption` |
| `CollectionType` | `SERVICE`, `REFERENCE`, `SERVICEOBJECTS`, `PROPERTIES`, `TUPLE` | `ReferenceDTO.collectionType` |
| `LifecycleHookKind` | `ACTIVATE`, `DEACTIVATE`, `MODIFIED`, `ACTIVATION_FIELD` | derived from the DS method names |
| `ReferenceBindingKind` | `BIND`, `UNBIND`, `UPDATED`, `FIELD` | as above |
| `DiagnosticSeverity` | `OK = 0`, `INFO = 1`, `WARNING = 2`, `ERROR = 4`, `CANCEL = 8` | analogous to `org.eclipse.emf.common.util.Diagnostic` |
| `ExpressionLanguage` | `OCL` | The discriminator for `ExpressionConstraint` / `Invariant`. In the prototype OCL only; the surface stays open for CEL/JSON-Logic/a sublanguage of our own without a model migration. |
| `CatalogStatus` | `ACTIVE`, `DEPRECATED` | A lifecycle marker on the `ServiceInterface` in the catalog. ACTIVE = freely publishable and lookupable; DEPRECATED = existing implementations keep running, lookups keep delivering, new publishes produce a WARNING. The transition is one-way — a revival is a new entry. |
| `ConnectionState` | `CONNECTED`, `DEGRADED`, `OFFLINE` | The health of the connection from a `LocalServiceRegistry` to its remote registry. CONNECTED = the snapshot has arrived and the event stream is alive; DEGRADED = the connection was lost, a reconnect is running, the cache view is active; OFFLINE = never connected, or the reconnect has permanently failed. Writes are allowed in CONNECTED only; reads work in every state out of the last known state. |
| `FlavorKind` | `REST`, `MQTT` | The discriminator and the identifier string a consumer sends along in `ConsumerCapability.supportedFlavors`. Extensible through flavor plugins. |
| `HttpMethod` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS` | for `RestOperationFlavor.method` |
| `MqttQos` | `AT_MOST_ONCE = 0`, `AT_LEAST_ONCE = 1`, `EXACTLY_ONCE = 2` | the MQTT QoS levels |
| `RegistryKind` | `LOCAL`, `REMOTE` | The discriminator if one models the `ServiceRegistry` hierarchy through a discriminator rather than through subclasses *(see the decision in the API layer)* |

---

## Mixin interfaces

### `NamedElement` (abstract, interface)

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `name` | `EString` | `1..1` | `iD=true` |

### `VersionedElement` (abstract, interface)

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `version` | `EString` | `0..1` | semver, OCL validation TODO |

---

## The property layer

### `Property` (abstract) `extends NamedElement`

(no features of its own — the subclasses carry the `value`)

### The concrete property classes

All `extends Property`:

| Class | `value` type |
|---|---|
| `StringProperty` | `EString` |
| `IntProperty` | `EInt` |
| `LongProperty` | `ELong` |
| `DoubleProperty` | `EDouble` |
| `FloatProperty` | `EFloat` |
| `ShortProperty` | `EShort` |
| `BoolProperty` | `EBoolean` |
| `StringListProperty` | `EString[0..*]` |

---

## The description layer

### `ServiceInterface` `extends NamedElement, VersionedElement`

A class of its own with an identity, so that versioning and reuse work. It now carries the signature of the operations the interface offers — without that addition the API catalog would hold nothing about "what the services can do". **Conceptually immutable after `addCatalogEntry`** — a change ⇒ a new entry with a new version, the old one optionally deprecated.

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `description` | `EString` | `0..1` | | documentation text |
| `operations` | `ServiceOperation` | `0..*` | | **containment**, the interface's methods |
| `exceptions` | `ServiceException` | `0..*` | | **containment**, the exceptions the interface declares globally (operation-specific exceptions sit on the operation itself) |
| `invariants` | `Invariant` | `0..*` | | **containment**, class-level invariants |
| `status` | `CatalogStatus` | `1..1` | `ACTIVE` | The lifecycle marker in the catalog. `deprecateCatalogEntry` sets it to `DEPRECATED`; a one-way transition. |
| `deprecationReason` | `EString` | `0..1` | | free text, set at deprecation time; it lands in the WARNING diagnostic of `publishImplementation` |
| `replacedBy` | `ServiceInterface` | `0..1` | | non-containment, a migration hint towards a successor interface |

---

## The operation signature layer

It models *what* a service can do — methods, parameters with constraints, exceptions. It is the language-neutral description from which **POJO stubs** and typed interfaces are produced per language (published by the code publisher as a JAR / npm package / Python wheel — see REQUIREMENTS §5 "Code generation" and "Code distribution"). Code generation produces types only, no behaviour; behaviour lives in the framework of each language.

### `ServiceOperation` `extends NamedElement`

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `description` | `EString` | `0..1` | documentation text |
| `parameters` | `Parameter` | `0..*` | **containment**, ordered by `index` |
| `returnValue` | `Parameter` | `0..1` | **containment**, the return slot as a full parameter — type, multiplicity and constraints in one place; unset = void. `index` is meaningless here, and `optional` means "the result may be null" |
| `exceptions` | `ServiceException` | `0..*` | non-containment, pointers at exceptions defined on the `ServiceInterface` that this operation can throw |

### `Parameter` `extends NamedElement`

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `index` | `EInt` | `1..1` | | the positional index (0-based) — language-neutral and independent of the argument name in any language |
| `type` | `EString` | `0..1` | | a language-neutral type name; at least one of `type`/`eType` MUST be set |
| `eType` | `ecore::EClassifier` | `0..1` | | non-containment, the metamodel type: an `EClass` for EObject values, an Ecore `EDataType` for primitives. It travels over the wire as a cross-document href `<nsURI>#//<Name>` and does **not** have to be resolvable by the reader |
| `lowerBound` | `EInt` | `1..1` | `1` | the minimum number of values; for a single value it is redundant with `optional` and has to be kept consistent (`lowerBound = 0` exactly when `optional`) |
| `upperBound` | `EInt` | `1..1` | `1` | the maximum number of values, `-1` = an unbounded-but-finite collection. Unbounded streams over time belong to the interaction-style topic, not here |
| `optional` | `EBoolean` | `1..1` | `false` | |
| `defaultValue` | `EString` | `0..1` | | a string-encoded default value (parsing per type is the implementation's business) |
| `description` | `EString` | `0..1` | | documentation text |
| `constraints` | `ParameterConstraint` | `0..*` | | **containment** |

### `ParameterConstraint` (abstract)

The base for typed validity ranges. **Not** `extends NamedElement` — constraints are not named but positional under their parameter.

(no features of its own)

### The concrete constraint classes

All `extends ParameterConstraint`:

| Class | Features | Notes |
|---|---|---|
| `RequiredConstraint` | *(a marker)* | The parameter must not be null/missing. Redundant with `Parameter.optional = false`, but expressible explicitly. |
| `NumericRangeConstraint` | `min: EDouble [0..1]`, `max: EDouble [0..1]`, `inclusiveMin: EBoolean = true`, `inclusiveMax: EBoolean = true` | `EDouble` covers every numeric type; the implementation has to cast back into the parameter's type. |
| `StringPatternConstraint` | `pattern: EString [1..1]`, `minLength: EInt [0..1]`, `maxLength: EInt [0..1]` | A regex pattern; the syntax is ECMA-262 (natively supported in TS, compatible in Java/Python). |
| `EnumerationConstraint` | `allowedValues: EString [1..*]` | The allowed stringified values. |
| `CollectionSizeConstraint` | `minSize: EInt [0..1]`, `maxSize: EInt [0..1]` | For parameter types that are lists/arrays. |
| `ExpressionConstraint` *(extends `ParameterConstraint, NamedElement`)* | `language: ExpressionLanguage [1..1] = OCL`, `expression: EString [1..1]`, `message: EString [0..1]` | An open constraint with an identity (a name, for violation reporting). For everything the closed constraint list does not cover: cross-parameter checks, conditional rules, domain logic. The evaluation context: `self` = the runtime value of the parameter or return value; `op` = the enclosing `ServiceOperation`; `params` = a map name→value (available in pre/post). |

### `Invariant` `extends NamedElement`

A sibling class to `ExpressionConstraint`, but **not** a `ParameterConstraint` — invariants are not bound to a parameter or a return value but to the *operation* (pre/post) or to the *interface* (a class-level invariant).

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `language` | `ExpressionLanguage` | `1..1` | `OCL` | |
| `expression` | `EString` | `1..1` | | a boolean expression; multi-line is allowed |
| `message` | `EString` | `0..1` | | optional, for violation reporting |

**Used as containment in**:
- `ServiceOperation.preconditions [0..*]` — they hold on entry; the context is `self`, `params`, `op`. A violation ⇒ the call is refused and the implementation is not invoked.
- `ServiceOperation.postconditions [0..*]` — they hold on a successful exit; the context additionally has `result` = the return value. A violation ⇒ an ERROR diagnostic (a broken implementation contract).
- `ServiceInterface.invariants [0..*]` — they hold for every instance, before and after every operation; the context is `self` = the service object.

### `ServiceException` `extends NamedElement, VersionedElement`

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `description` | `EString` | `0..1` | documentation |
| `type` | `EString` | `1..1` | the symbolic exception name (language-neutral, FQN style) |
| `properties` | `Property` | `0..*` | **containment**, the fields the exception payload carries (e.g. `errorCode`, `retryable`) |

---

### `LifecycleHook` `extends NamedElement`

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `kind` | `LifecycleHookKind` | `1..1` | |
| `parameter` | `EInt` | `0..1` | only for the `init`/constructor param |

### `ReferenceBinding` `extends NamedElement`

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `kind` | `ReferenceBindingKind` | `1..1` | |
| `fieldOption` | `FieldOption` | `0..1` | only for `kind = FIELD` |

### `ComponentReference` `extends NamedElement`  (≈ `ReferenceDTO`)

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `interfaceName` | `EString` | `1..1` | | the FQN of the service interface |
| `cardinality` | `ReferenceCardinality` | `1..1` | `ONE` | |
| `policy` | `ReferencePolicy` | `1..1` | `STATIC` | |
| `policyOption` | `ReferencePolicyOption` | `1..1` | `RELUCTANT` | |
| `target` | `EString` | `0..1` | | an LDAP filter |
| `scope` | `ServiceScope` | `1..1` | `BUNDLE` | |
| `collectionType` | `CollectionType` | `0..1` | | |
| `parameter` | `EInt` | `0..1` | | the constructor parameter index (DS 1.4) |
| `bindings` | `ReferenceBinding` | `0..*` | | **containment** |

### `ComponentDescription` `extends NamedElement` (≈ `ComponentDescriptionDTO`)

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `factory` | `EString` | `0..1` | | the factory name (a DS factory component) |
| `scope` | `ServiceScope` | `1..1` | `SINGLETON` | |
| `implementationId` | `EString` | `1..1` | | language-neutral, instead of `implementationClass` |
| `defaultEnabled` | `EBoolean` | `1..1` | `true` | |
| `immediate` | `EBoolean` | `1..1` | `false` | |
| `configurationPolicy` | `ConfigurationPolicy` | `1..1` | `OPTIONAL` | |
| `configurationPid` | `EString` | `0..*` | | |
| `serviceInterfaces` | `ServiceInterface` | `0..*` | | non-containment (a shared identity) |
| `properties` | `Property` | `0..*` | | **containment** |
| `factoryProperties` | `Property` | `0..*` | | **containment** |
| `references` | `ComponentReference` | `0..*` | | **containment** |
| `lifecycleHooks` | `LifecycleHook` | `0..*` | | **containment**, replacing `activate/deactivate/modified/activationFields/init` |
| `provider` | `ServiceProvider` | `0..1` | | non-containment (the provider "owns" the descriptions, see `ServiceProvider.descriptions`) |

### `ServiceProvider` `extends NamedElement, VersionedElement`

The language-neutral counterpart of an OSGi `Bundle`.

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `symbolicName` | `EString` | `1..1` | |
| `descriptions` | `ComponentDescription` | `0..*` | **containment** |
| `implementations` | `ServiceImplementation` | `0..*` | **containment**, the concrete offerings this provider makes (see below) |

### `ServiceImplementation` `extends NamedElement, VersionedElement`

A concrete offering of one (or several) `ServiceInterface`s by a provider over particular transport flavors. **Kept apart from `ComponentDescription`**, because an implementation can exist without a DS declaration too (a "plain" Java service, say, or a TS/Python implementation with no DS counterpart). When it is DS-declared it points back at a `ComponentDescription`.

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `description` | `EString` | `0..1` | documentation |
| `implementationId` | `EString` | `1..1` | the language-neutral symbolic identifier of the implementation class / module / file |
| `serviceInterfaces` | `ServiceInterface` | `1..*` | non-containment, which interfaces this implementation fulfils |
| `flavors` | `ServiceFlavor` | `0..*` | **containment**, over which transports it is reachable (empty for a purely local offering) |
| `properties` | `Property` | `0..*` | **containment**, implementation-specific properties (service ranking, tenancy, …) |
| `componentDescription` | `ComponentDescription` | `0..1` | non-containment; set when it is DS-driven |

---

## The flavor / transport binding layer

It models *how* an implementation is reachable over the network. A `ServiceImplementation` can offer several `ServiceFlavor`s at once (the same service over REST *and* over MQTT, say). The consumer picks a flavor by its `ConsumerCapability` (see the API layer).

### `ServiceFlavor` (abstract) `extends NamedElement`

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `kind` | `FlavorKind` | `1..1` | the discriminator (REST/MQTT/…); redundant with the subclass but handy for consumer-side filtering |
| `operationFlavors` | `ServiceOperationFlavor` | `0..*` | **containment**, the transport-specific binding per operation |

### `RestFlavor` `extends ServiceFlavor`

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `host` | `EString` | `0..1` | | optional, may sit in the service reference at runtime |
| `basePath` | `EString` | `1..1` | | the URL prefix component shared by every operation |
| `contentTypes` | `EString` | `0..*` | | the allowed/produced content types as a default |

### `MqttFlavor` `extends ServiceFlavor`

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `brokers` | `EString` | `1..*` | | the broker URLs (mqtt(s)://…) |
| `requestTopic` | `EString` | `1..1` | | the default topic for requests; operations may override it |
| `responseTopic` | `EString` | `0..1` | | the default topic for responses |
| `defaultQos` | `MqttQos` | `1..1` | `AT_LEAST_ONCE` | |
| `defaultRetained` | `EBoolean` | `1..1` | `false` | |

### `ServiceOperationFlavor` (abstract) `extends NamedElement`

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `operation` | `ServiceOperation` | `1..1` | non-containment, which interface operation this binding points at |
| `consumes` | `EString` | `0..*` | the content types for the request body (overriding the flavor default) |
| `produces` | `EString` | `0..*` | the content types for the response body |

### `RestOperationFlavor` `extends ServiceOperationFlavor`

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `method` | `HttpMethod` | `1..1` | | |
| `path` | `EString` | `0..1` | | relative to `RestFlavor.basePath`; `null` = directly on `basePath` |
| `returnCodes` | `EInt` | `1..*` | | the HTTP status codes expected on success (typically `[200, 204]`) |

### `MqttOperationFlavor` `extends ServiceOperationFlavor`

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `requestTopic` | `EString` | `0..1` | | overrides the flavor default for this operation |
| `responseTopic` | `EString` | `0..1` | | as above |
| `qos` | `MqttQos` | `0..1` | | as above |
| `retained` | `EBoolean` | `0..1` | | as above |
| `correlation` | `EBoolean` | `1..1` | `true` | whether request and response have to be matched by correlationId |
| `returnPath` | `EString` | `0..1` | | optional, an alternative convention for asynchronous answers |

---

## The runtime layer

### `ServiceReference` (≈ `ServiceReferenceDTO`)

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `id` | `EString` | `1..1` | a UUID, `iD=true` |
| `properties` | `Property` | `0..*` | **containment** |
| `provider` | `ServiceProvider` | `1..1` | non-containment (instead of `bundle: long`) |
| `usingProviders` | `ServiceProvider` | `0..*` | non-containment (instead of `usingBundles: long[]`) |
| `registration` | `ServiceRegistration` | `0..1` | the opposite of `ServiceRegistration.reference` |

**EOperations**:
- `getProperty(key: EString) : EJavaObject` — look a property up by name
- `getPropertyKeys() : EString[*]` — the names of every property that is set

### `ServiceRegistration` (the provider's view)

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `reference` | `ServiceReference` | `1..1` | non-containment, the opposite of `ServiceReference.registration` *(the container is `ServiceRegistry.references`)* |
| `unregistered` | `EBoolean` | `1..1` (default `false`) | |

**EOperations**:
- `unregister() : void`
- `setProperties(props: Property[*]) : void`

### `ComponentConfiguration` `extends NamedElement` (≈ `ComponentConfigurationDTO`)

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `id` | `EString` | `1..1` | `iD=true`, the component.id |
| `description` | `ComponentDescription` | `1..1` | non-containment |
| `state` | `ComponentState` | `1..1` | |
| `properties` | `Property` | `0..*` | **containment** |
| `satisfiedReferences` | `SatisfiedReference` | `0..*` | **containment** |
| `unsatisfiedReferences` | `UnsatisfiedReference` | `0..*` | **containment** |
| `failure` | `Diagnostic` | `0..1` | **containment**, only when `state = FAILED_ACTIVATION` |
| `service` | `ServiceReference` | `0..1` | non-containment, the registered service reference (if the component registers a service) |

### `SatisfiedReference`

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `name` | `EString` | `1..1` | the name of the `ComponentReference` |
| `target` | `EString` | `0..1` | |
| `boundServices` | `ServiceReference` | `0..*` | non-containment |

### `UnsatisfiedReference`

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `name` | `EString` | `1..1` | the name of the `ComponentReference` |
| `target` | `EString` | `0..1` | |
| `targetServices` | `ServiceReference` | `0..*` | non-containment |

### `Diagnostic` (rebuilt language-neutrally, structured like `org.eclipse.emf.common.util.Diagnostic`)

| Feature | Type | Bounds | Default | Notes |
|---|---|---|---|---|
| `severity` | `DiagnosticSeverity` | `1..1` | `OK` | |
| `message` | `EString` | `0..1` | | |
| `source` | `EString` | `0..1` | | e.g. `"org.gecko.ddsr.runtime"` |
| `code` | `EInt` | `1..1` | `0` | |
| `data` | `EString` | `0..*` | | stringified context data (language-neutral; a Java object list was avoided) |
| `children` | `Diagnostic` | `0..*` | | **containment**, the recursive chain of causes |

---

## The event layer

### `ServiceEvent`

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `type` | `ServiceEventType` | `1..1` | |
| `reference` | `ServiceReference` | `1..1` | non-containment |
| `timestamp` | `EDate` | `0..1` | |

### `ServiceListener` (abstract, interface)

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `filter` | `EString` | `0..1` | an LDAP filter, optional |

**EOperations**:
- `serviceChanged(event: ServiceEvent) : void`

---

## The API layer

The registry hierarchy is two-tiered now: an abstract `ServiceRegistry` base plus two concrete subclasses, `LocalServiceRegistry` (in process) and `RemoteServiceRegistry` (the broker). The split is explicit because a local and a remote registry hold structurally and semantically different pieces of state.

### `ServiceRegistry` (abstract) `extends NamedElement`

The shared base. It holds *only* what both kinds of registry have in common.

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `kind` | `RegistryKind` | `1..1` | a discriminator on top of the subclass — it helps when filtering lists of heterogeneous registries |

**Abstract EOperations** (implemented by the subclasses):

| Operation | Parameters | Return |
|---|---|---|
| `getServiceReference` | `interfaceName: EString` | `ServiceReference` |
| `getServiceReferences` | `interfaceName: EString, filter: EString, capability: ConsumerCapability` | `ServiceReference[*]` |
| `getAllServiceReferences` | `interfaceName: EString, filter: EString, capability: ConsumerCapability` | `ServiceReference[*]` |
| `addServiceListener` | `listener: ServiceListener` | `void` |
| `removeServiceListener` | `listener: ServiceListener` | `void` |

A note: `capability` is a mandatory parameter on `getServiceReferences`/`getAllServiceReferences` because the flavor matching hangs off it. For an in-process lookup (a LocalServiceRegistry) it will mostly be `null`/empty — then every flavor counts as available.

### `LocalServiceRegistry` `extends ServiceRegistry`

What used to be `ServiceRegistry` — the OSGi-`ServiceRegistry`-like in-process component.

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `references` | `ServiceReference` | `0..*` | **containment**, every locally registered service |
| `registrations` | `ServiceRegistration` | `0..*` | **containment**, the provider's view of the services in `references` |
| `configurations` | `ComponentConfiguration` | `0..*` | **containment** |
| `providers` | `ServiceProvider` | `0..*` | **containment** |
| `listeners` | `ServiceListener` | `0..*` | non-containment |
| `remote` | `RemoteServiceRegistry` | `0..1` | non-containment, the remote registry delegated to (if connected) |
| `connectionState` | `ConnectionState` | `1..1` (default `OFFLINE`) | The health of the connection to the remote registry. In DEGRADED/OFFLINE: refuse writes, serve reads from the last known state. A reconnect synthesises ServiceEvents for the diff. |

**Additional EOperations** (beyond the abstract base):

| Operation | Parameters | Return |
|---|---|---|
| `registerService` | `provider: ServiceProvider, implementation: ServiceImplementation, props: Property[*]` | `ServiceRegistration` |
| `fireServiceEvent` | `event: ServiceEvent` | `void` |

The behaviour of `registerService`: the local entry *synchronously*, the local listeners *synchronously*, the remote propagation *asynchronously* (FR-Dist-Framework-Owns-Comms). The exact step ordering is pinned down in §5 Lifecycle of the REQUIREMENTS.

### `RemoteServiceRegistry` `extends ServiceRegistry`

The central broker. It holds the **API catalog** and the **implementation index**.

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `endpoint` | `EString` | `0..1` | The URL/address of the broker service as the client sees it (a cluster VIP, say). It may be empty on the server side. |
| `catalog` | `ServiceInterface` | `0..*` | **containment**, the API catalog: every service interface allowed in this system (with its operations + constraints + exceptions) |
| `implementations` | `ServiceImplementation` | `0..*` | non-containment, every reported implementation of every provider |
| `providers` | `ServiceProvider` | `0..*` | non-containment, the providers that have registered with the broker |

**Additional EOperations**:

| Operation | Parameters | Return |
|---|---|---|
| `publishImplementation` | `provider: ServiceProvider, implementation: ServiceImplementation` | `Diagnostic` *(success/failure; a WARNING when the referenced interface is DEPRECATED, OK otherwise)* |
| `withdrawImplementation` | `provider: ServiceProvider, implementation: ServiceImplementation` | `Diagnostic` — symmetric to `publishImplementation`; the provider is explicit so that the ownership check (the implementation MUST belong to the provider) and the PublishHook authorization are unambiguous |
| `deprecateCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` — a soft marker: it sets `status = DEPRECATED`, existing implementations stay, lookups keep delivering. One-way. |
| `removeCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` — a strict reject: ERROR with code `CATALOG_HAS_LIVE_IMPLS` as long as implementations reference it. No auto-cascade. |
| `addCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` — flowed through PDP/PEP, gates on governance officer authorization |
| `deprecateCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` |
| `removeCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` |

### `ConsumerCapability`

What a consumer sends along with a lookup so that flavor matching is possible on the registry side. **Not persistent** — passed per lookup.

| Feature | Type | Bounds | Notes |
|---|---|---|---|
| `consumerId` | `EString` | `0..1` | optional, for auditing / policy decisions |
| `supportedFlavors` | `FlavorKind` | `1..*` | at least one flavor — otherwise the consumer cannot consume anything at all |
| `properties` | `Property` | `0..*` | **containment**, additional capability hints (supported content types, encoding preferences, …) |

---

## Interception hooks

DDSR contains **no policy/PDP semantics of its own**. Instead there are three hook interfaces where an integrator can attach arbitrary external logic (a PDP, IAM, auditing, property mutation, tenant filtering, …). The prototype ships with *no* hook implementations and therefore runs hook-free (every action is allowed).

**The hook contract**: every hook method returns a `Diagnostic`. Severity `OK` / `INFO` / `WARNING` → carry on; `ERROR` / `CANCEL` → abort and hand that diagnostic back to the caller. Several hooks form a pipeline — each one has to return OK, otherwise it aborts.

The three hook lists hang off `ServiceRegistry` non-containment (and are therefore inherited by `LocalServiceRegistry` and `RemoteServiceRegistry`):

- `publishHooks : PublishHook [0..*]`
- `discoveryHooks : DiscoveryHook [0..*]`
- `distributionHooks : DistributionHook [0..*]`

### `PublishHook` (abstract, interface)

The provider side. Consulted before `publishImplementation` / `withdrawImplementation`.

**EOperations**:
- `onPublish(provider: ServiceProvider, implementation: ServiceImplementation) : Diagnostic`
- `onWithdraw(provider: ServiceProvider, implementation: ServiceImplementation) : Diagnostic`

### `DiscoveryHook` (abstract, interface)

The consumer side. Consulted before a lookup and a subscription, plus for filtering the result.

**EOperations**:
- `onLookup(interfaceName: EString, filter: EString, capability: ConsumerCapability) : Diagnostic`
- `filterReferences(capability: ConsumerCapability, references: ServiceReference[*]) : ServiceReference[*]`
- `onSubscribe(listener: ServiceListener) : Diagnostic`

### `DistributionHook` (abstract, interface)

The federation boundary. Consulted on outbound (local → broker) and inbound (broker → local) events.

**EOperations**:
- `onOutbound(event: ServiceEvent) : Diagnostic`
- `onInbound(event: ServiceEvent) : Diagnostic`

### The relationship to XACML / OPA

Integrators who want to attach an external XACML system write an adapter hook: in `onPublish` the adapter builds an XACML request, sends it to the PDP and maps the answer onto `Diagnostic.severity`. The same for OPA, IAM tokens, in-house policy engines. DDSR need know nothing about the concrete policy format — the hook is the point of transition.

---

## Deliberately NOT in the first cut

- **`ServiceComponentRuntime`** as an EClass of its own — it will follow if DS lifecycle operations (`enableComponent`, `disableComponent`, `getComponentDescriptions(provider)`, `getComponentConfigurations(description)`) are needed.
- **`PrototypeServiceFactory` / `ServiceObjects`** — the prototype scope is captured for now only as a `ServiceScope` enum value, with no creation API of its own.
- **The `Bundle` lifecycle** (`STARTING`/`ACTIVE`/`STOPPING`/…) — DDSR models services, not deployment units.
- **`Filter` as an EClass** — it is stored as a plain `EString` (LDAP filter syntax) for now. The parser/evaluator is the implementation's business.
- **Concrete policy engine implementations** (an XACML parser, OPA bindings, …). The model delivers the three interception hooks (`PublishHook`, `DiscoveryHook`, `DistributionHook`); authorization logic comes from outside as a hook adapter.
- **Further flavors beyond REST/MQTT** (gRPC, WebSocket, Kafka, AMQP, …). They will be needed once there is a concrete need; the flavor plugin concept is made for exactly that.
- **Concrete flavor client/server implementations.** The `RestFlavor`/`MqttFlavor` classes are pure descriptions — the HTTP and MQTT libraries that actually realise them live outside the Ecore model.

---

## Open TODOs after the first pass at the model

### OCL constraints

The OCL engine is **Fennec M2X OCL** under `/opt/git/m2m/workspace`. The documentation is in `docs/ocl-user-guide.md` and `docs/ocl-architecture.md`. The annotation namespace is `http://www.eclipse.org/fennec/m2x/ocl/1.0`, and the detail keys are the constraint names (for invariants) or the reserved `derive`, `initial`, `body`, `pre`, `post` for setting/invocation delegates.

**Already executable as M2X OCL in ddsr.ecore:**

| Class | Invariant name | Condition |
|---|---|---|
| `VersionedElement` | `validSemver` | `version = null or version.matches('^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$')` |
| `NumericRangeConstraint` | `atLeastOneBound` | `min ≠ null or max ≠ null` |
| `NumericRangeConstraint` | `rangeOrdered` | `min = null or max = null or min ≤ max` |
| `StringPatternConstraint` | `lengthBoundsNonNegative` | both length bounds ≥ 0 when set |
| `StringPatternConstraint` | `lengthBoundsOrdered` | `minLength ≤ maxLength` when both are set |
| `CollectionSizeConstraint` | `sizeBoundsNonNegative` | both size bounds ≥ 0 when set |
| `CollectionSizeConstraint` | `sizeBoundsOrdered` | `minSize ≤ maxSize` when both are set |
| `ComponentConfiguration` | `failureOnlyWhenFailed` | `failure ≠ null` ⇔ `state = FAILED_ACTIVATION` |
| `ServiceRegistration` | `unregisteredNotInRegistry` | when `unregistered`, it is contained in no `LocalServiceRegistry.registrations` |
| `ServiceImplementation` | `atLeastOneInterface` | `serviceInterfaces->notEmpty()` |
| `ServiceImplementation` | `operationFlavorsCoverInterfaces` | every `ServiceOperationFlavor.operation` belongs to one of the `serviceInterfaces` |
| `RemoteServiceRegistry` | `publishedImplsHaveFlavor` | every published implementation has ≥1 flavor |
| `RemoteServiceRegistry` | `publishedImplsReferenceCatalog` | every `serviceInterfaces` reference of a published impl exists in the `catalog` |
| `RemoteServiceRegistry` | `publishedImplsOwnedByListedProvider` | every published impl is, by containment, inside one of the `providers` |

**The regex syntax validity of StringPatternConstraint**: not statically decidable in OCL — it is checked by whoever calls the engine (Java `Pattern.compile`, TS `new RegExp`, Python `re.compile`).

**Withdraw ownership** (`withdrawImplementation(provider, implementation)`): `implementation.eContainer() = provider`. It is not expressed as a static OCL invariant today because it holds at runtime over a *pair of operation parameters*, not over persistent model state — it belongs in the operation's `pre:` body. **TODO** (as soon as we activate setting/invocation delegates):

```
pre ownershipCheck: implementation.eContainer() = provider
```

## ExpressionConstraint and Invariant in practice

Whoever wants to use plain OCL annotations on the Ecore EModelElement (a condition on the model rather than on the model's *content*, say) uses the M2X detail keys directly — `body`, `pre`, `post`, `derive`, `initial`, a constraint name. Whoever has to express a constraint **as model content** (the typical case: the governance officer writes `amount > 0` in the API catalog without knowing the Ecore) uses `ExpressionConstraint` / `Invariant`.

Examples:

```ocl
-- Parameter.constraints (on an 'amount: int' parameter)
ExpressionConstraint {
  name = "positiveAmount"
  expression = "self > 0"
  message = "amount must be positive"
}

-- Operation.preconditions
Invariant {
  name = "amountWithinBalance"
  expression = "params['amount'] <= self.balance"
}

-- Operation.postconditions
Invariant {
  name = "balanceDecreased"
  expression = "self.balance = self.balance@pre - params['amount']"
}

-- ServiceInterface.invariants
Invariant {
  name = "nonNegativeBalance"
  expression = "self.balance >= 0"
}
```

The DDSR engines (Java, TS) have to bring an evaluator for every supported `ExpressionLanguage`. In the prototype that is **OCL** only — on the Java side it runs through Fennec M2X, on the TS side an OCL implementation is needed; failing that, the alternative is to build an OCL-to-TS compiler in the code publisher that translates `ExpressionConstraint.expression` into TypeScript code. (That decision is not needed now — the model surface is open.)

### Model extension TODOs (once the REQUIREMENTS open questions are settled)

- **Q3 (the reverse-engineering source):** possibly an EAnnotation source of its own, `"http://geckoprojects.org/ddsr/reveng/1.0"`, with marker details (e.g. "extracted from Java annotation X").
- **Q4 (the PDP contract):** refine the `PolicyRequest`/`PolicyDecision` fields (a stub today).
- **Q8 (semver compatibility rules):** possibly EAnnotations on `ServiceOperation` / `Parameter` / `ParameterConstraint` carrying the version impact classification (e.g. `breakingChange="true"` as a hint for the code publisher).

### Miscellaneous
- GenModel: `oSGiCompatible = true`, `basePackage = org.gecko.ddsr.model`, `resource = XMI` (already in the current Ecore).
- Example instances `*.xmi` for one typical case each:
  - an API catalog snapshot (a `RemoteServiceRegistry` with two or three `ServiceInterface`s with operations + constraints)
  - a `ServiceImplementation` with a `RestFlavor` *and* an `MqttFlavor` for the same interface
  - a `LocalServiceRegistry` with a registration + reference + configuration
  - `ConsumerCapability` examples for a REST-only and a REST+MQTT consumer
  - the existing `ServiceProvider.xmi`, `ServiceRegistry.xmi`, `PaypalPaymentImpl.xmi` as migration templates.
