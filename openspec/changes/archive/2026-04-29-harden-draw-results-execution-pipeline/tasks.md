## Status: DRAFT

## 1. Configuration runtime

- [x] 1.1 Corriger les imports Spring des fichiers `application-draw.yaml` et `application-uslottery.yaml`
- [x] 1.2 Inventorier les doublons `tch.draw.results.*` entre les fichiers YAML
- [x] 1.3 Inventorier les doublons `tch.us-lottery.*` entre les fichiers YAML
- [x] 1.4 Conserver une seule source de vérité par propriété
- [x] 1.5 Aligner les classes `@ConfigurationProperties` avec l'arborescence réellement utilisée
- [x] 1.6 Ajouter un test de binding de propriétés pour `draw`, `drawresult` et `uslottery`

## 2. Fetch / projection / apply

- [x] 2.1 Cartographier le flow `result_slot.source_cfg -> provider client -> draw_result`
- [x] 2.2 Introduire une résolution explicite de `projection_cfg` par slot
- [x] 2.3 Conserver un fallback global documenté pour la projection Haïti
- [x] 2.4 Aligner `FetchExternalResultsWindowCommandHandler` sur cette résolution
- [x] 2.5 Aligner `RecordManualDrawResultCommandHandler` sur cette résolution
- [x] 2.6 Ajouter des tests pour slot actif/inactif, slot sans config exploitable, payload incomplet, projection par défaut et projection spécifique slot

## 3. Batchs et schedulers

- [x] 3.1 Aligner les propriétés cron/config du fetch scheduler
- [x] 3.2 Aligner les propriétés cron/config du apply scheduler
- [x] 3.3 Vérifier que `fetch` et `apply` utilisent les bonnes gates
- [x] 3.4 Corriger `DrawResultsOpsController` pour que `manual` et `override` utilisent leurs gates dédiées
- [x] 3.5 Dé-hardcoder `DrawSettleScheduler` pour dériver les providers depuis la configuration/runtime registry
- [x] 3.6 Vérifier les limites batch et la logique tenant-scoped/global de chaque scheduler
- [x] 3.7 Ajouter des tests unitaires sur les schedulers critiques

## 4. Providers uslottery

- [x] 4.1 Vérifier la cohérence entre `result_slot.provider` et `UsLotteryProviderClient.provider()`
- [x] 4.2 Vérifier la cohérence entre `source_cfg.*.external_game_code` et les channel codes réellement supportés
- [x] 4.3 Vérifier la cohérence entre `source_cfg.*.external_key` et les clients NY/FL/GA/TN/TX
- [x] 4.4 Vérifier les paths, headers et timezones des providers bindés depuis YAML
- [ ] 4.5 Ajouter des tests/adapters ciblés sur les clients providers actifs

## 5. Documentation et clarté du flow

- [x] 5.1 Mettre à jour `DOMAIN_DRAW.md`
- [x] 5.2 Mettre à jour `DOMAIN_DRAWRESULT.md`
- [x] 5.3 Mettre à jour `DOMAIN_USLOTTERY.md`
- [x] 5.4 Créer ou compléter `DOMAIN_DRAWCHANNEL.md` près du code
- [x] 5.5 Mettre à jour `FEATURE_PUBLICDRAW.md`
- [x] 5.6 Mettre à jour `tchalanet-docs/docs/02-functional/flows/draw-execution.md`
- [x] 5.7 Documenter explicitement le pipeline final generate/open/close/fetch/apply/settle

## 6. Vérification finale

- [ ] 6.1 Vérifier que le contexte Spring charge la config attendue
- [x] 6.2 Vérifier que fetch/apply/settle restent cohérents après nettoyage de config
- [x] 6.3 Vérifier que la doc backend et la doc fonctionnelle racontent le même flow
- [x] 6.4 Exécuter le build/tests ciblés backend
