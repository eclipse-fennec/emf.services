# DDSR TypeScript Client — Architektur & Implementierungsplan

**Stand:** 2026-05-28
**Basis:** `docs/REQUIREMENTS.md`, `docs/CLIENT_FRAMEWORK_GUIDE.md`, `ddsr.ecore`

---

## 1. Zielarchitektur

### 1.1 TSM als Fundament

Im Java-Stack ist OSGi DS die Runtime (Module, DI, Lifecycle). Das TS-Äquivalent ist **TSM** (`@eclipse-daanse/tsm`). Jedes DDSR-Paket ist ein TSM-Modul mit eigenem Manifest, Lifecycle-Hooks und Service-Deklarationen. TSM liefert:

- **Modul-Loading** mit Dependency-Auflösung und Versionierung
- **ServiceRegistry** mit DI (`@inject`, `@injectable`, Singleton/Transient)
- **Lifecycle-Hooks** (`activate` / `deactivate`)
- **PluginRegistry** für dynamische Plugin-Discovery
- **Hot-Reload** für Modul-Updates zur Laufzeit

```
┌──────────────────────────────────────────────────────────────┐
│  TSM Runtime                                                  │
│                                                               │
│  ┌──────────────┐   ┌───────────────┐                        │
│  │ ddsr-model    │   │ ddsr-client    │                        │
│  │ provides:     │   │ requires:      │                        │
│  │  ddsr.package │◄──│  ddsr.package  │                        │
│  │  ddsr.factory │   │ provides:      │                        │
│  └──────────────┘   │  ddsr.client   │                        │
│                      │  ddsr.provider │                        │
│                      │  ddsr.consumer │                        │
│                      │ requires:      │                        │
│                      │  ddsr.flavor.* │                        │
│                      └───────▲───────┘                        │
│                              │                                │
│               ┌──────────────┴───────────────┐               │
│               │                              │               │
│  ┌────────────────────┐   ┌──────────────────────┐           │
│  │ ddsr-flavor-rest    │   │ ddsr-flavor-mqtt      │           │
│  │ provides:           │   │ provides:             │           │
│  │  ddsr.flavor.rest   │   │  ddsr.flavor.mqtt     │           │
│  │  (FlavorPlugin)     │   │  (FlavorPlugin)       │           │
│  └────────────────────┘   └──────────────────────┘           │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

### 1.2 Broker = just another Service

Der Broker ist ein self-published REST-Service. Er veröffentlicht sich selbst als drei ServiceInterfaces im eigenen Catalog: `BrokerCatalog`, `BrokerImplementations`, `BrokerLookup`. Der Client kommuniziert mit dem Broker über denselben FlavorPlugin-Mechanismus wie mit jedem anderen Service — **kein separater BrokerHttpClient**.

```
                    Bootstrap (einmalig)
                    ┌─────────────────────────────────────────┐
                    │ GET /references?interface=BrokerCatalog  │
                    │                &interface=BrokerImpl...  │
                    │                &interface=BrokerLookup   │
                    │                &flavors=REST             │
                    └──────────────────┬──────────────────────┘
                                       │
                                       ▼
                    ServiceLocator für BrokerCatalog
                    ServiceLocator für BrokerImplementations
                    ServiceLocator für BrokerLookup
                                       │
                                       ▼
                    Ab hier: normaler invoke() über RestFlavorPlugin

consumer.find("Payment")
  → brokerLookup.invoke("getReferences", {interface: "Payment"})
    → RestFlavorPlugin
      → GET /dssr/rest/references?interface=Payment

provider.publish(self, impl)
  → brokerImpl.invoke("publish", {provider: self, implementation: impl})
    → RestFlavorPlugin
      → POST /dssr/rest/implementations  (XMI body)
