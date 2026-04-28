# spec-draw-provider-config-resultslot-2026-04-27.md

> **Type** : Spec opérationnelle — audit configuration providers de tirages + ResultSlot
> **Date** : 2026-04-27
> **Scope** : `tchalanet-server` > **Domaines** : `core.drawresult`, `core.uslottery`, `catalog.resultslot` > **Auteur** : Généré par analyse automatique du repo

---

## Résumé

Audit complet de l'alignement entre les propriétés YAML de configuration des providers
de tirages (`tch.draw.*`, `tch.us-lottery.*`) et leurs classes `@ConfigurationProperties` Java,
ainsi que de la cohérence entre les GameCodes configurés et les entrées `result_slot` en base.

---

## 1. Propriétés YAML manquantes ou mal configurées

### 1.1 Propriétés orphelines (configurées mais non bindées)

| Propriété YAML                                | Fichier                    | Classe attendue                         | Statut                                                                                                          |
| --------------------------------------------- | -------------------------- | --------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `tch.draw.results.mode`                       | `application.yaml:143`     | `DrawResultsCommonProperties`           | ❌ **MORT** — aucun champ Java, aucun usage dans le code                                                        |
| `tch.draw.results.shared.scheduler.active`    | `application-draw.yaml:16` | `DrawResultsCommonProperties.Scheduler` | ❌ **Non bindé** — champ `active` absent de la classe                                                           |
| `tch.draw.results.shared.scheduler.tick_cron` | `application-draw.yaml:17` | `DrawResultsCommonProperties.Scheduler` | ⚠️ **Partiellement utilisé** — lu via SpEL `@Scheduled(cron = "${...}")` mais pas accessible via l'objet config |
| `tch.draw.results.shared.defaults.days_back`  | `application-draw.yaml:12` | `DrawResultsCommonProperties`           | ❌ **Non bindé** — inner class `Defaults` absente                                                               |
| `tch.draw.results.shared.defaults.max_slots`  | `application-draw.yaml:13` | `DrawResultsCommonProperties`           | ❌ **Non bindé** — same                                                                                         |

### 1.2 Incohérence valeurs par défaut

| Propriété                                                      | Défaut YAML                     | Défaut Java                        | Assessment                                                                |
| -------------------------------------------------------------- | ------------------------------- | ---------------------------------- | ------------------------------------------------------------------------- |
| `tch.draw.results.shared.scheduler.due.max_minutes_after_draw` | `60` (application-draw.yaml:20) | `25` (DrawResultsCommonProperties) | ⚠️ YAML gagne à runtime (60) mais le développeur lisant la classe voie 25 |
| `tch.draw.results.shared.scheduler.cooldown_minutes`           | `10` (application-draw.yaml:23) | `20` (DrawResultsCommonProperties) | ⚠️ Même problème : 10 à runtime, 20 dans le code                          |

### 1.3 Config providers dupliquée

`application.yaml` définit `tch.us-lottery.providers.ny.games`, `.fl.games`, `.ga.games`, `.tn.games`
ET `application-uslottery.yaml` définit les mêmes paths. Spring Boot remplace les listes lors
des imports → **`application-uslottery.yaml` gagne toujours** — les values dans `application.yaml`
sont effectivement mortes mais masquent la source canonique.

**Codes dans `application.yaml` qui ne seront jamais bindés :**

| Provider | Code legacy (mort) | Raison                                                              |
| -------- | ------------------ | ------------------------------------------------------------------- |
| NY       | `US_NY_TAKE5_EVE`  | Absent de application-uslottery.yaml                                |
| FL       | `US_FL_LOTTO`      | Absent de application-uslottery.yaml (format 5 boules incompatible) |
| GA       | `US_GA_CASH3_MID`  | Code remplacé par `US_GA_CASH3_1229` (horaire)                      |
| GA       | `US_GA_CASH3_EVE`  | Code remplacé par `US_GA_CASH3_1859` (horaire)                      |
| TN       | `US_TN_PICK3_MID`  | Code remplacé par `US_TN_CASH3_1255`                                |
| TN       | `US_TN_PICK3_EVE`  | Code remplacé par `US_TN_CASH4_1255`                                |

### 1.4 Timezone NY app-token

`app-token: ${TCH_US_LOTTERY_NY_APP_TOKEN:}` → défaut **vide**. Sans token, NY SOCRATA API
applique des rate limits agressifs (1000 req/jour). En production, `TCH_US_LOTTERY_NY_APP_TOKEN`
doit être renseigné dans Doppler.

### 1.5 Staging sans isolation

`application-staging.yaml` ne désactive pas `tch.us-lottery.enabled` → en staging,
tous les providers tentent les appels API externes avec `enabled=true` (défaut).

