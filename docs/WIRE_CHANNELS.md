# DDSR — Wire Channels, Interaction Models, Capability/Requirement Matching

**Status:** Design-Diskussion. Modell-Skizze steht, mehrere offene Designfragen markiert. Implementierung als parallele v2 im Workspace (siehe §9).
**Letzte Aktualisierung:** 2026-07-02.

Spezifiziert das Wire-Channel-Modell, Interaktions-Styles (Request/Response, Streaming, Bidirectional), den `OperationChannel`-Begriff inkl. Korrelations-Strategien, und das generelle Capability/Requirement-Matching zwischen Provider und Consumer. Komplement zu [UPDATE_POLICY.md](UPDATE_POLICY.md) (die nutzt die Stream-Termination-Mechanik aus dieser Doc).

---

## 1. Motivation

Heutiger Stand (siehe [ARCHITECTURE.md §2.5](ARCHITECTURE.md)):

- Eine Operation hat einen `RestOperationFlavor`, der HTTP-Methode, Pfad und Content-Type bündelt.
- Implizite Annahme überall: **Request → Response, sync, eine Round-Trip, gleicher Kanal.**
- Operations mit `Cardinality 0..*` heißen "Liste als Return-Wert", nicht "Stream über Zeit". Das Modell kann eine endliche Liste und einen unendlichen Stream nicht unterscheiden.

Drei Designziele für die nächste Iteration:

1. **Interaktions-Styles als erste Klasse:** Request/Response, Fire-and-Forget, Event-Stream, Bidirectional-Stream als deklarative Wahl auf der Operation.
2. **Channels als gleichberechtigte Wire-Konstrukte:** Request-Kanal und Response-Kanal müssen nicht derselbe Flavor sein. Async-RPC (Request via REST, Response via Topic) wird konzeptionell sauber.
3. **Capability/Requirement-Matching** als generelles Provider↔Consumer-Resolution-System, statt `supportedFlavors` als Spezialfall.

Daraus folgt eine substantielle Modell-Erweiterung. Sie wird parallel zum bestehenden v1-Modell entwickelt (§9), nicht als Mega-Refactor.

## 2. Channel-Modell — neue Klassen und Strukturen

Skizze (vorläufiges Naming, kein Ecore-Code):

```
ServiceImplementation
├── publishedVia       : Flavor           (1; wie hat sich die Impl beim Broker registriert)
├── discoverableVia    : Flavor[*]        (über welche Channels macht der Broker sie auffindbar — meist = Broker-Default)
├── capabilities       : Capability[*]    (siehe §6)
└── operationFlavors   : OperationFlavor[*]

OperationFlavor
├── operation          : ServiceOperation (Crossref in den Catalog)
├── interactionStyle   : InteractionStyle (deklarativer Hint, siehe §3)
├── requestChannel     : OperationChannel?   (nil bei pure Subscribe-only Streams)
├── responseChannel    : OperationChannel?   (nil bei FIRE_AND_FORGET)
└── streamOptions      : StreamOptions?      (gesetzt wenn einer der Channels Stream-Typ ist)

OperationChannel
├── flavor             : Flavor              (RestFlavor, MqttFlavor, GrpcFlavor, …)
├── correlationStrategy: CorrelationStrategy (siehe §5)
└── timeout            : Duration?           (optional, default flavor-spezifisch)

StreamOptions
├── backpressure       : BackPressureStrategy
├── bufferSize         : int
├── delivery           : DeliveryGuarantee
└── keepAliveInterval  : Duration

InteractionStyle : enum { REQUEST_RESPONSE, FIRE_AND_FORGET, EVENT_STREAM, COMMAND_STREAM, BIDIRECTIONAL_STREAM }
CorrelationStrategy : enum { SYNCHRONOUS, HEADER_BASED, TOPIC_BASED, NONE }
BackPressureStrategy : enum { NONE, DROP_OLDEST, DROP_NEWEST, BLOCK }
DeliveryGuarantee : enum { AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE }
```

