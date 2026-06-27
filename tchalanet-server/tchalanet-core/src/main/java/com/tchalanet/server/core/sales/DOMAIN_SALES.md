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
- Pricing owner (`catalog.pricing` et `core.pricing`) — sales résout puis snapshot les valeurs effectives.
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
  - `TicketSaleStatus` : `PENDING_APPROVAL`, `REJECTED`, `APPROVED`, `CANCELLED`, `VOIDED`
  - `TicketResultStatus` : `NOT_RESULTED`, `PENDING`, `WON`, `LOST`, `VOID`, `OVERRIDDEN`
  - `TicketSettlementStatus` : `NOT_SETTLED`, `PAYOUT_PENDING`, `SETTLED`, `NO_PAYOUT`, `PAID`, `REVERSED`
- Audit : `createdAt`, `updatedAt`, `resultedAt (nullable)`
- Approval : `approvalRequestId (ApprovalRequestId nullable)` — placeholder UUID aléatoire typé (`// TODO: integrate approval domain later`)
- Lignes : `List<TicketLine>` (>= 1)

**Invariants enforcés** (constructeur + state machine) :

- `lines` non vide.
- Si `resultStatus == NOT_RESULTED` → `winningAmount` et `resultedAt` doivent être `null` ; `settlementStatus` ne peut pas être `SETTLED`.
- Sinon → `winningAmount >= 0` et `resultedAt` non null.
- `voidTicket` autorisé uniquement depuis une vente annulable.
- `markResulted` exige `saleStatus == APPROVED`.
- `forceResult(payout, resultStatus, when)` refuse si `saleStatus == VOID`.
- `settle/markAsPaid/markPayoutPaid` exigent `resultStatus != NOT_RESULTED`.
- L'application d'un résultat officiel auto-settle les tickets : gagnants en `PAID`,
  perdants/no-payout en `NO_PAYOUT`. Le seller terminal ne fait pas d'action manuelle "paid" en V1.

**Invariants SQL miroirs** (`V9__core_ticket.sql`) :

- `chk_ticket_result_fields` (cohérence NOT_RESULTED ↔ winning_amount/resulted_at).
- `chk_ticket_settlement_requires_result` (SETTLED implique result_status WON/LOST/OVERRIDDEN).
- `uq_ticket_tenant_code (tenant_id, ticket_code)`, `uq_ticket_public_code (public_code)`.

### `TicketLine` (record `domain/model/TicketLine.java`)

- `gameCode (GameCode)`, `selection (String — déjà normalisé)`, `stake (scale 2)`, `oddsSnapshot (scale 4)`, `potentialPayout (scale 2)`, `betType`, `betOption (Short nullable)`.
- `betOption` est validé via `catalog.game.api.model.BetOption`: absent pour les 2D simples, requis pour Maryaj, Loto 3, Loto 4 et Loto 5.
- Invariants compact constructor : montants > 0, cohérence `potentialPayout == round(stake × oddsSnapshot, 2, HALF_UP)`.

### Snapshots financiers de vente

La vente fige les faits financiers au moment du ticket. Ces snapshots sont la source pour le
settlement et analytics; ils ne doivent pas être recalculés depuis la configuration courante.

| Snapshot | Source de résolution | Utilisation |
|---|---|---|
| `TicketLine.oddsSnapshot` | `ResolveSellerTerminalOddsQuery`: override seller-terminal puis tenant default, y compris pour les lignes gratuites de promotion | Calcul `potentialPayout` et gain après résultat |
| `TicketContext.sellerCommissionRateSnapshot` | taux effectif du seller terminal au moment de la vente | Audit/explanation |
| `TicketContext.sellerCommissionAmountSnapshot` | `stake * rate / 100`, arrondi scale 2 | Analytics commissions par jour/tirage/seller terminal |
| `TicketMoneyBreakdown.charges` | `SaleChargeCalculator` + promotions charge waiver | Ticket total, print, analytics frais |

La commission reste configurée en pourcentage, mais les stats additionnent le montant snapshoté.
Changer le taux tenant ou terminal n'altère jamais un ticket déjà vendu.

### Charges

`TicketCharge` porte `type`, `amount`, `paidBy` et éventuellement une promotion de waiver.

- `paidBy=BUYER` non waived : ajouté au total payé par le client.
- `paidBy=SELLER` : hors total client, coût absorbé par le seller terminal.
- `paidBy=TENANT` : hors total client, coût d'exploitation tenant.
- `waived` : montant original conservé pour audit/print/promotion, mais exclu du total client.

Analytics projette ces catégories séparément (`buyerCharges`, `sellerCharges`, `tenantCharges`,
`waivedCharges`). Le net tenant soustrait `tenantCharges`, pas les frais payés par le client.

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

