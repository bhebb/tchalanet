# Tasks

## Phase 0 — Spec alignment
- [x] Corriger `cache.md` : liens réels, format de nom `:` + note legacy, section "Default TTL".
- [x] Ajouter la règle normative : aucun cache ne participe à une décision d'argent, de limite
      consommée ou de sécurité POS (cache.md §3 MUST NOT).
- [x] Convention de noms `:` canonique inscrite dans `cache.md` §6 (+ note legacy).

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
- [x] `platform:tenanttheme:runtime` (clé tenant:mode) + éviction (allEntries) sur les 3 écritures
      `TenantThemeAdminService` (applyPreset, deactivate, updateSettings). Vue sûre (Map, pas JsonNode).
- [x] `platform:tenantgame:runtime` + `:projection` + éviction (allEntries) sur les 4 écritures
      `TenantGameAdminService` (enable, disable, updateSettings, updateBetOptionConfig).
- [x] limit — **NO-CACHE by design** (décision validée). `EvaluateLimitPolicyQueryHandler` lit la
      règle (`assignments.listActiveForTargets(scopes, now)`) sur le chemin de décision, mêlée à
      l'exposition live ; clé `scopes+now` = ~aucun hit ; règle périmée = décision d'argent fausse.
- [x] sellerterminal — **NO-CACHE by design** (décision validée). `getMe` et `saleValidation`
      partagent `reader.getRequired`/`findById` → cacher exposerait la validation de vente (terminal
      suspendu validant encore) ; `findByExternalSubject` = binding d'auth. Option `getMe`-profil au
      niveau handler laissée en attente (valeur modeste, seulement si read home POS mesuré chaud).

## Phase 4 — Éviction after-commit
- [ ] Vérifier si l'éviction actuelle est réellement post-commit (pas seulement même transaction).
- [ ] Writes critiques : `AfterCommit.run(...)` ou evictor infra post-commit.
- [ ] Tests rollback : transaction rollback → cache non évincé / non repeuplé avec état faux.

## Phase 5 — Ops / kill-switch
- [x] `CacheToggle` (common) — stockage in-memory par instance. NOTE: pas encore via settings /
      partagé multi-instances (à backer plus tard) ; suffit pour couper un cache problématique.
- [x] Kill-switch au niveau CacheManager (`ToggleableCacheManager`/`ToggleableCache`) au lieu de
      `@Cacheable(condition=...)` : une seule place, couvre tous les caches (L1+L2 et Caffeine-seul),
      zéro churn d'annotations. Cache désactivé = no-op (reads miss, writes drop, evict/clear OK).
- [x] Ops : list toggles (`GET /toggles`), disable (`POST /{name}/disable`),
      enable (`POST /{name}/enable`) + clear existant — SUPER_ADMIN, audités.
- [x] Groupes Ops réels : catalog, tenant-config, pricing, drawresult (draw/public différés — noms
      de cache à vérifier avant de les lister).

## Phase 6 — Redis L2 readiness (sérialisation)
- [x] Serializer : retrait module typed-id scalaire (fix `serializeWithType`).
- [x] `enableSpringCacheNullValueSupport()` (NullValue round-trip).
- [x] Writer forçant le type racine `Object` + `WRAPPER_ARRAY` (collections `final` typées).
- [x] `disable(FAIL_ON_UNKNOWN_PROPERTIES)` (résilience schema drift).
- [x] Test de round-trip `RedisCacheSerializerRoundTripTest` (typed-id, JsonNode, List, NullValue).
- [x] Observabilité dev : log `CombinedCache` L1/L2/DB + loggers Redis DEBUG (profil local-ide-redis).
- [ ] Mapper les `JsonNode` cachés (`ResultSlotView.sourceCfg/projectionCfg`,
      `GameSummaryView.flags`) vers des structures stables — un `JsonNode` null revient en `NullNode`.
      ÉVALUÉ : blast radius large (sourceCfg/projectionCfg = 64 refs, flags = 28 refs, exposés au
      web) → refactor dédié, pas un petit commit. Le serializer round-trippe déjà le JsonNode
      (test OK) ; le résidu `null→NullNode` est toléré par les consommateurs `.path()/.isNull()`.
      Risque faible → à traiter dans un change à part (ou normaliser null→NullNode côté mapper).
- [ ] Vérifier activation prod/staging de `tch.cache.redis.enabled` (infra).
