# itest — the cross-language harness (FR-P4)

Automated end-to-end probes of both language tracks against a real
broker. Requirements and decisions:
[docs/DECISIONS_PARITY.md](../docs/DECISIONS_PARITY.md) (D10, D14–D16).

## Scenarios

- **A — Java provider → TS consumer:** a raw tap on the event stream (a
  curl, no SDK) asserting that the frame is a CloudEvent whose data is
  the document and that the frozen frame name has not moved, discovery,
  the sd1 fingerprint
  (the broker property = locally computed = the golden hash), all eight
  property types type-exact, invocation, and the FR-P3 probe: the
  UNREGISTERING reaches the consumer **before** the provider process
  ends (SIGTERM → FrameworkShutdownHook → a blocking withdraw →
  the broker fan-out → the process ends).
- **B — TS provider → Java consumer:** `client.bndrun`
  (TsPaymentDebug) calls getBalance/charge; on SIGTERM of the TS
  provider: the withdraw is acknowledged by the broker **before** the
  HTTP endpoint stops, and the broker no longer lists the provider
  afterwards.
- **C (podman only) — the MQTT wire proof:** a Mosquitto container; the
  TS `MqttEventSource` receives and decodes the broker-shaped message
  over real MQTT/TCP (topics `<prefix>/<interface>` and
  `<prefix>/_unknown`) — since #101 a CloudEvent in structured mode
  carrying the event document.

- **F — the same identity starts on a new port:** a second instance of
  the Java provider with the same `(name, version)` on port 9092, while
  the TS probe holds a tracked locator and a lease. Since #55 that is a
  modify in place: what is expected is `MODIFIED` under the same
  reference id, the locator takes the endpoint out of the event, the
  next call reaches the new port, and the lease survives (#55, #57, #58).
- **G — `DEPRECATE_AND_DRAIN`:** version 2.0.0 is published with
  `replaces=1.0.0` while the probe holds a lease on 1.0.0. Expected:
  `UPGRADE_AVAILABLE`, lookups deliver only the successor, the
  predecessor keeps answering, and after `DELETE /consumers/{id}` the
  policy sweep retires it (`UNREGISTERING/REPLACED` + `RETIRED`) and the
  locator switches over (#45, #58).
- **I (podman only) — RSA over MQTT:** a plain OSGi service is exported
  over MQTT by one framework and bound with `@Reference` in another —
  no contract document, no endpoint code, and no REST on the
  invocation path. Deliberately MQTT distribution with REST discovery:
  the announcement is a publish to the broker as always, only the calls
  travel over mosquitto (#98).
- **H — provider liveness:** the Java provider heartbeats every 2 s
  (`DDSR_PROVIDER_HEARTBEAT_SECONDS=2`) and is killed with SIGKILL — no
  withdraw, no shutdown hook. Expected: the broker retires the
  registration after two missed heartbeats with `PROVIDER_LOST`
  (`UNREGISTERING` + `RETIRED`), the lookup no longer lists the dead
  endpoint, the locator stands at `REBIND` and a call fails (there is no
  successor), the lease is gone; the harness measures the latency to the
  consumer and demands < 30 s (#52).

The payment provider is configurable through the environment for this
(`PAYMENTS_HTTP_PORT`, `PAYMENTS_PUBLIC_URL`, `PAYMENTS_IMPL_VERSION`,
`PAYMENTS_UPDATE_POLICY`, `PAYMENTS_REPLACES_VERSION`, `DDSR_BROKER_URL`,
`DDSR_PROVIDER_HEARTBEAT_SECONDS`, `DDSR_SESSION_INTERVAL_SECONDS`;
`configs/config.json` with ConfigAdmin interpolation). The example
provider keeps its consumer session interval at 0 and so acquires no
lease on its own service — otherwise that self-lease would block the
drain in scenario G.

## Runs

```bash
./itest/run-harness.sh          # host processes (no podman needed): A + B + F + G + H
./itest/run-harness-podman.sh   # containers (podman, --network=host): A + B + C + D + E + I + F + G + H
```

Both build first (`./gradlew build` + the bnd exports, `pnpm install` +
build). The export jars are deleted before the export: the bnd export
does not track cross-project bundle changes and would otherwise embed
stale bundles. Logs land in `itest/work/` and `itest/work-podman/`
respectively (gitignored); on a timeout a thread dump is additionally
shot into the saved container log.

## Container images (`containers/`)

- `Containerfile.java` — generic for any exported bnd jar
  (`--build-arg JAR=…`, the context is
  `generated/distributions/executable/`).
- `Containerfile.ts` — the pnpm workspace; every example and probe
  through a command override (`podman run … ddsr/ts provider.ts`).
- `mosquitto.conf` — an anonymous listener on 1883 for scenario C.

## Fixtures (`fixtures/`)

`fingerprint/` — the golden files of the sd1 schema (XMI, the canonical
form, the hash). Pinned byte-exactly by tests in **both** languages
(`ServiceDescriptionFingerprintTest`, `fingerprint.test.ts`); never
edit them without thinking about both tests and the sd1 specification —
the schema tag freezes the canonicalisation.
