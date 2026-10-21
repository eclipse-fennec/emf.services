# Cross-Language Harness

The harness (FR-P4) is the executable proof that the Java and
TypeScript tracks interoperate — not by unit-testing each side against
fixtures, but by running real brokers, providers and consumers against
each other and asserting the promises the documentation makes:
lifecycle order with millisecond timestamps, typed property fidelity,
fingerprint equality, session visibility, and both event transports
over a real wire.

## Running it

```bash
# host processes (scenarios A + B + F + G + H + N); podman only for Keycloak
./itest/run-harness.sh

# containerized incl. Mosquitto (scenarios A–J) — podman is the
# vehicle, not a requirement
./itest/run-harness-podman.sh

# either of them against an open broker, without Keycloak
HARNESS_AUTH=none ./itest/run-harness.sh
```

The podman variant builds four images from the exported BND launches
(`ddsr/broker`, `ddsr/payment-java`, `ddsr/client-java`,
`ddsr/client-mqtt`) plus one TS image, all on
`--network=host` so every localhost default applies unchanged. The TS
image is also *built* with `--network=host`: its build installs
dependencies from the npm registry, and rootless podman 5.x (pasta)
hands the build container the host's `/etc/resolv.conf` unchanged — on a
host that resolves through the systemd-resolved stub (`127.0.0.53`, the
GitHub runners among them) that nameserver is unreachable from inside
and every lookup fails as `EAI_AGAIN`. Logs of failing scenarios land
under `itest/work-podman/`.

## Secured by default

Both harnesses run the broker the way a deployment should: in mode
`bearer` ([Security](SECURITY.md) §7a), against a Keycloak realm that
runs as a container for the length of the run
(`quay.io/keycloak/keycloak:26.4` on port 18080, realm
`itest/keycloak/fennec-realm.json`). The first check after the broker
is up is that a call **without** a token is turned away with `401`.
Right after it come the role matrix — a consumer may neither publish
nor read the registry, the admin may ([Security](SECURITY.md) §7c) —
and the domain: the broker runs as domain `fennec` (`DDSR_DOMAIN`) and
says so when it starts, and a token of the same realm minted for
another audience is `401`, even the admin's ([Security](SECURITY.md) §3).

Every process calls the broker as a client of its own — one
confidential client per role, secret `<client>-secret`. Audiences are
optional client scopes of the same name: a process asks for
`scope=ddsr-broker` to get a token for the broker, the RSA consumer
asks for `scope=rsa-example` to get one for what the RSA example
serves, and no token carries another target's audience
([Security](SECURITY.md) §1, §7b):

| Client | Who |
| --- | --- |
| `harness` | the scripts' own curl checks |
| `payment-java` | every Java payment provider instance (A, F, G, H, J) |
| `ts-provider` | the TypeScript provider (B, E) |
| `java-client` | `client.jar`, `client-mqtt.jar` (P, B, D) |
| `harness-probe-ts` | the TypeScript probe (A, E, J) |
| `harness-lifecycle-f/-g/-h` | the lifecycle probes |
| `rsa-example` | the RSA exporting framework (I, K) — and the audience of what it serves |
| `rsa-example-consumer` | the RSA importing framework (I, K); holds the realm role `ledger-reader`, not `ledger-writer` |
| `ts-ledger` | the TypeScript ledger (T) — and the audience of what it serves |

Each service account holds the broker roles it needs and no more
([Security](SECURITY.md) §3): the providers `service-provider` and
`service-consumer` — the Java provider looks its own earlier
registration up to change it in place (scenario F), the TS provider
holds a session, the RSA exporter imports — the consumers and probes
`service-consumer`, the harness itself `service-consumer` and
`service-admin`. Right after the
broker is up, both harnesses check that a consumer may neither publish
nor read the registry, while the admin may.

