# DDSR — Security-by-Design: Bedrohungsanalyse, Risiken, Standards

**Status:** Design-Entwurf / lebendes Dokument. Wird mit jeder neuen
Funktionalität fortgeschrieben — Bedrohungsanalyse ist Teil der
Definition-of-Done jedes Features, nicht ein nachgelagerter Audit.
**Letzte Aktualisierung:** 2026-06-11.

Dieses Dokument etabliert Security als **erste Klasse** im DDSR-Design.
Es ergänzt [REQUIREMENTS.md](REQUIREMENTS.md) (dort `NFR-Security-*`),
[ARCHITECTURE.md](ARCHITECTURE.md) (heutiger Stand der Komponenten) und
[OPEN_ISSUES.md](OPEN_ISSUES.md) (konkrete Findings als `S*`-Serie).

---

## 0. Abgrenzung: Hooks ≠ Security

REQUIREMENTS §7 listet "Concrete security implementation" als
out-of-scope und verweist auf die drei Interception-Hooks
(`PublishHook` / `DiscoveryHook` / `DistributionHook`). **Das deckt nur
eine Hälfte ab.** Die Hooks sind der Andockpunkt für externe
**Authorization/Policy** (PDP/PEP/IAM). Sie regeln *nicht*:

- Transport-Härtung (TLS, mTLS, Cipher-Auswahl)
- Authentifizierung am Broker (wer darf überhaupt publishen/lookuppen)
- Sicheres Parsen des Wire-Formats (XXE, Entity-Expansion, SSRF über
  Cross-Doc-`href`)
- Input-Validierung und Ressourcen-Limits (DoS)
- Sichere Defaults (fail-closed statt fail-open)
- Secrets-Handling, Logging-Hygiene, Fehler-Leakage

Diese gehören in den **DDSR-Kern bzw. die Referenz-Flavors** und werden
hier spezifiziert. Die Hook-Schicht bleibt für Policy zuständig; alles
darunter ist DDSR-eigene Verantwortung.

---

## 1. Methodik — Bedrohungsanalyse pro Funktionalität

Für **jede** Funktionalität (bestehend wie neu) durchläuft das Team
denselben vierstufigen Zyklus. Das Ergebnis wird in §4 als Tabelle
festgehalten.

1. **Asset & Trust-Boundary benennen.** Was wird geschützt (Katalog-
   Integrität, Provider-Identität, Verfügbarkeit des Brokers …) und wo
   verläuft die Vertrauensgrenze (Client↔Broker, Local↔Remote,
   Provider↔Consumer-Wire)?
2. **Bedrohungen nach STRIDE ableiten.** Spoofing, Tampering,
   Repudiation, Information Disclosure, Denial of Service, Elevation of
   Privilege — je Trust-Boundary durchgehen.
3. **Risiko bewerten.** Schadenshöhe × Eintrittswahrscheinlichkeit auf
   einer 3-stufigen Skala (niedrig/mittel/hoch). Begründung dokumentieren,
   nicht nur die Zahl.
4. **Handlungsoption wählen.** Genau eine von: *Mitigate* (Maßnahme
   bauen), *Accept* (bewusst tragen, mit Begründung + Owner), *Transfer*
   (an Integrator/Deployment delegieren — typischer Fall bei den Hooks),
   *Avoid* (Funktionalität/Default streichen). Jede Option bekommt eine
   `S*`-ID in OPEN_ISSUES, wenn daraus Arbeit entsteht.

**Regel:** Eine Funktionalität gilt erst als *done*, wenn ihre
STRIDE-Zeile existiert und jede hoch/mittel-Bedrohung eine gewählte
Handlungsoption hat. Prototyp-Akzeptanzen sind erlaubt, müssen aber als
`Accept` mit Begründung markiert sein — nicht stillschweigend offen
bleiben.

---

## 2. Standards-Mapping

