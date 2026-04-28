## ADDED Requirements

### Requirement: Tout scheduler draw possède @SchedulerLock

Tout composant `@Scheduled` dans les packages `core.draw.*` et `core.drawresult.*`
SHALL être annoté `@SchedulerLock` avec des valeurs `lockAtMostFor` et `lockAtLeastFor`
adaptées à la durée d'exécution attendue.

#### Scenario: DrawProvisionalWatchdogScheduler protégé ShedLock

- **WHEN** deux instances de l'application tentent de déclencher `checkProvisionalStuck`
  simultanément
- **THEN** une seule instance acquiert le lock ShedLock nommé `draw_provisional_watchdog`
- **AND** la seconde instance saute silencieusement l'exécution

---

### Requirement: Tout scheduler draw vérifie BatchGate

Tout composant `@Scheduled` dans les packages `core.draw.*` et `core.drawresult.*`
SHALL appeler `batchGate.enabled(<key>, null)` en première instruction et retourner
immédiatement si la gate est désactivée.

#### Scenario: Watchdog désactivé via gate

- **WHEN** la gate `DRAW_WATCHDOG_PROVISIONAL` est désactivée (Unleash ou DB)
- **THEN** `checkProvisionalStuck()` retourne immédiatement sans faire aucune requête
- **AND** un log `batch.skip jobKey=DRAW_WATCHDOG_PROVISIONAL` est émis

#### Scenario: Watchdog actif par défaut

- **WHEN** la gate `DRAW_WATCHDOG_PROVISIONAL` n'est pas définie
- **THEN** `batchGate.enabled()` retourne `true` (fail-open par convention)
- **AND** le watchdog s'exécute normalement

---

### Requirement: Aucun cron hardcodé dans les schedulers draws

Toute `@Scheduled(cron = ...)` dans les packages `core.draw.*` et `core.drawresult.*`
SHALL utiliser une expression `${tch.draw.<scope>.<method>_cron:<défaut>}` résolue
depuis la configuration YAML. Aucune chaîne litérale de type `"0 */5 * * * *"` n'est autorisée.

#### Scenario: DrawSettleScheduler cron configurable

- **WHEN** `tch.draw.settle.cron=0 */2 * * * *` est positionnée dans l'environnement
- **THEN** `DrawSettleScheduler.tick()` est déclenché toutes les 2 minutes

#### Scenario: DrawSettleScheduler cron défaut

- **WHEN** aucune propriété `tch.draw.settle.cron` n'est définie
- **THEN** `DrawSettleScheduler.tick()` utilise le cron par défaut `0 */5 * * * *`

---

### Requirement: ExternalResultsApplyTickScheduler utilise le path YAML canonique

`ExternalResultsApplyTickScheduler` SHALL lire son cron sur
`tch.draw.results.shared.scheduler.apply_cron` (cohérent avec les autres props de
`DrawResultsCommonProperties.Scheduler`).

#### Scenario: apply_cron configurable

- **WHEN** `tch.draw.results.shared.scheduler.apply_cron=0 */3 * * * *` est définie
- **THEN** le scheduler apply utilise ce cron au lieu du défaut

#### Scenario: apply_cron chemin cohérent

- **WHEN** `application-draw.yaml` définit `tch.draw.results.shared.scheduler.apply_cron`
- **THEN** la valeur est effective dans `ExternalResultsApplyTickScheduler` (pas de fallback hardcodé utilisé)

---

### Requirement: app_user peut lire et écrire dans le schéma batch

`app_user` SHALL disposer de `USAGE` sur le schéma `batch` et de `ALL PRIVILEGES` sur
toutes les tables et séquences du schéma `batch`, de sorte que Spring Batch puisse
initialiser et utiliser son repository.

#### Scenario: Démarrage Spring Batch sans erreur de permission

- **WHEN** l'application démarre avec `spring.batch.job.enabled=false` profil `local-ide`
- **THEN** Spring Batch initialise `MapJobRepository` ou `JdbcJobRepository` sans
  `ERROR: permission denied for schema batch` dans les logs

#### Scenario: Batch job settleDraws exécutable

- **WHEN** `DrawSettleScheduler.tick()` est déclenché
- **THEN** Spring Batch insère dans `batch.BATCH_JOB_INSTANCE` sans erreur de permission
- **AND** l'exécution est enregistrée dans `batch.BATCH_JOB_EXECUTION`

---

### Requirement: DrawResultIngestedEvent replay-safe

L'événement `DrawResultIngestedEvent` traité par `DrawDomainEventListener.onDrawResultIngested`
SHALL être idempotent : rejouer l'événement avec le même `eventId` ne doit pas déclencher
une double invalidation de cache.

#### Scenario: Replay DrawResultIngestedEvent — pas de double éviction

- **WHEN** `DrawResultIngestedEvent` avec `eventId=X` est publié deux fois (ex: broker replay)
- **THEN** `cacheEvictor.evictAll()` est appelé exactement une fois
- **AND** la seconde réception retourne immédiatement après `alreadyProcessed()` → `true`
