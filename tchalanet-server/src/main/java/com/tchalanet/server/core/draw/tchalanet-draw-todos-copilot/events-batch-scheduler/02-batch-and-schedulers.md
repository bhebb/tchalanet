# TODO — `core.draw` batch et schedulers

## Files principaux

- `DrawLifeCycleTickScheduler`
- `ExternalResultsApplyTickScheduler`
- `DrawProvisionalWatchdogScheduler`
- `DrawSettleScheduler`
- `DrawProperties`
- `DrawResultsProperties`
- `DrawSchedulerWindows`
- `BatchGate`
- `BatchTchContextBinder`
- `BatchJobKeys`

## P0 — config/properties

YAML cible :

```yaml
tch:
  draw:
    cache:
      ttl:
        last7m: 5
        todaym: 5
        nexts: 60
    scheduler:
      active: true
      windows:
        enabled: true
        timezone: America/New_York
        fetch_results: '12:00-14:00,20:00-23:00'
        apply_results: '12:00-14:30,20:00-23:30'
        settle_draws: '12:00-15:00,20:00-23:30'
        close_draws: '11:30-14:00,19:30-23:00'
        open_draws: '02:00-06:00'
    lifecycle:
      active: true
      generate_cron: '0 0 5 * * *'
      open_close_cron: '0 */5 * * * *'
      generation_days: 7
      batch_size: 500
      lookahead_hours: 24
      lag_hours: 1
    settlement:
      active: true
      cron: '0 */5 * * * *'
      days_back: 1
      max_draws_per_tenant: 500
    results:
      active: true
      scheduler:
        active: true
        cron: '0 */5 * * * *'
        min_minutes_after_draw: 3
        max_minutes_after_draw: 120
        cooldown_minutes: 10
      limits:
        max_slots_per_tick: 100
        hard_days_back: 7
      defaults:
        manual_days_back: 0
        manual_max_slots: 50
```

TODO :

- [ ] Supprimer bloc YAML dupliqué.
- [ ] Supprimer `tch.draw.results.apply-scheduler.active`.
- [ ] Ajouter `apply_results` si apply a sa propre fenêtre.
- [ ] Ajouter `hard_days_back` si `clampDaysBack` existe.
- [ ] Vérifier mapping exact snake_case YAML -> properties Java.
- [ ] Déplacer config draw vers `application-draw.yaml` si souhaité.

## P0 — `DrawLifeCycleTickScheduler`

### Generate

- [ ] `@BatchScheduledJob("draw:lifecycle:generate")`.
- [ ] Respecter `tch.draw.scheduler.active`.
- [ ] Respecter `tch.draw.lifecycle.active`.
- [ ] Respecter global gate.
- [ ] Respecter tenant gate si disponible.
- [ ] Boucler tenants actifs.
- [ ] Binder contexte tenant via `BatchTchContextBinder`.
- [ ] Erreur tenant A ne stoppe pas tenant B.
- [ ] Timezone tenant/channel pour date generation.
- [ ] Metrics/logs : tenants, generated, skipped, errors.

### Open/Close

- [ ] `@BatchScheduledJob("draw:lifecycle:open_close")`.
- [ ] Respecter window `open_draws` pour open.
- [ ] Respecter window `close_draws` pour close.
- [ ] Appeler handlers avec `Instant now`.
- [ ] `lookahead_hours` / `lag_hours` depuis properties.
- [ ] Batch size depuis properties.
- [ ] Tenant-level gate.

## P0 — `ExternalResultsApplyTickScheduler`

- [ ] Utiliser `tch.draw.results.scheduler.active`.
- [ ] Ne plus lire `apply-scheduler.active`.
- [ ] `@BatchScheduledJob("draw:results:apply")`.
- [ ] Respecter `tch.draw.results.active`.
- [ ] Respecter global gate `RESULTS_EXTERNAL_APPLY`.
- [ ] Respecter tenant gate `RESULTS_EXTERNAL_APPLY`.
- [ ] Utiliser window `apply_results`, pas `fetch_results`.
- [ ] Loop tenants actifs.
- [ ] Binder contexte tenant.
- [ ] Continuer si un tenant échoue.
- [ ] `dryRun=false`, `force=false` en scheduler.
- [ ] `maxSlots` depuis `limits.max_slots_per_tick`.
- [ ] `daysBack` depuis defaults ou properties.
- [ ] Logs structurés : tenant, baseDate, applied, skipped, errors.

## P0 — due logic results

- [ ] Cron = cadence seulement.
- [ ] Window = autorisation opérationnelle.
- [ ] Due logic = readiness métier.
- [ ] Gate DB = kill switch runtime.

Règles :

- [ ] `min_minutes_after_draw = 3`.
- [ ] `max_minutes_after_draw = 120`.
- [ ] `cooldown_minutes = 10`.
- [ ] Ne pas spammer provider/apply.
- [ ] Calculer eligibility par slot timezone.

## P1 — `DrawProvisionalWatchdogScheduler`

- [ ] Ajouter `@BatchScheduledJob("draw:watchdog:provisional")` si notifications batch.
- [ ] Clarifier RLS : platform context ou loop tenants + binder.
- [ ] Ne pas modifier état métier.
- [ ] Log/metric seulement en MVP.
- [ ] Plus tard : notification ops via edge service.

## P1 — `DrawSettleScheduler`

Statut : deferred jusqu'à sales.

- [ ] Garder scheduler mais marquer settlement comme non final.
- [ ] Retirer provider/channelCode des params batch.
- [ ] Paramètres : tenant_id, request_id, actor, days_back, max_draws, dry_run, force.
- [ ] Query by tenant + status RESULTED + draw_result_id.
- [ ] Tenant gate.
- [ ] Idempotency.
- [ ] Ne pas settle si tickets pending/payout incompatible.

## P1 — batch notifications

Architecture cible :

```text
BatchScheduledJobAspect
  -> BatchEventNotificationService
  -> NotificationGatewayPort
  -> NodeNotificationGatewayAdapter
```

- [ ] Ajouter `@BatchScheduledJob` aux jobs critiques.
- [ ] FAILED notifié avec cooldown.
- [ ] SKIPPED notifié seulement si `gate_disabled`.
- [ ] STARTED/SUCCEEDED non notifiés par défaut.
- [ ] Fingerprint : `jobKey + tenant + status + code`.
- [ ] Cooldown cache TTL 30 min.

## Définition de terminé

- Schedulers lisent les bonnes propriétés.
- Apply n'utilise pas la fenêtre fetch.
- Tous les schedulers peuvent être désactivés via config + gate.
- Les erreurs tenant sont isolées.
- Logs/métriques exploitables.
