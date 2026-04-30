# Tasks

## 1. Batch Results alignment

- [ ] Update Results job registry:
  - [ ] `RESULTS_EXTERNAL_FETCH` registered as `GLOBAL`.
  - [ ] `RESULTS_EXTERNAL_APPLY` registered as `TENANT`.
  - [ ] `RESULTS_EXTERNAL_REFRESH` explicitly scoped.
- [ ] Remove `tenant_id` requirement from fetch.
- [ ] Keep `tenant_id` required for apply.
- [ ] Update `JobParamsValidator` rules.
- [ ] Update Ops endpoints if fetch request currently requires tenant.
- [ ] Update schedulers so global fetch is not run once per tenant.
- [ ] Ensure local/debug can run:
  - [ ] fetch manually;
  - [ ] apply manually;
  - [ ] refresh manually;
  - [ ] with `dryRun`;
  - [ ] with `force`;
  - [ ] with `reason`;
  - [ ] with `maxSlots` or `maxItems`.
- [ ] Add tests:
  - [ ] fetch job accepts no tenant id;
  - [ ] apply job requires tenant id;
  - [ ] refresh behavior is documented and tested.

## 2. DrawResult fetch pipeline

- [ ] Ensure fetch command uses `result_slot_key` as primary identity.
- [ ] Remove fetch usages driven by:
  - [ ] `draw_channel_code`;
  - [ ] `channelCode`;
  - [ ] sold `game_code`.
- [ ] Ensure `FetchExternalResultsWindowCommandHandler` is transactional if it writes.
- [ ] Clamp requested max slots by configured hard max.
- [ ] Resolve/preload result slots before nested loops.
- [ ] Resolve active result slot.
- [ ] Compute deterministic `occurredAt` from:
  - [ ] slot draw date;
  - [ ] slot draw time;
  - [ ] slot timezone.
- [ ] Fetch provider bundle by slot/date.
- [ ] Recompose pick3/pick4 bundle.
- [ ] Apply Haiti projection.
- [ ] Compute combined source hash for the full bundle.
- [ ] Upsert `draw_result` atomically.
- [ ] Ensure `source_result` is updated when source changes.
- [ ] Respect `force=false`:
  - [ ] do not overwrite `CONFIRMED`;
  - [ ] do not overwrite `OVERRIDDEN`;
  - [ ] update only allowed provisional/source-change cases.
- [ ] Respect `force=true`:
  - [ ] allow controlled overwrite;
  - [ ] require reason/audit for manual use.
- [ ] Replace direct `UUID.randomUUID()` with IdGenerator or DB UUID.
- [ ] Replace direct `Instant.now()` with injected `Clock`.
- [ ] Evict drawresult cache after commit.
- [ ] Add tests:
  - [ ] idempotent repeated fetch;
  - [ ] provider correction updates provisional result;
  - [ ] confirmed/overridden are protected without force;
  - [ ] force requires reason where applicable.

## 3. DrawResult persistence

- [ ] Ensure DB unique constraint:
  - [ ] `(result_slot_id, occurred_at)`.
- [ ] Ensure indexes:
  - [ ] `(result_slot_id, occurred_at)`;
  - [ ] `source_hash`;
  - [ ] status if useful.
- [ ] Ensure writer uses atomic SQL:
  - [ ] `INSERT ... ON CONFLICT (result_slot_id, occurred_at) DO UPDATE`.
- [ ] Ensure writer updates:
  - [ ] Haiti result;
  - [ ] raw/source result;
  - [ ] source hash;
  - [ ] flags;
  - [ ] status according to rules.
- [ ] Ensure cache eviction runs after commit.
- [ ] Add persistence tests for race/idempotency.

## 4. Apply pipeline

- [ ] Ensure `ApplyExternalResultsWindowCommandHandler` is transactional.
- [ ] Ensure apply command requires tenant id.
- [ ] Clamp max slots/items by configured hard max.
- [ ] Resolve/preload result slots before nested loops.
- [ ] Compute deterministic occurredAt/date.
- [ ] Lookup global draw result by:
  - [ ] result slot;
  - [ ] occurredAt or slot/date rule.
- [ ] Select tenant draws through:
  - [ ] `draw.draw_channel_id`;
  - [ ] `draw_channel.result_slot_id`.
- [ ] Only apply to eligible draws:
  - [ ] typically `CLOSED`;
  - [ ] not already resulted unless force/repair rule allows.
- [ ] Set `draw.draw_result_id`.
- [ ] Transition draw lifecycle according to domain rules.
- [ ] Publish `DrawResultAppliedEvent` after commit.
- [ ] Capture stable event time before after-commit lambda.
- [ ] Add tests:
  - [ ] result applies to matching tenant draw;
  - [ ] result does not apply to wrong tenant;
  - [ ] result does not apply to wrong slot;
  - [ ] event emitted once after commit;
  - [ ] no event on rollback.

## 5. US Lottery provider normalization

- [ ] Rename `ProviderDrawQuery.channelCodes` to:
  - [ ] `externalGameCodes`; or
  - [ ] `providerGameCodes`.
- [ ] Rename `LatestDraw.channelCode` to:
  - [ ] `externalGameCode`; or
  - [ ] `providerGameCode`.
- [ ] Update all provider clients:
  - [ ] NY;
  - [ ] FL;
  - [ ] GA;
  - [ ] TX;
  - [ ] TN if present.
