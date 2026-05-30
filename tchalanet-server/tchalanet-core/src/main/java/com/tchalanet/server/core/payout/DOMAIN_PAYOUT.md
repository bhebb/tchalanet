# Domaine `core.payout` — Claims & Payments

> Gère le cycle de vie des demandes de gain (PayoutClaim) : ouverture depuis le settlement, approbation, exécution du paiement, blocage, annulation, reversement.

> Functional overview (MkDocs) : `tchalanet-docs/docs/02-functional/domains/payout.md`
> Flow associé : `tchalanet-docs/docs/02-functional/flows/payout-field-flow.md`

---

## 1. Rôle du domaine

**Ce que le domaine fait**

- Ouvrir des `PayoutClaim` pour les tickets SETTLED (déclenché par `TicketWinningSettlementCreatedEvent`).
- Exposer le workflow de paiement : approbation, exécution (PAID), blocage, déblocage, annulation, reversement.
- Calculer les montants dus et tracker les acteurs (paidBy, blockedBy, cancelledBy, reversedBy).
- Exposer les queries session summary et réconciliation draw.

**Ce que le domaine ne fait pas**

- Émission de tickets (`core.sales`).
- Écritures comptables grand livre (`core.ledger`).
- Vérification des tickets (`core.sales.VerifyPublicTicketQuery`).

---

## 2. Enums

### `PayoutClaimStatus`

| Valeur | Transition | Sens |
|---|---|---|
| `OPEN` | initial | Claim ouvert, en attente de traitement |
| `BLOCKED` | depuis OPEN | Blocage temporaire (investigation, fraude suspectée) |
| `PAID` | depuis OPEN ou APPROVED | Paiement exécuté |
| `CANCELLED` | depuis OPEN/BLOCKED | Annulé (ticket invalide, correction admin) |
| `REVERSED` | depuis PAID | Reversement post-paiement |

> Pas de `PARTIALLY_PAID` — un claim est atomic (un ticket = un claim = un paiement).

### `PayoutStatus` (statut interne du payout individuel)

| Valeur | Sens |
|---|---|
| `REQUESTED` | Paiement demandé, pas encore approuvé |
| `APPROVED` | Approuvé manuellement ou auto-approved via entitlement |
| `REJECTED` | Refusé |
| `PAID` | Payé |
| `CANCELLED` | Annulé |

### `PayoutClaimSource`

| Valeur | Sens |
|---|---|
| `SALES_SETTLEMENT` | Ouvert automatiquement depuis `TicketWinningSettlementCreatedEvent` |
| `OPS_RECONCILIATION` | Créé manuellement par ops lors d'une réconciliation |
| `MANUAL_ADMIN_CORRECTION` | Correction admin manuelle |

### `RegisterPayoutStatus` (résultat du flow exécution)

| Valeur | Sens |
|---|---|
| `PAID` | Paiement exécuté immédiatement |
| `REQUESTED` | Paiement en attente d'approbation |
| `BLOCKED` | Paiement bloqué (limite auto-approve dépassée) |

---

## 3. Commandes (`application/command/`)

| Commande | Sens |
|---|---|
| `OpenPayoutClaimFromSettlementCommand` | Ouvre un claim depuis un event settlement (idempotent via `sourceEventId`) |
| `ExecutePayoutCommand` | Exécute le paiement (transition OPEN/APPROVED → PAID) |
| `ApprovePayoutCommand` | Approuve manuellement un paiement |
| `RejectPayoutCommand` | Rejette un paiement |
| `BlockPayoutClaimCommand` | Bloque un claim (blockReason obligatoire) |
| `UnblockPayoutClaimCommand` | Débloque un claim BLOCKED |
| `CancelPayoutClaimCommand` | Annule un claim OPEN ou BLOCKED |
| `RegisterPayoutCommand` | Enregistre un paiement externe (réconciliation) |
| `ReversePayoutPaymentCommand` | Reverse un paiement PAID |

---

## 4. Queries (`application/query/`)