---

## 2. ResultSlots manquants ou orphelins

### 2.1 Matrice complète GameCode ↔ result_slot (V34 seed, état 2026-04-27)

| ResultSlot Key | Provider | Timezone         | Draw Time | Days    | Pick3 GameCode   | Pick4 GameCode    | Active        | Draw Channel |
| -------------- | -------- | ---------------- | --------- | ------- | ---------------- | ----------------- | ------------- | ------------ |
| `NY_MID`       | NY       | America/New_York | 14:30     | MON-SUN | US_NY_NUM3_MID   | US_NY_NUM4_MID    | ✅            | HT_NY_MID    |
| `NY_EVE`       | NY       | America/New_York | 22:30     | MON-SUN | US_NY_NUM3_EVE   | US_NY_NUM4_EVE    | ✅            | HT_NY_EVE    |
| `FL_MID`       | FL       | America/New_York | 13:30     | MON-SUN | US_FL_PICK3_MID  | US_FL_PICK4_MID   | ✅            | HT_FL_MID    |
| `FL_EVE`       | FL       | America/New_York | 22:45     | MON-SUN | US_FL_PICK3_EVE  | US_FL_PICK4_EVE   | ✅            | HT_FL_EVE    |
| `GA_MID`       | GA       | America/New_York | 12:29     | MON-SUN | US_GA_CASH3_1229 | US_GA_CASH4_1229  | ✅            | HT_GA_MID    |
| `GA_EVE`       | GA       | America/New_York | 18:59     | MON-SUN | US_GA_CASH3_1859 | US_GA_CASH4_1859  | ✅            | HT_GA_EVE    |
| `GA_LATE`      | GA       | America/New_York | 23:34     | MON-SUN | US_GA_CASH3_2334 | US_GA_CASH4_2334  | ✅            | HT_GA_LATE   |
| `TN_MID`       | TN       | America/Chicago  | 12:55     | MON-SAT | US_TN_CASH3_1255 | US_TN_CASH4_1255  | ⏸️ (inactive) | HT_TN_MID    |
| `TX_1000`      | TX       | America/Chicago  | 10:00     | MON-SAT | US_TX_PICK3_1000 | US_TX_DAILY4_1000 | ✅            | HT_TX_1000   |
| `TX_1227`      | TX       | America/Chicago  | 12:27     | MON-SAT | US_TX_PICK3_1227 | US_TX_DAILY4_1227 | ✅            | HT_TX_1227   |
| `TX_1800`      | TX       | America/Chicago  | 18:00     | MON-SAT | US_TX_PICK3_1800 | US_TX_DAILY4_1800 | ✅            | HT_TX_1800   |
| `TX_2212`      | TX       | America/Chicago  | 22:12     | MON-SAT | US_TX_PICK3_2212 | US_TX_DAILY4_2212 | ✅            | HT_TX_2212   |

**Total actif** : 11 slots actifs + 1 inactif (TN_MID)

### 2.2 GameCodes orphelins (configurés mais sans result_slot)

| GameCode          | Provider | Raison                                                                 | Décision                                                                |
| ----------------- | -------- | ---------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| `US_NY_TAKE5_EVE` | NY       | Format Take 5 = 5 boules (39) — projection lot1/lot2/lot3 incompatible | **Hors scope MVP** — pas de result_slot à créer sans refonte projection |
| `US_FL_LOTTO`     | FL       | Format Lotto = 6 boules (53) — incompatible                            | **Hors scope MVP** — idem                                               |

### 2.3 SQL conditionnel — si NY_TAKE5_EVE validé scope futur

```sql
-- À exécuter UNIQUEMENT si US_NY_TAKE5_EVE est activé dans application-uslottery.yaml
-- Nécessite d'abord une décision sur le format de projection (5 boules)
INSERT INTO result_slot (
  id, key, provider, timezone, draw_time, days_of_week,
  active, sort_order, source_cfg, projection_cfg,
  created_at, updated_at, version
)
VALUES (
  gen_random_uuid(),
  'NY_TAKE5_EVE',
  'NY',
  'America/New_York',
  '22:30'::time,
  'MON-SUN',
  false,   -- inactive par défaut jusqu'à validation projection
  12,
  '{"take5": {"external_game_code": "US_NY_TAKE5_EVE", "external_key": "TAKE 5"}}'::jsonb,
  '{"version": 1, "rule_set": "TAKE5_DEFAULT", "rules": {}}'::jsonb,   -- à définir
  now(), now(), 0
)
ON CONFLICT (key) DO NOTHING;

-- draw_channel correspondant (default tenant)
INSERT INTO draw_channel (
  id, tenant_id, code, name,
  timezone, draw_time, cutoff_sec, days_of_week,
  active, sort_order, external_provider, result_slot_id,
  flags, notes, created_at, updated_at, version
)
SELECT
  gen_random_uuid(),
  '00000000-0000-0000-0000-000000000003'::uuid,
  'HT_NY_TAKE5_EVE', 'Haïti • New York • Take5 Evening',
  'America/New_York', '22:30'::time, 300, 'MON-SUN',
  false,   -- inactive
  12, 'NY', rs.id,
  '{}'::jsonb, 'Hors scope MVP — activer après validation projection Take5',
  now(), now(), 0
FROM result_slot rs
WHERE rs.key = 'NY_TAKE5_EVE'
ON CONFLICT (tenant_id, code) DO NOTHING;
```

