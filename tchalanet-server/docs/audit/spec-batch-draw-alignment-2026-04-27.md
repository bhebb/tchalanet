# spec-batch-draw-alignment-2026-04-27.md

> **Type** : Spec opérationnelle — audit batch jobs + schedulers draws
> **Date** : 2026-04-27
> **Scope** : `tchalanet-server` > **Domaines** : `core.draw`, `core.drawresult` > **Auteur** : Généré par analyse automatique du repo

---

## Résumé

Audit de conformité de tous les schedulers et Spring Batch jobs liés aux tirages.
Critères vérifiés : `@SchedulerLock`, `BatchGate`, `Clock` injecté, cron externalisé,
idempotence. Schéma Spring Batch V42 vérifié pour compatibilité SB6 et permissions.

---

## 1. Tableau de conformité — Schedulers draws

| Scheduler                                                |         @SchedulerLock         |          BatchGate          |     Clock     |                  Cron externalisé                   |      Idempotent       |      Statut global      |
| -------------------------------------------------------- | :----------------------------: | :-------------------------: | :-----------: | :-------------------------------------------------: | :-------------------: | :---------------------: |
| `DrawLifeCycleTickScheduler.generateNext7Days`           | ✅ `draw_generate_next_7_days` |     ✅ `DRAW_GENERATE`      |      ✅       |      ✅ `${tch.draw.lifecycle.generate_cron}`       |    ✅ Tenant-loop     |     ✅ **CONFORME**     |
| `DrawLifeCycleTickScheduler.openWindowed`                |    ✅ `draw_open_windowed`     |       ✅ `DRAW_OPEN`        |      ✅       |        ✅ `${tch.draw.lifecycle.open_cron}`         | ✅ BatchWindowsConfig |     ✅ **CONFORME**     |
| `DrawLifeCycleTickScheduler.closeWindowed`               |    ✅ `draw_close_windowed`    |       ✅ `DRAW_CLOSE`       |      ✅       |        ✅ `${tch.draw.lifecycle.close_cron}`        | ✅ BatchWindowsConfig |     ✅ **CONFORME**     |
| `DrawSettleScheduler.tick`                               |     ✅ `draw_settle_tick`      |      ✅ `DRAW_SETTLE`       |      ✅       |            ❌ `"0 */5 * * * *"` hardcodé            |  ✅ BatchJobStarter   |  ⚠️ **CRON HARDCODÉ**   |
| `ExternalResultsApplyTickScheduler.tickApply`            |  ✅ `draw_results_apply_tick`  | ✅ `RESULTS_EXTERNAL_APPLY` |      ✅       |                   ⚠️ Path erroné                    |     ✅ CommandBus     | ⚠️ **PATH YAML ERRONÉ** |
| `ExternalResultsFetchTickScheduler.tickFetch`            |  ✅ `draw_results_fetch_tick`  | ✅ `RESULTS_EXTERNAL_FETCH` |      ✅       | ✅ `${tch.draw.results.shared.scheduler.tick_cron}` | ⚠️ Cooldown in-memory |     ✅ **CONFORME**     |
| `DrawProvisionalWatchdogScheduler.checkProvisionalStuck` |         ❌ **ABSENT**          |        ❌ **ABSENT**        | ❌ **ABSENT** |     ✅ `${tch.draw.watchdog.provisional_cron}`      |    N/A (read-only)    |   ❌ **NON CONFORME**   |

---

## 2. Tableau de conformité — Spring Batch Jobs draws

| Job                                      |             JobBuilder SB6              |      TransactionManager      |            Listener            |                  Idempotent                  |  Statut global  |
| ---------------------------------------- | :-------------------------------------: | :--------------------------: | :----------------------------: | :------------------------------------------: | :-------------: |
| `settleDrawsJob` (`DrawSettleJobConfig`) | ✅ `new JobBuilder(..., jobRepository)` | ✅ `batchTxManager` sur step | ✅ `BatchJobExecutionListener` | ✅ SB empêche double run par `JobParameters` | ✅ **CONFORME** |

---

## 3. Schéma Spring Batch — V42

### 3.1 Compatibilité Spring Batch 6

