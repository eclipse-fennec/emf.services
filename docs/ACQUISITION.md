# Discovery, Acquisition, Invocation — die drei Stufen und die Broker-Grenze

Design-Entwurf (Stufe 3, wie [UPDATE_POLICY.md](UPDATE_POLICY.md) und
[WIRE_CHANNELS.md](WIRE_CHANNELS.md) — spezifiziert, nicht
implementiert). Entstanden aus der Frage: *Wir wissen, wer eine
Implementation anbietet — aber ein Consumer, der sich eine Referenz
holt, konsumiert deshalb noch lange nicht. Wo gehört das Wissen über
tatsächliche Nutzung hin?*

## 1. Die drei Stufen und der Broker-Scope

OSGi kennt drei Stufen der Service-Nutzung. DDSR bildet sie so ab:

| Stufe | OSGi | DDSR | Broker-Rolle |
|---|---|---|---|
| **Discovery** | `getServiceReference` | `GET /references` | vollständig — implementiert |
| **Acquisition** | `getService` / `ungetService`, Use-Count | ConsumerSession + Leases (dieses Dokument) | vollständig — **entworfen** |
| **Invocation** | Methodenaufruf | Flavor-Pfad (REST/MQTT/…) | **keine** — peer-to-peer |

**Festlegung: der Broker unterstützt genau die ersten beiden Stufen.**
Für die Invocation ist er ausschließlich Vermittler der
Peer-to-Peer-Verbindungsinformation (die Flavors in der Service-
Beschreibung); der Aufruf selbst berührt den Broker nie. Das ist die
bestehende Architektur und bleibt so — ein Broker-as-Gateway wäre ein
anderes System.

Konsequenz, ehrlich benannt: **Acquisition ist ein kooperatives
Protokoll, keine Durchsetzung.** Ein Consumer, der die
Verbindungsinformation einmal hat, kann am Broker vorbei aufrufen. Die
Akquisitionsstufe kauft Drain-Semantik (`DEPRECATE_AND_DRAIN`),
Stale-Cleanup (OPEN_ISSUES C3) und Nutzungs-Telemetrie — nicht
Zugriffskontrolle. Enforcement wäre Sache der Provider-Seite bzw. der
Hook-Architektur (A3) und ist hier bewusst außen vor.

## 2. Wer weiß was — die Epistemik

- **„Wer bietet an"** weiß der Broker transaktional sicher (Publish ist
  bestätigt und persistiert).
- **„Wer hat gesucht"** weiß er anekdotisch (`consumerId` am Lookup).
- **„Wer will konsumieren"** ist die Lücke, die dieses Dokument füllt —
  als *Behauptung mit Verfallsdatum* (Lease), denn verteilt gibt es
  keinen erzwungenen Use-Count: ein abgestürzter Consumer zählt nicht
  selbst herunter.
- **„Wer konsumiert tatsächlich"** weiß in letzter Instanz nur der
  Provider an seinem Endpoint — und auch nur, wenn Invocations eine
  Consumer-Identität tragen (tun sie heute nicht; bewusst kein Teil
  dieses Entwurfs).

## 3. Modell: die Session besitzt die Lease, die Registration bekommt eine Sicht

### 3.1 Warum nicht an der `ServiceReference`

`ServiceReference` ↔ `ServiceRegistration` ist ein 1:1-Paar pro
publiziertem Service. „Viele und flüchtig" sind nicht die Referenzen,
sondern ihre **Wire-Kopien**: Die Referenz ist das consumer-sichtbare
Artefakt und wird in jedes Lookup-Ergebnis und jedes Event-Dokument
kopiert. Nutzungszustand an der Referenz würde also (a) in jeder
Serialisierung mitreisen und wäre veraltet, sobald er den Draht
berührt, (b) Schreiblast auf ein read-mostly-Objekt legen, (c) dem
Consumer eine Live-Sicht suggerieren, die keine ist.

`ServiceReference.usingProviders` ist darüber hinaus doppelt
unglücklich: falscher Ort (siehe oben) **und falscher Typ** — ein
Consumer ist nicht notwendig ein `ServiceProvider`. Das Feature wird
deprecated und durch das Folgende ersetzt (OPEN_ISSUES M5).

### 3.2 Warum nicht als Zähler an der `ServiceRegistration`

