# The Payment Example

One contract, two languages, two transports — the example every test
and harness scenario in this repository runs against. This page walks
through the moving parts and how to run them yourself.

## The contract

`Payment` (operations `charge(amount, currency)` and
`getBalance(accountId)`, both returning `double`) is built field for
field identically on both sides — `PaymentPublisher` (Java) and
`payment-api.ts` (TypeScript) — so both produce the **same sd1
fingerprint**. That is the point: contract divergence between the
languages becomes a string comparison, pinned by the golden fixtures
under `itest/fixtures/fingerprint/`.

## Java provider

`org.eclipse.fennec.services.examples.payment` contains

- `PaymentResource` — a plain JAX-RS resource serving
  `POST /payments/charge` and `GET /payments/balance`;
- `PaymentPublisher` — a DS component that (1) ensures the `Payment`
  catalog entry and (2) publishes a `ServiceImplementation` with a
  `RestFlavor` pointing at the resource, carrying one property of
  every supported type (string, int, long, double, float, short, bool,
  string list) so consumers can assert typed round-trip fidelity.

On deactivate the publisher withdraws **synchronously** — deactivation
returns only after the broker acknowledged, so consumers were informed
while the endpoint was still up (the FR-P3 order).

```bash
./gradlew :org.eclipse.fennec.services.examples.payment:export.payment-provider
java -Dgosh.args=--nointeractive \
  -jar org.eclipse.fennec.services.examples.payment/generated/distributions/executable/payment-provider.jar
```

## TypeScript provider — one implementation, two flavors

`ddsr-ts-client/examples/payment/provider.ts` serves the same contract
over HTTP, and — when `MQTT_URL` is set — **additionally announces an
`MqttFlavor` on the same implementation** and serves the operations
over topics (`MqttOperationServer`), both transports sharing one
balance state. That is the DoD scenario's "same interface, different
transports" in one process.

```bash
cd ddsr-ts-client/examples/payment
BROKER_URL=http://localhost:8887/ddsr/rest \
SERVICE_URL=http://localhost:9090/payments \
MQTT_URL=mqtt://localhost:1883 \
  pnpm exec tsx provider.ts
```

Shutdown order on Ctrl-C: withdraw first, await the broker
confirmation, then stop the MQTT dispatcher and the HTTP server.

## Consumers

- **TypeScript:** `consumer.ts` — snapshot, SSE subscription, findOne,
  invocation through the `RestFlavorPlugin`; with the
  `MqttFlavorPlugin` configured, the same `locator.invoke()` call runs
  over topics instead (the plugin reads the broker address from the
  announced `MqttFlavor.brokers`).
- **Java:** the client launch registers a typed `PaymentRemote` proxy
  per discovered provider (`PaymentProxyRegistrar` +
  `ServiceProxyFactory`) — application code calls
  `payment.charge(10.0, "EUR")` without knowing the transport.

## What the harness asserts against this example

Scenario A (Java→TS) and B (TS→Java) prove discovery, typed property
fidelity, fingerprint equality, sessions/leases and the FR-P3 order
with millisecond timestamps; scenario D proves lifecycle events over
real MQTT/TCP; scenario E invokes `getBalance` over the announced MQTT
flavor. See [HARNESS.md](HARNESS.md).
