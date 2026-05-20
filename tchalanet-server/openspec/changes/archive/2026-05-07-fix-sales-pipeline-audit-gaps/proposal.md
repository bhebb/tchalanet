> Superseded by `consolidate-core-sales-cleanup`.
> Do not implement from this change directly; retained for audit/history.

## Why

L'audit `2026-04-26-sales-pipeline-audit.md` a révélé, au-delà des chantiers P0 traités séparément (`secure-sales-ticket-endpoints`, `harden-ticket-settlement-integrity`, `harden-public-ticket-verification`), un ensemble d'anomalies P1/P2 qui dégradent la maintenabilité, la cohérence des couches, et l'observabilité du pipeline sales :

- **Violations d'isolation** : `DrawResultViewPortJdbcAdapter` exécute un SQL JOIN cross-domain (`draw_result` + `result_slot`) depuis `core.sales`, bypassant les ports `core.drawresult.api`. `JpaTicketRepositoryAdapter` (couche infra/persistence) consomme 3 ports cross-domain (`DrawLookupPort`, `OutletReaderPort`, `PosSessionReaderPort`) pour assembler `TicketPrintView`.
- **Listener convention drift** : `SalesLedgerListener` utilise `@EventListener` synchrone (in-tx) au lieu de `@TransactionalEventListener(AFTER_COMMIT)`, et appelle directement un port-in de `core.ledger` avec un catch global qui masque les erreurs.
- **Doublons de modèle** : `CancelSaleCommand` vs `CancelTicketCommand` ; `Approve/RejectTicketSaleCommand` (actifs) vs `Approve/RejectPendingTicketSaleCommand` (orphelins sans handler). 3 méthodes équivalentes `Ticket.settle/markAsPaid/markPayoutPaid`. `markPayoutPending` no-op.
- **Surface API trompeuse** : `SellTicketRequest` body contient `tenantId/cashierId` silencieusement écrasés par contexte ; `sessionId` body jamais lu. `CancelTicketCommand.performedBy` et `OverrideTicketResultCommand.performedBy` en `UUID` brut au lieu de `UserId`.
- **Code mort** : `ExpireTicketsCommand` (record vide sans handler), `ApprovePendingTicketSaleCommand`, `RejectPendingTicketSaleCommand`. `TicketEventPublisherPort` défini mais publication via `DomainEventPublisher` direct.
- **Conventions** : typo `TicketWritterPort` (double 't'). `Ticket.updateSettlementStatus` mutator public sans contrôle d'invariant.
- **Timezone leak** : `TicketSalePolicy.evaluateLimitsAndAutonomy` et `CancelSaleCommandHandler.evaluateCancelLimits` utilisent `ZoneId.systemDefault()`.
- **Comportement MVP non documenté** : `TicketWinningCalculator` `LOTTO5_PATTERN` option 3 hardcodé `false` (silent fail).
- **Discordance JPA ↔ migration** : `TicketEntity.publicCode` annoté nullable JPA mais `NOT NULL` en migration → faille latente.
- **Locale hardcodée** : `JpaTicketRepositoryAdapter.getTicketPrintView` utilise `Locale.FRENCH`.
- **Aucun scheduler `core.sales`** : `ArchiveTicketsCommand` est un handler orphelin (jamais déclenché).
- **Pas de retry collision** : `ticketCode` / `publicCode` aléatoires sans logique de retry sur conflit DB.
- **Locale `getTicketPrintView` hardcodée** + adapter persistence orchestrant 3 domaines (cf. ci-dessus).

Sans ce ménage, la cartographie domain/application/infra reste floue et les nouvelles features héritent des dérives existantes.

## What Changes

- **[Cross-domain SQL leak]** Introduire `core.drawresult.api.DrawResultProjection` (record exposant `lot1, lot2, lot3, pick3, twoDigits, slotKey, occurredAt`) ; nouveau port `core.drawresult.api.DrawResultProjectionCatalog` avec `findById(DrawResultId)`. `core.sales.application.port.out.DrawResultViewPort` → consume `DrawResultProjectionCatalog` au lieu d'exécuter du SQL JOIN. Suppression de `DrawResultViewPortJdbcAdapter`.
- **[Persistence layer pollution]** `JpaTicketRepositoryAdapter.getTicketPrintView` extrait dans un nouveau `TicketPrintViewAssembler` (`core.sales.application.service`) qui orchestre les 3 lookups cross-domain. L'adapter persistence ne garde que les opérations CRUD pures sur `Ticket`. `TicketReaderPort.getTicketPrintView` migre dans `application.service` (ou un port dédié si besoin).
- **[Listener convention]** `SalesLedgerListener` :
  - Migrer `@EventListener` → `@TransactionalEventListener(phase = AFTER_COMMIT)`.
  - Migrer `core.ledger.application.port.in.RecordLedgerFromSalesPort` (port-in direct) → `CommandBus.send(RecordTicketSaleLedgerCommand)`.
  - Catch ciblé (laisse remonter les exceptions inattendues) ; log ERROR + metric `tch_sales_ledger_publish_failure_total`.
