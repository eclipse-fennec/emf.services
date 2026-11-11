# How the Java client is wired

The client SDK is a handful of Declarative Services components. This
page says which they are, what each one waits for, and how to read a
node that does not come up. It is the companion to
[Transports](TRANSPORTS.md), which lists the configuration, and to the
[client framework guide](CLIENT_FRAMEWORK_GUIDE.md), which is about the
API.

## The rule behind it

**The configuration is the setup.** What a node is supposed to consist
of — which broker, which token supplier, which flavors — is said in
its configuration, and the components are wired so that a node which
does not match its configuration is *not there*, rather than there and
quietly wrong.

That comes down to four habits:

- A collaborator the configuration prescribes is a **mandatory, static**
  reference. Which service it is, is the **target** of that reference,
  written into the configuration as an LDAP filter
  (`<reference>.target`).
- A component whose targets come from the configuration takes
  `ConfigurationPolicy.REQUIRE`. A static reference whose target
  changes rebuilds the component; one that started on defaults would
  be torn down the moment its configuration arrives, together with
  everything bound to it.
- **Optional** is for what a node really can do without — the
  telemetry seam (`CallTracer`), nothing else here. Optional means
  dynamic, bound by method, held in an `AtomicReference`.
- **Multiple and dynamic** is a whiteboard, and there is exactly one in
  the client: the one that collects invokers.

## The components

```
            configuration                      configuration
                 │                                   │
                 ▼                                   ▼
          ┌──────────────┐   tokens        ┌──────────────────┐
          │ RestTransport│◀───────────────▶│ TokenSupplier    │
          └──────┬───────┘  (two targets)  │ none | oauth     │
                 │                         └──────────────────┘
     ┌───────────┼────────────┬──────────────────┐
     ▼           ▼            ▼                  ▼
 broker proxies  RestEventSource   RestServiceInvoker   MessagingServiceInvoker
 (catalog, impl, (EventSource)     (ddsr.flavor=REST)   (ddsr.flavor=MQTT,AMQP)
  lookup, sessions)                       │                  │
     │               │                    └────────┬─────────┘
     │               │                             ▼  0..n, dynamic
     │               │                    ┌──────────────────┐
     │               │                    │ InvokerRegistry  │
     │               │                    │ = InvokerProvider│
     │               │                    │ fennec.invokers= │
     │               │                    │   [REST, MQTT, …]│
     │               │                    └───┬──────────┬───┘
     │               │      invokers.target   │          │ (no target)
     ▼               ▼      waited for only   ▼          ▼
   ┌───────────────────────────────────────────┐   ┌──────────────────┐
   │ DdsrClient                                │   │ ServiceProxy-    │
   │ configuration required                    │   │ Factory          │
   └───────────────┬───────────────────────────┘   └────────┬─────────┘
                   │                                        │
                   ▼                                        ▼
        ContractAnnouncement,                  whoever builds a proxy:
        whoever looks up or publishes          needs a locator from the
                                               client and the factory
```

| Component | Bundle | Configuration | Waits for |
| --- | --- | --- | --- |
| `RestTransport` | `client.rest` | `…client.rest`, **required** | `TokenSupplier` twice: `broker.tokens.target`, `provider.tokens.target` |
| broker proxies (`BrokerCatalog`, `BrokerImplementations`, `BrokerLookup`, `BrokerSessions`) | `client.rest` | — | `RestTransport` |
| `RestEventSource` / `MqttEventTransport` | `client.rest` / `client.messaging` | `…client.rest.events` / `…client.mqtt` (required) | `RestTransport` / its messaging link |
| `RestServiceInvoker` | `client.rest` | — | `RestTransport` |
| `MessagingServiceInvoker` | `client.messaging` | `…client.messaging.invoker` | — |
| `InvokerRegistry` (`InvokerProvider`) | `client.java` | — | nothing; collects every `ServiceInvoker` |
| `DdsrClient` | `client.java` | `…client`, **required** | the four broker proxies, an `EventSource` (`eventSource.target`), the `InvokerProvider` (`invokers.target`) |
| `ServiceProxyFactory` | `client.java` | — | the `InvokerProvider` |
| `ContractAnnouncement` | `client.java` | `…client.announcement`, **required**, derived | a `ServedContract` (`served.target`), `DdsrClient`, `BrokerCatalog` |

