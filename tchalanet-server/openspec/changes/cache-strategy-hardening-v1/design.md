# Design — cache-strategy-hardening-v1

Full analysis lives in the convention docs (not duplicated here):

- `docs/conventions/cache.md` — normative policy.
- `docs/conventions/cache-gap-analysis.md` — doc↔code gaps + per-cache inventory.
- `docs/conventions/cache-strategy-review.md` — self-contained state + TTL proposal by tier.

## Key decisions

### Two-tier cache
L1 Caffeine (in-process) + L2 Redis (shared) via `CombinedCacheManager`. Redis optional; the app
must work L1-only. Reads: L1 → L2 (promote to L1) → DB. Writes/evicts propagate to both.

### TTL follows modification frequency (the TTL is a safety net, eviction is the source of freshness)
- **Tier A** referentials quasi-statiques (plan, game, theme, resultslot-def, pagemodel, tenant):
  L1 30 min / L2 12 h.
- **Tier B** config par tenant (drawchannel, settings, i18n, tenant theme/game, entitlement,
  user/roles, pricing): L1 15 min / L2 6 h.
- **Tier C** séance (draw next/latest/status, promo active): court (10–60 s) + éviction événementielle.
- **Tier D** non caché: compteurs de limite consommés, tickets/ticket lines, stats live, audit,
  notifications, validation de vente / binding terminal.

### Cache correctness invariants
- Tout cache tenant-scopé porte `tenantId` dans la **clé** (jamais dans le nom).
- Allonger un TTL n'est autorisé que si l'éviction sur write est fiable et post-commit.
- Ne cacher que le **descriptif** (règle, profil), jamais ce qui **autorise** (compteurs, binding).

### L2 serialization (Jackson 3 / Spring Data Redis 4.1) — implemented
Value serializer built via `GenericJacksonJsonRedisSerializer` with:
- `enableSpringCacheNullValueSupport()` for `NullValue`.
- custom writer forcing root static type `Object` so `final` immutable collections
  (`java.util.ImmutableCollections$*` from `.toList()`/`List.of()`) stay type-tagged.
- `activateDefaultTyping(validator, NON_FINAL_AND_RECORDS, WRAPPER_ARRAY)`.
- `disable(FAIL_ON_UNKNOWN_PROPERTIES)` for schema-drift resilience.
- `BasicPolymorphicTypeValidator` allowlist incl. `NullValue` and `java.lang.`.
- **Not** the app `TypedIdsJacksonModule` (its scalar serializer lacks `serializeWithType()`);
  typed-ids serialize as native records — internally consistent for the cache round-trip.

Known caveat (Phase 6 remainder): a Java-`null` `JsonNode` field round-trips back as `NullNode`,
not `null` → cached JsonNode-bearing views must be mapped to stable shapes.

### Kill-switch (Phase 5)
Ops can flush but not disable a cache. Add `@Cacheable(condition = "@cacheToggle.on('name')")`
backed by settings so a problematic cache can be disabled at runtime without redeploy.
