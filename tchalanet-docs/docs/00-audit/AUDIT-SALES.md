# Audit : Sales

> Date : 2026-04-24  
> Scope : `core/sales/` · `core/limitpolicy/` · `core/session/` · `core/ledger/` · `core/payout/` · `features/tenantadmin/policies/`  
> Statut : 0 modification — rapport uniquement

---

## Ce qui existe (PRÉSENT / PARTIEL / ABSENT)

### SellTicketCommand + handler

**PRÉSENT**

- `SellTicketCommand` : record avec `tenantId`, `terminalId`, `cashierId` (UserId), `drawId`, `List<LineCommand>`, `currency`
- `SellTicketCommandHandler` : implémente le flux complet — `salePolicy.prepareSale()` → vérification limites → `BreachOutcome.BLOCK` → `PENDING_APPROVAL` ou `SOLD` → `ticketWriter.save()` → publication événement
- `TicketSaleFactory`, `TicketSalePolicy`, `TicketLinePreparationService` : présents et fonctionnels
- `ApproveTicketSaleCommandHandler` + `RejectTicketSaleCommandHandler` : présents (flux d'approbation)
- `CancelSaleCommandHandler` : présent
- `RecordDrawTicketsResultCommandHandler` : présent (application des résultats de tirage aux tickets)

### TicketEntity + TicketLineEntity

**PRÉSENT**

- `TicketEntity` (`@Entity`, table `ticket`) : champs complets — `terminalId`, `drawId`, `sessionId`, `ticketCode`, `publicCode`, `saleStatus`, `resultStatus`, `settlementStatus`, `currency`, `totalAmount`, `winningAmount`, `resultedAt`, `approvalRequestId`
- `TicketLineEntity` : présente, liée par `OneToMany`
- `TicketLine` (record domain) : `gameCode`, `selection`, `stake`, `oddsSnapshot`, `potentialPayout`, `betType`, `betOption`
- `Ticket` (domain aggregate) : machine à états complète — `SOLD`, `PENDING_APPROVAL`, `REJECTED`, `VOID` + résultats `WON/LOST/OVERRIDDEN` + `SETTLED/UNSETTLED`

### LimitPolicy + DrawExposure

**PRÉSENT**

- `LimitDefinitionJpaEntity` : `ruleKey`, `enabled`, `onBreach` (BLOCK/WARN), `params` (jsonb), `appliesTo` (jsonb)
- `LimitAssignmentJpaEntity` : `limitDefinitionId`, `targetType`, `targetId`, `enabled`, `startsAt`, `endsAt`
- `DrawExposureJpaEntity` : tracking temps réel — `drawId`, `scopeType`, `scopeId`, `betType`, `selectionKey`, `stakeTotal`, `salesCount`, `potentialPayoutTotal`
- `InProcessLimitEvaluationEngine` : moteur d'évaluation in-process
- `LimitPolicyRuntimeService` : service d'orchestration
- `EvaluateLimitPolicyQueryHandler` + `ApplyTicketExposureCommandHandler` : présents et câblés dans le flux de vente
- `UpsertLimitDefinitionCommandHandler` + `UpsertLimitAssignmentCommandHandler` : CRUD complet

### PosSession

**PRÉSENT**

- `PosSession` domain model : présent
- `PosSessionJpaEntity` + `PosSessionTotalsJpaEntity` : présents
- `OpenSessionCommandHandler` + `CloseSessionCommandHandler` : présents
- `PosSessionController` + `PosSessionTotalsController` : présents
- `PosSessionTotalsProjectionListener` : mise à jour des totaux sur événement

### Ledger

**PRÉSENT**

- `LedgerEntry` domain model + `LedgerEntryJpaEntity` : présents
- `LedgerEntryFactory` : fabrique les entrées selon le type (SALE, PAYOUT, DEPOSIT, WITHDRAW)
- `RecordTicketSaleLedgerCommandHandler` : présent — appelé via `RecordLedgerFromSalesPort`
- `RecordPayoutLedgerCommandHandler` : présent — appelé via `RecordLedgerFromPayoutPort`
- `GetLedgerBalanceQueryHandler` + `GetLedgerTransactionsQueryHandler` : présents

### Payout

**PRÉSENT**

- `Payout` domain model + `PayoutJpaEntity` : présents
- `RegisterPayoutCommandHandler` + `ExecutePayoutCommandHandler` : présents
- `ApprovePayoutCommandHandler` + `RejectPayoutCommandHandler` : présents
- `MarkTicketPayoutPaidCommandHandler` + `MarkTicketPayoutPendingCommandHandler` : présents
- `PayoutLedgerListener` : écoute `PayoutRegisteredEvent` → enregistre en ledger
- `PayoutAdminController` : présent (endpoints admin payout)

---

## Ce qui manque pour v1

| Item                                                                                                                                                                                                          | Criticité | Estimation |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- | ---------- |
| **Intégration domaine approbation** : `SellTicketCommandHandler` crée un `UUID.randomUUID()` comme `approvalRequestId` avec un `// TODO: integrate approval domain later`. Aucun domaine `approval` n'existe. | HAUTE     | **M**      |
| **Validation session obligatoire** : Le `SellTicketCommandHandler` dispose d'un commentaire `// If your business requires session to sell, enforce here` — la session n'est pas enforced                      | HAUTE     | **S**      |
| **AgentId absent de SellTicketCommand** : La référence au vendeur est `cashierId` (UserId). Si un `AgentId` distinct est nécessaire pour le tracking vendeur, il est absent du command                        | MOYENNE   | **S**      |
| **Endpoints limites tenant** : `TenantAdminPoliciesLimitsController` existe mais les endpoints ont besoin d'être validés end-to-end avec le moteur de limites                                                 | MOYENNE   | **M**      |
| **Tests end-to-end du flux complet** : `SellTicketCommandHandler` → Limites → Session → Ledger non testé de bout en bout                                                                                      | HAUTE     | **L**      |
| **Payout REST incomplet** : `PayoutAdminController` présent mais flux de claim/payout côté caissier non exposé (pas de `POST /tenant/payouts/claim`)                                                          | MOYENNE   | **M**      |

---

## Endpoints existants

| Méthode               | Path                                    | Statut                            | Rôles                       |
| --------------------- | --------------------------------------- | --------------------------------- | --------------------------- |
| `POST`                | `/tenant/tickets`                       | ✅ PRÉSENT                        | CASHIER, ADMIN, SUPER_ADMIN |
| `GET`                 | `/tenant/tickets`                       | ✅ PRÉSENT                        | CASHIER, ADMIN, SUPER_ADMIN |
| `GET`                 | `/tenant/tickets/{id}`                  | ✅ PRÉSENT                        | CASHIER, ADMIN, SUPER_ADMIN |
| `PATCH`               | `/tenant/tickets/{id}/cancel`           | ✅ PRÉSENT                        | —                           |
| `POST`                | `/tenant/tickets/{id}/approve`          | ✅ PRÉSENT                        | ADMIN, SUPER_ADMIN          |
| `POST`                | `/tenant/tickets/{id}/reject`           | ✅ PRÉSENT                        | ADMIN, SUPER_ADMIN          |
| `PATCH`               | `/tenant/tickets/{id}/result/override`  | ✅ PRÉSENT                        | ADMIN, SUPER_ADMIN          |
| `GET`                 | `/tenant/tickets/{id}/print`            | ✅ PRÉSENT                        | —                           |
| `GET`                 | `/tenant/tickets/{id}/print.pdf`        | ✅ PRÉSENT                        | —                           |
| `GET`                 | `/tenant/tickets/{id}/print.escpos`     | ✅ PRÉSENT                        | —                           |
| `GET`/`POST`/`DELETE` | `/admin/tenant/policies/limits/...`     | ⚠️ PARTIEL                        | TENANT_ADMIN                |
| `POST`                | `/tenant/payouts/claim` (cashier claim) | ❌ ABSENT                         | —                           |
| `GET`                 | `/tenant/sessions/current`              | ✅ PRÉSENT (PosSessionController) | —                           |

---

## Violations conventions

| Fichier                     | Violation                                                                                              | Sévérité                                                   |
| --------------------------- | ------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------- |
| `TicketController.sell()`   | Retourne `ResponseEntity<ApiResponse<TicketResponse>>` au lieu de `ApiResponse<T>` + `@ResponseStatus` | ⚠️ MINEURE — justifiée (contrôle de 201 vs 202)            |
| `TicketController.print()`  | Retourne `ResponseEntity<String>`                                                                      | ⚠️ MINEURE — justifiée (contrôle Content-Type)             |
| `PublicTicketController`    | `ResponseEntity<?>` avec `.status(404).build()` au lieu d'exception `ResponseStatusException`          | 🔴 VIOLATION — convention = exceptions, pas ResponseEntity |
| `LimitPolicyRuntimeService` | `@Autowired(required = false)` ×2 — field injection interdit                                           | 🔴 VIOLATION — constructor injection requis                |
| `SellTicketCommandHandler`  | `approvalRequestId = UUID.randomUUID()` — TODO non implémenté, pas de domaine approbation              | ⚠️ DETTE TECHNIQUE                                         |
| `SellTicketCommandHandler`  | Session non enforced — choix de conception non documenté (pas d'ADR)                                   | ⚠️ DETTE                                                   |
| `Terminal.id` (domain)      | Typé `UUID` au lieu de `TerminalId` dans le modèle de domaine                                          | ⚠️ VIOLATION typed-ids                                     |

---

## Tests existants

| Fichier                            | Type | Ce qu'il teste                                       |
| ---------------------------------- | ---- | ---------------------------------------------------- |
| `DrawCutoffRuleTest`               | Unit | Règle cutoff tirage (refus vente après cutoff)       |
| `TicketLinePreparationServiceTest` | Unit | Préparation des lignes de ticket (calcul stake/odds) |
| `ApplySaleExposureHandlerTest`     | Unit | Handler exposition limits                            |

**Couverture globale** : ~15% — très insuffisante pour v1. Pas de test d'intégration ni de test end-to-end du flux de vente complet.

---

## Dépendances vers autres domaines

```
Sales dépend de :
  ├── CATALOG/pricing     → PricingOddsEntity (odds snapshot au moment de la vente)
  ├── CATALOG/drawchannel → DrawChannelCatalog (validation du tirage, cutoff check)
  ├── CORE/tenantconfig   → TenantId (résolution tenant)
  ├── CORE/pos (Terminal) → TerminalId (terminal associé au ticket)
  ├── CORE/session        → PosSession (session caissier — optionnel mais recommandé)
  ├── CORE/limitpolicy    → LimitPolicyRuntimeService (vérification avant vente)
  ├── CORE/ledger         → RecordLedgerFromSalesPort (écriture comptable after-commit)
  └── CORE/payout         → MarkTicketPayoutPaidCommandHandler (cycle de vie paiement)
```

**Blocants v1** :

- Payout claim UI/API manquant
- Intégration approval domain manquante (actuellement UUID stub)
- Session non enforced (risque de tickets orphelins)

---

## Estimation gaps : S / M / L par item manquant

| Gap                                                          | Taille |
| ------------------------------------------------------------ | ------ |
| Enforcer validation session dans `SellTicketCommandHandler`  | **S**  |
| Implémenter endpoint claim payout cashier                    | **M**  |
| Connecter domaine approval (ou supprimer le stub)            | **M**  |
| Tests end-to-end flux vente + limites                        | **L**  |
| Corriger violations conventions (ResponseEntity, @Autowired) | **S**  |
