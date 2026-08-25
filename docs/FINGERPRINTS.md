# Fingerprint Specification — sd1 and im1

Content-based fingerprints make service contracts and implementations
comparable across languages, processes and time with a single string
comparison. Two frozen schemes exist:

| Scheme | Over | Answers | Value |
|---|---|---|---|
| **sd1** | `ServiceInterface` (the contract) | "do we speak the same contract?" | `sd1:<sha256-hex>` |
| **im1** | `ServiceImplementation` | "does the broker still hold my registration, unchanged?" | `im1:<sha256-hex>` |

Both schemes are **frozen with their tag**: any change to the canonical
form is a new tag (`sd2`, `im2`), never a silent edit. The reference
implementations live in
`org.eclipse.fennec.services.xmi.codec` (Java,
`ServiceDescriptionFingerprint` / `ServiceImplementationFingerprint`)
and `ddsr-ts-client/packages/ddsr-client/src/fingerprint/` (TypeScript).
Cross-language golden fixtures under `itest/fixtures/fingerprint/` pin
both implementations to byte-identical canonical text and hashes —
divergence between the languages is a bug in one of them, never a
"dialect".

## Shared canonical rules

- Line-oriented traversal, 2/4-space indentation, lines joined with
  `\n`, **no trailing newline**; hash = SHA-256 over UTF-8.
- Escaping in every text field: `\` `|` LF CR are escaped; list
  elements additionally escape `,`.
- Lists render as `<count>:<e1,e2,…>`.
- `null` and the empty string both render empty.
- Enum and boolean attributes render their **model default explicitly**
  — EMF omits defaults on the wire, the canonical form does not
  (`FlavorKind → REST`, `HttpMethod → GET`, `MqttFlavor.defaultQos →
  AT_LEAST_ONCE`, `MqttOperationFlavor.qos → AT_MOST_ONCE`,
  `correlation → true`, `retained`/`defaultRetained → false`).
- Numeric property values: integers as exact decimal text;
  double/float as IEEE-754 bit patterns (`bits:<hex>`) so `0.1` is the
  same on every platform.
- **Doc text never moves a hash**: `description` fields are excluded
  everywhere; im1 additionally excludes `componentDescription`
  (deployment detail, not endpoint identity).

## sd1 — contract fingerprint

```
I|<name>|version=<version>|status=<literal>
  X|<name>|type=|version=            exceptions, sorted by name
    pr|<tag>|<name>|value=<value>      exception properties, sorted
  O|<name>|returnType=<type>         operations, DECLARED order
    p|<name>|type=|index=|optional=|defaultValue=
    x|<exceptionName>                  op exceptions, sorted by name
```

Property tags: `S` string, `i` int, `l` long, `d` double, `f` float,
`s` short, `b` bool, `SL` string list; an unknown subclass renders `?`
plus its eClass name (conservative: visible, the hash moves).

Operation **order is contract** (it renders in declared order);
exceptions are sorted by name (a set, not a sequence).

## im1 — implementation fingerprint

im1 **composes** over the sd1 values of the referenced interfaces (the
Merkle principle) instead of re-traversing them:

```
IM|<implementationId>|name=<name>|version=<version>
  c|<sd1-value>                        contracts, DECLARED order
  F|<eClass>|<name>|kind=<literal>…    flavors, DECLARED order
    o|<eClass>|<name>|operation=|consumes=<list>|produces=<list>…
  pr|<tag>|<name>|value=<value>        properties, sorted (sd1 rules)
```

Flavor-specific fields: `RestFlavor` adds
`|host=|basePath=|contentTypes=<list>`, `MqttFlavor` adds
`|brokers=<list>|requestTopic=|responseTopic=|defaultQos=|defaultRetained=`;
operation flavors add their REST (`|method=|path=|returnCodes=`) or
MQTT (`|requestTopic=|responseTopic=|qos=|retained=|correlation=|returnPath=`)
fields. Unknown subclasses render only their eClass name.

## Where fingerprints appear

- The broker decorates every `ServiceReference` with the catalog
  contract's sd1 (`ddsr.fingerprint`, or `ddsr.fingerprint.<name>` for
  multi-interface implementations) and the implementation's im1
  (`ddsr.impl.fingerprint`) — computed AFTER the publish path rewired
  the implementation onto the live catalog entries, so both values are
  catalog truth.
- Lookups filter by contract:
  `GET /references?interface=Payment&fingerprint=sd1:…` returns only
  implementations of exactly that contract.
- The catalog key is `(name, sd1)`: same-named contracts with different
  content **coexist** as separate entries. Addressing uses a
  *lifecycle-normalized* sd1 (status, deprecationReason and replacedBy
  neutralized) so deprecating an entry does not move its address.

## The three-valued reconnect check

A provider that restarts asks: *does the broker still hold my
registration, unchanged?* The answer is a fingerprint comparison
against the reference decoration:

| Comparison | Meaning | Action |
|---|---|---|
| im1 equal | everything unchanged | reuse the registration — publish is skipped, consumers see no churn |
| im1 differs, all sd1 equal | endpoint/property drift | re-publish (the broker retires the old entry) |
| an sd1 differs | **contract** drift | re-publish with a warning — a policy question, not just an update |

An exact im1 match under the same provider name IS the provider's own
registration: im1 covers the implementationId, the endpoints and the
contract tokens. Both SDKs implement this check inside `publish()`.

## What fingerprints do NOT provide

Identity, not compatibility: hashes are order-free — a
backwards-compatible addition changes sd1 exactly as much as a breaking
change. "Consumer of 1.4 may bind a 1.5 provider" needs declared
versions/ranges or explicit compatibility assertions; the fingerprint
then *verifies* what the version label *claims*. Conservatively
false-different, never false-equal.