`Flavor` wird selbst zur abstrakten Basis-Klasse, von der `RestFlavor`, `MqttFlavor`, `GrpcFlavor` etc. erben (heute ist `RestFlavor` schon eine konkrete Subklasse; das passt). Jede Flavor-Subklasse trägt ihre transport-spezifischen Properties (Host, BasePath für REST; Broker, Topic für MQTT; etc.) und deklariert ihre transport-fähigen InteractionStyles als Capability (siehe §6, §7).

`publishedVia`/`discoverableVia` und die `operationFlavors`/`OperationChannel`-Struktur können nicht nur handgeschrieben, sondern auch **aus einem importierten OpenAPI-/AsyncAPI-Contract abgeleitet** werden — das Mapping steht in §12.

## 3. InteractionStyle — deklarativer Hint, nicht primäre Achse

Mit der Channel-Verallgemeinerung ergibt sich der "Style" einer Operation strikt genommen aus der Channel-Konfiguration:

- Beide Channels da, Single-Roundtrip, sync Correlation → `REQUEST_RESPONSE`
- Nur Request-Channel → `FIRE_AND_FORGET`
- Nur Response-Channel (lang) → `EVENT_STREAM`
- Beide Channels, beide lang → `BIDIRECTIONAL_STREAM`

`InteractionStyle` als Modell-Property ist deshalb **redundant zur Kanal-Konfiguration**, wird aber **trotzdem behalten** als:

- **Lesbarkeit** — Autor sagt was er meint; Generator/Reader müssen die Channel-Konfiguration nicht decodieren um den Intent zu sehen.
- **Validierbarkeit** — Modell-Validator kann Inkonsistenzen finden ("Style sagt `FIRE_AND_FORGET`, aber `responseChannel != null` → Modellfehler").
- **Migration** — heutige Modelle (alle implizit `REQUEST_RESPONSE`) kriegen einen sauberen Anker, ohne dass eine Migration die Channel-Struktur ändern muss.

## 4. Symmetrische vs. asymmetrische Channels

**Symmetric (Default-Shorthand):** `requestChannel` und `responseChannel` referenzieren **dasselbe `OperationChannel`-Objekt**. Im XMI ist das ein cross-doc-href auf dieselbe Stelle. Das deckt ~80% der heutigen Operations ab (sync REST).

**Asymmetric (Split-Channel):** zwei verschiedene `OperationChannel`-Objekte, eventuell unterschiedliche Flavors. Notwendig bei:

- Async-RPC: Request über REST POST `/start-job`, Response über MQTT-Topic oder SSE-Stream (Webhook-Pattern, CompletableFuture-Style).
- Streaming-Patterns: Request einmaliges Subscribe via REST, Response ist offener Event-Stream über separaten Channel.
- Cross-Transport-Pattern: Consumer sitzt hinter Firewall, Request geht raus über HTTP, Response kommt über persistentes MQTT.

**Shorthand-Regel im Modell:** wenn `responseChannel` nicht gesetzt ist und `interactionStyle == REQUEST_RESPONSE`, default es auf dasselbe Objekt wie `requestChannel`. Wenn beide unterschiedlich sind, wird Correlation-Strategie zur Pflicht (siehe §5).

## 5. Correlation Strategies

Bei Split-Channel muss der Producer wissen, **welcher Response zu welchem Request** gehört. Vier Strategien:

| Strategie | Mechanismus | Beispiel-Flavor |
|---|---|---|
| `SYNCHRONOUS` | Antwort kommt in derselben Connection / im gleichen RPC-Frame zurück. Keine ID nötig. | REST GET/POST, gRPC unary |
| `HEADER_BASED` | UUID/Correlation-ID im Request-Header oder Message-Property, Producer kopiert sie in die Response. | MQTT message property `correlation-data`, JMS `JMSCorrelationID`, AMQP RPC |
| `TOPIC_BASED` | Consumer öffnet einen Response-Topic, hängt dessen Identität an den Request (`reply-to`). Producer published auf den Topic. | MQTT request/response pattern mit `response-topic` |
| `NONE` | Keine Korrelation; gilt nur für `FIRE_AND_FORGET` und manche Streams (wo die Identität implicit in der Subscription liegt). | Pure event publish |

