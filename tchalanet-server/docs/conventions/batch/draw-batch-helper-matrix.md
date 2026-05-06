# Tchalanet — Draw Batch Helper Matrix

## Objectif

Ce document centralise les batchs Tchalanet : propriétés, settings/gates, inputs Ops, horaires, préconditions, effets DB et vérifications.

---

## 1. Matrix des batchs

| Batch           | Job key                  | Scope  | Déclenchement   | Horaire / cron | Properties principales         | Settings / gate                        | Input Ops                        | Préconditions              | Effet attendu        |
| --------------- | ------------------------ | ------ | --------------- | -------------- | ------------------------------ | -------------------------------------- | -------------------------------- | -------------------------- | -------------------- |
| Generate draws  | DRAW_GENERATE_NEXT       | Tenant | Scheduler + Ops | ex. 05:00 UTC  | lifecycle.generate.\*          | batch.DRAW_GENERATE_NEXT.enabled       | tenantId, from, to, dryRun       | draw_channel actif         | crée draws SCHEDULED |
| Open due draws  | DRAW_OPEN_DUE            | Global | Scheduler + Ops | ex. \*/5 min   | openHorizonHours, openLagHours | batch.DRAW_OPEN_DUE.enabled            | now, limit, horizon, lag, dryRun | SCHEDULED + fenêtre valide | SCHEDULED → OPEN     |
| Close due draws | DRAW_CLOSE_DUE           | Global | Scheduler + Ops | ex. \*/5 min   | closeLagHours                  | batch.DRAW_CLOSE_DUE.enabled           | now, limit, dryRun               | OPEN + cutoff passé        | OPEN → CLOSED        |
| Fetch results   | RESULTS_EXTERNAL_FETCH   | Global | Scheduler + Ops | ex. \*/5 min   | tch.draw.results.\*, providers | batch.RESULTS_EXTERNAL_FETCH.enabled   | now, daysBack, maxSlots, dryRun  | result_slot actif          | crée draw_result     |
| Apply results   | RESULTS_EXTERNAL_APPLY   | Tenant | Scheduler + Ops | post-fetch     | limits._, defaults._           | batch.RESULTS_EXTERNAL_APPLY.enabled   | tenantId, now, daysBack, dryRun  | CLOSED + draw_result       | CLOSED → RESULTED    |
| Refresh results | RESULTS_EXTERNAL_REFRESH | Global | Scheduler + Ops | ex. \*/5 min   | fetch + apply                  | batch.RESULTS_EXTERNAL_REFRESH.enabled | now, daysBack                    | fetch + apply OK           | orchestration        |
| Settle draws    | DRAW_SETTLE_DUE          | Tenant | Scheduler + Ops | ex. \*/10 min  | settlement.\*                  | batch.DRAW_SETTLE_DUE.enabled          | tenantId, now, dryRun            | RESULTED                   | RESULTED → SETTLED   |

---

## 2. Properties recommandées

```yaml
tch:
  draw:
    results:
      active: true
      scheduler:
        enabled: true
        tick-cron: '0 */5 * * * *'
      limits:
        hard-max-slots: 40
        hard-days-back: 7
      defaults:
        max-slots: 10
        days-back: 2
```

---

## 3. Exemple Inputs Ops

### Open

```json
{
  "now": "2026-05-01T01:56:51.019Z",
  "limit": 40,
  "openHorizonHours": 34,
  "openLagHours": 24,
  "dryRun": true
}
```

### Close

```json
{
  "now": "2026-05-03T18:04:15.916Z",
  "limit": 10,
  "dryRun": true
}
```

### Fetch

```json
{
  "now": "2026-05-03T18:04:15.916Z",
  "daysBack": 2,
  "maxSlots": 10,
  "dryRun": true
}
```

### Apply

```json
{
  "tenantId": "TENANT_UUID",
  "now": "2026-05-03T18:04:15.916Z",
  "daysBack": 2,
  "maxSlots": 10,
  "dryRun": true
}
```

---

## 4. Debug checklist

1. draws générés ?
2. draws OPEN ?
3. draws CLOSED ?
4. draw_result créé ?
5. apply exécuté ?
6. status RESULTED ?

---

## 5. Notes

- Fetch = global
- Apply = tenant-scoped
- Toujours tester avec dryRun=true avant