Ein gespeicherter `usageCount:int` driftet beim ersten Consumer-Crash —
niemand zählt herunter. **Die Wahrheit ist die Lease; der Count ist
eine Abfrage.**

### 3.3 Das Modell

```
ConsumerSession                       (containment: LocalServiceRegistry.sessions)
  consumerId    : String              (Identität, siehe §7)
  capabilities  : ConsumerCapability  (containment; greedy, supportedFlavors, …)
  lastRenewal   : Instant
  acquisitions  : ServiceRegistration[*]   (non-containment)

ServiceRegistration
  usingSessions : ConsumerSession[*]  (eOpposite zu acquisitions — abgeleitete Sicht)
```

- **Owner ist die Session**, denn der Lease-Lebenszyklus folgt dem
  Consumer, nicht dem Service: Ein Heartbeat erneuert *alle*
  Akquisitionen eines Consumers auf einen Schlag, ein Shutdown gibt
  alle frei, ein Crash lässt alle gemeinsam verfallen. Das dominante
  Ereignis „Consumer weg" trifft genau eine Session statt N
  Registrations. (Auch OSGi hält die Use-Counts im *BundleContext* des
  Nutzers; `getUsingBundles()` ist nur die aggregierte Sicht.)
- Die Drain-Frage von `DEPRECATE_AND_DRAIN` wird zur Abfrage der
  Gegenrichtung: `registration.usingSessions.isEmpty()`.
- Akquisitionen zeigen auf die **Registration** (stabil, 1:1 zur
  publizierten Implementation, überlebt die Referenz-Id-Regeneration
  beim Broker-Neustart *nicht* — siehe §6, das ist okay).

Die Ecore-Änderung (neue EClass `ConsumerSession`,
`LocalServiceRegistry.sessions`, eOpposite an `ServiceRegistration`,
Deprecation von `usingProviders`) erfolgt separat; Codegen macht wie
üblich der Modell-Owner.

## 4. Protokoll: ein idempotenter Endpoint statt drei Verben

Acquire, Release und Heartbeat kollabieren zu **einem idempotenten
Voll-Abgleich** — dasselbe Muster wie FR-Sync-Reconnect (Snapshot statt
Delta, Zustand statt Historie):

```
PUT    /consumers/{consumerId}    Body: ConsumerSession-XMI
                                  (capabilities + acquisitions als Referenz-Ids)
                                  → legt an oder ersetzt vollständig; erneuert die Lease
DELETE /consumers/{consumerId}    → Shutdown: gibt alle Akquisitionen sofort frei
GET    /consumers/{consumerId}    → Diagnose (was glaubt der Broker über mich?)
```

- **Acquire** = Ref in die lokale Liste aufnehmen, `PUT`.
  **Release** = Ref entfernen, `PUT`. **Heartbeat** = unverändertes
  `PUT` im Intervall. Ein Roundtrip pro Intervall, crash-sicher,
  reihenfolge-unempfindlich (letzter `PUT` gewinnt).
- **TTL:** Lease verfällt nach 2× Intervall ohne `PUT` (Werte wie in
  UPDATE_POLICY §4: 10 min / 20 min; konfigurierbar am Broker). Verfall
  gibt alle Akquisitionen der Session frei.
- **Liveness-Abkürzung:** Der Abriss der Event-Verbindung (SSE/MQTT-
  Session) *darf* den Verfall vorziehen — die offene Verbindung ist ein
  Gratis-Präsenzsignal und unterbietet den Timeout im Normalfall. Der
  Heartbeat bleibt die Wahrheit für Transporte ohne
  Verbindungssemantik. Umgekehrt gilt nicht: eine offene Verbindung
  ersetzt den `PUT` nicht (sie sagt „lebt", nicht „hält Ref X").
- Der Client hat alles schon: die `noteReference`-Map und
  `subscribedInterfaces()` des SDK sind genau die `acquisitions`-Liste;
  `close()`/Shutdown-Hooks rufen das `DELETE` (Java und TS symmetrisch,
  FR-P3-Ordnung: erst Withdraw/Release, dann Endpoint/Streams).
