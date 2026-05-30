# Réconciliation — Flow

> Vérification de cohérence post-tirage entre tickets, résultats, claims et paiements.  
> Domaine : `core.reconciliation`  
> Déclencheur : scheduler quotidien ou manuel par ops

---

## Pourquoi

Le settlement est automatique mais peut produire des incohérences en cas de race condition, bug, résultat incorrect ou correction manuelle. La réconciliation détecte ces anomalies sans les corriger — elle produit un rapport pour décision humaine.

**La réconciliation ne modifie pas les données.** Elle détecte et expose.

---

## États d'un ReconciliationRun

| État | Signification |
|---|---|
| `RUNNING` | Run en cours |
| `COMPLETED` | Terminé sans erreur technique |
| `FAILED` | Erreur technique pendant le run |

| Type | Déclencheur |
|---|---|
| `SCHEDULED` | Run automatique quotidien (scheduler) |
| `FORCED` | Déclenché manuellement par ops |

---

## Pipeline d'un run

```
Scheduler (quotidien) ou ops (forced)
  └─ ReconciliationDailyRunService.run()
       → ReconciliationRun créé (RUNNING)
       → Vérifications par type d'anomalie (séquentielles)
       → Anomalies enregistrées avec sévérité
       → ReconciliationRun → COMPLETED ou FAILED
       → ReconciliationRunCompletedEvent publié
```

---

## Types d'anomalies et sévérité

| Anomalie | Sévérité | Signification |
|---|---|---|
| `DRAW_WINNER_COUNT_MISMATCH` | HIGH | Nombre de gagnants différent d'attendu |
| `DRAW_PAYOUT_TOTAL_MISMATCH` | HIGH | Total payout différent du total calculé |
| `DRAW_PAID_TOTAL_EXCEEDS_EXPECTED` | CRITICAL | Total payé dépasse le total attendu |
| `TICKET_RESULT_STATUS_MISSING_AFTER_DRAW_RESULT` | MEDIUM | Ticket sans statut résultat après tirage |
| `EXPECTED_WINNER_NOT_RESULTED` | HIGH | Gagnant attendu non résulté |
| `FALSE_WINNER_RESULTED` | CRITICAL | Ticket résulté WON mais ne devrait pas l'être |
| `SALES_OUTCOME_AMOUNT_MISMATCH` | MEDIUM | Montant outcome ventes incohérent |
| `WINNER_WITHOUT_PAYOUT_CLAIM` | HIGH | Gagnant sans claim de paiement |
| `CLAIM_FOR_NON_WINNING_TICKET` | HIGH | Claim sur ticket non gagnant |
| `PAYOUT_CLAIM_AMOUNT_MISMATCH` | HIGH | Montant claim différent du gain |
| `PAYMENT_EXCEEDS_CLAIM_AMOUNT` | CRITICAL | Paiement dépasse le claim |
| `PAYMENT_CLAIM_STATUS_MISMATCH` | MEDIUM | Incohérence statut claim/paiement |
| `PAID_NON_WINNING_TICKET` | CRITICAL | Ticket non gagnant payé |

**Niveaux :**
- `LOW` — informatif, aucune action immédiate
- `MEDIUM` — à investiguer dans la journée
- `HIGH` — à traiter avant le prochain tirage
- `CRITICAL` — bloquer les paiements, intervention immédiate

---

## Run forcé par ops

```
POST /platform/reconciliation/run   (ou /admin/reconciliation/run)
  { drawId?: "...", scope?: "DRAW" | "GLOBAL" }
  → { runId, status:"RUNNING" }

GET /platform/reconciliation/runs/{runId}
  → { runId, status, startedAt, completedAt, anomalyCount, anomalies:[...] }
```

Un run forcé peut être ciblé sur un tirage spécifique ou global.

---

## Lecture des anomalies

```
GET /admin/reconciliation/runs/{runId}/anomalies
  → [{ type, severity, drawId, ticketId, detail, detectedAt }]

GET /admin/reconciliation/runs?status=COMPLETED&limit=10
  → Liste des runs récents avec résumé
```

---

## Workflow de résolution (humain)

```
Anomalie CRITICAL détectée
  → Ops notifié (ReconciliationRunCompletedEvent → notification)
  → Ops consulte les anomalies
  → Investigation manuelle
  → Action corrective selon le type :
      FALSE_WINNER → invalider le résultat, re-trigger settlement
      PAID_NON_WINNING → ReversePayoutPaymentCommand
      WINNER_WITHOUT_CLAIM → OpenPayoutClaimFromSettlementCommand manuel
```

La réconciliation ne corrige pas — elle fournit le diagnostic. L'ops choisit l'action.

---

## Invariants

- Un run `RUNNING` ne peut pas être re-déclenché (idempotency par type+date)
- Les anomalies sont immuables — un run `COMPLETED` ne modifie pas ses résultats
- `CRITICAL` déclenche une alerte platform (via `core.notification`)
- Le run `FAILED` n'a pas de résultats partiels fiables — relancer un run forcé

---

## Références

- Domaine : `core/reconciliation/DOMAIN_RECONCILIATION.md`
- Settlement qui précède : [settlement](./settlement.md)
- Pipeline résultats : [draw-execution](./draw-execution.md)
- Payout (correction possible) : [payout-field-flow](./payout-field-flow.md)
