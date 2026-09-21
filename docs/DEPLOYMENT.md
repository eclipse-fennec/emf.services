# Deployment

The deployable artifact of this repository is a **container image of the
broker**. Everything else — providers, consumers, the TS track — is a
library or an example: those run wherever your application runs and only
need the broker's URL. This document covers the image, its
configuration surface and the CI that publishes it. For running the
whole flow from source see [GETTING_STARTED.md](GETTING_STARTED.md).

## The image

| | |
|---|---|
| Docker Hub | `docker.io/eclipsefennec/emf.services` |
| GHCR | `ghcr.io/eclipse-fennec/emf.services` |
| Tags | `broker-snapshot` (branch `snapshot`), `broker-latest` (branch `main`), plus the immutable `broker-<bundle version>` of every build |
| Platforms | `linux/amd64`, `linux/arm64/v8` |
| Base | `gcr.io/distroless/java21-debian12:nonroot` — no shell, no package manager, runs as uid **65532** |
| Content | the bnd executable jar exported from `broker.bndrun` (REST + SSE, MQTT bundles present but dormant) |

The image contains exactly the `broker.jar` that was exported in the
same CI build that published the bundles to Maven, so an image tag and a
bundle version always describe the same code.

## Running it

```bash
docker run -d --name ddsr-broker \
  -p 8887:8887 \
  -v ddsr-broker-data:/opt/services/data \
  -e DDSR_PUBLIC_URL=http://broker.example.com:8887/ddsr/rest \
  docker.io/eclipsefennec/emf.services:broker-snapshot

curl http://localhost:8887/ddsr/rest/catalog     # readiness
```

The broker is up as soon as `GET /ddsr/rest/catalog` answers 200 —
about two seconds. There is no `HEALTHCHECK` in the image because the
distroless base ships neither a shell nor curl; wire that probe up in
your orchestrator instead (Kubernetes: an `httpGet` readiness probe on
`/ddsr/rest/catalog`).

### Configuration

Every knob is an environment variable, read by the broker's
`OSGI-INF/configurator` JSON through the Felix ConfigAdmin
interpolation plugin. The defaults are the ones
[GETTING_STARTED.md](GETTING_STARTED.md) uses, so a host launch of
`broker.jar` behaves identically and honours the same variables.

| Variable | Default | Meaning |
|---|---|---|
| `DDSR_PUBLIC_URL` | `http://localhost:8887/ddsr/rest` | The address the broker **advertises for itself** in the catalog. Consumers dial exactly this URL, so in any deployment other than `--network=host` it must be the externally reachable one. |
| `DDSR_HTTP_PORT` | `8887` | Port the broker listens on (inside the container). |
| `DDSR_HTTP_HOST` | `0.0.0.0` | Interface the broker binds. |

