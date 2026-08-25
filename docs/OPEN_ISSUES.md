# Eclipse Fennec Services (Arbeitsname DDSR) — Offene Probleme und Bugs

Lebende Liste der bekannten Defekte, Limitierungen und nächsten
Schritte. Jeder Eintrag: kurze Beschreibung, Symptom, Ursache,
Lösungsskizze. Status: `open`, `partial` (teilweise gefixt),
`workaround` (umgangen), `won't-fix` (bewusst offen).

Letzte Aktualisierung: 2026-08-25, Branch `feat/ts-parity` (siehe
Nachtrag).

---

## Nachtrag 2026-08-25 (Branch `feat/ts-parity`)

Sammel-Update nach TS-Paritäts-Umbau, Fingerprints, Lifecycle-Kette und
Cross-Language-Harness; Entscheidungen und Details in
[DECISIONS_PARITY.md](DECISIONS_PARITY.md) (D1–D16):

- **W1 → closed für `/registry` und `/catalog` (list):** beide liefern
  jetzt eine unter dem Read-Lock erstellte, in einer In-Memory-Resource
  geparkte Kopie (D9) — kein `file:`-Leak, kein
  Live-Serialisierungs-Race mehr. W2 (alte Snapshots) bleibt.
- **M4 → closed:** TS-Ecore-Kopie synchronisiert, Modell mit
  emfts-codegen 0.0.1-next.8 regeneriert (`UNSPECIFIED` vorhanden);
  `@emfts/core` auf 0.1.1-next.18 (D12 dokumentiert die verbleibenden
  emf.ts-Kanten: Write-Side-Refs auf Nicht-Root-Ziele, Reader liefert
  Attributwerte als Strings, Attributform für many-valued Attribute).
- **T3 → partial, deutlich besser:** Java 126 Tests (broker.core 51+16
  Filter+8 Concurrency-Stress, xmi.codec 14+12 Fingerprint, client.java
  17, MQTT 15), TS **68 Tests** (vorher 0; SSE-Parser, EventSource,
  ListenerRegistry, BrokerHttp, Provider/Consumer, Properties,
  Fingerprint-Golden, MQTT). Dazu die FR-P4-Harness (`itest/`):
  Host- und Podman-Variante, Szenarien A/B/C grün.
- **O1 → mutmaßlich behoben (D16):** Wurzel war der doppelte
  HTTP-Stack, den `enableJakartaREST` per `-runbundles.jersey` in jeden
  Launch injizierte (felix.http.jetty 5.x **und** jetty12). Die bndruns
  leeren `jersey.deps` jetzt; Container-Starts fielen von Minuten auf
  Sekunden. Retry-Workarounds in den Debug-Komponenten bleiben vorerst.
- **O3 → partial:** eingecheckte Configs defaulten auf `localhost`
  statt LAN-IP; die OCD-Defaults in `PaymentPublisher.Config` tragen
  noch die alten Adressen (werden von den Configs überschrieben).
- **A1 → closed:** SSE + Snapshot-on-Reconnect + Listener-Registry
  jetzt auch TS-seitig (D3); Broker sendet SSE-Heartbeats
  (`org.eclipse.fennec.services.broker.rest.sse`, Default 10 s) — Teil der
  D14-Shutdown-Kette.
- **A2 → partial+:** Event-Transport in beiden Sprachen
  (`@ddsr/transport-mqtt` neu); **Drahtnachweis-Entscheidung gefallen:**
  Mosquitto-Container in der Podman-Harness (Szenario C) statt
  Moquette-Abhängigkeiten — für die TS-Seite über echtes TCP grün.
  Offen: derselbe TCP-Nachweis für die Java-Bundles (Launch mit
  broker.mqtt/client.mqtt gegen den Mosquitto-Container) und Etappe 2
  (MQTT als Service-Flavor).
- **Neu behoben, vorher unentdeckt:** REST-Withdraw des Java-Clients
  scheiterte seit jeher client-seitig (Jersey verweigert DELETE mit
  Body) und war doppelt verschluckt → kanonisch jetzt
  `POST /implementations/withdraw` (D15); SIGTERM stoppte exportierte
  Jars ohne Framework-Stop → `FrameworkShutdownHook` (D14); SSE-Close
  konnte den Framework-Stop pinnen → begrenzter Closer + Heartbeat
  (D14); `charge` der Java-Ressource widersprach dem publizierten
  Vertrag (accountId-Pflicht) → Default-Account.
- **Neu (Beobachtung):** bnd-`export`-Tasks betten bei reinen
  Dependency-Änderungen veraltete Bundles ein (Gradle-Up-to-date-Check
  kennt die Cross-Projekt-Outputs nicht); die Harness löscht die
  Export-Jars deshalb vor jedem Export.
- **O2 → closed:** die verbliebene Doppel-Aktivierung war die optionale
  Config-Policy des `PaymentPublisher` (Geister-Publish mit
  OCD-Defaults vor Eintreffen der Configurator-Config) →
  `configurationPolicy = REQUIRE`; von der Harness aufgedeckt.
- **Logging-Konvention umgesetzt:** alle Java-Module loggen mit JUL
  (INFO = Lifecycle, WARNING = Probleme, FINE = Diagnose wie der
  Request-Body-Dump); `System.out/err` nur noch begründet auf
  JVM-Shutdown-Pfaden (JULs Cleanup-Hook resettet den LogManager, D14)
  — Konvention steht im Projekt-CLAUDE.md.
- **Neu: ACQUISITION.md** — Design-Entwurf für die Akquisitionsstufe
  (Broker unterstützt Discovery + Acquisition; Invocation bleibt
  peer-to-peer über den Flavor-Pfad): `ConsumerSession`/Lease-Modell,
  idempotenter `PUT /consumers/{id}`-Vollabgleich statt
  acquire/release/heartbeat, bewusst nicht persistiert; deprecated
  `ServiceReference.usingProviders` (neues M5).
- **S4 weitgehend entschärft:** der LDAP-Filter delegiert jetzt an
  `FrameworkUtil.createFilter` / OSGi `Filter` (D7-Revision auf
  User-Hinweis) — der Parser ist Felix' erprobte Implementierung statt
  Eigenbau; unser Code besitzt nur noch die Property→Attribut-Abbildung.
  `~=` ist damit unterstützt (OSGi-konform), Syntaxfehler enden
  weiterhin im leeren Ergebnis + WARNING.

---

## Wire / Persistenz

### W1 — `/registry` und `/catalog` (list) leaken `file:`-URIs · status: closed (2026-08-25, siehe Nachtrag)

**Symptom.** Im Response-Body stehen Non-Containment-Refs als
absolute Dateipfade:

```xml
<implementations href="file:/opt/git/kloster-prototype/org.eclipse.fennec.services.broker.rest/./broker-state.xmi#/1/@implementations.0"/>
<providers       href="file:/opt/git/kloster-prototype/org.eclipse.fennec.services.broker.rest/./broker-state.xmi#/1"/>
```

