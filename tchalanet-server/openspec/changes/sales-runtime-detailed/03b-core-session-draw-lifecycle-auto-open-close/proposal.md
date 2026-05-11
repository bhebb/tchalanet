# Change: core.session Auto Open/Close Around Draw Lifecycle

## Why

For MVP seller UX, a seller should be able to arrive and sell without always manually opening a session.
The platform may auto-open a seller sales session when daily draws open, and auto-close it after the last operational slot/day is closed and no selling work remains.

Manual control must remain available:

- seller can open before the scheduler
- seller can set opening float/cash amount
- seller can close when they want
- scheduler never overrides an explicit manual session decision without safe rules

## What

Add a dedicated scheduler/ops flow for SalesSession auto-open/auto-close driven by draw lifecycle and outlet configuration.

## Key decisions

- SalesSession remains seller-scoped.
- Auto-open is configured per outlet.
- Auto-open uses eligible assigned sellers, or a configured default/system seller if the tenant chooses that mode.
- Auto-open may use a virtual terminal.
- Opening float:
  - if seller opens manually, seller provides the amount
  - if scheduler auto-opens, default amount is 0 unless outlet config defines another value
  - if `require_opening_float=true` and auto-open is not allowed with zero/default, scheduler skips with reason
- Auto-close is allowed after the last relevant draw slot/window is closed and no sellable draws remain for the business day.
- Auto-close must not wait for payout, because payout can happen after selling session closure.
- Tickets remain attached to the closed sales session.

## Non-goals

- Do not make session terminal-scoped again.
- Do not close sessions just because one draw closes if other sellable draws remain open.
- Do not implement payout session lifecycle here.
