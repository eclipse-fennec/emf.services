# DDSR — Service-API Update Policy

**Status:** Design-Entwurf nach Diskussion. Implementierung steht aus (Stufe-3-Material).
**Letzte Aktualisierung:** 2026-05-29.

Regelt, wie der Broker mit neuen Versionen einer Service-API umgeht, ob und wann alte Versionen verschwinden, und wie Consumer davon erfahren. Komplement zur Architektur-Beschreibung in [ARCHITECTURE.md](ARCHITECTURE.md) und löst implizit OPEN_ISSUES A1 (SSE/Event-Stream), C2 (provider-aware lookup) und C3 (stale providers) mit ab.

---

## 1. Motivation

Heute (siehe [ARCHITECTURE.md §2.6](ARCHITECTURE.md)) dedupliziert der Broker bei `publishImplementation` über `(name, version)` und retired die alte Impl synchron, wenn eine neue gleicher Identität publisht wird. Das ist gut, aber:

- **Es gibt nur eine Variante.** Für die Broker-API selbst will man Versionen parallel laufen lassen ("evergreen"); für gewöhnliche APIs will man einen Migrations-Fenster mit Deprecation; manchmal will man einen harten Cut.
- **Consumer erfahren von Änderungen passiv.** Solange er nicht aktiv re-look-uppt, hängt er auf der alten Reference.
- **Es gibt keinen sauberen Drain-Mechanismus.** Der Broker weiß nicht, wer eine Reference noch hält, also kann er nicht wissen, wann es sicher ist, eine alte Version wegzuräumen.

Diese Doc spezifiziert drei Policies, das Trigger-Modell, und das dafür nötige Heartbeat-Protokoll.

## 2. Die drei Update-Policies

Policy wird **am `ServiceInterface`** annotiert (Default für alle Impls dieses Interfaces); per `ServiceImplementation` überschreibbar, wenn eine konkrete Impl abweichendes Verhalten haben soll. Default für Catalog-Einträge ohne explizite Angabe: `DEPRECATE_AND_DRAIN` (sicher, freundlich).

### 2.1 `EVERGREEN`

Mehrere Major-Versionen laufen unbegrenzt parallel. Keine wird automatisch retired. Soft-Migrations bleiben Consumer-Sache (sie wählen explizit eine Version via `versionRange`).

**Wann:** Broker-API selbst, infrastrukturelle Dienste mit unbekannten Konsumenten, alles wo der Provider keine Kontrolle über die Consumer-Lifecycles hat.

**Broker-Verhalten:**

- `publishImplementation(v2)` nimmt v2 zusätzlich auf. v1 bleibt unverändert.
- `find()` ohne `versionRange` returnt die höchste non-deprecated Version (entspricht heutigem Default).
- `find(versionRange="[1.0,2.0)")` returnt v1, `find(versionRange="[2.0,3.0)")` returnt v2.
- Retire passiert nur explizit per `withdrawImplementation(v1)` durch den Provider.

### 2.2 `DEPRECATE_AND_DRAIN`

Neue Version koexistiert mit der alten; alte wird als `deprecated=true` markiert. Neue `find()`-Aufrufe ohne explizite Range bekommen die neue Version. Existierende Consumer auf der alten Version bekommen ein `UPGRADE_AVAILABLE`-Event und können freiwillig switchen. Sobald der Broker keine aktiven Refs auf v1 mehr sieht (siehe §4), retired er v1 automatisch.

**Wann:** geplante API-Evolution mit Migrations-Fenster; sanftere Variante als HARD_CUTOVER.

**Broker-Verhalten:**

- `publishImplementation(v2)` nimmt v2 auf, setzt `v1.deprecated = true`, schreibt `v1.replacedBy = v2`.
- `find("Payment")` returnt **v2** (deprecated wird default ausgefiltert).
- `find("Payment", versionRange="[1.0,2.0)")` returnt v1 weiterhin (expliziter Bezug auf alte Range).
- Broker sendet `ServiceEvent.UPGRADE_AVAILABLE` an alle Consumer, die laut Heartbeat-Map noch Refs auf v1 halten.
- Greedy Consumer (Capability: `ddsr.consumer.greedy=true`) re-binden automatisch beim Eintreffen des Events.
- Sobald Heartbeat-Map kein Consumer mehr v1-Refs hält, sendet Broker `RETIRED` an niemanden (es gibt ja keinen mehr) und entfernt v1 aus dem Registry.
- **Kein hartes Timeout.** v1 darf beliebig lange leben, solange Consumer drauf sind.