- **Wire-Realisierung (umgesetzt):** Das PUT-Dokument ist ein
  Multi-Root-XMI nach der Publish-Konvention — die `ConsumerSession`
  (consumerId + capabilities containment) plus
  **Geschwister-`ServiceReference`-Stubs, die nur ihre `id` tragen**;
  die Stub-Liste *ist* die Acquisition-Liste. Das Modell-Feature
  `acquisitions` ist transient und reist nie im XMI (ebenso
  `usingSessions` und das `reference`⟷`registration`-Paar: die
  Provider-Handles sind broker-seitig Laufzeitobjekte ohne
  Containment-Heimat — ein serialisierter Link würde jeden Snapshot
  zerreißen). `GET` antwortet formsymmetrisch. Der Pfad besitzt die
  Identität; eine widersprechende Body-Id ist ein 400. **Stale
  Acquire** (Ref-Id, die es nicht mehr gibt — etwa nach
  Broker-Neustart mit regenerierten Ids): wird übersprungen und im
  Diagnostic benannt, nie abgelehnt (§5).
- Nebengewinn: dieselbe Session-Struktur ist der natürliche Träger für
  die **Interessenlage der Event-Subscription** — das dokumentierte
  A2-Loch, dass `EventSource.open()` keine Interessen transportiert.

## 5. Semantik und Garantien

- **Over-Claiming ist harmlos** (ein Consumer hält Leases auf Refs, die
  er nie aufruft — kostet nur verzögertes Drain), **Under-Claiming
  schadet nur ihm selbst** (wer nicht akquiriert, verliert den
  Drain-Schutz: sein Service kann unter ihm weg-retired werden). Beides
  bewusst symmetrisch zur „lieber überzustellen als verwerfen"-Regel
  der Events.
- Der Broker darf Leases jederzeit vergessen (Neustart, §6). Consumer
  müssen mit `RETIRED`/`UNREGISTERING` trotz gehaltener Lease umgehen
  können — die Lease ist Schutz *im Rahmen der Policy*, kein Vertrag.

## 6. Persistenz: bewusst nicht

Sessions und Leases sind **Laufzeitzustand** und gehören nicht in
`broker-state.xmi`. Nach einem Broker-Neustart bauen die Consumer die
Map über ihre regulären `PUT`s selbst wieder auf — dieselbe
Philosophie wie FR-Sync-Reconnect, und es erspart das
Verfallsdatum-Problem beim Laden alter Snapshots. (Praktisch:
Referenz-Ids werden beim Reindex ohnehin regeneriert; persistierte
Leases zeigten ins Leere.)

**Folge-Regel für DEPRECATE_AND_DRAIN:** Direkt nach einem
Broker-Neustart ist die Session-Map bis zu einem Heartbeat-Intervall
lang leer. Auto-Retire darf „keine Nutzer mehr" daher erst glauben,
wenn der Broker mindestens ein volles Intervall läuft — sonst drained
ein Neustart versehentlich alles.

## 7. Sicherheit (Vorbehalt)

`consumerId` ist heute unauthentifiziert (S2). Ein idempotentes
`PUT /consumers/{id}` mit fremder Id **ersetzt fremde Sessions** —
damit ließe sich die Drain-Semantik stören (Leases fremder Consumer
löschen → vorzeitiges Retire) oder aufblähen. Für den Prototyp
akzeptiert und hier festgehalten; sobald S2 (AuthN) gelöst ist, wird
die Session an die authentifizierte Identität gebunden und `{id}`
gegen sie geprüft. In SECURITY.md als Ergänzung zu S2 zu vermerken,
sobald die Implementierung ansteht.

## 8. Registration als materialisierte Tatsache — die fehlenden Modell-Refs

Die Registrierung einer Implementation durch einen Provider
**materialisiert** eine `ServiceRegistration` — sie ist die dauerhafte
Tatsache „Provider P hat Implementation I publiziert". Heute trägt die
EClass diese Tatsache aber gar nicht: sie hat nur das
`reference`-eOpposite und `unregistered`; die Zuordnung zu Provider und
Implementation lebt im Broker als `implByRegistration`-**Side-Map**
(IdentityHashMap, mit dokumentiert unspezifizierter
Iterationsreihenfolge). Das gehört ins Modell:

