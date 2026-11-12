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
- **T (host only, secured) — a TypeScript provider holds the roles:**
  `ledger-ts.ts` serves a guarded contract with a WebCrypto verifier;
  401 without a token, 403 without the role, 200 with it, and a broker
  token is 401 there. Skipped with `HARNESS_AUTH=none`.
- **N (host only) — the developer's broker, open:** a second broker from
  the development launch with no security variable, a TypeScript
  provider without credentials publishing to it, and a lookup without a
  token finding it.
- **M (host only) — client certificates:** a CA issued per run
  (`tls/make-certs.sh`), a broker in mode `mtls` with HTTPS demanding a
  certificate and a plain listener for a gateway's header; no
  connection without a certificate of the CA, roles per CN (consumer
  `403` on the registry, admin `200`), the header counts from the
  gateway's address, the Java client subscribes and lists the catalog
  with its key store, the TypeScript provider publishes through
  `tlsFetch` with its own certificate, and the TypeScript ledger serves
  by certificate with roles per CN. Secured, it goes on: a provider announces
  `PersonStore` by certificate and serves it in bearer, and a client
  finds it by certificate and calls it with a token for `person-store`;
  then the Ledger of K is exported and imported over the mtls broker by
  RSA nodes whose role configuration names their certificates.
  The mTLS half runs whatever `HARNESS_AUTH` says.
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
- **MQTT 5 beside 3.1.1 (podman, from D on):** the registry publishes
  its events over MQTT 5 (`DDSR_MQTT_VERSION=5`) while everything that
  hears them speaks 3.1.1 or is TypeScript, and in scenario I the
  exporting node serves over MQTT 5 while the importing node calls over
  3.1.1 (#138). A broker carries messages between the two; the reply
  address and the correlation are in the envelope.
- **I (podman only) — RSA over MQTT:** a plain OSGi service is exported
  over MQTT by one framework and bound with `@Reference` in another —
  no contract document, no endpoint code, and no REST on the
  invocation path. Deliberately MQTT distribution with REST discovery:
  the announcement is a publish to the broker as always, only the calls
  travel over mosquitto (#98).
  The same framework is asked to export the guarded `Ledger` too, and
  the scenario checks that MQTT refuses it — a call there carries no
  credential. The example's `Ticker` returns a `PushStream`: the
  importing framework collects its five values and the end of the
  stream, and a TypeScript consumer (`ticker-probe.ts`) looks the
  derived contract up and iterates the same stream as an
  `AsyncIterable`.
- **R (podman only) — RSA over AMQP:** scenario I on the third
  transport (#138). The same service is exported over AMQP 0-9-1 by one
  framework and bound with `@Reference` in another, over a RabbitMQ
  container: the call goes to the topic exchange under a routing key,
  the answer comes back on the protocol's own reply-to. The importing
  node hears the registry's events over AMQP too and is started first,
  so what makes it import is the `REGISTERED` event on the routing key;
  the exporting node serves over AMQP and announces over REST. The
  guarded `Ledger` is refused here as it is over MQTT, and the
  `Ticker`'s stream arrives here as it does there — in the importing
  framework and in TypeScript.
- **S (podman only) — a call encoded as JSON:** scenario R again, with
  the exporting node told `encoding=application/json`. The payload is
  written and read by the resource factory registered for that content
  type — emf.codec — on both ends; the consumer learns the encoding
  from the announced flavor. Proven by the call being answered: a node
  without a factory for the content type refuses the document.
- **K (podman only, secured) — roles per operation over RSA:** the
  `Ledger` interface carries `@RolesAllowed` and `@PermitAll`; exported
  over REST with the node's verifier, imported by a framework whose
  client holds `ledger-reader` only. Version and balance answer,
  `record` is refused with `403`, and a token minted for the broker is
  `401` at the provider. Skipped with `HARNESS_AUTH=none`.
- **J (podman only) — MQTT with no provider code:** the implementation
  is an ordinary OSGi service; a factory configuration of
  `provider.messaging` reads the contract from a model document, subscribes
  per operation and announces what it serves. The probe reads the
  announced `MessagingFlavor` and calls `echo` over mosquitto, and the three
  arguments come back in the contract's order — both ends read one
  document the same way (#25). The twin of #84 on this transport: what
  the deployment states is the broker, nothing else.
- **U (podman only) — AMQP with no provider code, called from
  TypeScript:** J on the other protocol (#138). The same generic
  distribution with another model document (`binding-probe-amqp.xmi`)
  and an `amqp://` broker serves `BindingProbe` over RabbitMQ; the probe
  is the same TypeScript plugin built for AMQP and calls it under a
  routing key, with the answer arriving on a key of its own. The first
  time the TypeScript track speaks AMQP on a real wire.
- **Q (podman only) — MQTT over mutual TLS:** J's call again, over a
  second mosquitto on 8883 that demands a certificate of the run's CA
  (`itest/tls/make-certs.sh`), takes the CN as the user and holds the
  topics to an ACL: callers write requests, the provider reads its tree
  and answers into any reply subtree, each node reads only the replies
  under its own name. Checked: no connection without a certificate, the
  Java provider serves over `ssl://` and the TypeScript probe calls it
  with its own, and a third certificate subscribed to every reply
  receives none.
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
./itest/run-harness.sh          # host processes: A + B + F + G + H + T + N (podman only for Keycloak)
./itest/run-harness-podman.sh   # containers (podman, --network=host): A + B + C + D + E + I + R + S + K + J + U + Q + F + G + H
HARNESS_AUTH=none ./itest/run-harness.sh   # open broker, no Keycloak, no podman
```

Both run the broker **secured** by default: Keycloak
(`keycloak/fennec-realm.json`, port 18080) issues every process a token
of its own client, the broker verifies them in mode bearer, and a call
without a token is checked to be `401`, as are the broker's role matrix
and its domain (`fennec`; a token for another audience is `401`).
`keycloak/auth.sh` holds the
helpers both scripts share. The table of clients and why the consumer
id is the client id is in [HARNESS.md](../docs/HARNESS.md#secured-by-default).

Both build first (`./gradlew build` + the bnd exports, `pnpm install` +
build). The export jars are deleted before the export: the bnd export
does not track cross-project bundle changes and would otherwise embed
stale bundles. Logs land in `itest/work/` and `itest/work-podman/`
respectively (gitignored); on a timeout a thread dump is additionally
shot into the saved container log (`<container>.timeout.log`), and a
podman run that fails saves the log of every container it started
(`<container>.failed.log`) before removing them.

## Container images (`containers/`)

- `Containerfile.java` — generic for any exported bnd jar
  (`--build-arg JAR=…`, the context is
  `generated/distributions/executable/`).
- `Containerfile.ts` — the pnpm workspace; every example and probe
  through a command override (`podman run … ddsr/ts provider.ts`).
- `mosquitto.conf` — an anonymous listener on 1883 for scenario C.

**"Unable to open config file" from the mosquitto container** is the
host, not the image. Ubuntu 26.04 ships an AppArmor profile for
`/usr/sbin/mosquitto`; a profile is attached by path, so it confines
the binary inside the container as well, and it allows neither
`/mosquitto/config` nor a signal from the container runtime.
`journalctl -k | grep 'profile="mosquitto"'` shows the denials. On a
machine that runs no mosquitto of its own the profile can be disabled,
which is what the CI workflow does:

```bash
sudo ln -sf /etc/apparmor.d/mosquitto /etc/apparmor.d/disable/
sudo apparmor_parser -R /etc/apparmor.d/mosquitto
```

## Fixtures (`fixtures/`)

`fingerprint/` — the golden files of the sd1 schema (XMI, the canonical
form, the hash). Pinned byte-exactly by tests in **both** languages
(`ServiceDescriptionFingerprintTest`, `fingerprint.test.ts`); never
edit them without thinking about both tests and the sd1 specification —
the schema tag freezes the canonicalisation.