### 2.3 `HARD_CUTOVER`

Neue Version wird publisht; nach kurzem Failover-Fenster wird die alte zwangsweise retired. Consumer bekommen `UNREGISTERING`, müssen rebinden (oder gehen in Fehler).

**Wann:** Security-Fix, Bug-Killer, breaking change ohne Migrationsbudget.

**Broker-Verhalten (Reihenfolge ist load-bearing):**

1. **Phase 1 — Parallel-Aufnahme:** `publishImplementation(v2, policy=HARD_CUTOVER, replaces=v1)` nimmt v2 auf. v1 bleibt momentan noch verfügbar.
2. **Phase 2 — Failover-Fenster:** Broker wartet konfigurierbares Intervall (Default: `30s`; konfigurierbar pro Impl via `cutoverGraceMillis`). Während dieses Fensters können Consumer parallel sowohl v1 als auch v2 sehen — sie können bewusst auf v2 rebinden, wenn sie wollen.
3. **Phase 3 — Unregister-Event:** Broker sendet `ServiceEvent.UNREGISTERING(impl=v1, reason=BROKER_CUTOVER)` an alle Consumer aus der Heartbeat-Map, die Refs auf v1 halten. Bei Stream-Channels (siehe `WIRE_CHANNELS.md`) initiiert der Broker den graceful Close auf Provider- und Consumer-Seite.
4. **Phase 4 — Retire:** Broker entfernt v1 aus Catalog und Registry. `find()` returnt ab jetzt nur noch v2.

**Warum erst v2 dann v1 retiren, nicht andersrum:** im kurzen Overlap-Fenster (Phase 2) hat der Consumer eine echte Failover-Option. Andernfalls (v1 erst raus, dann v2 rein) gäbe es ein Zeitfenster ohne *irgendeine* Implementation — laufende Calls würden hart fehlschlagen.

## 3. Trigger-Modell — alles über Events, kein Poll

Consumer erfahren von Policy-Wechseln über den Event-Stream (Stufe-3-Feature, OPEN_ISSUES A1). Polling ist explizit nicht vorgesehen — wir wollen reaktive Konsumenten.

**Event-Typen, die der Broker sendet:**

| Event | Wann | Reaktion erwartet |
|---|---|---|
| `UPGRADE_AVAILABLE` | bei `DEPRECATE_AND_DRAIN`-Publish | Hint; Consumer entscheidet selbst |
| `UNREGISTERING` | bei `HARD_CUTOVER` Phase 3, oder bei `withdrawImplementation()` | Consumer **muss** rebinden, sonst Fehler |
| `RETIRED` | nachdem die alte Impl tatsächlich entfernt wurde | Cleanup-Hinweis; kein Action-Required für Consumer (er sollte sich schon längst umgebunden haben) |
| `REGISTERED` | bei neuer Impl-Publish (egal welche Policy) | Information für interessierte Subscriber |

**Greediness als Consumer-Property** (`ddsr.consumer.greedy`, Default `false`):

- `greedy=true`: Consumer-Facade rebindet auf `UPGRADE_AVAILABLE` automatisch. Geeignet für Long-Running-Services, die immer "die aktuellste API" wollen.
- `greedy=false`: Consumer-Facade liefert den Event durch zum Application-Code, der selbst entscheidet (Default — defensiver).

`UNREGISTERING` ist nicht greediness-abhängig; der Consumer **muss** in jedem Fall reagieren.

## 4. Heartbeat-Protokoll (Voraussetzung für Drain & C3)

Der Broker muss zuverlässig wissen, welcher Consumer welche Refs hält, sonst funktioniert weder `DEPRECATE_AND_DRAIN`-auto-retire noch generelles Stale-Cleanup (OPEN_ISSUES C3).

