# DDSR Model Spec

Designspezifikation für `ddsr.ecore` — sprachneutrales OSGi-Service-Modell auf Basis von EMF/Ecore.

- **Package**: `ddsr`
- **nsURI**: `http://geckoprojects.org/ddsr/1.0`
- **nsPrefix**: `ddsr`
- **basePackage**: `org.gecko.ddsr.model`

Vorlage: OSGi-DS-DTOs (`ComponentDescriptionDTO`, `ComponentConfigurationDTO`, `ReferenceDTO`, `SatisfiedReferenceDTO`, `UnsatisfiedReferenceDTO`, `ServiceReferenceDTO`) sowie `ServiceEvent`/`ServiceRegistration`/`ServiceReference` aus `org.osgi.framework`. Java-spezifisches ist sprachneutral übersetzt (siehe Abschnitt „Sprachneutrale Übersetzungen").

## Implementations-Architektur (Kontext)

Das Modell ist sprachneutral, die Implementations folgen aber pro Sprache derselben **Drei-Schicht-Struktur** (siehe REQUIREMENTS §6 NFR-Three-Layer-Architecture):

| Schicht | Java | TypeScript | Python |
|---|---|---|---|
| **EMF / POJOs** *(aus dem Modell generiert)* | EMF | `ecore.ts` | PyEcore |
| **Component-Lifecycle / DI** *(nativ vorhandene Engines)* | OSGi DS *(OSGi-Flavor)* oder DDSR-eigener Plain-Java-Lifecycle-Core | Daanse TSM | iPOPO |
| **DDSR-eigene Schicht** *(Registry, Lookup, Events, Listeners, Remote-Bridge)* | hand-geschrieben, spec-implementierend | hand-geschrieben, spec-implementierend | hand-geschrieben, spec-implementierend |

Die unterste Zeile ist der Hebel für **Behavioral Parity** — sie ist überall die direkte Umsetzung dieser Spec. Mappings vom DDSR-`ComponentState`-Enum auf die jeweiligen nativen Lifecycle-States (DS-States / TSM `registered…stopped` / iPOPO-States) müssen pro Sprache explizit und dokumentiert sein.

---

## Sprachneutrale Übersetzungen

| OSGi-DTO / API | DDSR-Äquivalent | Begründung |
|---|---|---|
| `BundleDTO bundle` | EClass `ServiceProvider` | „Deployment-Einheit, die Services registriert" — sprachneutral |
| `String implementationClass` | `implementationId: EString` | symbolisch statt FQN-Java-Klasse |
| `bind/unbind/updated/field` | EClass `ReferenceBinding` (Container) | weg von Java-Method-Namen, hin zu „Hook"-Begriff |
| `activate/deactivate/modified/activationFields/init` | EClass `LifecycleHook` (Container) | s.o. |
| `service.id : long` | `EString` (UUID) | flexibler über Sprachen + Netzwerk |
| `Map<String,Object> properties` | `Property[0..*]` containment, typisierte Subklassen | DTO-tauglich + sprachneutral |
| `Throwable failure` | EClass `Diagnostic` (Struktur wie EMF-`Diagnostic`) | strukturiert statt Stack-Trace-String |

---

## EEnums

| Enum | Literale (Wert) | Quelle |
|---|---|---|
| `ServiceScope` | `SINGLETON`, `BUNDLE`, `PROTOTYPE` | `ComponentDescriptionDTO.scope` |
| `ReferenceCardinality` | `ZERO_OR_ONE`, `ONE`, `ZERO_OR_MANY`, `ONE_OR_MANY` | `ReferenceDTO.cardinality` |
| `ReferencePolicy` | `STATIC`, `DYNAMIC` | `ReferenceDTO.policy` |
| `ReferencePolicyOption` | `RELUCTANT`, `GREEDY` | `ReferenceDTO.policyOption` |
| `ConfigurationPolicy` | `OPTIONAL`, `REQUIRE`, `IGNORE` | `ComponentDescriptionDTO.configurationPolicy` |
| `ComponentState` | `UNSATISFIED_CONFIGURATION = 1`, `UNSATISFIED_REFERENCE = 2`, `SATISFIED = 4`, `ACTIVE = 8`, `FAILED_ACTIVATION = 16` | `ComponentConfigurationDTO` static ints (Werte bitweise!) |
| `ServiceEventType` | `REGISTERED = 1`, `MODIFIED = 2`, `UNREGISTERING = 4`, `MODIFIED_ENDMATCH = 8` | `ServiceEvent` static ints (Werte bitweise!) |
| `FieldOption` | `REPLACE`, `UPDATE` | `ReferenceDTO.fieldOption` |
| `CollectionType` | `SERVICE`, `REFERENCE`, `SERVICEOBJECTS`, `PROPERTIES`, `TUPLE` | `ReferenceDTO.collectionType` |
| `LifecycleHookKind` | `ACTIVATE`, `DEACTIVATE`, `MODIFIED`, `ACTIVATION_FIELD` | abgeleitet aus DS-Method-Namen |
| `ReferenceBindingKind` | `BIND`, `UNBIND`, `UPDATED`, `FIELD` | s.o. |
| `DiagnosticSeverity` | `OK = 0`, `INFO = 1`, `WARNING = 2`, `ERROR = 4`, `CANCEL = 8` | analog `org.eclipse.emf.common.util.Diagnostic` |
| `ExpressionLanguage` | `OCL` | Discriminator für `ExpressionConstraint` / `Invariant`. Im Prototyp nur OCL; Surface offen für CEL/JSON-Logic/eigene Sublanguage ohne Modell-Migration. |
| `CatalogStatus` | `ACTIVE`, `DEPRECATED` | Lifecycle-Marker am `ServiceInterface` im Katalog. ACTIVE = frei publishable und lookupable; DEPRECATED = bestehende Implementationen laufen weiter, Lookups liefern weiter, neue Publishes erzeugen WARNING. Transition ist einseitig — Revival = neuer Eintrag. |
| `ConnectionState` | `CONNECTED`, `DEGRADED`, `OFFLINE` | Health der Verbindung von `LocalServiceRegistry` zu ihrer Remote Registry. CONNECTED = Snapshot empfangen + Event-Stream lebt; DEGRADED = Verbindung verloren, Reconnect läuft, Cache-Sicht aktiv; OFFLINE = nie verbunden oder Reconnect permanent fehlgeschlagen. Writes nur in CONNECTED erlaubt; Reads in allen Zuständen aus dem letzten bekannten State. |
| `FlavorKind` | `REST`, `MQTT` | Discriminator und Identifier-String, den Consumer in `ConsumerCapability.supportedFlavors` mitschickt. Erweiterbar über Flavor-Plugins. |
| `HttpMethod` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS` | für `RestOperationFlavor.method` |
| `MqttQos` | `AT_MOST_ONCE = 0`, `AT_LEAST_ONCE = 1`, `EXACTLY_ONCE = 2` | MQTT QoS-Levels |
| `RegistryKind` | `LOCAL`, `REMOTE` | Discriminator wenn man `ServiceRegistry`-Hierarchie über Discriminator statt Subklasse modelliert *(siehe Entscheidung im API-Layer)* |

---

## Mixin-Interfaces

### `NamedElement` (abstract, interface)

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `name` | `EString` | `1..1` | `iD=true` |

### `VersionedElement` (abstract, interface)

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `version` | `EString` | `0..1` | semver, OCL-Validation TODO |

---

## Property-Layer

### `Property` (abstract) `extends NamedElement`

(keine eigenen Features — Subklassen tragen `value`)

### Konkrete Property-Klassen

Alle `extends Property`:

| Klasse | `value`-Typ |
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

## Description-Layer

### `ServiceInterface` `extends NamedElement, VersionedElement`

Eigenständige Klasse mit Identität, damit Versionierung und Wiederverwendung funktionieren. Trägt jetzt die Signatur der Operationen, die das Interface anbietet — ohne diese Erweiterung wäre der API-Katalog leer von „was die Services können". **Nach `addCatalogEntry` konzeptionell immutable** — Änderungen ⇒ neuer Eintrag mit neuer Version, alter optional deprecated.

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `description` | `EString` | `0..1` | | Doku-Text |
| `operations` | `ServiceOperation` | `0..*` | | **containment**, die Methoden des Interfaces |
| `exceptions` | `ServiceException` | `0..*` | | **containment**, Exceptions, die das Interface global deklariert (operation-spezifische Exceptions stehen auf der Operation selbst) |
| `invariants` | `Invariant` | `0..*` | | **containment**, Klassen-Level-Invarianten |
| `status` | `CatalogStatus` | `1..1` | `ACTIVE` | Lifecycle-Marker im Katalog. `deprecateCatalogEntry` setzt auf `DEPRECATED`; einseitige Transition. |
| `deprecationReason` | `EString` | `0..1` | | Frei-Text, gesetzt bei der Deprecation; landet im WARNING-Diagnostic von `publishImplementation` |
| `replacedBy` | `ServiceInterface` | `0..1` | | non-containment, Migration-Hint zu einem Nachfolger-Interface |

---

## Operation-Signatur-Layer

Modelliert *was* ein Service kann — Methoden, Parameter mit Constraints, Exceptions. Ist die sprachneutrale Beschreibung, aus der pro Sprache **POJO-Stubs** und typisierte Interfaces erzeugt werden (vom Code Publisher als JAR/npm-Package/Python-Wheel veröffentlicht — siehe REQUIREMENTS §5 „Code generation" und „Code distribution"). Codegen erzeugt nur Typen, kein Verhalten; Verhalten lebt im Framework pro Sprache.

### `ServiceOperation` `extends NamedElement`

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `description` | `EString` | `0..1` | Doku-Text |
| `parameters` | `Parameter` | `0..*` | **containment**, Reihenfolge per `index` |
| `returnType` | `EString` | `0..1` | sprachneutraler Typname (z.B. `"string"`, `"int"`, `"money.Money"`); `null` = void |
| `returnConstraints` | `ParameterConstraint` | `0..*` | **containment**, Constraints am Return-Wert (gleiche Constraint-Sprache wie auf Parametern) |
| `exceptions` | `ServiceException` | `0..*` | non-containment, Verweise auf am `ServiceInterface` definierte Exceptions, die diese Operation werfen kann |

### `Parameter` `extends NamedElement`

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `index` | `EInt` | `1..1` | | Positionsindex (0-based) — sprachneutral und sprach-unabhängig vom Argument-Name |
| `type` | `EString` | `1..1` | | sprachneutraler Typname |
| `optional` | `EBoolean` | `1..1` | `false` | |
| `defaultValue` | `EString` | `0..1` | | String-kodierter Default-Wert (Parsen je Typ Sache der Implementation) |
| `description` | `EString` | `0..1` | | Doku-Text |
| `constraints` | `ParameterConstraint` | `0..*` | | **containment** |

### `ParameterConstraint` (abstract)

Basis für typisierte Gültigkeitsbereiche. **Nicht** `extends NamedElement` — Constraints sind nicht benannt, sondern positionell unter ihrem Parameter.

(keine eigenen Features)

### Konkrete Constraint-Klassen

Alle `extends ParameterConstraint`:

| Klasse | Features | Notes |
|---|---|---|
| `RequiredConstraint` | *(marker)* | Parameter darf nicht null/missing sein. Redundant mit `Parameter.optional = false`, aber explizit ausdrückbar. |
| `NumericRangeConstraint` | `min: EDouble [0..1]`, `max: EDouble [0..1]`, `inclusiveMin: EBoolean = true`, `inclusiveMax: EBoolean = true` | `EDouble` deckt alle numerischen Typen ab; Implementation muss in den Parameter-Typ zurückcasten. |
| `StringPatternConstraint` | `pattern: EString [1..1]`, `minLength: EInt [0..1]`, `maxLength: EInt [0..1]` | Regex-Pattern; Syntax: ECMA-262 (wird in TS nativ unterstützt, in Java/Python kompatibel). |
| `EnumerationConstraint` | `allowedValues: EString [1..*]` | Erlaubte stringifizierte Werte. |
| `CollectionSizeConstraint` | `minSize: EInt [0..1]`, `maxSize: EInt [0..1]` | Für Parameter-Typen, die Listen/Arrays sind. |
| `ExpressionConstraint` *(extends `ParameterConstraint, NamedElement`)* | `language: ExpressionLanguage [1..1] = OCL`, `expression: EString [1..1]`, `message: EString [0..1]` | Offener Constraint mit Identität (Name für Violation-Reporting). Für alles, was die geschlossene Constraint-Liste nicht abdeckt: Cross-Parameter-Checks, bedingte Regeln, Domain-Logik. Evaluierungs-Kontext: `self` = Laufzeitwert des Parameters/Return-Werts; `op` = enclosing `ServiceOperation`; `params` = Map Name→Wert (verfügbar bei pre/post). |

### `Invariant` `extends NamedElement`

Geschwister-Klasse zu `ExpressionConstraint`, aber **kein** `ParameterConstraint` — Invariants sind nicht an einen Parameter oder Return-Wert gebunden, sondern an die *Operation* (pre/post) oder das *Interface* (Klassen-Level-Invariante).

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `language` | `ExpressionLanguage` | `1..1` | `OCL` | |
| `expression` | `EString` | `1..1` | | Boolean-Expression; multi-line erlaubt |
| `message` | `EString` | `0..1` | | Optional, für Violation-Reporting |

**Verwendet als Containment in**:
- `ServiceOperation.preconditions [0..*]` — gelten beim Eintritt; Kontext: `self`, `params`, `op`. Violation ⇒ Aufruf wird abgelehnt, Implementation nicht invoked.
- `ServiceOperation.postconditions [0..*]` — gelten beim erfolgreichen Verlassen; Kontext zusätzlich `result` = Return-Wert. Violation ⇒ ERROR-Diagnostic (Implementation-Contract-Bruch).
- `ServiceInterface.invariants [0..*]` — gelten für jede Instanz, vor und nach jeder Operation; Kontext: `self` = Service-Objekt.

### `ServiceException` `extends NamedElement, VersionedElement`

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `description` | `EString` | `0..1` | Doku |
| `type` | `EString` | `1..1` | symbolischer Exception-Name (sprachneutral, FQN-style) |
| `properties` | `Property` | `0..*` | **containment**, Felder, die der Exception-Payload tragen (z.B. `errorCode`, `retryable`) |

---

### `LifecycleHook` `extends NamedElement`

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `kind` | `LifecycleHookKind` | `1..1` | |
| `parameter` | `EInt` | `0..1` | nur für `init`/Konstruktor-Param |

### `ReferenceBinding` `extends NamedElement`

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `kind` | `ReferenceBindingKind` | `1..1` | |
| `fieldOption` | `FieldOption` | `0..1` | nur für `kind = FIELD` |

### `ComponentReference` `extends NamedElement`  (≈ `ReferenceDTO`)

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `interfaceName` | `EString` | `1..1` | | FQN des Service-Interfaces |
| `cardinality` | `ReferenceCardinality` | `1..1` | `ONE` | |
| `policy` | `ReferencePolicy` | `1..1` | `STATIC` | |
| `policyOption` | `ReferencePolicyOption` | `1..1` | `RELUCTANT` | |
| `target` | `EString` | `0..1` | | LDAP-Filter |
| `scope` | `ServiceScope` | `1..1` | `BUNDLE` | |
| `collectionType` | `CollectionType` | `0..1` | | |
| `parameter` | `EInt` | `0..1` | | Konstruktor-Parameter-Index (DS 1.4) |
| `bindings` | `ReferenceBinding` | `0..*` | | **containment** |

### `ComponentDescription` `extends NamedElement` (≈ `ComponentDescriptionDTO`)

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `factory` | `EString` | `0..1` | | Factory-Name (DS factory component) |
| `scope` | `ServiceScope` | `1..1` | `SINGLETON` | |
| `implementationId` | `EString` | `1..1` | | sprachneutral statt `implementationClass` |
| `defaultEnabled` | `EBoolean` | `1..1` | `true` | |
| `immediate` | `EBoolean` | `1..1` | `false` | |
| `configurationPolicy` | `ConfigurationPolicy` | `1..1` | `OPTIONAL` | |
| `configurationPid` | `EString` | `0..*` | | |
| `serviceInterfaces` | `ServiceInterface` | `0..*` | | non-containment (geteilte Identität) |
| `properties` | `Property` | `0..*` | | **containment** |
| `factoryProperties` | `Property` | `0..*` | | **containment** |
| `references` | `ComponentReference` | `0..*` | | **containment** |
| `lifecycleHooks` | `LifecycleHook` | `0..*` | | **containment**, ersetzt `activate/deactivate/modified/activationFields/init` |
| `provider` | `ServiceProvider` | `0..1` | | non-containment (Provider „besitzt" Descriptions, siehe `ServiceProvider.descriptions`) |

### `ServiceProvider` `extends NamedElement, VersionedElement`

Sprachneutrales Pendant zu OSGi `Bundle`.

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `symbolicName` | `EString` | `1..1` | |
| `descriptions` | `ComponentDescription` | `0..*` | **containment** |
| `implementations` | `ServiceImplementation` | `0..*` | **containment**, konkrete Bereitstellungen, die dieser Provider anbietet (s.u.) |

### `ServiceImplementation` `extends NamedElement, VersionedElement`

Konkrete Bereitstellung eines (oder mehrerer) `ServiceInterface`s durch einen Provider über bestimmte Transport-Flavors. **Getrennt von `ComponentDescription`**, weil eine Implementation auch ohne DS-Deklaration existieren kann (z.B. ein „plain" Java-Service, oder eine TS/Python-Implementation ohne DS-Pendant). Wenn DS-deklariert, verweist sie zurück auf eine `ComponentDescription`.

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `description` | `EString` | `0..1` | Doku |
| `implementationId` | `EString` | `1..1` | sprachneutraler symbolischer Bezeichner der Implementations-Klasse / des Moduls / der Datei |
| `serviceInterfaces` | `ServiceInterface` | `1..*` | non-containment, welche Interfaces diese Implementation erfüllt |
| `flavors` | `ServiceFlavor` | `0..*` | **containment**, über welche Transports erreichbar (bei rein lokaler Bereitstellung leer) |
| `properties` | `Property` | `0..*` | **containment**, Implementations-spezifische Properties (Service-Ranking, Tenancy, …) |
| `componentDescription` | `ComponentDescription` | `0..1` | non-containment; gesetzt, wenn DS-getrieben |

---

## Flavor / Transport-Binding-Layer

Modelliert *wie* eine Implementation über das Netz erreichbar ist. Eine `ServiceImplementation` kann mehrere `ServiceFlavor`s gleichzeitig anbieten (z.B. derselbe Service per REST *und* per MQTT). Der Consumer wählt einen Flavor anhand seiner `ConsumerCapability` (siehe API-Layer).

### `ServiceFlavor` (abstract) `extends NamedElement`

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `kind` | `FlavorKind` | `1..1` | Discriminator (REST/MQTT/…); doppelt zu Subklasse aber praktisch für consumer-side filtering |
| `operationFlavors` | `ServiceOperationFlavor` | `0..*` | **containment**, transport-spezifische Bindung pro Operation |

### `RestFlavor` `extends ServiceFlavor`

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `host` | `EString` | `0..1` | | optional, kann zur Laufzeit in der Service-Reference stehen |
| `basePath` | `EString` | `1..1` | | gemeinsame URL-Präfix-Komponente aller Operationen |
| `contentTypes` | `EString` | `0..*` | | erlaubte/produzierte Content-Types als Default |

### `MqttFlavor` `extends ServiceFlavor`

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `brokers` | `EString` | `1..*` | | Broker-URLs (mqtt(s)://…) |
| `requestTopic` | `EString` | `1..1` | | Default-Topic für Requests; Operationen können überschreiben |
| `responseTopic` | `EString` | `0..1` | | Default-Topic für Responses |
| `defaultQos` | `MqttQos` | `1..1` | `AT_LEAST_ONCE` | |
| `defaultRetained` | `EBoolean` | `1..1` | `false` | |

### `ServiceOperationFlavor` (abstract) `extends NamedElement`

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `operation` | `ServiceOperation` | `1..1` | non-containment, auf welche Interface-Operation diese Bindung verweist |
| `consumes` | `EString` | `0..*` | Content-Types für Request-Body (überschreibt Flavor-Default) |
| `produces` | `EString` | `0..*` | Content-Types für Response-Body |

### `RestOperationFlavor` `extends ServiceOperationFlavor`

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `method` | `HttpMethod` | `1..1` | | |
| `path` | `EString` | `0..1` | | relativ zum `RestFlavor.basePath`; `null` = direkt auf `basePath` |
| `returnCodes` | `EInt` | `1..*` | | erwartete HTTP-Status-Codes für Erfolg (typisch z.B. `[200, 204]`) |

### `MqttOperationFlavor` `extends ServiceOperationFlavor`

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `requestTopic` | `EString` | `0..1` | | überschreibt Flavor-Default für diese Operation |
| `responseTopic` | `EString` | `0..1` | | s.o. |
| `qos` | `MqttQos` | `0..1` | | s.o. |
| `retained` | `EBoolean` | `0..1` | | s.o. |
| `correlation` | `EBoolean` | `1..1` | `true` | ob Request/Response per correlationId gematcht werden müssen |
| `returnPath` | `EString` | `0..1` | | optional, alternative Konvention für asynchrone Antworten |

---

## Runtime-Layer

### `ServiceReference` (≈ `ServiceReferenceDTO`)

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `id` | `EString` | `1..1` | UUID, `iD=true` |
| `properties` | `Property` | `0..*` | **containment** |
| `provider` | `ServiceProvider` | `1..1` | non-containment (statt `bundle: long`) |
| `usingProviders` | `ServiceProvider` | `0..*` | non-containment (statt `usingBundles: long[]`) |
| `registration` | `ServiceRegistration` | `0..1` | opposite zu `ServiceRegistration.reference` |

**EOperations**:
- `getProperty(key: EString) : EJavaObject` — Lookup einer Property nach Name
- `getPropertyKeys() : EString[*]` — Namen aller gesetzten Properties

### `ServiceRegistration` (Provider-Sicht)

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `reference` | `ServiceReference` | `1..1` | non-containment, opposite zu `ServiceReference.registration` *(Container ist `ServiceRegistry.references`)* |
| `unregistered` | `EBoolean` | `1..1` (default `false`) | |

**EOperations**:
- `unregister() : void`
- `setProperties(props: Property[*]) : void`

### `ComponentConfiguration` `extends NamedElement` (≈ `ComponentConfigurationDTO`)

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `id` | `EString` | `1..1` | `iD=true`, component.id |
| `description` | `ComponentDescription` | `1..1` | non-containment |
| `state` | `ComponentState` | `1..1` | |
| `properties` | `Property` | `0..*` | **containment** |
| `satisfiedReferences` | `SatisfiedReference` | `0..*` | **containment** |
| `unsatisfiedReferences` | `UnsatisfiedReference` | `0..*` | **containment** |
| `failure` | `Diagnostic` | `0..1` | **containment**, nur bei `state = FAILED_ACTIVATION` |
| `service` | `ServiceReference` | `0..1` | non-containment, das registrierte Service-Reference (falls Component Service registriert) |

### `SatisfiedReference`

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `name` | `EString` | `1..1` | Name der `ComponentReference` |
| `target` | `EString` | `0..1` | |
| `boundServices` | `ServiceReference` | `0..*` | non-containment |

### `UnsatisfiedReference`

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `name` | `EString` | `1..1` | Name der `ComponentReference` |
| `target` | `EString` | `0..1` | |
| `targetServices` | `ServiceReference` | `0..*` | non-containment |

### `Diagnostic` (sprachneutral nachgebaut, Struktur wie `org.eclipse.emf.common.util.Diagnostic`)

| Feature | Typ | Bounds | Default | Notes |
|---|---|---|---|---|
| `severity` | `DiagnosticSeverity` | `1..1` | `OK` | |
| `message` | `EString` | `0..1` | | |
| `source` | `EString` | `0..1` | | z.B. `"org.gecko.ddsr.runtime"` |
| `code` | `EInt` | `1..1` | `0` | |
| `data` | `EString` | `0..*` | | stringifizierte Kontextdaten (sprachneutral; Java-Object-Liste vermieden) |
| `children` | `Diagnostic` | `0..*` | | **containment**, rekursive Ursachenkette |

---

## Event-Layer

### `ServiceEvent`

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `type` | `ServiceEventType` | `1..1` | |
| `reference` | `ServiceReference` | `1..1` | non-containment |
| `timestamp` | `EDate` | `0..1` | |

### `ServiceListener` (abstract, interface)

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `filter` | `EString` | `0..1` | LDAP-Filter, optional |

**EOperations**:
- `serviceChanged(event: ServiceEvent) : void`

---

## API-Layer

Die Registry-Hierarchie ist jetzt zweistufig: eine abstract `ServiceRegistry`-Basis, plus zwei konkrete Subklassen `LocalServiceRegistry` (In-Process) und `RemoteServiceRegistry` (Broker). Die Aufteilung ist explizit, weil lokale und Remote Registry strukturell und semantisch unterschiedliche State-Bestandteile halten.

### `ServiceRegistry` (abstract) `extends NamedElement`

Gemeinsame Basis. Hält *nur* das, was beide Registry-Arten gemeinsam haben.

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `kind` | `RegistryKind` | `1..1` | Discriminator zusätzlich zur Subklasse — hilft Filtern in Listen heterogener Registries |

**Abstrakte EOperations** (von Subklassen implementiert):

| Operation | Parameter | Return |
|---|---|---|
| `getServiceReference` | `interfaceName: EString` | `ServiceReference` |
| `getServiceReferences` | `interfaceName: EString, filter: EString, capability: ConsumerCapability` | `ServiceReference[*]` |
| `getAllServiceReferences` | `interfaceName: EString, filter: EString, capability: ConsumerCapability` | `ServiceReference[*]` |
| `addServiceListener` | `listener: ServiceListener` | `void` |
| `removeServiceListener` | `listener: ServiceListener` | `void` |

Anmerkung: `capability` ist auf `getServiceReferences`/`getAllServiceReferences` Pflicht-Parameter, weil das Flavor-Matching daran hängt. Bei In-Process-Lookup (LocalServiceRegistry) wird sie meist `null`/leer sein — dann werden alle Flavors als verfügbar betrachtet.

### `LocalServiceRegistry` `extends ServiceRegistry`

Das, was bisher `ServiceRegistry` war — die OSGi-`ServiceRegistry`-artige In-Process-Komponente.

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `references` | `ServiceReference` | `0..*` | **containment**, alle lokal registrierten Services |
| `registrations` | `ServiceRegistration` | `0..*` | **containment**, Provider-Sicht zu den Services in `references` |
| `configurations` | `ComponentConfiguration` | `0..*` | **containment** |
| `providers` | `ServiceProvider` | `0..*` | **containment** |
| `listeners` | `ServiceListener` | `0..*` | non-containment |
| `remote` | `RemoteServiceRegistry` | `0..1` | non-containment, die Remote-Registry, an die delegiert wird (falls verbunden) |
| `connectionState` | `ConnectionState` | `1..1` (default `OFFLINE`) | Health der Verbindung zur Remote Registry. In DEGRADED/OFFLINE: Writes ablehnen, Reads aus letztem bekannten State. Reconnect synthetisiert ServiceEvents für den Diff. |

**Zusätzliche EOperations** (über die abstrakte Basis hinaus):

| Operation | Parameter | Return |
|---|---|---|
| `registerService` | `provider: ServiceProvider, implementation: ServiceImplementation, props: Property[*]` | `ServiceRegistration` |
| `fireServiceEvent` | `event: ServiceEvent` | `void` |

Verhalten von `registerService`: lokale Eintragung *synchron*, lokale Listener *synchron*, Remote-Propagation *asynchron* (FR-Dist-Framework-Owns-Comms). Genaue Schrittreihenfolge wird in §5 Lifecycle der REQUIREMENTS fixiert.

### `RemoteServiceRegistry` `extends ServiceRegistry`

Der zentrale Broker. Hält den **API-Katalog** und den **Implementations-Index**.

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `endpoint` | `EString` | `0..1` | URL/Adresse des Broker-Services aus Sicht des Clients (z.B. Cluster-VIP). Auf der Server-Seite kann leer sein. |
| `catalog` | `ServiceInterface` | `0..*` | **containment**, der API-Katalog: alle in diesem System erlaubten Service-Interfaces (mit ihren Operations + Constraints + Exceptions) |
| `implementations` | `ServiceImplementation` | `0..*` | non-containment, alle gemeldeten Implementations aller Provider |
| `providers` | `ServiceProvider` | `0..*` | non-containment, Provider, die sich beim Broker registriert haben |

**Zusätzliche EOperations**:

| Operation | Parameter | Return |
|---|---|---|
| `publishImplementation` | `provider: ServiceProvider, implementation: ServiceImplementation` | `Diagnostic` *(success/failure; WARNING wenn referenziertes Interface DEPRECATED ist, OK sonst)* |
| `withdrawImplementation` | `provider: ServiceProvider, implementation: ServiceImplementation` | `Diagnostic` — symmetrisch zu `publishImplementation`; Provider explizit, damit Ownership-Check (Implementation MUSS zum Provider gehören) und PublishHook-Authorization eindeutig sind |
| `deprecateCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` — Soft-Marker: setzt `status = DEPRECATED`, existing Implementationen bleiben, Lookups liefern weiter. Einseitig. |
| `removeCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` — Strict-Reject: ERROR mit code `CATALOG_HAS_LIVE_IMPLS`, solange Implementationen referenzieren. Kein Auto-Cascade. |
| `addCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` — flowed through PDP/PEP, gates on Governance officer authorization |
| `deprecateCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` |
| `removeCatalogEntry` | `serviceInterface: ServiceInterface, requestor: EString` | `Diagnostic` |

### `ConsumerCapability`

Was ein Consumer beim Lookup mitschickt, damit der Registry-seitig Flavor-Matching möglich ist. **Nicht persistent** — pro Lookup übergeben.

| Feature | Typ | Bounds | Notes |
|---|---|---|---|
| `consumerId` | `EString` | `0..1` | optional, für Auditing / Policy-Entscheidungen |
| `supportedFlavors` | `FlavorKind` | `1..*` | mindestens ein Flavor — sonst kann der Consumer gar nichts konsumieren |
| `properties` | `Property` | `0..*` | **containment**, zusätzliche Capability-Hinweise (z.B. unterstützte Content-Types, Encoding-Präferenzen) |

---

## Interception-Hooks

DDSR enthält **keine eigene Policy-/PDP-Semantik**. Stattdessen drei Hook-Interfaces, an denen ein Integrator beliebige externe Logik (PDP, IAM, Audit, Property-Mutation, Tenant-Filtering, …) anschließen kann. Der Prototyp shipt mit *keinen* Hook-Implementationen, läuft also Hook-frei (alle Aktionen erlaubt).

**Hook-Vertrag**: jede Hook-Methode gibt eine `Diagnostic` zurück. Severity `OK` / `INFO` / `WARNING` → fortfahren; `ERROR` / `CANCEL` → abbrechen, diese Diagnostic an den Caller zurückgeben. Mehrere Hooks bilden eine Pipeline — jeder muss OK liefern, sonst Abbruch.

Die drei Hook-Listen werden non-containment an `ServiceRegistry` (also vererbt nach `LocalServiceRegistry` und `RemoteServiceRegistry`) gehängt:

- `publishHooks : PublishHook [0..*]`
- `discoveryHooks : DiscoveryHook [0..*]`
- `distributionHooks : DistributionHook [0..*]`

### `PublishHook` (abstract, interface)

Provider-Seite. Wird vor `publishImplementation` / `withdrawImplementation` konsultiert.

**EOperations**:
- `onPublish(provider: ServiceProvider, implementation: ServiceImplementation) : Diagnostic`
- `onWithdraw(provider: ServiceProvider, implementation: ServiceImplementation) : Diagnostic`

### `DiscoveryHook` (abstract, interface)

Consumer-Seite. Wird vor Lookup und Subscription konsultiert, plus Ergebnis-Filterung.

**EOperations**:
- `onLookup(interfaceName: EString, filter: EString, capability: ConsumerCapability) : Diagnostic`
- `filterReferences(capability: ConsumerCapability, references: ServiceReference[*]) : ServiceReference[*]`
- `onSubscribe(listener: ServiceListener) : Diagnostic`

### `DistributionHook` (abstract, interface)

Federation-Grenze. Wird bei Outbound (Local → Broker) und Inbound (Broker → Local) Events konsultiert.

**EOperations**:
- `onOutbound(event: ServiceEvent) : Diagnostic`
- `onInbound(event: ServiceEvent) : Diagnostic`

### Verhältnis zu XACML / OPA

Integratoren, die ein externes XACML-System anschließen wollen, schreiben einen Adapter-Hook: in `onPublish` baut der Adapter einen XACML-Request, schickt ihn an den PDP, und mappt die Antwort auf `Diagnostic.severity`. Identisch für OPA, IAM-Tokens, hauseigene Policy-Engines. DDSR muss nichts von dem konkreten Policy-Format wissen — der Hook ist der Übergangspunkt.

---

## Bewusst NICHT im ersten Wurf

- **`ServiceComponentRuntime`** als eigene EClass — wird nachgezogen, falls DS-Lifecycle-Operations (`enableComponent`, `disableComponent`, `getComponentDescriptions(provider)`, `getComponentConfigurations(description)`) gebraucht werden.
- **`PrototypeServiceFactory` / `ServiceObjects`** — Prototyp-Scope wird vorerst nur als `ServiceScope`-Enum-Wert erfasst, ohne eigene Erzeugungs-API.
- **`Bundle`-Lifecycle** (`STARTING`/`ACTIVE`/`STOPPING`/…) — DDSR modelliert Services, nicht Deployment-Einheiten.
- **`Filter` als EClass** — wird vorerst als plain `EString` (LDAP-Filter-Syntax) gespeichert. Parser/Evaluator ist Sache der Implementierung.
- **Konkrete Policy-Engine-Implementationen** (XACML-Parser, OPA-Bindings, …). Das Modell liefert die drei Interception-Hooks (`PublishHook`, `DiscoveryHook`, `DistributionHook`); Authorization-Logik kommt von extern als Hook-Adapter.
- **Weitere Flavors über REST/MQTT hinaus** (gRPC, WebSocket, Kafka, AMQP, …). Werden erst gebraucht, wenn ein konkreter Bedarf besteht; das Flavor-Plugin-Konzept ist genau dafür gemacht.
- **Konkrete Flavor-Client/-Server-Implementations**. Die `RestFlavor`/`MqttFlavor`-Klassen sind reine Beschreibungen — die HTTP- bzw. MQTT-Bibliotheken, die das tatsächlich umsetzen, sind außerhalb des Ecore-Modells.

---

## Offene TODOs nach erstem Modellanlegen

### OCL-Constraints

Die OCL-Engine ist **Fennec M2X OCL** unter `/opt/git/m2m/workspace`. Doku in `docs/ocl-user-guide.md` und `docs/ocl-architecture.md`. Annotation-Namespace ist `http://www.eclipse.org/fennec/m2x/ocl/1.0`, Detail-Keys sind die Constraint-Namen (für invariants) bzw. die Reservierten `derive`, `initial`, `body`, `pre`, `post` für Setting-/Invocation-Delegates.

**Bereits ausführbar als M2X-OCL im ddsr.ecore:**

| Class | Invariant-Name | Bedingung |
|---|---|---|
| `VersionedElement` | `validSemver` | `version = null or version.matches('^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$')` |
| `NumericRangeConstraint` | `atLeastOneBound` | `min ≠ null or max ≠ null` |
| `NumericRangeConstraint` | `rangeOrdered` | `min = null or max = null or min ≤ max` |
| `StringPatternConstraint` | `lengthBoundsNonNegative` | beide Length-Bounds ≥ 0 wenn gesetzt |
| `StringPatternConstraint` | `lengthBoundsOrdered` | `minLength ≤ maxLength` wenn beide gesetzt |
| `CollectionSizeConstraint` | `sizeBoundsNonNegative` | beide Size-Bounds ≥ 0 wenn gesetzt |
| `CollectionSizeConstraint` | `sizeBoundsOrdered` | `minSize ≤ maxSize` wenn beide gesetzt |
| `ComponentConfiguration` | `failureOnlyWhenFailed` | `failure ≠ null` ⇔ `state = FAILED_ACTIVATION` |
| `ServiceRegistration` | `unregisteredNotInRegistry` | wenn `unregistered`, dann in keiner `LocalServiceRegistry.registrations` enthalten |
| `ServiceImplementation` | `atLeastOneInterface` | `serviceInterfaces->notEmpty()` |
| `ServiceImplementation` | `operationFlavorsCoverInterfaces` | jeder `ServiceOperationFlavor.operation` gehört zu einem der `serviceInterfaces` |
| `RemoteServiceRegistry` | `publishedImplsHaveFlavor` | jede publizierte Implementation hat ≥1 Flavor |
| `RemoteServiceRegistry` | `publishedImplsReferenceCatalog` | jede `serviceInterfaces`-Referenz einer publizierten Impl existiert im `catalog` |
| `RemoteServiceRegistry` | `publishedImplsOwnedByListedProvider` | jede publizierte Impl ist Containment-mäßig in einem der `providers` |

**StringPatternConstraint Regex-Syntax-Validität**: in OCL nicht statisch entscheidbar — wird vom Engine-Aufrufer geprüft (z.B. Java `Pattern.compile`, TS `new RegExp`, Python `re.compile`).

**Withdraw-Ownership** (`withdrawImplementation(provider, implementation)`): `implementation.eContainer() = provider`. Aktuell nicht als statischer OCL-Invariant ausgedrückt, weil sie auf einem Operations-*Parameter-Paar* zu Laufzeit gilt, nicht auf einem persistenten Modell-State — gehört in den `pre:`-Body der Operation. **TODO** (sobald wir Setting/Invocation-Delegates aktivieren):

```
pre ownershipCheck: implementation.eContainer() = provider
```

## ExpressionConstraint und Invariant in der Praxis

Wer reine OCL-Annotations am Ecore-EModelElement verwenden will (z.B. eine Bedingung am Modell, nicht am Modell-*Inhalt*), nutzt die M2X-Detail-Keys direkt — `body`, `pre`, `post`, `derive`, `initial`, Constraint-Name. Wer eine Constraint **als Modell-Inhalt** ausdrücken muss (typischer Fall: der Governance-Officer schreibt im API-Katalog `amount > 0` ohne den Ecore zu kennen), benutzt `ExpressionConstraint` / `Invariant`.

Beispiele:

```ocl
-- Parameter.constraints (an einem 'amount: int' Parameter)
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

Die DDSR-Engines (Java, TS) müssen für jede unterstützte `ExpressionLanguage` einen Evaluator mitbringen. Im Prototyp ist das nur **OCL** — auf Java-Seite läuft das durch Fennec M2X, auf TS-Seite braucht es eine OCL-Implementation; falls nicht verfügbar, ist die Alternative, einen OCL-zu-TS-Compiler im Code Publisher zu bauen, der `ExpressionConstraint.expression` in TypeScript-Code übersetzt. (Diese Entscheidung ist nicht jetzt nötig — Modell-Surface ist offen.)

### Modell-Erweiterungs-TODOs (sobald REQUIREMENTS Open Questions geklärt sind)

- **Q3 (Reverse-Engineering-Quelle):** ggf. eigene EAnnotation-Source `"http://geckoprojects.org/ddsr/reveng/1.0"` mit Marker-Details (z.B. „extracted from Java annotation X").
- **Q4 (PDP-Contract):** `PolicyRequest`/`PolicyDecision`-Felder verfeinern (heute Stub).
- **Q8 (Semver-Kompatibilitätsregeln):** ggf. EAnnotations an `ServiceOperation` / `Parameter` / `ParameterConstraint`, die Versions-Impact-Klassifizierung tragen (z.B. `breakingChange="true"` als Hinweis für den Code Publisher).

### Sonstiges
- GenModel: `oSGiCompatible = true`, `basePackage = org.gecko.ddsr.model`, `resource = XMI` (steht schon im aktuellen Ecore).
- Beispielinstanzen `*.xmi` für je einen typischen Fall:
  - API-Katalog-Snapshot (`RemoteServiceRegistry` mit zwei, drei `ServiceInterface`s mit Operations + Constraints)
  - `ServiceImplementation` mit `RestFlavor` *und* `MqttFlavor` für dasselbe Interface
  - `LocalServiceRegistry` mit Registration + Reference + Configuration
  - `ConsumerCapability`-Beispiele für REST-only und REST+MQTT-Consumer
  - Bestehende `ServiceProvider.xmi`, `ServiceRegistry.xmi`, `PaypalPaymentImpl.xmi` als Migrations-Vorlagen.
