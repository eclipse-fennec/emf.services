# Getting Started

This walkthrough takes you from a fresh checkout to a running broker, a
Java provider, a TypeScript consumer, and back — the same flow the
cross-language harness automates. Times below assume a warm Gradle
daemon; the first build downloads the BND/Maven world once.

## Prerequisites

- **Java 21** (the BND workspace builds and runs on 21)
- **Node.js 20+ with pnpm** for the TypeScript track
- optional: **podman** for the containerized harness, which also runs
  the MQTT scenarios against an Eclipse Mosquitto container

## 1. Build everything

```bash
./gradlew clean build          # Java: all bundles + unit tests
cd ddsr-ts-client
pnpm install && pnpm build && pnpm test
cd ..
```

## 2. Start the broker

The broker is an executable jar exported from a BND launch description:

```bash
./gradlew :org.eclipse.fennec.services.broker.rest:export.broker
java -Dgosh.args=--nointeractive \
  -jar org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker.jar
```

The broker serves REST + SSE on port **8887** and persists its registry
as an XMI snapshot (`./broker-state.xmi`, configurable via the
`org.eclipse.fennec.services.broker.core` PID). Check it is up:

```bash
curl http://localhost:8887/ddsr/rest/catalog
```

There is a second launch variant, `export.broker-mqtt`, that
additionally publishes lifecycle events to an MQTT broker at
`tcp://localhost:1883` — same bundles, plus a configuration that wakes
the dormant MQTT transport. That variant is harness-only.

Port, bind address and the URL the broker advertises for itself are
environment variables, so nothing has to be rebuilt to move the broker
off its defaults:

```bash
DDSR_HTTP_PORT=9887 DDSR_PUBLIC_URL=http://192.168.1.6:9887/ddsr/rest \
  java -Dgosh.args=--nointeractive \
  -jar org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker.jar
```

The same jar is published as a container image
(`eclipsefennec/emf.services:broker-snapshot`), which is the shortcut
past steps 1 and 2 if all you want is a broker to develop against:

```bash
docker run -d -p 8887:8887 -v ddsr-broker-data:/opt/services/data \
  docker.io/eclipsefennec/emf.services:broker-snapshot
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for the image's full configuration
surface, its state volume and the CI that builds it.

## 3. Publish a service — Java provider

The example provider registers the `Payment` contract in the catalog
and publishes an implementation with a REST flavor:

```bash
./gradlew :org.eclipse.fennec.services.examples.payment:export.payment-provider
java -Dgosh.args=--nointeractive \
  -jar org.eclipse.fennec.services.examples.payment/generated/distributions/executable/payment-provider.jar
```

Watch the broker's view:

```bash
curl 'http://localhost:8887/ddsr/rest/references?interface=Payment'
```

Every returned `ServiceReference` carries the provider's properties
plus the broker-computed fingerprints (`ddsr.fingerprint` = the sd1 of
the contract, `ddsr.impl.fingerprint` = the im1 of the implementation —
see [FINGERPRINTS.md](FINGERPRINTS.md)).

## 4. Consume it — TypeScript

```bash
cd ddsr-ts-client/examples/payment
BROKER_URL=http://localhost:8887/ddsr/rest pnpm exec tsx consumer.ts
```

The consumer pulls a snapshot, subscribes the SSE event stream, finds
the `Payment` reference and invokes `charge`/`getBalance` through the
REST flavor the provider announced. The same works in the other
direction: `provider.ts` publishes a TS implementation that the Java
example client (`export.client`) discovers and calls.

## 5. The other direction and MQTT

```bash
# TS provider (REST; add MQTT_URL to also serve over MQTT topics)
BROKER_URL=http://localhost:8887/ddsr/rest \
SERVICE_URL=http://localhost:9090/payments \
MQTT_URL=mqtt://localhost:1883 \
  pnpm exec tsx provider.ts

# Java consumer
./gradlew :org.eclipse.fennec.services.client.java:export.client
java -Dgosh.args=--nointeractive \
  -jar org.eclipse.fennec.services.client.java/generated/distributions/executable/client.jar
```

With `MQTT_URL` set, the TS provider announces a **second flavor on
the same implementation** — `MqttFlavor` beside `RestFlavor` — and
serves both transports from the same state. A consumer with the
`MqttFlavorPlugin` invokes operations over topics; the request/response
convention is specified in [WIRE_FORMAT.md](WIRE_FORMAT.md).

## 6. Lifecycle guarantee

Stop any provider with Ctrl-C / SIGTERM and watch the order: the
provider **first** withdraws at the broker and waits for the
confirmation (which means consumers received `UNREGISTERING`), and only
**then** stops its endpoint. That is the OSGi lifecycle promise carried
across languages, and it is exactly what the harness asserts with
millisecond timestamps — see [HARNESS.md](HARNESS.md).

## Where to go next

- [ARCHITECTURE.md](ARCHITECTURE.md) — bundle layout, design decisions,
  wire examples
- [WIRE_FORMAT.md](WIRE_FORMAT.md) — every endpoint, every wire
  convention
- [FINGERPRINTS.md](FINGERPRINTS.md) — the sd1/im1 specification
- [CLIENT_FRAMEWORK_GUIDE.md](CLIENT_FRAMEWORK_GUIDE.md) — writing your
  own Java provider/consumer
- [ACQUISITION.md](ACQUISITION.md) — discovery, acquisition, sessions,
  cold cache
- [DEPLOYMENT.md](DEPLOYMENT.md) — the broker container image, its
  configuration and the publishing pipeline
