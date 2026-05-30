# Sell Ticket — Flow

> Le flow le plus critique du système. Toute vente passe par ce pipeline.  
> Domaine pivot : `core.sales` · Référence : `core/sales/DOMAIN_SALES.md`

---

## Vue d'ensemble

```
Vendeur POS
    │
    ├─ Preview ─────────────────── Validation read-only (pas de réservation)
    │
    ├─ Sell ──────────────────────── Transaction : validate → save → event
    │         ├─ ACCEPTED (201)   → afficher displayCode immédiatement
    │         ├─ PENDING_APPROVAL (202) → workflow admin
    │         └─ REJECTED         → aucun ticket créé
    │
    ├─ Cancel ───────────────────── Fenêtre 3 min après vente
    │
    └─ Post-sell ────────────────── ResultedEvent → Settlement → Payout claim
```

---

## Modèle ticket — 3 statuts indépendants

Un ticket porte **3 statuts séparés** qui évoluent de façon indépendante :

| Statut | Valeurs | Cycle |
|---|---|---|
| `TicketSaleStatus` | `SOLD` · `PENDING_APPROVAL` · `VOID` · `REJECTED` | Vente / annulation |
| `TicketResultStatus` | `NOT_RESULTED` · `WON` · `LOST` · `OVERRIDDEN` | Après tirage |
| `TicketSettlementStatus` | `NOT_SETTLED` · `PAYOUT_PENDING` · `NO_PAYOUT` · `SETTLED` · `PAID` · `REVERSED` | Paiement |

Un ticket `SOLD` + `NOT_RESULTED` = vendu, résultat inconnu.  
Un ticket `SOLD` + `WON` + `PAYOUT_PENDING` = gagnant, paiement à effectuer.

---

## Phase 1 — Preview (validation read-only)

```
POST /tenant/cashier/tickets/preview
  { terminalId, drawId, drawChannelId, currency, lines:[...] }
  → SaleAcceptanceEvaluator.evaluatePreview()
```

**Pas de write.** Pas de réservation d'exposition.

| Décision | Signification | Action mobile |
|---|---|---|
| `ACCEPTABLE` | Panier valide, vente autorisée | Activer "Vendre" |
| `REQUIRES_CHANGES` | Approbation requise (mise > autonomie) | Afficher `sellerInstruction`, modifier |
| `REJECTED_FINAL` | Tirage fermé, session invalide | Bloquer |

> ⚠ Preview ne réserve pas l'exposition. Un sell peut encore retourner `EXPOSURE_CHANGED` si une autre vente arrive entre preview et sell.

---

## Phase 2 — Vente (transaction)

```
POST /tenant/cashier/tickets/sell
Idempotency-Key: <uuid-v4>
{ terminalId, drawId, drawChannelId, currency, lines:[...] }
```

### Pipeline interne (`SellTicketCommandHandler`, `@TchTx`)

```
1. TicketSalePolicy.prepareSale()
   ├─ validateSession()         → session OPEN + outlet non bloqué
   ├─ DrawCutoffRule()          → now < draw.cutoffAt
   ├─ normalize + mergeDuplicates(lines)
   ├─ EvaluateLimitPolicyQuery  → notices (WARN / BLOCK)
   ├─ ResolveAutonomyPolicy     → BLOCK → PENDING_APPROVAL si autonomy.requireApprovalOnBlock
   └─ PricingCatalog.oddsFor()  → snapshot odds + potentialPayout

2. Selon outcome :
   ├─ BLOCK sans autonomy → 422 limitBlocked
   ├─ BLOCK avec autonomy → TicketSaleFactory.newPendingApprovalTicket()
   │                        → save → 202 ACCEPTED (notice APPROVAL_REQUIRED)
   └─ OK → TicketSaleFactory.newSoldTicket()
           → save → emit TicketPlacedEvent (AfterCommit)
           → 201 CREATED
```

### Branches de réponse

