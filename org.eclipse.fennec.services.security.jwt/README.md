# Bearer token verification

A bearer token in, a verified principal out. This bundle is the
verifier half of the design in [docs/SECURITY.md](../docs/SECURITY.md):
it establishes the **who** from a credential, so that the broker and a
serving node can hold the **what** of a contract against it.

## Destined for emf.util

Nothing here is specific to the service registry: a JWT verifier
configured by issuer, audiences and JWKS, and a Jakarta REST whiteboard
extension that puts the principal on the request. It lives in this
repository until the second user exists, then it moves to
[emf.util](https://github.com/eclipse-fennec/emf.util) as
`org.eclipse.fennec.util.security.jwt` (or whatever name that
repository settles on), and this bundle becomes a dependency. Two
things will have to move with it, or be cut at the seam:

- the principal it produces is `VerifiedPrincipal`, a plain value;
  the registry's model `Principal` is made from it by `model()`, which
  is the only place the model is touched;
- `CallPrincipal` (the thread-local twin of `CallOrigin`) stays in the
  registry's api bundle, because it carries the model's `Principal`.
  The filter sets it; emf.util will offer a hook for that instead.

Until then: keep the dependency on `…services.model` to `model()` and
`CallPrincipal`, and nothing else.

## What it does

| Piece | What |
| --- | --- |
| `JwtVerifier` | `verify(compactJwt)` → `VerifiedPrincipal`, or `TokenRejectedException` with the reason. The one seam a different proof (mutual TLS, later) implements too. |
| `NimbusJwtVerifier` | the implementation on Nimbus JOSE+JWT: RS256 pinned (`none` and HMAC fail by construction), JWKS fetched and cached from the configured URI, `iss` exact, `aud` one of the configured, `exp`/`nbf`/`iat` with a clock skew, scopes read from a configurable claim. A DS component with `ConfigurationPolicy.REQUIRE` that registers the `JwtVerifier` service itself, and only when an `issuer` is configured: without a configuration, or with an empty issuer, there is no verifier, and without a verifier the filter below is not registered — fail closed. The empty issuer is what lets a deployment carry the configuration unconditionally, filled from the environment. An issuer without `jwks.uri` or audiences is refused at activation. |
| `BearerAuthFilter` | a `@PreMatching` request filter at `Priorities.AUTHENTICATION` on every application that says `fennec.auth=bearer`: no token or a rejected one is `401` with `WWW-Authenticate: Bearer`, a verified one lands as the request property `fennec.principal`, as the `SecurityContext` (`isUserInRole` answers for scopes) and on the thread in `CallPrincipal`; the response filter clears the thread. |
| `OptionalBearerAuthFilter` | the filter of mode `bearer-optional` (`fennec.auth=bearer-optional`): a token that is there must verify and a bad one is `401`, but a request without one goes on with no principal. For applications in which every route is answered by the generic dispatcher, which holds each operation to its requirement — what lets a `@PermitAll` operation sit next to guarded ones. |
| `NoAuthFilter` | the filter of mode `none` (`fennec.auth=none`): asks for nothing and clears `CallPrincipal` on the way in and out. It exists so that "no authentication" is a stated mode with a filter an application can require, not the absence of a property. |
| `SecurityNames` | the property and filter names. An application states its mode twice — `fennec.auth=<mode>` and `osgi.jakartars.extension.select=(osgi.jakartars.name=fennec-auth-<mode>)` — so that a bearer application on a node without a verifier is not deployed rather than deployed unguarded. |

The configuration of the verifier is the shape of a `BearerTokenScheme`
in the model — issuer, audiences, JWKS URI, scope claim — so that what
a flavor announces and what the node verifies cannot drift apart:

```json
"org.eclipse.fennec.services.security.jwt": {
  "issuer": "http://localhost:9080/auth/realms/dim",
  "jwks.uri": "http://keycloak:8080/auth/realms/dim/protocol/openid-connect/certs",
  "audiences": ["ddsr-broker"],
  "scope.claim": "scope"
}
```

`scope.claim` is a dot path. `scope` is the OAuth default (one string,
space-separated); `realm_access.roles` is where Keycloak puts realm
roles (a list). Both forms are read.

## What it is not

- Not a role filter. Which scopes an operation needs is the contract's
  business (`SecurityRequirement`) and enforced where the contract is
  served; this bundle only says who is calling and what scopes they
  carry.
- Not a token client. Obtaining a token is the calling node's business
  (the SDKs' token supplier, SECURITY.md §4).
- Not a gateway. It verifies on the node, whether or not something in
  front of it already did.
