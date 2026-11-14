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

### Methods that answer later

A method may return `org.osgi.util.promise.Promise`,
`java.util.concurrent.Future`, `CompletionStage` or `CompletableFuture`
— the holder types of the `osgi.async` intent (Remote Services §100.6),
which every distribution provider here promises:

```java
public interface Greeter {
    String greet(String name);
    Promise<String> greetLater(String name);
}
```

- **To the contract it returns what the holder holds.**
  `Promise<String>` is a string, exactly as `String` is, with
  `delivery="PROMISE"` on the return value: the contract says that the
  answer is an event, for whoever generates code from it or sets a
  timeout. It does not change the contract's identity — `PROMISE` is
  not part of the fingerprint — so a provider may answer later while
  its consumer reads synchronously, and the other way round.
- **The proxy returns the holder at once** and asks the transport for
  the call in the way that does not wait. The holder fails with the
  exception a synchronous call would have thrown; a declared error is
  the class the method declares for it.
- **A provider is answered when its holder settles.** Over a messaging
  broker the dispatch returns immediately, so a slow call no longer
  holds up the calls behind it on the same subscription. Over REST the
  request thread waits — an HTTP request is answered once.

What a call occupies while it waits depends on the transport. **Over a
messaging broker, nothing**: the request is published, the answer is
delivered, and the holder settles on the thread that brings it — no
thread of the consumer waits, for the connection, the hand-over or the
answer. **Over REST, one thread** of the proxy factory until the
response is there; the HTTP client underneath waits. A transport of
your own gets this by implementing `ServiceInvoker.invokeAsync`; the
default makes the synchronous call on a thread it is given.

The example's `Greeter.greetLater` is this, and the harness runs it
over MQTT and AMQP.

### Streams

A slot may be a stream: any number of values, one after the other, over
time. In the model that is `delivery="STREAM"` on a return value or on
an argument (`Parameter.delivery`), and a derived contract says it for a
method that returns or takes an `org.osgi.util.pushstream.PushStream<T>`
or a `java.util.concurrent.Flow.Publisher<T>`:

```java
PushStream<Double> readings(String sensorId);        // the provider streams to the caller
void feed(String sensorId, PushStream<Double> in);   // the caller streams to the provider
```

A stream is another exchange than a list of the same type and therefore
another contract: `STREAM` is part of the fingerprint.

