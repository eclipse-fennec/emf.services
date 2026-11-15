# The Payment Example

One contract, two languages, two transports — the example every test
and harness scenario in this repository runs against. This page walks
through the moving parts and how to run them yourself.

## The contract

`Payment` (operations `charge(amount, currency)` and
`getBalance(accountId)`, both returning `double`) is the same on both
sides — built by `PaymentPublisher` (Java), read from the document
`payment-provider.xmi` by the TypeScript provider — so both produce the
**same sd1 fingerprint**. That is the point: contract divergence between the
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
`MessagingFlavor` on the same implementation** and serves the operations
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
  announced `MessagingFlavor.brokers`).
- **Java:** the client launch registers a typed `PaymentRemote` proxy
  per discovered provider (`PaymentProxyRegistrar` in `examples.persons` +
  `ServiceProxyFactory`) — application code calls
  `payment.charge(10.0, "EUR")` without knowing the transport.

## Tagged: methods and terms

The example is also the tag overlay's ([Tags](TAGS.md), #281). Two
dimensions, two branches of one tree:

```
payment
├── method              → tags on implementations (each provider brings its own)
│   ├── paypal
│   └── card
└── terms               → tags on contracts
    ├── instant
    └── installment
```

| Contract | Tag | Implementations |
| --- | --- | --- |
| `Payment` (unchanged, paid at once) | `payment/terms/instant` | PayPal — the Java provider; card |
| `InstallmentPlan` (`offer(amount, months)`, `accept(planId)`) | `payment/terms/installment` | card only |

- **PayPal** is the Java provider: `PaymentPublisher` gives its
  implementation `payment.scheme=paypal` and asks for
  `payment/method/paypal`, and gives the `Payment` contract
  `payment/terms/instant`.
- **card** is `card-provider.ts` with `card-provider.xmi`: **one
  implementation serving two contracts**, `Payment` and `InstallmentPlan`,
  with `payment.scheme=card`, asking for `payment/method/card`.

`Payment` stays field for field the golden contract — tags are not part
of the sd1 ([Fingerprints](FINGERPRINTS.md)), and a test holds the card
provider's copy to the golden hash.

Seeding the tree is what an application does when it is installed; the
leaves may be given by a provider itself (`selfAssignable`), so the
providers' own tags are carried at once:

```bash
B=http://localhost:8887/ddsr/rest
curl -X PUT "$B/tags/tag?id=payment"
curl -X PUT "$B/tags/tag?parent=payment&id=method"
curl -X PUT "$B/tags/tag?parent=payment/method&id=paypal&selfAssignable=true"
curl -X PUT "$B/tags/tag?parent=payment/method&id=card&selfAssignable=true"
curl -X PUT "$B/tags/tag?parent=payment&id=terms"
curl -X PUT "$B/tags/tag?parent=payment/terms&id=instant&selfAssignable=true"
curl -X PUT "$B/tags/tag?parent=payment/terms&id=installment&selfAssignable=true"
# a rule: whatever says it is a card scheme is under payment/method/card
curl -X PUT "$B/tags/rule?path=payment/method/card&kind=implementation&filter=(payment.scheme%3Dcard)"

cd ddsr-ts-client/examples/payment
BROKER_URL=$B SERVICE_URL=http://localhost:9095/card pnpm exec tsx card-provider.ts
```

What it shows:

- **The hierarchy**: `find(payment)` over implementations finds PayPal
  and both of card's; `find(payment/method/card)` card's two — one
  implementation, two contracts; `find(payment/terms, contract)` both
  contracts.
- **A rule**: card carries `payment/method/card` by itself (`SOURCE`)
  and by the rule (`RULE`).
- **The facade per tag**: a shop whose broker URL is
  `$B/scoped/payment/terms/instant` sees `Payment` and no instalments;
  under `$B/scoped/payment/terms/installment` it finds `InstallmentPlan`.
  With OPA in front of it (#282), a shop holding the group
  `/tags/payment/terms/instant` would be allowed only the former.

## What the harness asserts against this example

Scenario A (Java→TS) and B (TS→Java) prove discovery, typed property
fidelity, fingerprint equality, sessions/leases and the FR-P3 order
with millisecond timestamps; scenario D proves lifecycle events over
real MQTT/TCP; scenario E invokes `getBalance` over the announced MQTT
flavor. See [HARNESS.md](HARNESS.md).
