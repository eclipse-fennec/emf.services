# Using Fennec Services from your own bnd workspace

Fennec Services ships two kinds of bnd library, the same way
[EMF OSGi](https://github.com/eclipse-fennec/emf.osgi) does: one for the
workspace, which makes the bundles resolvable, and four for projects,
which say what a project compiles against and what a launch runs.

> **Remote Service Admin is not published for now (#261).** The RSA
> bundles are not released to Maven Central, so there are no RSA project
> libraries either (`enableFennecServicesRsa`, `…RsaMqtt`, `…RsaAmqp`):
> one that named them would hand a downstream launch stale or missing
> snapshots. They come back with the bundles. Until then, a workspace that
> runs Remote Service Admin on this registry builds it from this
> repository.

| Library | Kind | Gives you |
| --- | --- | --- |
| `fennecServices` | workspace | a Maven repository with every bundle the four run lists below name, and the project libraries themselves |
| `enableFennecServicesClient` | project | the SDK: publish, look up, invoke |
| `enableFennecServicesSecurity` | project | bearer tokens: supply them, verify them |
| `enableFennecServicesMqtt` | project | the MQTT transport: call, hear events, serve a contract on topics |
| `enableFennecServicesAmqp` | project | the AMQP transport: call, serve a contract on routing keys |

## In the workspace

Add the workspace library to the `-library` of your `cnf/build.bnd` (or
the file your workspace keeps it in), next to the libraries you already
use:

```properties
-library: \
	fennecServices,\
	jakartaREST
```

That puts one repository into the workspace — *Eclipse Fennec Services*,
indexed from Maven Central and its snapshot repository — with exactly
what the four run lists name.

## In a project

```properties
# bnd.bnd of a project that publishes or consumes services
-library: enableFennecServicesClient
```

`project.bnd` of each library adds a `-buildpath.fennec.services.<role>`:

| Library | Compiles against |
| --- | --- |
| `enableFennecServicesClient` | `…services.api`, `…model`, `…runtime`, `…client.java` |
| `enableFennecServicesSecurity` | `…services.api` (`TokenSupplier`, `CallPrincipal`), `…security.jwt` (`SecurityNames`) |
| `enableFennecServicesMqtt` | `…flavor.messaging` (channels, where answers go, QoS), for a project that names placements itself |
| `enableFennecServicesAmqp` | `…flavor.messaging`, the same rules |

## In a launch

```properties
# app.bndrun
-library: \
	enableJakartaREST,\
	enableFennecServicesClient,\
	enableFennecServicesSecurity

-runbundles: \
	… your own bundles, Declarative Services, a configuration stack …
```

`bndrun.bnd` of each library adds a `-runbundles.fennec.services.<role>`,
which bnd merges into the launch's `-runbundles`. The lists carry this
project's bundles and what they need directly — never an HTTP stack or
Jersey, which `enableJakartaREST` (or your own choice) brings, and never
a configuration stack, which every launch picks itself. That is on
purpose: a library that brings a second HTTP stack into a launch is a
launch that takes minutes to start.

| Library | Runs |
| --- | --- |
| `enableFennecServicesClient` | the SDK and the REST transport, the wire codec, CloudEvents, EMF, protobuf |
| `enableFennecServicesSecurity` | the client-credentials token supplier, the JWT verifier and its filters, the client-certificate filter of mode `mtls`, Nimbus JOSE+JWT |
| `enableFennecServicesMqtt` | the messaging transport (`…client.messaging`, `…provider.messaging`, the flavor rules, the messaging link, Gecko Messaging `api` and `core`) with the `mqtt3` binding and the Paho build it drives — on top of the client. MQTT 5 (`mqtt.version=5`) is not in it: a launch that wants it adds `org.gecko.messaging.mqtt` and `org.eclipse.paho.mqttv5.client` |
| `enableFennecServicesAmqp` | the same messaging transport with the `amqp` binding, the RabbitMQ client and the Netty 4.2 it requires — on top of the client |

The security components act only where configured or where an
application asks for their mode, so the security list is safe in a
launch that runs open. How a node is
configured — `DDSR_OAUTH_*`, `DDSR_JWT_*` — is in
[Getting started](GETTING_STARTED.md#open-or-secured) and
[Security](SECURITY.md).

`enableFennecServicesMqtt` is the MQTT transport. Every MQTT part is
dormant until it is given a broker (`messaging.broker`), so the
transport list is safe in a launch that speaks only REST. A part
that is given a broker creates its connection as a messaging instance
by configuration, so such a launch needs the Configuration Admin
service — the one piece of the configuration stack the MQTT list does
count on. AMQP comes the same way, in `enableFennecServicesAmqp`. The transport
is the same bundles in both lists — one invoker and one distribution
for every protocol — and what a list adds is its binding and that
binding's client library. They are named in both, so that either list
stands alone.
Gecko Messaging 2.0 is a snapshot for now, which the
`fennecServices` index resolves from the Central snapshot repository.
Which
flavor a node speaks is in [Transports](TRANSPORTS.md).

## Versions

Every list names this project's bundles as a range on the release line —
`[0.1.0,0.2.0)` today, `base-version` up to the next minor: a patch
release is picked up, a minor release is a decision. Third-party bundles
carry the range this project's own launches resolve to.

## How the lists are kept honest

The lists are written once, in
`org.eclipse.fennec.services.bnd.library.project/library/`, and used
twice: the library bundle ships those directories, and
`org.eclipse.fennec.services.bnd.library.itest` runs them — with
`-library: <dir>;version=file`, so nothing has to be published first —
in one framework together with an HTTP stack, Jersey and Declarative
Services. Its test, part of every `./gradlew build`, checks that every
bundle a list names is installed and resolves, that nothing in the
framework stays unresolved, that every range follows `base-version`,
and that every bundle of ours is in the `fennecServices` index. A bundle
missing from a list is a red build, not a downstream launch that does
not start.