> **Präzisiert in [ACQUISITION.md](ACQUISITION.md):** die Session besitzt die Leases (Registration bekommt nur die abgeleitete Sicht), acquire/release/heartbeat kollabieren zu einem idempotenten `PUT /consumers/{id}`-Vollabgleich, Leases werden bewusst nicht persistiert (Neustart-Grace-Regel für Auto-Retire beachten), und `ServiceReference.usingProviders` wird deprecated (M5). Die Skizze unten bleibt als Ursprung stehen; bei Widerspruch gilt ACQUISITION.md.

**Daten-Modell im Broker:**

```
Map<ConsumerId, ConsumerState>
  ConsumerState
    lastHeartbeat : Instant
    activeRefs    : Set<ServiceReferenceId>
    capabilities  : ConsumerCapability   (greedy, supportedFlavors, …)
```

**Heartbeat-Protokoll:**

- Consumer sendet alle **10 Minuten** einen `ConsumerHeartbeat` an den Broker mit aktueller `activeRefs`-Liste.
- Broker aktualisiert `lastHeartbeat` und die Ref-Map.
- Fehlt ein Heartbeat für **2× das Intervall** (20 Minuten), markiert der Broker den Consumer als tot und gibt alle seine Refs frei.

**Shutdown-Notify:**

- Beim regulären Shutdown sendet der Consumer einen `ConsumerShutdown`-Event an den Broker (alle aktiven Refs werden freigegeben). Damit ist Drain auch ohne 20-Minuten-Wartezeit zügig.
- Best-Effort — wenn der Consumer abstürzt, übernimmt der Heartbeat-Timeout.

**Lösung für OPEN_ISSUES C3 als Side-Effect:** dasselbe Protokoll, applied auf Provider-Seite, gibt uns Provider-Reachability-Probing. Provider sendet alle 10 Min `ProviderHeartbeat`; nach 20 Min ohne Heartbeat retired der Broker den Provider-Eintrag inkl. aller Impls.

**Tradeoff:** ein toter Consumer wird bis zu 20 Minuten lang als Live-Ref-Halter geführt. Für Drain-Semantik nicht ideal, aber pragmatisch — das Alternativ-Modell (jeder `getService`/`ungetService` round-trip zum Broker) wäre eine Größenordnung mehr Traffic für minimal mehr Genauigkeit.

## 5. Version-Negotiation beim Lookup

Damit Consumer in `EVERGREEN`-Setups gezielt eine Version anfordern können, wird die Lookup-API erweitert:

- `find("Payment")` — Default: höchste non-deprecated Version, gefiltert durch Consumer-Capabilities.
- `find("Payment", versionRange="[2.0,3.0)")` — explizite Range nach OSGi-Versionsrange-Syntax.
- `ConsumerCapability.supportedVersions : Map<InterfaceName, VersionRange>` — Consumer kann pro Interface eine Range deklarieren, die der Broker bei allen Lookups automatisch anwendet (analog zu `supportedFlavors`).

**Deprecation-Filter:** by default werden `deprecated=true`-Impls aus dem Lookup-Result entfernt. Mit `find("Payment", includeDeprecated=true)` kann ein Consumer auch deprecated Versions sehen (Management-Use-Case, Migrations-Tools).

## 6. Verhältnis zur Hook-Architektur

Update-Policy ist die erste konkrete Verwendung der drei Hooks aus dem REQUIREMENTS-Dokument:

- **`PublishHook`** — enforced Policy auf der Publish-Seite. Wenn ein Provider `publishImplementation(v2)` ohne `replaces`-Hinweis macht, könnte der Hook das in einen `EVERGREEN`-Add verwandeln, oder den Publish ablehnen, wenn Policy `HARD_CUTOVER` verlangt aber `cutoverGraceMillis` fehlt.
- **`DiscoveryHook`** — filtert `deprecated=true` aus für non-greedy Consumer; appliziert `versionRange`-Filter.
- **`DistributionHook`** — bei `UNREGISTERING` von Stream-Channels: triggert den graceful Close auf beiden Seiten (siehe `WIRE_CHANNELS.md`, Stream-Termination).

Vollständige Hook-Spezifikation kommt in einer eigenen Doc; hier nur die Andockpunkte.

