# Eclipse Fennec Services (Arbeitsname DDSR) — Status gegen die Definition of Done

**Stand: 2026-08-23.** Bewertung des Prototyp-Stands gegen die
Success-Kriterien in [REQUIREMENTS.md §9](REQUIREMENTS.md). Dieses
Dokument ist eine datierte Momentaufnahme, kein Design: *was steht,
was fehlt, und wie weit ist "fertig" noch weg.*

Abgrenzung zu den Nachbardokumenten:

- [ARCHITECTURE.md](ARCHITECTURE.md) — **wie** das Gebaute funktioniert
- [OPEN_ISSUES.md](OPEN_ISSUES.md) — **welche** Defekte offen sind (stabile IDs)
- dieses Dokument — **wie weit** wir gegen den DoD sind

---

## 0. Zwischenstand 2026-08-24 — Branch `feat/ts-parity` (in Arbeit)

Dieser Abschnitt dokumentiert den laufenden Umbau; die Abschnitte
darunter beschreiben den Stand *davor* (2026-08-23) und werden nach
Abschluss des Branches neu bewertet. Anforderungen und alle
Entscheidungen: [DECISIONS_PARITY.md](DECISIONS_PARITY.md).

**Fertig und lokal verifiziert (committet als `fa3599d` + Folge-Commit):**

- **Java-Track (D1, D2, D6–D9):** UNREGISTERING ist selbstenthaltend
  und wird vor der HTTP-Antwort des Withdraw gefanout (FR-P3),
  Persist-Fehler rollen zurück; dabei einen Altbug behoben — der
  REST-Withdraw matchte per Objekt-Identität und lief immer auf 404.
  `sd1`-Fingerprint (`org.eclipse.fennec.services.fingerprint`) mit Golden-Fixtures
  in `itest/fixtures/fingerprint/`; Broker liefert `ddsr.fingerprint`
  als Reference-Property. RFC-1960-LDAP-Filter über die typisierten
  Properties (Filter war vorher ein No-op). Implementation-Properties
  werden auf die ServiceReference kopiert (OSGi-Regel).
  Concurrency-Fixes (Registry-Kopie unter Read-Lock — behebt zugleich
  das W1-`file:`-Leck für `GET /registry`; persist serialisiert;
  volatile delegate) plus Stress-Test. **Java: 126 Tests grün**
  (vorher 82).
- **TS-Track (D3–D5, D12, D13):** Der TS-Client hat jetzt echte
  Broker-HTTP-Proxies (Multi-Root-XMI-Publish/Withdraw mit
  SI-Stub-Geschwistern, `X-DDSR-Requestor`, Diagnostic-Parsing mit
  EMF-Default-Semantik), eine SSE-`EventSource` (handgerollter Parser,
  flacher Reconnect, Snapshot-vor-Events), die Listener-Registry mit
  Überzustellungs-Regel, blockierendes `close()` + Signal-Hooks
  (FR-P3), eine Katalog-API, typisierte Property-Helfer und den
  `sd1`-Fingerprint mit **demselben** Golden-Test wie Java.
  Ecore-Kopie synchronisiert und neu generiert (`UNSPECIFIED` da),
  `@emfts/core` auf `0.1.1-next.18` (Leserichtung Java→TS löst
  positionale Fragmente jetzt auf; verbleibende emf.ts-Kanten in
  DECISIONS_PARITY D12). **TS: 60 Tests grün** (vorher 0), Vitest 4.
  Beispiele auf SDK + Lifecycle umgestellt, LAN-IPs raus (O3),
  Configs defaulten auf localhost.

**Cross-Language-Harness (`itest/run-harness.sh`, FR-P4) — beide
Szenarien GRÜN (Stand 2026-08-25 früh):**

