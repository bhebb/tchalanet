## Context

### État actuel des schedulers draws

| Scheduler                           | @SchedulerLock | BatchGate | Clock     | Cron externalisé                           | Statut              |
| ----------------------------------- | -------------- | --------- | --------- | ------------------------------------------ | ------------------- |
| `DrawLifeCycleTickScheduler` (×3)   | ✅             | ✅        | ✅        | ✅ `${tch.draw.lifecycle.*_cron}`          | ✅ Conforme         |
| `DrawSettleScheduler`               | ✅             | ✅        | ✅        | ❌ `"0 */5 * * * *"` hardcodé              | ⚠️ Cron hardcodé    |
| `ExternalResultsApplyTickScheduler` | ✅             | ✅        | ✅        | ⚠️ Path incorrect                          | ⚠️ Path YAML erroné |
| `ExternalResultsFetchTickScheduler` | ✅             | ✅        | ✅        | ✅                                         | ✅ Conforme         |
| `DrawProvisionalWatchdogScheduler`  | ❌ ABSENT      | ❌ ABSENT | ❌ ABSENT | ✅ `${tch.draw.watchdog.provisional_cron}` | ❌ Non conforme     |

### Anomalie 1 — `DrawProvisionalWatchdogScheduler` : ShedLock + BatchGate manquants

```java
// État actuel — NON CONFORME
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawProvisionalWatchdogScheduler {
  private final DrawReaderPort drawReader;
  private final MeterRegistry meterRegistry;

  @Scheduled(cron = "${tch.draw.watchdog.provisional_cron:0 */15 * * * *}")
  // ❌ Pas de @SchedulerLock
  // ❌ Pas de BatchGate.enabled()
  // ❌ Pas de Clock (duration hardcodée : Duration.ofMinutes(30))
  public void checkProvisionalStuck() { ... }
}
```

En multi-instance (2+ pods), ce watchdog s'exécute en parallèle sur tous les pods toutes
les 15 minutes → N fois les métriques Micrometer sont incrémentées pour le même draw stuck.
Aucun moyen opérationnel de désactiver le watchdog sans redéploiement.

### Anomalie 2 — `DrawSettleScheduler` : cron hardcodé

```java
@Scheduled(cron = "0 */5 * * * *", zone = "America/New_York")  // ❌ hardcodé
@SchedulerLock(name = "draw_settle_tick", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
public void tick() { ... }
```

Tous les autres schedulers draws externalisent leur cron. Convention violée.

### Anomalie 3 — `ExternalResultsApplyTickScheduler` : cron path incorrect

```java
@Scheduled(cron = "${tch.draw.results.scheduler.apply_cron:30 */5 * * * *}")
//                  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                  Cette propriété n'existe pas dans application-draw.yaml
//                  La convention du projet est tch.draw.results.shared.scheduler.*
```

`application-draw.yaml` définit `tch.draw.results.shared.scheduler.*` (tick_cron, active…)
mais PAS `tch.draw.results.scheduler.*`. La propriété `apply_cron` est donc ignorée —
le scheduler tourne toujours sur `30 */5 * * * *` quel que soit le YAML.

### Anomalie 4 — `V42__spring_batch_schema.sql` : permissions manquantes

```sql
-- Ce que V42 fait :
CREATE SCHEMA IF NOT EXISTS batch;
SET search_path = batch;
CREATE TABLE BATCH_JOB_INSTANCE (...);
CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ ...;
-- ❌ Manque :
-- GRANT USAGE ON SCHEMA batch TO app_user;
-- GRANT ALL ON ALL TABLES IN SCHEMA batch TO app_user;
-- GRANT ALL ON ALL SEQUENCES IN SCHEMA batch TO app_user;
```

`app_user` n'est pas superuser. Sans GRANT, Spring Batch ne peut pas écrire dans
`batch.BATCH_JOB_INSTANCE` au démarrage → `ERROR: permission denied for schema batch`.

**Compatibility Spring Batch 6** :

- ✅ Tables correctes (PARAMETER_TYPE, PARAMETER_NAME, PARAMETER_VALUE — SB6 schema)
- ✅ 3 séquences présentes (BATCH_STEP_EXECUTION_SEQ, BATCH_JOB_EXECUTION_SEQ, BATCH_JOB_INSTANCE_SEQ)
- ❌ Pas de GRANT → blocant au démarrage

### État `DrawDomainEventListener` (événements draws)