## Why the client waits for invokers

A client tells the broker which flavors it speaks
(`supported.flavors`), and a lookup is filtered by that. If it says
`MQTT` and has no messaging invoker, it is handed services it cannot
call — and nothing reports that, because from the broker's side
nothing went wrong.

So the statement is made twice in the client's configuration, once for
the broker and once as a condition:

```json
"org.eclipse.fennec.services.client": {
    "supported.flavors": "REST,MQTT",
    "invokers.target": "(&(fennec.invokers=REST)(fennec.invokers=MQTT))"
}
```

- `InvokerRegistry` is bound to every `ServiceInvoker` as it comes and
  goes. It registers itself as `InvokerProvider` and keeps one service
  property up to date: `fennec.invokers`, the flavors it has an
  invoker for.
- The order is the point. An invoker is **put in first and published
  second**; on the way out its flavor is **withdrawn first and the
  invoker removed second**. Whoever is let in by the property finds
  the invoker, and whoever is being turned away still has it while DS
  turns them away.
- `DdsrClient` declares a reference to the `InvokerProvider` on the
  component itself. Nothing is injected; the reference exists to be
  satisfied. Its target is `invokers.target`.
- While an invoker is missing, the client is not activated and nothing
  has been said to the broker. When one goes away later, the target
  stops matching, DS deactivates the client, and the client releases
  its session.
- `ServiceProxyFactory` binds the same `InvokerProvider` without a
  target and asks it, per service, for the invoker of the first flavor
  the provider announces. It does not need the condition itself: a
  proxy is built over a locator, and a locator comes from a client.

The messaging invoker is one service that speaks `MQTT` and `AMQP`;
it is listed under both.

The RSA role (`rsa.config`) writes both properties from the flavors of
the node. A hand-written configuration writes them itself.

## The same idea, three times

| What is configured | Service property | Target in the configuration |
| --- | --- | --- |
| which token supplier the transport shows | `fennec.tokens` (`none`, `oauth`) | `broker.tokens.target`, `provider.tokens.target` on `…client.rest` |
| which flavors the client speaks | `fennec.invokers` (flavor kinds) | `invokers.target` on `…client` |
| which served contract an announcement is for, and whether it is reachable yet | `fennec.distribution`, `publish` | `served.target` on `…client.announcement` |

The third is the provider side: a generic distribution registers a
`ServedContract` — an OSGi `Condition` — with `publish=false`, raises
it to `true` once the contract is reachable, and the announcement is
activated by that. See [Transports](TRANSPORTS.md).

## A node that does not come up

Everything above shows as an unsatisfied reference, which is the
intended way for a mismatch to look. In the Gogo shell:

```
scr:list
scr:info org.eclipse.fennec.services.client.internal.DdsrClientComponent
```

| What `scr:info` shows | What it means |
| --- | --- |
| client: no configuration | there is no `org.eclipse.fennec.services.client` configuration; the client requires one |
| client: `invokers` unsatisfied | an invoker for one of the flavors in `invokers.target` is missing — check `fennec.invokers` on the `InvokerProvider` service (`inspect cap service <bundle>`), then why that invoker is not there |
| client: `lookup` / `sessions` / `implementations` unsatisfied | the broker proxies are missing, which means `RestTransport` is |
| transport: no configuration | there is no `org.eclipse.fennec.services.client.rest` configuration |
| transport: `broker.tokens` or `provider.tokens` unsatisfied | the target names a token supplier that is not registered — `(fennec.tokens=oauth)` without a `token.uri` for the OAuth supplier is the usual one |
| announcement: `served` unsatisfied | the contract is not reachable yet: no whiteboard lists the application, or the messaging broker has not confirmed the subscriptions |