```

**Ein Codepfad für alles.** Caching, Retries, Auth-Interceptor wirken automatisch auch auf Broker-Kommunikation. Zum Testen: Mock das FlavorPlugin → Broker-Calls sind gemockt.

**Bootstrap-Sequenz:**
1. Client kennt nur die Broker-URL (aus Config)
2. Einziger Sonderfall: synthetischer `ServiceLocator` aus der bekannten Broker-URL, mit den fixen Broker-API-Operationen
3. Erster Call über `RestFlavorPlugin` mit synthetischem Locator → holt die echten Broker-ServiceLocators
4. Synthetischer Locator wird durch echten ersetzt
5. Ab jetzt alles über `invoke()` → FlavorPlugin

### 1.3 Zwei Modi der Service-Nutzung

**Bekannter Service (Compile-time):** Der Consumer hat ein generiertes npm-Paket mit typisierten Interfaces. Er arbeitet mit dem Service wie mit einer normalen TS-Klasse.

```ts
import { Payment } from '@ddsr/payment-api';

const payment = consumer.getService<Payment>("Payment");
const result = await payment.charge({ amount: 42, currency: "EUR" });
```

Unter der Haube ist `payment` ein ES6 `Proxy`, typisiert durch das generierte Interface.

**Unbekannter Service (Runtime-only):** Kein generiertes Paket vorhanden. Der Consumer inspiziert das ServiceInterface-Modell und ruft per `invoke()` dynamisch auf.

```ts
const locator = await consumer.find("InventoryCheck");
const ops = locator.serviceInterface.operations;
const result = await locator.invoke("checkStock", { sku: "ABC-123" });
```

Beide Wege landen im selben ServiceProxy → selben FlavorPlugin.

### 1.4 Schichten

```
┌──────────────────────────────────────────────────────────────┐
│                      Consumer-Code                            │
│   payment.charge(...)          locator.invoke("op", params)   │
│   [typisiert]                  [dynamisch]                    │
└───────┬──────────────────────────────┬───────────────────────┘
        │                              │
        ▼                              ▼
┌──────────────────────────────────────────────────────────────┐
│                      ServiceProxy                             │
│  - matched Methode / invoke() gegen ServiceInterface          │
│  - validiert Parameter gegen ParameterConstraints             │
│  - dispatcht über FlavorPlugin aus TSM-Registry               │
└───────────────────────────┬──────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│               FlavorPlugin (TSM-Service)                       │
│  REST: fetch() → HTTP Request     (auch Broker-Calls!)        │
│  MQTT: publish/subscribe → Request/Response                   │
└───────────────────────────┬──────────────────────────────────┘
                            │
                            ▼
                   Provider / Broker (remote)