> **Kanonische Quellen liegen lokal im Workspace** und werden direkt
> konsultiert (nicht aus dem Gedächtnis/Web zitiert):
> - OWASP ASVS: `/opt/git/OWASP-ASVS/` — Version 5.0 als strukturiertes
>   JSON/CSV/XML unter `5.0/docs_en/`.
> - BSI „Stand der Technik"-Bibliothek: `/opt/git/BSI-Stand-der-Technik-Bibliothek/`
>   — **OSCAL**-Kataloge. Maßgeblich ist der **Anwenderkatalog
>   `Grundschutz++`** (`Anwenderkataloge/Grundschutz++/Grundschutz++-catalog.json`,
>   Stand 2026-04) sowie der Profil-Katalog **`Mindeststandard-TLS`**.
>   **Achtung:** Das ist die neue Grundschutz++-Taxonomie (DEV / KONF /
>   BER / ARCH / DET / RISK …), **nicht** das klassische
>   IT-Grundschutz-Kompendium mit CON/APP-Bausteinen.

### 2.1 OWASP ASVS 5.0

ASVS 5.0 gliedert sich in **17 Kapitel (V1–V17)**. DDSR ist eine
**Library/Framework + ein Broker-Service mit HTTP/SSE-API** — kein
User-facing Frontend. Anwendbare Kapitel (Namen exakt nach
`5.0/docs_en`), nur situativ relevant sind V3 Web Frontend Security,
V10 OAuth and OIDC (sofern kein eigener AuthZ-Server) und V17 WebRTC:

| ASVS-Kapitel | Relevanz für DDSR | Wo adressiert |
|---|---|---|
| **V1 Encoding and Sanitization** | XMI-Wire-Parsing, LDAP-Filter-Strings | §4 Wire-Codec, Lookup |
| **V2 Validation and Business Logic** | Catalog-Mutationen, Semver-Enforce, Cardinality | §4 Publish/Catalog |
| **V4 API and Web Service** | Broker-REST-Endpoints, SSE-Stream | §4 Broker-API |
| **V5 File Handling** | XMI-Snapshot-Persistenz, `file:`-URI-Auflösung | §4 Wire-Codec, Persistenz |
| **V6 Authentication** | Wer darf publishen/lookuppen; Provider-Identität | §4 Broker-API; S2 |
| **V7 Session Management** | SSE-Stream-Identität, Heartbeat-Consumer-ID | §4 Event-Stream/Heartbeat |
| **V8 Authorization** | Hook-Andockpunkt (an Integrator delegiert), Tenant-Isolation | §4; Transfer an Hooks |
| **V9 Self-contained Tokens** | falls Broker-AuthN über JWT/PASETO läuft | §4 Broker-API (Option) |
| **V11 Cryptography** | Schlüssel/Algorithmen für TLS, Snapshot-at-Rest | §3 Defaults; S1 |
| **V12 Secure Communication** | Transport-Security Client↔Broker und Provider↔Consumer | §3 Defaults; S1 |
| **V13 Configuration** | **sichere Defaults**, Härtung, deaktivierte Funktionen | §3 Defaults; S1/S2/S7 |
| **V14 Data Protection** | Katalog-/Property-Vertraulichkeit, Scrubbing am Federation-Boundary | §4; W1/S6 |
| **V15 Secure Coding and Architecture** | Security-by-Design über alle Module, Wire-Parsing | §1 Methodik; S3 |
| **V16 Security Logging and Error Handling** | Diagnostic-Leakage, Audit-Trail (`X-DDSR-Requestor`) | §4; S5 |

Der anzustrebende ASVS-**Level** (L1 / L2 / L3) wird pro
Deployment-Profil festgelegt: der vom DDSR-Projekt **selbst betriebene**
Broker zielt auf **L2**; eingebettete Library-Nutzung erbt das Profil
des einbettenden Systems.

### 2.2 BSI Grundschutz++ (OSCAL-Anwenderkatalog) + Mindeststandard-TLS

Die maßgeblichen Control-Familien aus dem `Grundschutz++`-Katalog für
DDSR. Control-IDs sind direkt aus dem OSCAL-Katalog zitiert:

| Familie | Gegenstand | Einschlägige Controls für DDSR |
|---|---|---|
| **DEV — Entwicklung** | Security by Design, Härtung, Code, Freigabe | DEV.2.1 (Security-by-Design-Architektur), DEV.2.3 (minimale Rechte), DEV.2.6 (Schutz gg. gängige Angriffsmuster), DEV.3.2/3.3 (Fehlerbehandlung; **keine** schützenswerten Daten in Fehlermeldungen → S5), DEV.4.9 (**Security by Default** → §3), DEV.4.3/4.4/4.7 (SBOM, Integrität externer Libs, deterministischer Binärcode → S9), DEV.4.10 (Code-Änderungs-Protokoll), DEV.6/7 (Freigabe, Zertifikats-Monitoring) |
| **KONF — Konfiguration** | sichere Voreinstellungen verteilter Anwendungen | KONF.14.1 (**Verschlüsselung beim Transport** → S1), KONF.14.5 (Timeout von Netzverbindungen → OperationChannel-Timeout), KONF.12.1 (**Eingabevalidierung** → S3/S4), KONF.11.1 (AuthN vor Zugriff → S2), KONF.11.8 (Einschränkung von Schnittstellen), KONF.11.9 (Verschlüsselung at-rest → Snapshot), KONF.15.1/15.2/15.3 (**Ressourcen-Limits/DoS** → S7), KONF.5.1 (AuthN am System → S2), KONF.10.2/10.3/10.4 (Krypto-Verfahren, Default-Zugangsdaten ändern, nicht benötigte Funktionen deaktivieren → §3) |
| **BER — Berechtigung** | Identität, AuthN, Schlüsselmanagement | BER.2 (Identitätsmanagement → S2/S8), BER.4 (Authentifizierung), BER.7.* (**Schlüsselmanagement** für TLS: etablierte Algorithmen, Schlüssellängen, Integrität/Authentizität/Gültigkeit vor Nutzung → S1) |
| **ARCH — Architektur** | Vertraulichkeit/Integrität im Netz, Ausfallsicherheit | ARCH.6.2 (Verschlüsselung von Weitverkehrsverbindungen → S1), ARCH.8.3 (redundante Server — out-of-scope Proto, vgl. NFR-RemoteRegistry-Singleton) |
| **DET — Detektion** | Protokollierung | DET.3.1 (sicherheitsrelevante Ereignisse protokollieren → S5), DET.3.4 (**Revisionssicherheit** des Audit-Logs), DET.3.5 (**Unbestreitbarkeit** → STRIDE-Repudiation) |
| **RISK — Risikomanagement** / **STM.8** | Risikobetrachtung | trägt die Methodik aus §1 (Risikoeigentümer, Freigabe der Umsetzungsplanung) |

**Mindeststandard-TLS** (eigener Profil-Katalog) konkretisiert die
TLS-Anforderung über `KONF.14.1` / `KONF.2.2` / `KONF.10.2` —
maßgeblich für die Cipher-/Versions-Auswahl in **S1**. Die genauen
TLS-Versionen/Parameter werden aus diesem Katalog gezogen, nicht
freihändig gesetzt.

---

## 3. Sichere Defaults (fail-closed)

Defaults sind ein expliziter Teil des Security-Designs — ein unsicherer
Default ist eine Schwachstelle, auch wenn ein sicherer Modus existiert.
Leitprinzip: **secure by default, opt-out statt opt-in**.

Übergreifend einschlägig: **DEV.4.9 "Voreinstellungen nach dem Prinzip
Security by Default"** und **KONF.10.3 "Änderung von Default-Zugangsdaten"**
/ **KONF.10.4 "Deaktivierung nicht benötigter Funktionen"**.

| Bereich | Default heute (Prototyp) | Ziel-Default | Standard-Bezug |
|---|---|---|---|
| **Transport Client↔Broker** | HTTP plaintext | **HTTPS/TLS**, plaintext nur per expliziter Dev-Flag | ASVS V12 / KONF.14.1 + Mindeststandard-TLS |
| **Provider↔Consumer-Wire** | HTTP plaintext | TLS; mTLS optional über Capability | ASVS V12 / KONF.14.1, ARCH.6.2 |
| **Broker-AuthN** | keine (offen) | **fail-closed**: ohne Credential kein Publish/Mutate; Read ggf. konfigurierbar offen | ASVS V6 / KONF.11.1, KONF.5.1 |
| **XMI-Parsing** | `Map.of()`, keine Limits | XXE **aus**, DTD/External-Entities **verboten**, Cross-Doc-`href` nur auf Allowlist, Größen-/Tiefen-Limit | ASVS V1/V5 / KONF.12.1; S3 |
| **Hook-Pipeline** | leer = alles erlaubt | leer bleibt erlaubt **nur** im Dev-Profil; Prod-Profil verlangt mind. einen AuthZ-Hook oder lehnt Start ab | DEV.2.3, ASVS V8 |
| **Fehler-Responses** | Exception-Message im Body möglich | generische Diagnostic nach außen, Details nur ins Log | ASVS V16 / DEV.3.3 |
| **Ressourcen-Limits** | keine | Max-Body-Size, Max-Operations/Interface, Rate-Limit pro Requestor, Connection-Timeout | KONF.15.1/15.3, KONF.14.5; S7 |
| **`connectionState` bei Partition** | — (FR-Partition) | Writes fail-closed (bereits so spezifiziert, FR-Partition-WritesRejected) | konsistent / fail-closed |

