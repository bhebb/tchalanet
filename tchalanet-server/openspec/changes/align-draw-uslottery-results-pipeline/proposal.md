# Change: Align draw, drawresult and US Lottery results pipeline

## Status

Proposed

## Owner

Backend / Draw results pipeline

## Why

The draw/results pipeline is one of the central flows in Tchalanet. The target model is clear:

- `game` = sold betting product, for example `HT_BOLET`, `HT_MARYAJ`, `HT_LOTO3`.
- `draw_channel` = tenant-scoped sales calendar/channel.
- `result_slot` = global expected external result slot, for example `NY_MID`, `FL_EVE`.
- `draw_result` = global external/Haiti-projected result for a slot and occurrence time.
- `draw` = tenant-scoped draw lifecycle record, which may reference a global `draw_result`.

Recent audit work found several inconsistencies:

- External fetch jobs are treated as tenant-scoped even though they write global `draw_result`.
- Some code still uses ambiguous `channelCode` naming in provider/external result logic.
- Fetch logic must be `result_slot`-first, not `draw_channel_code` or sold `game_code` first.
- US Lottery provider clients should normalize provider payloads only and not know tenant/draw/Haitian product concepts.
- Provider raw cache belongs under `core.uslottery`, not `core.draw`.
- `DrawResultIngestedEvent` and `DrawResultAppliedEvent` semantics are not consistently enforced.
- Some draw listeners consume drawresult events but use drawresult infra cache evictors from draw.
- Some draw apply queries/repositories still depend on legacy fields or naming patterns.
- Some event-driven cache invalidation uses `evictAll()` broadly.

This change aligns the pipeline with the stable domain model and prepares the codebase for the future consolidated OpenSpec around draw, drawresult, drawchannel, game and uslottery.

## What changes

### Result-slot-first pipeline

- External fetch is driven by `result_slot_key`.
- Provider source configuration is resolved from `result_slot.source_cfg`.
- Sold `game_code` is never used as provider fetch identity.
- Tenant `draw_channel.code` is never used as provider fetch identity.
- Apply joins tenant draws through `draw_channel.result_slot_id`.

### Batch scopes

- `RESULTS_EXTERNAL_FETCH` becomes global.
- `RESULTS_EXTERNAL_APPLY` remains tenant-scoped.
- `RESULTS_EXTERNAL_REFRESH` is clarified.

### DrawResult fetch

- Fetch resolves active global `result_slot`.
- Fetch calls the provider adapter once per slot/date where possible.
- Fetch recomposes pick3/pick4 bundle when needed.
- Fetch writes/upserts global `draw_result`.
- Draw result uniqueness is `(result_slot_id, occurred_at)`.
- Source hash is used for idempotency/change detection/provider corrections.
- Force semantics are respected.

### Apply

- Apply is tenant-scoped.
- Apply requires tenant context.
- Apply attaches global `draw_result` to matching tenant draws.
- Apply emits `DrawResultAppliedEvent` after commit.
- Apply uses deterministic occurredAt/date matching rules.

### US Lottery

- Provider clients are renamed/refactored to use provider terminology:
  - `externalGameCodes`;
  - `externalGameCode` or `providerGameCode`.
- Provider clients do not know tenant, draw, draw_channel, draw_result or Haitian sold game concepts.
- `UsLotteryExternalResultsFetchPortAdapter` maps `result_slot.source_cfg` to provider query codes.
- Query hash and source hash semantics are standardized.
- Provider clients use injected `Clock` where current time is needed.

### Cache

- US Lottery provider raw cache moves to `core.uslottery.infra.cache`.
- Cache name becomes `infra.uslottery.provider_raw`.
- `UsLotteryCacheSpecProvider` declares TTLs.
- Provider raw cache is safe if cache is absent or fails.

### Events

- `DrawResultIngestedEvent` is limited to global result ingestion.
- `DrawResultAppliedEvent` is the main tenant/cross-domain result event.
- Draw cache listeners live in `core.draw` and use `DrawCacheEvictor`.
- DrawResult cache listeners live in `core.drawresult` and use `DrawResultCacheEvictor`.
- Listeners are split and idempotent.

## Non-goals

- Do not redesign sales ticket settlement in this change.
- Do not implement the full future payout flow here.
- Do not redesign public ticket verification here.
- Do not add new US Lottery providers.
- Do not build a full outbox/event bus.
- Do not change tenant game/product configuration.
- Do not introduce multi-entry tickets.

## Expected impact

- Result fetching no longer repeats global writes per tenant.
- The meaning of provider codes, draw channels, games and result slots becomes clear.
- US Lottery code becomes isolated and easier to test.
- Draw/drawresult event flow becomes reliable.
- Cache invalidation is owned by the correct bounded context.
- Apply events become the stable trigger for sales, payout, stats and public projections.

## Risks

- Existing Ops endpoints or schedulers may expect tenant id for fetch.
- Existing tests may use `channelCode` terminology and need updates.
- Existing listeners may rely on `DrawResultIngestedEvent` for behavior that should move to `DrawResultAppliedEvent`.
- Cache name changes invalidate old Redis keys.
- Repository query changes may expose missing indexes.

## Rollout

1. Fix names and cache ownership first.
2. Correct batch job scopes.
3. Refactor fetch/apply handlers.
4. Refactor events and listeners.
5. Update repository queries.
6. Add tests for fetch, apply and events.
7. Validate local manual Ops flow with scheduler disabled.
8. Validate staging with fake and real providers.
