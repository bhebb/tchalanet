# Tchalanet — Draw Batch Helper Matrix

## Objectif

Ce document centralise les batchs Tchalanet : propriétés, settings/gates, inputs Ops, horaires, préconditions, effets DB et vérifications.

---

## 1. Matrix des batchs

| Batch           | Job key                    | Scope  | Déclenchement   | Horaire / cron | Properties principales         | Setting key (`namespace=batch`) | Input Ops                         | Préconditions              | Effet attendu        |
| --------------- | -------------------------- | ------ | --------------- | -------------- | ------------------------------ | ------------------------------- | --------------------------------- | -------------------------- | -------------------- |
| Generate draws  | `draw:lifecycle:generate`  | Tenant | Scheduler + Ops | ex. 05:00 UTC  | `tch.draw.scheduler.generate.*` | `jobs.draw:lifecycle:generate.enabled` | `tenant_id`, `from`, `to`, `days_ahead`, `dry_run` | draw_channel actif         | crée draws SCHEDULED |
| Open draws      | `draw:lifecycle:open`      | Tenant | Scheduler + Ops | ex. */5 min    | `tch.draw.scheduler.open-today.*` | `jobs.draw:lifecycle:open.enabled` | `tenant_id`, `date`, `max_items`, `dry_run` | SCHEDULED + fenêtre valide | SCHEDULED → OPEN     |
| Close draws     | `draw:lifecycle:close`     | Tenant | Scheduler + Ops | processing tick | `tch.draw.scheduler.processing.close.*` | `jobs.draw:lifecycle:close.enabled` | `tenant_id`, `max_items`, `dry_run` | OPEN + cutoff passé        | OPEN → CLOSED        |
| Fetch results   | `results:external:fetch`   | Global | Scheduler + Ops | processing tick | providers + result slots       | `jobs.results:external:fetch.enabled` | `date`, `slot_keys`, `days_back`, `max_slots`, `dry_run`, `include_raw` | result_slot actif          | crée draw_result     |
| Apply results   | `results:external:apply`   | Tenant | Scheduler + Ops | post-fetch     | processing apply windows       | `jobs.results:external:apply.enabled` | `tenant_id`, `date`, `slot_keys`, `days_back`, `max_slots`, `dry_run` | CLOSED + draw_result       | CLOSED → RESULTED    |
| Settle draws    | `draw:lifecycle:settle`    | Tenant | Scheduler + Ops | processing tick | settlement windows             | `jobs.draw:lifecycle:settle.enabled` | `tenant_id`, `date`, `max_items`, `days_back`, `dry_run` | RESULTED                   | RESULTED → SETTLED   |

`refresh` n'est pas un job V0 : lancer explicitement `results:external:fetch`, puis
`results:external:apply` pour les tenants ciblés.

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