Die Prototyp-Defaults dürfen unsicher bleiben — aber **markiert als
`Accept` mit Dev-Profil-Scope** (siehe §4), und das Produkt-Profil muss
die sichere Variante vor dem ersten externen Deployment erzwingen.

---

## 4. Threat-Tabellen pro Funktionalität

STRIDE-Spalten: S=Spoofing, T=Tampering, R=Repudiation, I=Info
Disclosure, D=DoS, E=Elevation. Risiko: N/M/H. Option: Mitigate /
Accept / Transfer / Avoid.

### 4.1 Broker-REST-API (`/catalog`, `/implementations`, `/references`, `/registry`)

| # | Bedrohung (STRIDE) | Risiko | Option | Maßnahme / Finding |
|---|---|---|---|---|
| | Unauthentisierter Publish fälscht Katalog (S/T/E) | **H** | Mitigate | Broker-AuthN → **S2** |
| | Beliebiger Client mutiert/entfernt Catalog-Entry (T/E) | **H** | Transfer→Mitigate | AuthZ via `PublishHook`; AuthN-Untergrenze in DDSR → S2 |
| | `/registry`-Dump leakt interne `file:`-Pfade (I) | M | Mitigate | **W1 → S6** |
| | Flooding mit Publishes / großen Bodies (D) | M | Mitigate | Rate-Limit + Body-Size-Cap → **S7** |
| | Kein nachvollziehbarer Audit-Trail (R) | M | Mitigate | strukturiertes Audit-Log → **S5** |

### 4.2 XMI-Wire-Codec (`xmi.codec`, server- und client-seitig)

| # | Bedrohung (STRIDE) | Risiko | Option | Maßnahme / Finding |
|---|---|---|---|---|
| | XXE / External-Entity beim Laden von Client-XMI (I/D) | **H** | Mitigate | sichere Parser-Defaults → **S3** |
| | SSRF: Cross-Doc-`href` zwingt Broker, interne URL aufzulösen (I/E) | **H** | Mitigate | `href`-Allowlist, `resolveAll` nur auf vertrauenswürdige URIs → **S3** |
| | Entity-Expansion / tiefe Verschachtelung (Billion Laughs) (D) | **H** | Mitigate | Limits → **S3** |
| | `file:`-URI im Output leakt Serverpfade (I) | M | Mitigate | **W1 → S6** |

### 4.3 Lookup / Discovery (`/references`)

| # | Bedrohung (STRIDE) | Risiko | Option | Maßnahme / Finding |
|---|---|---|---|---|
| | LDAP-Filter-Injection / teure Filter (I/D) | M | Mitigate | Filter-Syntax-Validierung + Komplexitäts-Limit → **S4** |
| | Consumer sieht Refs fremder Tenants (I) | M | Transfer | `DiscoveryHook.filterReferences` (Integrator) |
| | Provider-Locator zeigt auf Angreifer-Host (S) | M | Mitigate | C2 (reachability-aware) + signierte/verifizierte Provider-Identität |

### 4.4 Event-Stream / SSE *(Stufe 3, A1)*

| # | Bedrohung (STRIDE) | Risiko | Option | Maßnahme / Finding |
|---|---|---|---|---|
| | Unauthentisierter Subscriber liest alle Events (I) | **H** | Mitigate | AuthN am Stream + Capability-Filter (FR-Sync-Filtering) → S2 |
| | Viele offene Streams erschöpfen Broker (D) | M | Mitigate | Connection-Cap pro Requestor → S7 |
| | Event-Spoofing über DistributionHook-Boundary (T) | M | Transfer | `DistributionHook.onInbound/onOutbound` |

