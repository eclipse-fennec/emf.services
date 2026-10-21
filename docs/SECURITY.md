# Security — who may publish, look up, and call

The registry has three parties and the broker is never on the call
path: a consumer asks the broker who serves a contract, then calls the
provider directly. Security therefore splits into two questions with
two different enforcers:

| Question | Who enforces | Where it is written down |
| --- | --- | --- |
| who may publish, look up, hold a session, curate the catalog | the broker | the broker's own contract documents, plus two object-level rules in broker code |
| who may call an operation of a served contract | the serving node (a generic distribution, a hand-written resource, an MQTT broker's ACL) | the contract and its flavor |

What does **not** live here: data-level rules. Which rows a caller may
see is the business of the provider that owns the data, the way
row-level security stays with a database. The registry carries
documents; it does not interpret payloads (#100), and it does not
decide who sees which record.

This page describes the model (implemented), the validation gate on the
broker (implemented), and the enforcement design (decided, not yet
built). The status table at the end says which is which.

## 1. Identity: a peer is a client, provider and consumer are roles

Calls between nodes are backend-to-backend, so an identity is an OAuth
2.0 client with client credentials — a Keycloak client with a service
account, say — not a user. Every peer is one: a provider runtime, a
consumer process, an RSA node, a TypeScript service. What the token
carries is what the broker needs:

- **`sub` / `azp`**, the authenticated identity;
- **`aud`**, one audience for the broker and one per target the peer
  calls, so a token minted for the broker cannot be replayed against a
  provider, nor one tenant's token against another's node;
- **roles**, coarse, like a realm role per party; anything finer is
  expressed as scopes of a contract and mapped to roles in the identity
  provider, never in a document.

*Provider* and *consumer* are not identities but roles a peer plays.
The same runtime is routinely both — the payment example consumes the
person store and provides payment; an RSA node exports and imports.
Identity answers "who are you", the role answers "what may you do".

Everything the broker is told about identity today is self-declared,
and it stays as a claim, checked against the token rather than
replaced by it:

| Self-declared today | Becomes |
| --- | --- |
| `consumerId` of a session | bound to the principal's subject; a PUT with somebody else's id is refused |
| `ServiceProvider.symbolicName` | must belong to the principal, or the principal must be allowed to publish for it |
| `X-DDSR-Origin` (`ClientOrigin`, #125) | stays an audit label without a security claim; it names a deployment, not a person |
| `X-DDSR-Requestor` | frozen wire name (#4); the fallback when no token is present |

## 2. The model: what, how, who

The security aspect is part of `services.ecore`, not a transport
property, for one reason that outweighs typing: the broker's own three
contracts are served generically from their documents (#88). A
requirement written in the document is enforced by the same generic
dispatcher on the broker and on every peer, in Java and in TypeScript,
from one ecore. OSGi intents remain the RSA projection of it
(section 6).

Three questions, three places:

| | Class | Hangs on | Says |
| --- | --- | --- | --- |
| **what** | `SecurityRequirement` — `authenticated`, `scopes[*]` | `ServiceInterface.security` (default), `ServiceOperation.security` (override) | what a caller must present; transport-neutral, part of the contract |
| **how** | `SecurityScheme` — abstract, `kind`, extensible like `ServiceFlavor` | `ServiceFlavor.securitySchemes[*]`, alternatives (OR) | how a principal is proven on this transport |
| **who** | `Principal` — `subject`, `issuer`, `scopes[*]` | `ServiceRegistration.principal`, `ConsumerSession.principal`, both with a `domain` | the verified identity; written by the broker, never read from a document |

The concrete schemes:

- **`BearerTokenScheme`** — `issuer` (required), `audiences[*]`,
  `jwksUri`, `scopeClaim`. The token travels in the `Authorization`
  header over REST and as a user property of the request message over
  MQTT 5. The serving node verifies signature, issuer, audience and
  lifetime itself on every call, whether or not a gateway in front of it
  already did.
- **`MutualTlsScheme`** — `trustedIssuers[*]`. The principal is the
  client certificate's subject. A certificate carries no scopes; a
  requirement that names scopes is then satisfied from the node's own
  mapping of subjects, not from the wire.
- **`ConnectionScheme`** — no attributes. Identity is established once
  per connection and enforced outside the service: an MQTT broker's
  authentication and ACL on the request and response topic trees (the
  consumer segment of a response topic binds to the connection's
  identity, which is why the trees are separate, #98), or a gateway in
  front of a REST node. The scheme says explicitly that the node checks
  nothing per call, so a document without any scheme is never mistaken
  for this one.

Scopes are the contract's vocabulary, as in OpenAPI: `payment:write`
is a name the credential must carry. Mapping a scope to roles, groups
or clients is the identity provider's business and never touches a
document, so a policy change never moves a contract.

`principal` and `domain` are **transient**, like a session's
`acquisitions`: never on the wire in either direction. A client cannot
assert them in a document it sends, and a document returned to a
client does not carry them. An auditor sees them, like `publishedBy`,
only through a projection the broker chooses.

### The invariants

Three OCL constraints state what a document can state (fennec m2x
delegates, issue #7; see [Acquisition](ACQUISITION.md) §9):

| Constraint | On | Says |
| --- | --- | --- |
| `scopesImplyAuthenticated` | `SecurityRequirement` | scopes presuppose a principal to carry them |
| `authenticatedContractsHaveAScheme` | `ServiceImplementation` | a flavor serving an operation whose effective requirement (operation override, else interface default) demands authentication offers at least one scheme — the static half of *fail closed* |
| `scopesNeedAPerCallScheme` | `ServiceImplementation` | a flavor serving an operation whose effective requirement names scopes has a scheme other than `CONNECTION` |

They hang on the implementation, not on the flavor, because the model
avoids `eContainer()` and the implementation sees both sides,
interfaces and flavors — the same cut as `operationFlavorsCoverInterfaces`.

### What is deliberately not in the model

- the credential itself — a bearer is a header, not a model value;
- roles, groups, users — they belong to the identity provider;
- the object-level rules of section 3 — they compare two requests,
  which no document can;
- whether a contract without a requirement may be called anonymously
  — that is a decision of the serving node's configuration, never an
  implicit "open".

## 3. At the broker: role × role interface, plus ownership, plus domain

The api bundle already has the authorization matrix drawn, without
having meant to: four role interfaces. The token role is the other
axis:

| Token role | May | Object-level rule |
| --- | --- | --- |
| `provider` | everything on `BrokerImplementations`; read the catalog; add a contract | withdraw, modify and heartbeat only on registrations whose `principal` is its own |
| `consumer` | `BrokerLookup`, `BrokerSessions`, the event stream (SSE or MQTT) | only its own session |
| `admin` | curate the catalog (deprecate, remove), snapshot, expire | everything |

The first two columns are expressed as requirements in the broker's own
contract documents and enforced by the generic dispatcher like on any
peer. The third column is the one rule no contract can state; it lives
in `Registrations` and `Sessions`, where `immutableAfterPublish` is
enforced too. It closes the session-takeover hole recorded in
[Acquisition](ACQUISITION.md) §7.

A **domain** — a tenant, a topology — is a deployment fact in the first
version: an issuer-plus-audience pair. A broker is configured with the
domains it serves and refuses tokens of others; "one broker per domain,
enforced by the token". Several domains in one broker — a partitioned
catalog, events filtered per domain, a domain segment in the MQTT
topic — is the second version, and `ServiceRegistration.domain` /
`ConsumerSession.domain` are already where it will be written. A domain
never goes into a contract: contracts are domain-neutral, registrations
and sessions are domain-bound.

## 4. At the peers: the contract describes the wire, the peer enforces

The serving side needs three things from the registry, or every
provider builds its own:

1. **The flavor says what the wire requires** — section 2. Belongs to
   the implementation, not the interface: a provider that starts to
   demand a token is a different deployment of the same contract.
2. **The generic distributions enforce it generically.** The REST
   dispatcher and the MQTT distribution read the requirement and the
   scheme from the document the way they read paths and topics today.
   *Fail closed*: a served contract whose effective requirement demands
   authentication is not registered without a configured verifier,
   rather than served open. The m2t resource template emits
   `@RolesAllowed` from the same requirement for hand-written resources.
3. **Both SDKs get a credential seam.** A token supplier per audience
   in Java, the same through the `fetchFn` seam in TypeScript, with a
   client-credentials cache. This is the request-interceptor hook that
   [Client framework](CLIENT_FRAMEWORK_GUIDE.md) §10 has listed as open,
   with content.

Over MQTT the messaging broker is the enforcement point
(`ConnectionScheme`): identity on the connection, ACL on the topic
trees. A per-message token as a CloudEvents extension is possible but
visible to the messaging broker in clear; it is not the first choice.

A gateway in front of the broker — the APISIX pattern — is a deployment
option, never a requirement. The broker verifies itself, like the
nodes behind such a gateway do.

## 5. Why bearer first and mutual TLS second

Mutual TLS covers REST, MQTT and AMQP alike, and the model has the
scheme for it. It is still the second step:

- the broker's own matrix and scopes per operation need claims, and a
  certificate carries only a subject;
- the verifier, the realm and the client-credentials flow exist already
  (the timetracking prototype); a certificate authority with issuance
  and rotation does not;
- server TLS is needed first anyway — a bearer must never travel in
  clear — and client certificates are then one more switch on the same
  infrastructure;
- the seam is neutral: a verifier yields a `Principal` whether from a
  token or a certificate, so mTLS later is a second verifier, not a
  redesign.

The one deployment where mTLS comes first: few peers, no identity
provider, no fine scopes.

## 6. Remote Service Admin: intents as the projection

An RSA node is a peer like any other; its role configuration (#109) is
the one place that takes its client credentials and derives them into
the client (tokens for broker calls) and the distribution (the verifier
for served endpoints). `registry.name`, `consumer.id` and
`ddsr.provider.name` are thereby bound to the client id, as
`consumerId` is to the principal.

The specification's vocabulary maps onto the model in both directions:

- **export**: `JavaContracts` derives the `SecurityRequirement` from an
  annotation on the Java interface; the scheme comes from the node's
  configuration into the flavor. `service.exported.intents` stays the
  fail-closed switch the admin already honours — a distribution that
  does not promise an intent does not export (`osgi.confidential` is
  promised honestly only with TLS).
- **import**: the endpoint description gets the requirement reflected as
  `service.intents`, so a specification-conformant consumer can filter
  on it; the proxy reads the scheme from the flavor and asks the SDK's
  token supplier.

The one RSA-specific question is transparency: an imported service is a
plain `@Reference`, the caller has no place to hand over a token. First
version: **node identity** — the proxy calls with the node's own client
credentials. A user on whose behalf the node calls travels as a claim
for audit (an "on behalf of" subject as a CloudEvents extension, like
the trace context), not as a credential. Token exchange (RFC 8693)
comes when a target provider has to enforce the user's identity, not
before.

## 7. The validation gate

The broker validates every document it takes in against the model's
armed invariants before touching any state: a contract on
`addCatalogEntry`, an implementation on `publishImplementation` and
`modifyImplementation`. A violation is refused with
`CODE_CATALOG_ENTRY_INVALID` (204) or `CODE_IMPL_INVALID` (216), 422
over REST, the constraint names in the message and in the broker log at
WARNING. That makes the security invariants of section 2 true where the
document arrives — and enforces the catalog's older invariants for the
first time.

Two things the gate knows about the broker it guards:

- **No engine, no check — said once.** Without the fennec OCL engine
  EMF does not skip a delegated constraint; it reports every one as
  violated at ERROR. The gate asks for the delegate first and, when it
  is missing, warns once and lets the document through. The broker
  launches carry the engine as a runrequire; a runtime without it
  checks nothing, honestly, instead of refusing everything.
- **A proxy into a payload model is no defect.** A contract types its
  parameters with `eType` from the payload model, which the broker does
  not have (#100). EMF's own every-proxy-resolves check is therefore
  not counted. A proxy where a reference *into the contract* belongs is
  still caught, because the broker rewires those to its catalog entry
  first and a proxy is never a member of the live contract.

The second point found a gap on the gate's first run against the
harness: a Java publisher keeps its contract at the broker's
`/catalog/{name}` URL, so a binding's parameter arrives as an href the
codec refuses to follow, and only the operation reference was rewired
positionally. `CatalogStore.counterpart` now follows such an href along
its containment path into the live entry — parameters and exceptions
included.

## 7a. Enforcement: the verifier and the broker's mode

`org.eclipse.fennec.services.security.jwt` — see its
[README](../org.eclipse.fennec.services.security.jwt/README.md) — turns
a credential into a principal:

- `JwtVerifier.verify(token)` → `VerifiedPrincipal` (subject, issuer,
  scopes) or `TokenRejectedException`; `NimbusJwtVerifier` implements it,
  configured like a `BearerTokenScheme` (PID
  `org.eclipse.fennec.services.security.jwt`: `issuer`, `jwks.uri`,
  `audiences`, `scope.claim`, `clock.skew.seconds`) and only by
  configuration.
- `CallPrincipal` (api bundle, next to `CallOrigin`) carries the
  verified model `Principal` on the calling thread, for the broker core.

### An application states its mode

A Jakarta REST application that wants authentication says which, with
two properties, and needs both:

| Property | Why |
| --- | --- |
| `fennec.auth=bearer` | what the bearer filter selects, so it runs on the application |
| `osgi.jakartars.extension.select=(osgi.jakartars.name=fennec-auth-bearer)` | keeps the application down until that filter exists — fail closed |

Without the second, an application asking for bearer on a node without
a verifier would be deployed unguarded. With it, the whiteboard does
not deploy it at all.

`none` is a mode like any other: `fennec.auth=none` with a filter of
its own (`NoAuthFilter`, `fennec-auth-none`) that asks for nothing and
clears `CallPrincipal`, so a pooled thread does not lend the previous
caller's identity. Turning authentication off is therefore a stated
choice in the same words that turn it on, and both properties can come
from one placeholder.

### The broker (slice 2)

All four of the broker's REST applications carry the mode, from the
same placeholder in `broker.rest/configs/config.json`:

```
$[env:DDSR_AUTH_MODE;default=$[prop:fennec.auth.mode;default=bearer]]
```

The environment wins, then the launch's framework property, then
**bearer**. `broker.bndrun` sets `fennec.auth.mode=none` — it is the
development broker and the one the harness runs — and so does the
container image, out loud, until it can be given a verifier through the
environment (slice 4).

- The three contracts the generic distribution serves (`/catalog`,
  `/implementations`, `/references`) get the two properties through
  their factory configuration: Declarative Services copies a component's
  configuration onto the application it registers, so no code changed.
- The hand-written resources (`/events`, `/consumers`, `/registry`) live
  in the whiteboard's default application. `BrokerApplication` replaces
  it (`osgi.jakartars.name=.default`, base `/`), with the mode from its
  own configuration; paths and the default-application telemetry filter
  stay where they were. The three resources additionally select
  `(osgi.jakartars.name=fennec-auth-*)`: the whiteboard evaluates that
  per application, and a filter only counts for an application it
  selects — so if the replacement cannot be deployed and the built-in
  default comes back, the resources stay down there too.
- The ownership rule (§3) lives in `Ownership` and is asked by
  `Registrations` (publish over an existing registration, `replaces`,
  modify, withdraw), `Liveness` (heartbeat) and `Sessions` (put,
  delete). A registration records a copy of the caller's principal; a
  session's `consumerId` must be the caller's subject. Refusals are
  `CODE_IMPL_OWNERSHIP_VIOLATION` (211) and
  `CODE_SESSION_OWNERSHIP_VIOLATION` (231), both 403. An identity is
  subject and issuer together.
- A call without a principal (in process, or mode none) and a
  registration without one (published that way, or rebuilt from a
  snapshot — the principal is not persisted) are left alone: nothing
  was verified that a refusal could rest on. After a restart the
  provider's next publish claims its registration again.

Verified against a running broker: without a verifier, mode bearer
serves nothing (404 throughout); with one, no token or a wrong
audience is 401 on all four applications and the SSE stream, a valid
token is 200, and a withdraw, re-publish or session PUT by a second
principal is 403 while the owner's goes through.

Not in slice 2, and deliberately: the domain (`registration.domain`
stays null, the broker's single default domain), the role filter per
operation (slice 3), and the token supplier in the SDKs — an
authenticated broker cannot yet be used by the Java or TypeScript
client, which is why every launch that runs them says none.

## 8. Decisions taken, decisions open

Taken, and written into the model's documentation annotations:

- `consumerId`, the consumer segment of an MQTT response topic and the
  client id are one and the same name;
- a contract without a requirement is not "open"; the serving node's
  configuration decides;
- a `ConnectionScheme` cannot check scopes;
- `principal` and `domain` never travel;
- the requirement is part of sd1 and the schemes are part of im1, by
  the schemes' extension rule (a line only where something is
  declared), so the tags stayed — see [Fingerprints](FINGERPRINTS.md);
- the broker's default mode is `bearer`; `none` is stated explicitly
  where it applies — the development launch, the harness, the container
  image until slice 4 (§7a).

Open, in the order they will come up:

| Question | Leaning |
| --- | --- |
| an SSE stream outlives its token | the stream ends with `exp`; the client reconnects anyway |
| the verifier's home | lifted into `…services.security.jwt` here; moves to emf.util once a second user exists (its README names the two cuts) |
| user identity through an RSA proxy | claim for audit first, token exchange when a target needs it |

## 9. Status

| Piece | State |
| --- | --- |
| model: requirement, scheme, principal, domain, three invariants | implemented |
| validation gate on the broker, codes, HTTP mapping | implemented |
| href rewiring of parameter and exception bindings | implemented |
| requirement in sd1, schemes in im1, cross-language goldens | implemented |
| bearer verifier and filter (`…services.security.jwt`), `CallPrincipal` | implemented |
| the broker's applications state a mode (`DDSR_AUTH_MODE`, default bearer), ownership rule on registration, heartbeat and session | implemented — slice 2, see §7a |
| verifier configuration for the container image, Keycloak in the harness | not yet — slice 4 |
| role filter on the broker's contracts | not yet — slice 3 |
| token supplier in the Java and TypeScript SDKs | not yet |
| domain enforcement from issuer/audience | not yet |
| RSA: annotation-derived requirement, intents reflected on import, node credentials on the proxy | not yet |
| mutual TLS verifier | after bearer |