Die Strategie ist Property von `OperationChannel` (genauer: vom Response-Channel, weil dort die "wo kommt sie an"-Logik sitzt). Die Consumer-Facade implementiert das Korrelations-Routing transparent — der Application-Code sieht nur seinen `Future<T>` oder `Flux<T>`, egal ob der Wire-Pfad sync, async-header oder topic-based ist.

**Implementations-Konsequenz:** der heutige `RestServiceInvoker` ist eine `SYNCHRONOUS`-Implementierung. Für `HEADER_BASED` und `TOPIC_BASED` braucht es einen `AsyncInvoker` mit pending-call-Map (`correlation-id → CompletableFuture<Response>`), Timeout-Cleanup, und einer Subscription auf den Response-Channel.

## 6. Capability/Requirement-Matching

Anstatt `supportedFlavors` (heute) als Spezial-Filter zu führen, **modelliert DDSR ein generelles Capability/Requirement-System** analog zu OSGi `Provide-Capability` / `Require-Capability`. Provider und Consumer deklarieren beide Listen, der Broker resolked.

### 6.1 Grundform

```
Capability
├── namespace : String          (z.B. "ddsr.transport", "ddsr.contentType", "ddsr.version")
└── attributes : Map<String, String>

Requirement
├── namespace : String          (matcht gegen Capability.namespace)
└── filter    : LdapFilter      (OSGi-Style LDAP-Filter über attributes)
```

**Beispiele:**

Provider deklariert auf `ServiceImplementation.capabilities`:

```
namespace = "ddsr.transport",      attributes = { version="mqtt-v5", qos="2" }
namespace = "ddsr.contentType",    attributes = { type="application/json+cloudevents" }
namespace = "ddsr.interaction",    attributes = { style="EVENT_STREAM,BIDIRECTIONAL_STREAM" }
```

Consumer deklariert auf `ConsumerCapability.requirements`:

```
namespace = "ddsr.transport",      filter = "(version=mqtt-*)"
namespace = "ddsr.contentType",    filter = "(type=application/*)"
namespace = "ddsr.interaction",    filter = "(style=EVENT_STREAM)"
```

Broker resolked beim Lookup: nur Implementations zurückgeben, deren `capabilities` alle Consumer-`requirements` erfüllen.

### 6.2 Was über Capabilities ausgedrückt wird

- **Transport-Versionen:** `mqtt-v3.1`, `mqtt-v5`, `http/1.1`, `http/2`, `grpc-v1.x`
- **Content-Types:** `application/json`, `application/cloudevents+json`, `application/xml`, etc. Mit Wildcards im Requirement.
- **Codecs:** XMI, JSON, ProtoBuf, Avro
- **Quality-of-Service-Garantien:** at-most-once vs at-least-once etc. (überlappt mit `StreamOptions.delivery`, aber dort ist es eine *Bitte*, hier eine *Garantie*)
- **Security-Modi:** `tls-v1.3`, `mtls`, etc.
- **Compression:** `gzip`, `zstd`
- **Provider-Identität / Region / Tenant** — alles was heute hand-gewuselt über Service-Properties läuft

### 6.3 Verhältnis zum heutigen `supportedFlavors`

Das heutige `ConsumerCapability.supportedFlavors = [REST]` wird zu einem Requirement:

```
namespace = "ddsr.transport", filter = "(family=rest)"
```

`supportedFlavors` ist damit der **primitive Vorgänger** des Capability-Systems. Nach Migration zu v2 (siehe §9) ist es weg.

### 6.4 Verhältnis zum OSGi-Vorbild

Bewusst angelehnt an OSGi:

- Namespace-Konzept ist 1:1.
- LDAP-Filter-Syntax ist 1:1 (`(&(attr=val)(other<=5))`).
- Resolution-Semantik ist 1:1: alle Requirements müssen erfüllt sein, Wildcards im Filter werden gegen attribute-Werte gematcht.

Was anders ist:

- Es gibt kein Versions-Range-Hopping wie bei OSGi-Imports — Service-Versionen werden in `UPDATE_POLICY.md` separat geregelt.
- Resolution passiert zur Lookup-Zeit auf dem Broker, nicht zur Bundle-Wire-Zeit auf dem Resolver.

## 7. Cross-Achse-Constraints: welcher Flavor kann welchen Style?

Nicht jeder Flavor kann jeden InteractionStyle transportieren. Das wird über Flavor-Capabilities deklariert:

```
RestFlavor capabilities:
  ddsr.interaction.styles : { REQUEST_RESPONSE, FIRE_AND_FORGET, EVENT_STREAM (via SSE) }

MqttFlavor capabilities:
  ddsr.interaction.styles : { ALL }
  ddsr.correlation.strategies : { TOPIC_BASED, HEADER_BASED, NONE }

GrpcFlavor capabilities:
  ddsr.interaction.styles : { REQUEST_RESPONSE, EVENT_STREAM, COMMAND_STREAM, BIDIRECTIONAL_STREAM }
  ddsr.correlation.strategies : { SYNCHRONOUS, HEADER_BASED (via metadata) }
```

Modell-Validator prüft beim Publish: wenn `OperationFlavor.interactionStyle == BIDIRECTIONAL_STREAM` und `requestChannel.flavor` eine Subklasse von `RestFlavor` ist → Fehler (REST kann das nicht). Bei Publish-Hook (`PublishHook` aus REQUIREMENTS.md) ist das der natürliche Andockpunkt.

## 8. Stream-Termination — Wire-Protocol für graceful Close

Sowohl `UPDATE_POLICY.md` (HARD_CUTOVER bei Streams) als auch reguläre Service-Beendigung brauchen einen sauberen Mechanismus, einen offenen Stream zu schließen.

**Anforderungen:**

- Beide Seiten (Consumer und Provider) müssen unabhängig den Stream beenden können.
- Reason-Code muss übertragbar sein (für UI / Retry-Logik / Logging).
- Wer zuerst sendet, der andere muss antworten / aufräumen.

**Protokoll:**

```
StreamCloseFrame
├── reason : CloseReason
└── message : String?    (free-text Detail, optional)

CloseReason : enum {
  CONSUMER_LEFT,        // Consumer-seitig initiiert (regulärer Unsubscribe)
  PROVIDER_RETIRED,     // Provider-seitig initiiert (Service-Shutdown)
  BROKER_CUTOVER,       // Broker hat UNREGISTERING geschickt (Update-Policy)
  IDLE_TIMEOUT,         // keine Aktivität länger als keepAliveInterval
  PROTOCOL_VIOLATION,   // andere Seite hat Wire-Format verletzt
  INTERNAL_ERROR        // Catch-all
}
```

**Flow:**

1. Eine Seite (z.B. Provider) entscheidet: Stream beenden. Sendet `StreamCloseFrame(reason=PROVIDER_RETIRED)` auf dem Stream.
2. Die andere Seite (Consumer) bekommt den Frame, ruft seinen Application-Code an (`onStreamClose(reason)`), und sendet sein eigenes `StreamCloseFrame(reason=CONSUMER_LEFT, ackOf=…)`.
3. Beide Seiten geben Ressourcen frei (Buffer, Subscriptions, Heartbeat-Timer).

Per Flavor-spezifischer Mapping:

- **MQTT:** `StreamCloseFrame` als eigenes Topic-Suffix (`/control/close`) oder als spezielles Message-Property.
- **gRPC:** native Half-Close / Status-Code, mapped auf `CloseReason`.
- **WebSocket:** native Close-Frame mit Status-Code, das Wire-Format kapselt `CloseReason` darin.
- **REST+SSE:** Server sendet `event: close\ndata: {...}` und schließt die SSE-Verbindung. Consumer kann via separatem POST seinerseits abmelden.