```

### 1.5 Lokale Registry + Broker-Sync

```
┌──────────────────────────────────────────────────────────────┐
│  TS-Prozess (TSM Runtime)                                     │
│                                                               │
│  ┌──────────────────────────────────────┐                    │
│  │  LocalServiceRegistry                 │                    │
│  │  (baut auf TSM ServiceRegistry auf)   │                    │
│  │                                       │       ┌──────────┐│
│  │  - lokale Services (in-process)       │       │          ││
│  │  - gecachte Remote-Services           │◄─SSE──│  Broker  ││
│  │  - ServiceListener / Events           │       │          ││
│  │  - connectionState                    │       └──────────┘│
│  │                                       │   (Broker-Calls   │
│  │  Broker-Calls laufen auch über        │    via invoke()   │
│  │  invoke() → FlavorPlugin              │    → FlavorPlugin)│
│  └──────────────┬───────────────────────┘                    │
│                 │                                             │
│       consumer.find("Payment")                                │
│                 │ transparent: lokal oder remote               │
│                 ▼                                             │
│  ┌──────────────────────────┐                                │
│  │  ServiceLocator           │                                │
│  │  - reference              │                                │
│  │  - implementation         │                                │
│  │  - serviceInterface       │                                │
│  │  - invoke(op, params)     │                                │
│  │  - restFlavor()           │                                │
│  └──────────────────────────┘                                │
└──────────────────────────────────────────────────────────────┘
```

**Sync-Modell (FR-Sync-Hybrid):**
1. Connect: Client holt Snapshot vom Broker (via `invoke()` auf BrokerLookup)
2. SSE-Stream: Broker pusht inkrementelle Events (ServiceEvent, CatalogEvent)
3. Disconnect: Lokale Registry geht in Read-Only (connectionState = OFFLINE)
4. Reconnect: Frischer Snapshot, Diff → synthetische Events an lokale Listener

**ConsumerCapability ergibt sich automatisch** aus den geladenen FlavorPlugin-Modulen:

```ts
// In DdsrClient.activate():
const plugins = context.services.getAll<FlavorPlugin>("ddsr.flavor.*");
const supportedFlavors = plugins.map(p => p.flavorKind); // ["REST"]
```

### 1.6 Zwei Ebenen Codegen

| Ebene | Input | Output | Tool |
|---|---|---|---|
| **1. DDSR-Metamodell** | `ddsr.ecore` | TS-Klassen: ServiceInterface, ServiceOperation, DDSRFactory, etc. | `@emfts/codegen` |
| **2. Service-Stubs** | Catalog-Einträge (ServiceInterface-Instanzen) | Typisierte TS-Interfaces + Proxy-Factories für Business-Services | `ddsr-codegen` (CLI) |

---

## 2. TSM-Module im Detail

### 2.1 `ddsr-model`

Generierte EMF-Klassen aus `ddsr.ecore`. Registriert das EPackage in der EMF-Registry.

```ts
// ModuleManifest
{
  id: "ddsr-model",
  name: "DDSR Model",
  version: "1.0.0",
  entry: "./dist/index.js",
  provides: [
    { id: "ddsr.package", scope: "singleton" },
    { id: "ddsr.factory", scope: "singleton" }
  ],
  sharedDependencies: [
    { id: "@emfts/core", versionRange: "^0.1.0" }
  ]
}
```

**activate():** Registriert `DDSRPackage.eINSTANCE` in der globalen EPackage-Registry.

### 2.2 `ddsr-flavor-rest`

REST-FlavorPlugin. Übersetzt ServiceOperation + RestOperationFlavor → fetch-Call. Wird sowohl für Broker-Kommunikation als auch für Provider-Aufrufe benutzt.

```ts
// ModuleManifest
{
  id: "ddsr-flavor-rest",
  name: "DDSR REST Flavor Plugin",
  version: "1.0.0",
  entry: "./dist/index.js",
  provides: [
    { id: "ddsr.flavor.rest", scope: "singleton" }
  ]
}
```

**FlavorPlugin Interface:**

```ts
export interface FlavorPlugin {
  readonly flavorKind: string;              // "REST" | "MQTT" | ...
  canHandle(flavor: ServiceFlavor): boolean;
  invoke(
    operation: ServiceOperation,
    params: Record<string, unknown>,
    flavor: ServiceFlavor,
    operationFlavor: ServiceOperationFlavor
  ): Promise<unknown>;
}
```

**REST-spezifisch:**
- `RestOperationFlavor.method` → HTTP method (GET/POST/PUT/DELETE)
- `RestOperationFlavor.path` → URL path (appended to `RestFlavor.basePath`)
- Parameter-Serialisierung: GET → Query-Params, POST/PUT → JSON/XMI body
- Response-Deserialisierung: XMI → EObject (via `XMIResource` aus `@emfts/core`)

### 2.3 `ddsr-client`

Das Framework-Kernmodul. Nutzt die Broker-ServiceLocators intern für alle Broker-Kommunikation.

```ts
// ModuleManifest
{
  id: "ddsr-client",
  name: "DDSR Client Framework",
  version: "1.0.0",
  entry: "./dist/index.js",
  dependencies: [
    { id: "ddsr-model", versionRange: "^1.0.0" },
    { id: "ddsr-flavor-rest", versionRange: "^1.0.0" }
  ],
  requiresService: [
    { id: "ddsr.package" },
    { id: "ddsr.flavor.rest" }
  ],
  provides: [
    { id: "ddsr.client",   scope: "singleton" },
    { id: "ddsr.provider", scope: "singleton" },
    { id: "ddsr.consumer", scope: "singleton" }
  ]
}
```

**activate(context):**
1. Liest Broker-URL aus Config (default `http://localhost:8887/dssr/rest`)
2. Sammelt FlavorPlugins → baut ConsumerCapability
3. Bootstrap: synthetischer Locator → holt echte Broker-ServiceLocators
4. Erstellt DdsrProvider (nutzt `brokerImplementations`-Locator intern)
5. Erstellt DdsrConsumer (nutzt `brokerLookup`-Locator intern)
6. Registriert DdsrClient, DdsrProvider, DdsrConsumer in `context.services`

