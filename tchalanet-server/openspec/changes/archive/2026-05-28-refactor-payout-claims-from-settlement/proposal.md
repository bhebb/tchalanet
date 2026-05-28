# Change: refactor-payout-claims-from-settlement

## Why

The current payout model mixes manual payout request, approval/rejection, and actual payment. For Tchalanet V1, this is too fragile: sellers should not manually create the truth that a ticket is payable.

Payout must be driven by Sales settlement:

```text
Sales settles a winning ticket
  -> Payout opens a claim automatically
  -> POS/admin pays the claim securely
```

## What changes

- Replace normal `RegisterPayoutCommand` usage with `OpenPayoutClaimFromSettlementCommand`.
- Treat `Payout` as `PayoutClaim` in the domain language.
- Use statuses: `OPEN`, `BLOCKED`, `PAID`, `CANCELLED`, `REVERSED`.
- Open claims from `TicketWinningSettlementCreatedEvent`.
- Keep payment execution in `ExecutePayoutCommand`, strengthened with lock and operational rechecks.
- Add block/unblock/cancel/reverse commands.
- Publish events after commit: `PayoutClaimOpenedEvent`, `PayoutPaidEvent`, `PayoutReversedEvent`.
- Allow Sales to project payout paid/reversed status from payout events without becoming source of payment truth.

## Out of scope

- Cashier readiness UX.
- Ops reconciliation storage.
- Ledger/stats implementation details.
- Complex correction engine for result corrections.
- Approval workflow for high-value payouts; this can be V2.

## Event reminders

Payout should not listen directly to `DrawSettledEvent`.

Correct chain:

```text
DrawSettledEvent
  -> core.sales SettleTicketsForDrawCommand
  -> TicketWinningSettlementCreatedEvent
  -> core.payout OpenPayoutClaimFromSettlementCommand
  -> PayoutClaimOpenedEvent
  -> ExecutePayoutCommand
  -> PayoutPaidEvent
  -> core.sales MarkTicketPayoutPaidCommand / projection only
```