| Query                                               | Handler                                   | Notes                                                                                      |
| --------------------------------------------------- | ----------------------------------------- | ------------------------------------------------------------------------------------------ |
| `ListTicketsQuery(filter, pageRequest)`             | `ListTicketsQueryHandler`                 | `TchPage<TicketSummaryView>` ; `filter.tenantId` toujours `null` (RLS) ; émet audit `LIST` |
| `GetTicketDetailsQuery(ticketId)`                   | `GetTicketDetailsQueryHandler`            | Pas d'audit (décision v1 read-one)                                                         |
| `VerifyPublicTicketQuery(publicCode, now)`          | `VerifyPublicTicketQueryHandler`          | Public ; visibilité via `SettingsCatalog` (default 14 j)                                   |
| `ListRecentTicketsForCashierQuery`                  | `ListRecentTicketsForCashierQueryHandler` | —                                                                                          |
| `GetTicketQrPngByPublicCodeQuery(publicCode, size)` | `GetTicketQrPngByPublicCodeQueryHandler`  | Public, cache HTTP 1h                                                                      |
| `ExportDailySalesQuery(from, to)`                   | `ExportDailySalesQueryHandler`            | CSV RLS-scoped                                                                             |
| `GetAgentDailySalesQuery(from, to)`                 | `GetAgentDailySalesQueryHandler`          | Aggregate JPQL                                                                             |

### Services applicatifs

- `TicketSalePolicy` (`domain/service/`) : orchestration sell (session, cutoff, limits, autonomy).
- `DrawCutoffRule` (`application/rule/`) : `requireBeforeCutoff(drawId)` via `GetDrawQuery`.
- `TicketLinePreparationService` (`application/service/`) : normalize + odds effectifs → `TicketLine[]`.
- `SaleChargeCalculator` (`application/service/`) : matérialise les frais de communication selon
  policy tenant et `ChargePaidBy`.
- `SaleMoneyCalculator` (`application/service/`) : `total = stake + buyer-facing non-waived charges`.
- `TicketSaleFactory` (`domain/service/`) : génération codes (`TicketNumberGeneratorPort`, `TicketPublicCodeGeneratorPort`) + factory `Ticket.sell()` ou `Ticket.pendingApproval()`.
- `TicketWinningCalculator` (`domain/service/`) : switch sur `BetType` ; résout `BetOption` pour les familles à options ; lit/derive les faits ordonnés depuis `DrawResultProjection` (`lot1/lot2/lot3/lot4`, paires dérivées, pick3/pick4 quand disponibles).

---

## 4. Ports

### Ports OUT (`application/port/out/`)

- `TicketReaderPort` : `findById`, `findByPublicCode`, `findWithLinesById`, `search`, `exportDailySalesCsv`, `listRecentForCashier`, `getAgentDailySales`.
- `TicketPrintReaderPort` : `findTicketPrintView` pour `features.receipt`, `features.cashier` et les flux de communication.
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
| `TicketPlacedEvent`           | `SellTicketCommandHandler`, `ApproveTicketSaleCommandHandler` | `tenantId, ticketId, drawId, drawChannelId, sellerTerminalId, sellerCommissionRate/Amount, money{stake,total,potentialPayout,charges[]}, lines[]` |
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
| sales → `platform.audit`      | API                      | Functional audit through `AuditApi`/`@AuditLog`, not direct handler or repository access                                       |
| sales → `core.ledger`         | Port-in direct           | `RecordLedgerFromSalesPort` (depuis `SalesLedgerListener`)                                                                     |
| sales → `core.accesscontrol`  | Annotation               | `@RequiresPermission`                                                                                                          |
| sales → `core.pricing`        | Query                    | `ResolveSellerTerminalOddsQuery` pour odds effectifs seller-terminal override → tenant default                                 |
| `core.pricing` → `catalog.pricing` | API                 | `PricingCatalog.oddsFor(...)` pour le tenant default                                                                           |
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
- **Print/receipt** : endpoints PDF, ESC/POS et QR sous `features.receipt`; `core.sales` fournit seulement la projection via `TicketPrintReaderPort`.

---

## 9. Analysis V1 (2026-05-05) — Flow Validation

### Critical Path Validation

✅ **SellTicketCommand flow**:

- `prepareSale()` → policy checks (limits, session, draw state)
- Breach outcome: BLOCK → PendingApprovalTicket, WARN → Success with warnings, OK → Sold
- Typed IDs enforced: TenantId, TicketId, TerminalId, DrawId, EventId, AgentId
- AfterCommit.run() publishes TicketPlacedEvent
- RLS filters by tenant_id (no raw UUID in queries)

✅ **RecordDrawTicketsResult flow**:

- Triggered by DrawResultAppliedEvent listener
- Calculate winning via TicketWinningCalculator
- Publish TicketResultedEvent per ticket
- Side-effect: Ledger + Payout queue

✅ **Override flow**:

- Admin force result change via OverrideTicketResultCommand
- Publish TicketResultOverriddenEvent
- Audit trail captured (reason, timestamp)

### Architecture Compliance

- ✅ Hexagonal: domain/application/infra separation
- ✅ CQRS: Command/Query handlers with @TchTx on writers
- ✅ Events: AfterCommit pattern for cross-domain effects
- ✅ RLS: Tenant filtering at DB level (PostgreSQL policies)
- ✅ Typed IDs: Never raw UUID outside persistence layer

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

- `TicketWinningCalculator` : option-aware pour Maryaj exact/revers, Loto 3 exact/box, Loto 4 exact/box/front/back, Loto 5 options 1/2/3.
- `SalesTicketAdminAdapter.refuseNewTickets/allowNewTickets` : no-op v1.
- `ApprovalRequestId.of(UUID.randomUUID())` non lié à un domaine d'approbation (TODO).
- `findTicketPrintView` : la locale est fournie par le contexte appelant, avec fallback `Locale.FRENCH`.
- Aucun scheduler `core.sales` (archivage/expiration non automatisé).

---

## 10. Intégration `core.offlinesync` — Promotion de ventes offline

> Source : [`core/offlinesync/DOMAIN_OFFLINESYNC.md`](../offlinesync/DOMAIN_OFFLINESYNC.md) — voir aussi `openspec/changes/add-offlinesync-module/`.

Quand un POS hors-réseau vend des tickets, `core.offlinesync` les valide techniquement (15 checks dont signature Ed25519, quota grant, lock code, hash canonique), puis publie un event self-contained vers `core.sales`.

### Flow promotion

```
core.offlinesync                                                core.sales
─────────────────                                              ─────────────
OfflineSubmissionTechValidatedEvent (via outbox)
  ├─ drawId (pinné device au moment de la vente)
  ├─ OfflineSubmissionTicketDraft (seller, terminal, outlet, session,
  │     device, soldAt, stake, lines[], payloadHash)
  └─ promotionAttemptId
                                                  → OfflineSubmissionPromotionEventListener
                                                       ├─ idempotence (ProcessedEventPort "sales.offline-promotion")
                                                       ├─ CreateTicketFromOfflineSubmissionCommand
                                                       │     ├─ ticketReader.findByOfflineSubmissionId (fast-path duplicate)
                                                       │     ├─ OfflineSubmissionToTicketMapper.toTicket
                                                       │     │     ├─ GetDrawByIdQuery(draft.drawId)
                                                       │     │     ├─ TicketCodes generation
                                                       │     │     ├─ TicketLine[] depuis LineSnapshots
                                                       │     │     └─ Ticket.place(POS_OFFLINE_SYNCED, requiresApproval=false)
                                                       │     ├─ ticketWriter.save
                                                       │     └─ DataIntegrityViolationException → DUPLICATE
                                                       └─ publish OfflineSubmissionProcessedEvent
                                                            (PROMOTED | BUSINESS_REJECTED | DUPLICATE)
```

### Composants sales touchés

- **`OfflineSubmissionToTicketMapper`** (`internal/application/service/offline/`) — convertit `OfflineSubmissionTicketDraft → Ticket` via `QueryBus.ask(GetDrawByIdQuery)`.
  - **Compromis v1** : `oddsSnapshot = 1`, `TicketMoneyBreakdown stake==total` (pas de fees offline), `Selection.displayLabel = selectionKey` brut.
- **`CreateTicketFromOfflineSubmissionCommandHandler`** (`internal/application/command/handler/offline/`) — `@TchTx`, catch `DataIntegrityViolationException` → DUPLICATE, catch `NoSuchElementException|EntityNotFoundException` → `BUSINESS_REJECTED("sales.offline.draw_not_resolved")`.
- **`OfflineSubmissionPromotionEventListener`** (`internal/infra/event/offline/`) — `@TransactionalEventListener(AFTER_COMMIT)`, idempotence via `ProcessedEventPort`, publie `OfflineSubmissionProcessedEvent` via `AfterCommit.run` (TODO sales-side outbox).
- **`TicketSaleChannel.POS_OFFLINE_SYNCED`** — channel forbidden de pending approval (décision prise en amont par offlinesync).
- **`OfflineSaleRef`** (`api/model/origin/`) — référence vers la submission offline + sync batch + device + code, attachée au ticket.
- **DB** : `sales_ticket.offline_submission_id` (uuid, nullable) + unique partiel `(tenant_id, offline_submission_id) WHERE offline_submission_id IS NOT NULL`.

### Invariants supplémentaires (offline)

