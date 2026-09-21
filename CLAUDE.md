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

**Alles in `docs/` ist englisch** (Konvention des Users, 2026-09-20), ebenso `itest/README.md` und die Modell-Spezifikation. Nur diese Datei hier darf deutsch bleiben.

**Benutzer-Guide** — fuer jemanden, der Fennec Services *benutzen* will:

- [docs/OVERVIEW.md](docs/OVERVIEW.md) — Einstieg: welche Seite fuer welche Aufgabe
- [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md) — vom frischen Checkout zu Broker, Provider und Consumer
- [docs/TRANSPORTS.md](docs/TRANSPORTS.md) — was mitgeliefert wird, was ein Deployment konfigurieren muss, und seit #100: wie der Vertrag die Payload-Kodierung waehlt
- [docs/EVENTING.md](docs/EVENTING.md) — SSE und MQTT, was ein Consumer garantiert sieht und was nicht
- [docs/FINGERPRINTS.md](docs/FINGERPRINTS.md) — sd1 und im1, was einen Fingerprint bewegt
- [docs/CODE_GENERATION.md](docs/CODE_GENERATION.md) — Ecore nach src-gen, Arbeitsteilung am Modell
- [docs/RUNTIME.md](docs/RUNTIME.md) — die beiden Runtime-Services: was ein Knoten hält, und wie man von Änderungen erfährt
- [docs/TELEMETRY.md](docs/TELEMETRY.md) — OpenTelemetry: die CallTracer-Naht, wo der Trace-Kontext reist, was Spans und Gauges zeigen
- [docs/RSA.md](docs/RSA.md) — OSGi Remote Service Admin auf dieser Registry
- [docs/EXAMPLE_PAYMENT.md](docs/EXAMPLE_PAYMENT.md) — das Payment-Beispiel
- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) — Broker-Container-Image, Konfigurationsflaeche, Publish-Pipeline

**Vertiefung:**

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — Bundle-Layout, Architektur-Entscheidungen, Demo-Reproduktion
- [docs/CLIENT_FRAMEWORK_GUIDE.md](docs/CLIENT_FRAMEWORK_GUIDE.md) — Java-Provider/-Consumer schreiben
- [docs/ACQUISITION.md](docs/ACQUISITION.md) — Discovery/Acquisition/Invocation, ConsumerSession/Lease-Modell, im1/Cold-Cache
- [docs/UPDATE_POLICY.md](docs/UPDATE_POLICY.md) — Update-Policies, Heartbeat (praezisiert durch ACQUISITION.md)
- [docs/WIRE_FORMAT.md](docs/WIRE_FORMAT.md) — Draht-Dokumente, ihre Felder und die Payload-Kodierung
- [docs/WIRE_CHANNELS.md](docs/WIRE_CHANNELS.md) — Channel-Modell (v2-Design)
- [docs/HARNESS.md](docs/HARNESS.md) / [itest/README.md](itest/README.md) — Cross-Language-Harness (Host + Podman + Mosquitto)

## Module