## 7. Cross-Constraints zu Streams

Bei `HARD_CUTOVER` auf einem Stream-Service: das `UNREGISTERING`-Event geht an Consumer und Provider. Beide initiieren graceful Close ihres Stream-Endes (Reason-Code: `BROKER_CUTOVER`). Wer zuerst sendet, der andere kriegt's via Wire-Close-Frame und antwortet symmetrisch. Stream wird sauber beendet, neue Calls gegen v1 schlagen mit `SERVICE_RETIRED` fehl.

Bei `DEPRECATE_AND_DRAIN` auf einem Stream: der Stream zählt als aktive Ref. v1 wird erst retired, wenn alle Stream-Channels graceful beendet sind. Lange Streams können also v1 unbegrenzt am Leben halten — das ist by design.

Stream-Termination-Mechanismus selbst ist in `WIRE_CHANNELS.md` spezifiziert.

## 8. Implementations-Reihenfolge

Die Features bauen aufeinander auf. Vorgeschlagene Reihenfolge:

1. **Heartbeat-Protokoll** (löst C3, ist Voraussetzung für alles andere). Standalone wertvoll.
2. **Event-Stream** (OPEN_ISSUES A1). Voraussetzung für jegliche aktive Benachrichtigung. Sobald die Stream-Channel-Erweiterung aus `WIRE_CHANNELS.md` da ist, wird daraus ein konkreter Event-Channel.
3. **Policy-Annotation am Modell** (`ServiceInterface.updatePolicy`, `ServiceImplementation.updatePolicy`, `replaces`-Ref, `deprecated`-Flag, `cutoverGraceMillis`). Modell-Erweiterung; im v2-Workspace direkt mit reinbauen.
4. **Broker-Logik pro Policy** in der order EVERGREEN → DEPRECATE_AND_DRAIN → HARD_CUTOVER. EVERGREEN ist trivial (heutige Dedup-Logik ohne Retire). DEPRECATE_AND_DRAIN braucht Deprecation-Filter + Drain-Watcher. HARD_CUTOVER braucht Failover-Fenster-Timer + UNREGISTERING-Distribution.
5. **Version-Range im Lookup** — kann auch früher gehen, ist orthogonal.

## 9. Offene Punkte

- **Default-Policy:** ich habe oben `DEPRECATE_AND_DRAIN` als Default vorgeschlagen. Alternativ wäre `EVERGREEN` (sicherer für unbekannte Konsumenten) oder gar **kein Default** (Modell-Validator erzwingt explizite Angabe). Zu entscheiden.
- **`cutoverGraceMillis` Default:** 30s ist ein Schätzwert. Wahrscheinlich service-typ-abhängig (für interaktive Services länger, für Batch-Backends kürzer).
- **Heartbeat-Intervall:** 10 Min ist pragmatisch. Falls sich rausstellt dass Drain zu langsam ist, kürzer machen (Tradeoff Traffic).
- **`replaces`-Semantik bei mehrstufigen Migrations:** wenn v1 → v2 → v3 hintereinander publisht werden, ist v3 die `replaces` von v2 (welches die `replaces` von v1 ist). Reicht das, oder soll der Broker eine Chain auflösen ("retire v1 sobald v3 publisht, nicht erst wenn v2 retired ist")?
- **Multi-Catalog:** wenn ein Interface in mehreren Catalogs gleichzeitig existiert (Stufe 4+), gilt die Policy pro-Catalog oder global? Erstmal: pro-Catalog. Aber das müssen wir mit der Federation-Diskussion zusammen klären.

## 10. Referenzen

- [ARCHITECTURE.md](ARCHITECTURE.md) — heutiger Stand des Brokers, insb. §2.6 (Dedup, Reindex)
- [WIRE_CHANNELS.md](WIRE_CHANNELS.md) — Channel-Modell, Stream-Termination, Capability/Requirement-System
- [OPEN_ISSUES.md](OPEN_ISSUES.md) — A1 (SSE/Event-Stream), C2 (provider-aware lookup), C3 (stale providers)
- [REQUIREMENTS.md](REQUIREMENTS.md) — Stufe-2-DoD und Cross-Language-Demo-Flow
