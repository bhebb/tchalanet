> Superseded by `consolidate-core-sales-cleanup`.
> Do not implement from this change directly; retained for audit/history.

## Why

L'audit `2026-04-26-sales-pipeline-audit.md` (§Pipeline C, §Pipeline D, top 5 #2 et #3) a identifié deux failles critiques dans la chaîne de settlement :

1. **Idempotence settlement absente** : `RecordDrawTicketsResultCommandHandler` n'écrit jamais dans la table `ticket_settlement` (créée par la migration `V9__core_ticket.sql` exactement pour ce besoin). `DrawResultedEventListener` n'utilise pas `ProcessedEventPort`. Une re-publication de `DrawResultAppliedEvent` (par exemple après un redémarrage Spring Batch ou un re-apply manuel) ré-itère sur tous les tickets `SOLD+NOT_RESULTED`. Ceux déjà passés en `WON/LOST` lèvent `IllegalStateException` via `Ticket.markResulted` (`requireSaleStatus(SOLD)` et `resultStatus != NOT_RESULTED`) — la boucle s'interrompt prématurément, laissant une partie des tickets dans un état incohérent.

2. **Override casse l'historique de paiement** : `Ticket.forceResult(payout, resultStatus, when)` réaffecte systématiquement `settlementStatus = UNSETTLED`. Un ticket déjà `SETTLED` (payout déjà exécuté par `core.payout`) ré-overridé revient à `UNSETTLED` sans trace ni garde. `core.payout.MarkTicketPayoutPaidCommandHandler` qui réagit à `TicketResultOverriddenEvent` (ou redéclenché manuellement) peut alors exécuter un second payout — risque de double paiement réel.

Ces deux failles touchent l'intégrité financière du domaine. Elles doivent être colmatées avant tout travail d'évolution sur le pipeline settlement.

## What Changes

- **[Idempotence settlement]** Introduire un nouveau port `TicketSettlementIdempotencyPort` dans `core.sales.application.port.out` :
  - `boolean tryMarkSettled(TenantId tenantId, TicketId ticketId, DrawResultId drawResultId)` — INSERT dans `ticket_settlement` ; retourne `false` si conflit UNIQUE.
  - `boolean isAlreadySettled(TenantId tenantId, TicketId ticketId, DrawResultId drawResultId)` — SELECT.
- **[Idempotence settlement]** `RecordDrawTicketsResultCommandHandler` :
  - Avant `markResulted` sur chaque ticket, appeler `tryMarkSettled(...)`. Si `false` → skip (déjà traité), incrémenter compteur `skipped`.
  - Sinon → procéder normalement.
  - Loguer le résumé `processed=, won=, lost=, skipped=`.
- **[Idempotence event]** `DrawResultedEventListener.onDrawResultApplied` :
  - Utiliser `ProcessedEventPort` (pattern projet) avec clé `(eventId, "RecordDrawTicketsResult")`.
  - Si déjà traité → log INFO + skip ; sinon → envoyer la commande puis marquer.
- **[Override safety]** `Ticket.forceResult(payout, resultStatus, when)` :
  - **REFUSE l'override si `settlementStatus == SETTLED`** — lever `IllegalStateException` (ou domain exception dédiée) avec message explicite. Pas de chemin silencieux qui repasse en `UNSETTLED`.
  - Pour le cas exceptionnel "réversion de payout déjà exécuté", introduire une commande dédiée `ReverseTicketSettlementCommand` (hors scope de ce change — tracking issue).
- **[Override safety]** `OverrideTicketResultCommandHandler` :
  - Avant `forceResult`, vérifier `ticket.settlementStatus`. Si SETTLED → `ProblemRest.conflict("Cannot override result of an already SETTLED ticket. Use ReverseSettlement workflow.")`.
- **[Cleanup `OverrideTicketResultCommand`]** Le champ `status: TicketStatus` est trompeur (seul `resultStatus` est lu). Le remplacer par `TicketResultStatus resultStatus` direct ; les champs `saleStatus` et `settlementStatus` du body web sont supprimés.
- **[Tests]** Couverture exhaustive idempotence + override.

## Capabilities

### New Capabilities

- `sales-ticket-settlement`: Définit le contrat d'idempotence du settlement post-tirage : utilisation obligatoire de la table `ticket_settlement` comme guard, propriétés du `ProcessedEventPort` sur le listener, comportement du handler face à un re-play (skip vs fail), et règles de transition autorisées (`Ticket.markResulted`, `Ticket.forceResult`, `Ticket.markPayoutPaid`) avec leurs gardes.

## Impact

- **Code créé** :
  - `core.sales.application.port.out.TicketSettlementIdempotencyPort`
  - `core.sales.infra.persistence.adapter.TicketSettlementIdempotencyJpaAdapter` (INSERT `ticket_settlement` avec catch `DataIntegrityViolationException` → false)
  - Tests unitaires + intégration (`@DataJpaTest` + Testcontainers Postgres pour vérifier la contrainte UNIQUE)
- **Code modifié** :
  - `Ticket.java` — `forceResult` lève si `SETTLED`
  - `OverrideTicketResultCommand.java` — champ `status` → `resultStatus`
  - `OverrideTicketResultRequest.java` (web) — adapter
  - `OverrideTicketResultCommandHandler.java` — guard SETTLED + utilisation du nouveau champ
  - `RecordDrawTicketsResultCommandHandler.java` — boucle idempotente avec compteur skip
  - `DrawResultedEventListener.java` — `ProcessedEventPort` ajouté
  - `TicketWebMapper.java` — adapter le mapping override
- **Code supprimé** :
  - `Ticket.updateSettlementStatus(...)` — mutator public sans invariant (plus utilisé après ce change ; vérification cross-domain) — _sortie hors scope si encore consommé ailleurs_
- **Migration DB** : aucune (table `ticket_settlement` déjà créée par `V9__core_ticket.sql`).
- **API** :
  - `OverrideTicketResultRequest` : champ `status` simplifié → `resultStatus`. **BREAKING (admin API)** — communiqué via CHANGELOG ; clients admin (web) à adapter.
  - Réponse `409 Conflict` ajoutée sur override d'un ticket SETTLED.
- **Tests** :
  - `RecordDrawTicketsResultCommandHandlerTest` — scénarios re-play (3 tickets dont 2 déjà settled → 1 traité, 2 skipped, batch ne crashe pas)
  - `OverrideTicketResultCommandHandlerTest` — scénario override sur SETTLED → 409
  - `TicketSettlementIdempotencyAdapterIT` — intégration Postgres pour valider UNIQUE
  - `DrawResultedEventListenerTest` — re-play même eventId → skip via `ProcessedEventPort`
- **Docs** :
  - `tchalanet-server/src/main/java/com/tchalanet/server/core/sales/DOMAIN_SALES.md` §6 et §9
  - `tchalanet-docs/docs/02-functional/flows/sell-ticket.md` §Override / §Settlement
  - `tchalanet-docs/docs/02-functional/flows/draw-execution.md` §Settle (lien vers idempotence)
- **Non scope** :
  - Refonte des 3 méthodes équivalentes `Ticket.settle/markAsPaid/markPayoutPaid` (traité dans `fix-sales-pipeline-audit-gaps`)
  - Workflow `ReverseTicketSettlement` (nouveau use case — change séparé à venir, juste tracé via TODO)
  - Suppression du champ `Ticket.updateSettlementStatus` (audit cross-domain à faire d'abord)
  - Migration `RecordDrawTicketsResultCommand` vers Spring Batch (hors scope ; cursor batch en DB suffit pour la taille actuelle)