## 9. Implementations-Strategie — Parallel-Workspace, kein Mega-Refactor

Das oben skizzierte Modell ist substantiell größer als heute. Statt das bestehende v1-Modell und den Broker zu refactoren, wird das neue Modell als **paralleler Workspace** aufgebaut:

```
org.eclipse.fennec.services.model            (v1, bleibt)
org.eclipse.fennec.services.broker.core      (v1, bleibt)
…
org.eclipse.fennec.services.model.v2         (neu — komplettes Ecore inkl. Channel-Model)
org.eclipse.fennec.services.broker.core.v2   (neu — Broker mit v2-Modell, Capability-Resolver, Stream-Support)
```

Beide Versionen laufen parallel im selben BND-Workspace. v2-Bundles können v1-Wire-Format lesen (für Migration / Mixed-Setup), aber publizieren in v2-Form. Cross-Checks gegen v1 sind über die Demo möglich: derselbe `payment.charge(10.0, "EUR")` muss gegen v1-Broker und v2-Broker dasselbe Ergebnis liefern.

Nach Stabilisierung von v2: v1-Bundles werden retired, der Workspace-Default-Build hängt nur noch auf v2. Modell-Code-Generierung wird über das v2-`.genmodel` umgestellt.

**Was im v2-Modell direkt mit reingebaut wird (kein zweiter Sprung):**

- Channel-Model aus §2
- `InteractionStyle`, `CorrelationStrategy`, `BackPressureStrategy`, `DeliveryGuarantee` Enums
- `Capability` / `Requirement` mit LDAP-Filter
- `updatePolicy`, `replaces`, `deprecated`, `cutoverGraceMillis` aus [UPDATE_POLICY.md](UPDATE_POLICY.md)
- `Consumer` mit `activeRefs` + Heartbeat-Felder
- `StreamCloseFrame` als Wire-Konstrukt (separates Ecore? oder Klasse im Modell?)

## 10. Use-Case-Katalog — `TODO`

Bevor wir die Stream-Pfade konkret implementieren, sammelt der Team-Use-Case-Workshop hier konkrete Beispiele. Diese Liste ist **bewusst leer** und wird beim nächsten Treffen gefüllt — die Architektur-Entscheidungen unten hängen daran.

```
// TODO: Use Case 1 — ein realer Stream-Service (Beispiel: Live-Telemetrie?
//        Order-Status-Updates? Catalog-Subscription für Auto-Re-Bind?)
// TODO: Use Case 2 — Async-Long-Running mit Webhook-Response
// TODO: Use Case 3 — Bidirectional: Consumer steuert Producer-State,
//        empfängt parallel Event-Stream
// TODO: Use Case 4 — Pure-Event-Publish (Producer-only, kein Control-Channel)
```

**Architektur-Entscheidungen, die erst mit Use Cases entschieden werden:**

- **Resource-Lifecycle bei Streams:** wer schließt eine Stream-Subscription, wenn der zugehörige Provider während des Streams einen `HARD_CUTOVER` macht? Der Broker triggert das `UNREGISTERING`; aber wer ist verantwortlich, dem neuen Provider den State zu übergeben (z.B. last-event-id für resumable streams)? Ohne konkreten Use-Case bleibt das offen.
- **State-Recovery nach Reconnect:** bei kurzem Netzwerk-Aussetzer auf einem Stream — Consumer reconnected, will er den ganzen Stream nochmal von Anfang an, ab letztem Event, oder ab Reconnect-Zeitpunkt? Ist Use-Case-abhängig.
- **Multi-Subscriber-Semantik:** ein Event-Stream-Service hat mehrere Subscriber. Bekommen alle dieselben Events (Fanout), oder werden Events partitioniert (Worker-Pattern)? Im Modell sicher beides darstellbar, aber Default-Wahl will gut überlegt sein.

## 11. Offene Designfragen