- *Szenario A (Java-Provider → TS-Consumer):* Discovery ✓,
  **sd1-Fingerprint Broker = lokal = Golden-Hash über den echten
  Draht** ✓, alle acht Property-Typen typgenau ✓, Invocation ✓, und
  die FR-P3-Probe: das UNREGISTERING erreicht den Consumer ~170 ms
  **bevor** der Provider-Prozess endet ✓.
- *Szenario B (TS-Provider → Java-Consumer):* `getBalance`/`charge`
  cross-language ✓; beim SIGTERM des TS-Providers: Withdraw vom Broker
  bestätigt **vor** dem Stopp des Endpoints, Broker listet den
  Provider danach nicht mehr ✓.

Die Harness hat dabei eine ganze Defekt-Kette gefunden (Details in
[DECISIONS_PARITY.md](DECISIONS_PARITY.md) D14/D15): Java-Ressource
verletzte den publizierten charge-Vertrag; SIGTERM stoppte das
Framework der exportierten Jars gar nicht (→ `FrameworkShutdownHook`);
der SSE-Close-Pfad pinnte den Framework-Stop im
JDK-ChunkedInputStream-Drain (→ begrenzter Closer + SSE-Heartbeat im
Broker); und **der Java-REST-Withdraw hat nie funktioniert** — Jersey
verweigert DELETE-mit-Body client-seitig, doppelt verschluckt durch
`catch(ignore)` bzw. JULs LogManager-Reset im Shutdown (→ kanonischer
Withdraw jetzt `POST /implementations/withdraw`).

**Podman-Vehikel (D10c) — GRÜN:** `itest/run-harness-podman.sh` fährt
dieselben Szenarien mit Broker/Providern/Probes als Container
(`itest/containers/`, generisches Java-Image + TS-Workspace-Image,
`--network=host`). Dabei zwei weitere Wurzeln gefunden:
`enableJakartaREST` injizierte einen **zweiten HTTP-Stack** in jeden
Launch (D16 — mutmaßlich die O1-Wurzel; Container-Starts fielen von
Minuten auf Sekunden), und `podman logs | grep -q` + pipefail ließ
Warteschleifen exakt im Erfolgsfall scheitern.

**MQTT (D11 Etappe 1) — TS-Seite fertig:** neues Paket
`@ddsr/transport-mqtt` (MqttEventSource, Subscribe `<prefix>/#`,
gleiche Payload wie SSE, Snapshot bei jedem Re-Connect, 8 Tests).
**Drahtnachweis-Entscheidung:** Mosquitto-Container in der Harness
(Szenario C) statt Moquette — TS über echtes TCP grün. TS gesamt: 68
Tests.

**Offen auf diesem Branch:** Java-MQTT-TCP-Nachweis (Launch mit
broker.mqtt/client.mqtt gegen den Mosquitto-Container), MQTT Etappe 2
(Service-Flavor), ARCHITECTURE-Feinschliff (Endpoint-Tabelle:
`POST /implementations/withdraw`, SSE-Heartbeat).

---

## 1. Kernaussage

Die **Cross-Language-Demo ist erreicht**, der **geschriebene DoD ist es
nicht**. Das sind zwei verschiedene Begriffe von "fertig", die in der
Doku leicht ineinanderrutschen:

| | |
|---|---|
| **Was gelaufen ist** | Payment-Service über REST-Flavor in beiden Richtungen (TS-Consumer → Java-Provider und umgekehrt), mit manuell gestarteten Launches und handgeschriebenen Stubs. Stand der Verifikation: 2026-05-28/29. |
| **Was der DoD verlangt** | Zusätzlich MQTT-Flavor, SSE-Event-Stream, zwei Katalog-Interfaces, Workaround-Freiheit und eine grüne Cross-Language-Parity-Testsuite. |

Der Abstand ist also nicht "letzte 10 %", sondern zwei substanzielle
Features (A1, A2) plus das komplette Verifikationsfundament.

---

## 2. Was steht