| HTTP | `outcome` | `TicketSaleStatus` | Event |
|---|---|---|---|
| 201 | `ACCEPTED` | `SOLD` | `TicketPlacedEvent` (AfterCommit) |
| 202 | `PENDING_APPROVAL` | `PENDING_APPROVAL` | aucun (émis à l'approve) |
| 422 | `REJECTED` | non créé | aucun |

### Règle idempotency

`Idempotency-Key` = UNE FOIS par panier.  
Si timeout → re-poster le **même payload avec la même clé** → réponse stockée rejouée.  
Si panier change → **générer une nouvelle clé**.

### Sur ACCEPTED : afficher `backup.displayCode` immédiatement

```json
"backup": {
  "displayCode": "40CP-JBMR",
  "verificationShortUrl": "https://app.tchalanet.com/ticket/40CP-JBMR",
  "shareableText": "Ticket Tchalanet...\nCode: 40CP-JBMR"
}
```

C'est la preuve offline client. Disponible avant print/send.

---

## Phase 3 — Approbation (PENDING_APPROVAL uniquement)

```
POST /tenant/tickets/{id}/approve   [TENANT_ADMIN, SUPER_ADMIN]
  → re-valide cutoff + session
  → Ticket: PENDING_APPROVAL → SOLD
  → TicketPlacedEvent publié

POST /tenant/tickets/{id}/reject    [TENANT_ADMIN, SUPER_ADMIN]
  → Ticket: PENDING_APPROVAL → REJECTED
  → aucun event
```

> ⚠ Si le draw a dépassé son cutoff entre la mise en attente et l'approbation → 409 Conflict.

---

## Phase 4 — Annulation (fenêtre 3 min)

```
POST /tenant/cashier/tickets/{id}/cancel
{ terminalId, reason }
→ Ticket: SOLD → VOID
→ TicketCancelledEvent (AfterCommit)
   → core.limitpolicy : libère l'exposition
   → core.session : décrémente les totaux
   → features.stats : met à jour les agrégats
```

**Fenêtre V1 : 3 minutes après la vente.**  
Au-delà → `REJECTED` + issue `CANCEL_WINDOW_EXPIRED`.

---

## Événements post-commit

| Event | Producteur | Consommateurs |
|---|---|---|
| `TicketPlacedEvent` | `SellTicketCommandHandler` · `ApproveTicketSaleCommandHandler` | `core.limitpolicy` (exposure), `core.session` (totaux), `core.ledger` (écriture comptable), `features.stats` (×2) |
| `TicketCancelledEvent` | `CancelSaleCommandHandler` | `core.limitpolicy`, `core.session`, `features.stats` |
| `TicketResultedEvent` | `RecordDrawTicketsResultCommandHandler` | `features.stats`, `core.sales` → settlement |
| `TicketWinningSettlementCreatedEvent` | `core.sales` | `core.payout` → `OpenPayoutClaimFromSettlementCommand` |

> `SalesLedgerListener` consomme `TicketPlacedEvent` en `@EventListener` synchrone (dans la TX) — anomalie connue, les exceptions sont loguées mais silencieusement ignorées.

---

## Phase 5 — Post-sell : résultat et settlement

Après le tirage :

```
DrawResultAppliedEvent
  → core.sales : RecordDrawTicketsResultCommandHandler
    → Évaluation de chaque ligne vs résultat
    → Ticket: NOT_RESULTED → WON / LOST
    → TicketResultedEvent publié

Si WON :
  → TicketWinningSettlementCreatedEvent
  → core.payout : OpenPayoutClaimFromSettlementCommand
  → PayoutClaim OPEN → vendeur peut payer

Si LOST :
  → TicketSettlementStatus: NO_PAYOUT
  → Cycle terminé
```

→ Voir flow complet : [settlement](./settlement.md) · [payout-field-flow](./payout-field-flow.md)

---

## Anomalies connues

| Anomalie | Impact | Statut |
|---|---|---|
| `SalesLedgerListener` synchrone (dans TX) | Exception ledger ignorée silencieusement | Connu |
| `outlet bloqué → 500` au lieu de `403/422` | Message d'erreur technique exposé | Connu |
| `PENDING_APPROVAL` : `outletId/agentId` null si session non retrouvée à l'approve | Données audit incomplètes | Connu |
| `DrawSalesGuardPort` = NoOp | Annulation draw sans vérification des tickets vendus | À corriger |
| Vérification "single gameCode per ticket" après save | Rollback nécessaire si lignes mixtes | Connu (MVP) |

---

## Domaines impliqués

| Domaine | Rôle dans le flow |
|---|---|
| `core.sales` | Pivot : commande, factory, persistence, events |
| `core.session` | Validation session ouverte |
| `core.outlet` | Vérification outlet non bloqué |
| `core.draw` | Vérification cutoff |
| `catalog.pricing` | Snapshot odds |
| `core.limitpolicy` | Évaluation seuils, application exposure post-event |
| `core.autonomy` | Décision BLOCK → PENDING_APPROVAL |
| `core.ledger` | Écriture comptable (event listener) |
| `core.payout` | Claim de paiement post-settlement |
| `features.stats` | Agrégats temps réel |

---

## Références

- Domaine : `core/sales/DOMAIN_SALES.md`
- Intégration mobile : `features.cashier/MOBILE_FLOW.md`
- Session POS requise : [session-opening](./session-opening.md)
- Settlement après tirage : [settlement](./settlement.md)
- Payout terrain : [payout-field-flow](./payout-field-flow.md)
- Vérification ticket public : [verify-ticket](./verify-ticket.md)
- Audit pipeline : `tchalanet-server/docs/audit/2026-04-26-sales-pipeline-audit.md`