- **Default-`CorrelationStrategy`:** `SYNCHRONOUS` für REST, `HEADER_BASED` für MQTT? Pro Flavor-Subklasse default-setzen oder explicit-required?
- **Wer wählt zwischen mehreren `discoverableVia`-Optionen?** Wenn ein Provider sowohl via REST-Catalog als auch via MQTT-Discovery findbar ist, und der Consumer beide kann — wer entscheidet? Bauchgefühl: Consumer hat eine `preferredDiscovery`-Ordnung, Broker respektiert sie. Aber: das verschiebt Resolution-Logik in den Consumer, was bisher nicht so war.
- **Capability-Namespace-Konventionen:** ein registry-style Dokument für offizielle Namespaces (`ddsr.transport`, `ddsr.contentType`, …) — eigene Doc oder hier inline?
- **Resolution-Performance:** LDAP-Filter-Evaluation pro Lookup ist OK für überschaubare Catalogs, aber bei 1000+ Implementations brauchen wir Index-Strukturen über die Capability-Namespaces. Stufe-4-Optimierung.
- **Flavor-Subklassen vs. Generischer Flavor mit Capabilities:** statt `RestFlavor extends Flavor` und `MqttFlavor extends Flavor` als hartcodierte Subklassen könnte man auch *einen* generischen `Flavor` mit Capabilities deklarieren (`ddsr.transport.family=rest`). Tradeoff: extensibler vs. weniger Typsicherheit. Heute haben wir Subklassen — sollen wir bleiben?
- **`OperationChannel`-Identität:** beim Symmetric-Default sind `requestChannel` und `responseChannel` dasselbe Objekt im XMI (per Cross-Ref). Ist das stabil über Reload (EMF Identity), oder müssen wir aufpassen, dass nach `EcoreUtil.copy` die Identität erhalten bleibt?

## 12. Contract-Import (OpenAPI / AsyncAPI) und stub-freie Consumption im v2-Modell

