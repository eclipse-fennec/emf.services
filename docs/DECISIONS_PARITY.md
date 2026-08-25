# Entscheidungslog: TS/Java-Parität, Lifecycle, Fingerprints (feat/ts-parity)

Stand: 2026-08-24. Dieses Dokument hält die **Anforderungen** dieses
Arbeitspakets und die **Implementierungsentscheidungen** (Problemstellung,
Optionen, Entscheidung, Rationale) für beide Tracks fest, damit sie im
Nachgang geprüft und ggf. korrigiert werden können. Es ergänzt
[REQUIREMENTS.md](REQUIREMENTS.md) und wird bei Abweichungen in der
Umsetzung nachgezogen.

Kontext: Ausgangsstand ist der gemergte PR #11 (A1 fertig, A2 Etappe 1).
Der TS-Client hatte zu Beginn **weder** SSE, Snapshot, Listener-API,
brauchbares Publish (kein Multi-Root-XMI), Shutdown-Lifecycle **noch**
Tests — die Parity-Lücke ist also ein Ausbau des TS-SDK-Kerns, nicht nur
„SSE nachziehen".

---

## 1. Anforderungen

- **FR-P1 — Feature-Parität TS ↔ Java.** TS-Consumer und TS-Provider
  arbeiten gegen denselben Broker wie die Java-Seite, mit gleichem
  Verhalten: Publish/Withdraw, Discovery, Events, Snapshot-on-Reconnect,
  Listener.
- **FR-P2 — REST + SSE beidseitig (höchste Priorität).** Producer- und
  Consumer-Seite in Java **und** TS über REST + SSE Ende-zu-Ende.
- **FR-P3 — Lifecycle nach OSGi-Vorbild.** Die Unregistrierung eines
  Service ist erst abgeschlossen, wenn die Consumer informiert wurden:
  der Broker beantwortet das Withdraw erst nach dem Event-Fan-out, und
  der Producer-Shutdown **blockiert**, bis diese Antwort da ist (der
  Service-Endpoint bleibt bis dahin bedienbar — Analogie zu OSGi, wo das
  Service-Objekt während der UNREGISTERING-Zustellung nutzbar bleibt und
  `unregister()` erst nach der Listener-Benachrichtigung zurückkehrt).
  Gilt für Java und TS.
- **FR-P4 — Cross-Language-Testsetup.** Automatisierte Tests
  Java-Producer → TS-Consumer und TS-Producer → Java-Consumer gegen einen
  echten Broker; Podman als Ausführungsvehikel wo sinnvoll.
- **FR-P5 — Testabdeckung.** Insbesondere (a) Service-Properties:
  Roundtrip-Fidelity aller Property-Typen über den Draht, (b)
  konkurrierende Zugriffe auf den Broker.
- **FR-P6 — Fingerprints für Service-Descriptions.** Inhaltsbasierte
  Fingerprints (Vorbild: `emf.fingerprint`/`fp1` in Fennec emf.osgi), um
  schnell prüfen zu können, ob Producer, Broker und Consumer dieselbe
  Beschreibung sehen. Muss in Java und TS **identisch** berechnet werden.
- **FR-P7 — MQTT komplettieren (Stretch).** Nicht nur Service-Events,
  sondern auch Service-Description-Austausch über MQTT, in TS und Java.
- Die Beispiel-Services dürfen erweitert werden, wo sie für die
  Lifecycle-/Property-Proben zu trivial sind.

---

## 2. Entscheidungen

### D1 — Broker: Withdraw-Reihenfolge und selbstenthaltendes UNREGISTERING

