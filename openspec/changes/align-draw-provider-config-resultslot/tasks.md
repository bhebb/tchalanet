## 1. Nettoyage application.yaml

- [ ] 1.1 Supprimer le bloc `tch.us-lottery` complet de `application.yaml` (NY/FL/GA/TN games + common.holidays dupliqués)
- [ ] 1.2 Supprimer `tch.draw.results.mode: ${TCH_RESULTS_MODE:api_then_fake}` de `application.yaml`
- [ ] 1.3 Ajouter un commentaire dans `application.yaml` : `# Providers US Lottery : voir application-uslottery.yaml`
- [ ] 1.4 Vérifier que `spring.config.import: optional:classpath:application-uslottery.yml` est présent (ne pas supprimer)

## 2. DrawResultsCommonProperties — champs manquants

- [ ] 2.1 Ajouter `private boolean active = true` dans `DrawResultsCommonProperties.Scheduler`
- [ ] 2.2 Ajouter `private String tickCron = "0 */5 * * * *"` dans `DrawResultsCommonProperties.Scheduler`
- [ ] 2.3 Ajouter inner class `Defaults` dans `DrawResultsCommonProperties` avec `private int daysBack = 0` et `private int maxSlots = 200`
- [ ] 2.4 Ajouter l'instance `private Defaults defaults = new Defaults()` dans `DrawResultsCommonProperties`

## 3. Staging isolation

- [ ] 3.1 Ajouter dans `application-staging.yaml` :
  ```yaml
  tch:
    us-lottery:
      enabled: ${TCH_US_LOTTERY_ENABLED:false}
  ```
- [ ] 3.2 Vérifier que `UsLotteryConfig` hérite bien du `@ConditionalOnProperty(prefix = "tch.us-lottery", name = "enabled", havingValue = "true", matchIfMissing = true)` actuel (ne rien changer si ok)

## 4. Matrice GameCode ↔ ResultSlot

- [ ] 4.1 Créer `tchalanet-server/docs/DRAW_PROVIDER_RESULTSLOT_MATRIX.md` avec la matrice complète :
  - 12 slots actifs (NY×2, FL×2, GA×3, TN×1 inactif, TX×4)
  - Pour chaque slot : key, provider, timezone, draw_time, days, GameCodes pick3/pick4, draw_channel
- [ ] 4.2 Documenter les GameCodes orphelins (`US_NY_TAKE5_EVE`, `US_FL_LOTTO`) avec décision "Hors scope MVP"
- [ ] 4.3 Documenter les codes legacy supprimés (`US_GA_CASH3_MID`, `US_GA_CASH3_EVE`, `US_TN_PICK3_MID`)

## 5. Spec document audit

- [ ] 5.1 Créer `tchalanet-server/docs/audit/spec-draw-provider-config-resultslot-2026-04-27.md` avec les sections complètes demandées :
  - Section "Propriétés YAML manquantes ou mal configurées"
  - Section "ResultSlots manquants ou orphelins" + SQL d'insertion conditionnel
  - Section "Changes YAML diff avant/après"
  - Checklist DoD

## 6. Validation

- [ ] 6.1 `./mvnw spring-boot:run -Dspring-boot.run.profiles=local-ide` → démarrage sans `BindValidationException` ni avertissement `Unrecognized fields` pour `tch.draw.*`
- [ ] 6.2 Écrire un test `DrawResultsCommonPropertiesBindingTest` : vérifier que `active`, `tickCron`, `defaults.daysBack`, `defaults.maxSlots` se bindent depuis YAML
- [ ] 6.3 En profil staging : `UsLotteryProperties.isEnabled()` retourne `false` sans `TCH_US_LOTTERY_ENABLED`
- [ ] 6.4 `UsLotteryProperties.getProviders().get("ny").getGames()` → 4 entrées (pas de TAKE5_EVE)
