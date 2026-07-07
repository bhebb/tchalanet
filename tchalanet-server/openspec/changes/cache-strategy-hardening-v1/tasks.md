# Tasks

## Phase 0 — Spec alignment
- [x] Corriger `cache.md` : liens réels, format de nom `:` + note legacy, section "Default TTL".
- [ ] Ajouter la règle normative : aucun cache ne participe à une décision d'argent, de limite
      consommée ou de sécurité POS.
- [ ] Décider officiellement la convention de noms (`:` canonique) et l'inscrire dans `cache.md`.

## Phase 1 — Bugs fonctionnels P0
- [x] `catalog:drawchannel:calendar_rows` : ajouter l'éviction sur tous les writes DrawChannel /
      DrawChannelGame (Admin ×8, Game ×4, Provisioning ×1). Note : les annotations `@CacheEvict`
      exigent des littéraux → pas d'array `ALL` possible, on liste les constantes String.
- [x] Supprimer les caches morts `catalog:drawchannel:by_result_slot_*` (jamais peuplés par
      `@Cacheable`) — constantes + références d'éviction retirées.
- [x] Câbler `core.drawresult.*` avec de vrais `@Cacheable` : `findViewById` (BY_ID) et
      `findByResultSlotIdAndOccurredAt` (ID_BY_SLOT_OCCURRED) sur `DrawResultJdbcReaderAdapter`.
      Éviction déjà en place (writer → `AfterCommit.run(evictAll)` pour upsert/confirm/override).
      `LATEST_BY_SLOT` retiré (mort). `DrawResultCacheNames` + `DrawResultCacheSpecProvider`
      (TTL court L1 30s / L2 2min, chemin settlement).
- [ ] Tests d'intégration : write config → cache évincé → lecture reflète la nouvelle valeur
      (drawchannel calendar_rows + drawresult by_id/id_by_slot_occurred).

## Phase 2 — CacheSpecProviders manquants
- [x] Specs pour plan, game, theme, resultslot, pagemodel, drawchannel, settings, i18n, tenant.
- [x] Appliquer les TTL par tier (A: 30 min / 12 h · B: 15 min / 6 h · pagemodel SEARCH court 2/5 min).
- [x] Documenter les defaults L1 (5 min runtime vs 10 min `CacheSpec`) — note ajoutée dans `cache.md`.
- [ ] Test de démarrage qui échoue si un cache `@Cacheable` connu n'a pas de `CacheSpecProvider`
      (nécessite un scan des annotations `@Cacheable` — à faire dans la passe test).

## Phase 3 — Caches runtime
- [ ] `platform:tenanttheme:runtime` (clé tenant:mode) + éviction sur `TenantThemeAdminService`.
- [ ] `platform:tenantgame:runtime` + `:projection` + éviction sur `TenantGameAdminService`.
- [ ] `core:limit:policy_by_scope` — définition de règle uniquement, jamais les compteurs consommés.
- [ ] `core:sellerterminal:profile_by_id` (`getMe`), TTL court, interdit pour validation de vente.

## Phase 4 — Éviction after-commit
- [ ] Vérifier si l'éviction actuelle est réellement post-commit (pas seulement même transaction).
- [ ] Writes critiques : `AfterCommit.run(...)` ou evictor infra post-commit.
- [ ] Tests rollback : transaction rollback → cache non évincé / non repeuplé avec état faux.

## Phase 5 — Ops / kill-switch
- [ ] `CacheToggleProvider` (abstraction) + stockage runtime via settings.
- [ ] `@Cacheable(condition = "@cacheToggle.on('name')")` sur les caches désactivables.
- [ ] Ops : list toggles, disable, enable (+ clear existant) — SUPER_ADMIN, audité.
- [ ] Groupes Ops réels : catalog, tenant-config, pricing, draw, drawresult, public.

## Phase 6 — Redis L2 readiness (sérialisation)
- [x] Serializer : retrait module typed-id scalaire (fix `serializeWithType`).
- [x] `enableSpringCacheNullValueSupport()` (NullValue round-trip).
- [x] Writer forçant le type racine `Object` + `WRAPPER_ARRAY` (collections `final` typées).
- [x] `disable(FAIL_ON_UNKNOWN_PROPERTIES)` (résilience schema drift).
- [x] Test de round-trip `RedisCacheSerializerRoundTripTest` (typed-id, JsonNode, List, NullValue).
- [x] Observabilité dev : log `CombinedCache` L1/L2/DB + loggers Redis DEBUG (profil local-ide-redis).
- [ ] Mapper les `JsonNode` cachés (`ResultSlotView.sourceCfg/projectionCfg`,
      `GameSummaryView.flags`) vers des structures stables — un `JsonNode` null revient en `NullNode`.
- [ ] Vérifier activation prod/staging de `tch.cache.redis.enabled` (infra).