```
ServiceRegistration
  provider       : ServiceProvider        [1]  (non-containment)
  implementation : ServiceImplementation  [1]  (non-containment)
  reference      : ServiceReference       [1]  (eOpposite, wie bisher)
  usingSessions  : ConsumerSession[*]          (eOpposite zu acquisitions, §3)
```

Damit ist das Bild symmetrisch: **Registration = Provider-Seite der
Nutzungsbeziehung, Session = Consumer-Seite** — beide referenzieren
ihre Identität non-containment, und die `ServiceReference` bleibt das
neutrale Wire-Artefakt dazwischen. Die Side-Map im Broker entfällt
ersatzlos (ihre Scans werden Modell-Navigation).

## 9. Abgeleitete Felder per OCL (fennec m2x)

Der Consumer-Count wird als **derived/volatile/transient**-Feature
modelliert, mit fennec-m2x-OCL-Annotation
(Namespace `http://www.eclipse.org/fennec/m2x/ocl/1.0` — das Ecore
nutzt ihn bereits für die `unregisteredNotInRegistry`-Invariante an
genau dieser Klasse):

```
ServiceRegistration.consumerCount : EInt  (derived, volatile, transient)
  ocl: self.usingSessions->size()
```

Das passt doppelt: derived+transient heißt **nie auf dem Wire** (genau
die §3.1-Anforderung), und „der Count ist eine Abfrage" wird wörtlich —
die Abfrage steht deklarativ im Modell statt imperativ im Broker.
Kosten, ehrlich: der Broker bekommt die OCL-Engine als
Laufzeit-Abhängigkeit, und es wäre die erste *aktive* OCL-Nutzung im
Projekt (M2 „Constraints nicht aktiv" würde in einem Aufwasch
angefasst). Die Engine bringt für wiederholte Auswertung ihren
`FingerprintExpressionCache` mit (m2x-Modul `ocl.fingerprint`,
Cache-Key nach demselben Modell-Fingerprint-Prinzip wie emf.osgi/sd1) —
Auswertungskosten pro Zugriff sind also beherrschbar. Fallback bleibt
eine schlichte Java-Ableitung; die OCL-Variante ist die modellierte.

## 10. Hot/Cold-Cache: Registrierungen ohne Consumer auslagern

Mit belastbarer Nutzungsinformation wird eine Speicher-Policy möglich:
**hält eine Registration für die Dauer T keine Session** (und gab es
für T keine Lookups auf ihr Interface), wandert sie von „hot"
(In-Memory-Registry) nach „cold" (Platte).

Die eine Regel, die das Design trägt: **kalt ≠ unauffindbar.** Ein
Service ohne Consumer muss discoverbar bleiben — sonst findet ihn nie
wieder jemand und kalt wäre für immer kalt. Deshalb bleibt pro kaltem
Eintrag ein kleiner **In-Memory-Stub** im Lookup-Index:

```
ColdEntry: interfaceName, implementationId, provider.name,
           sd1-Fingerprint (+ ggf. Impl-Fingerprint, §11),
           Pfad der Kalt-Datei
```

Trifft ein Lookup oder Acquire den Stub, wird der Eintrag lazy
rehydriert (XMI von Platte, zurück in Registry + Index) und ist wieder
hot. Die OSGi-Parallele ist die **delayed activation** von DS:
ein Service ohne Nutzer materialisiert seine Instanz nicht — der
Cold-Cache ist dasselbe Prinzip eine Ebene höher, auf der Registry.

Einordnung: für den Prototyp ist der Speichergewinn irrelevant (der
Katalog ist winzig); der Wert ist architektonisch — der Broker skaliert
mit der Katalog-, nicht mit der Hot-Set-Größe. Entwurf als **optionale
Policy** (Default aus), Umsetzung frühestens nach §3–§5.

## 11. Fingerprint-gestützter Reconnect und Contract-Adressierung

### 11.1 Komposition: der Impl-Fingerprint faltet die Vertrags-Fingerprints ein

sd1 identifiziert den **Vertrag** (`ServiceInterface`). Die
Implementation bekommt ein eigenes, separat eingefrorenes Schema
(Arbeitstitel `im1`), das **komponiert** statt neu zu traversieren —
das Merkle-Prinzip, exakt das `derivationInputs`-Muster aus emf.osgi:

```
im1(Impl) = H( sd1(SI₁), …, sd1(SIₙ),
               implementationId, Flavors/Endpoints, Properties )
```

