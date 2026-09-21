# Code generation

Two generators run in this workspace, in opposite directions, and a
third path deliberately generates nothing at all.

- **Ecore to model code** — the `services.ecore` metamodel becomes the
  Java and TypeScript classes everything else is written against.
- **Model to text** — a contract becomes a Java interface, and a REST
  flavor becomes a JAX-RS resource.
- **Java to model** — an existing Java interface becomes a contract, by
  reflection, with no file written.

## Ecore to model code

The metamodel is `org.eclipse.fennec.services.model/model/services.ecore`,
and it is the canonical one. The TypeScript track keeps a copy whose
*content* is synced from it.

Generation is driven by bnd, not by an IDE:

```
-generate:\
	model/services.genmodel;\
		generate=fennecEMF;\
		genmodel=model/services.genmodel;\
		output=src-gen
```

```bash
./gradlew :org.eclipse.fennec.services.model:generate   # just the codegen
./gradlew build                                         # runs it as part of the build
```

On the TypeScript side:

```bash
cd ddsr-ts-client && pnpm --filter @ddsr/model generate
```

Two rules that are easy to get wrong:

- **`src-gen` is generated.** Never edit it. Anything you write there
  disappears on the next build.
- **The genmodel is not reconciled automatically.** Change the
  `.ecore` and the `.genmodel` does *not* follow on its own. It has to
  be maintained by hand in the same change, or generation will quietly
  produce the old shape.

**Who changes the model.** The `.ecore` belongs to the model owner. A
change to it is a decision about the wire, about fingerprints and about
both languages at once, which is why it is not a routine edit.

## Model to text

The templates live in `org.eclipse.fennec.services.m2t/templates/`,
written in Acceleo, and they are the deliverable. The generated
sources checked in beside them exist to prove the templates compile.

| Template | Produces |
| --- | --- |
| `service-interface.mtl` | one Java interface per contract, plus `package-info` |
| `rest-resource.mtl` | one JAX-RS resource class per REST flavor, delegating to that interface |
| `service-publisher.mtl` | one component per implementation that announces it and withdraws it in the FR-P3 order |
| `java-types.mtl` | shared: type mapping, imports, file header |

Generation is again a bnd instruction, with the generator `fennecM2T`.
One caveat worth knowing before you add a second one: the fileset in
front of the attributes is the *key* of the instruction, so two
generations written with identical filesets collide and the later one
silently replaces the earlier. They also need separate `output`
directories, because bnd empties an output before a run.

### What tells the generator about your language

A **`LanguageBinding`** in a model document. It references the
contracts to generate (never contains them), names the target package,
carries the file header line by line, and maps types. `JavaBinding`
adds whether the result is a provider or a consumer API.

A binding is build-time configuration. It never travels on the wire and
it is not part of a fingerprint.

`TypeScriptBinding` and `PythonBinding` exist as discriminators with no
attributes yet — a place to hang language specifics when a generator
for them arrives.

### When you need generated code at all

Less often than it looks, and that is worth knowing before you generate
anything:

| You have | You need |
| --- | --- |
| a contract and an implementation, and no wish to write transport code | a factory configuration of the generic REST or MQTT distribution — it serves *and* announces from the document, with no code at all |
| a hand-written implementation you want served generically | the same configuration, with `service.filter` pointing at your service |
| an endpoint you write yourself | the JAX-RS resource template, and your own implementation behind it |
| a lifecycle of your own — publish once your endpoint serves, a version you supersede, your own configuration | the publisher template |
| to call a contract from Java | the interface template plus `ServiceProxyFactory.newProxy(Contract.class, locator)` — no stub is generated, and none is needed |

### The publisher template

What it writes is the part that is easy to get subtly wrong: the order.
Announce after the endpoint serves, and on the way down withdraw
*before* stopping it, so a consumer is told while the endpoint still
answers (FR-P3).

What it does **not** write is the contract. The component reads the
document that ships with its bundle — the same one a generic
distribution would serve from — so the contract is stated once and has
one fingerprint. Its configuration is deployment only: where this
instance runs, which document to read, and what it supersedes.

### What exists today, and what does not

Not built yet, part of the open issue #25:

- **No TypeScript stub generation** — split out as its own issue,
  because the decision it carries is where the generator runs and in
  what. `TypeScriptBinding` is still an empty discriminator.
- **No MQTT template.** There is no generated MQTT handler, and since
  the generic MQTT distribution arrived there is less reason for one:
  serving a contract over topics is a factory configuration.
- **The template tests assert on content, not on byte-pinned golden
  files** the way the fingerprint fixtures do. Deliberate: a golden file
  for a template churns on every whitespace change, and what protects
  here is that the generated output is checked in and compiles.

### Why the demo provider does not use the generated resource

It did, briefly, and was moved back. Its endpoint is now served by the
generic REST distribution straight from the model document, with no
generated transport code at all.

That is not a retreat from generation, it is the same idea taken
further. If the model fully describes where every value travels — and
since parameter bindings became authoritative, it does — then a
dispatcher can read the model at runtime and needs no generated class.
Generation is for when you want a typed Java interface to implement or
to call. Serving does not require it.

The contract interface is still generated. Only the endpoint is not.

## Java to model

`org.eclipse.fennec.services.derive` goes the other way: hand it a Java
interface and a version, and it builds a `ServiceInterface` contract by
reflection.

```java
ServiceInterface contract = JavaContracts.contractOf(Greeter.class, "1.0.0");
```

This is what lets [Remote Service Admin](RSA.md) export an ordinary
OSGi service with no contract document anywhere.

**Determinism is the whole point.** The derived contract gets
fingerprinted, so the same interface must always produce the same
contract. `Class#getMethods()` has no defined order, so operations are
sorted by name and then by parameter types. Static methods are not
operations; `default` methods are.

One thing to know: parameter names are only in the class file when it
was compiled with `-parameters`. Without it you get `arg0`, `arg1`,
which is stable but not meaningful — and a build that flips that flag
changes the contract, and therefore its fingerprint.

## Read next

- [Fingerprints](FINGERPRINTS.md) — why determinism matters here
- [Transports](TRANSPORTS.md) — the generic distribution that needs no generated code
- [Remote Service Admin](RSA.md) — derivation in use