**deactivate(context):**
1. Schließt SSE-Connection (wenn Inkrement 3)
2. Gibt Ressourcen frei

### 2.4 `ddsr-flavor-mqtt` (Inkrement 4)

Analog zu REST, mit MQTT-Client als Shared Dependency.

### 2.5 `ddsr-codegen` (kein TSM-Modul)

CLI-Tool für Ebene-2-Codegen. Läuft zur Build-Zeit.

```
npx ddsr-codegen generate \
  --catalog http://localhost:8887/dssr/rest/catalog \
  --interface Payment \
  --output ./src/generated/
```

---

## 3. Repo-Struktur

```
ddsr-ts-client/
├── package.json                         # pnpm workspace root
├── tsconfig.base.json
├── vitest.workspace.ts
│
├── packages/
│   ├── ddsr-model/                      # TSM-Modul: Ebene-1-Codegen
│   │   ├── package.json                 # deps: @emfts/core
│   │   ├── model/
│   │   │   ├── ddsr.ecore               # Kopie aus Java-Projekt
│   │   │   └── ddsr.genconfig.xmi
│   │   └── src/
│   │       ├── ddsr/                    # generiert: DDSRPackage, DDSRFactory, alle Typen
│   │       └── module.ts               # TSM ModuleLifecycle + Manifest
│   │
│   ├── ddsr-client/                     # TSM-Modul: Framework-Kern
│   │   ├── package.json                 # deps: @emfts/core, ddsr-model, @eclipse-daanse/tsm
│   │   └── src/
│   │       ├── api/                     # Öffentliche Interfaces
│   │       │   ├── ddsr-client.ts       # DdsrClient
│   │       │   ├── ddsr-provider.ts     # DdsrProvider — publish/withdraw
│   │       │   ├── ddsr-consumer.ts     # DdsrConsumer — find/getService/subscribe
│   │       │   ├── registration.ts      # Registration Handle
│   │       │   ├── service-locator.ts   # ServiceLocator — invoke() + typed access
│   │       │   ├── flavor-plugin.ts     # FlavorPlugin SPI
│   │       │   └── diagnostics.ts       # DiagnosticCode Enum
│   │       │
│   │       ├── registry/                # Lokale Registry (auf TSM ServiceRegistry)
│   │       │   ├── local-service-registry.ts
│   │       │   ├── service-event-bus.ts
│   │       │   └── broker-sync.ts       # Snapshot + SSE
│   │       │
│   │       ├── proxy/                   # ServiceProxy
│   │       │   ├── proxy-factory.ts     # createServiceProxy<T>()
│   │       │   ├── parameter-validator.ts
│   │       │   └── invocation-handler.ts
│   │       │
│   │       ├── bootstrap/               # Broker-Bootstrap
│   │       │   ├── bootstrap.ts         # Synthetischer Locator → echte Locators
│   │       │   └── broker-api.ts        # Bekannte Broker-API-Shape (fix)
│   │       │
│   │       ├── module.ts               # TSM ModuleLifecycle + Manifest
│   │       └── index.ts
│   │
│   ├── ddsr-flavor-rest/                # TSM-Modul: REST FlavorPlugin
│   │   ├── package.json
│   │   └── src/
│   │       ├── rest-flavor-plugin.ts    # FlavorPlugin Implementierung
│   │       ├── request-builder.ts       # Operation → fetch Request
│   │       ├── response-parser.ts       # Response → Result (XMI/JSON)
│   │       └── module.ts               # TSM ModuleLifecycle + Manifest
│   │
│   ├── ddsr-flavor-mqtt/                # TSM-Modul: MQTT FlavorPlugin (Inkrement 4)
│   │   └── ...
│   │
│   └── ddsr-codegen/                    # CLI-Tool (kein TSM-Modul)
│       ├── package.json
│       └── src/
│           ├── cli.ts
│           ├── stub-generator.ts
│           └── templates/
│               ├── service-interface.ts.ejs
│               └── proxy-factory.ts.ejs
│
└── examples/
    └── payment/
        ├── tsm-config.ts                # TSM-Runtime: Module laden
        ├── payment-provider.ts          # Publish Payment
        └── payment-consumer.ts          # Find + invoke + typisiert
```