- Un ticket dont `origin.channel == POS_OFFLINE_SYNCED` doit avoir `offlineSaleRef != null` et `requiresApproval == false`.
- Si `DataIntegrityViolationException` à la création : la submission a déjà été promue (idempotence DB) → outcome DUPLICATE, retourner le `ticketId` existant.
- Si `GetDrawByIdQuery` 404 (draw archivé / id invalide) : BUSINESS_REJECTED `sales.offline.draw_not_resolved` — l'admin peut investigate via `/admin/offline/submissions/{id}`.

### Risques connus (offline-side, cf. ROADMAP)

- Le mapper ne valide pas encore `clientSoldAt ≤ draw.cutoffAt` (ROADMAP R4) — un ticket peut être créé pour une draw dont le cutoff est dépassé.
- `OfflineSubmissionProcessedEvent` publié via `AfterCommit.run` direct, pas outbox sales-side (ROADMAP R2) — risque de perte si crash entre commit et publish.

---

## 11. SalePreparation & Maryaj gratuit automatique — SPÉCIFIÉ, non implémenté

> Source de vérité : `openspec/changes/maryaj-gratis-auto-selection-v1/`
> (proposal + design + tasks). Cette section décrit l'état cible ; mettre à
> jour le statut quand les slices sont livrées.

### Pourquoi

Le ticket final doit contenir exactement les numéros vus au preview (ligne
Maryaj gratuite auto-générée, régénérable avant confirmation). Persister des
lignes générées n'est plus une query → flux en 3 commands.

### Commands cibles

```text
PrepareSaleCommand
  paidLines -> EvaluatePromotionQuery -> FREE_GAME_LINE
  -> SelectionGenerationService -> persiste SalePreparation (DRAFT)
  -> SalePreparationView (preparationId + lignes finales)

RegenerateSalePreparationPromotionLineCommand
  DRAFT non expirée, ligne origin=PROMOTION uniquement,
  regenerableBeforeConfirm=true, count < maxRegenerationsBeforeConfirm (3),
  audit actor/session/terminal, remplacement de la sélection.

ConfirmPreparedSaleCommand
  payload = preparationId + idempotencyKey uniquement (jamais de lignes
  client). Persiste exactement les lignes préparées ; double confirm même
  idempotencyKey -> même ticket. Pas de réévaluation promotion au confirm.
```

`PreviewTicketSaleQuery` (stateless) reste pour le calcul pur.

### SalePreparation (rétention)

```text
TTL DRAFT 10 min ; statuts DRAFT/CONFIRMED/EXPIRED/CANCELLED ;
input_hash/cart_hash serveur ; expiration paresseuse + job périodique ;
purge EXPIRED/CANCELLED à 7 j ; CONFIRMED gardé 30 j ou jusqu'à
réconciliation ; index (tenant_id, status, expires_at).
```

### SelectionGenerationService (`core.sales`, jamais `core.promotion`) — ✅ IMPLÉMENTÉ (2026-06-09)

```text
internal/application/service/sell/generation/
  SelectionGenerationService (interface)
  DefaultSelectionGenerationService  (dispatch stratégie + canonicalisation SelectionApi)
  RandomSelectionGenerator           (SecureRandom ; paire 2D distincte pour MARRIAGE_2D2D)
api/model/selection/
  SelectionGenerationStrategy : RANDOM (V1) | LOW_EXPOSURE_RANDOM (refusée partout V1)
  SelectionGenerationPurpose  : PROMOTION_FREE_LINE | CASHIER_SUGGESTION
```

Les règles de forme viennent de `catalog.game.api` (`BetType.canonicalWidth`,
`BetOption`) + `core.selection` (`SelectionApi.canonicalize`) — pas de config
DB supplémentaire. Branché dans `PromotionSelectionResolver` : remplace
l'ancien générateur hash déterministe qui produisait un seul nombre 2D pour un
Maryaj (sélection invalide pour `MARRIAGE_2D2D` — bug latent corrigé).

### Ligne promotionnelle (snapshot TicketLine)

```text
gameCode=HT_MARYAJ_GRATUIT, origin=PROMOTION, pricingSource=PROMOTION,
selectionSource=AUTO_GENERATED, stakeAmount=0, lineTotal=0,
payoutBaseAmount=montant effet, promotionDecisionId.
```

Même avec `stakeAmount=0`, la ligne promotionnelle snapshot l'odd effectif via
`ResolveSellerTerminalOddsQuery`. Le montant de gain potentiel est donc
`payoutBaseAmount * oddsSnapshot`, et le settlement futur ne relit jamais les
odds courants.

Maryaj gratuit automatique = **online only** V1 (pas de ligne gratuite via
`core.offlinesync` tant que la préparation signée côté device n'existe pas).
