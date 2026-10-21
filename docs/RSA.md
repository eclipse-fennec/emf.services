# Remote Service Admin

OSGi has a specification for making a service in one framework usable
in another: Remote Service Admin, chapter 122. Fennec Services
implements it on top of the registry described in
[Overview](OVERVIEW.md).

The point is what you *do not* write. An ordinary OSGi service, with
one property added, becomes callable from another framework. No
contract document, no generated stubs, no IDL.

## The smallest example

In the exporting framework:

```java
@Component(
        service = Greeter.class,
        property = {
                "service.exported.interfaces=*",
                "service.exported.configs=fennec.rest"
        })
public class GreeterImpl implements Greeter {
    public String greet(String who) {
        return "Hello, " + who + "!";
    }
}
```

In the importing framework:

```java
@Reference
private Greeter greeter;
```

That is the whole of it. `service.exported.interfaces=*` is the
specification's way of saying "export me"; `service.exported.configs`
names the flavor. The contract for `Greeter` is derived from the Java
interface by reflection (`…services.derive`), published to the broker,
and found again on the other side.

One optional property is worth adding in practice:
`ddsr.provider.name` gives the registration a name you will recognise
in the broker's catalogue instead of a generated one.

`…examples.rsa` and `…examples.rsa.consumer` are exactly this example,
runnable.

## How a node is configured