The broker names a caller by `azp` — the client id — so a consumer's
session id is its client id, and a provider can only withdraw what it
published itself. The processes get their identity from the same
environment variables in both languages (`DDSR_OAUTH_TOKEN_URI`,
`DDSR_OAUTH_CLIENT_ID`, `DDSR_OAUTH_CLIENT_SECRET`, `DDSR_OAUTH_SCOPE`;
the consumer id falls back to the client id); `itest/keycloak/auth.sh`
hands them out, and `provider_args` the verifier side of a node that
serves guarded contracts (`DDSR_JWT_*`, roles from `realm_access.roles`).

**Scenario K** (podman) is where roles per operation are proven: the
RSA example's `Ledger` interface says `@RolesAllowed("ledger-reader")`,
`@RolesAllowed("ledger-writer")` on `record`, `@PermitAll` on
`version`. One framework exports it over REST with the node's verifier,
another binds it with a plain `@Reference` and calls as
`rsa-example-consumer` — version and balance answer, `record` is `403`,
and a broker token is `401` at the provider. Scenario I exports the same
interface over MQTT and checks that it is refused there.

**Scenario T** (host) is its TypeScript half: `ledger-ts.ts` serves a
guarded `TsLedger` with a verifier of its own audience (`ts-ledger`).
Its version answers without a token; its balance is `401` without one,
`403` with a token whose holder lacks `ledger-reader`, `200` as
`rsa-example-consumer`; recording is `403` for that reader; and a token
minted for the broker is `401` there.

`HARNESS_AUTH=none` runs either harness open, without Keycloak — and
without podman for the host variant. Independently of that, the host
harness ends with **scenario N**: a second broker from the development
launch with no security variable at all, a provider without credentials
publishing to it, and a lookup without a token finding it. The path a
developer takes stays proven whatever the rest of the run did.

After it, **scenario M** proves the other authentication mode
([Security](SECURITY.md) §7d), again independent of Keycloak.
`itest/tls/make-certs.sh` issues a CA for the run, a server certificate
for `localhost` and client certificates for `java-client` and
`harness` — plus one of another authority that must not get in. A third
broker runs in mode `mtls`: HTTPS on 8898 demanding a client
certificate, roles mapped per CN, and a plain listener on 8899 for a
gateway's header. Checked from outside: no TLS connection without a
certificate of the CA; `java-client` reads the catalog but not the
registry (`403`), `harness` reads the registry; on the plain listener a
call without the header is `401` and the admin's certificate in the
header from `127.0.0.1` gets in. Finally the Java client runs against
it with its key store and must subscribe to the event stream and list
the catalog without a single TLS failure. In the secured run M goes on
with discovery by certificate and calls by token: the Java payment
provider (`payment-java`, port 9095) announces `PersonStore` over its
certificate and serves it in mode `bearer` for the Keycloak audience
`person-store`; a call without a token is `401`, the lookup names the
bearer scheme, and a client with both credentials finds the store by
certificate and calls it with a token of that audience.

## Scenarios

