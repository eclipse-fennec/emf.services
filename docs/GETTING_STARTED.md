# Getting Started

This walkthrough takes you from a fresh checkout to a running broker, a
Java provider, a TypeScript consumer, and back — the same flow the
cross-language harness automates. Times below assume a warm Gradle
daemon; the first build downloads the BND/Maven world once.

## Prerequisites

- **Java 21** (the BND workspace builds and runs on 21)
- **Node.js 24 with corepack (pnpm 11)** for the TypeScript track — pnpm 11 does not start on Node 20
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

The launch already carries the MQTT event transport, asleep. Set
`DDSR_MQTT_URL=tcp://localhost:1883` and the broker publishes its
lifecycle events there as well as over SSE; leave it empty and the
bundles sit idle. There is no second launch and no second image —
[DEPLOYMENT.md](DEPLOYMENT.md) lists the five variables.

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
  -e DDSR_AUTH_MODE=none \
  docker.io/eclipsefennec/emf.services:broker-snapshot
```

`DDSR_AUTH_MODE=none` is the one line to notice: the image is a
deployment artifact and runs secured unless told otherwise, the launch
from step 2 is a development launch and runs open.

### Open or secured

The broker has one switch for authentication, `DDSR_AUTH_MODE`:

| Mode | Who it is for | What it takes |
| --- | --- | --- |
| `none` | development — any client may call | nothing; the default of `broker.bndrun` |
| `bearer` | anything shared, and the integration tests | a token issuer the broker trusts: `DDSR_JWT_ISSUER`, `DDSR_JWT_JWKS_URI`, `DDSR_JWT_AUDIENCES` |
| `mtls` | few peers with certificates, no identity provider | HTTPS with client certificates (`DDSR_HTTPS_*`) and the roles per certificate CN (`DDSR_MTLS_ROLES`) — see [Security](SECURITY.md) §7d |

```bash
# the development launch, secured against a local Keycloak realm
DDSR_AUTH_MODE=bearer \
DDSR_JWT_ISSUER=http://localhost:8080/realms/fennec \
DDSR_JWT_JWKS_URI=http://localhost:8080/realms/fennec/protocol/openid-connect/certs \
  java -Dgosh.args=--nointeractive \
  -jar org.eclipse.fennec.services.broker.rest/generated/distributions/executable/broker.jar
```

In bearer mode every call needs `Authorization: Bearer <token>`, the
caller needs the role for what it does — `service-provider` to publish,
`service-consumer` to look up and hold a session, `service-admin` to
curate the catalog — and a provider can only withdraw or change what it
published itself. With Keycloak, add `DDSR_JWT_SCOPE_CLAIM=realm_access.roles`
so the broker reads realm roles. A broker
in bearer mode without an issuer serves nothing — it fails closed, so a
forgotten variable never leaves it open. What a client needs to obtain
a token, and why the switch exists at all, is in
[Security](SECURITY.md) §7a.

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
the same implementation** — `MessagingFlavor` beside `RestFlavor` — and
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

## 7. A contract on its own

A contract does not need an implementation. It goes into the catalog
alone, stays there whether anybody implements it or not, and outlives
every implementation that comes and goes. Consumers can rely on it
before the first provider exists — that is the point of keeping the
contract apart from its implementations (#271).

**Putting it in** needs the role `service-provider`:

| From | How |
| --- | --- |
| REST | `POST /catalog` with the `ServiceInterface` as XMI; 200, or 409 with code 202 when the same content is already there |
| Java | `BrokerCatalog.addCatalogEntry(contract, requestor)` — the Java provider of section 3 does exactly that before it publishes |
| TypeScript | `client.catalog.ensureEntry(contract)`; an entry that is already there counts as success |

**Taking it out** needs the role `service-admin`: `DELETE
/catalog/{name}`, `BrokerCatalog.removeCatalogEntry` or
`client.catalog.removeEntry(name)`. A contract nobody implements is
removed; while an implementation still uses it — live or parked in the
cold cache — the broker refuses with code 200 (`CATALOG_HAS_LIVE_IMPLS`,
HTTP 409). Withdraw the implementations first. With two contracts of
the same name, the sd1 fingerprint says which one (`?fingerprint=`, in
TypeScript the last argument of `getEntry`/`deprecateEntry`/`removeEntry`).

**What a consumer sees:**

| When | Lookup | Events | A proxy |
| --- | --- | --- | --- |
| before the first implementation | none — an empty list, never an error | nothing yet | none: there is nothing to bind to |
| an implementation arrives | it | `REGISTERED` | can be created now |
| it goes, and no other is left | none again | `UNREGISTERING` | a call fails with `DdsrServiceUnavailableException` (TypeScript `DdsrServiceUnavailableError`) |
| another one arrives | it | `REGISTERED` | the same proxy binds to it on the next call |

The catalog itself raises no events. To wait for an implementation,
listen for `REGISTERED` on the interface rather than calling in a loop;
the Java example consumer does it this way (`PaymentProxyRegistrar`),
and so does an RSA node that wants an interface before anybody exports
it. A listener also hears what happened while its event stream was
down: after a reconnect the SDK compares what it reads with what it
knew and delivers the difference ([Eventing](EVENTING.md)).

The catalog is read with `GET /catalog` and `GET /catalog/{name}` (any
authenticated caller) or `client.catalog.getEntry(name)` in TypeScript.
Harness scenarios B and V run all of this end to end.

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

## In your own workspace

Everything above runs from this repository. To build on Fennec Services
from another bnd workspace — one line for the repository, one per
project and launch for what it compiles against and runs — see
[bnd libraries](BND_LIBRARIES.md).
