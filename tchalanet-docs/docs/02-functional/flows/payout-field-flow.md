# Payout Terrain — NON DOCUMENTÉ

> **Statut** : NON DOCUMENTÉ — slice suspendue des docs actifs  
> **Raison** : `core.payout` n'est pas documenté pour le moment. Le contenu ci-dessous est conservé pour référence future mais peut être inexact (acteur `CASHIER` → `SELLER_TERMINAL`, `terminalId`/`sessionId` retirés).  
> **Ne pas référencer** ce flow dans le nouveau code ou les nouvelles docs.

---

---

## Pourquoi

Après le tirage, les tickets gagnants sont identifiés et des claims de paiement sont ouverts automatiquement. Le vendeur terrain réalise le paiement physique au client et l'enregistre dans le système.

Le payout terrain est distinct du payout administratif (override, annulation) — il est réalisé depuis le POS mobile ou web cashier.

---

## Deux types de payout

| Type | Déclencheur | Acteur |
|---|---|---|
| **Terrain (field)** | Vendeur POS après vérification ticket | `CASHIER` ou `TENANT_ADMIN` en mode POS |
| **Administratif** | Admin depuis l'interface tenant | `TENANT_ADMIN` |

Ce flow documente le payout terrain.

---

## États du PayoutClaim

```
OPEN → PAID
     ↘ BLOCKED → OPEN (déblocage)
     ↘ CANCELLED
     ↘ REVERSED
```

| État | Signification |
|---|---|
| `OPEN` | Claim disponible pour paiement |
| `BLOCKED` | Suspendu par l'admin (fraude suspectée, limite, etc.) |
| `PAID` | Paiement enregistré |
| `CANCELLED` | Claim annulé |
| `REVERSED` | Paiement annulé après enregistrement |

---

## États du Payout

| État | Signification |
|---|---|
| `REQUESTED` | Paiement demandé (enregistré) |
| `APPROVED` | Approuvé (workflow admin si requis) |
| `REJECTED` | Refusé |
| `PAID` | Paiement effectué |
| `CANCELLED` | Annulé |

---

## Flow : Payout terrain standard

```
1. Vendeur scanne ou entre le code du ticket
   POST /tenant/cashier/tickets/verify
   { ticketCode ou url }
   → { status, severity, actions:[..., "EXECUTE_PAYOUT"] }

2. Si action EXECUTE_PAYOUT disponible :
   POST /tenant/cashier/payout/execute
   { ticketId, terminalId, declaredAmount }
   → RegisterPayoutCommand → PayoutClaim PAID
   → SalesSessionPayoutRecordedEvent

3. Imprimer/envoyer le reçu de paiement (optionnel) :
   POST /tenant/cashier/payout/{payoutId}/print
```

---

## Claim bloqué — flow admin

```
Admin bloque le claim :
  BlockPayoutClaimCommand → PayoutClaim BLOCKED
  
Vendeur tente de payer → 403 / issue PAYOUT_BLOCKED
Vendeur informe le client et l'admin

Admin débloque (après investigation) :
  UnblockPayoutClaimCommand → PayoutClaim OPEN
  → Vendeur peut re-tenter
```

---

## Origine du PayoutClaim

Les claims sont ouverts automatiquement après le settlement d'un tirage :

```
DrawResult publié
  → TicketResultedEvent
  → TicketWinningSettlementCreatedEvent
  → OpenPayoutClaimFromSettlementCommand
  → PayoutClaim OPEN pour chaque ticket gagnant
```

Le vendeur n'ouvre pas lui-même le claim — il l'exécute.

---

## Readiness cashier

`CashierReadinessResponse` affiche un badge si des claims `OPEN` existent pour la session :
- `PREVIOUS_UNPAID_PAYOUTS` → action `VIEW_PAYOUTS_TO_PROCESS`

Le cashier n'est pas propriétaire des claims — il les consulte et les exécute.

---

## Invariants

- Un PayoutClaim `BLOCKED` ne peut pas être payé
- `declaredAmount` est déclaratif — le système valide vs le montant du claim
- Le payout terrain nécessite un contexte opérationnel trusted (terminal + session actifs)
- `REVERSED` ne recrée pas un claim — contacter l'admin pour traitement exceptionnel

---

## Références

- Domaine payout : `core/payout/DOMAIN_PAYOUT.md`
- Settlement qui crée les claims : [settlement](./settlement.md)
- Vérification ticket : [verify-ticket](./verify-ticket.md)
- Contexte opérationnel : `docs/conventions/context/operational-context.md`