| Critère                                                  | Statut | Détail                                                                          |
| -------------------------------------------------------- | ------ | ------------------------------------------------------------------------------- |
| Table `BATCH_JOB_EXECUTION_PARAMS` avec `PARAMETER_TYPE` | ✅     | Schéma SB6 correct (remplace `STRING_VAL/LONG_VAL/DOUBLE_VAL/DATE_VAL` de SB4)  |
| 3 séquences présentes                                    | ✅     | `BATCH_STEP_EXECUTION_SEQ`, `BATCH_JOB_EXECUTION_SEQ`, `BATCH_JOB_INSTANCE_SEQ` |
| Schéma dédié `batch`                                     | ✅     | `CREATE SCHEMA IF NOT EXISTS batch`                                             |
| `GRANT USAGE ON SCHEMA batch TO app_user`                | ❌     | **ABSENT** — permission denied au démarrage                                     |
| `GRANT ALL ON ALL TABLES IN SCHEMA batch TO app_user`    | ❌     | **ABSENT**                                                                      |
| `GRANT ALL ON ALL SEQUENCES IN SCHEMA batch TO app_user` | ❌     | **ABSENT**                                                                      |

### 3.2 Impact des permissions manquantes

Au démarrage Spring Boot avec `spring.batch.job.enabled=false` (profil local) :
Spring Batch tente de lire `batch.BATCH_JOB_INSTANCE` pour la validation du repository.
Sans GRANT, l'erreur est :

```
ERROR: permission denied for schema batch
  Detail: app_user does not have USAGE privilege on schema batch
```

---

## 4. DrawDomainEventListener — replay safety

| Critère                                     | Statut | Détail                                                         |
| ------------------------------------------- | ------ | -------------------------------------------------------------- |
| `ProcessedEventPort` utilisé                | ✅     | `alreadyProcessed()` + `markProcessed()` sur les deux handlers |
| `@TransactionalEventListener(AFTER_COMMIT)` | ✅     | Side-effects after-commit uniquement                           |
| `onDrawResultIngested` replay-safe          | ✅     | Même `eventId` → retour immédiat, cache non double-évicté      |
| `onDrawSettled` replay-safe                 | ✅     | Même pattern                                                   |
| Cache effectivement invalidé (plus de TODO) | ✅     | `cacheEvictor.evictAll()` appelé dans les deux handlers        |

---

## 5. Fixes requis

### Fix 1 — `DrawProvisionalWatchdogScheduler.java`

```java
// AVANT — NON CONFORME
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawProvisionalWatchdogScheduler {
  private final DrawReaderPort drawReader;
  private final MeterRegistry meterRegistry;

  @Scheduled(cron = "${tch.draw.watchdog.provisional_cron:0 */15 * * * *}")
  public void checkProvisionalStuck() {
    var stuckDraws = drawReader.findResultedWithProvisionalOlderThan(Duration.ofMinutes(30));
    // ...
  }
}

// APRÈS — CONFORME
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawProvisionalWatchdogScheduler {
  private final DrawReaderPort drawReader;
  private final MeterRegistry meterRegistry;
  private final BatchGate batchGate;
  private final Clock clock;
  private final DrawProperties drawProps;

  @Scheduled(cron = "${tch.draw.watchdog.provisional_cron:0 */15 * * * *}", zone = "UTC")
  @SchedulerLock(name = "draw_provisional_watchdog", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
  public void checkProvisionalStuck() {
    if (!batchGate.enabled(BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL, null)) {
      log.debug("batch.skip jobKey={} reason=disabled", BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL);
      return;
    }
    int stuckMinutes = drawProps.getWatchdog().getProvisionalStuckMinutes();
    var stuckDraws = drawReader.findResultedWithProvisionalOlderThan(Duration.ofMinutes(stuckMinutes));
    // ...
  }
}
```

### Fix 2 — `DrawSettleScheduler.java`

```java
// AVANT
@Scheduled(cron = "0 */5 * * * *", zone = "America/New_York")

// APRÈS
@Scheduled(cron = "${tch.draw.settle.cron:0 */5 * * * *}", zone = "UTC")
```

### Fix 3 — `ExternalResultsApplyTickScheduler.java`

```java
// AVANT
@Scheduled(cron = "${tch.draw.results.scheduler.apply_cron:30 */5 * * * *}")

// APRÈS
@Scheduled(cron = "${tch.draw.results.shared.scheduler.apply_cron:30 */5 * * * *}")
```

### Fix 4 — `BatchJobKeys.java`