- **[Cancel command unification]** Supprimer `CancelTicketCommand` ; `TicketWebMapper` construit directement `CancelSaleCommand`. Adapter `CancelTicketRequest` si nécessaire (renommer ou réutiliser).
- **[Approve/Reject orphans]** Supprimer `ApprovePendingTicketSaleCommand` et `RejectPendingTicketSaleCommand` (sans handler).
- **[ExpireTicketsCommand dead code]** Supprimer `ExpireTicketsCommand` (record vide sans handler).
- **[Ticket settle aliases]** Conserver une seule méthode `Ticket.settle(when)` ; supprimer `markAsPaid`, `markPayoutPaid`, `markPayoutPending`. Adapter les consommateurs (`core.payout.MarkTicketPayoutPaidCommandHandler` notamment).
- **[Ticket invariant breach]** Supprimer `Ticket.updateSettlementStatus(...)` ; vérifier qu'aucun consommateur ne s'en sert.
- **[Sell request cleanup]** `SellTicketRequest` :
  - Supprimer les champs `tenantId`, `sessionId`, `cashierId` du body.
  - Le `TicketWebMapper` continue de les résoudre depuis le contexte (`TchContext`).
  - **BREAKING (tenant API)** — coordination POS/web requise.
- **[Typed IDs `performedBy`]** :
  - `CancelTicketCommand.performedBy: UUID` → `UserId` (et idem pour `CancelSaleCommand` si pas déjà typé)
  - `OverrideTicketResultCommand.performedBy: UUID` → `UserId`
  - `TicketCancelledEvent.performedBy: UUID` → `UserId`
  - `TicketResultOverriddenEvent.performedBy: UUID` → `UserId`
- **[Port typo]** Renommer `TicketWritterPort` → `TicketWriterPort`. Adapter tous les consommateurs.
- **[Port unused]** Supprimer `TicketEventPublisherPort` (publication via `DomainEventPublisher` directement utilisée partout).
- **[Timezone leak]** `TicketSalePolicy.evaluateLimitsAndAutonomy` et `CancelSaleCommandHandler.evaluateCancelLimits` :
  - Récupérer la timezone depuis `Draw.drawChannel.timezone()` (déjà résolu).
  - Plus de `ZoneId.systemDefault()`.
- **[LOTTO5 option 3 silent fail]** `TicketWinningCalculator.lotto5` :
  - Soit implémenter la règle correcte (à clarifier avec produit) — option recommandée.
  - Soit, à défaut, lever `UnsupportedOperationException` au lieu de retourner `false` silencieusement.
- **[JPA ↔ migration sync]** `TicketEntity.publicCode` annoter `nullable = false` (cohérent avec `V9__core_ticket.sql`).
- **[Locale dynamique]** `JpaTicketRepositoryAdapter.getTicketPrintView` (post-extraction) → recevoir `Locale` en paramètre depuis le contexte web (`Accept-Language` ou `TchContext`).
- **[Archive scheduler]** `core.sales.infra.scheduler.TicketArchiveScheduler` :
  - Cron `0 0 3 * * *` UTC (3h00 nightly).
  - `@SchedulerLock` (cf. capability `scheduler-distributed-locking` créée dans `fix-draw-pipeline-audit-gaps`).
  - Cutoff configurable via `tch.sales.archive.retention-days` (default 90).
  - Envoie `ArchiveTicketsCommand`.
- **[Code retry collision]** `JpaTicketRepositoryAdapter.save(Ticket)` :
  - Catch `DataIntegrityViolationException` sur conflit `uq_ticket_tenant_code` ou `uq_ticket_public_code`.
  - Retry avec nouveaux codes (max 3 tentatives) ; au-delà, lever `TicketCodeGenerationException` (mappée 503 Service Unavailable).

## Capabilities

### New Capabilities

- `sales-ticket-lifecycle`: Définit l'agrégat `Ticket` et sa state machine canonique : 4 statuts vente (`SOLD/PENDING_APPROVAL/VOID/REJECTED`), 4 statuts résultat (`NOT_RESULTED/WON/LOST/OVERRIDDEN`), 2 statuts settlement (`UNSETTLED/SETTLED`), transitions autorisées (table), méthodes domaine `sell/approve/reject/void/markResulted/forceResult/settle`, invariants enforcés (`@Audited`, RLS, code unique per-tenant), commandes uniques par use case (pas de doublons), génération du `publicCode` Crockford 12 chars + retry collision (3 tentatives).
- `sales-event-publishing`: Définit la convention de publication des events `core.sales` : tous via `AfterCommit.run() + DomainEventPublisher` (pas de `TicketEventPublisherPort`), listeners cross-domain via `@TransactionalEventListener(AFTER_COMMIT)`, communication cross-domain via `CommandBus.send` (pas de port-in direct), gestion d'erreurs avec catch ciblé + log ERROR + metric.

