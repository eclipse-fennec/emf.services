# Eclipse Fennec Services

A **cross-language service registry** (working title *DDSR — Dynamic Distributed Service Registry*): a language-independent, EMF/Ecore-based OSGi-style service model with a broker, REST/SSE and MQTT event transports, and SDKs for providers and consumers in **Java** and **TypeScript**.

- Providers publish typed service descriptions (interfaces, operations, flavors, properties) into a broker catalog; consumers discover and invoke them peer-to-peer over the announced flavor (REST today, MQTT in progress).
- Lifecycle follows the OSGi model: consumers are informed **before** a service disappears — a producer's shutdown blocks until the broker confirmed the withdrawal and fanned the `UNREGISTERING` event out.
- Service descriptions are **content-addressed**: the `sd1` fingerprint makes producer, broker and consumer views comparable with a string comparison, computed identically in both languages and pinned by a shared golden test.

## Layout

| Area | Content |
|---|---|
| `org.eclipse.fennec.services.*` | Java bundles: model (EMF codegen), broker (core/REST/MQTT), client SDK (core/REST/MQTT), XMI codec + fingerprint, examples |
| `ddsr-ts-client/` | TypeScript track (pnpm workspace): model, client SDK, REST flavor, MQTT transport |
| `docs/` | The documentation source of truth (published subset: see `docs-site/`) |
| `itest/` | Cross-language harness — Java↔TS end-to-end against a real broker, as host processes or podman containers (incl. a Mosquitto MQTT wire proof) |
| `docker/` | Build contexts for the deployable images — today the broker (see [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)) |

## Build

```bash
./gradlew build                                   # Java: full build incl. tests
cd ddsr-ts-client && corepack pnpm install && corepack pnpm -r build && corepack pnpm exec vitest run
./itest/run-harness.sh                            # cross-language end-to-end
```

A broker to develop against, without building anything:

```bash
docker run -d -p 8887:8887 -v ddsr-broker-data:/opt/services/data \
  docker.io/eclipsefennec/emf.services:broker-snapshot
```

Requires Java 21 and Node ≥ 20. See [CLAUDE.md](CLAUDE.md) for the workspace map and [docs/STATUS.md](docs/STATUS.md) for where the project stands.

## License

[Eclipse Public License 2.0](LICENSE)
