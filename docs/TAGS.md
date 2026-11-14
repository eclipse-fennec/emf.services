# Tags

Tags put what the registry offers into categories: contracts and
implementations go under `payment/method/card` or `iot/sensors`, and a
consumer asks for everything under a category instead of naming
services one by one. The tags are an **overlay**: they sit beside the
broker API, keep their own state, and change nothing about what a
contract or an implementation is — no fingerprint moves when something
is tagged ([Fingerprints](FINGERPRINTS.md)).

The design is collected in issue #272; this page grows with its
sub-issues. What exists today is the model (#273) and the tag service
as a library (#274). Serving it as a contract on the broker, provider
tags at registration, rules, the change broadcast, the consumer facade
and the TypeScript side follow.

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
| `RULE` | a rule over the target's properties matched (#277) |

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

`find` answers keys only. What a contract or an implementation looks
like still comes from the lookup, so there is one source for it — and a
later access check sits in one place.

Refusals are exceptions: `UnknownTagException` (no tag has the path,
neither now nor before a rename), `InvalidTagException` (an empty id, a
`/` in it, an id taken among the siblings, a tag under itself),
`TagStoreException` (the change could not be kept and was rolled back).

## Where the tags are kept

In one file, `broker-tags.xmi`, beside the broker's `broker-state.xmi`
— in the image that will be the data volume, once the broker runs the
service (#275, [Deployment](DEPLOYMENT.md)).
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
  properties (for rules) and the tags it asks for. The services
  registry offers contracts and implementations through it (#275).
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
from an annotation on its EPackage. Tree, assignments, queries and the
store would stay as they are. The model (`org.eclipse.fennec.tags.model`)
and the service (`org.eclipse.fennec.tags.service`) carry no services
type for that reason — the build enforces it — and move to a repository
of their own once the atlas uses them.

## Read next

- [Overview](OVERVIEW.md) — the three parties
- [Code generation](CODE_GENERATION.md) — the tag model beside the services model
- [Fingerprints](FINGERPRINTS.md) — why tags never move a hash
