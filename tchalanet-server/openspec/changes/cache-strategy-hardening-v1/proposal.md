# Change: cache-strategy-hardening-v1

## Why

An audit of the L1 (Caffeine) / L2 (Redis) cache revealed correctness and consistency gaps:

- **Serialization** with the L2 Redis mapper was broken for typed-ids, root-level collections
  and `NullValue` (production warnings on `catalog:resultslot:v2:active` and
  `platform.tenant.cache.REGISTRY_BY_ID`). Fixed as an inline hotfix; captured here for record.
- **~20 caches have no `CacheSpecProvider`** and silently fall back to generic defaults
  (L1 5 min / L2 60 min) — a rarely-mutated referential expires as fast as volatile session data.
- **`catalog:drawchannel:calendar_rows`** is cached but **never evicted** → stale draw calendars
  after channel/game edits (returns wrong data until TTL).
- Dead scaffolding: `catalog:drawchannel:by_result_slot_*` evicted but never populated;
  `core.drawresult.*` has an evictor but no `@Cacheable` populates it.
- No per-cache kill-switch: Ops can only flush a cache (it refills), not disable it.
- No policy forbidding caches from participating in money / consumed-limit / POS-security decisions.

Reference (design basis, not duplicated here):
`docs/conventions/cache.md`, `docs/conventions/cache-gap-analysis.md`,
`docs/conventions/cache-strategy-review.md`.

## What Changes

- Fix P0 correctness bugs: `calendar_rows` eviction, remove/​wire dead caches, wire `drawresult`.
- Declare a `CacheSpecProvider` for every cache; TTLs driven by modification frequency (tiers).
- Add stable per-tenant runtime caches (tenant theme, tenant game) with eviction on write.
- Add descriptive-only caches for limit policy definition and seller-terminal profile — never for
  consumed counters or sale/binding validation.
- Verify eviction is after-commit; add rollback tests.
- Add a runtime per-cache kill-switch (`CacheToggleProvider`) + Ops enable/disable + real groups.
- L2 readiness: keep the serializer hardening, map cached `JsonNode` fields to stable shapes.

## Impact

- Backend only (`tchalanet-server`): `app/config/cache`, `catalog/*`, `platform/tenant*`,
  `core/{drawresult,limit,sellerterminal}`, `features/ops/cache`.
- New Ops endpoints for cache enable/disable (SUPER_ADMIN, audited).
- Redis L2 wire format changes → flush cache on deploy of the serializer hotfix.
- No schema/migration changes expected.

## Non-goals

- Caching money-affecting or gating data (consumed limits, ticket lists, audit, sale validation).
- Migrating legacy dot-named cache names (`core.drawresult.*`, `platform.tenant.cache.*`) — keep to
  avoid breaking persisted L2 keys.
- Enabling Redis L2 in production (config/infra decision, tracked separately).
