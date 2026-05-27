# Context — Draw → Sales → Payout → Cashier → Reconciliation event flow

## Confirmed language

Do not use `settled` for every domain.

- **Draw settled**: draw has a confirmed result and is ready for sales ticket settlement.
- **Ticket settled**: sales applied the result to a ticket and calculated won/lost/payout amount.
- **Payout claim opened**: payout materialized a payable claim from a winning settled ticket.
- **Payout paid**: money was actually paid through a POS/admin action.
- **Reconciliation completed**: ops compared domain truths and produced anomalies/repair actions.

## Canonical event chain

```text
core.draw
  GenerateDrawsForRangeCommand
    -> Draw SCHEDULED

core.draw
  OpenTodayDrawsCommand
    -> Draw OPEN

core.draw
  CloseDrawCommand
    -> Draw CLOSED

core.drawresult
  FetchDrawResultCommand
    -> DrawResult CONFIRMED

core.draw
  ApplyDrawResultToDrawCommand
    -> Draw RESULTED

core.draw
  SettleDrawCommand
    -> Draw SETTLED
    -> DrawSettledEvent

core.sales
  DrawSettledEvent
    -> SettleTicketsForDrawCommand
    -> TicketSettledEvent
    -> TicketWinningSettlementCreatedEvent for winning tickets only

core.payout
  TicketWinningSettlementCreatedEvent
    -> OpenPayoutClaimFromSettlementCommand
    -> PayoutClaim OPEN
    -> PayoutClaimOpenedEvent

features.cashier
  scan QR/public code
    -> verify ticket/claim status
    -> may call ExecutePayoutCommand if PAYABLE and context is valid

core.payout
  ExecutePayoutCommand
    -> PayoutClaim PAID
    -> PayoutPaidEvent

core.sales
  PayoutPaidEvent
    -> MarkTicketPayoutPaidCommand / projection update only

stats/ledger/reconciliation
  consume TicketPlacedEvent, TicketSettledEvent, PayoutClaimOpenedEvent,
  PayoutPaidEvent, PayoutReversedEvent, offline submission/ticket events
```

## Boundary rule

- Draw owns draw lifecycle.
- DrawResult owns official provider/global result state.
- Sales owns ticket settlement and winning amount snapshots.
- Payout owns payout claim/payment truth.
- Cashier orchestrates POS UX, verification, and payout execution calls.
- Reconciliation compares truths and triggers repairs through owner-domain commands.