**A stream a service returns travels over every transport.** Over REST
it is the answer to the call as Server-Sent Events, one event per value
([the wire](WIRE_FORMAT.md#a-stream-over-rest)): the caller ends it by
closing the connection, and TCP holds the provider back while the
caller reads slowly. Over a messaging broker, MQTT and AMQP alike, it
travels in a **duplex session** of Gecko Messaging: the call
opens it, the provider accepts it, and every value is one message of
the session until the stream is over
([the wire](WIRE_FORMAT.md#a-stream-as-the-answer)). On the other side
the proxy hands back a stream of the type the method declares —
`PushStream<T>` (the Push Stream bundle is then needed in the launch;
the SDK imports it optionally) or `Flow.Publisher<T>` — and opens the
session when the stream is consumed: a terminal operation on the
`PushStream`, `subscribe` on the publisher. A failure, a declared error
included, ends the stream as its error.

**A caller that leaves stops the stream at its source.** Closing the
`PushStream`, cancelling the subscription or leaving a `for await` in
TypeScript closes the session, and the provider's stream is closed with
it. Both sides keep the session alive with pings within its lease (10 s
by default); a side that hears nothing for a lease ends it. So a caller
that goes away without a word — a crash, a lost network — is noticed
too, and a slow stream lives on as long as both sides are there.

A service that streams is called over the first flavor it announces
whose transport carries streams — REST and messaging both do now.

What is refused, with the slots named, rather than accepted and failing
on the first call: **a stream as an argument**, on every transport: the
session can carry it, and nothing here sends one yet
([#248](https://github.com/eclipse-fennec/emf.services/issues/248)).

The same holds for a contract document served by the generic REST or
messaging distribution.

Limits worth knowing before you build on it:

- **A slow consumer holds the provider back.** The consumer grants the
  provider credit — as many values as its buffer holds — and grants
  more as its application takes them; the provider sends only within
  the credit and holds the service's stream otherwise. A Java consumer
  grants its receive buffer, a TypeScript one its `streamBuffer` (256
  values by default; the test in the harness reads thirty values
  through a buffer of four). A consumer that would rather skip values
  than slow the source down says so: `policy: 'DISCARD_OLDEST'` in
  TypeScript, and then grants no credit and keeps the latest.
- **A session stays in the trees a broker's ACL is written for**: the
  caller listens in its reply subtree where the flavor names a reply
  tree, the provider below the operation's request channel
  ([Security](SECURITY.md)). A flavor that names no reply tree gets a
  channel the messaging library generates.

#### A stream every receiver shares

Some streams have one source and many listeners at once — readings,
prices, a live ledger. Calling once per listener would run the source
once per listener, and holding the source back for the slowest would
slow down everyone. For these the contract says `BROADCAST`
([#252](https://github.com/eclipse-fennec/emf.services/issues/252)), and
a Java interface says it with `@Broadcast`:

```java
@Broadcast(overflow = "DISCARD_OLDEST")   // org.eclipse.fennec.services.derive
PushStream<Integer> beats();
```

A broadcast is a return value of a method **without arguments**: it is
subscribed to, not called. The service is asked for the stream once, and
what it sends goes to every listener at the source's pace. There is **no
backpressure and no credit**: the source never learns how fast a
listener is. Each listener protects itself with a buffer of its own, and
the contract's **overflow policy** (`Parameter.overflow`) says what may
be lost when it is full:

| Policy | A listener that falls behind | For |
| --- | --- | --- |
| `FAIL` (default) | loses nothing: once it has taken what its buffer holds, its stream fails ("fell behind") and it starts over | postings, anything that must not be lost |
| `DISCARD_OLDEST` | skips: the oldest value it holds makes room for the newest — latest wins | readings, where only the current value counts |
| `BLOCK` | loses nothing and holds up its own delivery; a messaging broker may drop a listener that stays slow, and over REST it holds the source | rarely what you want |

The policy is part of the fingerprint: a contract that changes what a
listener may lose is another contract. A listener may make the policy
stricter, never looser.

How it travels:

- **over a messaging broker** the provider publishes every value on the
  operation's channel; a listener subscribes to it through Gecko
  Messaging's per-subscription buffer (`bufferSize`, `queuePolicy`). A
  Java listener's buffer is `broadcast.buffer` of the messaging invoker
  (256 values), a TypeScript one's the plugin's `streamBuffer.size`.
  TypeScript cannot hold up a single mqtt.js or amqplib subscription, so
  `BLOCK` fails there too.
- **over REST** the provider is the distribution point: every listener
  holds a connection, the service is asked when the first one comes and
  its stream is cancelled when the last one goes. The listener's buffer
  is on the provider's side of the connection (256 values).

The example's `Ticker` has all of this: `count` (`PushStream<Integer>`)
ends by itself, `forever` does not, and `beats` is a broadcast where
latest wins. The harness runs them over MQTT and AMQP — bound by
`@Reference` in a second framework and iterated by a TypeScript
consumer as an `AsyncIterable<number>` — and checks that the provider's
`forever` stops when each of the two callers leaves after three
numbers, that a Java listener hears fifty beats in a row, and that of
two TypeScript listeners at once the fast one hears every beat while
the slow one (a buffer of four, a tenth of a second per beat) skips to
the latest.

The one thing a consumer of a possibly remote service should not do is
call it from `activate`: the call waits for a connection and an answer
on a thread Declarative Services shares with others (see
[Transports](TRANSPORTS.md)). The example's consumers start a thread
of their own for it.

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
before anything is written. Over MQTT the same key and trust store also
reach the node's MQTT connections — the invoker, the distribution's
connection and the event source — for a messaging broker that demands
a certificate; a REST-only node writes none of them.

Everything both roles accept is in the metatype, visible in any OSGi
console, on `RsaProvider.Config` and `RsaConsumer.Config`.

## The pieces

| Bundle | What it is |
| --- | --- |
| `…rsa` | the `RemoteServiceAdmin` itself, and two SPIs |
| `…rsa.distribution.rest` | serving an exported service over REST |
| `…rsa.distribution.messaging` | the same over a messaging broker: one provider for `fennec.mqtt` (#98) and one for `fennec.amqp` (#138), on one distribution |
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

`fennec.amqp` is the third configuration type. A provider that serves
over AMQP and announces over REST says

```json
"org.eclipse.fennec.services.rsa.provider": {
  "distribution.flavor": "fennec.amqp",
  "amqp.url": "amqp://localhost:5672",
  "broker.url": "http://localhost:8887/ddsr/rest"
}
```

and a consumer `"flavor": "fennec.amqp"` with its own `amqp.url`. A
node whose one flavor is AMQP hears the registry's events over AMQP as
well: the same event transport as over MQTT, given an `amqp://` URL,
subscribed to the routing key `ddsr.events.#`. That needs a registry
that publishes there (`DDSR_AMQP_URL`, see
[Deployment](DEPLOYMENT.md)); where it does not, the node says
`"discovery.flavor": "fennec.rest"`. Calls travel to the topic exchange under
`ddsr.rpc.<provider>.<contract>.<op>`; the answer comes back on the
reply address of the protocol, in a queue only the caller can read, so
there is no reply tree and no ACL to write for it. A user and password
for the broker are not part of `amqp.url` — the URL is announced with
every export — but of the connection
([Transports](TRANSPORTS.md#providermessaging--serving-a-contract-over-a-messaging-broker)).
The harness runs this as scenario R.

**What a call is encoded in.** An exported interface has no contract
document that could say it, so the role may: `"encoding":
"application/json"` on the provider makes every call and answer of what
it serves over a messaging broker JSON instead of XMI. The consumer is
told nothing — it reads the encoding off the announced flavor. Both
launches need a resource factory registered for that content type;
for JSON that is emf.codec (`org.eclipse.fennec.codec`). A node that
has none refuses to write or read the document rather than falling
back to XMI. Scenario S of the harness is that call.

**How large a call may be.** A body is read up to 1 MiB and refused
above it (413 over REST). A node that takes larger models says so on
its role: `"max.body.bytes": 33554432` on the provider raises what its
exports read — over REST the request body, over a messaging broker the
call's document and what the connection carries; on the consumer it
raises what an answer over a messaging broker may be. Zero, the
default, leaves the megabyte
([Transports](TRANSPORTS.md#how-large-a-body-may-be)).

Three things the role configuration keeps in step, each of which is a
silent failure when done by hand:

- a node hearing over MQTT gets the event transport configured and its
  client pointed at it;
- a node serving over MQTT or AMQP gets no HTTP stack, because an
  export that answers on topics or routing keys needs no server;
- the client's `supported.flavors` follows what the node actually
  speaks. A lookup filters by it, so a node that says REST is never
  shown a service reachable only over MQTT — and nothing reports that,
  because from the broker's side nothing went wrong. The same list is
  written a second time as `invokers.target`, the condition the client
  waits on: there is no client before there is an invoker for each of
  those flavors.

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
  contract at all, and neither do MQTT and AMQP, where a call carries
  no credential of its own: refused, never served open.
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

- **Three flavors ship**, `fennec.rest`, `fennec.mqtt` and
  `fennec.amqp`, and a node may use one for serving and another for
  announcing — see
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
