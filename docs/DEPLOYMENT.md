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

### State and the data volume

The broker persists its registry as a synchronous XMI snapshot after
every acknowledged mutation, to `broker-state.xmi` in its working
directory — which in the image **is** the volume mount point
`/opt/services/data`. On start it restores the snapshot if one is there,
so a restart keeps catalog, providers and implementations.

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

### MQTT

The image carries the MQTT bundles but no MQTT configuration, so the
transport stays dormant and the broker is REST/SSE-only — that is the
deliberate split described in
[the harness MQTT config bundle](https://github.com/eclipse-fennec/emf.services/blob/snapshot/org.eclipse.fennec.services.itest.mqtt.config/bnd.bnd):
production launches ship the MQTT bundles unconfigured, and the
`broker-mqtt` launch variant that wakes them is harness-only (it hard-wires
`tcp://localhost:1883`). An MQTT-enabled image therefore needs its own
configuration bundle and image variant; it does not exist yet.

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
          └─ container  (.github/workflows/reusable-container.yml)
                         downloads "release-jars", reads Bundle-Version off the
                         bundle jar, buildx-builds docker/broker/ for amd64+arm64,
                         pushes to Docker Hub and GHCR
```

The container job never rebuilds the jar — that is what keeps the image
from drifting away from the published bundles.

Prerequisites on the repository:

- secrets `DOCKER_USERNAME` and `DOCKER_API_TOKEN` (Docker Hub); GHCR
  uses the run's `GITHUB_TOKEN`, which is why the calling workflows
  raise `packages: write`.

To add a second variant, add a `docker/<variant>/` context with its
`Dockerfile` and `prepareDocker` task, then call
`reusable-container.yml` once more with that variant's
`runtime-jar`/`docker-context`.