**Problem.** Heute emittiert `DdsrBrokerImpl.withdrawImplementation` das
UNREGISTERING-Event **nach** Entfernen aus Lookup/Registry und nach
`persist()`. Zu diesem Zeitpunkt ist die Implementation nicht mehr
auflösbar → das Event-Dokument enthält weder Impl noch Interfaces →
MQTT-Topic `_unknown`, Zustellung an **alle** Listener (Überzustellung),
und FR-P3 („Consumer informieren gehört zur Unregistrierung") ist nur
zufällig erfüllt, weil alles synchron im selben Request läuft.
Zusätzlich: Withdraw ist die einzige Mutation **ohne Rollback** bei
Persist-Fehler.

**Optionen.** (a) Event vor der Entfernung emittieren — verletzt den
`EventSink`-Vertrag („nur bestätigte und persistierte Mutationen"); ein
gescheitertes Persist hätte dann ein falsches Event publiziert.
(b) Event-Dokument **vor** der Entfernung bauen (voller Inhalt), aber
erst **nach** erfolgreichem Persist publizieren. (c) Status quo.

**Entscheidung: (b)**, plus Rollback bei Persist-Fehler (Wiederherstellen
von Lookup-Index, Registry- und Provider-Containment, Side-Map), plus:
die HTTP-Antwort auf `DELETE /implementations` geht erst **nach** dem
Fan-out raus (war implizit schon so, wird jetzt Vertrag und getestet).

**Rationale.** OSGi-Mapping: in OSGi wird der Service erst aus der
Registry genommen (keine neue Discovery), dann UNREGISTERING synchron
zugestellt, und `unregister()` kehrt erst danach zurück. Genau diese
Reihenfolge bildet (b) ab: Entfernen → Persist → Fan-out → Antwort.
Das vorab gebaute Dokument macht das UNREGISTERING selbstenthaltend
(Interfaces bekannt) — Consumer-Routing wird präzise statt
Broadcast, und MQTT bekommt echte Topics statt `_unknown`.
Die `_unknown`-/Überzustellungs-Pfade bleiben als Fallback bestehen
(Events aus fremden/alten Quellen).

**Beifang bei der Umsetzung (behoben):** `withdrawImplementation`
prüfte die Zugehörigkeit per **Objekt-Identität** — ein über
`DELETE /implementations` frisch geparster Provider konnte nie matchen,
der REST-Withdraw lief also mutmaßlich seit jeher auf 404
(`IMPL_NOT_PUBLISHED`), und der Java-Provider schluckte das still.
Withdraw löst Wire-Objekte jetzt per (Name, Version) auf die
Live-Objekte auf; getestet.

### D2 — Producer-Shutdown blockiert bis zur bestätigten Abmeldung

**Problem.** Java blockiert heute schon (synchroner `withdraw()` in
`@Deactivate`), TS hat gar keinen Shutdown-Pfad (`close()` ist leer,
keine Signal-Hooks, Withdraw nur manuell).

**Entscheidung.** TS: `DdsrClient.close()` withdrawt alle offenen
Registrierungen **und wartet** auf die Broker-Antworten, danach erst
werden SSE-Streams geschlossen. Neues `attachShutdownHooks(client)`
(SIGINT/SIGTERM/beforeExit) als opt-in-Helfer; der Beispiel-Provider
fährt seinen HTTP-Server erst herunter, **nachdem** `close()`
zurückgekehrt ist. Java: Verhalten bleibt, wird aber getestet und der
`catch (Exception ignore)` in `PaymentPublisher.deactivate` loggt
künftig statt still zu schlucken.

**Rationale.** FR-P3. Ein Producer, der den Endpoint vor der bestätigten
Abmeldung schließt, produziert genau die toten Referenzen, die das
Event-System verhindern soll.

### D3 — TS-SSE: handgerollter Parser über `fetch`, flacher Reconnect

**Problem.** TS braucht eine `EventSource`-Entsprechung für
`GET /events?flavors=…` (`text/event-stream`).

**Optionen.** (a) npm-Paket `eventsource`/`undici`-EventSource,
(b) Browser-`EventSource`-API (in Node 24 vorhanden, aber ohne
Header-/Abort-Kontrolle und mit eigener Reconnect-Semantik),
(c) handgerollter Parser über `fetch` + `ReadableStream`.

**Entscheidung: (c)**, als `RestEventSource`-Port: `data:`-Zeilen
akkumulieren, Blank-Line flusht, alle anderen Felder ignorieren; flacher
Reconnect mit konfigurierbarem Delay (Default 3 s, wie Java);
`onStreamEstablished()` vor dem Pumpen (auch beim Erstconnect) →
Snapshot-Refresh. Keine neue Dependency.

**Rationale.** Der Java-Client parst SSE aus denselben Gründen selbst
(dort: SPI-Fly/ServiceLoader-Problem; hier: Kontrolle über Reconnect und
Establish-Signal). Verhaltensparität — gleicher Reconnect, gleiches
Establish-Signal — ist wichtiger als Bibliothekskomfort, und der Parser
ist ~40 Zeilen.

### D4 — TS-Broker-Zugriff über dedizierte HTTP-Proxies statt synthetischem Locator

**Problem.** Der TS-`publish()` läuft über den synthetischen
Bootstrap-Locator, kann nur Single-Root-XMI senden (der Broker verlangt
Provider **mit** SI-Stub-Geschwistern für die positionalen Cross-Refs),
verwirft die Broker-Referenz (`pending:`-Platzhalter), sendet kein
`X-DDSR-Requestor` und die synthetischen Operationsnamen weichen von den
vom Broker selbst publizierten ab.

**Entscheidung.** Port der Java-Struktur: leichtgewichtige HTTP-Proxies
(`CatalogHttpProxy`, `ImplementationsHttpProxy`, `LookupHttpProxy`) im
`ddsr-client`-Paket, die direkt gegen die Broker-REST-API sprechen —
Multi-Root-XMI (Provider + SI-Stubs), `X-DDSR-Requestor`-Header,
`DELETE /implementations` mit Body, Diagnostic-Parsing auch bei
4xx/5xx (fehlender `severity`/`code` = Default OK/0 — EMF lässt
Default-Werte weg!). `publish()` macht wie Java den zweiten Roundtrip
(`GET /references`) und liefert eine echte `Registration` mit
`reference` und `diagnostic()`. Der FlavorPlugin-Mechanismus bleibt für
**Anwendungs**-Services (Invocation) unverändert.

**Rationale.** Spiegelung der bewährten Java-Architektur; die
Broker-API ist Infrastruktur, kein „discovered Service" — der
Bootstrap-über-Locator-Ansatz hat auf der Java-Seite dieselbe
Entwicklung genommen. Der synthetische Bootstrap entfällt.

### D5 — TS-Modell: Ecore-Kopie synchronisieren, Regeneration versuchen

**Problem.** `packages/ddsr-model/model/ddsr.ecore` hinkt der
kanonischen Java-Ecore einen Hunk hinterher (`NamedElement.name` noch
`iD="true"`), und der generierte Code hinkt der eigenen Kopie hinterher
(`ServiceEventType.UNSPECIFIED` fehlt).

**Entscheidung.** Ecore-Kopie 1:1 von der Java-Seite übernehmen und
`emfts-codegen` laufen lassen. Risikoarm, weil (a) emfts-codegen `iD`
ohnehin nicht abbildet und (b) `UNSPECIFIED` vom Broker nie emittiert
wird — die Regeneration ist Hygiene, kein Blocker; schlägt sie fehl,
wird das dokumentiert und nicht von Hand in generiertem Code editiert.

### D6 — Fingerprint-Schema `sd1` für Service-Descriptions

**Problem.** Producer, Broker und Consumer haben je eine eigene Sicht
auf eine `ServiceInterface`-Beschreibung (Katalog, href-Auflösung,
Kopien im Lookup-Ergebnis). Ob zwei Sichten inhaltsgleich sind, ist
heute nur per Strukturvergleich feststellbar — über Sprachgrenzen
praktisch nie.

**Entscheidung.** Inhaltsbasierter Fingerprint nach dem
`fp1`-Vorbild aus Fennec emf.osgi, aber über `ServiceInterface` statt
`EPackage`: **kanonische zeilenorientierte Textform aus einer
Feature-Traversierung des In-Memory-Modells — nie über die
XMI-Bytes** —, SHA-256, Wertformat `sd1:<64 hex>`. Kanonik (eingefroren
mit dem Tag `sd1`):

- eine Zeile pro Element, Pipe-getrennte Felder, Einrückung für
  Verschachtelung;
- `I|<name>|version=|status=` für das Interface (Deprecation-Metadaten
  `deprecationReason`/`replacedBy` **ausgenommen** — sie beschreiben den
  Katalog-Zustand, nicht den Vertrag);
- Operationen in **deklarierter Reihenfolge** (positionale Cross-Refs
  machen die Reihenfolge bedeutungstragend), Parameter in deklarierter
  Reihenfolge mit `name|type|index|optional|defaultValue`;
- Exceptions je Operation nach Name sortiert;
- Properties (wo vorhanden) nach Name sortiert, mit Typ-Tag und Wert;
- XMI-IDs, `ServiceReference.id`s, Objekt-Identität und
  Serialisierungsreihenfolge gehen **nie** ein.

Verortung: Java als exportiertes Paket `org.eclipse.fennec.services.fingerprint` im
Bundle `org.eclipse.fennec.services.xmi.codec` (das geteilte Wire-Bundle ohne
JAX-RS-Abhängigkeiten; ein eigenes Bundle wäre sauberer, lohnt für eine
Klasse noch nicht — dokumentierte Schuld). TS in **`@ddsr/client`**
(`src/fingerprint/`), nicht in `@ddsr/model` wie ursprünglich skizziert:
`ddsr-model/src` wird von emfts-codegen überschrieben, handgeschriebener
Code dort würde die Regeneration verminen. Hash via `node:crypto`.

**Grammatik-Präzisierungen, mit dem Tag sd1 eingefroren** (vollständige
Spezifikation im Javadoc von `ServiceDescriptionFingerprint`):
Interface-Exceptions als sortierter `X|`-Block mit Payload-Properties
(`pr|<tag>|<name>|value=`), Operations-Exceptions nur als Namensreferenz
`x|<name>` (geteilte Exceptions ohne Duplikation); Escaping `\ | LF CR`;
**Double/Float als IEEE-754-Bitmuster** (`bits:<16/8 hex>`), weil
dezimales Float-Rendering zwischen Sprachen nicht deterministisch ist;
StringList als `<count>:<e1,e2>` mit `\,`-Escaping (Count trennt leere
Liste von leerem Element). Golden-Hash der Payment-Fixture:
`sd1:baafb26e152b76e0e6e713bb86a5e418c2d014d855b24df4d938a517c59807c0`.

Nutzung: der Broker berechnet den Fingerprint beim `addCatalogEntry`
(Rückgabe in der OK-Diagnostic-Message) und liefert ihn in
Lookup-Antworten als `StringProperty` **`ddsr.fingerprint`** auf der
`ServiceReference` (die SI selbst hat im Modell keine Properties); bei
Implementations mit mehreren Interfaces stattdessen
`ddsr.fingerprint.<interfaceName>` pro Interface. Producer und Consumer
hashen ihre lokale Sicht und vergleichen. **Der Paritätsanker ist der
Golden-Test über dieselben Fixture-Dateien
(`itest/fixtures/fingerprint/`) in beiden Sprachen — bestanden.**

**Rationale.** Übernimmt die geprüften Designregeln aus emf.osgi
(Traversierung statt Bytes, Schema-Tag versioniert den Algorithmus,
frozen scheme, konservativ falsch-verschieden statt falsch-gleich) und
beantwortet die eigentliche Frage („sehen alle drei dasselbe?") mit
einem Stringvergleich.

### D7 — LDAP-Filter minimal implementieren

**Problem.** `find(interface, filter)` nimmt den Filter entgegen,
`InMemoryLookupBackend` ignoriert ihn dokumentiert — die typisierte
Property-Hierarchie war bislang komplett unbenutzt.

**Entscheidung.** Minimaler RFC-1960-Evaluator im Broker
(`=`, Presence `=*`, Substring, `>=`, `<=`, `&`, `|`, `!`) über die
Properties der `ServiceReference`; Typkoerzierung entlang des
Property-Typs (numerischer Vergleich für Int/Long/Double/Float/Short,
`true/false` für Bool, elementweises Matching für StringList — Semantik
wie OSGi-Filter über `String[]`). Kein `~=`, keine Extensible-Matching-
Syntax. Ein syntaktisch kaputter Filter führt zu leerem Ergebnis plus
Log, nicht zu 500.

**Rationale.** FR-P5 verlangt „korrektes Testen der Service-Properties" —
ohne einen Konsumenten der Properties gäbe es nichts Korrektes zu
testen. Der Filter ist der OSGi-native Konsument und macht die
Property-Roundtrip-Tests End-to-End statt nur strukturell.

**Revision 2026-08-25 (User-Frage: geht das nicht mit den
OSGi-FrameworkUtils?):** Ja. Der handgeschriebene 454-Zeilen-Evaluator
ist durch einen ~140-Zeilen-Adapter um `FrameworkUtil.createFilter` /
`org.osgi.framework.Filter` ersetzt. Standalone verifiziert: seit Core
R7 trägt die OSGi-API eine eigene Filter-Implementierung —
Plain-JUnit-Tests brauchen kein Framework, zur Laufzeit liefert Felix.
Der Adapter besitzt nur noch die Abbildung der typisierten
Property-Hierarchie auf Filter-Attribute (StringList → Collection mit
OSGi-Any-Match) und matcht über `Filter.match(Dictionary)` via
`FrameworkUtil.asDictionary` — `matches(Map)` wäre bei den Keys
case-sensitiv. Bewusste Verhaltensänderung: `~=` wird jetzt
unterstützt statt abgelehnt (OSGi-Konformität). Alle 17 Filter-Tests
grün; entschärft zugleich S4 (Parser ist jetzt Felix' erprobte
Implementierung).

### D8 — Properties am Beispiel-Service, Fidelity-Tests

**Entscheidung.** Der Payment-Beispiel-Provider (Java und TS) setzt
einen repräsentativen Property-Satz (alle acht Typen inkl.
`StringListProperty`) auf seiner Implementation/Reference. Tests in
beiden Sprachen prüfen den Roundtrip Publish → Broker → Lookup →
Consumer typgenau (Long bleibt Long, 0.5 bleibt Double, leere Liste ≠
fehlende Property), plus Cross-Language in der Harness (FR-P4).

### D9 — broker.core: Concurrency-Fixes vor Concurrency-Tests

**Problem.** Recherchierte echte Races: (1) `GET /registry` /
`GET /catalog` geben das **live** Registry-Objekt als JAX-RS-Entity
zurück — serialisiert wird **nach** Freigabe des Read-Locks, während
Publish/Withdraw mutieren (CME / zerrissenes Dokument); (2) `persist()`
läuft unter dem **Read**-Lock — zwei parallele `snapshot()` schreiben
dieselbe Resource in dieselbe Datei; (3) `DdsrBrokerComponent.delegate`
ist nicht `volatile` und wird ohne Null-Check dereferenziert.

**Entscheidung.** (1) Kopie unter dem Read-Lock (`EcoreUtil.Copier`)
zurückgeben; (2) `persist()` intern serialisieren (dediziertes Monitor-
Objekt); (3) `delegate` volatile + definierter 503-Pfad statt NPE.
Danach Stress-Tests (parallele Publish/Withdraw/Lookup/Snapshot-Salven,
Invarianten-Checks am Ende). Bewusst **nicht** angefasst: die grobe
Lock-Granularität selbst (ein RW-Lock für alles) — für den Prototyp
korrekt vor schnell.

### D10 — Cross-Language-Harness: Skripte zuerst, Container als Vehikel

**Problem.** FR-P4 braucht einen echten Broker + Prozesse beider
Sprachen. Vollcontainerisierung des BND-Builds ist schwergewichtig.

**Entscheidung.** Neues Top-Level-Verzeichnis `itest/`:
(a) `bnd export` der vorhandenen bndruns → ausführbare Jars;
(b) Orchestrierung als Shell/Node-Skripte, die Broker, Java-Provider,
TS-Provider starten und die Consumer-Assertions beider Richtungen
fahren (inkl. Lifecycle-Probe: Withdraw → UNREGISTERING beim
Consumer **vor** Endpoint-Schließung); (c) Containerfiles (Broker: JRE-17-
Basis + exportiertes Jar; TS: node-Basis), sodass dieselbe Suite über
Podman laufen kann — Podman ist das Vehikel, nicht die Voraussetzung:
die Suite läuft auch als Host-Prozesse, damit sie ohne Registry-Zugriff
und in CI ohne Podman nutzbar bleibt.

### D11 — MQTT-Komplettierung (Stretch, nur bei Restbudget)

**Entscheidung.** Reihenfolge: (1) TS-Event-Transport
(`ddsr-flavor-mqtt`-Paket, `mqtt`-npm-Client, Subscribe `<prefix>/#`,
gleiche Payload-Dekodierung wie SSE) — stellt Event-Parität her;
(2) Service-Description-Austausch über MQTT (DoD-Schritte 3/5) als
Request/Response-Korrelation über Topics
(`<prefix>/rpc/<service>/<op>/<corrId>` mit Response-Topic-Property) —
wird zunächst **nur entworfen** (WIRE_CHANNELS-Anschluss), implementiert
nur, wenn REST/SSE-Parität, Lifecycle, Fingerprints und Harness stehen.
Der Drahtnachweis (OPEN_ISSUES A2) läuft in der Harness über einen
Mosquitto-Container, nicht über Moquette-Abhängigkeiten im Build.

**Umsetzungsstand 2026-08-25:** (1) ist fertig — `@ddsr/transport-mqtt`
(MqttEventSource über mqtt.js, Subscribe `<prefix>/#`, gleiche Payload
wie SSE, Snapshot bei jedem Re-Connect, 8 Tests) plus Harness-Szenario C:
Broker-förmiges Event-Dokument über echten Mosquitto/TCP zugestellt und
dekodiert. Offen: derselbe TCP-Nachweis für die Java-Bundles und (2).

### D12 — emf.ts: Versionsanhebung und dokumentierte Reader/Writer-Kanten

**Problem.** Der TS-Track hängt an `@emfts/core`. Mit der gepinnten
`0.1.1-next.8` konnte der Reader positionale Fragmente auf
Nicht-Root-Ziele (`provider="/0/@providers.0"`, wie der Java-Broker sie
in Lookup-Antworten schreibt) **nicht** auflösen — Referenz-Ids und
damit das ganze Event-Routing wären TS-seitig blind gewesen.

**Entscheidung.** Anhebung auf **`@emfts/core 0.1.1-next.18`** (und
`@emfts/codegen 0.0.1-next.8`); die 0.2.0-Linie (EList-Unifikation,
API-Bruch) bewusst nicht. Damit löst die kritische Leserichtung
(Java-Broker → TS) identitätsgenau auf; verifiziert per Probe und durch
die Testsuite (Fixtures pinnen die exakte Java-Wire-Form von Hand).

**Verbleibende, dokumentierte Kanten (Kandidaten für Upstream-Fixes in
emf.ts, DDSR-seitig mitigiert):**
1. *Write-Side:* Non-Containment-Referenzen auf **Nicht-Root-Ziele ohne
   ID** werden beim Serialisieren verworfen bzw. als unbrauchbare
   absolute URI geschrieben (betrifft `operationFlavor.operation` in
   TS-Publish-Bodies). Mitigation: Java matcht Operation-Flavors über
   den Namen; kein Funktionsverlust im Demo-Pfad.
2. *Reader-Typen:* Attributwerte kommen als **Strings** (EInt → `"42"`).
   Mitigation: alle DDSR-Lesepfade koerzieren entlang des Modelltyps
   (`normalizeDiagnostic`, `propertyValue`, Fingerprint-Kanonik).
3. *Many-valued Attribute:* emf.ts **schreibt** die verlustbehaftete
   space-separierte Attributform (Java-EMF schreibt wiederholte
   `<value>`-Elemente, **liest** aber beide Formen; emf.ts liest die
   Java-Elementform korrekt). Mitigation/Konvention:
   StringList-Einträge sind whitespace-freie, nicht-leere Tokens — der
   TS-Builder erzwingt das, der TS-Reader splittet Whitespace (identisch
   zu dem, was Java beim Lesen der Attributform tut).

### D13 — TS-Testinfrastruktur

**Entscheidung.** Vitest-4-kompatible Konfiguration (`test.projects`
statt des entfernten `defineWorkspace`), Tests als `*.test.ts` neben den
Quellen; HTTP wird über die vorhandene `fetchFn`-Naht bzw. injizierte
Streams gemockt (Spiegel der Java-Konvention „plain JUnit, Fakes statt
Framework" → „plain vitest, Fakes statt msw"). Beispiel-Skripte
verlieren die hartkodierte LAN-IP (`BROKER_URL` Pflicht bzw.
localhost-Default).

### D14 — Die Shutdown-Kette: drei verdeckte Defekte unter FR-P3

**Problem.** Die Harness-Lifecycle-Probe (Szenario A) blieb rot, obwohl
Withdraw-Ordering (D1) und blockierender Shutdown (D2) implementiert
waren. Dahinter steckten **drei** einander verdeckende Defekte — jeder
einzelne hätte FR-P3 allein ausgehebelt:

1. **SIGTERM stoppte das Framework nicht.** Der bnd-Launcher der
   exportierten Jars installiert keinen Shutdown-Hook; ein SIGTERM
   beendete die JVM in ~40 ms ohne ein einziges `@Deactivate` — kein
   Withdraw, kein finaler Broker-Snapshot. **Fix:**
   `FrameworkShutdownHook` (DS-Komponente in broker.core, damit in
   allen drei Launches): JVM-Hook → `framework.stop()` +
   `waitForStop(15s)`.
2. **Der SSE-Close-Pfad pinnte den Framework-Stop.** JDKs
   `ChunkedInputStream.close()` draint den Restbody unter dem
   Connection-Read-Lock, den der blockierte Reader-Thread hält — der
   Cross-Thread-Close in `RestEventSource.close()` hing dadurch bis zum
   `waitForStop`-Timeout und mit ihm der `FelixStartLevel`-Thread (per
   Thread-Dump belegt). **Fix:** Close auf einem begrenzten
   Closer-Thread (join ≤ 2 s), plus **SSE-Heartbeat im Broker**
   (`SseEventBridge`, PID `org.eclipse.fennec.services.broker.rest.sse`,
   `heartbeat.seconds` Default 10): Kommentar-Frames wecken die Reader
   regelmäßig (Close greift ≤ 1 Intervall) und räumen tote Subscriber
   ab. Der TS-Client (fetch/AbortController) hat das Problem nicht.
3. **Der Java-REST-Withdraw hat nie funktioniert.** Jersey verweigert
   client-seitig ein DELETE mit Entity (`IllegalStateException: Entity
   must be null for http method DELETE`) — jeder Withdraw des
   Java-Clients scheiterte seit jeher vor dem ersten Byte auf dem
   Draht. Unsichtbar blieb das doppelt: früher durch `catch (Exception
   ignore)`, nach D2 durch JULs eigenen Shutdown-Hook, der den
   LogManager vor unserem Hook zurücksetzt — `System.Logger`-Ausgaben
   im Shutdown-Pfad verschwinden spurlos. **Fixes:** siehe D15;
   Shutdown-Pfade loggen zusätzlich direkt nach `System.err`.

**Lehre (im OSGi-Sinn):** „Consumer werden vor dem Verschwinden
informiert" ist keine Eigenschaft einer einzelnen Methode, sondern der
gesamten Kette Signal → Framework-Stop → Deaktivierung → Transport →
Broker-Fan-out. Erst die End-to-End-Probe der Harness (FR-P4) hat die
Kette geschlossen: Szenario A misst jetzt, dass das UNREGISTERING den
Consumer ~170 ms **vor** dem Prozessende des Producers erreicht.

### D15 — Withdraw-Verb: POST statt Body-tragendem DELETE

**Problem.** `DELETE /implementations` mit XMI-Body war als Wire-Falle
dokumentiert und hat mit Jersey (Client-Validierung) real zugeschlagen;
auch JDK-`HttpURLConnection` und diverse Proxies sind DELETE-Bodies
gegenüber feindlich.

**Entscheidung.** Kanonischer Withdraw ist **`POST
/implementations/withdraw`** (gleicher Body wie Publish: Provider +
SI-Stubs). Beide Clients (Java `ImplementationsHttpProxy`, TS
`BrokerHttp`) senden POST; der DELETE-Endpoint bleibt server-seitig aus
Draht-Kompatibilität bestehen (fetch kann ihn senden), ist aber
deprecated.

### D16 — Doppelter HTTP-Stack in jedem Launch (enableJakartaREST)

**Problem.** Die Podman-Harness zeigte Container-Startzeiten von
Minuten bis zum Publish. Ursache: die `enableJakartaREST`-Library
injiziert über `-runbundles.jersey` einen **zweiten, älteren
HTTP-Stack** (`org.apache.felix.http.jetty` 5.x, `servlet-api` 2.1,
slf4j 1.7) zusätzlich zu den in den bndruns gepinnten jetty12-Bundles —
zwei konkurrierende HTTP-Whiteboards in jedem Launch, seit jeher. Auf
schnellen Hosts gewann jetty12 das Rennen unauffällig (mutmaßlich die
eigentliche Wurzel der O1-Startup-Races); in Containern eskalierte es.

**Entscheidung.** Die bndruns überschreiben das Library-Makro
`jersey.deps` mit leer — alle benötigten Jersey-Bundles sind dort
ohnehin explizit gepinnt. Exporte enthalten jetzt genau einen
HTTP-Stack. (Upstream-Hinweis an die Library: Versionsbereiche der
Beiträge kollidieren mit moderneren Workspaces.)

---

## 3. Prüfpunkte (Definition of Done dieses Pakets)

1. Java- und TS-Testsuiten grün (`./gradlew test`, `pnpm -r test`).
2. Golden-Fingerprint-Test identisch in beiden Sprachen.
3. Harness: beide Richtungen grün, inkl. Lifecycle-Probe
   (Consumer sieht UNREGISTERING, bevor der Producer-Endpoint schließt)
   und Property-Fidelity cross-language.
4. Dieses Dokument aktualisiert, OPEN_ISSUES/STATUS nachgezogen.