Der Reconnect-Check eines Providers — *„hat der Broker meine
Registrierung noch, und unverändert?"* — wird damit dreistufig
aussagekräftig (Identitätsschlüssel ist `(provider.name,
implementationId)`, beides existiert heute):

| Vergleich | Bedeutung | Aktion |
|---|---|---|
| `im1` gleich | alles unverändert | nur Session/Lease erneuern (`PUT`, §4) |
| `im1` ungleich, alle `sd1` gleich | Endpoint-/Property-Drift | Re-Publish der Impl |
| ein `sd1` ungleich | **Contract**-Drift | Katalog-/Policy-Frage (UPDATE_POLICY), nicht bloß Re-Publish |

Consumer haben den sd1-Check heute schon: lokal berechneter Wert gegen
die `ddsr.fingerprint`-Reference-Property (die FR-P4-Harness prüft
genau das).

**Stand 2026-08-25: umgesetzt** (Branch feat/im1-fingerprint, Issue #6).
Das Schema ist als `im1` eingefroren — kanonische Grammatik im Javadoc
von `ServiceImplementationFingerprint` (xmi.codec, geteilt mit dem
Broker), TS-Spiegel `service-implementation-fingerprint.ts`; beide
Sprachen sind über die Goldens `itest/fixtures/fingerprint/
payment-impl.{xmi,canonical.txt,im1}` byte-identisch gepinnt.
Ausgeschlossen sind `description` und `componentDescription`
(Doku-/Deployment-Detail, nicht Endpoint-Identität). Der Broker
dekoriert jede Reference zusätzlich mit `ddsr.impl.fingerprint` —
berechnet NACH dem Katalog-Rewire, die `c|sd1:…`-Zeilen sind also
Katalogwahrheit. Der Identitätsvergleich beim Reconnect läuft rein
inhaltsbasiert: ein im1-Match unter gleichem `provider.name` IST die
eigene Registrierung (im1 enthält implementationId, Endpoints und die
sd1-Tokens). `publish()` ist damit in beiden SDKs idempotent: im1-Match
→ Publish übersprungen, Registration wiederverwendet, Consumer sehen
keinen UNREGISTERING/REGISTERED-Churn; Drift → Re-Publish (der Broker
retired den Alt-Eintrag), Richtung wird geloggt (sd1 gleich →
Endpoint-Drift INFO, sd1 ungleich → Contract-Drift WARNING — Publish
bleibt der sichere Default, der Broker validiert gegen den Live-Katalog).
Die Zeile „nur Lease erneuern" aus der Tabelle heißt praktisch: der
wiederverwendete Publish-Pfad kommt ohne Broker-Mutation aus, die
Session-Erneuerung (§4) läuft ohnehin.

### 11.2 Contract-Adressierung: Lookup und Katalog über `(name, sd1)`

Interface-Fingerprints tragen auf der Consumer-Seite mehr als den
Drift-Check: sie machen den Contract **adressierbar**.

- **Lookup:** `GET /references?interface=Payment&fingerprint=sd1:…` —
  oder besser: die `ConsumerCapability` deklariert die Contracts, die
  der Consumer *spricht* (seine Stubs sind aus einem konkreten SI-Stand
  generiert, den sd1 exakt benennt), und der Broker filtert jeden
  Lookup automatisch darauf — analog zu `supportedFlavors`.
- **Katalog:** der Schlüssel wird `(name, sd1)` statt `name`.
  Gleichnamige Interfaces mit unterschiedlicher Signatur/Properties
  sind dann schlicht verschiedene Katalog-Einträge, die koexistieren
  (heute: `CATALOG_ENTRY_ALREADY_EXISTS`); ein Consumer bekommt
  konstruktionsbedingt nur Implementierungen des Contracts, den er
  exakt kennt. Das entschärft nebenbei M1 (Namen nicht global
  eindeutig).

### 11.3 Abgrenzung: Identität gratis, Kompatibilität nicht

Was Fingerprints gratis liefern, ist **Identitäts-Versionierung**
(content-addressed contracts): jedes abweichende Detail trennt sauber.
Was sie **nicht** liefern können, ist Kompatibilitäts*semantik*:
Hashes sind ordnungslos — eine abwärtskompatible Ergänzung (neue
optionale Operation) ändert sd1 genauso stark wie ein harter Bruch.
„Consumer von 1.4 darf 1.5-Provider binden" kann kein Hash ausdrücken
(so auch die emf.osgi-Doku wörtlich: *not a version number, no
compatibility semantics*; konservativ falsch-verschieden, nie
falsch-gleich). Dafür bleiben deklarierte Versionen/Ranges
(UPDATE_POLICY §5) oder explizite Kompatibilitäts-Assertions im
Katalog („ersetzt sd1:X kompatibel" — eine Behauptung des Publishers).

Beides ergänzt sich: **die Version kommuniziert die Absicht, der
Fingerprint verifiziert die Realität.** Ein Range-Match, dessen
Fingerprint-Vergleich scheitert, ist ein gelogenes Versionslabel —
und wird damit sichtbar.

**Synergie mit §10:** sd1 und im1 liegen im Cold-Stub — die Frage
„noch da und unverändert?" beantwortet der Broker, ohne den kalten
Eintrag zu rehydrieren.

## 12. Umsetzungsreihenfolge

**Stand 2026-08-25: Schritte 1–3 sind umgesetzt** (Branch
feat/acquisition): Broker hält Sessions als Laufzeit-Map mit
TTL-Verfall (`org.eclipse.fennec.services.broker.core`,
`session.expiry.seconds` Default 1200, Sweep bei einem Viertel),
`PUT/GET/DELETE /consumers/{id}`, Side-Map `implByRegistration` durch
die Modell-Refs `registration.provider/.implementation` ersetzt
(deterministische Insertion-Order statt IdentityHashMap-Scan);
Withdraw/Republish **lösen die Leases der betroffenen Registration**
(Rollback bei Persist-Fehler stellt sie wieder her); SDKs beidseitig
(Java: `SessionsHttpProxy` + Renewal-Scheduler im Client-Component,
`session.interval.seconds` Default 600, DELETE im Shutdown vor dem
Stream-Close; TS: `putConsumerSession`/`deleteConsumerSession`/
`getConsumerSession` + Timer in `DdsrClientImpl`, gleiche
close()-Ordnung). Aus §11.2 ist die **Lookup-Contract-Adressierung**
umgesetzt: `GET /references?...&fingerprint=sd1:…` filtert exakt gegen
die broker-berechneten Katalog-Fingerprints (Java via
`ddsr.fingerprint`-Property an der ConsumerCapability, TS via
`find(interface, filter, fingerprint)`). Schritt 4 (Auto-Retire/Drain)
ist bewusst abgetrennt und kommt mit der Policy-Maschinerie.

**Stand 2026-08-25, Nachtrag:** aus Schritt 5 ist der
**Fingerprint-Reconnect für Provider** umgesetzt (im1, §11.1 — Branch
feat/im1-fingerprint); offen aus Schritt 5 bleibt der Cold-Cache (§10),
aus §11.2 der Katalog-Schlüssel `(name, sd1)` (Issue #6-Rest).

1. Ecore: `ConsumerSession`, `LocalServiceRegistry.sessions`,
   eOpposite `ServiceRegistration.usingSessions`, dazu
   `ServiceRegistration.provider`/`.implementation` (§8, ersetzt die
   `implByRegistration`-Side-Map) und Deprecation
   `ServiceReference.usingProviders` (M5) — Codegen durch Modell-Owner.
   Optional im selben Zug: `consumerCount` als OCL-derived Feature (§9).
2. Broker: die drei Endpoints + TTL-Verfall (ein Scheduler, analog zum
   SSE-Heartbeat) + Verbindungsabriss-Hook.
3. SDK Java + TS symmetrisch: `PUT` aus der bestehenden
   Ref-Buchführung, `DELETE` in den FR-P3-Shutdown-Pfad.
4. Dann erst konsumieren: `DEPRECATE_AND_DRAIN`-Auto-Retire und
   C3-Cleanup aus UPDATE_POLICY auf die Session-Map umstellen.
5. Danach optional: Fingerprint-Reconnect für Provider (§11, braucht
   nur die vorhandenen sd1-Properties + Identitätsvergleich) und
   zuletzt der Cold-Cache (§10, eigene Policy).