**Ursache.** Beide Endpoints rendern `broker.getRegistry()` als
single-Root via `XmiMessageBodyWriter`. Der Writer macht
`EcoreUtil.copy(registry)` — die Provider sind aber Sibling-Roots
im broker-internen Resource (URI = `file:.../broker-state.xmi`),
nicht im Copy enthalten. EMF emittiert Cross-Doc-hrefs auf die
Originale → File-URI.

**Risiko.** Exponiert interne Pfade, ist nicht portabel (Clients
können `file:` nicht resolven), inkonsistent (Lookup macht's anders).

**Fix-Skizze.**

A. Symmetrisch zu `LookupResource`: `RegistryResource` und
   `CatalogResource.list()` bauen einen `XmiBundle` mit Copier,
   nehmen Provider als Sibling-Roots auf, Cross-Refs werden
   intra-document.

B. Sauberer: URI des broker-internen Resources auf einen
   kanonischen, nicht-File-URI ändern (z. B.
   `ddsr:internal/state`). Dann emittiert EMF diese URI — kein
   File-Pfad-Leak, kein Refactor der Endpoints. Clients sehen
   immer noch eine Cross-Doc-href, aber ohne Privacy-Verlust.

C. Hybrid: Resource bleibt File-URI, aber `XmiMessageBodyWriter`
   nutzt einen URIHandler, der File-URIs beim Save in canonical
   Broker-URLs umschreibt (`http://broker/.../catalog/{name}`).

**Empfehlung.** Variante A für jetzt (Konsistenz mit Lookup);
Variante B mittelfristig (eleganter).

---

### W2 — Operation-Refs in alten Snapshots zeigen auf File-URIs · status: workaround

**Symptom.** Wenn ein bestehendes `broker-state.xmi` aus älterer
Iteration geladen wird, können Operation-Cross-Refs auf File-URIs
zeigen.

**Workaround.** `broker-state.xmi` löschen, Broker neu starten
(Self-Publish läuft frisch, mit korrekten positional-URIs).

**Fix-Skizze.** `reindex()` könnte beim Laden einen Cleanup-Pass
durchlaufen und Operation-Refs auf live Catalog-Operations
re-wiren — ähnlich zur Publish-Zeit-Logik in
`DdsrBrokerImpl.rewireOperationRefs`.

---

### W3 — Wire-Konvention für gemischte Body-Args fehlt · status: open

**Symptom.** Der `RestServiceInvoker` unterstützt heute:

- GET → alle Args als Query-Params
- POST/PUT/DELETE mit **einem** EObject-Arg → XMI-Body
- POST/PUT/DELETE mit primitive/string Args → alle als Query-Params

Mischmodell (z. B. ein EObject im Body **und** zusätzlich
Primitive als Query) ist nicht implementiert. Auch komplexe
nested DTO als JSON-Body fehlt.

**Fix-Skizze.** Marker-Mechanismus im Modell (z. B.
`ServiceParameter.binding=BODY|QUERY|HEADER` Attribute), den der
Invoker auswertet. Default-Heuristik wie heute.

---

### W4 — Pflicht-Attribute am Default fehlen auf dem Wire · status: closed

**Symptom.** `ServiceEvent.type` ist im Modell `lowerBound="1"`, also
Pflicht. Auf dem Wire fehlt es trotzdem, sobald der Wert
`REGISTERED` ist:

```xml
<services:ServiceEvent reference="799c52dc-…" timestamp="2026-08-23T20:55:42+0200"/>
```

**Ursache.** `REGISTERED` ist das erste Literal von `ServiceEventType`
und damit EMFs Default. EMF entscheidet beim Serialisieren über
`eIsSet()`, und für ein nicht-`unsettable` Attribut ist ein Wert gleich
dem Default nie „gesetzt". Damit fällt das Attribut weg.
`XMLResource.OPTION_KEEP_DEFAULT_CONTENT` hilft **nicht** — geprüft.

**Risiko.** Ein Leser muss EMFs Default-Regeln kennen, um das Dokument
richtig zu interpretieren. Solange alle Bindings aus derselben `.ecore`
generiert werden, stimmt das Ergebnis zufällig; es ist aber genau die
Art impliziter Vertrag, die Cross-Language-Parität bricht — und es
betrifft jedes Pflicht-Attribut, dessen Wert dem Default entspricht,
nicht nur dieses.

**Fix-Optionen (beide Modell-Änderungen).**

A. `ServiceEvent.type` auf `unsettable="true"`. Dann ist `eIsSet`
   explizit und EMF schreibt den Wert immer. Minimaler Eingriff,
   wirkt aber nur für dieses Attribut.

B. `ServiceEventType` ein führendes Literal ohne Bedeutung geben
   (z. B. `UNSPECIFIED = 0`). Dann ist `REGISTERED` nie der Default
   und wird immer geschrieben. Macht zugleich „nicht gesetzt"
   unterscheidbar von „REGISTERED", was für einen Wire-Typ die
   ehrlichere Modellierung ist. Ändert dafür den Enum-Vertrag, auch
   für den TS-Client.

**Behoben mit Variante B.** `ServiceEventType` führt jetzt
`UNSPECIFIED = 0`. Damit ist kein bedeutungstragendes Literal mehr der
Default, `type` steht wieder auf dem Wire (live verifiziert:
`<services:ServiceEvent type="REGISTERED" …>`), und „nicht gesetzt" ist von
`REGISTERED` unterscheidbar.

Der TS-Client hat gezeigt, dass das kein theoretisches Problem war: er
generiert ein String-Union-Enum **ohne** Default-Begriff. Ein
TS-Consumer hätte bei fehlendem Attribut `undefined` bekommen, nicht
`REGISTERED` — ein echter Cross-Language-Bruch.

**Test.** `XmiCodecHardeningTest.aRequiredEnumAttributeIsAlwaysOnTheWire`
hält den Vertrag fest; er fällt, wenn wieder ein bedeutungstragendes
Literal an die erste Position rutscht.

**Offen dazu.** Die TS-Kopie der Ecore hat die Änderung, aber der
generierte TS-Code noch nicht — dort muss
`pnpm --filter ddsr-model generate` laufen (siehe **M4**).

---

## Catalog / Lookup

### C1 — Mehrere Refs pro Provider+Impl-Kombo bei restartetem Broker · status: partial

**Symptom.** Vor Impl-Dedup-Fix (`findImplementationByNameVersion`)
sammelten sich bei jedem Republish Impl-Einträge im Registry an.
Nach Restart erzeugte `reindex()` eine ServiceReference pro Impl —
Lookup gab N Refs für eine echte Service zurück.

**Status.** Neue Publishes deduplizieren `(name, version)`-gleiche
Impls. Alte broker-state.xmi-Dateien aus Pre-Fix-Welt enthalten
möglicherweise noch Duplikate, die beim Restart als separate Refs
auftauchen.

**Empfehlung.** Einmaliges `rm broker-state.xmi` nach Update;
alternativ `reindex()` Cleanup-Pass einbauen.

---

### C2 — Provider-Auswahl im Client ist nicht reachability-aware · status: open

**Symptom.** Wenn der Broker mehrere Provider für ein Interface
kennt (z. B. `payments-ts` auf nicht erreichbarem Host und
`payments-java` lebend), pickt ein naiver Consumer
`locators.get(0)` non-deterministisch. Treffer auf toten Host →
ConnectException nach Retry-Loop.

**Workaround.** Service-Property `ddsr.provider.name` beim
`PaymentProxyRegistrar` → Consumer targetet via `@Reference(target=...)`
deterministisch den gewünschten Provider.

**Fix-Skizze.** Optional: vor Bind kurze TCP-Probe oder
HTTP-HEAD; nur erreichbare Locators registrieren. Oder ein
generisches Failover im Proxy (locator-Liste statt einzelnem
Locator).

---

### C3 — Stale Provider bleiben für immer im Catalog · status: open

**Symptom.** Wenn ein Provider crasht oder offline geht, bleibt
sein Eintrag im Broker bis zu einem expliziten `withdrawImplementation`.

**Fix-Skizze.** TTL- oder Heartbeat-Mechanismus
(`ServiceImplementation.lastHeartbeat`, Cleanup-Job entfernt
abgestandene Einträge). Heute außerhalb des MVP.

---

## Modell

### M1 — `NamedElement.name` nicht mehr global eindeutig · status: open

**Symptom.** Nach iD-Entfernung wird die globale Eindeutigkeit von
Namen nicht mehr modell-seitig erzwungen. Cross-Refs laufen via
positionale URI-Fragmente, was funktioniert, aber implizit ist.

**Fix-Skizze.** iD selektiv wieder einführen, nur auf Klassen,
die echt global eindeutig sein sollen
(`ServiceInterface`, `ServiceProvider`), nicht auf Inner-Containment
wie `ServiceOperation` oder `RestOperationFlavor`. Das löst den
ursprünglichen Kollisions-Bug und bringt Eindeutigkeit zurück.

---

### M4 — Modell-Änderungen in die TS-Kopie nachziehen · status: closed (2026-08-25, siehe Nachtrag)

**Festlegung.** `org.eclipse.fennec.services.model/model/services.ecore` ist die
**kanonische** Quelle. `ddsr-ts-client/packages/ddsr-model/model/ddsr.ecore`
ist eine Kopie davon und wird nachgezogen — das ist so gewollt und kein
Architekturproblem. (TS-Naming folgt separat: der TS-Track behält den
`ddsr`-Namen, bis das ein Kollege eigenständig umbenennt.)

**Was daraus folgt.** Eine Modell-Änderung ist erst fertig, wenn sie
dokumentiert ist, denn der TS-Track zieht anhand dieser Doku nach. Kein
Build vergleicht die Kopien, es gibt also kein automatisches Signal.
Zum Nachziehen gehören immer zwei Schritte:

1. Ecore-Änderung in die TS-Kopie übernehmen.
2. `pnpm --filter ddsr-model generate` im `ddsr-ts-client`-Workspace —
   der generierte TS-Code entsteht mit `emfts-codegen` und ist **nicht**
   Teil des Gradle-Builds.

**Sync-Log.** Chronologisch, neueste zuletzt. „Ecore" = in der TS-Kopie
angekommen, „Codegen" = TS-Code regeneriert.

| Änderung | Ecore | Codegen | Anmerkung |
|---|---|---|---|
| `NamedElement.name`: `iD="true"` entfernt (**M1**) | **nein** | nein | Java löst Cross-Refs seither positional auf und erlaubt gleiche Namen im Dokument; die TS-Kopie erwartet weiter global eindeutige XMI-iDs. Ältester offener Posten. |
| `ServiceEventType`: `UNSPECIFIED = 0` als erstes Literal (**W4**) | ja | **nein** | Ohne Regenerierung kennt der TS-Client das Literal nicht. |

**Warum der erste Eintrag zählt.** Solange `name` in den beiden
Bindings unterschiedlich modelliert ist, kann ein von Java geschriebenes
Dokument auf der TS-Seite anders gelesen oder abgelehnt werden. Das ist
der Posten mit dem grössten Parity-Risiko und sollte beim nächsten
TS-Sync mit erledigt werden.

---

### M5 — `ServiceReference.usingProviders` deprecaten zugunsten `ConsumerSession` · status: closed (2026-08-25, Ecore umgesetzt)

**Problem.** Das Feature sitzt am falschen Ort (die Referenz ist das
consumer-sichtbare, in jedes Lookup-Ergebnis kopierte Wire-Artefakt —
Nutzungszustand daran wäre veraltet, sobald er den Draht berührt) und
hat den falschen Typ (ein Consumer ist nicht notwendig ein
`ServiceProvider`). Es ist bislang tote Modellierung — nichts befüllt es.

**Entscheidung.** Ersetzt durch das Lease-Modell in
[ACQUISITION.md](ACQUISITION.md): `ConsumerSession` (containment im
Registry, Owner der Akquisitionen) mit eOpposite-Sicht
`ServiceRegistration.usingSessions`; kein gespeicherter Zähler — die
Wahrheit ist die Lease, der Count ist eine Abfrage. Im selben Zug
bekommt `ServiceRegistration` die fehlenden non-containment-Refs
`provider` und `implementation` (ACQUISITION.md §8) — das ersetzt die
`implByRegistration`-Side-Map des Brokers durch Modellstruktur —,
optional plus OCL-derived `consumerCount` (§9).

**Umgesetzt 2026-08-25** (auf Anweisung des Modell-Owners): `usingProviders`
**entfernt** (nichts befüllte es; EMF kennt keine sprachübergreifende
Deprecation — totes Gewicht in jedem generierten Binding wäre teurer als
der Schnitt), `ConsumerSession` (consumerId, lastRenewal, capabilities,
acquisitions ⟷ eOpposite `ServiceRegistration.usingSessions`),
`ServiceRegistration.provider`/`.implementation`,
`LocalServiceRegistry.sessions`; Java (fennecEMF `-generate`) und TS
(emfts-codegen) regeneriert, beide Testsuiten grün. **Bewusst noch
nicht:** `consumerCount` als OCL-derived Feature — der EMF-Codegen
erzeugt für derived/volatile ohne aktive OCL-Delegation
UnsupportedOperationException-Stubs; kommt zusammen mit der
OCL-Aktivierung (M2). Broker-Umbau auf die neuen Refs
(implByRegistration-Side-Map ablösen, Session-Endpoints) ist
ACQUISITION §12 Schritt 2+.

---

### M2 — OCL-Constraints nicht aktiv · status: won't-fix (in MVP)

**Symptom.** Die `.ecore` deklariert OCL-Annotations
(`-- ServiceInterface ist conceptually immutable …` etc.), die
zur Laufzeit nicht ausgewertet werden.

**Fix-Skizze.** Fennec-OCL einbinden (`org.eclipse.fennec.m2x.ocl`),
Validation in den Publish-Pfaden aufrufen. Für nach MVP.

---

### M3 — `ServiceOperationFlavor.name` mit `ServiceOperation.name` koppeln · status: convention

**Symptom.** Heute setzt der Publisher `of.setName(opName)` und
`of.setOperation(operation)` parallel — die Namen sind per
Konvention gleich. Bricht, wenn jemand sie auseinanderlaufen
lässt.

**Fix-Skizze.** Im Modell `OperationFlavor.name` derived aus
`operation.name` machen, oder einen Validator
(`name == operation.name`) einbauen.

---

## Codegen / Tooling

### T1 — Kein Codegen für Service-Stubs · status: open

**Symptom.** `PaymentRemote`, `BrokerCatalogRemote` sind
hand-geschrieben. Für jeden neuen Service-Vertrag braucht's
manuelle Wartung.

**Fix-Skizze.** Generator aus `ServiceInterface.xmi` →
Java-Interface (Methoden = Operationen, Parameter = Modell-
Parameter). Optional auch TS und Python emittieren. Heutige
hand-geschriebenen Stubs sind die Referenz-Form.

---

### T2 — Kein Cleanup-Tool für broker-state · status: open

**Workaround.** `rm broker-state.xmi` zwischen Inkrement-Wechseln.

**Fix-Skizze.** Migration- oder Repair-Endpoint
(`POST /admin/cleanup`?) im Broker, der inkonsistente Refs
rewired und Duplikate dedupliziert.

---

### T3 — Keine Testsuite in beiden Tracks · status: partial

**Symptom (ursprünglich).** Es existierte kein einziger automatisierter
Test:

- Java: alle acht `test/`-Ordner der Module waren angelegt und **leer**,
  `*Test.java` fand 0 Treffer. Der Build meldete entsprechend
  `test NO-SOURCE` und `testOSGi SKIPPED` in jedem Modul.
- TypeScript: `vitest.workspace.ts` ist konfiguriert, aber es gibt
  **0** `*.test.ts` / `*.spec.ts`.

**Stand Java.** Zwei Module haben jetzt eine Suite, plain JUnit ohne
OSGi-Runtime wie vorgesehen:

- `xmi.codec` — 11 Tests, entstanden mit der S3-/S7-Härtung
  (XXE, Entity-Expansion, SSRF, Body-Cap, plus die Zusagen, die dabei
  erhalten bleiben mussten).
- `broker.core` — 25 Tests auf `DdsrBrokerImpl`: Ownership- und
  Katalog-Validierung, Rewiring auf lebende Katalog-Einträge,
  Provider- und Impl-Dedup (die **C1**-Regeln), Withdraw,
  Katalog-Governance, Persist/Rehydrate inklusive `reindex`.

Alle Security- und Dedup-Tests sind per Mutationsprobe gegen den
ungepatchten Code geprüft, fallen dort also tatsächlich.

Die Suite hat beim Schreiben einen echten Defekt gefunden: den
Withdraw-Leak (siehe unten in diesem Eintrag). Nebenbei fiel die
IDE-/Gradle-Interferenz **O5** auf — kein Projektdefekt, aber ein
Fehlerbild, das überzeugend nach einem aussieht.

**Offen.** TypeScript hat weiterhin 0 Tests. Die
Cross-Language-Parity-Suite aus Success-Kriterium 3 fehlt vollständig
und hängt an **A1**/**A2**. Von den Java-Modulen sind sechs weiter
ohne Tests — am ehesten lohnt `client.rest`
(`RestServiceInvoker`-Wire-Konventionen) als nächstes.

**Gefunden und behoben: Withdraw-Leak.** `withdrawImplementation`
entfernte die Implementation nur aus `registry.implementations`, ließ
sie aber in `provider.implementations` stehen. Folgen: die
zurückgezogene Implementation wurde weiter in jeden Snapshot
geschrieben, und sie behielt ihre Non-Containment-Referenz auf ihr
Katalog-Interface — ein späteres `removeCatalogEntry` riss dieses
Interface aus seiner Containment, während etwas im Resource noch darauf
zeigte, und **jeder** folgende Save schlug mit
`not contained in a resource` (Code 500) fehl. Da die Katalog-Operationen
den In-Memory-Stand bei Persist-Fehler nicht zurückrollen, blieb der
Broker dauerhaft in diesem Zustand. Behoben durch Detach vom Provider,
analog zu dem, was `retireImplementation` auf dem Republish-Pfad ohnehin
tat.

**Ebenfalls behoben: Rollback der Katalog-Operationen.**
`addCatalogEntry`, `deprecateCatalogEntry` und `removeCatalogEntry`
hatten kein Rollback bei Persist-Fehler, anders als
`publishImplementation` — ein fehlgeschlagener Save liess die
Modelländerung stehen, der In-Memory-Stand lief also dem Snapshot
voraus. Alle drei rollen jetzt zurück; `removeCatalogEntry` stellt den
Eintrag an seiner **ursprünglichen Position** wieder her, weil
Cross-Refs in den Katalog positionale URI-Fragmente benutzen
(`//@catalog.N`). Die Severity-Prüfung liegt dafür in einem
gemeinsamen `isError`-Helper statt zweimal voll qualifiziert inline.

**Risiko.** `REQUIREMENTS.md §9` Success-Kriterium 3 verlangt eine
Testsuite, die Cross-Language-Parity zusichert und auf einem Clean
Checkout grün läuft — der DoD ist ohne das nicht erreichbar. Heute
wird die Parity-Aussage ausschließlich durch manuelles Log-Lesen
gestützt.

Zweite Folge: ohne Tests und ohne beobachtetes CI gibt es kein Signal,
wenn etwas wegbricht. Konkret eingetreten — der `main`-Build war
zwischen 2026-07-02 und 2026-08-23 rot bzw. ungebaut, ohne dass es
auffiel (Ursache war ein upstream aufgeräumter Snapshot der
fennec.bnd-Libraries, nicht der Projektcode).

**Fix-Skizze.** Erste Ebene gemäß der vorgesehenen Teststrategie
(plain JUnit, ohne OSGi-Runtime, OSGi-APIs gemockt): `DdsrBrokerImpl`
mit publish / Provider-Dedup / Impl-Dedup / `rewireOperationRefs` /
`reindex`. Danach `xmi.codec` — dort fallen die Fixtures für **S3**
(bösartiges XMI) ohnehin an, sodass Härtung und Testinfrastruktur in
einem Schritt entstehen. Cross-Language-Parity-Suite als dritte Ebene,
sobald A1/A2 stehen.

Hängt inhaltlich an **S3** (gemeinsame Fixtures) und ist Voraussetzung
für Success-Kriterium 3.

---

## Runtime / Operations

### O1 — Startup-Race zwischen Publish und JAX-RS-Wiring · status: mutmaßlich behoben (2026-08-25, D16 — siehe Nachtrag)

**Symptom.** Im selben Launch published `PaymentPublisher` seinen
Endpoint vor Jetty/Jakartars das Resource wired hat. Erste
Client-Calls hitten ConnectException.

**Workaround.** Retry mit Backoff in `PaymentDebug` /
`TsPaymentDebug` (5×400 ms).

**Fix-Skizze.** Publisher referenziert eine Whiteboard-Readiness-
Condition (oder pollt `JakartarsServiceRuntime.getRuntimeDTO()`
auf `applicationDTO[…].failedResources == empty`).

---

### O2 — Mehrfach-Aktivierung beim DS-Bootstrap · status: closed (2026-08-25, siehe Nachtrag)

**Symptom.** Hatte sich Vorher mehrfach gezeigt:
`PaymentPublisher.activate` wurde 2-3× durchlaufen, jeder Run
sendete einen Publish.

**Status.** `@Reference` auf `DYNAMIC RELUCTANT` umgestellt +
Activate-Guard (`if (registration != null) return`). Sollte jetzt
einmalig sein.

**2026-08-25, Wurzel gefunden und behoben:** Die verbliebene
Doppel-Aktivierung war die **optionale** Configuration-Policy —
`PaymentPublisher` aktivierte vor Eintreffen der Configurator-Config
und publizierte eine Geister-Registrierung mit den OCD-Defaults
(LAN-IPs), nach der Re-Aktivierung dann erneut mit der echten Config;
Consumer konnten kurzzeitig den toten Endpoint discovern (in der
FR-P4-Harness als `fetch failed` sichtbar). Fix:
`configurationPolicy = REQUIRE` — die Config ist ins Bundle
eingebettet, REQUIRE kostet nichts. Genau eine Publish-Zeile pro
Start, Harness deterministisch.

---

### O3 — Cross-Network-Erreichbarkeit · status: partial (2026-08-25, siehe Nachtrag)

**Symptom.** Cross-Machine-Publish (z. B. TS auf `192.168.1.5`)
wird auf `localhost` registriert, ist von außen unerreichbar.
Zusätzlich blockt Host-Firewall (firewalld / iptables / Windows-
Defender) oft eingehende Ports.

**Workaround.** Publisher-Configs auf LAN-IPs setzen, Firewall
manuell öffnen (`firewall-cmd --add-port=…`).

**Fix-Skizze.** Eventuell ein `auto-detect`-Modus für die IP des
gewählten Netzwerk-Interfaces; Doku-Hinweise zur Firewall.

---

### O4 — Mehrfache `[DDSR-Client] activated`-Zyklen während Bootstrap · status: cosmetic

**Symptom.** Im Launch-Log kommt das mehrfach. Wirkt unsauber,
hat aber keine funktionale Konsequenz (Idempotenz-Guards greifen).

**Fix-Skizze.** `client.java`-Component-Referenzen ebenfalls auf
`DYNAMIC RELUCTANT` umstellen, analog zum `PaymentPublisher`.

---

### O5 — IDE-Auto-Build und Gradle im selben Arbeitsverzeichnis · status: workaround

**Kein Projektdefekt** — hier festgehalten, weil das Fehlerbild
überzeugend nach einem Build-Bug aussieht und leicht eine Stunde
Fehlersuche am falschen Ende kostet.

**Symptom.** `./gradlew clean build` bricht sporadisch ab, mit
wechselnden und unzusammenhängenden Fehlern:

```
> Task :…broker.core:compileTestJava FAILED
  Ungültige Klassendatei: …/bin/…/DdsrDiagnostics.class
    Zugriff nicht möglich: java.nio.file.NoSuchFileException: …/bin/…/DdsrDiagnostics.class
```

```
> Task :…broker.core:test FAILED
  java.lang.NoClassDefFoundError: org/eclipse/fennec/services/broker/core/internal/InMemoryLookupBackend
```

```
> Task :…broker.rest:jar FAILED
  warning: Unable to determine whether the annotation jakarta.ws.rs.GET is a
  component property type as it is not on the project build path
```

Gemeinsames Muster: Klassen oder Buildpath-Einträge, die es gibt,
sind für einen Moment nicht da.

**Ursache.** Ein parallel laufender IDE-Auto-Build (Eclipse/bndtools)
auf demselben Arbeitsverzeichnis. IDE und Gradle teilen `bin/`,
`generated/` und `cnf/cache` — die IDE räumt dort auf, während Gradle
liest. Sichtbarer Hinweis darauf: unter `cnf/cache/` liegen zwei
bnd-Versionen nebeneinander (Gradle nutzt die aus
`gradle.properties`, die IDE ihre eigene).

**Messung.** Mit aktivem IDE-Auto-Refresh fielen 1–2 von 3 Läufen;
nach Umstellen der Refresh-Einstellung 5 von 5 grün, bei
unverändertem Code.

**Workaround.** Beim Arbeiten auf der Kommandozeile den Auto-Build
bzw. Auto-Refresh der IDE aus lassen — oder umgekehrt. Bei einem
sporadisch roten Build zuerst prüfen, ob die IDE gleichzeitig gebaut
hat, bevor man den Code verdächtigt.

---

## Architektur / nächste Iterationen

### A1 — SSE / Event-Stream · status: closed (2026-08-25, beide Sprachen — siehe Nachtrag)

**Implementiert, java-seitig Ende-zu-Ende.** Der Broker verteilt
`ServiceEvent`s, der SSE-Endpoint liegt auf `GET /events`, und der
Java-Client stellt sie an `DdsrServiceListener` zu. Verifiziert im
Launch: ein Publish löst `UNREGISTERING` + `REGISTERED` beim Listener
aus, in dieser Reihenfolge.

**Transport-agnostisch gebaut**, damit MQTT später eine Ergänzung statt
eines Umbaus ist: `EventSink` in `broker.core` (Whiteboard, mehrere
Sinks), `EventSource` in `client.java`. SSE ist je eine Implementierung
davon, in `broker.rest` bzw. `client.rest`. Das folgt auch
`UPDATE_POLICY.md §8`, wo aus dem Stream später ein Event-Channel
werden soll.

**Emissions-Vertrag.** Nur für akzeptierte *und* persistierte
Mutationen, unter dem Write-Lock (Ordnung pro Service), und ein
werfender Sink darf keine committete Mutation zurückdrehen.

**Noch offen:**

- `CatalogEvent` — laut `REQUIREMENTS.md §9` für den Prototyp
  ausdrücklich out of scope, braucht eine Modell-Ergänzung.
- Der TS-Client abonniert nicht; DoD-Schritt 7 ist damit erst
  java-seitig erfüllt.
- ~~Kein Snapshot-Nachziehen bei Reconnect~~ — **erledigt.** Der
  Consumer liest bei jedem (Wieder-)Verbinden für jedes abonnierte
  Interface neu ein, und `find()` merkt sich zusätzlich, was es
  zurückgibt. Das repariert die Zuordnung, die das
  `UNREGISTERING`-Routing braucht: ein Consumer, der erst nach dem
  Publish zuhört, kannte die Referenz vorher nicht und musste auf
  „an alle" ausweichen. Gegen einen echten Broker-Neustart verifiziert
  (Snapshot, Abbruch mit 3-Sekunden-Retry, Reconnect, zweiter
  Snapshot).

  Was hier bewusst **nicht** passiert: `find()` weiter aus einem
  lokalen Cache beantworten. Der Lookup bleibt Pull-Through; ein
  lokaler Registry-Cache im Sinne von FR-Sync-Hybrid wäre ein eigener
  Entwurf mit eigener Staleness-Frage.
- Filterung nur über `?flavors=`. Der LDAP-`filter` eines Listeners
  wird noch nicht ausgewertet — genauso wie im Lookup (siehe
  `InMemoryLookupBackend`).

**Nebenbefund, festgehalten weil er Zeit kostet.**
`jakarta.ws.rs.sse.SseEventSource` ist client-seitig **nicht** nutzbar:
es findet seine Implementierung per ServiceLoader, und
`jakarta.ws.rs-api` deklariert keine `osgi.serviceloader`-Requirements,
also webt SPI-Fly es nicht — `Provider for
jakarta.ws.rs.sse.SseEventSource.Builder cannot be found`, obwohl
`jersey-media-sse` im Runtime liegt. Server-seitig tritt das nicht auf,
weil das Whiteboard SSE explizit verdrahtet. `RestEventSource` liest
den Stream deshalb mit einem normalen JAX-RS-Client und parst SSE selbst
(ein Zeilenprotokoll), was zugleich präzise Reconnect-Signale erlaubt.

### A2 — MQTT · status: partial (2026-08-25: Events beide Sprachen + TS-TCP-Nachweis — siehe Nachtrag)

**Etappe 1 erledigt: MQTT als Event-Transport.** `broker.mqtt` bringt
einen `EventSink`, `client.mqtt` eine `EventSource`. Damit ist die
Abstraktion aus **A1** einmal durchgemessen: weder `broker.core` noch
`client.java` noch der SSE-Transport wurden dafür angefasst. Die
Nutzlast ist dasselbe selbstenthaltene XMI-Dokument, erzeugt von der
geteilten `EventDocument` in `broker.core` — ein Consumer liest also
identisch, egal wie das Event ankam.

Topic-Layout: `<prefix>/<interface>`, ein Publish pro Interface. Das ist
das MQTT-native Gegenstück zum `?flavors=` des SSE-Transports — gleiche
Anforderung (FR-Sync-Filtering), je in der Sprache des Transports.
Events, deren Interface nicht bestimmbar ist (der Normalfall bei
UNREGISTERING), gehen auf `<prefix>/_unknown`; Subscriber nehmen das mit
und matchen über die Referenz-Id, dieselbe „lieber überzustellen als
verwerfen"-Regel wie im Client-Registry.

Nicht retained: ein Event beschreibt einen Übergang, keinen Zustand. Ein
später hinzukommender Subscriber darf nicht über eine längst
zurückgezogene Registrierung informiert werden — er zieht einen
Snapshot.

**Dabei gefunden: `EventSource` transportiert die Interessenlage nicht.**
`open(Handler)` sagt nicht, *welche* Interfaces der Consumer will. Der
SSE-Transport konnte das hinter einem konfigurierten `?flavors=`
verstecken, MQTT kann es nicht: sein natürlicher Filter ist das Topic,
und der Broker publiziert genau deshalb pro Interface. Die
MQTT-Quelle abonniert daher `<prefix>/#` und lässt das Routing im
SDK machen — korrekt, aber sie bewegt Bytes, die sie sich sparen könnte.

Die Interessenlage ändert sich zur Laufzeit (Listener kommen und gehen),
eine bloße Parameter-Ergänzung reicht also nicht; es braucht entweder
einen `Supplier` oder ein Re-Open bei Änderung. Das ist ein eigener
Entwurfsschritt, bewusst nicht nebenbei entschieden. **Der eigentliche
Wert dieser Etappe:** genau diese Grenze wäre bei einem einzigen
Transport nie aufgefallen.

**Ebenfalls dabei behoben.** `xmi.codec` warf
`jakarta.ws.rs.WebApplicationException`, womit jedes Bundle, das das
Wire-Format liest, JAX-RS mitschleppen musste — für ein MQTT-Bundle
offensichtlich falsch, und die MQTT-Tests fielen sofort darüber
(`NoClassDefFoundError: jakarta/ws/rs/WebApplicationException`). Der
Codec wirft jetzt `XmiCodecException` mit einem `Reason`; die
JAX-RS-Provider und `ImplementationsResource` übersetzen das über
`XmiHttpErrors` nach 400/413. Dass die HTTP-Semantik unverändert bleibt,
ist eigens getestet.

**Offen: Etappe 2, MQTT als Service-Flavor.** Das ist, was der DoD
verlangt (Schritte 3 und 5): ein Provider, der `MqttFlavor` annonciert,
und ein Consumer, der ihn darüber *aufruft*. Braucht
Request/Response-Korrelation über Topics und eine Provider-Seite, die
auf Topics hört. Modellseitig vorgesehen (`MqttFlavor`,
`MqttOperationFlavor`), noch nicht gebaut.

**Offen: Nachweis auf dem Draht.** Die Tests laufen gegen einen
gefälschten MQTT-Client, prüfen also Topic-Layout, Nutzlast und
Vertragstreue, aber nicht Paho-über-TCP. Ein eingebetteter Broker war
der Plan; Moquette kostet allerdings **18** Compile-Abhängigkeiten
(Netty, H2, HikariCP, Dropwizard-Metrics, Librato, Bugsnag,
commons-codec), was für einen Test-Broker viel Oberfläche ist —
Telemetrie-SDKs zumal. Offene Optionen: (a) ein minimaler
QoS-0-Test-Broker im Testcode, abhängigkeitsfrei; (b) die 18
Koordinaten in `central.mvn`; (c) einmalig manuell gegen einen externen
Mosquitto. Paho selbst ist eine Koordinate ohne transitive
Abhängigkeiten und liegt im Index.

### A3 — Authentifizierung / Hooks nicht implementiert · status: open

`REQUIREMENTS.md` skizziert `PublishHook`, `DiscoveryHook`,
`DistributionHook` als Erweiterungspunkte. Heute nur
`X-DDSR-Requestor`-Header für Audit, kein PDP, keine Hook-
Pipeline.

Update-Policy ([UPDATE_POLICY.md](UPDATE_POLICY.md)) liefert
die ersten konkreten Use-Cases für alle drei Hooks (Policy-
Enforcement beim Publish, Deprecation-Filter beim Discovery,
graceful Stream-Close bei Cutover).

### A4 — `RestFlavor.host` kann nur eine URL · status: open

Heute kennt der Provider genau einen Hostnamen. Für Load-Balancing
oder Multi-Region wäre eine Liste sinnvoll. Modell-Änderung.

### A5 — Service-API Update-Policy nicht implementiert · status: design

Update-Modell als Design-Entwurf vorhanden ([UPDATE_POLICY.md](UPDATE_POLICY.md)):
drei Policies (EVERGREEN / DEPRECATE_AND_DRAIN / HARD_CUTOVER) am
`ServiceInterface` annotiert, Trigger über Event-Stream (hängt
an A1), Drain-Mechanik über Heartbeat-Protokoll (löst gleichzeitig
C3). Implementation parallel im v2-Workspace nach Stabilisierung
des Channel-Modells aus [WIRE_CHANNELS.md](WIRE_CHANNELS.md).

### A6 — Wire-Channel-Modell ist auf Request/Response begrenzt · status: design

Heute ist jede Operation implizit sync Request/Response über
einen REST-Roundtrip. Streaming (Event-Stream, Bidirectional),
asymmetrische Request-/Response-Kanäle (Async-RPC), Capability/
Requirement-Matching zwischen Provider und Consumer fehlen.

Design-Diskussion in [WIRE_CHANNELS.md](WIRE_CHANNELS.md): neue
Klassen `OperationChannel`, `StreamOptions`, `Capability`,
`Requirement`; `InteractionStyle`-Enum; Flavor-aufgespalten in
Publish-Flavor / Discovery-Flavors / Operation-Channels. Wird
als paralleles `org.eclipse.fennec.services.model.v2`-Projekt implementiert,
ohne v1 zu refactoren.

### A7 — OpenAPI / AsyncAPI-Ingestion in den Katalog · status: design

Contract-Formate als zusätzliche Katalog-Quelle neben handgeschriebenem
XMI und (späterem) Code-Reverse-Engineering: ein OpenAPI-Dokument →
`ServiceInterface` + `RestFlavor`, ein AsyncAPI-Dokument →
`ServiceInterface` + `MqttFlavor`/async. Erlaubt Onboarding von
Brownfield-Services mit vorhandenem Spec ohne XMI-Handarbeit.

**Wichtig:** OpenAPI/AsyncAPI sind *Import-Formate*, **kein** neuer
`FlavorKind` — der abgeleitete Transport bleibt REST bzw. MQTT (siehe
`REQUIREMENTS.md` §8 Deferred „OpenAPI / AsyncAPI ingestion").

Design in `REQUIREMENTS.md` §5: FR-Rev-OpenAPI, FR-Rev-AsyncAPI,
FR-Rev-Contract-Governance (Import läuft über `addCatalogEntry` →
`PublishHook`, aktiviert sich nicht selbst), FR-Rev-Contract-SafeFetch
(untrusted input → hängt an **S3**: XXE/Entity-Limits + SSRF-Allowlist
für remote `$ref`/Spec-URLs). Lossy-safe-Kontrakt wie
FR-Rev-Lossless-or-Diagnostic.

### A8 — Dynamischer (stub-freier) Consumer · status: design

Late-bound Invocation ohne generiertes Artefakt: der Consumer treibt den
Aufruf direkt aus `ServiceReference` + `ServiceInterface` aus dem Lookup.
Zusätzlicher Konsumtionsmodus neben den typisierten Stubs (nicht Ersatz),
für Gateways, Low-Code/Scripting, Test-Harnesses, Browser-Konsolen,
Admin-Tooling.

Kern-Insight: das Katalog-`ServiceInterface` ist bereits eine
vollständige, sprachneutrale Laufzeit-Beschreibung → dynamische
Invocation ist eine allgemeine Framework-Fähigkeit, entkoppelt von A7
(OpenAPI ist nur *eine* Befüll-Quelle des Katalogs).

Design in `REQUIREMENTS.md` §5 „Dynamic (stub-free) consumption":
FR-Dynamic-Invoke (`invoke(reference, operationName, args)`, generisches
Result), FR-Dynamic-Validation (gleiche `ParameterConstraint`s wie im
typisierten Pfad, Verstoß → `Diagnostic`), FR-Dynamic-Transport-Unchanged
(gleiche Flavor-Client-Plugins + Wire-Format, für den Provider
ununterscheidbar von einem Stub-Call), FR-Dynamic-Parity (Java/TS
identisch), FR-Dynamic-FromImport (bei A7-Import direkt nutzbar).

Offene Details: konkrete API-Shape, generische Result-Repräsentation,
ob/wieviel Roh-Spec als Annotation für Tooling erhalten bleibt.

Hängt an **T1** (Codegen ist der komplementäre typisierte Pfad) und
profitiert von **A7**, ist aber unabhängig davon nutzbar.

---

## Security

Vollständige Methodik, Threat-Tabellen und Standards-Mapping (OWASP
ASVS 5.0, BSI Grundschutz CON.8 / CON.10 / APP.3.1) in
[SECURITY.md](SECURITY.md). Die folgenden `S*`-Findings sind die daraus
abgeleiteten, umzusetzenden Punkte.

### S1 — Kein Transport-Security-Default · status: open

**Symptom.** Client↔Broker und Provider↔Consumer laufen über HTTP
plaintext. Kein TLS-Default, kein mTLS-Pfad.

**Risiko.** Mitlesen/Manipulieren von Catalog-Mutationen und
Service-Calls im Netz.

**Standards.** ASVS V12 (Secure Communication); Grundschutz++ KONF.14.1
(Verschlüsselung beim Transport), ARCH.6.2, BER.7.* (Schlüsselmanagement);
TLS-Parameter aus dem **Mindeststandard-TLS**-Katalog
(`KONF.14.1`/`KONF.2.2`/`KONF.10.2`).

**Fix-Skizze.** HTTPS/TLS als Default, plaintext nur per expliziter
Dev-Flag. mTLS optional über Capability verhandelbar. TLS-Versionen/
Cipher nicht freihändig — aus Mindeststandard-TLS ziehen.

### S2 — Broker hat keine Authentifizierung · status: open

**Symptom.** Jeder, der den Broker erreicht, kann publishen,
Catalog-Einträge mutieren und lookuppen. Heute existiert nur der
`X-DDSR-Requestor`-Header als Audit-Hinweis — keine Verifikation.

**Risiko.** Katalog-Fälschung, Provider-Spoofing, Elevation. **Hoch.**

**Standards.** ASVS V6 (Authentication); Grundschutz++ KONF.11.1
(AuthN vor Zugriff), KONF.5.1 (AuthN am System), BER.2 (Identitäts-
management).

**Fix-Skizze.** AuthN-Untergrenze im DDSR-Kern (fail-closed:
ohne Credential kein Publish/Mutate; Read ggf. konfigurierbar
offen). AuthZ darüber bleibt Sache des `PublishHook` (Transfer an
Integrator). Optional Self-contained Tokens (ASVS V9).

### S3 — XMI-Parser nicht gehärtet (XXE / Entity-Expansion / SSRF) · status: open

**Symptom.** `XmiCodec.loadInto` lädt Client-XMI mit `Map.of()` —
keine Parser-Restriktionen. External Entities und DTDs sind nicht
verboten; `readBundle` ruft `EcoreUtil.resolveAll`, was Cross-Doc-
`href`s auf **beliebige** URIs auflöst.

**Risiko.** **Hoch — heute aktiv ausnutzbar.** XXE (Datei-Leak,
Entity-Expansion/Billion-Laughs-DoS) und SSRF: ein Client schickt
ein XMI, dessen `href` auf eine broker-interne URL zeigt, der Broker
löst sie beim `resolveAll` auf.

**Standards.** ASVS V1 (Encoding & Sanitization), V5 (File Handling),
V15 (Secure Coding & Architecture); Grundschutz++ KONF.12.1
(Eingabevalidierung), DEV.2.6 (Schutz gg. gängige Angriffsmuster).

**Fix-Skizze.** Sichere `XMLResource`-Load-Options:
External-Entities/DTD verbieten, Entity-Expansion-Limit, Tiefen-/
Größen-Limit. Cross-Doc-`href`-Auflösung nur gegen eine Allowlist
(Broker-eigene Catalog-URLs), niemals gegen vom Client gelieferte
beliebige URIs. Diese Härtung sitzt zentral in `xmi.codec` und
schützt damit Server- **und** Client-Seite.

### S4 — LDAP-Filter ungeprüft · status: open

**Symptom.** Lookup-Filter (`/references?filter=`) werden als
LDAP-Syntax-String durchgereicht; weder Syntax noch Komplexität
werden begrenzt.

**Fix-Skizze.** Syntax-Validierung beim Eingang, Komplexitäts-/
Längen-Limit gegen teure Filter-Evaluation (DoS). Greift mit dem
Capability/Requirement-Matching aus [WIRE_CHANNELS.md](WIRE_CHANNELS.md) §6
ineinander.

### S5 — Audit & Fehler-Leakage · status: open

**Symptom.** `X-DDSR-Requestor` ist nur ein Header ohne strukturiertes
Audit-Log. Exception-Messages können im Response-Body nach außen
gelangen.

**Standards.** ASVS V16 (Security Logging & Error Handling);
Grundschutz++ DET.3.1 (sicherheitsrelevante Ereignisse), DET.3.4
(Revisionssicherheit), DET.3.5 (Unbestreitbarkeit), DEV.3.3 (keine
schützenswerten Daten in Fehlermeldungen).

**Fix-Skizze.** Strukturiertes, revisionssicheres Audit-Log für alle
Mutationen (wer/was/wann). Nach außen nur generische `Diagnostic`,
Details ausschließlich ins Log.

### S6 — `file:`-URI-Leak · status: open (= W1)

Deckt sich mit **W1**: `/registry` und `/catalog` leaken interne
`file:`-Pfade. Aus Security-Sicht Information Disclosure (ASVS V5,
CON.10.A9). Fix-Varianten siehe W1; Variante B (kanonischer
nicht-File-URI) ist auch die security-sauberste.

### S7 — Keine Ressourcen-Limits · status: partial

**Symptom.** Kein Max-Body-Size, keine Rate-Limits, kein
Connection-Cap (relevant ab SSE-Stream, A1).

**Standards.** Grundschutz++ KONF.15.1 (Speicher-Begrenzung), KONF.15.2
(Rechenleistung), KONF.15.3 (Denial of Service), KONF.14.5 (Timeout von
Netzverbindungen).

**Fix-Skizze.** Body-Size-Cap am JAX-RS-Eingang, Rate-Limit pro
Requestor, Connection-Cap pro Requestor für den Event-Stream,
Connection-Timeout (DoS-Schutz).

**Erledigt: Body-Size-Cap.** `WireBody.MAX_BYTES` (1 MiB) wird in
`XmiCodec` für *jeden* Parse-Pfad erzwungen — sowohl die Resources, die
einen rohen `InputStream` nehmen, als auch die über einen
`MessageBodyReader`. Übergröße ist `413`, nicht `400`: der Body ist
nicht kaputt, sondern zu groß. Zusätzlich ist der
`entityStream.readAllBytes()` in `ImplementationsResource.logAndParse`
ersetzt und der dortige stdout-Dump auf die ersten 4 KiB begrenzt —
der Dump ist laut Klassen-Javadoc gewollte Debug-Hilfe für
Cross-Language-Bodies und bleibt erhalten, aber ein unbegrenzter,
caller-kontrollierter Body gehört nicht ins Log (das ist zugleich ein
Stück **S5**: Angreifer-Inhalt in der Operator-Konsole).

**Offen: Rate-Limit.** Bewusst nicht implementiert, weil es heute nicht
sinnvoll verankert werden kann. Ein Limit pro Requestor müsste am
`X-DDSR-Requestor`-Header hängen, und der ist unauthentifiziert und
damit frei fälschbar — ein Angreifer variiert den Header und umgeht das
Limit vollständig. Sinnvoll erst nach **S2** (authentisierte Identität);
alternativ auf Transport-Ebene (Remote-Adresse) statt in der Anwendung.

**Offen: Connection-Cap und Timeouts.** Der Connection-Cap hängt an
**A1** (ohne Event-Stream gibt es keine langlebigen Verbindungen zu
begrenzen). Connection-Timeouts sind Jetty-Konfiguration und nicht
Anwendungscode.

**Offen: Konfigurierbarkeit des Caps.** Heute eine Konstante. Ein pro
Deployment einstellbarer Wert gehört in die Broker-Konfiguration.

### S8 — Heartbeat/Consumer-ID nicht an Session gebunden · status: design

**Symptom.** *(Stufe 3, hängt an UPDATE_POLICY §4.)* Eine frei
wählbare `ConsumerId` im Heartbeat erlaubt Spoofing: fremde Refs
am Leben halten oder per `ConsumerShutdown` vorzeitig drainen.

**Fix-Skizze.** `ConsumerId` an die authentisierte Session (S2)
binden, nicht als frei wählbares Feld akzeptieren.

### S9 — Code-Publisher ohne Supply-Chain-Integrität · status: design

**Symptom.** *(Stufe 3/4, FR-CodeDist-*.)* Generierte Artefakte
(JAR/npm/wheel) sind weder signiert noch verifizierbar.

**Standards.** Grundschutz++ DEV.4.3 (SBOM), DEV.4.4 (Integrität
externer Libs), DEV.4.7 (deterministischer Binärcode), DEV.6.1
(Freigabe).

**Fix-Skizze.** Artefakt-Signierung + Reproducible-Build
(FR-CodeDist-Reproducible, deckt DEV.4.7) als Supply-Chain-Schutz +
SBOM je Artefakt; Release nur über autorisierten `PublishHook`
(FR-Hook-CodeDist-Authority).

---

## Indexierung

Die Bug-IDs (W*, C*, M*, T*, O*, A*, S*) sind stabil — neue Einträge
am Ende der Sektion anhängen, IDs nicht neu vergeben. Erledigte
Punkte auf `status: closed` setzen statt zu löschen, damit
Historie nachvollziehbar bleibt. Die `S*`-Serie (Security) wird aus
[SECURITY.md](SECURITY.md) gespeist.