| | What runs | What is asserted |
|---|---|---|
| **A** | Java provider → TS consumer | discovery; sd1 golden + broker/local fingerprint match; im1 decoration; all 8 typed property kinds round-trip; REST invocation; session visible at the broker; contract-addressed lookup (exact sd1 hits, foreign sd1 empty); **UNREGISTERING reaches the consumer measurably BEFORE the provider process exits** |
| **B** | TS provider → Java consumer | reflective REST invocation from Java; withdraw is confirmed by the broker before the TS endpoint stops; the broker no longer lists the provider afterwards |
| **C** | TS MQTT wire probe | a broker-shaped event document is delivered and decoded over a real Mosquitto TCP connection |
| **D** | Java broker sink + Java client source over Mosquitto | lifecycle events (REGISTERED + UNREGISTERING) delivered over real MQTT/TCP; ordering control pins the events onto the MQTT path (the last stream (re)open before the events is the MQTT subscription — the SDK hands the stream over when a higher-ranked transport appears) |
| **P** | Java provider → Java consumer, protobuf body | a modelled argument and a modelled result travel in the encoding the contract declares (#100): `PersonStore` says `consumes`/`produces` = `application/x-protobuf`, and neither side mentions an encoding in code. **Java on both ends on purpose** — the TypeScript track has no protobuf-to-EMF binding, so this contract is one a TS consumer cannot read |
| **J** | Java provider serves `BindingProbe` over MQTT from a factory configuration; TS probe invokes it | the provider side needed no code at all: an ordinary OSGi service, a model document and a configuration (#25). The probe's client speaks MQTT for this scenario, because a lookup is filtered by the flavors a consumer states — an MQTT-only implementation is invisible to a REST-only consumer, and rightly so |
| **E** | TS provider announces `MqttFlavor` beside `RestFlavor`; probe invokes over MQTT | the DoD's "same interface, different transports": `getBalance` is invoked over the announced MQTT flavor, broker address taken from `MqttFlavor.brokers`, request/response via the frozen envelope ([WIRE_FORMAT.md](WIRE_FORMAT.md)) |

The MQTT transports ship dormant (`configurationPolicy = REQUIRE`) in
every launch. Scenario D wakes the broker's the way a deployment does —
the shipped `ddsr/broker` image with `DDSR_MQTT_URL` set — so the
documented environment surface is under test here and not only in a
`docker run` example. The client has no such surface and keeps its
`-mqtt` launch variant with the harness-only configuration bundle
`org.eclipse.fennec.services.itest.mqtt.config`.

**F — same identity restarts on a new port.** A second instance of the
Java provider with the same `(name, version)` starts on port 9092 while the
TS probe holds a tracked locator and a lease. Since #55 this is a modify in
place: the probe expects `MODIFIED` under the same reference id, its locator
takes the new endpoint from the event, the next call reaches the new port
and the lease is unchanged — no `UNREGISTERING`, no rebind.

**G — `DEPRECATE_AND_DRAIN`.** Version 2.0.0 publishes with
`replaces=1.0.0` while the probe holds a lease on 1.0.0. The probe expects
`UPGRADE_AVAILABLE`, lookups that return only the successor, a predecessor
that keeps answering, and — after `DELETE /consumers/{id}` — the policy
sweep retiring it (`UNREGISTERING/REPLACED` + `RETIRED`) and the locator
rebinding to the successor.

**H — provider liveness (#52).** The Java provider heartbeats every 2 s
(`DDSR_PROVIDER_HEARTBEAT_SECONDS=2`) and is then killed with SIGKILL: no
withdraw, no shutdown hook. The broker has to notice the silence (two
missed heartbeats, then its liveness sweep) and retire the registration
with `PROVIDER_LOST`. The probe expects `UNREGISTERING/PROVIDER_LOST` +
`RETIRED/PROVIDER_LOST` for the held reference, a lookup that no longer
lists the dead endpoint, a locator in `REBIND` whose invoke fails (no
successor) and a released lease; the harness asserts the event reached
the consumer within 30 s of the kill and prints the latency.

The provider instances are configured through the environment
(`PAYMENTS_HTTP_PORT`, `PAYMENTS_PUBLIC_URL`, `PAYMENTS_IMPL_VERSION`,
`PAYMENTS_UPDATE_POLICY`, `PAYMENTS_REPLACES_VERSION`, `DDSR_BROKER_URL`,
`DDSR_PROVIDER_HEARTBEAT_SECONDS`, `DDSR_SESSION_INTERVAL_SECONDS`) via
ConfigAdmin interpolation in `configs/config.json`. The example provider
keeps its consumer session interval at 0, so it never acquires a lease on
its own service — otherwise the self-lease would stall scenario G's drain.

## CI

`.github/workflows/harness.yml` runs on every push and pull request:
two gate jobs (full Java build, npm build + TS unit tests) must pass
first, then the harness job executes the podman variant. A red gate
never wastes harness minutes; a green harness is the merge signal that
cross-language behavior still holds.

## Reading a failure

Every scenario prints its assertion inline (`✓`/`✗` per probe check,
`SCENARIO X FAILED: <reason>` with the offending log dumped). The
timeout path additionally sends the JVM a thread-dump signal before
capturing the container log — a hung launch shows its stacks in
`itest/work-podman/<container>.timeout.log`.