---

## 4. Implementierungsplan

### Inkrement 1: Fundament (Broker-Client + dynamisches invoke)

Ziel: TSM-basiertes Framework. Publish, Lookup, Withdraw gegen den Broker. `ServiceLocator.invoke()` für dynamischen Service-Aufruf. Alles — inklusive Broker-Kommunikation — läuft über das RestFlavorPlugin.

| Schritt | Was | Details |
|---|---|---|
| **1.0** | Repo + Workspace | pnpm workspace, tsconfig, vitest, TSM + emfts als Dependencies |
| **1.1** | `ddsr-model` generieren | `@emfts/codegen` auf `ddsr.ecore`. TSM-Manifest + activate (Package-Registrierung). |
| **1.2** | `FlavorPlugin` Interface | SPI-Definition: `flavorKind`, `canHandle()`, `invoke()`. |
| **1.3** | `ddsr-flavor-rest` | REST-Implementierung: `RestOperationFlavor` → `fetch()`. Request-Builder, Response-Parser (XMI via `XMIResource`). Eigenes TSM-Modul. |
| **1.4** | Bootstrap | Synthetischer Locator aus Broker-URL → erster Call via RestFlavorPlugin → echte Broker-ServiceLocators. `broker-api.ts` definiert die bekannte Broker-API-Shape. |
| **1.5** | `DdsrProvider` + `Registration` | `publish()` → `brokerImpl.invoke("publish", ...)`. `withdraw()` → `brokerImpl.invoke("withdraw", ...)`. Diagnostic-Handling. |
| **1.6** | `DdsrConsumer` + `ServiceLocator` | `find()` → `brokerLookup.invoke("getReferences", ...)`. Multi-Root-XMI parsen. `ServiceLocator` mit `invoke()`. |
| **1.7** | `ServiceProxy` + `invoke()` | `invoke(opName, params)` → matched Operation im ServiceInterface → dispatcht über FlavorPlugin aus TSM-Registry. |
| **1.8** | `DiagnosticCode` Enum | Parity mit Java `DdsrDiagnostics` Codes. |
| **1.9** | `DdsrClient` als TSM-Modul | Manifest, `activate()` (FlavorPlugin-Discovery, Bootstrap, Service-Registration), `deactivate()`. |
| **1.10** | Smoke-Test Payment | TSM-Runtime starten → Module laden → Publish → Lookup → `invoke("charge")` → Withdraw. |

**Ergebnis:** Lauffähiges TSM-basiertes Framework. Ein Codepfad für Broker- und Provider-Calls.

### Inkrement 2: Typisierte Proxies (Codegen)

Ziel: Generierte Interfaces + Proxy-Factories. `consumer.getService<Payment>()`.

| Schritt | Was | Details |
|---|---|---|
| **2.1** | `ddsr-codegen` CLI | Input: ServiceInterface (XMI oder live vom Broker). |
| **2.2** | Interface-Generator | `ServiceInterface` → TS Interface. Parameter-Typen, Return-Typen, Exceptions. |
| **2.3** | ProxyFactory-Generator | `createPaymentProxy(locator): Payment` — wraps ServiceProxy. |
| **2.4** | `consumer.getService<T>()` | Typisierte Variante: `find()` + Proxy-Wrap. |
| **2.5** | Parameter-Validierung | `ParameterConstraint`s client-seitig prüfen vor Dispatch. |
| **2.6** | Payment-Beispiel typisiert | `payment.charge({amount: 42})` mit IDE-Autocomplete. |