- `org.eclipse.fennec.services.model` — EMF-Codegen aus `model/services.ecore` nach **`src-gen`** (fennecEMF `-generate`; das persistierte GenModel wird NICHT auto-reconciled — bei Ecore-Änderungen manuell mitpflegen)
- `org.eclipse.fennec.services.api` — der Vertrag, auf den sich alles andere einigt: vier Rollen-Interfaces, Diagnostics, EventSink/EventDocument, LookupBackend, Exceptions, dazu die sd1/im1-Fingerprints, die Flavor-Konvention und `JavaContracts` (Vertrag aus einem Java-Interface ableiten, deterministisch). Keine Komponenten, keine Konfiguration. Bewusst **ohne** den XMI-Codec: ein Fingerprint wird über dem Modell im Speicher gerechnet, nie über serialisierten Bytes — und genau das erlaubt, den Codec nach emf.osgi zu spenden, ohne die eingefrorenen Draht-Schemata mitzugeben. `FrameworkShutdown.installFor` liegt hier als Hilfsklasse; installiert wird der Hook von den zwei Komponenten, die ohnehin einen Lebenszyklus führen (Broker und Client)
- `org.eclipse.fennec.services.broker.core` / `broker.rest` / `broker.mqtt` — der Broker selbst, JAX-RS/SSE, MQTT-EventSink. `DdsrBrokerImpl` ist seit #110 nur noch Fassade; dahinter je ein Belang (`BrokerState`, `Registrations`, `CatalogStore`, `Lookups`, `UpdatePolicies`, `Liveness`, `Sessions`, `ColdCache`, `Announcements`), und `Retirement`/`Republication` sind die zwei Nähte, über die sie einander aufrufen. Alle drei Broker-Verträge werden seit #88 generisch serviert — die Regel, was mit einer Antwort reist, macht die mehrwurzelige Lookup-Antwort beschreibbar, `BrokerLookupRest` ist der Adapter mit der Signatur, die der Vertrag beschreibt. Handgeschrieben bleibt nur `EventsResource`: SSE ist kein Aufruf
- `org.eclipse.fennec.services.cloudevents` — die Hülle: CloudEvents 1.0 über dem `io.cloudevents.model`-Ecore, beide Content-Modi (structured JSON für MQTT/SSE, `ce-*`-Header über HTTP). Kennt CloudEvents und nichts über Payload-Kodierung — die bleibt die Wahl des Vertrags (#100); die Nutzlast reist als Bytes, weil ein Event-Dokument mehrwurzelig ist und `data : EObject` nur eines hält
- `org.eclipse.fennec.services.xmi.codec` — Wire-Codec: die Kodierung wird seit #100 per Content-Type aus der ResourceSet-Factory-Registry gewählt (XMI als Vorgabe, Protobuf über emf.util), ein nicht registrierter Content-Type wird abgelehnt statt still als XMI geschrieben; die Härtung hängt am Format, nicht am Codepfad. Der Klassenname ist zu eng und bleibt vorerst
- `org.eclipse.fennec.services.client.java` / `client.rest` / `client.mqtt` — transport-agnostisches SDK + Transporte
- `org.eclipse.fennec.services.rsa` / `rsa.distribution.rest` / `rsa.distribution.mqtt` / `rsa.discovery.rest` / `rsa.discovery.mqtt` / `rsa.discovery.local` / `rsa.topology` / `rsa.config` — OSGi Remote Service Admin auf DDSR (#24); der OSGi-RSA-TCK läuft grün (`org.eclipse.fennec.services.rsa.tck`, `./itest/run-tck.sh`): Kern mit zwei SPIs (`FlavorDistribution`, `ServiceDiscovery`), je Flavor ein Provider über RSA-Config-Types, lokale Service-Registry auf der EObject-Registry; `examples.rsa.api` / `examples.rsa` / `examples.rsa.consumer` sind der Beweis: ein simpler OSGi-Service, in einem Framework exportiert und in einem anderen per `@Reference` gebunden — ohne Vertragsdokument. Importierte Proxies registriert ein leeres Host-Bundle (`ProxyHost`), nicht das RSA-Bundle: das scheiterte am Class-Space-Check. Konfiguriert wird ein Knoten über **eine** Rolle (`rsa.config`: PID `…rsa.provider` bzw. `…rsa.consumer`), aus der eine Konfigurationskomponente die einzelnen PIDs ableitet, prüft, in fester Reihenfolge schreibt und in der Gegenrichtung abräumt (#109) — die neun handgeschriebenen Konfigurationen sind damit Implementierungsdetail
- `org.eclipse.fennec.services.telemetry` — OpenTelemetry hinter den beiden Nähten (#126): `CallTracer` (W3C-Kontext über REST-Header bzw. CloudEvents-Extensions), die Runtime-Services als Gauges und die JUL-Brücke, die Logzeilen mit dem Span korreliert. Bringt eigene W3C-Propagatoren mit, weil die OSGi-Technology-Integration einen Noop registriert; die otel-Bundles kommen als Snapshot von Central
- `org.eclipse.fennec.services.flavor.rest` — die REST-Platzierungsregeln, geteilt von Consumer, Dispatcher und Template (kein JAX-RS)
- `org.eclipse.fennec.services.flavor.mqtt` / `provider.mqtt` — dasselbe Paar für MQTT (#98): die Regeln (Topics, QoS, Nachrichtenform) ohne Paho, und die Distribution, die einen Vertrag auf Topics serviert. Seit #25 auch **konfigurationsgetrieben**: `GenericMqttDistribution` ist der Zwilling von #84 — eine Factory-Konfiguration serviert einen Vertrag aus seinem Modelldokument und meldet ihn optional an; `mqtt.broker` ist die einzige Deployment-Angabe, Topics und QoS stehen im Vertrag. Beim Herunterfahren erst zurückziehen, dann die Subscription schließen (FR-P3 auf diesem Transport). Antwort-Topics tragen ein Consumer-Segment, Request und Response liegen in getrennten Bäumen — erst damit ist eine Broker-ACL schreibbar
- `org.eclipse.fennec.services.provider.rest` — generische REST-Distribution: eine Factory-Konfiguration serviert einen Vertrag aus seinem Modell und meldet ihn optional selbst an (#84)
- `org.eclipse.fennec.services.m2t` — die Templates sind das Produkt, die eingecheckte Ausgabe beweist nur, dass sie kompilieren: Interface je Vertrag, JAX-RS-Ressource je RestFlavor und seit #25 `service-publisher.mtl` — eine Komponente je Implementierung, die in der FR-P3-Reihenfolge anmeldet und zurückzieht und den Vertrag **nicht** in Code wiederholt, sondern aus dem Dokument liest. Zwei Fallen beim Erweitern: eine Generation braucht ein eigenes `output`-Verzeichnis (bnd leert es vorher) und ein Template sieht nur, was sein Modul `extends` — ein Query aus einem anderen Template ist kein Fehler, sondern erzeugt stillschweigend gar nichts
- `org.eclipse.fennec.services.examples.payment` / `examples.model` / `examples.persons` — Demo-Provider + Beispielmodell + der Consumer des `PersonStore`-Vertrags, dessen Rumpf ein Modell ist und dessen Kodierung Protobuf (#100); eigenes Bundle, weil das SDK nicht von einem Beispielmodell abhängen darf und der Provider seine Konfiguration mitbringt
- `org.eclipse.fennec.services.telemetry.rest` — Whiteboard-Extension (`ContainerRequestFilter`/`ContainerResponseFilter`), die handgeschriebenen JAX-RS-Ressourcen denselben Server-Span gibt, den der generische Dispatcher schon hat. Trifft die Default-Application, also genau dort, wo eine blanke `@JakartarsResource` landet — und bewusst **nicht** die benannten Applications der generischen Distribution (die bringen eigene Provider mit, #125), damit ein Aufruf einmal getract wird. `text/event-stream` wird ausgelassen: ein Abonnement ist kein Aufruf
- `org.eclipse.fennec.services.demo.telemetry` / `demo.traffic` — Demo-Gerüst für die Präsentation: die Konfiguration, die das OTel-SDK auf den Collector zeigen lässt (ein Bundle, weil `configurator.initial` zu früh gelesen wird), und ein Consumer, der auf einem Timer ruft (`ConfigurationPolicy.REQUIRE`, also still, wo ihn niemand konfiguriert). Die zugehörigen Launches sind `broker-otel.bndrun`, `payment-provider-otel.bndrun`, `client-otel.bndrun` — je ein `-include` des echten Launches plus ein handgepflegtes `-runbundles.otel`-Delta (ein Resolve würde die absichtlich mitgeführten Bundles verwerfen)
- `demo/observability/` — der Stack (Collector, Tempo, Prometheus, Loki, Grafana als podman-Pod, aus dem OSGi-Technology-Projekt übernommen) plus `run-demo.sh`, das Stack und alle drei Frameworks startet; in `gradle.properties` via `bnd_exclude` vom bnd-Build ausgenommen
- `ddsr-ts-client/` — TypeScript-Track. Provider-Seite seit #154 ohne Transportcode: `restDispatcher(flavor, objekt)` liest jedes Argument dort, wo der Flavor es hinschreibt (der Zwilling von `RestDispatcher`), `operationHandlers(vertrag, objekt)` leitet die MQTT-Handler aus dem Vertrag ab. Was dort **noch** in Code steht und in Java nicht: der Vertrag selbst (`payment-api.ts` baut ihn mit der Factory) — die zweite Hälfte von #154. Telemetrie-Naht wie Java (#146): `@ddsr/telemetry` (nur die Naht, keine Abhängigkeiten), `@ddsr/telemetry-otel` (OTLP/HTTP über das Node-SDK), `withTracing` als Zwilling von `RestTransport.send` und ein Span um jede Invocation im REST-Flavor-Plugin. Weil beide Seiten denselben W3C-Header schreiben und lesen, ist ein Aufruf über die Sprachgrenze **ein** Trace — `examples/payment/demo-traffic.ts` zeigt es. Unterschied zur JVM: einen Kontext betritt man in JS nur per Callback, deshalb `telemetry.during(name, fn)` statt try-with-resources (pnpm-Workspace; in `gradle.properties` via `bnd_exclude` vom bnd-Build ausgenommen, ebenso `itest`)
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

IP-Prüfung (Eclipse Dash): `tools/dash-licenses.sh` erzeugt `DEPENDENCIES`; `--review` öffnet IP-Anträge (braucht `DASH_IPLAB_TOKEN` und `DASH_PROJECT_ID`). Seit #90 ist die Liste **das, was der Build benennt**, nicht mehr der Repository-Index: `tools/used-dependencies.py` liest jede `-buildpath`/`-testpath`/`-runbundles`-Angabe des Workspace samt der Templates der eingeschalteten `-library`s und filtert die Koordinaten darauf (147 von 464, 2 statt 31 `restricted`). Ein Bundle, das benannt wird und zu keiner Koordinate passt, wird gemeldet — das ist die Form, die ein Fehler dort annimmt.

License-Header-Check wie die `license.yml`-Action: `docker run -it --rm -v $(pwd):/github/workspace ghcr.io/apache/skywalking-eyes/license-eye header check` — Header im Eclipse-Foundation-Stil (siehe `.licenserc.yaml`); `itest/fixtures/**` ist ausgenommen (Golden-Dateien sind byte-genau hash-gepinnt — nie editieren ohne die sd1-Spezifikation zu bedenken).

## Konventionen

- **`src-gen`-Ordner sind generierter Code** — nie von Hand editieren; aus `.ecore`/`.genmodel` regenerieren. Ecore-Änderungen macht grundsätzlich der Modell-Owner (User) bzw. nur auf dessen explizite Anweisung.
- **Im Java-Code immer `import`-Anweisungen verwenden** — nie voll qualifizierte Klassennamen im Code-Body.
- **Logging mit JUL** (`java.util.logging.Logger`), nicht `System.Logger`/slf4j; `System.out/err` nur begründet auf JVM-Shutdown-Pfaden (JULs Cleanup-Hook resettet den LogManager — DECISIONS_PARITY D14) und in Demos. INFO = Lifecycle, WARNING = Probleme, FINE = Diagnose.
- **Keine `Bundle-Version` in bnd.bnd** — die fennec-Library besitzt die Version (`base-version` in `cnf/ext/fennec.bnd`).
- **Commits immer mit** `Signed-off-by: Mark Hoffmann <m.hoffmann@datainmotion.com>` (Eclipse DCO).
- Teststrategie: plain JUnit 5 + AssertJ (Fakes oder Mockito — beides erlaubt), OSGi-Runtime nur wo OSGi selbst getestet wird; TS: vitest mit Fakes über die `fetchFn`-/Client-Seams. Die Harness ist der End-to-End-Anker.
