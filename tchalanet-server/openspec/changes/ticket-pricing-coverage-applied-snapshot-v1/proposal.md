# Ticket pricing coverage and applied result snapshot

## Why

The current ticket payload exposes settlement terms used by the result calculator, but not the
complete set of payout possibilities shown to an operator before the draw result exists. This is
not sufficient for the intended behavior: one Bòlèt selection can be covered by lot 1, lot 2 and
lot 3, while the settled ticket must identify only the winning lot and its realized payout.

The same historical rule applies as for commission snapshots: changing a seller or tenant pricing
configuration after a sale must never change an existing ticket.

## What

Persist two distinct immutable concepts on each ticket line:

1. The existing `SettlementTermsSnapshot`, populated with every effective payout rule that could
   apply to the commercial game selection at sale time, including the source of each rule. This is
   both the settlement input and the pre-result pricing coverage.
2. `appliedSettlementSnapshot`: the rule or rules that actually won after official results, with
   the realized amount for each applied rule.

The settlement terms remain the authoritative input for result evaluation and the source for the
pre-result coverage display. No second persisted coverage snapshot is needed.

## Scope

- Backend sales, pricing, settlement and POS ticket details contracts.
- Persistence and event payloads required to preserve the snapshots.
- Web/mobile display contracts may consume the new fields, but UI implementation is separate.

## Non-goals

- Reading current pricing configuration to render historical tickets.
- Changing commission calculation or recomputing old commissions.
- Printing potential payout values on customer receipts.
- Replacing the existing settlement algorithm with a UI-only inference.
