# Domaine `core.sales` — Tickets & Ventes

> Cycle de vie tickets : émission, approbation, annulation, settlement après tirage, override, vérification publique. Domaine critique argent (multi-tenant, RLS, Envers).

> Functional overview (MkDocs) : `tchalanet-docs/docs/02-functional/domains/sales.md`
> Flows associés : `tchalanet-docs/docs/02-functional/flows/sell-ticket.md`, `verify-ticket.md`, `claim-payout.md`, `draw-execution.md`
> Audit récent : `tchalanet-server/docs/audit/2026-04-26-sales-pipeline-audit.md`

---

## 1. Rôle du domaine

- Émettre, approuver, rejeter, annuler des tickets.
- Calculer le gain (`winningAmount`) après tirage et persister `RESULTED_*` (handler `RecordDrawTicketsResultCommandHandler`).
- Override admin du résultat (`OverrideTicketResultCommandHandler`, `@RequiresPermission`).
- Exposer la vérification publique d'un ticket par `publicCode` (sans authentification).
- Publier les events métier consommés par `core.limitpolicy`, `core.session`, `core.ledger`, `core.payout`, `features.stats`.

**Ce que le domaine ne fait pas**

- Ouverture/fermeture des draws (`core.draw`).
- Ingestion des résultats externes (`core.drawresult`).
- Évaluation des limites & autonomie (`core.limitpolicy`, `core.autonomy`) — sales **consomme** via `QueryBus` + service.
- Pricing (`catalog.pricing`).
- Écritures comptables (`core.ledger`) — sales émet l'event ; ledger consomme.
- Exécution du payout (`core.payout`) — sales expose `Ticket.markPayoutPaid()`, `core.payout` l'invoque via ses handlers.

---

## 2. Modèle & invariants

### Agrégat `Ticket` (`domain/model/Ticket.java`)

Classe mutable, multi-tenant (`BaseTenantEntity` côté JPA).

**Champs** :

- Identité : `id (TicketId)`, `tenantId`, `terminalId`, `sessionId (nullable — backfills)`, `drawId`, `ticketCode` (per-tenant unique), `publicCode` (NOT NULL en DB, globalement unique)
- Money : `currency`, `totalAmount`, `winningAmount (nullable)`
- Statuts (split status pattern, 3 enums distincts) :
  - `TicketSaleStatus` : `SOLD`, `PENDING_APPROVAL`, `VOID`, `REJECTED`
  - `TicketResultStatus` : `NOT_RESULTED`, `WON`, `LOST`, `OVERRIDDEN`
  - `TicketSettlementStatus` : `UNSETTLED`, `SETTLED`
- Audit : `createdAt`, `updatedAt`, `resultedAt (nullable)`
- Approval : `approvalRequestId (UUID nullable)` — placeholder UUID aléatoire (`// TODO: integrate approval domain later`)
- Lignes : `List<TicketLine>` (>= 1)

**Invariants enforcés** (constructeur + state machine) :

- `lines` non vide.
- Si `resultStatus == NOT_RESULTED` → `winningAmount` et `resultedAt` doivent être `null` ; `settlementStatus` ne peut pas être `SETTLED`.
- Sinon → `winningAmount >= 0` et `resultedAt` non null.
- `voidTicket` autorisé uniquement depuis `SOLD` ou `PENDING_APPROVAL`.
- `markResulted` exige `saleStatus == SOLD`.
- `forceResult(payout, resultStatus, when)` refuse si `saleStatus == VOID`.
- `settle/markAsPaid/markPayoutPaid` exigent `resultStatus != NOT_RESULTED`.

**Invariants SQL miroirs** (`V9__core_ticket.sql`) :

- `chk_ticket_result_fields` (cohérence NOT_RESULTED ↔ winning_amount/resulted_at).
- `chk_ticket_settlement_requires_result` (SETTLED implique result_status WON/LOST/OVERRIDDEN).
- `uq_ticket_tenant_code (tenant_id, ticket_code)`, `uq_ticket_public_code (public_code)`.

### `TicketLine` (record `domain/model/TicketLine.java`)

- `gameCode (GameCode)`, `selection (String — déjà normalisé)`, `stake (scale 2)`, `oddsSnapshot (scale 4)`, `potentialPayout (scale 2)`, `betType`, `betOption (Short nullable)`.
- Invariants compact constructor : montants > 0, `betOption` requis pour `LOTTO4_PATTERN`/`LOTTO5_PATTERN` (1..3), cohérence `potentialPayout == round(stake × oddsSnapshot, 2, HALF_UP)`.

