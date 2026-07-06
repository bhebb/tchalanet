# Proposal: supported-bet-options-combinations-v1

## Summary

Define the official Tchalanet-supported bet options and implicit settlement variants for Haiti
lottery games.

This change separates:

- `BetType` / `BetOption`: what the seller terminal and clients understand.
- `SettlementVariant`: what backend computes for winning calculation and payout/audit.
- Display labels: how web/mobile/admin render the option without leaking technical variants
  everywhere.

The goal is not to copy every provider combination. Provider docs are inputs, but the Tchalanet
support matrix is the product contract.

## Problem

Some lottery combinations are implicit in Haitian betting practice.

Example:

```text
Loto 4 · Permuté · 1234
```

The seller does not choose `24-way box`, but the backend must know that `1234` is a
`LOTTO4_BOX_24_WAY` case to settle correctly and explain support/admin views.

Currently:

- `BetType` and `BetOption` already represent useful sale choices.
- `TicketWinningCalculator` decides won/lost.
- There is no explicit `SettlementVariant` contract.
- Web/admin cannot safely explain variations without duplicating backend rules.
- Future provider-specific options such as 3-way, 6-way, 12-way, 24-way, straight/box, fireball,
  wheel, or boosters could be accidentally introduced as labels without settlement support.

## Goals

- Keep the selling experience simple for the seller terminal.
- Define exactly which options Tchalanet supports.
- Represent implicit variants explicitly in backend settlement.
- Make winning calculation deterministic and testable.
- Allow admin/support/result pages to explain variations.
- Keep web/mobile labels aligned with backend semantics.
- Prevent unsupported provider options from being sold silently.

## Non-Goals

- Do not support every provider-listed combination.
- Do not add straight/box combined bets in this change.
- Do not add fireball, bonus, wheel, or provider-specific boosters.
- Do not expose 3-way, 6-way, 12-way, or 24-way as seller choices in v1.
- Do not move settlement rules into frontend.
- Do not turn `catalog.game` into a settlement engine.

## Architecture Decision

`catalog.game` keeps `BetType` and `BetOption` as the sale/display catalog.

`core.sales` owns settlement calculation through:

- `SettlementVariant`
- `SettlementVariantResolver`
- `TicketWinningCalculator`

This respects the current architecture: catalog is read-mostly/reference data and should not own
business invariants, while critical business logic belongs in core sales.

Handlers and application services must keep using command/query boundaries and typed models;
controllers stay thin and do not calculate settlement.

## Product Rule

Tchalanet sells simple, understandable options.

The seller terminal displays:

- Exact
- Permuté
- Deux premiers
- Deux derniers
- Ordre exact
- Revers / double
- 1er + 2e lot
- 1er + 3e lot
- Mixte

The backend computes technical variants:

- 3-way
- 6-way
- 12-way
- 24-way

The client receipt shows the simple option.

Admin/support may show the computed technical variant.

Unsupported provider combinations must not be sold unless explicitly added to the support matrix
and tested in settlement.

## Persistence Position

At sale preview, backend resolves the computed settlement variant from `gameCode`, `betType`,
`betOption`, and canonical selection.

At sale confirmation, backend persists the existing sale contract (`betType`, `betOption`,
selection, odds/potential payout snapshot). Persisting `computed_settlement_variant` is a design
decision in this change:

- If payout depends on variant-specific odds, persist it as a sale-time snapshot.
- If payout does not depend on the variant, backend may recompute it for settlement.
- For audit/support clarity, persistence is preferred if a migration is accepted.

## Cross-Slice Impact

- Backend: catalog bet option contract and core sales settlement.
- Web/mobile/seller terminal: consume supported sale labels; do not expose technical variants by
  default.
- Admin/platform: may display computed settlement variants in support/debug/detail contexts.