### Modified Capabilities

- `sales-ticket-settlement` (créée par `harden-ticket-settlement-integrity`) : étendue avec la suppression des aliases `Ticket.markAsPaid/markPayoutPaid/markPayoutPending` au profit d'une seule méthode `settle(when)`.

## Impact

- **Code modifié** : `JpaTicketRepositoryAdapter`, `TicketEntity`, `Ticket` (suppression méthodes), `TicketWebMapper`, `SellTicketRequest`, `OverrideTicketResultCommand`, `CancelTicketCommand`, `CancelSaleCommand`, `TicketCancelledEvent`, `TicketResultOverriddenEvent`, `TicketSalePolicy`, `CancelSaleCommandHandler`, `SalesLedgerListener`, `core.payout.MarkTicketPayoutPaidCommandHandler` (consomme nouveau `Ticket.settle`), `TicketWinningCalculator`, tous les consommateurs de `TicketWritterPort` (rename), `DrawResultViewPort`
- **Code créé** :
  - `core.drawresult.api.DrawResultProjection` (record)
  - `core.drawresult.api.DrawResultProjectionCatalog` (interface)
  - `core.drawresult.internal.read.DrawResultProjectionCatalogImpl` (implémentation)
  - `core.sales.application.service.TicketPrintViewAssembler`
  - `core.sales.infra.scheduler.TicketArchiveScheduler`
  - `core.sales.config.TicketArchiveProperties`
  - `core.sales.application.command.model.TicketCodeGenerationException`
  - `core.ledger.application.command.model.RecordTicketSaleLedgerCommand` (port-in → command + handler)
  - Tests + ArchUnit pour interdire SQL JOIN cross-domain depuis adapters
- **Code supprimé** :
  - `DrawResultViewPortJdbcAdapter` (remplacé par appel `DrawResultProjectionCatalog`)
  - `CancelTicketCommand` (remplacé par `CancelSaleCommand`)
  - `ExpireTicketsCommand`
  - `ApprovePendingTicketSaleCommand`, `RejectPendingTicketSaleCommand`
  - `Ticket.markAsPaid`, `Ticket.markPayoutPaid`, `Ticket.markPayoutPending`
  - `Ticket.updateSettlementStatus`
  - `TicketEventPublisherPort`
  - `core.ledger.application.port.in.RecordLedgerFromSalesPort` (remplacé par command)
  - Champs body `tenantId/sessionId/cashierId` de `SellTicketRequest`
- **Renommages** :
  - `TicketWritterPort` → `TicketWriterPort` (et adapter)
  - `core.sales.application.command.model.SellTicketCommand.cashierId` reste mais documenté qu'il est dérivé du contexte
- **API** :
  - **BREAKING (tenant API)** : `SellTicketRequest` perd 3 champs body
  - **BREAKING (admin API)** : `CancelTicketRequest` (si renommé)
- **Migration DB** : aucune
- **Configuration** : `tch.sales.archive.{enabled,retention-days,cron}` (defaults documentés)
- **Tests** :
  - Tests unitaires pour le nouveau scheduler (mock `CommandBus`)
  - Tests intégration (`@SpringBootTest`) pour le retry collision
  - ArchUnit `SalesIsolationArchTest` : aucun fichier dans `core.sales.infra.persistence.adapter.*` ne doit exécuter de SQL référençant des tables hors `core.sales` (`ticket`, `ticket_line`, `ticket_settlement`, `pricing_odds`)
- **Docs** :
  - `tchalanet-server/src/main/java/com/tchalanet/server/core/sales/DOMAIN_SALES.md` — refonte §3, §4, §6, §7, §8, §9
  - `tchalanet-docs/docs/02-functional/flows/sell-ticket.md` — mise à jour du body request
  - `docs/conventions/inter_domain_calls.md` — mention de la nouvelle règle "no cross-domain SQL JOIN in adapters"
- **Non scope** :
  - Refonte du modèle de pricing (`catalog.pricing`)
  - Migration vers `@PreAuthorize` (cohérence avec autres méthodes du contrôleur)
  - Implémentation du workflow `ReverseTicketSettlement` (change séparé)
  - Audit cross-domain de `Ticket.updateSettlementStatus` consommateurs si > 0 (nécessite un follow-up dédié si trouvé)
