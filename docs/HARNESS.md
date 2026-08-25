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
# host processes (no containers; scenarios A + B)
./itest/run-harness.sh

# containerized incl. Mosquitto (scenarios A–E) — podman is the
# vehicle, not a requirement
./itest/run-harness-podman.sh
```

The podman variant builds four images from the exported BND launches
(`ddsr/broker`, `ddsr/broker-mqtt`, `ddsr/payment-java`,
`ddsr/client-java`, `ddsr/client-mqtt`) plus one TS image, all on
`--network=host` so every localhost default applies unchanged. Logs of
failing scenarios land under `itest/work-podman/`.

## Scenarios

| | What runs | What is asserted |
|---|---|---|
| **A** | Java provider → TS consumer | discovery; sd1 golden + broker/local fingerprint match; im1 decoration; all 8 typed property kinds round-trip; REST invocation; session visible at the broker; contract-addressed lookup (exact sd1 hits, foreign sd1 empty); **UNREGISTERING reaches the consumer measurably BEFORE the provider process exits** |
| **B** | TS provider → Java consumer | reflective REST invocation from Java; withdraw is confirmed by the broker before the TS endpoint stops; the broker no longer lists the provider afterwards |
| **C** | TS MQTT wire probe | a broker-shaped event document is delivered and decoded over a real Mosquitto TCP connection |
| **D** | Java broker sink + Java client source over Mosquitto | lifecycle events (REGISTERED + UNREGISTERING) delivered over real MQTT/TCP; ordering control pins the events onto the MQTT path (the last stream (re)open before the events is the MQTT subscription — the SDK hands the stream over when a higher-ranked transport appears) |
| **E** | TS provider announces `MqttFlavor` beside `RestFlavor`; probe invokes over MQTT | the DoD's "same interface, different transports": `getBalance` is invoked over the announced MQTT flavor, broker address taken from `MqttFlavor.brokers`, request/response via the frozen envelope ([WIRE_FORMAT.md](WIRE_FORMAT.md)) |

Scenario D runs the `-mqtt` launch variants: the MQTT transports ship
dormant (`configurationPolicy = REQUIRE`) in every launch and are woken
by the harness-only configuration bundle
`org.eclipse.fennec.services.itest.mqtt.config`.

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