```java
// Ajouter :
public static final JobKey DRAW_WATCHDOG_PROVISIONAL = JobKey.of("draw:watchdog:provisional");
```

### Fix 5 — `DrawProperties.java`

```java
// Ajouter inner class :
@Getter @Setter
public static class Watchdog {
  private int provisionalStuckMinutes = 30;
}
private Watchdog watchdog = new Watchdog();
```

### Fix 6 — `application-draw.yaml`

```yaml
# Ajouter dans tch.draw :
tch:
  draw:
    settle:
      cron: ${TCH_DRAW_SETTLE_CRON:0 */5 * * * *}

    results:
      shared:
        scheduler:
          active: ${TCH_RESULTS_SCHEDULER_ACTIVE:true}
          tick_cron: ${TCH_RESULTS_TICK_CRON:0 */5 * * * *}
          apply_cron: ${TCH_RESULTS_APPLY_CRON:30 */5 * * * *} # AJOUT
```

### Fix 7 — `V44__batch_grants.sql` (nouvelle migration Flyway)

```sql
-- V44: Grants app_user sur le schéma batch (Spring Batch 6)
-- Correction de V42 qui ne définissait pas les permissions pour app_user

GRANT USAGE ON SCHEMA batch TO app_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA batch TO app_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA batch TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA batch GRANT ALL PRIVILEGES ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA batch GRANT ALL PRIVILEGES ON SEQUENCES TO app_user;
```

---

## 6. Test de non-régression — Replay `DrawResultIngestedEvent`

### Scénario : rejeu d'un événement dupliqué

```java
@SpringBootTest
class DrawDomainEventListenerReplayTest {

    @Autowired DrawDomainEventListener listener;
    @MockBean DrawResultCacheEvictor cacheEvictor;
    @MockBean ProcessedEventPort processedEventPort;

    @Test
    void onDrawResultIngested_replay_doesNotDoubleEvictCache() {
        var eventId = EventId.of(UUID.randomUUID());
        var event = new DrawResultIngestedEvent(
            eventId, TenantId.of(UUID.randomUUID()),
            DrawId.of(UUID.randomUUID()), null, LocalDate.now()
        );

        // Première réception — non encore traité
        when(processedEventPort.alreadyProcessed(anyString(), eq(eventId.value())))
            .thenReturn(false);

        listener.onDrawResultIngested(event);

        verify(cacheEvictor, times(1)).evictAll();
        verify(processedEventPort, times(1)).markProcessed(anyString(), eq(eventId.value()));

        // Deuxième réception — déjà traité
        when(processedEventPort.alreadyProcessed(anyString(), eq(eventId.value())))
            .thenReturn(true);

        listener.onDrawResultIngested(event);

        // Le cache ne doit pas être re-évicté
        verify(cacheEvictor, times(1)).evictAll();  // still 1, not 2
        verify(processedEventPort, times(1)).markProcessed(anyString(), eq(eventId.value()));  // still 1
    }
}
```

---

## 7. Checklist DoD

```
[ ] Migration V44
    [ ] V44__batch_grants.sql créé et appliqué
    [ ] app_user peut accéder au schéma batch (test psql)

[ ] DrawProvisionalWatchdogScheduler conforme
    [ ] @SchedulerLock présent (draw_provisional_watchdog)
    [ ] BatchGate.enabled(DRAW_WATCHDOG_PROVISIONAL) appelé en première ligne
    [ ] Clock injecté
    [ ] Duration.ofMinutes(30) → drawProps.getWatchdog().getProvisionalStuckMinutes()

[ ] DrawSettleScheduler cron externalisé
    [ ] @Scheduled(cron = "${tch.draw.settle.cron:...}")
    [ ] tch.draw.settle.cron dans application-draw.yaml

[ ] ExternalResultsApplyTickScheduler path corrigé
    [ ] cron = "${tch.draw.results.shared.scheduler.apply_cron:...}"
    [ ] apply_cron dans application-draw.yaml

[ ] BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL défini

[ ] DrawProperties.Watchdog inner class créée avec provisionalStuckMinutes

[ ] Build + tests
    [ ] ./mvnw verify → vert
    [ ] Démarrage local sans "permission denied for schema batch"
    [ ] DrawProvisionalWatchdogSchedulerTest (gate OFF → skip, gate ON → query)
    [ ] DrawDomainEventListenerReplayTest (replay → 1 seule éviction)
```
