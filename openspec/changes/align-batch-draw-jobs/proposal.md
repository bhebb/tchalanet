## Why

L'audit des schedulers et batch jobs liés aux tirages (2026-04-27) révèle 4 anomalies
actionnables réparties sur 3 classes :

1. **`DrawProvisionalWatchdogScheduler`** — ni `@SchedulerLock` ni `BatchGate.enabled()` :
   en multi-instance, plusieurs pods exécutent simultanément ce watchdog ; aucune gate
   ne permet de le désactiver sans redéploiement.

2. **`DrawSettleScheduler`** — cron expression hardcodée (`"0 */5 * * * *"`) : impossible
   de modifier la fréquence de settle sans redéployer ; contredit la convention
   d'externalisation des crons.

3. **`ExternalResultsApplyTickScheduler`** — cron path incohérent : la propriété lue est
   `tch.draw.results.scheduler.apply_cron` (non définie dans les YAMLs) alors que la
   convention du projet est `tch.draw.results.shared.scheduler.*`. La valeur par défaut
   hardcodée prend toujours effet — la propriété YAML est silencieusement ignorée.

4. **`V42__spring_batch_schema.sql`** — aucun `GRANT` sur le schéma `batch` et ses tables
   pour `app_user` : au démarrage Spring Batch tente d'écrire dans `batch.BATCH_JOB_INSTANCE`
   et obtient `ERROR: permission denied for schema batch`.

## What Changes

- **Ajouter** `@SchedulerLock` + `BatchGate` dans `DrawProvisionalWatchdogScheduler`
- **Externaliser** le cron de `DrawSettleScheduler` → `${tch.draw.settle.cron:0 */5 * * * *}`
- **Corriger** le cron path de `ExternalResultsApplyTickScheduler` →
  `${tch.draw.results.shared.scheduler.apply_cron:30 */5 * * * *}`
- **Ajouter** les `GRANT` manquants dans `V42` via une migration corrective `V44__batch_grants.sql`
- **Créer le document d'audit** `spec-batch-draw-alignment-2026-04-27.md`

## Capabilities

### New Capabilities

- `batch-draw-scheduler-safety` : Règles d'exhaustivité applicables à tous les schedulers
  liés aux tirages. Tout `@Scheduled` dans `core.draw.*` ou `core.drawresult.*` DOIT avoir :
  `@SchedulerLock`, `BatchGate.enabled()`, cron externalisé, `Clock` injecté.

### Modified Capabilities

_(Aucune spec existante dans openspec/specs/ impactée — changements purement techniques.)_

## Impact

### Code modifié

| Fichier                                                            | Changement                                                             |
| ------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| `core/draw/infra/scheduler/DrawProvisionalWatchdogScheduler.java`  | + `@SchedulerLock`, + `BatchGate`, + `Clock`, cron déjà externalisé ✅ |
| `core/draw/infra/scheduler/DrawSettleScheduler.java`               | cron externalisé `${tch.draw.settle.cron:0 */5 * * * *}`               |
| `core/draw/infra/scheduler/ExternalResultsApplyTickScheduler.java` | cron path corrigé → `tch.draw.results.shared.scheduler.apply_cron`     |

### Migration créée

| Fichier                 | Contenu                                                                                        |
| ----------------------- | ---------------------------------------------------------------------------------------------- |
| `V44__batch_grants.sql` | `GRANT USAGE ON SCHEMA batch`, `GRANT ALL ON ALL TABLES/SEQUENCES IN SCHEMA batch TO app_user` |

### application-draw.yaml

Ajouter `apply_cron` dans la section `tch.draw.results.shared.scheduler` + `settle_cron` dans
une nouvelle section `tch.draw.settle`.

### Non scope

- Remplacer le cooldown in-memory de `ExternalResultsFetchTickScheduler` par Redis
  (problème mineur, ShedLock compense en multi-instance)
- Migration du `DrawSettleJobConfig` vers Spring Batch partitioned step
- Ajout de nouveaux batch jobs draws
