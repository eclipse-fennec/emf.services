# Fennec Services — the guide

Everything in this folder is written for someone who wants to *use*
Fennec Services: publish a service, find one, run a broker, export an
ordinary OSGi service to another framework.

The documents one level up (`docs/ARCHITECTURE.md`,
`docs/ACQUISITION.md`, `docs/WIRE_FORMAT.md` and the rest) are written
for the people building it. They stay where they are. These pages link
into them instead of repeating them, and where a page and one of those
disagree, the older document is the one to trust about the wire.

Every page says what is true **today**. Where something is planned but
not built, the page says so and names the issue, rather than describing
it as if it existed.

## Which page

| You want to | Read |
| --- | --- |
| understand what this is and who talks to whom | [1 · Architecture](01-architecture.md) |
| get a broker and a provider running | [docs/GETTING_STARTED.md](../GETTING_STARTED.md) |
| know what a consumer is told, and when | [2 · Eventing](02-eventing.md) |
| know why a provider was refused, or ignored | [3 · Fingerprints](03-fingerprints.md) |
| change the model, or generate from it | [4 · Code generation](04-code-generation.md) |
| configure REST or MQTT for your deployment | [5 · Transports](05-transports.md) |
| export an OSGi service to another framework | [6 · Remote Service Admin](06-rsa.md) |

## The shortest possible summary

Three parties. A **provider** publishes what it offers. A **broker**
holds the catalogue of contracts and the list of who currently serves
them. A **consumer** asks the broker who serves a contract, and then
talks to that provider directly. The broker is never in the call path.

What travels between them is a model, not a document format that
happens to describe one. The same `services.ecore` is generated into
Java and TypeScript, so both sides mean the same thing by a contract,
and both can compute the same fingerprint for it.