The context path (`/ddsr/rest`) is a frozen wire contract, not a
configuration — see the wire-name note in [CLAUDE.md](https://github.com/eclipse-fennec/emf.services/blob/snapshot/CLAUDE.md).

### Events over MQTT, when a deployment wants them

The image carries the MQTT event transport and leaves it asleep. One
variable turns it on; nothing else about the container changes, and
there is no second image:

```bash
docker run -d --name ddsr-broker \
  -p 8887:8887 \
  -e DDSR_PUBLIC_URL=http://broker.example.com:8887/ddsr/rest \
  -e DDSR_MQTT_URL=tcp://mosquitto:1883 \
  docker.io/eclipsefennec/emf.services:broker-snapshot
```

| Variable | Default | Meaning |
|---|---|---|
| `DDSR_MQTT_URL` | *(empty)* | Where lifecycle events are published. **Empty means no MQTT at all**: the transport registers no sink, logs one line saying so, and the broker serves its events over SSE alone. |
| `DDSR_MQTT_TOPIC_PREFIX` | `ddsr/events` | Events go to `<prefix>/<interface>`. A frozen wire name until #4 — change it only if both sides of your deployment agree. |
| `DDSR_MQTT_CLIENT_ID` | `ddsr-broker` | MQTT client identifier. Two brokers on one MQTT broker need two, or they disconnect each other. |
| `DDSR_MQTT_EVENT_SOURCE` | `/fennec/services/broker` | The CloudEvents `source` of every event this broker sends — what a reader uses to tell two brokers apart. |
| `DDSR_MQTT_QOS` | `0` | Fits a stream whose consumers re-snapshot on reconnect anyway; raise it if your consumers do not. |

Both transports run side by side: a consumer that subscribes over SSE
and one that subscribes over MQTT see the same events, and
[EVENTING.md](EVENTING.md) says what each is guaranteed.

What the image does **not** carry yet is OpenTelemetry. The SDK is a
snapshot dependency whose propagators are still a no-op upstream
(eclipse-osgi-technology/opentelemetry#22), and
[TELEMETRY.md](TELEMETRY.md) describes how a deployment adds it in the
meantime.

### State and the data volume

The broker persists its registry as a synchronous XMI snapshot after
every acknowledged mutation, to `broker-state.xmi` in its working
directory — which in the image **is** the volume mount point
`/opt/services/data`. On start it restores the snapshot if one is there,
so a restart keeps catalog, providers and implementations.

A snapshot is only readable by the model version that wrote it. EMF
rejects a document carrying a feature the current model no longer has —
`ServiceOperation.returnType` became `returnValue` with issue #41, for
example — and the broker then logs a WARNING and **starts with an empty
registry**. Upgrading across such a model change therefore means the
providers re-publish; delete the stale `broker-state.xmi` so the warning
does not repeat on every start.

The mount has to be writable by uid 65532. A named volume inherits the
image's ownership and just works; a **bind-mounted host directory does
not** unless you chown it:

```bash
mkdir -p /srv/ddsr-broker && chown 65532:65532 /srv/ddsr-broker
docker run ... -v /srv/ddsr-broker:/opt/services/data ...
```

Getting this wrong fails in a way worth knowing about: a mutation whose
persist fails is **rolled back**, so the broker starts, answers, and
reports `cannot self-publish: at least one API interface is missing from
the catalog` — an empty catalog rather than a crash.

## Building the image locally

```bash
./gradlew :org.eclipse.fennec.services.broker.rest:export.broker \
          :docker:broker:prepareDocker
podman build -t emf.services/broker:local docker/broker/
```

`prepareDocker` stages `broker.jar` into `docker/broker/content/`, which
is the same layout the CI container job assembles — so a local build and
a CI build read the same `Dockerfile` with the same context.

## The CI pipeline

```
push to snapshot / main
  └─ verify            (reusable-verify, eclipse-fennec/.github)
      └─ release       (reusable-release) — build + testOSGi + export.broker + publish
          │              uploads broker.jar and the broker.rest bundle jar as "release-jars"
          ├─ docs       (reusable-docs)
          └─ container  (reusable-container, eclipse-fennec/.github)
                         downloads "release-jars", reads Bundle-Version off the
                         bundle jar, buildx-builds docker/broker/ for amd64+arm64,
                         pushes to Docker Hub and GHCR
```

The container job never rebuilds the jar — that is what keeps the image
from drifting away from the published bundles.

All four jobs are central workflows owned by `eclipse-fennec/.github`
and pinned here by commit. This repository states only what is its own:
the image name, the variant prefix, the build context and the two jar
names. The tags that come out are `broker-<label>` and
`broker-<Bundle-Version>`, in both registries.

Prerequisites on the repository:

- secrets `DOCKER_USERNAME` and `DOCKER_API_TOKEN` (Docker Hub); GHCR
  uses the run's `GITHUB_TOKEN`, which is why the calling workflows
  raise `packages: write`.

To add a second variant, add a `docker/<variant>/` context with its
`Dockerfile` and `prepareDocker` task, then add a second `container-*`
job with that variant's `runtime-jar`/`docker-context`; the variants
then build in parallel and a broken one does not block the others.

A variant is for an image that is genuinely a different thing to run.
Something a deployment merely switches — a second transport, an
exporter, a port — belongs in an environment variable of the one image,
which is exactly why MQTT is a variable here and not a `broker-mqtt`
image: two images that differ by a configuration are two images to
build, scan, sign and keep in step.
