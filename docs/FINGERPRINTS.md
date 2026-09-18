# Fingerprint Specification — sd1 and im1

Content-based fingerprints make service contracts and implementations
comparable across languages, processes and time with a single string
comparison. Two frozen schemes exist:

| Scheme | Over | Answers | Value |
|---|---|---|---|
| **sd1** | `ServiceInterface` (the contract) | "do we speak the same contract?" | `sd1:<sha256-hex>` |
| **im1** | `ServiceImplementation` | "does the broker still hold my registration, unchanged?" | `im1:<sha256-hex>` |

Both schemes are **frozen with their tag**: any change to the canonical
form that would move an already computable value is a new tag (`sd2`,
`im2`), never a silent edit. New model state may be added to the grammar
only when every fingerprint that could be computed before stays
bit-identical — see [Typed slots and multiplicity](#typed-slots-and-multiplicity--and-why-the-tag-is-still-sd1). The reference
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
  O|<name>|returnType=<type>[|returnEType=][|returnLower=|returnUpper=][|returnOptional=true]
    p|<name>|type=|index=|optional=|defaultValue=[|eType=][|lower=|upper=]
    x|<exceptionName>                  op exceptions, sorted by name
```

The bracketed fields are the **extension fields** of issue #41; they are
appended in that order and only where they apply — see below.

Property tags: `S` string, `i` int, `l` long, `d` double, `f` float,
`s` short, `b` bool, `SL` string list; an unknown subclass renders `?`
plus its eClass name (conservative: visible, the hash moves).

Operation **order is contract** (it renders in declared order);
exceptions are sorted by name (a set, not a sequence).

### Typed slots and multiplicity — and why the tag is still sd1

Since #41 a `Parameter` can name its metamodel type (`eType`, an EClass
or an Ecore EDataType) and its multiplicity (`lowerBound`/`upperBound`),
and an operation's return is a `Parameter` of its own
(`ServiceOperation.returnValue`) instead of a bare string. The canonical
form renders the new state **only where it says something the old model
could not express**, so no fingerprint that could be computed before
moves — which is the reason the tag did not have to become `sd2`:

- `returnType=` is the return slot's `type`, exactly where the old
  `ServiceOperation.returnType` string stood. No return slot renders the
  same empty field as a void operation did.
- `eType=` / `returnEType=` render `<nsURI>#//<Name>`, read from the
  **proxy URI** where the metamodel is not on the classpath — the normal
  case for a broker holding a foreign provider's contract. Resolving is
  never required, and a resolved classifier renders identically. Absent
  where no `eType` is set.
- `lower=`/`upper=` render **as a pair and only for a multi-valued slot**
  (`upperBound ≠ 1`). For a single value the multiplicity is already
  fully stated by `optional`, and `lowerBound` would be a second, weaker
  copy of it: a factory-built `Parameter` answers the model default `1`,
  one parsed from a document written before the bounds existed answers
  nothing, and an optional parameter written consistently says `0`. All
  three mean the same thing, so none of them may move the hash — in
  either language.
- `returnOptional=true` renders only for a nullable result; the old model
  had no return-side optionality, so `false` stays silent.
- The return slot's **name never renders**. `NamedElement` forces one on
  it (`result` by convention in both SDKs), but unlike a parameter name
  it has no wire role — it is not an argument-map key — and emitting it
  would move the fingerprint of every contract migrated from the old
  string.

This is the one documented way to extend a frozen scheme: the grammar
grew, no computable value changed. An extension that cannot meet that
bar is an `sd2`.

## im1 — implementation fingerprint

im1 **composes** over the sd1 values of the referenced interfaces (the
Merkle principle) instead of re-traversing them:

```
IM|<implementationId>|name=<name>|version=<version>
  c|<sd1-value>                        contracts, DECLARED order
  F|<eClass>|<name>|kind=<literal>…    flavors, DECLARED order
    o|<eClass>|<name>|operation=|consumes=<list>|produces=<list>…
      pb|<parameter>|binding=<literal>|wireName=   parameter bindings, sorted by parameter
      xb|<exception>|status=<int>                  exception bindings, sorted by exception
  pr|<tag>|<name>|value=<value>        properties, sorted (sd1 rules)
```

The two binding lines render **only where the flavor declares any**, so an
implementation that binds nothing hashes exactly as it did before they
existed — the same extension rule as in sd1: the grammar may grow, no
computable value may change.

Flavor-specific fields: `RestFlavor` adds
`|host=|basePath=|contentTypes=<list>`, `MqttFlavor` adds
`|brokers=<list>|requestTopic=|responseTopic=|defaultQos=|defaultRetained=`;
operation flavors add their REST (`|method=|path=|returnCodes=`) or
MQTT (`|requestTopic=|responseTopic=|qos=|retained=|correlation=|returnPath=`)
fields. Unknown subclasses render only their eClass name.

## Outside both schemes — by design

Both canonical forms enumerate their features explicitly, so a model
extension does not move a hash unless the renderer is changed. The
following features (added with the update-policy / capability uptake)
are deliberately **not** rendered and are pinned as such by
`ServiceDescriptionFingerprintTest`, `ServiceImplementationFingerprintTest`
and `ContractAddressingTest`:

| Feature | Why it stays out |
|---|---|
| `ServiceInterface.updatePolicy`, `replacedBy`, `deprecationReason` | lifecycle metadata of a catalog entry, not the contract; a policy change must not move the address of every implementation |
| `ServiceImplementation.updatePolicy`, `replaces`, `cutoverGraceMillis` | input to the broker's update state machine, not "what is registered" |
| `ServiceImplementation.capabilities`, `ServiceFlavor.capabilities` | feed the lookup resolver; the registration itself is unchanged |
| `ServiceEvent.reasonCode` | events are not fingerprinted |

`RestOperationFlavor.parameterBindings` used to stand in this table: a
binding nobody read was decoration, and im1 was frozen with its tag.
Since the SDKs place every argument where the binding says (#74), two
implementations differing only in bindings are **not** the same endpoint
— a consumer holding the older binding calls it wrongly — so the
fingerprint would have been wrong about the one thing it exists to
answer. `exceptionBindings` joins it for the same reason. The tag stayed
`im1` because no value that could be computed before has moved.

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
| im1 differs, all sd1 equal | endpoint/property drift | **modify in place** (`PUT /implementations`, #55) — the broker keeps the reference id and the leases, refreshes the decoration and emits `MODIFIED`; falls back to publish if the broker refuses |
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
