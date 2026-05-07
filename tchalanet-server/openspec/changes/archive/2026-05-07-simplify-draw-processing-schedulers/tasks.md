# Tasks — simplify-draw-processing-schedulers

## P0 — Configuration and policy alignment

- [x] Create/align scheduler properties under `tch.draw.scheduler`.
- [x] Add nested configs:
  - [x] `generate`
  - [x] `openToday`
  - [x] `processing.close`
  - [x] `processing.fetch`
  - [x] `processing.apply`
  - [x] `processing.settle`
- [x] Replace ambiguous current config names such as `due.min/max/lookback` with:
  - [x] `startMinutesAfterDraw`
  - [x] `retryEveryMinutes`
  - [x] `stopMinutesAfterDraw`
  - [x] `defaultSalesOpenTime`
- [x] Add local YAML defaults with debug-friendly values if needed.
- [x] Log effective scheduler config once at startup or first tick.

## P0 — Generate and open schedulers

- [x] Implement/adjust `DrawGenerateScheduler`:
  - [x] cron default `0 0 5 * * *`
  - [x] generate next 7 days
  - [x] all active tenants
  - [x] idempotent command
- [x] Implement/adjust open-today scheduling in `DrawLifeCycleTickScheduler`:
  - [x] cron default `0 */5 4-10 * * *`
  - [x] open SCHEDULED draws for each channel-local draw date when effective sales opening time is due
  - [x] skip already advanced statuses
- [x] Ensure both schedulers honor gates.

## P0 — Processing tick orchestration

- [x] Create or simplify one processing tick scheduler.
- [x] Run steps in order:
  - [x] close
  - [x] fetch
  - [x] apply
  - [x] settle
- [x] Ensure each step has its own active flag and gate.
- [x] Ensure one step failure does not prevent later safe steps unless configured otherwise.
- [x] Add concise summary logs.

## P0 — Close due draws

- [x] Close due policy:
  - [x] target only OPEN draws
  - [x] due if `cutoff_at <= now`
  - [x] no global `minutesBeforeDraw`
- [x] Scheduler must not recompute provider/slot business rules.
- [ ] Add/adjust `CloseDueDrawsCommand` to accept:
  - [ ] `tenantId`
  - [ ] `drawDate`
  - [ ] `slotKeys`
  - [ ] `drawIds`
  - [ ] `force`
  - [x] `dryRun`
  - [ ] `reason`
  - [x] `maxItems`
- [ ] Force close must require reason and audit.

## P0 — Fetch due results

- [x] Fetch due policy:
  - [x] start at `drawTime + 5 minutes`
  - [x] retry every `5/10 minutes`
  - [x] stop after configured window, default 240 minutes
- [x] Use result slots as the source of truth:
  - [x] `slotKey`
  - [x] `drawTime`
  - [x] `timezone`
- [x] Do not require tenant for fetch.
- [x] Do not fetch slot/date already CONFIRMED unless forced.
- [x] Replace `cooldown` naming with retry interval where possible.
- [x] Track retry by stable fingerprint such as `fetch:{slotKey}:{drawDate}`.
- [x] Ensure fetch writes/upserts global draw_result only.

## P0 — Apply results

- [x] Apply due policy:
  - [x] start at `drawTime + 10 minutes`
  - [x] retry every `30 minutes`
  - [x] stop after configured window, default 720 minutes
- [x] Apply only when matching `draw_result` exists.
- [x] Apply only to CLOSED tenant draws without `draw_result_id`.
- [x] Do not overwrite existing attached result; use dedicated correction/override flow.
- [x] Publish after-commit event when draw transitions to RESULTED.

## P0 — Settle tickets

- [x] Settle due policy:
  - [x] start at `drawTime + 20 minutes`
  - [x] retry every `30 minutes`
  - [x] stop after configured window, default 1440 minutes
- [x] Settle only RESULTED draws with `draw_result_id`.
- [x] Settle only tickets not already settled/finalized.
- [x] Ensure idempotency is atomic where possible.
- [x] Do not double payout.

## P1 — Ops endpoints

- [x] Ensure Ops endpoints exist for:
  - [x] generate
  - [x] open today
  - [x] close due
  - [x] fetch results
  - [x] apply results
  - [ ] settle tickets
- [ ] Add request models with common fields:
  - [ ] `tenantId`
  - [ ] `drawDate`
  - [ ] `slotKeys`
  - [ ] `drawIds`
  - [ ] `force`
  - [ ] `dryRun`
  - [ ] `reason`
  - [ ] `maxItems`
- [ ] Fetch request additionally supports optional `drawTimeOverride` and `skipRetry`.
- [ ] Force operations require non-blank reason.
- [ ] Force operations are audited.

## P1 — Observability and tests

- [x] Add tests for due-window calculations.
- [x] Add tests for retry interval behavior.
- [ ] Add tests for force bypassing scheduler windows but not business invariants.
- [ ] Add integration/smoke tests for full flow:
  - [ ] generate
  - [ ] open
  - [ ] close
  - [ ] fetch
  - [ ] apply
  - [ ] settle
- [x] Add logs for effective config and tick summaries.

## P2 — Cleanup old scheduler code

- [x] Remove obsolete `due.lookback` logic if unused.
- [x] Remove duplicate schedulers that overlap with the new processing tick.
- [x] Archive old config keys or support temporary aliases with deprecation logs.
- [ ] Update docs and OpenSpec index.

## Implementation note

- `CloseDueDrawsCommand` now uses generated `draw.cutoff_at` and dry-run/max-items path.
- `OpenTodayDrawsCommand` uses `draw_channel.sales_open_time`, falling back to configured `defaultSalesOpenTime`.
- Full force close targeting (`tenantId`, `drawDate`, `slotKeys`, `drawIds`, forced reason/audit) remains open for the manual Ops hardening slice.
- Settle manual endpoint is still open; the automatic processing tick reuses the existing idempotent settlement batch.
