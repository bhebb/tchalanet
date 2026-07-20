# Process Confirmed Draw Results V0

## Why

The current processing path can attach a `PROVISIONAL` global result to tenant draws. That emits
`DrawResultAppliedEvent`, calculates ticket winnings and records automatic payouts even though the
draw cannot be settled until the result is confirmed. In addition, an individual ticket-processing
failure is swallowed and the source event is marked processed, leaving the ticket pending without a
deterministic retry path.

## What Changes

- Treat `PROVISIONAL` as operations-visible only: it never attaches to tenant draws and never
  triggers ticket calculation, payout events, analytics winning updates, or draw settlement.
- Allow tenant draw application only for `CONFIRMED` and `OVERRIDDEN` global results.
- Make ticket result processing bounded and replayable from persisted ticket state. A failed ticket
  remains eligible for a later processing attempt; it cannot be hidden by event idempotency.
- Settle a tenant draw only after every eligible ticket has a terminal result/settlement state.
- Retire the separate draw-settlement batch path in favour of one processing pipeline. Keep an
  explicit ops force action for deterministic backfill/recovery.
- Replace the provisional watchdog's log-only signal with the existing platform notification and
  Slack bridge, without creating a second notification mechanism.

## Impact

- `core.drawresult`, `core.draw`, `core.sales`, analytics event consumers, scheduler/batch wiring,
  tests and near-code domain documentation.
- No schema migration is planned for V0: replay eligibility comes from existing ticket and draw
  lifecycle state. A durable per-ticket processing journal requires a separate approved migration
  if future scale measurements prove it necessary.

## Non-goals

- Changing the V0 rule that a winning ticket is automatically paid when it is settled.
- Redesigning correction/reversal accounting after a settled draw.
- Changing provider fetch policies or tenant-facing result notification audiences.