---

## 3. Changes YAML diff avant/après

### 3.1 `application.yaml` — suppressions

```diff
  tch:
    cache:
      redis:
        enabled: false
-
-   # Rate-limiting for public ticket endpoints (/public/tickets/**)
    public:
      ...
    draw:
      results:
-       # kept alias to old props for backwards compat with env names
-       mode: ${TCH_RESULTS_MODE:api_then_fake}
        generation:
    ...
-
-   # US Lottery API configuration
-   us-lottery:
-     common:
-       holidays:
-        - "01-01"   # New Year
-        - "12-25"   # Christmas
-     providers:
-       ny:
-         enabled: ${TCH_US_NY_LOTTERY_ENABLED:true}
-         base-url: ${TCH_US_LOTTERY_NY_BASEURL:...}
-         ...
-       fl: ...
-       ga: ...
-       tn: ...
+   # US Lottery providers : voir application-uslottery.yaml (importé via spring.config.import)
```

### 3.2 `application-staging.yaml` — ajout

```diff
  app:
    cors:
      allowed-origins: ...
    apiBaseUrl: ...

  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            issuer-uri: https://auth.stg.tchalanet.com/realms/tchalanet

+ tch:
+   us-lottery:
+     # Désactivé en staging par défaut — activer via TCH_US_LOTTERY_ENABLED=true si besoin
+     enabled: ${TCH_US_LOTTERY_ENABLED:false}
```

### 3.3 `DrawResultsCommonProperties.java` — ajouts

```diff
  public static class Scheduler {
    private Due due = new Due();
    private int cooldownMinutes = 20;
+   private boolean active = true;
+   // tickCron mirror of @Scheduled(cron = "${tch.draw.results.shared.scheduler.tick_cron:...}")
+   private String tickCron = "0 */5 * * * *";

    ...
  }
+
+ @Getter
+ @Setter
+ public static class Defaults {
+   private int daysBack = 0;
+   private int maxSlots = 200;
+ }
+
+ private Defaults defaults = new Defaults();
```

---

## 4. Checklist DoD

```
[ ] Suppression tch.draw.results.mode
    [ ] Propriété absente de application.yaml
    [ ] grep TCH_RESULTS_MODE dans tchalanet-infra → 0 résultats

[ ] Suppression doublons tch.us-lottery dans application.yaml
    [ ] Aucune section tch.us-lottery dans application.yaml
    [ ] application-uslottery.yaml toujours importé (spring.config.import présent)

[ ] DrawResultsCommonProperties champs ajoutés
    [ ] active: boolean présent dans Scheduler
    [ ] tickCron: String présent dans Scheduler
    [ ] Defaults inner class présente avec daysBack et maxSlots
    [ ] Test DrawResultsCommonPropertiesBindingTest vert

[ ] Staging isolation
    [ ] tch.us-lottery.enabled: ${TCH_US_LOTTERY_ENABLED:false} dans application-staging.yaml
    [ ] ./mvnw -Dspring.profiles.active=staging spring-boot:run → UsLotteryConfig non instancié

[ ] ResultSlot documentation
    [ ] DRAW_PROVIDER_RESULTSLOT_MATRIX.md créé dans docs/
    [ ] Matrice 12 slots complète
    [ ] Orphelins documentés (TAKE5_EVE, LOTTO)

[ ] Démarrage local sans erreur
    [ ] ./mvnw spring-boot:run -Dspring-boot.run.profiles=local-ide → pas de BindValidationException
    [ ] GET /api/v1/public/draws?provider=NY → 200 avec résultats
    [ ] fetch tick scheduler déclenche ExternalResultsFetchTickScheduler → log "action=FETCH slotKey=NY_MID"

[ ] CHANGELOG.md mention breaking change
    [ ] TCH_RESULTS_MODE documenté comme variable obsolète à supprimer
    [ ] Staging tch.us-lottery.enabled=false documenté
```