### `TicketVerificationResult` (record exposé en public)

Champs : `ticketId, publicCode, saleStatus, resultStatus, settlementStatus, drawId, terminalMasked, createdAt, totalAmount, potentialTotalPayout, payoutStatus, outletName, outletAddress, lines[]`.
`payoutStatus` ∈ {`POTENTIAL_WIN`, `NO_PAYOUT`, `EXPIRED`}.

---

## 3. Use cases (commandes & queries)

### Commands (`application/command/`)

| Command                          | Handler                                              | Auth                                                                           | Notes                                                                                                                                             |
| -------------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SellTicketCommand`              | `SellTicketCommandHandler`                           | `@Secured CASHIER/ADMIN/SUPER_ADMIN` côté controller                           | Orchestration via `TicketSalePolicy` (session + cutoff + limits + autonomy + odds) ; outcome `SUCCESS / SUCCESS_WITH_WARNINGS / PENDING_APPROVAL` |
| `ApproveTicketSaleCommand`       | `ApproveTicketSaleCommandHandler`                    | `@Secured ADMIN/SUPER_ADMIN`                                                   | Re-valide cutoff + session ; transition `PENDING_APPROVAL → SOLD` ; publie `TicketPlacedEvent`                                                    |
| `RejectTicketSaleCommand`        | `RejectTicketSaleCommandHandler`                     | `@Secured ADMIN/SUPER_ADMIN`                                                   | `PENDING_APPROVAL → REJECTED` ; pas d'event                                                                                                       |
| `CancelSaleCommand`              | `CancelSaleCommandHandler`                           | `@Secured CASHIER/ADMIN/SUPER_ADMIN` ✅                                        | Évalue limites (`OperationType.CANCEL`) + autonomy ; `voidTicket` ; publie `TicketCancelledEvent`                                                 |
| `CancelTicketCommand`            | (mappé en `CancelSaleCommand` par `TicketWebMapper`) | idem                                                                           | Doublon de modèle                                                                                                                                 |
| `OverrideTicketResultCommand`    | `OverrideTicketResultCommandHandler`                 | `@Secured ADMIN/SUPER_ADMIN` + `@RequiresPermission("ticket.result.override")` | `forceResult(payout, resultStatus, when)` ; publie `TicketResultOverriddenEvent`                                                                  |
| `RecordDrawTicketsResultCommand` | `RecordDrawTicketsResultCommandHandler`              | (interne — déclenché par `DrawResultedEventListener`)                          | Cursor batch 250 ; calcul gain via `TicketWinningCalculator` ; publie `TicketResultedEvent` par ticket                                            |
| `ArchiveTicketsCommand`          | `ArchiveTicketsCommandHandler`                       | (interne — pas de scheduler câblé)                                             | Soft-delete via `archiveOldTickets(cutoff)`                                                                                                       |

Code mort/orphelin : `ExpireTicketsCommand` (sans handler), `ApprovePendingTicketSaleCommand` / `RejectPendingTicketSaleCommand` (sans handler localisé).

### Queries (`application/query/`)

| Query                                                  | Handler                                   | Notes                                                                                      |
| ------------------------------------------------------ | ----------------------------------------- | ------------------------------------------------------------------------------------------ |
| `ListTicketsQuery(filter, pageRequest)`                | `ListTicketsQueryHandler`                 | `TchPage<TicketSummaryView>` ; `filter.tenantId` toujours `null` (RLS) ; émet audit `LIST` |
| `GetTicketDetailsQuery(ticketId)`                      | `GetTicketDetailsQueryHandler`            | Pas d'audit (décision v1 read-one)                                                         |
| `VerifyPublicTicketQuery(publicCode, now)`             | `VerifyPublicTicketQueryHandler`          | Public ; visibilité via `SettingsCatalog` (default 14 j)                                   |
| `ListRecentTicketsForCashierQuery`                     | `ListRecentTicketsForCashierQueryHandler` | —                                                                                          |
| `GetTicketPrintEscPosQuery` / `GetTicketPrintPdfQuery` | handlers idem                             | Print binaire via `TicketReceiptFormatter`                                                 |
| `GetTicketQrPngByPublicCodeQuery(publicCode, size)`    | `GetTicketQrPngByPublicCodeQueryHandler`  | Public, cache HTTP 1h                                                                      |
| `ExportDailySalesQuery(from, to)`                      | `ExportDailySalesQueryHandler`            | CSV RLS-scoped                                                                             |
| `GetAgentDailySalesQuery(from, to)`                    | `GetAgentDailySalesQueryHandler`          | Aggregate JPQL                                                                             |

### Services applicatifs

- `TicketSalePolicy` (`domain/service/`) : orchestration sell (session, cutoff, limits, autonomy, odds).
- `DrawCutoffRule` (`application/rule/`) : `requireBeforeCutoff(drawId)` via `GetDrawQuery`.
- `TicketLinePreparationService` (`application/service/`) : normalize + merge + odds → `TicketLine[]`.
- `TicketSaleFactory` (`domain/service/`) : génération codes (`TicketNumberGeneratorPort`, `TicketPublicCodeGeneratorPort`) + factory `Ticket.sell()` ou `Ticket.pendingApproval()`.
- `TicketWinningCalculator` (`domain/service/`) : switch sur `BetType` ; lit `lot1/lot2/lot3/pick3/twoDigits` du `DrawResultMatchView`. ⚠ `LOTTO5_PATTERN` option 3 non supportée (MVP).

---

## 4. Ports

### Ports OUT (`application/port/out/`)

- `TicketReaderPort` : `findById`, `findByPublicCode`, `findWithLinesById`, `search`, `getTicketPrintView`, `exportDailySalesCsv`, `listRecentForCashier`, `getAgentDailySales`.
- `TicketWriterPort` (typo : double 't') : `save`, `archiveOldTickets`.
- `TicketSettlementPort` : `findNextBatchForDraw(drawId, cursor, limit)` — cursor keyset (`createdAt`, `id`).
- `TicketSettlementQueryPort` : `existsPendingByDrawId`, `countPendingByDrawId`.
- `TicketEventPublisherPort` : `publishTicketPlacedEvent` — défini mais usage via `DomainEventPublisher` direct.
- `TicketNumberGeneratorPort` ↔ `TimeBasedTicketNumberGenerator` (`TCK-YYMMDD-HHMMSS-XXXXXX-C`).
- `TicketPublicCodeGeneratorPort` ↔ `CrockfordPublicCodeGenerator` (12 chars Base32).
- `DrawResultViewPort` : `findById(drawResultId)` → `DrawResultMinimalView { lot1, lot2, lot3, pick3, twoDigits[] }`.

### Adapters (`infra/persistence/adapter/`)

- `JpaTicketRepositoryAdapter` (implémente Reader + Writer dans la même classe).
- `TicketSettlementJpaAdapter` (cursor batch).
- `TicketSettlementQueryJpaAdapter`.
- `DrawResultViewPortJdbcAdapter` (SQL JOIN `draw_result` + `result_slot` — cross-domain, voir audit).

---

## 5. Controllers HTTP

### `TicketController` — `/tenant/tickets`

- `POST /` (sell) — `@Secured CASHIER/ADMIN/SUPER_ADMIN`
- `POST /{id}/approve` — `@Secured ADMIN/SUPER_ADMIN`
- `POST /{id}/reject` — `@Secured ADMIN/SUPER_ADMIN`
- `GET /` (list filtré paginé) — `@Secured CASHIER/ADMIN/SUPER_ADMIN`
- `GET /{id}` (details) — `@Secured CASHIER/ADMIN/SUPER_ADMIN`
- `PATCH /{id}/cancel` — `@Secured CASHIER/ADMIN/SUPER_ADMIN` ✅ (corrigé 2026-04-26)
- `GET /{id}/print[.escpos|.pdf]` — `@Secured CASHIER/ADMIN/SUPER_ADMIN` ✅ (corrigé 2026-04-26)
- `PATCH /{id}/result/override` — `@Secured ADMIN/SUPER_ADMIN` + `@RequiresPermission("ticket.result.override")`

Toutes les réponses utilisent `ApiResponse<T>` sauf les endpoints de print binaire.

### `PublicTicketController` — `/public/tickets`

- `GET /verify/{publicCode}` — public ; retourne `ResponseEntity<ApiResponse<TicketVerificationResult>>` ✅ (corrigé 2026-04-26) ; headers `X-Robots-Tag: noindex, nofollow` + `Cache-Control: no-store` ; rate-limité par IP (10 req/s, burst 30) ✅ (ajouté 2026-04-26).
- `GET /qr/{publicCode}.png?size=280` — public ; `byte[]` PNG ; cache HTTP 1h.

---

## 6. Événements

### Publiés (`AfterCommit.run()` → `DomainEventPublisher`)

| Event                         | Producer (sales)                                              | Champs clés                                                                                                                        |
| ----------------------------- | ------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `TicketPlacedEvent`           | `SellTicketCommandHandler`, `ApproveTicketSaleCommandHandler` | `tenantId, ticketId, outletId, agentId, terminalId, sessionId, drawId, drawChannelId, gameCode, stakeCents, currencyCode, lines[]` |
| `TicketCancelledEvent`        | `CancelSaleCommandHandler`                                    | `tenantId, ticketId, terminalId, sessionId, performedBy (UUID brut), reason, totalStakeCents, currency, drawId`                    |
| `TicketResultedEvent`         | `RecordDrawTicketsResultCommandHandler`                       | `tenantId, ticketId, resultStatus, settlementStatus, totalPayout`                                                                  |
| `TicketResultOverriddenEvent` | `OverrideTicketResultCommandHandler`                          | `tenantId, ticketId, drawId, winningAmount, resultStatus, reason, performedBy (UUID brut)`                                         |
| `TicketPaidEvent`             | publié par `core.payout` (cross-domain)                       | —                                                                                                                                  |
| `TicketPaymentPendingEvent`   | usage non confirmé                                            | —                                                                                                                                  |

### Listeners (consume)

- `core.sales.application.event.DrawResultedEventListener` (`@TransactionalEventListener AFTER_COMMIT`) consomme `core.draw.domain.event.DrawResultAppliedEvent` → émet `RecordDrawTicketsResultCommand`. **Pas d'idempotence (`ProcessedEventPort` non utilisé).**
- `core.sales.infra.event.SalesLedgerListener` (`@EventListener` synchrone — anomalie : devrait être `AFTER_COMMIT`) consomme `TicketPlacedEvent` → appelle `RecordLedgerFromSalesPort.recordTicketSale(...)` (port-in `core.ledger`).

---

## 7. Intégrations cross-domain

| Direction                     | Type                     | Détail                                                                                                                         |
| ----------------------------- | ------------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| sales → `core.draw`           | Query + port + event     | `GetDrawQuery`, `DrawLookupPort`, `DrawChannelLabelResolver`, `DrawOccurrenceLabelResolver`, consomme `DrawResultAppliedEvent` |
| sales → `core.drawresult`     | SQL JOIN brut (anomalie) | `DrawResultViewPortJdbcAdapter` lit `draw_result` + `result_slot` directement                                                  |
| sales → `core.session`        | Port                     | `SalesSessionReaderPort`                                                                                                       |
| sales → `core.outlet`         | Port + impl              | `OutletReaderPort`, `OutletLookupPort`, `SessionLookupPort` ; expose `SalesTicketAdminPort` via bridge                         |
| sales → `core.terminal`       | Port                     | `TerminalReaderPort`                                                                                                           |
| sales → `core.address`        | Port                     | `AddressReaderPort`                                                                                                            |
| sales → `core.limitpolicy`    | Query                    | `EvaluateLimitPolicyQuery`                                                                                                     |
| sales → `core.autonomy`       | Service                  | `ResolveAutonomyPolicyService`                                                                                                 |
| sales → `core.audit`          | Handler direct           | `AuditLoggingCommandHandler` (instance, pas via bus)                                                                           |
| sales → `core.ledger`         | Port-in direct           | `RecordLedgerFromSalesPort` (depuis `SalesLedgerListener`)                                                                     |
| sales → `core.accesscontrol`  | Annotation               | `@RequiresPermission`                                                                                                          |
| sales → `catalog.pricing`     | API                      | `PricingCatalog.oddsFor(...)`                                                                                                  |
| sales → `catalog.settings`    | API                      | `SettingsCatalog.resolve(...)` (visibilité publique)                                                                           |
| sales → `catalog.drawchannel` | View                     | `DrawChannelView`                                                                                                              |
| `core.payout` → sales         | Ports + events           | `TicketReaderPort`, `TicketWriterPort`, `TicketPaidEvent`, `TicketPaymentPendingEvent`                                         |
| `core.limitpolicy` → sales    | Event                    | `TicketPlacedEvent`                                                                                                            |
| `core.session` → sales        | Event                    | `TicketPlacedEvent` (via `SalesSessionTotalsProjectionListener`)                                                               |
| `features.stats` → sales      | Event                    | `TicketPlacedEvent` (×2 listeners)                                                                                             |

---

## 8. Notes techniques

- **Multi-tenant strict** : `BaseTenantEntity` + RLS PostgreSQL. Aucun `WHERE tenant_id = ?` côté Java (les filtres sont systématiquement à `null` dans `JpaTicketRepositoryAdapter.search`).
- **Envers** : `@Audited` sur `TicketEntity` + `TicketLineEntity` + `pricing_odds`.
- **Codes ticket** : `ticketCode` interne (TCK-YYMMDD-HHMMSS-XXXXXX-C, check digit modulo 10) ; `publicCode` (12 chars Crockford Base32, `SecureRandom`). **Aucun retry sur collision** au save.
- **Idempotence** :
  - `ticket_settlement` (table prévue par migration) **non utilisée** par le code Java.
  - `DrawResultedEventListener` n'utilise pas `ProcessedEventPort`.
  - Settlement ré-appliqué → `IllegalStateException` au premier ticket `WON/LOST` (markResulted exige SOLD) → batch interrompu.
- **Timezone** : `TicketSalePolicy` et `CancelSaleCommandHandler` utilisent `ZoneId.systemDefault()` dans `LimitContext` (anomalie).
- **Cache** : aucun `@Cacheable` côté domaine ; QR PNG cacheable 1h en HTTP.
- **Print** : `Locale.FRENCH` hardcodé dans `JpaTicketRepositoryAdapter.getTicketPrintView`.

---

## 9. État connu — Anomalies & TODO (cf. audit 2026-04-26)

**P0 / Sécurité** ✅ RÉSOLU (2026-04-26 — `secure-sales-ticket-endpoints`)

- ~~`PATCH /tenant/tickets/{id}/cancel` et `GET /tenant/tickets/{id}/print*` sans `@Secured`~~ → `@Secured(CASHIER/ADMIN/SUPER_ADMIN)` ajouté sur les 4 endpoints. `securedEnabled = true` activé dans `SecurityConfig`.
- ~~`PublicTicketController.verify` retourne `ResponseEntity<?>` raw + aucun rate-limiting effectif~~ → retourne `ResponseEntity<ApiResponse<TicketVerificationResult>>` + rate-limit IP Bucket4j ajouté.

**P0 / Données**

- `Ticket.forceResult` réaffecte systématiquement `settlementStatus = UNSETTLED` — un ticket déjà SETTLED redevient UNSETTLED → risque de double payout.
- `RecordDrawTicketsResultCommandHandler` ne consigne rien dans `ticket_settlement` → idempotence rompue.

**P0 / Public verify**

- `payoutStatus` calculé sur `potentialPayout` (avant tirage) au lieu de `winningAmount` réel.
- `outletAddress.id`, `outletAddress.tenantId` exposés en clair (le `maskAddress` n'efface que les lignes d'adresse).
- Catch `Exception ignored` masque les erreurs de chargement.

**P1 / Architecture**

- `DrawResultViewPortJdbcAdapter` : SQL JOIN cross-domain bypassant les ports `core.drawresult.api`.
- `JpaTicketRepositoryAdapter` : adapter persistence dépendant de 3 autres domaines (`DrawLookupPort`, `OutletReaderPort`, `SalesSessionReaderPort`).
- `SalesLedgerListener` : `@EventListener` synchrone au lieu de `@TransactionalEventListener(AFTER_COMMIT)` ; appelle un port-in directement avec catch global qui masque les erreurs.

**P1 / Modélisation**

- `Ticket.updateSettlementStatus()` mutator public sans contrôle d'invariant.
- 3 méthodes équivalentes : `Ticket.settle()`, `markAsPaid()`, `markPayoutPaid()`.
- `Ticket.markPayoutPending()` no-op.
- 4 commandes pour 2 use cases approve/reject (2 orphelines : `Approve/RejectPendingTicketSaleCommand`).
- 2 commandes pour 1 use case cancel (`CancelSaleCommand` / `CancelTicketCommand`).
- `ExpireTicketsCommand` sans handler (code mort).
- `SellTicketRequest.tenantId/cashierId` body écrasés par contexte ; `sessionId` body jamais lu.
- `CancelTicketCommand.performedBy` et `OverrideTicketResultCommand.performedBy` en `UUID` brut.
- Typo `TicketWriterPort` (double 't').

**P2 / Comportements MVP**

- `TicketWinningCalculator` : `LOTTO5_PATTERN` option 3 hardcodé `false`.
- `SalesTicketAdminAdapter.refuseNewTickets/allowNewTickets` : no-op v1.
- `approvalRequestId = UUID.randomUUID()` non lié à un domaine d'approbation (TODO).
- `getTicketPrintView` : `Locale.FRENCH` hardcodé (TODO).
- Aucun scheduler `core.sales` (archivage/expiration non automatisé).