**Ergebnis:** Volle Type-Safety für bekannte Services. Dynamischer `invoke()` für unbekannte.

### Inkrement 3: Lokale Registry + Events

Ziel: In-Process-Registry mit Broker-Sync und ServiceListener.

| Schritt | Was | Details |
|---|---|---|
| **3.1** | `LocalServiceRegistry` | Baut auf TSM `ServiceRegistry` auf. `registerService()` / `unregister()`. |
| **3.2** | `ServiceEventBus` | `ServiceListener` + `ServiceEvent` (REGISTERED / MODIFIED / UNREGISTERING / MODIFIED_ENDMATCH). |
| **3.3** | Snapshot-Sync | Connect: `brokerLookup.invoke("getSnapshot")` → populate lokale Registry. |
| **3.4** | SSE-Client | `EventSource` für Broker-Event-Stream → inkrementelle Registry-Updates. |
| **3.5** | ConnectionState | `CONNECTED` / `DEGRADED` / `OFFLINE`. Read-Only bei Partition. |
| **3.6** | Reconnect-Diff | Frischer Snapshot, Diff zu Cache, synthetische Events. |
| **3.7** | Transparenter Lookup | `consumer.find()` sucht lokal + remote — transparent. |

**Ergebnis:** Vollständiges Framework mit lokaler Registry, Events, Offline-Fähigkeit.

### Inkrement 4: MQTT-Flavor + erweitertes Plugin-System

| Schritt | Was | Details |
|---|---|---|
| **4.1** | `ddsr-flavor-mqtt` TSM-Modul | `MqttFlavor` → MQTT pub/sub (Request/Response-Pattern). |
| **4.2** | Flavor-Matching | ConsumerCapability automatisch aus geladenen Flavor-Modulen. |
| **4.3** | Cross-Flavor Test | Selber Service über REST und MQTT. Consumer wählt je nach Plugin. |

---

## 5. Technologie-Stack

| Aspekt | Wahl | Begründung |
|---|---|---|
| Runtime | Node 20+ | Native `fetch`, `EventSource` |
| Modul-System | `@eclipse-daanse/tsm` | DI, Lifecycle, Plugin-Discovery — OSGi-DS-Äquivalent |
| EMF Runtime | `@emfts/core` (npm) | XMI, EObject, ResourceSet, Package Registry |
| EMF Codegen | `@emfts/codegen` | `ddsr.ecore` → TS-Metamodell-Klassen |
| Package Manager | pnpm workspace | Monorepo mit lokalen Package-Referenzen |
| Build | `tsc` → ESM (`"type": "module"`) | Konsistent mit emfts + TSM |
| Test | `vitest` | Konsistent mit emfts + TSM |
| HTTP | `fetch` (built-in) via RestFlavorPlugin | Ein Codepfad für alles |
| SSE | `EventSource` (built-in) | Für Inkrement 3 |
| Service-Proxy | `Proxy` (ES6 built-in) | Method-Calls abfangen für typisierten Zugang |
| XMI | `XMIResource` aus `@emfts/core` (`sax`) | Kein eigener Parser |

---

## 6. Abhängigkeitsgraph

```
@emfts/core ◄───── ddsr-model ◄───── ddsr-client ◄───── examples/payment
                        ▲                 ▲    ▲
                        │                 │    │
                        │   @eclipse-daanse/tsm │
                        │                      │
                        │         ddsr-flavor-rest
                        │         ddsr-flavor-mqtt (Inkrement 4)
                        │
              ddsr-codegen (CLI, Build-Zeit)
```

Keine zirkulären Abhängigkeiten. Alle Runtime-Module sind TSM-Module.

---

## 7. Mapping DDSR ↔ TSM ↔ OSGi

