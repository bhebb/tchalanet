## 1. Migration Flyway — permissions batch

- [ ] 1.1 Vérifier le numéro de la prochaine migration disponible : `ls tchalanet-server/src/main/resources/db/migration/V4*.sql` (choisir V44 ou suivant)
- [ ] 1.2 Créer `V44__batch_grants.sql` :
  ```sql
  -- Grants pour app_user sur le schéma batch (Spring Batch 6)
  GRANT USAGE ON SCHEMA batch TO app_user;
  GRANT ALL ON ALL TABLES IN SCHEMA batch TO app_user;
  GRANT ALL ON ALL SEQUENCES IN SCHEMA batch TO app_user;
  ALTER DEFAULT PRIVILEGES IN SCHEMA batch GRANT ALL ON TABLES TO app_user;
  ALTER DEFAULT PRIVILEGES IN SCHEMA batch GRANT ALL ON SEQUENCES TO app_user;
  ```

## 2. BatchJobKeys — nouvelle clé watchdog

- [ ] 2.1 Ajouter `DRAW_WATCHDOG_PROVISIONAL = JobKey.of("draw:watchdog:provisional")` dans `BatchJobKeys`

## 3. DrawProperties — Watchdog config

- [ ] 3.1 Ajouter inner class `Watchdog` dans `DrawProperties` :
  ```java
  @Getter @Setter
  public static class Watchdog {
    private int provisionalStuckMinutes = 30;
    private String provisionalCron = "0 */15 * * * *";
  }
  private Watchdog watchdog = new Watchdog();
  ```

## 4. DrawProvisionalWatchdogScheduler — mise à la norme

- [ ] 4.1 Injecter `BatchGate batchGate`, `Clock clock`, `DrawProperties drawProps`
- [ ] 4.2 Remplacer `@Scheduled(cron = "${tch.draw.watchdog.provisional_cron:0 */15 * * * *}")` par `@Scheduled(cron = "${tch.draw.watchdog.provisional_cron:0 */15 * * * *}", zone = "UTC")`
- [ ] 4.3 Ajouter `@SchedulerLock(name = "draw_provisional_watchdog", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")`
- [ ] 4.4 Ajouter en première ligne de `checkProvisionalStuck()` :
  ```java
  if (!batchGate.enabled(BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL, null)) {
    log.debug("batch.skip jobKey={} reason=disabled", BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL);
    return;
  }
  ```
- [ ] 4.5 Remplacer `Duration.ofMinutes(30)` par `Duration.ofMinutes(drawProps.getWatchdog().getProvisionalStuckMinutes())`

## 5. DrawSettleScheduler — externalisation cron

- [ ] 5.1 Remplacer `@Scheduled(cron = "0 */5 * * * *", zone = "America/New_York")` par `@Scheduled(cron = "${tch.draw.settle.cron:0 */5 * * * *}", zone = "UTC")`
- [ ] 5.2 Ajouter dans `application-draw.yaml` sous une section `tch.draw.settle` :
  ```yaml
  tch:
    draw:
      settle:
        cron: ${TCH_DRAW_SETTLE_CRON:0 */5 * * * *}
  ```

## 6. ExternalResultsApplyTickScheduler — correction path cron

- [ ] 6.1 Remplacer `${tch.draw.results.scheduler.apply_cron:30 */5 * * * *}` par `${tch.draw.results.shared.scheduler.apply_cron:30 */5 * * * *}`
- [ ] 6.2 Ajouter dans `application-draw.yaml` sous `tch.draw.results.shared.scheduler` :
  ```yaml
  apply_cron: ${TCH_RESULTS_APPLY_CRON:30 */5 * * * *}
  ```

## 7. Document d'audit

- [ ] 7.1 Créer `tchalanet-server/docs/audit/spec-batch-draw-alignment-2026-04-27.md` avec :
  - Tableau exhaustif schedulers (conforme / à corriger)
  - Section "Fixes requis" avec code exact
  - Section "Test de non-régression DrawResultIngestedEvent"
  - Checklist DoD

## 8. Validation

- [ ] 8.1 `./mvnw spring-boot:run -Dspring-boot.run.profiles=local-ide` → pas de `permission denied for schema batch`
- [ ] 8.2 Test `DrawProvisionalWatchdogSchedulerTest` : gate OFF → pas de query DB, gate ON → query émise
- [ ] 8.3 Test : deux invocations `onDrawResultIngested` avec même `eventId` → `cacheEvictor.evictAll()` appelé une seule fois
- [ ] 8.4 `./mvnw verify` → build vert (tous les tests)
