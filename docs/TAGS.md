# Tags

Tags put what the registry offers into categories: contracts and
implementations go under `payment/method/card` or `iot/sensors`, and a
consumer asks for everything under a category instead of naming
services one by one. The tags are an **overlay**: they sit beside the
broker API, keep their own state, and change nothing about what a
contract or an implementation is — no fingerprint moves when something
is tagged ([Fingerprints](FINGERPRINTS.md)).

The design is collected in issue #272; this page grows with its
sub-issues. What exists today: the model (#273), the tag service (#274),
the `Tags` contract served beside the broker (#275) and the tags a
provider brings, with requests for the ones it may not give itself
(#276), rules over properties (#277) and the change broadcast (#278).
The consumer facade and the TypeScript side follow.

## The tree

A tag has

| | |
| --- | --- |
| `id` | short and technical (`card`), no `/`, unique among its siblings — not globally: `iot/weather` and `marketing/weather` may both exist |
| `name` | human-readable, change it any time |
| `description` | free text |
| `path` | the ids from the root down, `payment/method/card` — **derived**, never stored |
| `selfAssignable` | whether a target may give itself this tag |
| `formerPaths` | the paths it had before a rename or move |

The path is how a tag is named everywhere: in a query, in what a
provider asks for, and later in the URI a policy decides on. Because it
is derived from the ids, renaming a tag moves its whole subtree at
once. A rename also remembers the old path on the tag and on every
descendant, and wherever a path is taken an old one is taken too — so
whoever named a tag by its old path keeps it. What is answered is
always the current path.

A tag counts for its ancestors: something tagged `payment/method/card`
is found when asking for `payment/method` or `payment`.

## Targets, and what they carry

A target is a **kind** and a **key**. For the services registry the
kinds are `contract` (key: the interface name — not the fingerprint, so
a tag survives a change of the contract) and `implementation` (key:
provider name and contract name). The tag model knows no services type;
the kinds are strings.

What a target carries is worked out when a query asks, and every tag
comes with its origin:

| Origin | |
| --- | --- |
| `ADMIN` | an administrator assigned it |
| `SOURCE` | the target asked for it itself — a provider at registration — and the tag exists and is self-assignable |
| `RULE` | a rule over the target's properties matched |

A tag carried for two reasons is reported twice. Nothing is stored per
target, so a tag created after a target arrived applies to it at once,
and an assignment to a target that does not exist yet waits for it.

## The operations

| Operation | |
| --- | --- |
| `tags()` | the whole tree |
| `putTag(parentPath, id, name, description, selfAssignable)` | create a tag, or change one that is there; answers its path |
| `moveTag(path, newParentPath, newId)` | rename, move, or both; answers the new path. A tag cannot move under itself or onto an id its new siblings have |
| `removeTag(path)` | the tag, its subtree and every assignment to any of them |
| `assign(path, kind, key)` / `unassign(path, kind, key)` | an administrator's assignment; doing it twice changes nothing, unassigning leaves tags carried for another reason |
| `find(paths, mode, kind)` | the keys of the targets of a kind that carry the tags, sorted. `ANY`: at least one path, `ALL`: every one. Only targets their source knows right now; a path no tag has matches nothing |
| `tagsOf(kind, key)` | the tags a target carries, with their origin |
| `putRule(path, kind, filter)` / `removeRule(path, kind, filter)` | every target of the kind whose properties match the LDAP filter carries the tag |
| `rules()` | the rules, together with the tree they reference |
| `changes()` | every change, after it was kept and in the order it was made — a stream every receiver shares |
| `requestedTags()` | what targets asked for and may not give themselves, oldest first |
| `acceptRequestedTag(path, kind, key)` | grant a request: the tag is created where missing (with its parents, none self-assignable) and assigned |
| `discardRequestedTag(path, kind, key)` | turn a request down; it comes back with the next registration that asks |

`find` answers keys only. What a contract or an implementation looks
like still comes from the lookup, so there is one source for it — and a
later access check sits in one place.

## Tags a provider brings

A provider asks for tags when it registers: the paths in
`ServiceImplementation.tags` for the implementation, and in
`ServiceInterface.tags` for the contracts it serves.

```java
implementation.getTags().add("payment/method/card");   // Java SDK: the model as it is published
```

```ts
implementation.tags.push('payment/method/card');       // TypeScript, the same field
```

What happens to each path:

| The tag | |
| --- | --- |
| exists and is `selfAssignable` | carried at once, origin `SOURCE` — nobody has to do anything |
| exists, not self-assignable | filed as a request |
| does not exist | filed as a request |

An administrator lists the requests (`requestedTags`) and accepts one —
the tag is created where it is missing and assigned to the target — or
discards it. Nothing is blocked: a discarded request comes back with the
next registration that asks for it. A request is not filed for a tag the
target already carries, and one whose tag has become self-assignable
since is no longer open.

When a target asks: on the services registry, whenever a provider
registers an implementation (the broker's `REGISTERED` event); that
registration asks for the implementation's tags and for those of the
contracts it serves. Targets that are there when the tag service starts
ask once at that moment, so a request discarded before a restart of the
broker is filed again.

## Rules

A rule puts a tag on every target of a kind whose properties match an
LDAP filter — the filter syntax of OSGi (RFC 4515), read with the OSGi
framework API:

```
payment/method/card  ⇐  (payment.scheme=card)
payment/sandbox      ⇐  (payments.sandbox=true)
payment/method       ⇐  (&(payment.scheme=*)(payment.sandbox=false))
```

- **When**: a rule is applied when a query asks, not when a target
  arrives, so a new rule covers what is registered already and a
  removed one stops at once.
- **On what**: the properties the target's source offers. On the
  services registry those are an implementation's properties, plus
  `ddsr.provider.name` and `ddsr.implementation.name`; a contract has
  none, so rules are for `implementation` (the default kind).
- **Typed values** compare as OSGi filters do: `(payments.sandbox=true)`
  matches the boolean `true`, `(service.ranking>=10)` an integer, and a
  list property matches when one of its values does.
- **The tag object**, not its path, is what a rule holds: a rename moves
  the rule with it, removing the tag removes its rules.
- A target a rule tags does not have to ask for that tag — no request is
  filed for it.
- A filter that cannot be read is refused (`InvalidTagException`); the
  same rule twice is kept once.

## Changes

`changes()` is a broadcast (`delivery=BROADCAST`): every receiver hears
every change from the moment it listens, after the change was kept and
in the order it was made. Each value is a `TagChange`:

| `kind` | carries |
| --- | --- |
| `TAG_PUT`, `TAG_REMOVED` | `path` |
| `TAG_MOVED` | `path` (new), `formerPath` |
| `ASSIGNED`, `UNASSIGNED` | `path`, `target` |
| `RULE_PUT`, `RULE_REMOVED` | `path`, `ruleKind`, `filter` |
| `REQUESTED`, `REQUEST_ACCEPTED`, `REQUEST_DISCARDED` | `path`, `target` |

and `at`, when it happened. A change that was refused, rolled back or
changed nothing (assigning twice) is not reported. In XMI a change
without a `kind` attribute is `TAG_PUT`, the default literal.

There is no replay: a receiver that falls behind is ended (overflow
`FAIL`), and one that connects reads the tree, the rules and the
requests first, then listens. It is meant for an administrator's UI —
only `service-admin` may listen; what a consumer may see is the
facade's business (#279). In Java, `TagService.onChange(listener)` is
the same stream without a transport.

## On the broker: the `Tags` contract

A broker whose launch carries `org.eclipse.fennec.tags.model`,
`org.eclipse.fennec.tags.service` and `org.eclipse.fennec.tags.ddsr` (the
development launch and the image do) serves the contract `Tags` beside
its own API, from the document `tags-api.xmi`, through the same generic
REST distribution that serves the broker's contracts. It is mounted at
`/tags` under the broker's REST root, in the broker's authentication
mode.

| Operation | REST | Role | Answer |
| --- | --- | --- | --- |
| `tags()` | `GET /tags/tree` | `service-consumer` | a `TagRegistry` holding only the tree |
| `putTag` | `PUT /tags/tag?parent=&id=&name=&description=&selfAssignable=` | `service-admin` | the path, as text |
| `moveTag` | `POST /tags/tag/move?path=&parent=&id=` | `service-admin` | the new path, as text |
| `removeTag` | `DELETE /tags/tag?path=` | `service-admin` | 204 |
| `assign` / `unassign` | `PUT` / `DELETE /tags/assignment?path=&kind=&key=` | `service-admin` | 204 |
| `find` | `GET /tags/find?path=…&path=…&mode=ANY\|ALL&kind=` | `service-consumer` | `FoundTargets` (kind, keys) |
| `tagsOf` | `GET /tags/of?kind=&key=` | `service-consumer` | `TaggedTarget` (target, tags with origin) |
| `putRule` / `removeRule` | `PUT` / `DELETE /tags/rule?path=&kind=&filter=` | `service-admin` | 204 |
| `rules` | `GET /tags/rules` | `service-admin` | a `TagRegistry` holding the tree and the rules |
| `changes` | `GET /tags/changes` — Server-Sent Events, one `value` frame per change | `service-admin` | `TagChange`, as a broadcast |
| `requestedTags` | `GET /tags/requests` | `service-admin` | a `TagRegistry` holding only the open requests |
| `acceptRequestedTag` / `discardRequestedTag` | `PUT` / `DELETE /tags/request?path=&kind=&key=` | `service-admin` | 204 |

In XMI an `EffectiveTag` without an `origin` attribute carries `SOURCE`:
it is the first literal and so the default, which XMI leaves out.

The refusals are declared errors of the contract: `UnknownTag` (code
300, HTTP 404 — also for a request nobody filed or a rule that is not
there), `InvalidTag` (301, 409 — also for a filter that cannot be read), `TagNotKept` (302, 503).

```bash
B=http://localhost:8887/ddsr/rest
curl -X PUT "$B/tags/tag?id=payment"
curl -X PUT "$B/tags/tag?parent=payment&id=terms"
curl -X PUT "$B/tags/tag?parent=payment/terms&id=instant&selfAssignable=true"
curl -X PUT "$B/tags/assignment?path=payment/terms/instant&kind=contract&key=Payment"
curl "$B/tags/find?path=payment&kind=contract"
```

The targets are the registry's own, read through the catalog contract
(`listCatalog`) by `DdsrTagSubjects`:

| Kind | Key | Asks for | Properties |
| --- | --- | --- | --- |
| `contract` | the interface name, `Payment` | `ServiceInterface.tags` | — |
| `implementation` | provider and contract, `payments-ts/Payment` — once per contract it serves | `ServiceImplementation.tags` | its properties, plus `ddsr.provider.name` and `ddsr.implementation.name` |

An implementation parked in the cold cache is not in the registry and is
not found until a lookup brings it back.

Not yet: the contract is served, not announced in the catalog, so it is
reached at its address under the broker and not found by a lookup.

Refusals are exceptions: `UnknownTagException` (no tag has the path,
neither now nor before a rename), `InvalidTagException` (an empty id, a
`/` in it, an id taken among the siblings, a tag under itself),
`TagStoreException` (the change could not be kept and was rolled back).

## Where the tags are kept

In one file, `broker-tags.xmi`, beside the broker's `broker-state.xmi`
in its working directory — in the image that is the data volume
([Deployment](DEPLOYMENT.md)). `DDSR_TAGS_FILE` names another file
(configuration `org.eclipse.fennec.tags.service`, `store.file`).
Every change is written before it is acknowledged; a change whose write
fails is rolled back. A write goes to a file beside the target and
replaces it in one move, so a crash leaves the previous version.

The file is kept apart from the broker state on purpose. Tags are an
administrator's hand work; a change to the registry model that starts
the broker empty (the providers simply publish again) must not take
them along. The file depends on the tag model only.

## The two seams

The service stands on two interfaces, and both are where other
implementations plug in:

- **`TagSubjects`** — where the targets come from: the kinds a source
  knows, the targets of a kind that exist right now, each with its
  properties (for rules) and the tags it asks for, and — optionally —
  when targets register, which is when they ask. The services registry
  offers contracts and implementations through it (#275) and says when
  they register by listening to the broker's events (#276).
- **`TagStore`** — where the registry is kept. `XmiTagStore` is the
  first; a database or a search index can take over later.

The service needs the Fennec OCL engine, because a tag's path is
derived in OCL; the broker launch has it. EMF binds the derivation when
a tag object is created, so the engine has to be registered before the
first registry is loaded.

## Model Atlas

The Fennec Model Atlas does for models much of what this registry does
for services, and the tags are built to serve it as well. Its scopes
say who owns a model; a tag would classify models across scopes. What
it would take is a `TagSubjects` of kind `model` (key: nsURI and scope)
reading the atlas's scope service, with the tags a model asks for read
from an annotation on its EPackage and a model reaching a stage as the
moment it asks (where requests are filed). Tree, assignments, queries and the
store would stay as they are. The model (`org.eclipse.fennec.tags.model`)
and the service (`org.eclipse.fennec.tags.service`) carry no services
type for that reason — the build enforces it — and move to a repository
of their own once the atlas uses them.

## Read next

- [Overview](OVERVIEW.md) — the three parties
- [Code generation](CODE_GENERATION.md) — the tag model beside the services model
- [Fingerprints](FINGERPRINTS.md) — why tags never move a hash
