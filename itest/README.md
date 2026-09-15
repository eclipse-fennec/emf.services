# itest — Cross-Language-Harness (FR-P4)

Automatisierte End-to-End-Proben beider Sprach-Tracks gegen einen
echten Broker. Anforderungen und Entscheidungen:
[docs/DECISIONS_PARITY.md](../docs/DECISIONS_PARITY.md) (D10, D14–D16).

## Szenarien

- **A — Java-Provider → TS-Consumer:** Discovery, sd1-Fingerprint
  (Broker-Property = lokal berechnet = Golden-Hash), alle acht
  Property-Typen typgenau, Invocation, und die FR-P3-Probe: das
  UNREGISTERING erreicht den Consumer, **bevor** der Provider-Prozess
  endet (SIGTERM → FrameworkShutdownHook → blockierender Withdraw →
  Broker-Fan-out → Prozessende).
- **B — TS-Provider → Java-Consumer:** `client.bndrun`
  (TsPaymentDebug) ruft getBalance/charge; beim SIGTERM des
  TS-Providers: Withdraw vom Broker bestätigt **vor** dem Stopp des
  HTTP-Endpoints, Broker listet den Provider danach nicht mehr.
- **C (nur Podman) — MQTT-Drahtnachweis:** Mosquitto-Container; die
  TS-`MqttEventSource` empfängt und dekodiert das Broker-förmige
  Event-Dokument über echtes MQTT/TCP (Topics `<prefix>/<interface>`
  und `<prefix>/_unknown`).

- **F — gleiche Identität startet auf neuem Port:** zweite Instanz des
  Java-Providers mit gleicher `(name, version)` auf Port 9092, während
  die TS-Probe einen Tracked Locator und eine Lease hält. Seit #55 ist
  das ein Modify in place: erwartet wird `MODIFIED` unter derselben
  Referenz-ID, der Locator übernimmt den Endpoint aus dem Event, der
  nächste Aufruf erreicht den neuen Port, die Lease bleibt (#55, #57, #58).
- **G — `DEPRECATE_AND_DRAIN`:** Version 2.0.0 publiziert mit
  `replaces=1.0.0`, die Probe hält eine Lease auf 1.0.0. Erwartet:
  `UPGRADE_AVAILABLE`, Lookups liefern nur den Nachfolger, der Vorgänger
  antwortet weiter, nach `DELETE /consumers/{id}` retired der Policy-Sweep
  ihn (`UNREGISTERING/REPLACED` + `RETIRED`), der Locator wechselt (#45, #58).

Der Payment-Provider ist dafür per Umgebung konfigurierbar
(`PAYMENTS_HTTP_PORT`, `PAYMENTS_PUBLIC_URL`, `PAYMENTS_IMPL_VERSION`,
`PAYMENTS_UPDATE_POLICY`, `PAYMENTS_REPLACES_VERSION`, `DDSR_BROKER_URL`;
`configs/config.json` mit ConfigAdmin-Interpolation).

## Läufe

```bash
./itest/run-harness.sh          # Host-Prozesse (kein Podman nötig): A + B + F + G
./itest/run-harness-podman.sh   # Container (podman, --network=host): A + B + C + D + E + F + G
```

Beide bauen zuerst (`./gradlew build` + bnd-Exporte, `pnpm install` +
Build). Die Export-Jars werden vor dem Export gelöscht: der bnd-Export
trackt Cross-Projekt-Bundle-Änderungen nicht und würde sonst veraltete
Bundles einbetten. Logs landen in `itest/work/` bzw.
`itest/work-podman/` (gitignored); bei Timeout wird zusätzlich ein
Thread-Dump in das gesicherte Container-Log geschossen.

## Container-Images (`containers/`)

- `Containerfile.java` — generisch für jedes exportierte bnd-Jar
  (`--build-arg JAR=…`, Kontext = `generated/distributions/executable/`).
- `Containerfile.ts` — der pnpm-Workspace; jedes Beispiel/jede Probe per
  Kommando-Override (`podman run … ddsr/ts provider.ts`).
- `mosquitto.conf` — anonymer Listener 1883 für Szenario C.

## Fixtures (`fixtures/`)

`fingerprint/` — die Golden-Dateien des sd1-Schemas (XMI, kanonische
Form, Hash). Byte-genau gepinnt von Tests in **beiden** Sprachen
(`ServiceDescriptionFingerprintTest`, `fingerprint.test.ts`); niemals
editieren, ohne beide Tests und die sd1-Spezifikation zu bedenken —
das Schema-Tag friert die Kanonik ein.