Zwei zusammenhängende Ideen, die das v2-Channel-Modell direkt aufgreift. Anforderungs-Seite: `REQUIREMENTS.md` §5 (FR-Rev-OpenAPI / FR-Rev-AsyncAPI / FR-Rev-Contract-*, „Dynamic (stub-free) consumption" / FR-Dynamic-*) und §8 Deferred; Backlog: [OPEN_ISSUES.md](OPEN_ISSUES.md) A7 (Ingestion) und A8 (dynamischer Consumer).

**Wichtig:** OpenAPI und AsyncAPI sind *Import-/Beschreibungsformate*, **kein** neuer Flavor. Der abgeleitete Transport ist ein ganz normaler `RestFlavor` bzw. `MqttFlavor` — es entsteht keine neue Flavor-Subklasse.

### 12.1 Contract → v2-Flavor/Channel-Mapping

- **OpenAPI → REST.** Das Dokument liefert Catalog-`ServiceInterface`(s); die publizierende `ServiceImplementation` bekommt `publishedVia`/`discoverableVia` = abgeleiteter `RestFlavor`. Jede OpenAPI-Operation (`path` + method) → ein `OperationFlavor` mit `interactionStyle = REQUEST_RESPONSE` und einem **symmetrischen** `OperationChannel` (§4): `flavor = RestFlavor`, `correlationStrategy = SYNCHRONOUS`. Deckt sich mit dem heutigen v1-Verhalten.
- **AsyncAPI → MQTT/async.** `channels`/`messages` → `OperationFlavor`s. **Hier wird das v2-Modell erst tragend:** AsyncAPI-Operationen sind typisch `EVENT_STREAM` oder `FIRE_AND_FORGET`, und die request-/response-Kanäle liegen auf *verschiedenen* Topics → **asymmetrischer Split-Channel** (§4) mit `MqttFlavor`. AsyncAPI-`replyTo`/`correlationId` mappen direkt auf `CorrelationStrategy = TOPIC_BASED` bzw. `HEADER_BASED` (§5); QoS/Delivery → `StreamOptions.delivery` (§2).
- **Warum ins v2-Modell, nicht (nur) v1:** v1 kennt nur den sync REST-Roundtrip. Ein AsyncAPI-Stream lässt sich in v1 gar nicht verlustfrei abbilden — er braucht die InteractionStyle-/Channel-Achse aus §2/§3. Der `RestFlavor`-Zweig eines OpenAPI-Imports funktioniert dagegen auch schon gegen v1.
- **Validierung:** der Cross-Achse-Validator aus §7 greift automatisch — ein AsyncAPI-Stream-Op, der versehentlich auf einen `RestFlavor` gemappt würde, fällt beim Publish über den `PublishHook` durch.

### 12.2 Stub-freier (dynamischer) Consumer über das v2-Modell

Das v2-Modell macht den stub-freien Consumer (FR-Dynamic-*) **stärker** als v1: `OperationFlavor` + Channels + `interactionStyle` sind eine vollständige Wire-Beschreibung. `invoke(reference, operationName, args)` liest sie zur Laufzeit und wählt den passenden Invoker:

- `SYNCHRONOUS` → der heutige `RestServiceInvoker`.
- `HEADER_BASED` / `TOPIC_BASED` → der `AsyncInvoker` mit pending-call-Map (§5).

„Keine generierten Typen" heißt im v2-Kontext konkret: der Invoker bezieht `OperationChannel.flavor` + `correlationStrategy` + `interactionStyle` aus dem Modell statt aus generiertem Code. Der Application-Code sieht — je nach Style — `Future<T>` oder `Flux<T>`, ohne dass je ein Stub generiert wurde. Die Correlation-Abstraktion aus §5 ist damit der Hebel, der den dynamischen Consumer über sync REST hinaus trägt.

### 12.3 Contract-Metadaten → Capabilities

OpenAPI/AsyncAPI sind reicher als das DDSR-Kernmodell. Statt diese Reichhaltigkeit still zu verwerfen (verletzt FR-Rev-Lossless-or-Diagnostic), wird sie als `Capability` (§6) abgelegt:

- Content-Types (OpenAPI `content`) → `ddsr.contentType`.
- Security-Schemes (OAuth2, apiKey, mTLS) → `ddsr.security`.
- Transport-Version (http/1.1 vs. http/2; mqtt-v3.1 vs. mqtt-v5) → `ddsr.transport`.
- AsyncAPI-QoS/Delivery → `ddsr.transport` bzw. `StreamOptions`.

Dadurch ist ein importierter Contract sofort im Capability/Requirement-Resolver (§6) nutzbar: ein Consumer mit `requirement (type=application/json)` matcht gegen die aus dem OpenAPI-`content` abgeleitete Capability — ohne Sonderpfad für importierte Services.

> Offene Frage (verwandt mit §11 „Wer wählt zwischen mehreren `discoverableVia`-Optionen?"): ob ein importierter Contract, der mehrere Server/Transports deklariert, mehrere `discoverableVia`-Flavors erzeugt und wer dann beim Consumer die Wahl trifft.

## 13. Referenzen

- [ARCHITECTURE.md](ARCHITECTURE.md) — heutiger v1-Stand, insb. §2.5 (Reflective Proxy + ServiceInvoker)
- [UPDATE_POLICY.md](UPDATE_POLICY.md) — Update-Policy benutzt Stream-Termination aus §8, und Heartbeat-Protokoll
- [OPEN_ISSUES.md](OPEN_ISSUES.md) — A1 (SSE/Event-Stream), W3 (gemischte Body-Args), A2 (MQTT-Flavor), A7 (OpenAPI/AsyncAPI-Ingestion, → §12), A8 (dynamischer stub-freier Consumer, → §12)
- [REQUIREMENTS.md](REQUIREMENTS.md) — Cross-Language-Demo-Flow, Hook-Architektur, FR-Rev-OpenAPI/AsyncAPI + FR-Dynamic-* (→ §12)
- OSGi Core Spec, Kapitel "Provide-Capability / Require-Capability" — Vorbild für §6
