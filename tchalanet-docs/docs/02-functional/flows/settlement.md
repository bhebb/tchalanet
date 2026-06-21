# Settlement — Flow

> Traitement des tickets gagnants après publication du résultat d'un tirage.  
> Domaine : `core.sales` · `core.drawresult`  
> Déclencheur : event-driven, batch-assisted

---

## Pourquoi

Quand un résultat de tirage est publié, chaque ticket vendu pour ce tirage doit être "résulté" (winner ou non-winner). Les tickets gagnants reçoivent un settlement — un enregistrement comptable et la création d'un claim de gain pour le paiement terrain.

Le settlement est automatique — il n'est pas déclenché manuellement par un opérateur.

---

## États du TicketSettlementStatus

```
NOT_SETTLED
  → PAYOUT_PENDING   ← ticket gagnant — claim ouvert, paiement à venir
  → NO_PAYOUT        ← ticket non gagnant ou sans gain
  → PAID             ← paiement enregistré
  → REVERSED         ← paiement annulé après enregistrement
```

| État | Signification |
|---|---|
| `NOT_SETTLED` | Pas encore résulté (tirage pas encore publié) |
| `PAYOUT_PENDING` | Gagnant — claim `OPEN`, en attente de paiement terrain |
| `NO_PAYOUT` | Non gagnant — settlement terminé, pas de paiement |
| `SETTLED` | Résulté (état intermédiaire interne) |
| `PAID` | Paiement terrain enregistré |
| `REVERSED` | Paiement reversé (exceptionnel) |

---

## Pipeline settlement

```
DrawResult publié (externe ou manuel)
  └─ DrawResultPublishedEvent

core.sales reçoit l'event
  └─ Pour chaque ticket sur ce tirage :
       → Évaluer les lignes vs le résultat
       → TicketResultedEvent publié
           ├─ ticket non gagnant → SettlementLifecycle.settledWithoutPayout()
           │   → TicketSettlementStatus: NO_PAYOUT
           └─ ticket gagnant → SettlementLifecycle.settledPendingPayout()
               → TicketWinningSettlementCreatedEvent
               → TicketSettlementStatus: PAYOUT_PENDING

Événement publié : TicketWinningSettlementCreatedEvent
  → ouverture d'un claim de gain (domaine non documenté)
```

---

## Données du settlement

`TicketWinningSettlementCreatedEvent` contient :

| Champ | Source |
|---|---|
| `ticketId` | Ticket gagnant |
| `drawId` | Tirage résulté |
| `amountCents` | Gain calculé |
| `currency` | Devise du tenant |
| `sellerTerminalId` | SellerTerminal qui a vendu le ticket |

---

## Cas settlement manuel (ops)

Si le résultat automatique est incorrect ou manquant :

```
Ops déclare un résultat manuel :
  RecordManualDrawResultCommand
  → DrawResult créé avec source MANUAL

Ou ops overrides un résultat existant :
  OverrideDrawResultCommand
  → MarkDrawResultOverriddenCommand
  → Re-déclenche le pipeline settlement
```

Le re-settlement d'un tirage déjà settled est géré — les tickets déjà `PAID` ou `REVERSED` ne sont pas recalculés.

---

## Invariants

- Un ticket ne peut être résulté qu'une fois par tirage (idempotency via `sourceEventId`)
- L'idempotency du claim de gain est garantie par `sourceEventId` côté consommateur
- Après `PAID`, le ticket ne peut pas revenir `PAYOUT_PENDING` — reversal → `REVERSED`
- Le settlement est **lecture-seule** vis-à-vis du ticket original — il ne modifie pas les lignes

---

## Références

- Domaine sales : `core/sales/DOMAIN_SALES.md`
- Pipeline résultats tirage : [draw-execution](./draw-execution.md)
