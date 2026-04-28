## ADDED Requirements

### Requirement: Toute propriété YAML tch.draw.\* est bindée à un champ Java

Chaque clé déclarée sous `tch.draw.*` ou `tch.us-lottery.*` dans tout fichier
`application*.yaml` SHALL avoir un champ correspondant dans une classe
`@ConfigurationProperties` enregistrée au démarrage. Aucune clé orpheline.

#### Scenario: Propriété tch.draw.results.mode absente

- **WHEN** `application.yaml` ne contient plus `tch.draw.results.mode`
- **THEN** le démarrage Spring Boot ne produit aucun avertissement `Unrecognized fields`
  ni `BindValidationException` pour ce préfixe

#### Scenario: Propriétés scheduler bindées dans DrawResultsCommonProperties

- **WHEN** `DrawResultsCommonProperties.Scheduler` possède les champs `active` (boolean) et `tickCron` (String)
- **THEN** `tch.draw.results.shared.scheduler.active` et `tch.draw.results.shared.scheduler.tick_cron`
  sont accessibles via `properties.getScheduler().isActive()` et `properties.getScheduler().getTickCron()`

#### Scenario: Propriétés defaults bindées dans DrawResultsCommonProperties

- **WHEN** `DrawResultsCommonProperties` possède une inner class `Defaults` avec `daysBack` et `maxSlots`
- **THEN** `tch.draw.results.shared.defaults.days_back` et `max_slots` sont accessibles via l'objet de config

---

### Requirement: application.yaml ne contient pas de config tch.us-lottery.providers

`application.yaml` (fichier de base) SHALL déléguer entièrement la configuration
des providers `tch.us-lottery.providers.*` à `application-uslottery.yaml`.
Aucun bloc `tch.us-lottery.providers.*` ne doit aparaître dans `application.yaml`.

#### Scenario: Source unique pour les games providers

- **WHEN** `application.yaml` ne contient plus de section `tch.us-lottery`
- **THEN** le binding effectif de `tch.us-lottery.providers.ny.games` correspond
  exactement aux GameCodes définis dans `application-uslottery.yaml` (sans TAKE5_EVE ni LOTTO)

#### Scenario: Codes legacy absents du binding effectif

- **WHEN** `application.yaml` a supprimé les codes legacy (US_GA_CASH3_MID, US_NY_TAKE5_EVE, US_FL_LOTTO)
- **THEN** `UsLotteryProperties.getProviders().get("ny").getGames()` ne contient
  aucun jeu avec `code = "US_NY_TAKE5_EVE"`

---

### Requirement: Staging désactive les providers externes par défaut

En environnement staging (`SPRING_PROFILES_ACTIVE=staging`), la propriété
`tch.us-lottery.enabled` SHALL valoir `false` sauf si `TCH_US_LOTTERY_ENABLED=true`
est explicitement positionné dans l'environnement.

#### Scenario: Staging démarre sans calls externes

- **WHEN** le profil `staging` est actif et `TCH_US_LOTTERY_ENABLED` n'est pas défini
- **THEN** `UsLotteryProperties.isEnabled()` retourne `false`
- **AND** aucun bean `nyLotteryRestClient`, `floridaLotteryRestClient`, etc. n'est créé
  (`@ConditionalOnProperty` sur `tch.us-lottery.enabled` bloque leur instanciation)

#### Scenario: Activation manuelle en staging

- **WHEN** `TCH_US_LOTTERY_ENABLED=true` est positionné dans l'environnement staging
- **THEN** `UsLotteryProperties.isEnabled()` retourne `true` et les clients REST sont créés

---

### Requirement: Matrice GameCode ↔ ResultSlot documentée et exhaustive

Un fichier `tchalanet-server/docs/DRAW_PROVIDER_RESULTSLOT_MATRIX.md` SHALL lister
pour chaque `result_slot.key` : le provider, les GameCodes configurés dans `source_cfg`,
le `draw_channel.code` correspondant, le statut actif/inactif, et les décisions sur
les GameCodes hors scope (TAKE5_EVE, LOTTO).

#### Scenario: Slot NY_MID documenté

- **WHEN** la matrice est consultée pour le slot `NY_MID`
- **THEN** elle indique : provider=NY, GameCodes=[US_NY_NUM3_MID, US_NY_NUM4_MID],
  draw_channel=HT_NY_MID, timezone=America/New_York, draw_time=14:30, active=true

#### Scenario: Orphelins documentés avec décision

- **WHEN** la matrice liste `US_NY_TAKE5_EVE` et `US_FL_LOTTO`
- **THEN** elle indique explicitement leur statut : "Hors scope MVP —
  format incompatible ou non validé. Pas de result_slot correspondant."
