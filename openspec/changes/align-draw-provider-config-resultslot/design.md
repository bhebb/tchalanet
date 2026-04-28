## Context

### État actuel — Propriétés orphelines

**`tch.draw.results.mode`** est déclaré dans `application.yaml` ligne 143 :

```yaml
tch:
  draw:
    results:
      mode: ${TCH_RESULTS_MODE:api_then_fake}
```

Aucune classe Java ne bind ce préfixe. La propriété est silencieusement ignorée au démarrage.
Aucun code ne référence `api_then_fake`, `fake`, ou `resultsMode` → **propriété morte**.

**`tch.draw.results.shared.scheduler.active`** et **`tick_cron`** sont dans `application-draw.yaml` :

```yaml
tch:
  draw:
    results:
      shared:
        scheduler:
          active: ${TCH_RESULTS_SCHEDULER_ACTIVE:true} # non bindé
          tick_cron: ${TCH_RESULTS_TICK_CRON:0 */5 * * * *} # non bindé dans la classe
```

`DrawResultsCommonProperties.Scheduler` ne possède pas les champs `active` et `tickCron`.
Le scheduler lit `tick_cron` via SpEL direct `@Scheduled(cron = "${tch.draw.results.shared.scheduler.tick_cron:0 */5 * * * *}")` — ce qui fonctionne, mais le champ n'est pas accessible via l'objet de config.

**`tch.draw.results.shared.defaults.*`** (`days_back`, `max_slots`) sont dans `application-draw.yaml` mais absents de `DrawResultsCommonProperties`.

### État actuel — Doublons application.yaml vs application-uslottery.yaml

`application.yaml` définit des games pour NY, FL, GA, TN dans `tch.us-lottery.providers.*`.
`application-uslottery.yaml` (importé après) définit les MÊMES paths avec des valeurs différentes.

Spring Boot remplace les listes lors d'imports successifs → `application-uslottery.yaml` gagne.
Les valeurs dans `application.yaml` sont effectivement mortes mais induisent en erreur :

| Provider | Codes dans application.yaml (morts)                         | Codes dans application-uslottery.yaml (actifs)              |
| -------- | ----------------------------------------------------------- | ----------------------------------------------------------- |
| NY       | US_NY_NUM3_MID/EVE, US_NY_NUM4_MID/EVE, **US_NY_TAKE5_EVE** | NUM3_MID/EVE, NUM4_MID/EVE (sans TAKE5)                     |
| FL       | PICK3/PICK4 MID/EVE, **US_FL_LOTTO**                        | PICK3/PICK4 MID/EVE (sans LOTTO)                            |
| GA       | **US_GA_CASH3_MID, US_GA_CASH3_EVE** (codes legacy)         | CASH3_1229/1859/2334, CASH4_1229/1859/2334 (codes horaires) |
| TN       | US_TN_PICK3_MID/EVE (codes anciens)                         | US_TN_CASH3_1255, US_TN_CASH4_1255                          |

### État actuel — ResultSlot alignment

Matrice complète GameCode ↔ result_slot_key (V34 seed) :

| Slot Key  | Provider | GameCodes bindés (source_cfg)       | Status            |
| --------- | -------- | ----------------------------------- | ----------------- |
| `NY_MID`  | NY       | US_NY_NUM3_MID, US_NY_NUM4_MID      | ✅                |
| `NY_EVE`  | NY       | US_NY_NUM3_EVE, US_NY_NUM4_EVE      | ✅                |
| `FL_MID`  | FL       | US_FL_PICK3_MID, US_FL_PICK4_MID    | ✅                |
| `FL_EVE`  | FL       | US_FL_PICK3_EVE, US_FL_PICK4_EVE    | ✅                |
| `GA_MID`  | GA       | US_GA_CASH3_1229, US_GA_CASH4_1229  | ✅                |
| `GA_EVE`  | GA       | US_GA_CASH3_1859, US_GA_CASH4_1859  | ✅                |
| `GA_LATE` | GA       | US_GA_CASH3_2334, US_GA_CASH4_2334  | ✅                |
| `TN_MID`  | TN       | US_TN_CASH3_1255, US_TN_CASH4_1255  | ✅ (active=false) |
| `TX_1000` | TX       | US_TX_PICK3_1000, US_TX_DAILY4_1000 | ✅                |
| `TX_1227` | TX       | US_TX_PICK3_1227, US_TX_DAILY4_1227 | ✅                |
| `TX_1800` | TX       | US_TX_PICK3_1800, US_TX_DAILY4_1800 | ✅                |
| `TX_2212` | TX       | US_TX_PICK3_2212, US_TX_DAILY4_2212 | ✅                |