### Queries opérationnelles

| Query | Résultat |
|---|---|
| `GetPayoutDetailsQuery` | `PayoutDetails` — détails complets d'un claim |
| `ListPayoutsQuery` | `TchPage<PayoutRow>` — liste paginée |
| `GetPayoutSummaryBySessionQuery` | `PayoutSessionSummary` — totaux payouts d'une session |
| `GetPayoutReceiptQuery` | `PayoutReceiptView` — receipt pour impression |

### Queries réconciliation

| Query | Sens |
|---|---|
| `GetPayoutSummaryForDrawQuery` | Résumé agrégé payouts pour un draw |
| `ListPayoutClaimsForDrawQuery` | Tous les claims d'un draw (`PayoutClaimForDrawRow`) |
| `ListPayoutPaymentsForDrawQuery` | Tous les payments d'un draw (`PayoutPaymentForDrawRow`) |

---

## 5. Modèles de lecture

### `PayoutDetails`

| Champ | Type | Sens |
|---|---|---|
| `id` | `PayoutId` | — |
| `ticketId` | `TicketId` | Ticket source |
| `drawId` | `DrawId` | Draw associé |
| `amount` | `BigDecimal` | Montant dû |
| `status` | `PayoutClaimStatus` | Statut courant |
| `source` | `PayoutClaimSource` | Origine du claim |
| `outletId/Name` | — | PDV de vente |
| `sessionId` | `SalesSessionId` | Session de vente |
| `terminalId` | `TerminalId` | Terminal |
| `paidBy/blockedBy/cancelledBy/reversedBy` | `UserId` | Acteurs des transitions |
| `openedAt/paidAt/blockedAt/cancelledAt/reversedAt` | `Instant` | Timestamps |
| `blockReason/cancelReason/reverseReason` | `String` | Motifs |

### `PayoutRow` (liste)

`id, ticketId, amount, status, openedAt, outletId, outletName`

---

## 6. Événements publiés (after-commit)

| Événement | Déclencheur |
|---|---|
| `PayoutClaimOpenedEvent` | `OpenPayoutClaimFromSettlementCommand` |
| `PayoutPaymentPostedEvent` | `ExecutePayoutCommand` |
| `PayoutPaymentReversedEvent` | `ReversePayoutPaymentCommand` |
| `PayoutClaimClosedEvent` | Transition finale (PAID/CANCELLED/REVERSED) |

---

## 7. Invariants

- Idempotence sur `OpenPayoutClaimFromSettlementCommand` via `sourceEventId`.
- Optimistic lock `version` sur le claim (concurrence multi-terminal).
- `netPaid <= amountDue` — paiement partiel interdit (un claim = une transaction).
- Paiements immutables : append-only, pas de mise à jour — reversal via entrée miroir.
- RLS actif (multi-tenant).

---

## 8. Intégrations

| Direction | Type | Détail |
|---|---|---|
| `core.sales` → payout | Event | `TicketWinningSettlementCreatedEvent` déclenche `OpenPayoutClaimFromSettlementCommand` |
| `core.payout` → `core.sales` | Ports | `TicketReaderPort`, `markPayoutPaid()` |
| `core.payout` → `core.ledger` | Event | `PayoutPaymentPostedEvent` consommé par ledger |
| `core.reconciliation` → payout | Query | `ListPayoutClaimsForDrawQuery`, `GetPayoutSummaryForDrawQuery` |
| `core.session` → payout | Query | `GetPayoutSummaryBySessionQuery` (clôture de session) |

---

## 9. TODO / Anomalies

- Vérifier la cohérence entre `PayoutStatus` (internal) et `PayoutClaimStatus` (api) — deux enums pour des transitions partiellement redondantes.
- `RegisterPayoutCommand` : confirmer le cas d'usage exact (réconciliation externe ou saisie manuelle).
- Auto-approve entitlement : `FEATURE_PAYOUT_AUTO_APPROVE` non encore hookée dans le handler (ROADMAP).
