# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projekt

**Eclipse Fennec Services** (Arbeitsname DDSR — Dynamic Distributed Service Registry). Cross-Language Service-Registry (Java + TypeScript): sprachunabhängiges, EMF/Ecore-basiertes OSGi-Service-Modell, Broker mit REST/SSE- und MQTT-Event-Transport, SDKs für Provider und Consumer in beiden Sprachen, Cross-Language-Harness. Migriert 2026-08-25 aus dem Prototyp-Repo `geckoprojects-org/ddsr` (Quelle: Branch `feat/ts-parity`); Tracking: Issue #1.

Maven groupId: `org.eclipse.fennec.services` (Quelle: `gradle.properties`, Single Source für Gradle und bnd). Default-Branch ist **`snapshot`**. Java **21**.

**Zwei Migrations-Festlegungen, die Code-Arbeit hier einschränken:**

1. **Wire-Namen sind eingefroren, bis der TS-Namespace-Rename kommt (Issue #4):** `ddsr.fingerprint`, `ddsr.provider.name/lang`, `ddsr.broker.transport`, Header `X-DDSR-Requestor`, MQTT-Topic-Prefix `ddsr/events`, SSE-Event-Name `ddsr-service-event`, Context-Path `/ddsr/rest`. Sie sind Cross-Language-Verträge mit dem TS-Code — einseitig umbenennen bricht die Harness.
2. **Der TS-Track behält sein Original-Naming** (`@ddsr/*`, `DDSRPackage`, `org/gecko/ddsr`-Pfade, `model/ddsr.ecore` als Dateiname) — Umbenennung macht ein Kollege separat. Der ECORE-INHALT ist synchron zum Java-Modell (nsURI `http://eclipse.org/fennec/services/1.0`, nsPrefix `services`); kanonische Quelle ist `org.eclipse.fennec.services.model/model/services.ecore`, die TS-Kopie wird von dort gesynct + mit `pnpm --filter @ddsr/model generate` regeneriert.

## Begleit-Dokumentation

Inhalt lebt in `docs/`, nicht hier. Bei Widerspruch gilt: **ARCHITECTURE → CLIENT_FRAMEWORK_GUIDE**; die Harness schlägt beide, weil sie den Zustand tatsächlich ausführt.

**Benutzer-Guide** (englisch, neu mit #111) — für jemanden, der Fennec Services *benutzen* will:

- [docs/guide/README.md](docs/guide/README.md) — Einstieg: welche Seite für welche Aufgabe
- [docs/guide/01-architecture.md](docs/guide/01-architecture.md) — die drei Parteien, warum das Modell der Vertrag ist, Bundle-Landkarte
- [docs/guide/02-eventing.md](docs/guide/02-eventing.md) — SSE und MQTT, was ein Consumer garantiert sieht und was nicht
- [docs/guide/03-fingerprints.md](docs/guide/03-fingerprints.md) — sd1 und im1, was einen Fingerprint ändert
- [docs/guide/04-code-generation.md](docs/guide/04-code-generation.md) — Ecore nach src-gen, Arbeitsteilung am Modell
- [docs/guide/05-transports.md](docs/guide/05-transports.md) — was mitgeliefert wird und was ein Deployment konfigurieren muss
- [docs/guide/06-rsa.md](docs/guide/06-rsa.md) — OSGi Remote Service Admin auf dieser Registry

**Interne Dokumente** — für uns geschrieben, deutsch:

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — Bundle-Layout, Wire-Format, Architektur-Entscheidungen, Demo-Reproduktion
- [docs/CLIENT_FRAMEWORK_GUIDE.md](docs/CLIENT_FRAMEWORK_GUIDE.md) — Java-Provider/-Consumer schreiben
- [docs/ACQUISITION.md](docs/ACQUISITION.md) — Discovery/Acquisition/Invocation, ConsumerSession/Lease-Modell, im1/Cold-Cache
- [docs/UPDATE_POLICY.md](docs/UPDATE_POLICY.md) — Update-Policies, Heartbeat (präzisiert durch ACQUISITION.md)
- [docs/WIRE_FORMAT.md](docs/WIRE_FORMAT.md) — Draht-Dokumente und ihre Felder
- [docs/WIRE_CHANNELS.md](docs/WIRE_CHANNELS.md) — Channel-Modell (v2-Design)
- [docs/FINGERPRINTS.md](docs/FINGERPRINTS.md) — sd1/im1 im Detail
- [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md) — vom frischen Checkout zu Broker, Provider und Consumer
- [docs/EXAMPLE_PAYMENT.md](docs/EXAMPLE_PAYMENT.md) — das Payment-Beispiel
- [docs/HARNESS.md](docs/HARNESS.md) / [itest/README.md](itest/README.md) — Cross-Language-Harness (Host + Podman + Mosquitto)
- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) — Broker-Container-Image, Konfigurationsfläche, Publish-Pipeline

## Module

- `org.eclipse.fennec.services.model` — EMF-Codegen aus `model/services.ecore` nach **`src-gen`** (fennecEMF `-generate`; das persistierte GenModel wird NICHT auto-reconciled — bei Ecore-Änderungen manuell mitpflegen)
- `org.eclipse.fennec.services.broker.api` — nur die API: vier Rollen-Interfaces, Diagnostics, EventSink/EventDocument, LookupBackend, Exceptions. Keine Komponenten. Wer nur mit einem Broker *spricht*, nimmt dieses Bundle (#105)
- `org.eclipse.fennec.services.shutdown` — ein Component: stoppt das Framework bei SIGTERM, damit jedes `@Deactivate` noch laeuft. Gehoert in **jeden** Launch; lag vorher in `broker.core` und arbeitete dort unbemerkt fuer jeden, der das Bundle versehentlich mitzog
- `org.eclipse.fennec.services.broker.core` / `broker.rest` / `broker.mqtt` — der Broker selbst, JAX-RS/SSE, MQTT-EventSink. `DdsrBrokerImpl` ist seit #110 nur noch Fassade; dahinter je ein Belang (`BrokerState`, `Registrations`, `CatalogStore`, `Lookups`, `UpdatePolicies`, `Liveness`, `Sessions`, `ColdCache`, `Announcements`), und `Retirement`/`Republication` sind die zwei Nähte, über die sie einander aufrufen
- `org.eclipse.fennec.services.xmi.codec` — XMI-Wire-Codec + `…services.fingerprint` (sd1)
- `org.eclipse.fennec.services.client.java` / `client.rest` / `client.mqtt` — transport-agnostisches SDK + Transporte
- `org.eclipse.fennec.services.derive` — Vertrag aus einem Java-Interface ableiten (Reflection, deterministisch: stabiler sd1); Baustein für die RSA-Facade (#24)
- `org.eclipse.fennec.services.rsa` / `rsa.distribution.rest` / `rsa.discovery.rest` / `rsa.discovery.local` / `rsa.topology` / `rsa.config` — OSGi Remote Service Admin auf DDSR (#24); der OSGi-RSA-TCK läuft grün (`org.eclipse.fennec.services.rsa.tck`, `./itest/run-tck.sh`): Kern mit zwei SPIs (`FlavorDistribution`, `ServiceDiscovery`), je Flavor ein Provider über RSA-Config-Types, lokale Service-Registry auf der EObject-Registry; `examples.rsa.api` / `examples.rsa` / `examples.rsa.consumer` sind der Beweis: ein simpler OSGi-Service, in einem Framework exportiert und in einem anderen per `@Reference` gebunden — ohne Vertragsdokument. Importierte Proxies registriert ein leeres Host-Bundle (`ProxyHost`), nicht das RSA-Bundle: das scheiterte am Class-Space-Check. Konfiguriert wird ein Knoten über **eine** Rolle (`rsa.config`: PID `…rsa.provider` bzw. `…rsa.consumer`), aus der eine Konfigurationskomponente die einzelnen PIDs ableitet, prüft, in fester Reihenfolge schreibt und in der Gegenrichtung abräumt (#109) — die neun handgeschriebenen Konfigurationen sind damit Implementierungsdetail
- `org.eclipse.fennec.services.flavor.rest` — die REST-Platzierungsregeln, geteilt von Consumer, Dispatcher und Template (kein JAX-RS)
- `org.eclipse.fennec.services.provider.rest` — generische REST-Distribution: eine Factory-Konfiguration serviert einen Vertrag aus seinem Modell und meldet ihn optional selbst an (#84)
- `org.eclipse.fennec.services.examples.payment` / `examples.model` — Demo-Provider + Beispielmodell
- `ddsr-ts-client/` — TypeScript-Track (pnpm-Workspace; in `gradle.properties` via `bnd_exclude` vom bnd-Build ausgenommen, ebenso `itest`)
- `itest/` — Harness: `run-harness.sh` (Host) und `run-harness-podman.sh` (Container inkl. Mosquitto für den MQTT-Drahtnachweis)
- `docker/broker/` — Build-Kontext des deploybaren Broker-Images (`Dockerfile` + `prepareDocker`-Staging); in `gradle.properties` via `bnd_exclude` vom bnd-Build ausgenommen. Gebaut und gepusht von `.github/workflows/reusable-container.yml` (siehe [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md))

## Build & Test

BND-Workspace (bnd 7.4.0-Snapshot, siehe gradle.properties), Gradle als Treiber, fennec-Libraries (`cnf/ext/fennec.bnd`), Maven-Index `cnf/ext/central.mvn`.

```bash
./gradlew build                 # voller Build inkl. Tests
./gradlew :<module>:test        # Tests eines Moduls
./gradlew :<module>:generate    # EMF-Codegen anstoßen
cd ddsr-ts-client && corepack pnpm install && corepack pnpm -r build && corepack pnpm exec vitest run
./itest/run-harness.sh          # Cross-Language End-to-End (Host)
./itest/run-harness-podman.sh   # dito als Container + MQTT-Drahtnachweis

# Deploybares Broker-Image lokal bauen (CI macht dasselbe mit dem publizierten Jar)
./gradlew :org.eclipse.fennec.services.broker.rest:export.broker :docker:broker:prepareDocker
podman build -t emf.services/broker:local docker/broker/
```

IP-Prüfung (Eclipse Dash): `tools/dash-licenses.sh` erzeugt `DEPENDENCIES` aus den Maven-Koordinaten des Workspace; `--review` öffnet IP-Anträge (braucht `DASH_IPLAB_TOKEN` und `DASH_PROJECT_ID`). Die Liste ist noch der Repository-Index, nicht der Auflösungs-Closure — siehe Issue #90.

License-Header-Check wie die `license.yml`-Action: `docker run -it --rm -v $(pwd):/github/workspace ghcr.io/apache/skywalking-eyes/license-eye header check` — Header im Eclipse-Foundation-Stil (siehe `.licenserc.yaml`); `itest/fixtures/**` ist ausgenommen (Golden-Dateien sind byte-genau hash-gepinnt — nie editieren ohne die sd1-Spezifikation zu bedenken).

## Konventionen

- **`src-gen`-Ordner sind generierter Code** — nie von Hand editieren; aus `.ecore`/`.genmodel` regenerieren. Ecore-Änderungen macht grundsätzlich der Modell-Owner (User) bzw. nur auf dessen explizite Anweisung.
- **Im Java-Code immer `import`-Anweisungen verwenden** — nie voll qualifizierte Klassennamen im Code-Body.
- **Logging mit JUL** (`java.util.logging.Logger`), nicht `System.Logger`/slf4j; `System.out/err` nur begründet auf JVM-Shutdown-Pfaden (JULs Cleanup-Hook resettet den LogManager — DECISIONS_PARITY D14) und in Demos. INFO = Lifecycle, WARNING = Probleme, FINE = Diagnose.
- **Keine `Bundle-Version` in bnd.bnd** — die fennec-Library besitzt die Version (`base-version` in `cnf/ext/fennec.bnd`).
- **Commits immer mit** `Signed-off-by: Mark Hoffmann <m.hoffmann@datainmotion.com>` (Eclipse DCO).
- Teststrategie: plain JUnit 5 + AssertJ (Fakes oder Mockito — beides erlaubt), OSGi-Runtime nur wo OSGi selbst getestet wird; TS: vitest mit Fakes über die `fetchFn`-/Client-Seams. Die Harness ist der End-to-End-Anker.