**Orphelins** (GameCodes configurés sans result_slot correspondant) :

- `US_NY_TAKE5_EVE` — dans application.yaml section NY ; absent de V34 et de application-uslottery.yaml
- `US_FL_LOTTO` — dans application.yaml section FL ; absent de V34 (format 5 boules incompatible)
- `US_GA_CASH3_MID` / `US_GA_CASH3_EVE` — codes legacy remplacés par codes horaires

## Goals / Non-Goals

**Goals:**

- Toute propriété `tch.draw.*` déclarée en YAML a un champ Java `@ConfigurationProperties` correspondant
- `application.yaml` ne contient plus de config `tch.us-lottery.providers.*` (dédupliquée)
- Staging ne requête pas les APIs externes par défaut
- Matrice GameCode ↔ ResultSlot documentée en source unique

**Non-Goals:**

- Refonte du `DrawResultViewPortJdbcAdapter` et de ses champs `pick3`/`two_digits`
- Activation de `US_NY_TAKE5_EVE` ou `US_FL_LOTTO` (scope MVP non validé)
- Migration vers un système de config dynamique (DB-driven providers)

## Decisions

### D1 — Supprimer `tch.draw.results.mode` (propriété morte)

La propriété `tch.draw.results.mode` n'est consommée par aucun bean Java. Aucun test
ne la référence. La supprimer évite la confusion et alerte si `TCH_RESULTS_MODE` est
encore positionné dans les `.env` (Spring émettra un avertissement de propriété inconnue).

**Alternative écartée** : Ajouter un champ dans `DrawResultsCommonProperties`. Non retenu car
aucun code ne l'utilise — implémenter une fonctionnalité morte n'a pas de valeur.

### D2 — Ajouter `active` et `tickCron` dans `DrawResultsCommonProperties.Scheduler`

Le `@Scheduled(cron = "${tch.draw.results.shared.scheduler.tick_cron:...}")` fonctionne
mais rend impossible le test unitaire de la valeur configurée. En ajoutant `tickCron` à la
classe, on pourrait écrire un test de configuration.

`active` permet au code de vérifier `schedulerProps.getScheduler().isActive()` plutôt que
de lire `DrawResultsProperties.isActive()` (deux endroits distincts qui tracent la même chose).

### D3 — Nettoyer application.yaml sans supprimer application-uslottery.yaml

`application-uslottery.yaml` est le fichier canonique pour les providers. `application.yaml`
doit uniquement contenir les propriétés globales de l'application (`spring.*`, `app.*`, `tch.draw.*`
sans les providers).

**Ordre d'import préservé** : `import: optional:classpath:application-uslottery.yml` reste en place.

### D4 — `tch.us-lottery.enabled: false` en staging via env var

Plutôt qu'une valeur hardcodée `false` dans `application-staging.yaml`, utiliser une env var
avec défaut `false` pour permettre l'activation manuelle en staging si besoin :

```yaml
tch:
  us-lottery:
    enabled: ${TCH_US_LOTTERY_ENABLED:false} # défaut false en staging
```

## Risks / Trade-offs

**[Risque] Suppression `TCH_RESULTS_MODE` — scripts d'env existants**
→ Mitigation : grep sur tous les `.env.*` et `Makefile` de `tchalanet-infra` pour vérifier
que `TCH_RESULTS_MODE` n'est pas positionné. Documenter dans `CHANGELOG.md`.

**[Risque] `DrawResultsCommonProperties` changement de `maxMinutesAfterDraw` 25→60**
→ En YAML : `max_minutes_after_draw: 60` vs classe Java : `private int maxMinutesAfterDraw = 25`.
Si la config YAML est active (fichier importé), la valeur 60 gagne. Aucun risque de régression.

**[Trade-off] `application.yaml` allégé**
→ Les développeurs habitués à chercher les games NY/FL dans `application.yaml` ne les trouveront
plus. Le commentaire `# voir application-uslottery.yaml` guide la recherche.

## Migration Plan

1. Ajouter les champs dans `DrawResultsCommonProperties` → build + tests verts
2. Supprimer le bloc `tch.us-lottery.*` de `application.yaml` + `tch.draw.results.mode` → build
3. Ajouter `tch.us-lottery.enabled: ${TCH_US_LOTTERY_ENABLED:false}` dans `application-staging.yaml`
4. Créer `DRAW_PROVIDER_RESULTSLOT_MATRIX.md`
5. Merge → vérifier démarrage local sans `BindValidationException`

**Rollback** : `git revert` — les deux fichiers YAML sont indépendants, rollback par fichier possible.

## Open Questions

_Aucune — décisions toutes tranchées._
