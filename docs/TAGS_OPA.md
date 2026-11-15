# Who sees which tag: OPA and a gateway

The tag facade ([Tags](TAGS.md)) narrows the registry to a tag:
`<broker>/scoped/<tag>/…`. Which caller may use which tag is not the
facade's decision. It is made in front of it, by a policy, from two
facts every request carries: who calls (the token) and which tag (the
path). This page is the recipe — what to set up, in which order — shown
on the Payment example ([the example](EXAMPLE_PAYMENT.md#tagged-methods-and-terms))
with Keycloak, OPA and APISIX, the setup harness scenario Y runs (#282).
Any identity provider, policy engine and gateway that can play the same
roles will do.

```
 caller ──token──▶ APISIX (enforces) ──▶ tag facade /scoped/<tag>/… ──▶ broker
                      │  ▲
            request ──┘  └── allow / deny
                      ▼
                    OPA (decides: groups in the token × tag in the path)
```

## The layers

No layer trusts the one in front of it blindly, and no policy is written
twice:

1. **Authorization at the gateway** — OPA is the policy decision point
   (PDP), the gateway the policy enforcement point (PEP). Caller and path
   are all it needs.
2. **Authentication in the facade** — the broker runs in mode `bearer`
   and verifies every token itself (signature, issuer, audience). A call
   around the gateway without a valid token fails there.
3. **A role for the facade** — `tags-consumer`. Without it a caller
   cannot use the facade at all, whatever the policy says.
4. **A role for the raw API** — `service-consumer`. A caller holding only
   `tags-consumer` cannot read the registry around the facade
   ([Security](SECURITY.md)).
5. **The network** — broker and facade are reachable only through the
   gateway. That is the deployment's statement, not code.

## 1. Mirror the tag tree as groups

A group `/tags/<path>` grants the tag `<path>` and everything under it.
In Keycloak, nested groups give exactly that path:

```json
"groups": [{ "name": "tags", "subGroups": [{ "name": "payment", "subGroups": [
  { "name": "method", "subGroups": [{ "name": "paypal" }, { "name": "card" }] },
  { "name": "terms",  "subGroups": [{ "name": "instant" }, { "name": "installment" }] }
]}]}]
```

The token has to carry the groups with their full path — a protocol mapper
`oidc-group-membership-mapper` with `full.path=true` and claim name
`groups` on the client (or a client scope). A caller is a member of the
groups it may see and holds the role `tags-consumer`:

```json
{ "username": "service-account-shop-instant", "serviceAccountClientId": "shop-instant",
  "realmRoles": ["tags-consumer"], "groups": ["/tags/payment/terms/instant"] }
```

**When an application is installed** it seeds both at once: its tags at
the tag service (`PUT /tags/tag…`, [the example's seeding](EXAMPLE_PAYMENT.md#tagged-methods-and-terms))
and the matching groups at the identity provider. Renaming a tag
(`moveTag`) means renaming its group too; the policy sees current paths
only.

## 2. The policy

`itest/opa/tags.rego`, package `fennec.tags`:

- **default deny** — no token, no tag, no group: no;
- the tag is read from the path, `/…/scoped/<tag>/<references|events|catalog|consumers>…`;
- a group `/tags/<granted>` allows `<granted>` and every tag under it —
  `payment/terms` covers `payment/terms/instant`, not `payment/termsX`,
  and a child group never covers its parent;
- `service-admin` (from `realm_access.roles`) allows every tag;
- only facade paths are decided on; anything else is not this policy's.

The token is decoded, not verified, by the policy — the facade verifies
it (layer 2), so a forged token gets past OPA only to be refused there. A
deployment that wants OPA to verify too uses `io.jwt.decode_verify` with
the issuer's JWKS.

`itest/opa/tags_test.rego` holds these
decisions as tests:

```bash
podman run --rm -v $PWD/itest/opa:/policy:ro,Z docker.io/openpolicyagent/opa:1.4.2 test /policy -v
```

Run OPA as a server with the policy:

```bash
podman run -d --name opa --network host -v $PWD/itest/opa:/policy:ro,Z \
  docker.io/openpolicyagent/opa:1.4.2 run --server --addr 127.0.0.1:18181 /policy/tags.rego
```

## 3. The gateway

APISIX standalone — no etcd, the route from a file
(`itest/opa/apisix-config.yaml`,
`itest/opa/apisix.yaml`): one route, the facade, behind
the `opa` plugin with the policy `fennec/tags`:

```yaml
routes:
  - uri: /ddsr/rest/scoped/*
    upstream: { type: roundrobin, nodes: { "127.0.0.1:8887": 1 } }
    plugins:
      opa: { host: "http://127.0.0.1:18181", policy: "fennec/tags" }
#END
```

```bash
podman run -d --name apisix --network host \
  -v $PWD/itest/opa/apisix-config.yaml:/usr/local/apisix/conf/config.yaml:ro,Z \
  -v $PWD/itest/opa/apisix.yaml:/usr/local/apisix/conf/apisix.yaml:ro,Z \
  docker.io/apache/apisix:3.12.0-debian
```

The plugin hands OPA the request — method, path, headers — and lets it
through only on `allow`; a denial is `403`. Only the facade is routed:
the raw API is not reachable through the gateway at all (`404`). In
production, OPA is reached over TLS.

## 4. Check it — the whole checklist

`itest/opa/access-matrix.tsv` is the checklist, one row per case: who
calls (no token, a forged or broken one, a client), what (method and
path, through the gateway or straight at the broker), what must come
back (a status, a class such as `4xx`, or "not 200"), and optionally
what the answer must or must not contain. `itest/opa/check-access.sh`
runs it and writes **JUnit XML**, one testcase per row, which CI systems
and test dashboards read as they read unit tests:

```bash
itest/opa/check-access.sh itest/opa/access-matrix.tsv \
  http://gateway:18092/ddsr/rest http://broker:8887/ddsr/rest tag-access.junit.xml
```

Tokens come from `token_for <client>` (the harness's Keycloak helper), or
from `TOKEN_<CLIENT>` variables for a deployment of your own — so the same
checklist can watch a real setup, not only the harness. The harness runs
it in scenario Y; CI keeps the report as the artifact `harness-reports`.

What the checklist covers:

| Group | Cases |
| --- | --- |
| `unauthenticated` | no token, an empty bearer, garbage, basic credentials → 403 at the gateway; a forged token with the right group passes the policy (it only decodes) and is refused by the facade (401) |
| `role` | `service-consumer` without a group → 403; the group without `tags-consumer` → the gateway lets it through, the facade refuses (403); an administrator likewise |
| `tag` | the own tag (both Payment providers, no instalment plan, and card's answer names only Payment although it serves both); sibling, parent, root, another branch, a foreign tree, a prefix (`instantX`), upper case → 403; `..`, an encoded `/`, an empty segment, no tag at all → never 200 |
| `api` | events, catalog list (without the instalment contract) and entry (a hidden contract is 404), session put/get/delete under the own tag; the same under a sibling → 403 |
| `write` | adding or removing a contract or a heartbeat through the facade → 4xx |
| `general` | the raw lookup, catalog, publish, events, sessions, registry, the tag service, the root — not routed through the gateway (404), not even for an administrator |
| `around` | straight at the broker: `tags-consumer` reads no raw lookup, adds no contract, changes no tag (403); the facade still wants a valid token (401) |
| `credit` | the credit application sees the instalment plan and not what is paid at once |

One row is a **known limit**, kept visible on purpose: straight at the
broker, the facade serves any tag to a holder of `tags-consumer` — the
policy lives in the gateway, so only the network (layer 5) keeps callers
there. A deployment that cannot guarantee that puts the check into the
facade as well; nothing in this setup does.

A consumer needs nothing of its own for any of this: its broker URL is the
gateway's facade address, e.g.
`http://gateway:18092/ddsr/rest/scoped/payment/terms/instant`, and it
shows its token as it would to the broker.

## Other setups

- **Another gateway**: whatever can ask OPA (or another PDP) with the
  path and the `Authorization` header — Envoy `ext_authz`, Kong, nginx
  `auth_request`, Traefik forward auth. The policy stays the same; only
  the shape of `input` may differ (APISIX puts the request under
  `input.request`).
- **Another identity provider**: anything that puts the caller's grants
  into the token as tag paths. The policy reads `groups` and strips
  `/tags/`; adapt those two lines to the claim your provider writes.
- **Without OPA**: the facade still narrows to the tag, verifies tokens
  and asks for `tags-consumer`; every holder of that role may then use
  every tag. Fine for a closed network, not for anything else.
- **What this does not cover**: whether a provider serves a call. Seeing
  a service through a tag is not being allowed to call it — that stays
  with the provider's own security (bearer or mTLS, per operation).

## Model Atlas

The same recipe applies to models: a facade over the atlas's REST API
under `/scoped/<tag>/atlas/rest/…`, the same groups mirroring the same
tree, the same policy with the path pattern of that facade. Nothing in
the policy knows whether a tag covers services or models.

## Read next

- [Tags](TAGS.md) — the tree, the facade, its roles
- [Security](SECURITY.md) — roles, bearer, mTLS
- [The Payment example](EXAMPLE_PAYMENT.md) — the providers and the seeding
