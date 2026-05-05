# Tasks

## 1. Configuration and local debug

- [ ] Replace `tch.draw.results.shared.*` with canonical `tch.draw.results.*`.
- [ ] Restructure `DrawResultsProperties` under `tch.draw.results` with `active`, `scheduler`, `limits`, `defaults`, `fetch.http`.
- [ ] Make local profiles disable schedulers by default and enable Ops/manual endpoints.
- [ ] Support fake providers locally for deterministic pipeline testing.
- [ ] Ensure all manual Ops requests support `dryRun`, `force`, `reason`, and `maxSlots/maxItems`.

## 2. Draw lifecycle

- [ ] Generate D..D+N using tenant timezone resolver, fallback UTC.
- [ ] Use one lifecycle tick for close/open due draws.
- [ ] Add `@TchTx` to generate/open/close handlers that write.
- [ ] Fix `OpenDueDrawsCommandHandler` to bulk open only non-locked IDs.
- [ ] Inject `Clock` everywhere; remove direct `Instant.now()` / `ZonedDateTime.now()`.
- [ ] Use `Instant` for domain event timestamps; use timezone only for calculations.
- [ ] Remove `DrawChannelView` dependency from `Draw` aggregate; use channel id/code/snapshot.
- [ ] Move `DrawSummary` and `DrawChannelSummary` out of domain into query projections.

## 3. Draw channel and result slot model

- [ ] Move `DrawChannelLabelResolver` out of `core.draw`.
- [ ] Rename to `DrawChannelDisplayFormatter` under `catalog.drawchannel`.
- [ ] Remove `shortLabel()` parsing.
- [ ] Add structured `draw_channel.period` field.
- [ ] Ensure `draw_channel` references `result_slot_id` for external result binding.
- [ ] Move provider/external mapping out of `draw_channel` and into `result_slot.source_cfg`.

## 4. Draw result persistence and writer

- [ ] Add unique constraint `uq_draw_result_slot_occurred(result_slot_id, occurred_at)`.
- [ ] Add indexes on `(result_slot_id, occurred_at)` and `source_hash`.
- [ ] Replace find-then-insert writer with atomic `INSERT ... ON CONFLICT`.
- [ ] Update `source_result` during update.
- [ ] Respect `force`: do not overwrite `CONFIRMED`/`OVERRIDDEN` when `force=false`.
- [ ] Replace `UUID.randomUUID()` with `IdGenerator` or DB-generated UUID.
- [ ] Inject `Clock`; replace `Instant.now()`.
- [ ] Evict draw result cache after commit; target precise evictions later.
- [ ] Standardize statuses: `PROVISIONAL`, `CONFIRMED`, `OVERRIDDEN`, optional `REJECTED`/`INVALID`.

## 5. Fetch results

- [ ] Add `@TchTx` to `FetchExternalResultsWindowCommandHandler`.
- [ ] Clamp `cmd.maxSlots()` with `tch.draw.results.limits.hardMaxSlots`.
- [ ] Preload/resolve result slots before date loop.
- [ ] Expose/log counters for slot not found, inactive, and no external result.
- [ ] Use combined `source_hash` for pick3+pick4/bundle.
- [ ] Keep `SourceResultBuilder` as one null-safe infra util.
- [ ] Delete or refactor `ExternalPickMapper` to `fromPick3Pick4(p3,p4)` if kept.
- [ ] Rename/reposition `DefaultHaitiProjectionConfigAdapter` because it is not persistence.

## 6. Apply results

- [ ] Add `@TchTx` to `ApplyExternalResultsWindowCommandHandler`.
- [ ] Keep fetch global and apply tenant-scoped.
- [ ] Clamp `cmd.maxSlots()` with hard max.
- [ ] Preload/resolve result slots before date loop.
- [ ] Use deterministic `occurredAt = date + slot.drawTime + slot.timezone`.
- [ ] Publish `DrawResultAppliedEvent` after commit.
- [ ] Capture stable `eventTime = now` before `AfterCommit`.
- [ ] Review/retire legacy `DrawResultIngestedEvent`.
- [ ] Ensure listeners target correct cache/domain (`draw` vs `drawresult`).

## 7. Repositories cleanup

- [ ] Remove/refactor legacy `DrawBatchQueryRepository` using `channel_code/draw_date` and provider fields.
- [ ] Replace joins with `draw_channel.result_slot_id`, `draw.draw_date`, and `draw.draw_result_id`.
- [ ] Fix `@Param` mismatch in `findClosedDrawIdsForSlot`.
- [ ] Remove invalid `PENDING` status from draw queries unless enum/model supports it.
- [ ] Fix `DrawApplyJdbcRepository` timestamp binding.
- [ ] Consolidate `ApplyCandidateDrawJdbcRepository` into `DrawApplyJdbcRepository`.
- [ ] Use tenant timezone resolver for date-range queries.

## 8. Haiti projection

- [ ] Remove Spring/logging/Jackson from `core.haiti.domain.*`.
- [ ] Instantiate `DefaultHaitiResultProjector` via infra config.
- [ ] Prefer domain `HaitiResult` and map to common contract in adapter.
- [ ] Use `result_slot.projection_cfg` as source of projection config.
- [ ] MVP requires both pick3 and pick4 for valid projection.
- [ ] Store versioned `draw_result.haiti_result` JSON.

## 9. US Lottery

- [ ] Keep one dedicated provider client per provider.
- [ ] Rename `ProviderDrawQuery.channelCodes` to `externalGameCodes`.
- [ ] Rename `LatestDraw.channelCode` to `externalGameCode` or `providerGameCode`.
- [ ] Verify real NY/FL/GA/TX/TN responses and finalize mapping.
- [ ] Ensure provider clients only parse provider payloads and return `List<LatestDraw>`.
- [ ] Ensure provider clients do not know tenant/draw/draw_channel/draw_result/HT game codes.
- [ ] Use `result_slot.source_cfg` in `UsLotteryExternalResultsFetchPortAdapter` to call provider once per slot/date.
- [ ] Remove approximated provider-client draw times where result slot is authoritative.
- [ ] Inject `Clock`; remove direct `Instant.now()` in provider clients and flags.
- [ ] Standardize `queryHash` and `sourceHash` semantics.
- [ ] Rename cache name to `infra.uslottery.provider_raw`.
- [ ] Keep `UsLotteryGameRegistry` as provider config registry only.

## 10. Ops controllers

- [ ] Move apply endpoint to `/platform/ops/draw-results/apply`.
- [ ] Keep fetch endpoint global with `tenantId = null` and gate tenant null.
- [ ] Keep apply/refresh/manual/override tenant-scoped with tenant required.
- [ ] Inject `Clock` in Ops controllers for default `now/baseDate`.
- [ ] Replace hardcoded max slots default with `DrawResultsCommonProperties.defaults.maxSlots`.
- [ ] Validate `slotKeys` explicitly or support explicit `allActiveSlots` mode.
- [ ] Add `@AuditLog` to manual/override/force refresh and other sensitive Ops actions.