One configuration per role (#109). A node that serves:

```json
{
  "org.eclipse.fennec.services.rsa.provider": {
    "broker.url": "http://localhost:8887/ddsr/rest",
    "http.port": "9095",
    "context.path": "services",
    "registry.name": "my-node"
  }
}
```

A node that only consumes:

```json
{
  "org.eclipse.fennec.services.rsa.consumer": {
    "broker.url": "http://localhost:8887/ddsr/rest",
    "registry.name": "my-consumer",
    "consumer.id": "my-consumer"
  }
}
```

From that, a configuration component derives the individual
configurations for the client, the transports, the local registry, the
admin and the topology manager, in an order that brings them up and
takes them down safely. Before #109 a deployment wrote those nine by
hand and had to keep several of their values in agreement.

Two values are worth knowing about:

- **`public.url`** is derived from `http.port` and `context.path`
  unless you state it. State it when something sits in front of the
  node — a reverse proxy, a container port mapping — because then the
  address consumers should dial is genuinely not the one the node
  binds.
- **`policy`** and **`import.policy`** are `promiscuous` by default:
  export everything that asks, import everything anyone waits for. Set
  either to `manual` to do it yourself through the `RemoteServiceAdmin`
  service.

A node's **identity** is said in the same place — its client
credentials, and for a serving node the issuer it accepts:

```json
"org.eclipse.fennec.services.rsa.provider": {
  "...": "as above",
  "token.uri": "https://idp.example.com/realms/fennec/protocol/openid-connect/token",
  "client.id": "my-node",
  "client.secret": "…",
  "scope": "ddsr-broker",
  "issuer": "https://idp.example.com/realms/fennec",
  "jwks.uri": "https://idp.example.com/realms/fennec/protocol/openid-connect/certs",
  "audiences": ["my-node"],
  "scope.claim": "realm_access.roles",
  "subject.claim": "azp"
}
```

From it the role derives the token supplier, the verifier and the
scheme the REST distribution names — the same issuer in all three — and
uses the client id as the consumer id unless one is said. The consumer
role takes the four client attributes only. Without them a node is what
it was: open, and calling without a token. See [Who may call](#who-may-call).

Against a broker in mode `mtls` ([Security](SECURITY.md) §7d) both
roles also take the node's **certificate**, and can prove themselves to
the broker with it while calls to providers keep their tokens:

```json
"org.eclipse.fennec.services.rsa.consumer": {
  "...": "as above",
  "broker.url": "https://registry.example.com:8443/ddsr/rest",
  "broker.authentication": "certificate",
  "tls.keystore": "/etc/fennec/tls/my-node.p12",
  "tls.keystore.password": "…",
  "tls.truststore": "/etc/fennec/tls/truststore.p12",
  "tls.truststore.password": "…"
}
```

The role derives them into the client's REST transport. Give the
certificate the CN of the client id, so the node is one name for the
broker and for the providers it calls; `broker.authentication` is
`token` unless said, and `certificate` without a key store is refused
before anything is written.

Everything both roles accept is in the metatype, visible in any OSGi
console, on `RsaProvider.Config` and `RsaConsumer.Config`.

## The pieces

| Bundle | What it is |
| --- | --- |
| `…rsa` | the `RemoteServiceAdmin` itself, and two SPIs |
| `…rsa.distribution.rest` | serving an exported service over REST |
| `…rsa.distribution.mqtt` | the same over MQTT (#98) |
| `…rsa.discovery.rest` | announcing and finding endpoints via the broker |
| `…rsa.discovery.mqtt` | the same, with the broker's events heard over MQTT |
| `…rsa.discovery.local` | endpoints declared in a bundle (122.6.2 XML) |
| `…rsa.topology` | deciding what gets exported and imported |
| `…rsa.config` | one configuration per role |

The two SPIs are the extension points. **`FlavorDistribution`** knows
how to serve a contract over one flavor; **`ServiceDiscovery`** knows
how to announce and find one. MQTT arrived as a second pair of these
and two configuration lines (#98), and the admin did not change —
which is what the SPIs were for.

An admin serves exactly one configuration type, named by
`remote.configs.supported`, and its distribution and discovery are
selected by target filter from its configuration. A node that exports
nothing says so, by pointing its distribution at the one that exports
nothing, rather than being left with a reference that finds nobody.

## Two transports, and splitting them

`fennec.mqtt` is the second configuration type. A service asks for it
the way it asks for anything:

```java
property = { "service.exported.interfaces=*",
             "service.exported.configs=fennec.mqtt" }
```

Calls then travel over MQTT: `<prefix>/req/<provider>/<contract>/<op>`
carries the call, `<prefix>/res/<provider>/<contract>/<consumer>/<call>`
carries the answer, both as CloudEvents with a `ServiceInvocation`
inside (#101). The two trees are separate so that a broker ACL can say
who may call and who may read — [Wire format](WIRE_FORMAT.md) has the
rules and a sketch of the ACL.

**Serving and announcing are two halves, and a node may split them.**
The role configuration takes `distribution.flavor` and
`discovery.flavor`, each falling back to `flavor`:

```json
"org.eclipse.fennec.services.rsa.provider": {
  "distribution.flavor": "fennec.mqtt",
  "mqtt.url": "tcp://localhost:1883",
  "broker.url": "http://localhost:8887/ddsr/rest"
}
```

That node serves over MQTT and announces over REST — which is the
pairing that shows the two SPIs are really independent, and what the
harness runs as scenario I.

Three things the role configuration keeps in step, each of which is a
silent failure when done by hand:

- a node hearing over MQTT gets the event transport configured and its
  client pointed at it;
- a node serving over MQTT gets no HTTP stack, because an export that
  answers on topics needs no server;
- the client's `supported.flavors` follows what the node actually
  speaks. A lookup filters by it, so a node that says REST is never
  shown a service reachable only over MQTT — and nothing reports that,
  because from the broker's side nothing went wrong.

## Topology

The topology manager decides *what* gets exported and imported; the
admin only does as it is told. Three of them ship:

- `ExportEverythingAsked` — exports every service carrying
  `service.exported.interfaces`
- `ImportWhatIsAskedFor` — watches for services somebody in this
  framework is waiting for, and imports those
- `ImportWhatIsDiscovered` — imports every endpoint discovery reports

A service that asks to be exported before any admin exists is not
lost: when an admin arrives, it is offered everything that has been
waiting. The same holds for endpoints discovery reported early. That
was not true until recently, and it made the example export nothing at
all.

`ExportEverythingAsked` exports on a thread of its own, never on the
thread that registered the service or the admin. That thread may be
the one an export waits for: the REST admin comes up on the Jakarta
REST whiteboard's update thread, and serving a contract waits until
that whiteboard has deployed it. Exporting there waited for itself
(#164). An export that fails does not count as taken: its registration
is closed, and the service is asked about again when it changes or
another admin arrives.

## Where imported proxies live

A proxy for an imported service is registered by an empty host bundle
(`ProxyHost`), not by the RSA bundle. Registering it from the RSA
bundle failed the framework's class-space check: the RSA bundle does
not, and should not, import the consumer's interface package.

## How much of the specification is satisfied

The OSGi Remote Service Admin TCK 8.1.0 runs green: **39 of 39**.
`org.eclipse.fennec.services.rsa.tck` is the launch,
`./itest/run-tck.sh` runs it. The broker runs outside the test.

Implemented and exercised by the TCK: export and import registrations
with `update`, endpoint descriptions and events, the listener contracts
of 122.6, the Event Admin topics of 122.7, and the capabilities that
declare which configuration types and policies this implementation
speaks.

## Who may call

A service says who may call it the way a JAX-RS resource does, with the
Jakarta security annotations on its interface ([Security](SECURITY.md)
§6):

```java
@RolesAllowed("ledger-reader")              // the contract's default
public interface Ledger {

    double balance(String account);         // ledger-reader

    @RolesAllowed("ledger-writer")          // this operation's own
    double record(String account, double amount);

    @PermitAll                              // anybody
    String version();
}
```

Nothing else changes in the service: it is exported and imported like
any other.

- **Export.** The derived contract carries the annotations as its
  requirements — `@RolesAllowed` as `scopes`, `@PermitAll` as an
  explicit open operation; `@DenyAll` is refused, since a contract
  cannot state an operation nobody may call. The REST distribution puts
  the node's bearer scheme (issuer, audience, JWKS) into the flavor and
  serves the endpoint behind the bearer filter; every operation is then
  held to its requirement — `401` without a token, `403` naming the
  missing role. A node without an issuer does not export a guarded
  contract at all, and neither does MQTT, where a call carries no
  credential of its own: refused, never served open.
- **Import.** The proxy is a plain `@Reference`. Every call carries the
  importing node's token — one of the provider's audience, asked for
  from the identity provider per target, so a token minted for the
  broker never reaches a provider. A guarded operation on a node without
  credentials fails with that reason rather than a bare `401`.
- **Intents.** A node with an issuer promises `fennec.bearer`; an
  endpoint carries it where its contract demands a caller, on both
  sides, so a consumer can filter for it. A service that asks for
  `fennec.bearer` in `service.exported.intents` without annotating gets
  a contract that demands an authenticated caller and nothing more.

Roles are whatever the verifier reads from the token: with
`scope.claim=realm_access.roles` they are Keycloak realm roles, so
`@RolesAllowed("ledger-writer")` and a realm role `ledger-writer` meet
without a mapping in between.

## Limits worth knowing

- **Two flavors ship**, `fennec.rest` and `fennec.mqtt`, and a node may
  use one for serving and the other for announcing — see
  [Two transports](#two-transports-and-splitting-them).
- **Calls carry the node's identity, not a user's.** An imported proxy
  calls with the importing node's own client credentials. Acting on
  behalf of a user — token exchange — is not built; see
  [Security](SECURITY.md) §6.
- **Intents are checked at export, not verified in flight.** A service
  asking for an intent the transport does not promise is not exported,
  which is the safe direction: a consumer relying on that intent would
  otherwise be told a lie. What nothing checks is that a transport
  promising `osgi.basic` actually behaves that way — only that it
  promised.
- **A contract derived from a Java interface** covers what reflection
  can see. Generics beyond the simple cases, and anything depending on
  parameter names at runtime, are the places to look first when a
  derived contract is not what you expected.

## Read next

- [Transports](TRANSPORTS.md) — what REST needs configured
- [Fingerprints](FINGERPRINTS.md) — why an imported service may
  refuse a provider