### 4.5 Heartbeat / Consumer-State *(Stufe 3, UPDATE_POLICY §4)*

| # | Bedrohung (STRIDE) | Risiko | Option | Maßnahme / Finding |
|---|---|---|---|---|
| | Gefälschte `ConsumerId` hält fremde Refs am Leben / drained sie weg (S/T) | M | Mitigate | Heartbeat an authentisierte Session binden, nicht an frei wählbare ID → S8 |
| | `ConsumerShutdown`-Spoof retired fremde Drains vorzeitig (T) | M | Mitigate | s.o. |

### 4.6 Code Publisher *(Stufe 3/4, FR-CodeDist-*)*

| # | Bedrohung (STRIDE) | Risiko | Option | Maßnahme / Finding |
|---|---|---|---|---|
| | Nicht-autorisierter Release schiebt Artefakt (E) | **H** | Transfer→Mitigate | `PublishHook` (FR-Hook-CodeDist-Authority) + AuthN |
| | Manipuliertes Artefakt (Supply-Chain) (T) | **H** | Mitigate | Artefakt-Signierung + Reproducible-Build (FR-CodeDist-Reproducible) → S9 |

### 4.7 Snapshot-Persistenz (`broker-state.xmi`)

| # | Bedrohung (STRIDE) | Risiko | Option | Maßnahme / Finding |
|---|---|---|---|---|
| | Lesezugriff auf Snapshot-Datei leakt gesamten Katalog (I) | M | Mitigate | Dateirechte/Encryption-at-Rest je Deployment-Profil → S6 |
| | Manipulation des Snapshots zwischen Restarts (T) | M | Accept (Proto) | Dev-Akzeptanz; Prod: Integritätsschutz |

---

## 5. Findings-Übersicht (Verweise nach OPEN_ISSUES `S*`)

Die konkreten, umzusetzenden Punkte leben als `S*`-Serie in
[OPEN_ISSUES.md](OPEN_ISSUES.md) (stabile IDs, gleiches Status-Schema
wie der Rest des Backlogs):

- **S1** — Transport-Security-Default (TLS/mTLS)
- **S2** — Broker-Authentifizierung (fail-closed Publish/Mutate)
- **S3** — XMI-Parser-Härtung (XXE / Entity-Expansion / SSRF über `href`)
- **S4** — LDAP-Filter-Validierung und Komplexitäts-Limit
- **S5** — Audit-Logging über `X-DDSR-Requestor` hinaus, Fehler-Leakage
- **S6** — `file:`-URI-Leak schließen (deckt sich mit W1)
- **S7** — Ressourcen-Limits (Body-Size, Rate, Connection-Cap)
- **S8** — Heartbeat/Consumer-ID an authentisierte Session binden
- **S9** — Code-Publisher: Artefakt-Signierung + Supply-Chain-Integrität

**Priorisierung für den nächsten Schritt:** S3 ist die einzige
**heute aktiv ausnutzbare** Schwachstelle im laufenden Code (der Broker
parst Client-XMI mit `Map.of()`-Defaults). S3 vor allem anderen, dann
S2/S1 als Fundament für jedes externe Deployment.

---

## 6. Referenzen

- [REQUIREMENTS.md](REQUIREMENTS.md) — `NFR-Security-*`, §7 Abgrenzung
- [OPEN_ISSUES.md](OPEN_ISSUES.md) — `S*`-Findings, W1
- [ARCHITECTURE.md](ARCHITECTURE.md) — Komponenten, Wire-Format
- **OWASP ASVS 5.0** — lokal: `/opt/git/OWASP-ASVS/5.0/docs_en/`
  (strukturiertes JSON/CSV/XML + PDF)
- **BSI „Stand der Technik"-Bibliothek (OSCAL)** — lokal:
  `/opt/git/BSI-Stand-der-Technik-Bibliothek/` — maßgeblich:
  `Anwenderkataloge/Grundschutz++/Grundschutz++-catalog.json` und
  `Anwenderkataloge/Mindeststandard-TLS/Mindeststandard-TLS-catalog.json`
</content>
</invoke>
