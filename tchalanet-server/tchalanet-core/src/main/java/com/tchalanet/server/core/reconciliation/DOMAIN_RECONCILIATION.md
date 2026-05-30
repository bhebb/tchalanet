# Domain — Reconciliation

## Responsabilité

`core.reconciliation` vérifie la cohérence entre les sources de vérité après chaque tirage : tickets résultés, gagnants attendus, paiements réalisés, montants correspondants.

Il ne corrige pas. Il détecte, classe et expose les anomalies pour décision humaine.

---

## Cycle d'un run

```
Scheduler quotidien
  → ReconciliationDailyRunService.run()
  → ReconciliationRun créé (RUNNING)
  → Vérifications par type d'anomalie
  → Anomalies enregistrées
  → ReconciliationRun → COMPLETED ou FAILED
  → ReconciliationRunCompletedEvent publié
```

---

## États d'un run

| Statut | Signification |
|---|---|
| `RUNNING` | Run en cours |
| `COMPLETED` | Terminé sans erreur technique |
| `FAILED` | Erreur technique pendant le run |

| Type | Usage |
|---|---|
| `SCHEDULED` | Run automatique quotidien |
| `FORCED` | Run déclenché manuellement par ops |

---

## Types d'anomalies

| Anomalie | Signification |
|---|---|
| `DRAW_WINNER_COUNT_MISMATCH` | Nombre de gagnants différent d'attendu |
| `DRAW_PAYOUT_TOTAL_MISMATCH` | Total payout différent du total calculé |
| `DRAW_PAID_TOTAL_EXCEEDS_EXPECTED` | Total payé dépasse le total attendu |
| `TICKET_RESULT_STATUS_MISSING_AFTER_DRAW_RESULT` | Ticket sans statut résultat après tirage |
| `EXPECTED_WINNER_NOT_RESULTED` | Gagnant attendu non résulté |
| `FALSE_WINNER_RESULTED` | Ticket résulté WON mais ne devrait pas l'être |
| `SALES_OUTCOME_AMOUNT_MISMATCH` | Montant outcome ventes incohérent |
| `WINNER_WITHOUT_PAYOUT_CLAIM` | Gagnant sans claim de paiement |
| `CLAIM_FOR_NON_WINNING_TICKET` | Claim sur ticket non gagnant |
| `PAYOUT_CLAIM_AMOUNT_MISMATCH` | Montant claim différent du gain |
| `PAYMENT_EXCEEDS_CLAIM_AMOUNT` | Paiement dépasse le claim |
| `PAYMENT_CLAIM_STATUS_MISMATCH` | Incohérence statut claim/paiement |
| `PAID_NON_WINNING_TICKET` | Ticket non gagnant payé |

---

## Sévérité des anomalies

| Sévérité | Signification |
|---|---|
| `LOW` | Incohérence mineure, à surveiller |
| `MEDIUM` | Anomalie à corriger |
| `HIGH` | Anomalie critique à traiter rapidement |
| `CRITICAL` | Blocage ou fraude potentielle |

---

## États d'une anomalie

| Statut | Signification |
|---|---|
| `OPEN` | Détectée, non traitée |
| `RESOLVED` | Traitée et close par ops |

---

## Invariants

- `core.reconciliation` ne modifie jamais les tickets, claims ou paiements.
- Les anomalies `CRITICAL` doivent être visibles immédiatement en dashboard ops.
- Un run `FAILED` ne supprime pas les anomalies partiellement détectées.
- Un run `FORCED` suit les mêmes règles qu'un run `SCHEDULED`.

---

## Règles

- Le run est déclenché par batch/scheduler — pas par une action utilisateur directe.
- Les anomalies sont exportables CSV (`ReconciliationAnomalyCsvExporter`) pour audit externe.
- Voir conventions : `docs/conventions/batch/batch.md` · `docs/conventions/event_model.md`
