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
# host processes (no containers; scenarios A + B + F + G + H)
./itest/run-harness.sh

# containerized incl. Mosquitto (scenarios A–J) — podman is the
# vehicle, not a requirement
./itest/run-harness-podman.sh
```

The podman variant builds four images from the exported BND launches
(`ddsr/broker`, `ddsr/broker-mqtt`, `ddsr/payment-java`,
`ddsr/client-java`, `ddsr/client-mqtt`) plus one TS image, all on
`--network=host` so every localhost default applies unchanged. The TS
image is also *built* with `--network=host`: its build installs
dependencies from the npm registry, and rootless podman 5.x (pasta)
hands the build container the host's `/etc/resolv.conf` unchanged — on a
host that resolves through the systemd-resolved stub (`127.0.0.53`, the
GitHub runners among them) that nameserver is unreachable from inside
and every lookup fails as `EAI_AGAIN`. Logs of failing scenarios land
under `itest/work-podman/`.

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

Scenario D runs the `-mqtt` launch variants: the MQTT transports ship
dormant (`configurationPolicy = REQUIRE`) in every launch and are woken
by the harness-only configuration bundle
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