```java
// ✅ CONFORME — audit précédent corrigé
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onDrawResultIngested(DrawResultIngestedEvent event) {
  if (processedEventPort.alreadyProcessed(KEY_CACHE_INVALIDATE, event.eventId().value())) return;
  cacheEvictor.evictAll();   // ✅ cache invalidé (plus de TODO)
  processedEventPort.markProcessed(KEY_CACHE_INVALIDATE, event.eventId().value());
}
// idem pour onDrawSettled
```

Replay-safe : si l'événement est rejoué (ex: message dupliqué), `alreadyProcessed` retourne
`true` → `return` sans re-invalider le cache ni doubler l'incrément de compteur.

## Goals / Non-Goals

**Goals:**

- Tous les `@Scheduled` dans `core.draw.*` et `core.drawresult.*` ont `@SchedulerLock` + `BatchGate`
- Aucun cron hardcodé dans les schedulers draws
- `app_user` peut accéder au schéma `batch` au démarrage Spring Batch
- `ExternalResultsApplyTickScheduler` utilise le bon path YAML

**Non-Goals:**

- Remplacer le cooldown in-memory par Redis dans `ExternalResultsFetchTickScheduler`
- Partitioning du `DrawSettleJobConfig`
- Ajout de metrics/alerting sur les schedulers

## Decisions

### D1 — `DrawProvisionalWatchdogScheduler` : ajout ShedLock + BatchGate + Clock

`@SchedulerLock(name = "draw_provisional_watchdog", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")` :

- `lockAtMostFor = PT10M` car le watchdog est read-only (query + metric) → lock court suffisant
- `lockAtLeastFor = PT1M` pour éviter une double exécution rapide

`BatchGate.enabled(DRAW_WATCHDOG_PROVISIONAL, null)` — clé `BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL`
à créer dans `BatchJobKeys`.

`Clock` injecté + `drawProps.getWatchdog().getProvisionalStuckMinutes()` pour sortir la valeur `30`
de la configuration. Ajouter `Watchdog` inner class dans `DrawProperties`.

### D2 — `DrawSettleScheduler` : externalisation cron

Propriété : `tch.draw.settle.cron` (nouveau path) dans `application-draw.yaml`.
Valeur par défaut inchangée : `0 */5 * * * *`.

Conserver `zone = "America/New_York"` sur `@Scheduled` — c'est un time zone de référence
opérationnelle stable, pas un appel `ZoneId.systemDefault()`.

### D3 — `ExternalResultsApplyTickScheduler` : correction path cron

Changer `${tch.draw.results.scheduler.apply_cron:...}` → `${tch.draw.results.shared.scheduler.apply_cron:30 */5 * * * *}`.
Ajouter `apply_cron: ${TCH_RESULTS_APPLY_CRON:30 */5 * * * *}` dans `application-draw.yaml`
sous `tch.draw.results.shared.scheduler`.

### D4 — `V44__batch_grants.sql` : migration corrective

Migration séparée (pas modifier V42, Flyway interdit) pour ajouter les grants.

```sql
GRANT USAGE ON SCHEMA batch TO app_user;
GRANT ALL ON ALL TABLES IN SCHEMA batch TO app_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA batch TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA batch GRANT ALL ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA batch GRANT ALL ON SEQUENCES TO app_user;
```

**Pas de V43** : vérifier si V43 existe déjà avant de nommer V44.

## Risks / Trade-offs

**[Risque] `BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL` — gate pas définie en DB**
→ `BatchGate.enabled()` retourne `true` si la clé est inconnue (comportement par défaut de la gate)
→ Aucun impact fonctionnel à l'ajout de la gate.

**[Trade-off] `DrawProvisionalWatchdogScheduler` — `lockAtLeastFor = PT1M` ralentit les tests**
→ En test, utiliser `@TestConfiguration` pour remplacer le scheduler ou désactiver le profil.

**[Risque] V44 appliqué après V42 sur une instance existante (staging/prod)**
→ Mitigation : la migration est idempotente (`GRANT` ne fail pas si déjà accordé sur PostgreSQL 15+
via `GRANT ... IF NOT EXISTS` ou simplement GRANT qui est idempotent).

## Migration Plan

1. Créer `V44__batch_grants.sql`
2. Modifier `DrawProvisionalWatchdogScheduler` + `BatchJobKeys`
3. Modifier `DrawSettleScheduler` cron
4. Modifier `ExternalResultsApplyTickScheduler` cron path
5. Mettre à jour `application-draw.yaml`
6. `DrawProperties` : ajouter inner class `Watchdog`
7. Vérifier build + tests

## Open Questions

_Aucune — toutes les décisions sont tranchées._