- [ ] Ensure provider clients return normalized `List<LatestDraw>`.
- [ ] Ensure provider clients do not import:
  - [ ] tenant types;
  - [ ] draw types;
  - [ ] draw_channel types;
  - [ ] draw_result persistence types;
  - [ ] Haitian game/product concepts.
- [ ] Ensure provider clients parse provider payload only.
- [ ] Ensure `UsLotteryExternalResultsFetchPortAdapter` maps:
  - [ ] `result_slot.source_cfg`;
  - [ ] provider key;
  - [ ] external game codes;
  - [ ] date.
- [ ] Standardize `queryHash`.
- [ ] Standardize `sourceHash`.
- [ ] Inject `Clock` where current time is needed.
- [ ] Add tests per provider with sample payloads.

## 6. US Lottery cache cleanup

- [ ] Create `UsLotteryCacheSpecProvider` under `core.uslottery.infra.cache`.
- [ ] Declare cache:
  - [ ] `infra.uslottery.provider_raw`.
- [ ] Use short TTLs:
  - [ ] L1 around 1-2 minutes;
  - [ ] L2 around 5-10 minutes.
- [ ] Update `UsLotteryProviderRawCache.CACHE_NAME`.
- [ ] Remove provider raw cache declaration from `core.draw`.
- [ ] Add safe get/put/evict in `UsLotteryProviderRawCache`.
- [ ] Move cache hit/miss logs to DEBUG.
- [ ] Keep WARN for cache failures.
- [ ] Keep local anti-stampede lock as MVP.
- [ ] Do not add Redis distributed lock yet.
- [ ] Add tests:
  - [ ] cache hit;
  - [ ] cache miss;
  - [ ] fetcher null is not cached;
  - [ ] cache unavailable still fetches;
  - [ ] cache put failure does not fail fetch.

## 7. Draw cache cleanup

- [ ] Rename draw cache names to `core.draw.*`.
- [ ] Remove `catalog.draw.*` for lifecycle draw data.
- [ ] Fix `CacheSpec.of(name, ttlL1, ttlL2)` order.
- [ ] Remove or consolidate `DrawCacheKeyBuilder`.
- [ ] Add draw key helpers to common `CacheKeyBuilder` if appropriate.
- [ ] Use `tenantId.value()` in draw keys.
- [ ] Add `DrawCacheEvictor` if missing.
- [ ] Replace `evictAll()` with targeted eviction where practical:
  - [ ] by tenant;
  - [ ] by draw id;
  - [ ] by draw date;
  - [ ] by channel.
- [ ] Add tests for draw cache key and eviction.

## 8. Event cleanup

- [ ] Review `DrawResultIngestedEvent`.
- [ ] Confirm it represents global ingestion only.
- [ ] Remove tenant/draw fields if they do not belong to global ingestion.
- [ ] Rename or retire event if semantics are wrong.
- [ ] Create or standardize `DrawResultAppliedEvent`.
- [ ] Ensure it includes necessary consumer data:
  - [ ] event id;
  - [ ] occurredAt;
  - [ ] tenant id;
  - [ ] draw id;
  - [ ] draw date;
  - [ ] result slot id/key if needed;
  - [ ] draw result id if needed;
  - [ ] channel id/code if needed.
- [ ] Emit `DrawResultAppliedEvent` after commit in apply handler.
- [ ] Move drawresult cache invalidation listener to `core.drawresult`.
- [ ] Move draw cache invalidation listener to `core.draw`.
- [ ] Ensure draw listener uses `DrawCacheEvictor`.
- [ ] Ensure drawresult listener uses `DrawResultCacheEvictor`.
- [ ] Remove `core.draw` listener dependency on `core.drawresult.infra.cache.DrawResultCacheEvictor`.
- [ ] Split generic `DrawDomainEventListener`.
- [ ] Add consumer idempotency with atomic mark-if-absent.
- [ ] Add event tests.

## 9. Repository/query cleanup

- [ ] List all draw/drawresult batch repositories.
- [ ] Remove legacy apply queries based on:
  - [ ] `channel_code + draw_date` where result_slot is needed;
  - [ ] `external_game_key`;
  - [ ] `external_channel_code`.
- [ ] Ensure apply SQL joins through:
  - [ ] `draw.draw_channel_id`;
  - [ ] `draw_channel.result_slot_id`;
  - [ ] `draw_result.result_slot_id`.
- [ ] Ensure draw status enum values are valid.
- [ ] Remove invalid status literals such as `PENDING` if not part of enum.
- [ ] Fix `@Param` mismatches.
- [ ] Use timestamp setters, not string timestamp fallback.
- [ ] Inject `Clock`.
- [ ] Add repository tests.

## 10. Scheduler/local debug

- [ ] Ensure scheduler only decides when to try.
- [ ] Scheduler SHALL NOT contain core business logic.
- [ ] Disable schedulers in local profile if configured.
- [ ] Provide Ops/manual endpoints to drive:
  - [ ] generate;
  - [ ] open;
  - [ ] close;
  - [ ] fetch;
  - [ ] apply;
  - [ ] refresh.
- [ ] Support request fields:
  - [ ] `dryRun`;
  - [ ] `force`;
  - [ ] `reason`;
  - [ ] `maxItems`;
  - [ ] `maxSlots`.
- [ ] Ensure Ops endpoints are SUPER_ADMIN-only where appropriate.
- [ ] Ensure manual force operations are audited.