**Java-Seite: vollständig und lebt.** Bundle-Layout wie in
[ARCHITECTURE.md §1](ARCHITECTURE.md). Am 2026-08-23 verifiziert:
Broker startet, seedet den Katalog, `GET /ddsr/rest/catalog` liefert
korrektes XMI.

Architektonisch tragfähig und mehr als ein Durchstich:

- Role-Interface-Splitt (`BrokerCatalog` / `BrokerImplementations` /
  `BrokerLookup`) statt Mono-Service
- embedded vs. remote als reine Service-Property
  (`ddsr.broker.transport`)
- Catalog-URL als kanonische SI-Referenz mit href-Rewiring **ohne**
  HTTP-Fetch (Proxy-URI-Fragment-Auflösung)
- Impl-Dedup über `(name, version)` — Publishes überleben Restarts
  ohne Akkumulation
- reflective Proxy, der Argumentnamen **aus dem Modell** zieht statt
  aus Java-Reflection (unabhängig von `-parameters`)

**TS-Seite: im Repo, drei Pakete.** Seit `1d1504e` (2026-07-02) liegt
`ddsr-ts-client/` hier, nicht mehr im Workspace eines Kollegen:
`ddsr-model`, `ddsr-client`, `ddsr-flavor-rest`.

---

## 3. Was zum DoD fehlt

Der DoD-Flow in [REQUIREMENTS.md §9](REQUIREMENTS.md) hat acht
Schritte. Vier sind heute nicht lauffähig:

| DoD-Anforderung | Status | Issue |
|---|---|---|
| Schritt 1: Broker öffnet HTTP-SSE-Event-Stream | **vorhanden** — `GET /events`, java-seitig Ende-zu-Ende verifiziert | A1 |
| Schritt 3: TS-Provider published Payment **via MQTT** | fehlt — kein `client.mqtt` (Java), kein `ddsr-flavor-mqtt` (TS); MQTT existiert nur als generierte `MqttFlavor*`-Modellklassen | **A2** |
| Schritt 5: TS-Consumer erhält beide Refs, ruft REST **und** MQTT | fällt mit A2 | **A2** |
| Schritt 7: UNREGISTERING-Event per SSE beim TS-Consumer | java-seitig erfüllt; der TS-Client abonniert noch nicht | A1 |
| Katalog mit **zwei** Interfaces: `Payment` + `OrderQuery` | `OrderQuery` existiert nirgends im Repo (grep über Java/TS/XMI/Ecore: 0 Treffer) | — |

Dazu die Klausel *"without manual workarounds"* aus Success-Kriterium 1.
Drei Punkte sind in [OPEN_ISSUES.md](OPEN_ISSUES.md) explizit als
`workaround` geführt und damit heute Teil des Demo-Ablaufs:

- **O1** — Startup-Race Publish ↔ JAX-RS-Wiring, umgangen per
  Retry-Loop (5×400 ms)
- **O3** — LAN-IPs hart in den Configs, Firewall manuell öffnen
- **C1 / W2** — `rm broker-state.xmi` zwischen Iterationswechseln

Der Flow läuft also, aber nicht workaround-frei.

---

## 4. Größte Lücke: es gibt keine Tests

Success-Kriterium 3 verlangt eine Testsuite, die Cross-Language-Parity
zusichert und auf einem Clean Checkout über `./gradlew test` / `npm test`
grün läuft.

**Es existiert kein einziger Test in beiden Tracks:**

| | |
|---|---|
| Java | alle acht `test/`-Ordner der Module sind angelegt und **leer**; `*Test.java`: 0 Treffer. Build-Output entsprechend: `test NO-SOURCE`, `testOSGi SKIPPED` in jedem Modul. |
| TypeScript | `vitest.workspace.ts` ist konfiguriert; `*.test.ts` / `*.spec.ts`: **0 Dateien**. |