| DDSR-Konzept | TSM | OSGi |
|---|---|---|
| FlavorPlugin Discovery | `services.getAll("ddsr.flavor.*")` | `@Reference(target="(flavor=REST)")` |
| DdsrClient Lifecycle | `activate(ctx)` / `deactivate(ctx)` | `@Activate` / `@Deactivate` |
| Service-Registration | `context.services.register("id", svc)` | `BundleContext.registerService()` |
| Service-Lookup | `context.services.get<T>("id")` | `BundleContext.getServiceReference()` |
| Modul = Deployment-Einheit | TSM Module (entry.js) | OSGi Bundle (JAR) |
| ConsumerCapability | Automatisch aus geladenen Flavor-Modulen | Automatisch aus installierten Bundles |
| Hot-Reload Flavor | TSM Hot-Reload | OSGi Bundle-Update |
| Broker-Kommunikation | invoke() → FlavorPlugin (gleicher Pfad) | OSGi Remote Services |

---

## 8. Risiken & offene Punkte

| Risiko | Mitigation |
|---|---|
| `@emfts/codegen` + `ddsr.ecore` (~90 Classifier) | Frühzeitig testen (Schritt 1.1). Fixes direkt in emfts-codegen. |
| Multi-Root-XMI im Lookup-Response | `@emfts/core` XMIResource kann es. Früh mit echtem Broker-Response testen. |
| Package-Registrierung der generierten DDSR-Klassen | Im `ddsr-model` TSM-activate: DDSRPackage in globale Registry. |
| TSM `services.getAll("ddsr.flavor.*")` Pattern-Matching | TSM-ServiceRegistry unterstützt `getAll` mit Pattern. Verifizieren. |
| Bootstrap Chicken-and-Egg | Synthetischer Locator aus bekannter Broker-API-Shape. Ersetzt sich nach erstem Call. |
| Ebene-2-Codegen: Typ-Mapping ServiceOperation → TS | `returnType` ist String im Modell. Mapping definieren. |
| XMI-Serialisierung für POST-Bodies | RestFlavorPlugin muss EObjects zu XMI serialisieren können (`XMIResource.save`). |

---

## 9. Abnahmekriterien pro Inkrement

**Inkrement 1 — done when:**
- [ ] `ddsr-model` baut, alle ~90 Classifier als TS-Klassen generiert
- [ ] XMI Round-Trip: DDSRFactory → XMIResource.save → load → identische Struktur
- [ ] TSM-Runtime startet, alle Module aktivieren (ddsr-model, ddsr-flavor-rest, ddsr-client)
- [ ] Bootstrap: synthetischer Locator → echte Broker-Locators via RestFlavorPlugin
- [ ] FlavorPlugin automatisch entdeckt, ConsumerCapability = ["REST"]
- [ ] `provider.publish()` geht über `brokerImpl.invoke()` → RestFlavorPlugin → Broker
- [ ] `consumer.find()` geht über `brokerLookup.invoke()` → RestFlavorPlugin → Broker
- [ ] Payment Smoke-Test: Publish → Lookup → `invoke("charge")` → Withdraw
- [ ] Diagnostic-Codes stimmen mit Java überein

**Inkrement 2 — done when:**
- [ ] `ddsr-codegen` generiert `Payment` Interface + ProxyFactory aus Catalog-Eintrag
- [ ] `payment.charge({amount: 42})` kompiliert und läuft typisiert
- [ ] Parameter-Validierung lehnt ungültige Params client-seitig ab
- [ ] Selber Test: untypisiert via `invoke()` + typisiert via Proxy

**Inkrement 3 — done when:**
- [ ] Lokale Registry hält Snapshot nach Connect
- [ ] ServiceListener empfängt REGISTERED/UNREGISTERING Events via SSE
- [ ] connectionState wechselt CONNECTED → OFFLINE → CONNECTED
- [ ] Transparent Lookup: lokaler + remote Service in einem `find()`

**Inkrement 4 — done when:**
- [ ] REST + MQTT FlavorPlugins als separate TSM-Module
- [ ] Consumer mit nur REST-Plugin bekommt nur REST-Impls
- [ ] MQTT-basierter Service-Aufruf end-to-end
