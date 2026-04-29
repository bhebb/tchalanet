## Why

L'analyse du 2026-04-27 des configurations YAML des providers de tirages et du catalog
`result_slot` révèle trois classes de problèmes bloquants :

1. **Propriétés YAML orphelines** : `tch.draw.results.mode`, `tch.draw.results.shared.scheduler.active`,
   `tch.draw.results.shared.defaults.*` sont configurées mais n'ont aucun champ Java correspondant
   dans les `@ConfigurationProperties` concernées → silencieusement ignorées au démarrage.

2. **Config providers dupliquée** : `application.yaml` (base) et `application-uslottery.yaml`
   (importé) définissent tous les deux `tch.us-lottery.providers.*.games` avec des codes de jeux
   différents. Spring Boot écrase les listes → les codes legacy dans `application.yaml`
   (`US_NY_TAKE5_EVE`, `US_FL_LOTTO`, `US_GA_CASH3_MID`, `US_GA_CASH3_EVE`) ne feront jamais
   partie du binding effectif mais induisent en erreur les développeurs.

3. **Staging non isolé** : aucune propriété dans `application-staging.yaml` ne désactive
   `tch.us-lottery.enabled` → tous les providers tentent de requêter les APIs externes en staging.

## What Changes

### Partie A — Config YAML providers

- **Supprimer les doublons** de `tch.us-lottery.providers.*` dans `application.yaml` — garder
  uniquement `application-uslottery.yaml` comme source de vérité pour les providers.
- **Ajouter** dans `DrawResultsCommonProperties.Scheduler` les champs `active: boolean` et
  `tickCron: String` pour binder `tch.draw.results.shared.scheduler.active` et `tick_cron`.
- **Ajouter** dans `DrawResultsCommonProperties` une inner class `Defaults` avec `daysBack: int`
  et `maxSlots: int` pour binder `tch.draw.results.shared.defaults.*`.
- **Supprimer** `tch.draw.results.mode: ${TCH_RESULTS_MODE:api_then_fake}` de `application.yaml`
  — ce mode n'est bindé nulle part (**BREAKING** pour les scripts d'env qui positionneraient
  `TCH_RESULTS_MODE`; documenter dans CHANGELOG).
- **Ajouter** dans `application-staging.yaml` : `tch.us-lottery.enabled: false` pour empêcher
  les calls externes en staging par défaut.

### Partie B — ResultSlot alignment

- **Créer migration Flyway** `V45__seed_resultslot_ny_take5.sql` : ajouter le slot `NY_TAKE5_EVE`
  correspondant à `US_NY_TAKE5_EVE` si on décide de le supporter.
- **Décision documentée** : `US_FL_LOTTO` (5 balls, format différent) est hors scope MVP —
  commenter explicitement dans `application-uslottery.yaml`.
- **Créer spec document** `tchalanet-server/docs/DRAW_PROVIDER_RESULTSLOT_MATRIX.md` :
  matrice exhaustive GameCode ↔ result_slot_key ↔ draw_channel_code.

## Capabilities

### New Capabilities

- `draw-provider-config` : Définit les règles d'alignement de la configuration YAML des
  providers de tirages. Toute propriété `tch.draw.*` ou `tch.us-lottery.*` doit avoir un champ
  Java @ConfigurationProperties correspondant. Les doublons YAML sont interdits.
  Le staging désactive les providers externes par défaut.

### Modified Capabilities

_(Aucune capability existante dans openspec/specs/ n'est impactée — les changements sont
de nature infrastructure/config sans impact contrat API.)_

## Impact

### Fichiers modifiés

| Fichier                                                        | Modification                                                                                 |
| -------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| `tchalanet-server/src/main/resources/application.yaml`         | Suppression bloc `tch.us-lottery.*` complet (doublons) + suppression `tch.draw.results.mode` |
| `tchalanet-server/src/main/resources/application-staging.yaml` | Ajout `tch.us-lottery.enabled: false`                                                        |
| `common/config/draw/DrawResultsCommonProperties.java`          | Ajout champs `active`, `tickCron` dans `Scheduler` + inner class `Defaults`                  |

### Fichiers créés

| Fichier                                                                              | Contenu                                           |
| ------------------------------------------------------------------------------------ | ------------------------------------------------- |
| `tchalanet-server/src/main/resources/db/migration/V45__seed_resultslot_ny_take5.sql` | Seed `NY_TAKE5_EVE` result_slot (si scope validé) |
| `tchalanet-server/docs/DRAW_PROVIDER_RESULTSLOT_MATRIX.md`                           | Matrice GameCode ↔ slot ↔ channel                 |

### Breaking changes

| Changement                                               | Impact                                                                                                                                                              |
| -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Suppression `tch.draw.results.mode` / `TCH_RESULTS_MODE` | Scripts d'environnement qui positionnent `TCH_RESULTS_MODE` n'auront plus d'effet — variable inutile à retirer des `.env`                                           |
| Staging `tch.us-lottery.enabled: false`                  | Comportement staging change : les schedulers `ExternalResultsFetchTickScheduler` ne déclenchent plus de fetch externe en staging sans `TCH_US_LOTTERY_ENABLED=true` |

### Non scope

- Refonte du `DrawResultViewPortJdbcAdapter` (champ `pick3` ambigu vs projeté — analyse séparée)
- Ajout de nouveaux providers US
- Migration de `US_FL_LOTTO` (format 5 balls incompatible avec la projection par défaut)