Von allen Lücken ist das die, die am weitesten von "fast fertig" weg
ist, weil sie das Verifikationsfundament betrifft: die Aussage
"cross-language parity" wird derzeit ausschließlich durch manuelles
Log-Lesen gestützt. Erfasst als **T3** in
[OPEN_ISSUES.md](OPEN_ISSUES.md).

Sichtbare Folge: der `main`-Build war zwischen 2026-07-02 und
2026-08-23 rot bzw. ungebaut, ohne dass es auffiel — ohne Tests und
ohne beobachtetes CI gibt es kein Signal, wenn etwas wegbricht.

---

## 5. Design läuft der Implementierung voraus

**A5**–**A8** stehen auf `status: design`, nicht `open`:
Update-Policies, Channel-Modell, OpenAPI/AsyncAPI-Ingestion,
dynamischer stub-freier Consumer. Dazu vier Design-Dokumente
([UPDATE_POLICY](UPDATE_POLICY.md), [WIRE_CHANNELS](WIRE_CHANNELS.md),
[SECURITY](SECURITY.md), [TS_CLIENT_PLAN](TS_CLIENT_PLAN.md)) und ein
v2-Ecore.

Der Prototyp ist damit konzeptionell deutlich vor seinem Code. Das
erklärt die DoD-Lücke: die Energie ging in den Stufe-3/4-Entwurf,
während Stufe-2-DoD (MQTT, SSE, Tests) offen blieb. Als bewusste
Priorisierung vertretbar — sollte aber nicht mit Fortschritt am
DoD verwechselt werden.

---

## 6. Security

**S1**–**S9** offen. Hervorzuheben: **S3** (XMI-Parser ohne XXE-,
Entity-Expansion- und SSRF-Härtung) ist als *heute aktiv ausnutzbar*
eingestuft und betrifft den real laufenden Broker —
`XmiCodec.loadInto` lädt mit `Map.of()`, und `readBundle` ruft
`EcoreUtil.resolveAll` auf client-geliefertes XMI.

Die Härtung sitzt zentral in `xmi.codec` und schützt damit Server-
**und** Client-Seite. Details und Standards-Mapping in
[SECURITY.md](SECURITY.md).

---

## 7. Repo-Hygiene

Nicht Prototyp-Funktionalität, aber Reibung im Alltag:

| Thema | Stand 2026-08-23 |
|---|---|
| `main`-Build | grün (war 7 Wochen rot; Ursache war ein aufgeräumter `0.0.9-SNAPSHOT` der fennec.bnd-Libraries, jetzt auf Release `0.0.10` gepinnt) |
| License-Header-Check | rot — `452 files, valid: 68, invalid: 269` |
| Dependabot | 6–10 offene Findings auf `main` |
| DCO | verlangt `Signed-off-by`; nirgends im Repo dokumentiert (`git commit -s`) |

---

## 8. Empfohlene Reihenfolge

1. **S3 + erste Testebene zusammen.** S3 ist klein, zentral und
   aktiv ausnutzbar — und braucht zum Nachweis ohnehin Fixtures mit
   bösartigem XMI (XXE, Billion Laughs, `href` auf interne URL).
   Damit entsteht die Testinfrastruktur an echtem Inhalt statt an
   einem Spielzeug-Testfall.
2. **T3 ausbauen:** JUnit gegen `DdsrBrokerImpl`
   (publish / dedup / rewire / reindex) ohne OSGi-Runtime, gemäß der
   ohnehin vorgesehenen Teststrategie. Ab hier existiert ein Signal,
   das nicht von manuellem Log-Lesen abhängt.
3. **A1 (SSE).** DoD-Blocker, und **A5** (Update-Policy) hängt daran.
4. **A2 (MQTT).** Der größte Brocken, weil in zwei Sprachen. Schließt
   die restlichen DoD-Schritte.
5. **`OrderQuery` in den Katalog.** Klein, aber vom DoD ausdrücklich
   verlangt.
