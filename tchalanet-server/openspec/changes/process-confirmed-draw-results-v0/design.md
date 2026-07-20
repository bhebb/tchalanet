# Design: Process Confirmed Draw Results V0

## State boundary

`PROVISIONAL` means a global provider result exists but is not financially actionable. It may be
shown to platform operations and trigger a stale-provisional reminder. It must not be attached to a
tenant draw.

`CONFIRMED` and `OVERRIDDEN` are actionable. Application attaches the result to each eligible
closed draw and emits one tenant-scoped `DrawResultAppliedEvent` per draw.

## Processing pipeline

```
global result (CONFIRMED | OVERRIDDEN)
  -> attach to closed tenant draw
  -> DrawResultAppliedEvent
  -> bounded ticket result command
  -> TicketResultedEvent / TicketPayoutPaidEvent
  -> terminal ticket state
  -> draw settlement
```

The event listener is a low-latency trigger, not the only recovery mechanism. A scheduler/batch
candidate reads `RESULTED` draws with non-terminal tickets and replays bounded commands. It only
settles a draw when the persisted ticket state proves there are no remaining eligible tickets.

## Failure semantics

- A per-ticket calculation/persistence error is logged with ticket/draw correlation and leaves that
  ticket pending.
- It is not counted as a successful event delivery and it cannot cause the draw to settle.
- Replays select pending tickets from the database; already terminal tickets emit no duplicate
  result or payout event.
- A systemic command failure is propagated so normal event/batch retry machinery can observe it.

## Operations notifications

The existing `DRAW_RESULT_ACTION_REQUIRED` notification is extended with a provisional-stuck reason.
The scheduler dispatches a command only; the command persists the notification and the existing
notification-to-communication bridge delivers Slack idempotently.
